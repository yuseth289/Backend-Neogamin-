from pydantic import BaseModel


class HealthResponse(BaseModel):
    status: str
    agents: list[str]
    gemini_available: bool
    nano_banana_available: bool


class ErrorResponse(BaseModel):
    detail: str
    error_code: str | None = None
