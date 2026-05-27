from fastapi import Request
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware
from src.core.config import settings

_PUBLIC_PATHS = {"/api/v1/ai/health", "/", "/docs", "/openapi.json", "/redoc"}


class InternalTokenMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        if request.url.path in _PUBLIC_PATHS:
            return await call_next(request)
        token = request.headers.get("X-Internal-Token")
        if not token or token != settings.ai_internal_secret:
            return JSONResponse(status_code=403, content={"detail": "Forbidden: invalid X-Internal-Token"})
        return await call_next(request)
