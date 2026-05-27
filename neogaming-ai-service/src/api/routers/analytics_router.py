from fastapi import APIRouter
from src.agents.analytics.analytics_agent import run_analytics_agent
from src.core.exceptions import AIServiceError
from src.models.analytics_models import AnalyticsRequest, AnalyticsResponse

router = APIRouter(prefix="/api/v1/ai/analytics", tags=["Analytics"])


@router.post("", response_model=AnalyticsResponse)
async def analytics_query(request: AnalyticsRequest) -> AnalyticsResponse:
    try:
        return await run_analytics_agent(request)
    except AIServiceError:
        raise
    except Exception as exc:
        raise AIServiceError(f"Analytics agent error: {exc}") from exc
