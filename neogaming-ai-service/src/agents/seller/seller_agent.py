import asyncio
import io
import json
import time
from typing import TypedDict

from langgraph.graph import StateGraph, END
from langchain_core.messages import HumanMessage
from google.genai import types

from src.agents.seller.seller_prompts import (
    IMAGE_ANALYSIS_PROMPT,
    CONTENT_OPTIMIZATION_PROMPT,
    LISTING_SCORE_PROMPT,
    SELLER_BI_PROMPT,
)
from src.agents.seller.seller_tools import decode_base64_to_pil, get_category_guide, fetch_seller_dashboard
from src.core.logging import get_logger
from src.models.seller_models import (
    ImageAnalysisResult,
    ListingQualityScore,
    OptimizedContent,
    RawProductData,
    SellerAssistRequest,
    SellerAssistResponse,
    SellerBIRequest,
    SellerBIResponse,
    SellerBIKPI,
)
from src.core.config import settings
from src.services.gemini_client import get_chat_model, get_client

logger = get_logger(__name__)


class SellerState(TypedDict):
    product_data: RawProductData
    seller_id: str
    category_guide: str
    image_analysis: list[ImageAnalysisResult]
    optimized_content: OptimizedContent | None
    listing_score: ListingQualityScore | None


def _extract_text(content: str | list) -> str:
    if isinstance(content, list):
        return "".join(
            part.get("text", "") if isinstance(part, dict) else str(part)
            for part in content
        )
    return content


def _parse_json_safely(text: str | list) -> dict | list:
    cleaned = _extract_text(text).strip()
    if cleaned.startswith("```"):
        lines = cleaned.split("\n")
        cleaned = "\n".join(lines[1:-1]) if len(lines) > 2 else cleaned
    return json.loads(cleaned)


async def _analyze_single_image(index: int, b64: str) -> ImageAnalysisResult:
    try:
        pil_img = decode_base64_to_pil(b64)
        buf = io.BytesIO()
        pil_img.save(buf, format="JPEG", quality=95)
        image_part = types.Part.from_bytes(data=buf.getvalue(), mime_type="image/jpeg")

        prompt = IMAGE_ANALYSIS_PROMPT.format(index=index)
        client = get_client()
        response = await asyncio.to_thread(
            client.models.generate_content,
            model=settings.gemini_model,
            contents=[prompt, image_part],
        )
        data = _parse_json_safely(response.text)
        return ImageAnalysisResult(**data)
    except Exception as exc:
        logger.warning("image_analysis_failed", index=index, error=str(exc))
        return ImageAnalysisResult(
            image_index=index,
            quality_score=50.0,
            issues=["No se pudo analizar la imagen"],
            recommendations=["Sube una imagen de mayor calidad"],
            background_type="other",
            needs_background_removal=True,
            lighting_quality="good",
            sharpness="acceptable",
        )


def fetch_category_guide_node(state: SellerState) -> dict:
    guide = get_category_guide(state["product_data"].category)
    return {"category_guide": guide}


async def analyze_images_node(state: SellerState) -> dict:
    images = state["product_data"].images_base64
    if not images:
        return {"image_analysis": []}
    tasks = [_analyze_single_image(i, b64) for i, b64 in enumerate(images)]
    results = await asyncio.gather(*tasks)
    return {"image_analysis": list(results)}


async def generate_content_node(state: SellerState) -> dict:
    model = get_chat_model(temperature=0.6)
    prompt = CONTENT_OPTIMIZATION_PROMPT.format(
        product_data=json.dumps(state["product_data"].model_dump(), ensure_ascii=False),
        category_guides=state["category_guide"],
    )
    try:
        response = await model.ainvoke([HumanMessage(content=prompt)])
        data = _parse_json_safely(response.content)  # type: ignore[arg-type]
        content = OptimizedContent(**data)
    except Exception as exc:
        logger.warning("content_generation_failed", error=str(exc))
        content = OptimizedContent(
            seo_title=state["product_data"].name,
            commercial_description="Producto gaming de alta calidad para tus partidas.",
            key_benefits=["Alta calidad", "Buen rendimiento"],
            seo_keywords=[state["product_data"].category or "gaming"],
            tags=["gaming", "neogaming"],
        )
    return {"optimized_content": content}


async def calculate_score_node(state: SellerState) -> dict:
    model = get_chat_model(temperature=0.1)
    prompt = LISTING_SCORE_PROMPT.format(
        product_data=json.dumps(state["product_data"].model_dump(), ensure_ascii=False),
        content=json.dumps(state["optimized_content"].model_dump() if state["optimized_content"] else {}, ensure_ascii=False),
        image_analysis=json.dumps([a.model_dump() for a in state["image_analysis"]], ensure_ascii=False),
    )
    try:
        response = await model.ainvoke([HumanMessage(content=prompt)])
        data = _parse_json_safely(response.content)  # type: ignore[arg-type]
        score = ListingQualityScore(**data)
    except Exception as exc:
        logger.warning("score_calculation_failed", error=str(exc))
        score = ListingQualityScore(
            total_score=60.0,
            content_score=60.0,
            completeness_score=60.0,
            seo_score=60.0,
            image_score=60.0,
            missing_fields=[],
            improvement_suggestions=["Agrega más imágenes", "Completa la descripción técnica"],
        )
    return {"listing_score": score}


