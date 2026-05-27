from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from src.api.middleware.auth_middleware import InternalTokenMiddleware
from src.api.middleware.logging_middleware import RequestLoggingMiddleware
from src.api.routers import search_router, analytics_router, seller_router
from src.core.config import settings
from src.core.exceptions import AIServiceError, ImageEnhancementError
from src.core.logging import configure_logging, get_logger
from src.models.shared_models import HealthResponse
from src.services.gemini_client import configure_gemini, is_gemini_available

logger = get_logger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    configure_logging()
    configure_gemini()
    logger.info("neogaming_ai_service_started", port=settings.port)
    yield
    logger.info("neogaming_ai_service_stopped")


app = FastAPI(
    title="NeoGaming AI Service",
    version="0.1.0",
    description="LangGraph agents powered by Gemini & Nano Banana for NeoGaming marketplace",
    lifespan=lifespan,
)

app.add_middleware(InternalTokenMiddleware)
app.add_middleware(RequestLoggingMiddleware)

app.include_router(search_router.router)
app.include_router(analytics_router.router)
app.include_router(seller_router.router)


@app.exception_handler(AIServiceError)
async def ai_service_error_handler(request: Request, exc: AIServiceError) -> JSONResponse:
    return JSONResponse(status_code=exc.status_code, content={"detail": str(exc)})


@app.exception_handler(ImageEnhancementError)
async def image_enhancement_error_handler(request: Request, exc: ImageEnhancementError) -> JSONResponse:
    return JSONResponse(status_code=exc.status_code, content={"detail": str(exc)})


@app.get("/api/v1/ai/health", response_model=HealthResponse, tags=["Health"])
async def health_check() -> HealthResponse:
    return HealthResponse(
        status="ok",
        agents=["search", "analytics", "seller"],
        gemini_available=is_gemini_available(),
        nano_banana_available=is_gemini_available(),
    )


@app.get("/", include_in_schema=False)
async def root():
    return {"service": "NeoGaming AI Service", "version": "0.1.0", "docs": "/docs"}
