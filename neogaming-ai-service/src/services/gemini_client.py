from google import genai
from langchain_google_genai import ChatGoogleGenerativeAI
from src.core.config import settings
from src.core.logging import get_logger

logger = get_logger(__name__)

_client: genai.Client | None = None


def get_client() -> genai.Client:
    global _client
    if _client is None:
        _client = genai.Client(api_key=settings.gemini_api_key)
    return _client


def configure_gemini() -> None:
    if settings.gemini_api_key:
        get_client()
        logger.info("gemini_configured")
    else:
        logger.warning("gemini_api_key_missing", msg="Running without Gemini — agents will fail")


def get_chat_model(temperature: float = 0.3, model: str | None = None) -> ChatGoogleGenerativeAI:
    model = model or settings.gemini_model
    return ChatGoogleGenerativeAI(
        model=model,
        google_api_key=settings.gemini_api_key,
        temperature=temperature,
        max_retries=1,
    )


def is_gemini_available() -> bool:
    return bool(settings.gemini_api_key)
