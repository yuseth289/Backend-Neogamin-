import json
import time
from typing import TypedDict

from langgraph.graph import StateGraph, END
from langchain_core.messages import HumanMessage

from src.agents.search.search_prompts import (
    QUERY_ANALYSIS_PROMPT,
    EXPLANATION_PROMPT,
    CLARIFICATION_PROMPT,
)
from src.agents.search.search_tools import fetch_products_from_spring
from src.core.logging import get_logger
from src.models.search_models import (
    ExtractedEntities,
    ProductRecommendation,
    SearchRequest,
    SearchResponse,
)
from src.core.llm_json import extract_text as _extract_text, parse_json_safely as _parse_json_safely
from src.services.gemini_client import get_chat_model

logger = get_logger(__name__)


class SearchState(TypedDict):
    query: str
    clarification: str | None
    user_id: str
    session_id: str
    entities: ExtractedEntities | None
    intent: str | None
    filters: dict | None
    products: list[dict]
    recommendations: list[ProductRecommendation]
    needs_clarification: bool
    clarification_question: str | None
    greeting: str | None
    closing_message: str | None
    start_time: float


async def analyze_query_node(state: SearchState) -> dict:
    """Single Gemini call extracts entities + intent together — avoids concurrent call issues."""
    clarification_ctx = f"\nUser clarification: {state['clarification']}" if state.get("clarification") else ""
    prompt = QUERY_ANALYSIS_PROMPT.format(query=state["query"], clarification_context=clarification_ctx)
    model = get_chat_model(temperature=0.1)
    try:
        response = await model.ainvoke([HumanMessage(content=prompt)])
        data = _parse_json_safely(response.content)
        entities = ExtractedEntities(**(data.get("entities") or {}))
        intent = data.get("intent", "search").lower()
        if intent not in {"search", "comparison", "compatibility", "recommendation", "clarification_needed"}:
            intent = "search"
    except Exception as exc:
        logger.warning("query_analysis_failed", error=str(exc))
        entities = ExtractedEntities()
        intent = "search"
    return {"entities": entities, "intent": intent}


def build_filters_node(state: SearchState) -> dict:
    entities = state.get("entities") or ExtractedEntities()
    filters: dict = {}
    if entities.budget_max_cop:
        filters["maxPrice"] = entities.budget_max_cop
    if entities.budget_min_cop:
        filters["minPrice"] = entities.budget_min_cop
    if entities.brand:
        filters["brand"] = entities.brand

    # Spring LIKE '%q%' requires a single-word substring.
    # Split compound phrases and take the primary keyword.
    raw_term = entities.category or entities.brand or state["query"]
    search_term = raw_term.split()[0] if raw_term else ""
    filters["q"] = search_term
    filters["page"] = 0
    filters["size"] = 20
    return {"filters": filters}


async def call_products_api_node(state: SearchState) -> dict:
    filters = state.get("filters") or {}
    products = await fetch_products_from_spring(filters)
    # If no products found with the keyword, retry without `q` so Gemini can
    # rank from the full catalog (gift/recommendation queries have no clear keyword).
    if not products and filters.get("q"):
        broader = {k: v for k, v in filters.items() if k != "q"}
        products = await fetch_products_from_spring(broader)
    return {"products": products}


