import time
import uuid
from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware
import structlog


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        request_id = str(uuid.uuid4())[:8]
        start = time.monotonic()
        structlog.contextvars.clear_contextvars()
        structlog.contextvars.bind_contextvars(request_id=request_id, path=request.url.path, method=request.method)
        logger = structlog.get_logger()
        logger.info("request_start")
        try:
            response = await call_next(request)
            elapsed_ms = int((time.monotonic() - start) * 1000)
            logger.info("request_complete", status=response.status_code, elapsed_ms=elapsed_ms)
            response.headers["X-Request-Id"] = request_id
            return response
        except Exception as exc:
            logger.error("request_error", error=str(exc))
            raise
