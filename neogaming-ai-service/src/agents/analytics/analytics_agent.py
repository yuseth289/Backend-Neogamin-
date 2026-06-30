import json
import time
from typing import TypedDict

from langgraph.graph import StateGraph, END
from langchain_core.messages import HumanMessage

from src.agents.analytics.analytics_prompts import (
    INTENT_CLASSIFICATION_PROMPT,
    NARRATIVE_PROMPT,
    KPI_EXTRACTION_PROMPT,
    RECOMMENDATIONS_PROMPT,
    TOP_SELLERS_NARRATIVE_PROMPT,
    TRENDING_NARRATIVE_PROMPT,
    CATEGORIES_NARRATIVE_PROMPT,
)
from src.agents.analytics.analytics_tools import (
    fetch_sales_data,
    fetch_inventory_data,
    fetch_users_data,
    fetch_top_sellers,
    fetch_trending_products,
    fetch_category_breakdown,
)
from src.core.logging import get_logger
from src.models.analytics_models import (
    AnalyticsRequest,
    AnalyticsResponse,
    ExecutiveSummary,
    KPIResult,
)
from src.core.llm_json import parse_json_safely as _parse_json_safely
from src.services.gemini_client import get_chat_model

logger = get_logger(__name__)

_BI_INTENTS = {"top_sellers", "trending", "categories"}

_NARRATIVE_PROMPTS = {
    "top_sellers": TOP_SELLERS_NARRATIVE_PROMPT,
    "trending": TRENDING_NARRATIVE_PROMPT,
    "categories": CATEGORIES_NARRATIVE_PROMPT,
}


class AnalyticsState(TypedDict):
    query: str
    admin_id: str
    date_from: str | None
    date_to: str | None
    report_type: str
    intent: str | None
    raw_data: dict
    narrative: str | None
    kpis: list[KPIResult]
    alerts: list[str]
    recommendations: list[str]


async def classify_intent_node(state: AnalyticsState) -> dict:
    model = get_chat_model(temperature=0.0)
    prompt = INTENT_CLASSIFICATION_PROMPT.format(query=state["query"])
    valid = {"sales", "inventory", "users", "top_sellers", "trending", "categories", "report"}
    try:
        response = await model.ainvoke([HumanMessage(content=prompt)])
        intent = response.content.strip().lower()
        if intent not in valid:
            intent = "report"
    except Exception:
        intent = "report"
    return {"intent": intent}


async def fetch_data_node(state: AnalyticsState) -> dict:
    intent = state.get("intent", "report")
    raw: dict = {}

    if intent == "top_sellers":
        raw["top_sellers"] = await fetch_top_sellers(limit=15)
    elif intent == "trending":
        raw["trending"] = await fetch_trending_products(days=30, limit=15)
    elif intent == "categories":
        raw["categories"] = await fetch_category_breakdown()
    else:
        # sales / inventory / users / report — fetch relevant standard data
        if intent in {"sales", "report"}:
            raw["sales"] = await fetch_sales_data(state.get("date_from"), state.get("date_to"))
        if intent in {"inventory", "report"}:
            raw["inventory"] = await fetch_inventory_data()
        if intent in {"users", "report"}:
            raw["users"] = await fetch_users_data()
        # Always pull top sellers + trending for full reports
        if intent == "report":
            raw["top_sellers"] = await fetch_top_sellers(limit=10)
            raw["trending"] = await fetch_trending_products(days=30, limit=10)
            raw["categories"] = await fetch_category_breakdown()

    return {"raw_data": raw}


async def generate_narrative_node(state: AnalyticsState) -> dict:
    model = get_chat_model(temperature=0.5)
    intent = state.get("intent", "report")
    data_str = json.dumps(state.get("raw_data", {}), ensure_ascii=False, default=str)

    # Use specialized prompt for BI intents
    if intent in _NARRATIVE_PROMPTS:
        prompt = _NARRATIVE_PROMPTS[intent].format(
            query=state["query"],
            data=data_str,
        )
    else:
        prompt = NARRATIVE_PROMPT.format(
            query=state["query"],
            data=data_str,
        )

    try:
        response = await model.ainvoke([HumanMessage(content=prompt)])
        narrative = response.content.strip()
    except Exception:
        narrative = "Análisis de datos completado. Revisa los KPIs a continuación."
    return {"narrative": narrative}


