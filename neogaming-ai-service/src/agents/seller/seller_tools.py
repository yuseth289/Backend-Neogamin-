import base64
import io
import httpx
from PIL import Image
from src.agents.seller.seller_prompts import CATEGORY_GUIDES
from src.core.config import settings
from src.core.logging import get_logger

logger = get_logger(__name__)
_TIMEOUT = httpx.Timeout(15.0)
_HEADERS = lambda: {"X-Internal-Token": settings.ai_internal_secret}


def decode_base64_to_pil(b64: str) -> Image.Image:
    data = base64.b64decode(b64)
    return Image.open(io.BytesIO(data))


def get_category_guide(category: str | None) -> str:
    if not category:
        return CATEGORY_GUIDES["default"]
    key = category.lower().strip()
    return CATEGORY_GUIDES.get(key, CATEGORY_GUIDES["default"])


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
        logger.warning("seller_tool_error", path=path, error=str(exc))
        return {}


async def fetch_seller_dashboard(seller_id: str) -> dict:
    """Fetch full seller dashboard: revenue, orders, top products, ratings."""
    return await _get(f"/internal/analytics/seller/{seller_id}/dashboard")  # type: ignore[return-value]


async def fetch_seller_top_products(seller_id: str) -> list:
    """Top 5 products by units sold for a specific seller."""
    data = await _get(f"/internal/analytics/seller/{seller_id}/dashboard")
    if isinstance(data, dict):
        return data.get("topProductos", [])
    return []