def _build_graph() -> StateGraph:
    graph = StateGraph(SellerState)
    graph.add_node("fetch_category_guide", fetch_category_guide_node)
    graph.add_node("analyze_images", analyze_images_node)
    graph.add_node("generate_content", generate_content_node)
    graph.add_node("calculate_score", calculate_score_node)

    graph.set_entry_point("fetch_category_guide")
    graph.add_edge("fetch_category_guide", "analyze_images")
    graph.add_edge("fetch_category_guide", "generate_content")
    graph.add_edge("analyze_images", "calculate_score")
    graph.add_edge("generate_content", "calculate_score")
    graph.add_edge("calculate_score", END)
    return graph


_compiled_graph = _build_graph().compile()


# ── Seller BI — conversational business intelligence ─────────────────────────

async def run_seller_bi_query(request: SellerBIRequest) -> SellerBIResponse:
    start = time.monotonic()
    logger.info("seller_bi_query_start", seller_id=request.seller_id, query=request.query[:60])

    dashboard = await fetch_seller_dashboard(request.seller_id)

    model = get_chat_model(temperature=0.4)
    prompt = SELLER_BI_PROMPT.format(
        query=request.query,
        dashboard_data=json.dumps(dashboard, ensure_ascii=False, default=str),
    )

    try:
        response = await model.ainvoke([HumanMessage(content=prompt)])
        data = _parse_json_safely(response.content)  # type: ignore[arg-type]
        kpis = [SellerBIKPI(**k) for k in data.get("kpis", [])]
        narrative = data.get("narrative", "Análisis completado.")
        recommendations = data.get("recommendations", [])
    except Exception as exc:
        logger.warning("seller_bi_fallback", error=str(exc))
        kpis = _kpis_from_dashboard(dashboard)
        narrative = _narrative_from_dashboard(dashboard, request.query)
        recommendations = [
            "Revisa tus productos más vendidos y asegúrate de tener stock suficiente.",
            "Responde rápido a los pedidos pendientes para mejorar tu calificación.",
            "Agrega más imágenes profesionales a tus listados con menor puntaje.",
        ]

    elapsed_ms = int((time.monotonic() - start) * 1000)
    logger.info("seller_bi_query_done", elapsed_ms=elapsed_ms)

    return SellerBIResponse(
        narrative=narrative,
        kpis=kpis,
        recommendations=recommendations,
        processing_time_ms=elapsed_ms,
    )


def _kpis_from_dashboard(d: dict) -> list[SellerBIKPI]:
    kpis: list[SellerBIKPI] = []
    if not d:
        return kpis

    ingr_mes = d.get("ingresosEsteMes", 0)
    ingr_ant = d.get("ingresosMesAnterior", 0)
    var: float | None = None
    if ingr_ant and float(ingr_ant) > 0:
        var = round((float(ingr_mes) - float(ingr_ant)) / float(ingr_ant) * 100, 1)

    kpis.append(SellerBIKPI(
        name="Ingresos este mes",
        value=float(ingr_mes),
        unit="COP",
        period="Este mes",
        variation_pct=var,
        trend="up" if (var or 0) > 0 else ("down" if (var or 0) < 0 else "stable"),
        is_alert=float(ingr_mes) == 0,
    ))
    kpis.append(SellerBIKPI(
        name="Órdenes este mes",
        value=d.get("ordenesEsteMes", 0),
        unit="órdenes",
        period="Este mes",
        variation_pct=None,
        trend="stable",
        is_alert=False,
    ))
    pendientes = d.get("ordenesPendientes", 0)
    kpis.append(SellerBIKPI(
        name="Órdenes pendientes",
        value=pendientes,
        unit="órdenes",
        period="Ahora",
        variation_pct=None,
        trend="stable",
        is_alert=int(pendientes) > 5,
    ))
    rating = d.get("promedioCalificacion", 0)
    kpis.append(SellerBIKPI(
        name="Calificación promedio",
        value=round(float(rating), 1),
        unit="⭐",
        period="Total",
        variation_pct=None,
        trend="up" if float(rating) >= 4.0 else "down",
        is_alert=float(rating) < 3.5,
    ))
    return kpis


def _narrative_from_dashboard(d: dict, query: str) -> str:
    if not d:
        return "No hay datos de ventas disponibles aún. Comienza a publicar productos para ver tus métricas."
    ingr = d.get("ingresosEsteMes", 0)
    ordenes = d.get("ordenesEsteMes", 0)
    unidades = d.get("unidadesVendidasEsteMes", 0)
    rating = d.get("promedioCalificacion", 0)
    return (
        f"Este mes tienes {ordenes} órdenes con ingresos de COP {float(ingr):,.0f} "
        f"y {unidades} unidades vendidas. "
        f"Tu calificación promedio es {rating:.1f}/5.0. "
        f"Aquí está el resumen de tu desempeño actual en NeoGaming."
    )


async def run_seller_agent(request: SellerAssistRequest) -> SellerAssistResponse:
    start = time.monotonic()
    initial_state: SellerState = {
        "product_data": request.product_data,
        "seller_id": request.seller_id,
        "category_guide": "",
        "image_analysis": [],
        "optimized_content": None,
        "listing_score": None,
    }

    logger.info("seller_agent_start", product=request.product_data.name)
    final_state = await _compiled_graph.ainvoke(initial_state)
    elapsed_ms = int((time.monotonic() - start) * 1000)
    logger.info("seller_agent_done", elapsed_ms=elapsed_ms)

    return SellerAssistResponse(
        optimized_content=final_state["optimized_content"],
        listing_score=final_state["listing_score"],
        image_analysis=final_state["image_analysis"],
        processing_time_ms=elapsed_ms,
    )