async def extract_kpis_node(state: AnalyticsState) -> dict:
    model = get_chat_model(temperature=0.1)
    data_str = json.dumps(state.get("raw_data", {}), ensure_ascii=False, default=str)
    prompt = KPI_EXTRACTION_PROMPT.format(data=data_str)
    try:
        response = await model.ainvoke([HumanMessage(content=prompt)])
        kpis_raw: list[dict] = _parse_json_safely(response.content)  # type: ignore[assignment]
        kpis = [KPIResult(**k) for k in kpis_raw]
        alerts = [k.name for k in kpis if k.is_alert]
    except Exception as exc:
        logger.warning("kpi_extraction_failed", error=str(exc))
        kpis = _fallback_kpis(state.get("raw_data", {}))
        alerts = [k.name for k in kpis if k.is_alert]
    return {"kpis": kpis, "alerts": alerts}


async def generate_recommendations_node(state: AnalyticsState) -> dict:
    model = get_chat_model(temperature=0.6)
    data_str = json.dumps(state.get("raw_data", {}), ensure_ascii=False, default=str)
    prompt = RECOMMENDATIONS_PROMPT.format(data=data_str)
    try:
        response = await model.ainvoke([HumanMessage(content=prompt)])
        recs: list[str] = _parse_json_safely(response.content)  # type: ignore[assignment]
    except Exception:
        recs = ["Revisar métricas de ventas para identificar oportunidades de crecimiento."]
    return {"recommendations": recs}


def _fallback_kpis(raw: dict) -> list[KPIResult]:
    kpis: list[KPIResult] = []
    sales = raw.get("sales", {})
    if isinstance(sales, dict):
        if "ingresosEsteMes" in sales:
            kpis.append(KPIResult(
                name="Ingresos este mes", value=float(sales["ingresosEsteMes"]),
                unit="COP", period="Este mes", variation_pct=None, trend="stable",
            ))
        if "ordenesEsteMes" in sales:
            kpis.append(KPIResult(
                name="Órdenes este mes", value=int(sales["ordenesEsteMes"]),
                unit="órdenes", period="Este mes", variation_pct=None, trend="stable",
            ))
    top_sellers = raw.get("top_sellers", [])
    if isinstance(top_sellers, list) and top_sellers:
        kpis.append(KPIResult(
            name="Top tienda", value=top_sellers[0].get("storeName", "N/A"),
            unit="tienda", period="Total", variation_pct=None, trend="up",
        ))
    return kpis


def _build_graph() -> StateGraph:
    graph = StateGraph(AnalyticsState)
    graph.add_node("classify_intent", classify_intent_node)
    graph.add_node("fetch_data", fetch_data_node)
    graph.add_node("generate_narrative", generate_narrative_node)
    graph.add_node("extract_kpis", extract_kpis_node)
    graph.add_node("generate_recommendations", generate_recommendations_node)

    graph.set_entry_point("classify_intent")
    graph.add_edge("classify_intent", "fetch_data")
    graph.add_edge("fetch_data", "generate_narrative")
    graph.add_edge("fetch_data", "extract_kpis")
    graph.add_edge("fetch_data", "generate_recommendations")
    graph.add_edge("generate_narrative", END)
    graph.add_edge("extract_kpis", END)
    graph.add_edge("generate_recommendations", END)
    return graph


_compiled_graph = _build_graph().compile()


async def run_analytics_agent(request: AnalyticsRequest) -> AnalyticsResponse:
    start = time.monotonic()
    initial_state: AnalyticsState = {
        "query": request.query,
        "admin_id": request.admin_id,
        "date_from": request.date_from.isoformat() if request.date_from else None,
        "date_to": request.date_to.isoformat() if request.date_to else None,
        "report_type": request.report_type,
        "intent": None,
        "raw_data": {},
        "narrative": None,
        "kpis": [],
        "alerts": [],
        "recommendations": [],
    }

    logger.info("analytics_agent_start", query=request.query)
    final_state = await _compiled_graph.ainvoke(initial_state)
    elapsed_ms = int((time.monotonic() - start) * 1000)
    logger.info("analytics_agent_done", elapsed_ms=elapsed_ms)

    summary = ExecutiveSummary(
        title=f"Análisis: {request.query}",
        period=f"{request.date_from or 'Inicio'} — {request.date_to or 'Hoy'}",
        highlights=[final_state.get("narrative") or ""],
        kpis=final_state.get("kpis") or [],
        top_products=final_state.get("raw_data", {}).get("trending", []),
        alerts=final_state.get("alerts") or [],
        recommendations=final_state.get("recommendations") or [],
        chart_data=final_state.get("raw_data") or {},
    )

    return AnalyticsResponse(
        narrative=final_state.get("narrative") or "",
        summary=summary,
        query_intent=final_state.get("intent") or "report",
        processing_time_ms=elapsed_ms,
    )
