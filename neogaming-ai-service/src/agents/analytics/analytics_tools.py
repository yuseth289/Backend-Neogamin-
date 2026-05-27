import httpx
from src.core.config import settings
from src.core.logging import get_logger

logger = get_logger(__name__)
_TIMEOUT = httpx.Timeout(15.0)
_HEADERS = lambda: {"X-Internal-Token": settings.ai_internal_secret}


async def _get(path: str, params: dict | None = None) -> dict | list:
    try:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            resp = await client.get(
                f"{settings.spring_boot_base_url}{path}",
                headers=_HEADERS(),
                params={k: v for k, v in (params or {}).items() if v is not None},
            )
            resp.raise_for_status()
            return resp.json()
    except Exception as exc:
        logger.warning("analytics_tool_error", path=path, error=str(exc))
        return {}


# ── Existing admin dashboard endpoints ───────────────────────────────────────

async def fetch_sales_data(date_from: str | None, date_to: str | None) -> dict:
    return await _get("/internal/analytics/sales", {"from": date_from, "to": date_to})  # type: ignore[return-value]


async def fetch_inventory_data() -> dict:
    return await _get("/internal/analytics/inventory")  # type: ignore[return-value]


async def fetch_users_data() -> dict:
    return await _get("/internal/analytics/users")  # type: ignore[return-value]


# ── Marketplace BI — new endpoints ────────────────────────────────────────────

async def fetch_top_sellers(limit: int = 10) -> list:
    """Top stores ranked by total revenue."""
    data = await _get("/internal/analytics/marketplace/top-sellers", {"limit": limit})
    return data if isinstance(data, list) else []


async def fetch_trending_products(days: int = 30, limit: int = 10) -> list:
    """Most sold products in the last N days."""
    data = await _get("/internal/analytics/marketplace/trending", {"days": days, "limit": limit})
    return data if isinstance(data, list) else []


async def fetch_category_breakdown() -> list:
    """Revenue and volume breakdown by product category."""
    data = await _get("/internal/analytics/marketplace/categories")
    return data if isinstance(data, list) else []