async def generate_explanations_node(state: SearchState) -> dict:
    products = state.get("products") or []
    if not products:
        return {"recommendations": [], "needs_clarification": False, "greeting": None, "closing_message": None}

    model = get_chat_model(temperature=0.4)
    products_summary = [{"product_id": p.get("id"), "name": p.get("name"), "price": p.get("price")} for p in products[:10]]
    prompt = EXPLANATION_PROMPT.format(
        query=state["query"],
        entities=state["entities"].model_dump() if state["entities"] else {},
        products=json.dumps(products_summary, ensure_ascii=False),
    )
    greeting = "¡Hola! Aquí tienes algunas opciones para lo que buscas."
    closing_message = "¿Quieres que busque algo más específico?"
    try:
        response = await model.ainvoke([HumanMessage(content=prompt)])
        parsed = _parse_json_safely(response.content)
        # Algunos modelos (ej. Groq) devuelven el array plano en vez del objeto pedido
        if isinstance(parsed, list):
            explanations_raw: list[dict] = parsed
        else:
            explanations_raw = parsed.get("items", [])
            greeting = parsed.get("greeting") or greeting
            closing_message = parsed.get("closing") or closing_message
    except Exception as exc:
        logger.warning("explanation_generation_failed", error=str(exc))
        explanations_raw = [{"product_id": p.get("id"), "explanation": "Producto relevante para tu búsqueda.", "price_fit": True} for p in products[:5]]

    entities = state.get("entities") or ExtractedEntities()
    recommendations = []
    for item in explanations_raw[:10]:
        product_data = next((p for p in products if str(p.get("id")) == str(item.get("product_id"))), None)
        if not product_data:
            continue
        price = product_data.get("price", 0) or 0
        price_fit = item.get("price_fit")
        if price_fit is None:
            price_fit = True
        if entities.budget_max_cop and price > entities.budget_max_cop:
            price_fit = False
        recommendations.append(ProductRecommendation(
            product_id=str(item["product_id"]),
            relevance_score=0.9 - (recommendations.__len__() * 0.05),
            explanation=item.get("explanation", ""),
            price_fit=price_fit,
        ))

    needs_clarification = len(recommendations) == 0 and state.get("intent") != "search"
    return {
        "recommendations": recommendations,
        "needs_clarification": needs_clarification,
        "greeting": greeting if recommendations else None,
        "closing_message": closing_message if recommendations else None,
    }


async def request_clarification_node(state: SearchState) -> dict:
    model = get_chat_model(temperature=0.5)
    prompt = CLARIFICATION_PROMPT.format(
        query=state["query"],
        entities=state["entities"].model_dump() if state["entities"] else {},
    )
    try:
        response = await model.ainvoke([HumanMessage(content=prompt)])
        question = _extract_text(response.content).strip()
    except Exception:
        question = "¿Podrías darme más detalles sobre lo que buscas?"
    return {"clarification_question": question, "needs_clarification": True}


def should_request_clarification(state: SearchState) -> str:
    already_clarified = bool(state.get("clarification"))
    if state.get("intent") == "clarification_needed" and not already_clarified:
        return "request_clarification"
    if not state.get("products") and not already_clarified:
        return "request_clarification"
    return "generate_explanations"


def _build_graph() -> StateGraph:
    graph = StateGraph(SearchState)
    graph.add_node("analyze_query", analyze_query_node)
    graph.add_node("build_filters", build_filters_node)
    graph.add_node("call_products_api", call_products_api_node)
    graph.add_node("generate_explanations", generate_explanations_node)
    graph.add_node("request_clarification", request_clarification_node)

    graph.set_entry_point("analyze_query")
    graph.add_edge("analyze_query", "build_filters")
    graph.add_edge("build_filters", "call_products_api")
    graph.add_conditional_edges(
        "call_products_api",
        should_request_clarification,
        {"request_clarification": "request_clarification", "generate_explanations": "generate_explanations"},
    )
    graph.add_edge("request_clarification", END)
    graph.add_edge("generate_explanations", END)
    return graph


_compiled_graph = _build_graph().compile()


async def run_search_agent(request: SearchRequest) -> SearchResponse:
    start = time.monotonic()
    initial_state: SearchState = {
        "query": request.query,
        "clarification": request.clarification,
        "user_id": request.user_id,
        "session_id": request.session_id,
        "entities": None,
        "intent": None,
        "filters": None,
        "products": [],
        "recommendations": [],
        "needs_clarification": False,
        "clarification_question": None,
        "greeting": None,
        "closing_message": None,
        "start_time": start,
    }

    logger.info("search_agent_start", query=request.query, user_id=request.user_id)
    final_state = await _compiled_graph.ainvoke(initial_state)
    elapsed_ms = int((time.monotonic() - start) * 1000)
    logger.info("search_agent_done", elapsed_ms=elapsed_ms, results=len(final_state["recommendations"]))

    filters = final_state.get("filters") or {}
    filters.pop("q", None)

    return SearchResponse(
        greeting=final_state.get("greeting"),
        closing_message=final_state.get("closing_message"),
        recommendations=final_state["recommendations"],
        structured_filters=filters,
        needs_clarification=final_state["needs_clarification"],
        clarification_question=final_state.get("clarification_question"),
        intent_classified=final_state.get("intent") or "search",
        processing_time_ms=elapsed_ms,
    )
