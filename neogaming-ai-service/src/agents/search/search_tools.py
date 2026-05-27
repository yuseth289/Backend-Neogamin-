import httpx
from src.core.config import settings
from src.core.exceptions import SpringBootUnreachableError
from src.core.logging import get_logger

logger = get_logger(__name__)

_TIMEOUT = httpx.Timeout(15.0)


async def fetch_products_from_spring(filters: dict) -> list[dict]:
    """Call Spring Boot /internal/products/search with structured filters."""
    params = {k: v for k, v in filters.items() if v is not None}
    try:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            resp = await client.get(
                f"{settings.spring_boot_base_url}/internal/products/search",
                params=params,
                headers={"X-Internal-Token": settings.ai_internal_secret},
            )
            resp.raise_for_status()
            return resp.json().get("content", [])
    except httpx.ConnectError as exc:
        raise SpringBootUnreachableError() from exc
    except httpx.HTTPStatusError as exc:
        logger.error("spring_products_search_error", status=exc.response.status_code)
        return []


async def fetch_product_by_id(product_id: str) -> dict | None:
    try:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            resp = await client.get(
                f"{settings.spring_boot_base_url}/internal/products/{product_id}",
                headers={"X-Internal-Token": settings.ai_internal_secret},
            )
            if resp.status_code == 404:
                return None
            resp.raise_for_status()
            return resp.json()
    except (httpx.ConnectError, httpx.HTTPStatusError):
        return None


async def fetch_categories() -> list[dict]:
    try:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            resp = await client.get(
                f"{settings.spring_boot_base_url}/internal/categories",
                headers={"X-Internal-Token": settings.ai_internal_secret},
            )
            resp.raise_for_status()
            return resp.json()
    except (httpx.ConnectError, httpx.HTTPStatusError):
        return []
