import structlog
from fastapi import APIRouter, Request
from src.agents.search.search_agent import run_search_agent
from src.core.exceptions import AIServiceError
from src.models.search_models import SearchRequest, SearchResponse

router = APIRouter(prefix="/api/v1/ai/search", tags=["Search"])
logger = structlog.get_logger()


@router.post("", response_model=SearchResponse)
async def intelligent_search(raw_request: Request) -> SearchResponse:
    body_bytes = await raw_request.body()
    body_str = body_bytes.decode("utf-8", errors="replace")
    try:
        request = SearchRequest.model_validate_json(body_str)
        return await run_search_agent(request)
    except AIServiceError:
        raise
    except Exception as exc:
        logger.error("search_failed", error=str(exc))
        raise AIServiceError(f"Search agent error: {exc}") from exc
