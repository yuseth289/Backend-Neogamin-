from google import genai
from langchain_core.runnables import Runnable
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_groq import ChatGroq
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


def get_chat_model(temperature: float = 0.3, model: str | None = None) -> Runnable:
    """Modelo de chat con Gemini como primario y Groq/Llama como respaldo.

    Si Gemini falla (ej. 429 por cuota agotada), la cadena reintenta
    automáticamente con Groq en vez de propagar la excepción.
    """
    # max_retries=0: si Gemini falla, caer a Groq de inmediato en vez de
    # reintentar contra el mismo proveedor agotado de cuota — cada llamada
    # de un agente (search, seller, analytics) encadena varios pasos
    # secuenciales, y los reintentos acumulados pueden superar el timeout
    # del proxy y cortar la conexión antes de que el fallback responda.
    primary = ChatGoogleGenerativeAI(
        model=model or settings.gemini_model,
        google_api_key=settings.gemini_api_key,
        temperature=temperature,
        max_retries=0,
    )
    if not settings.groq_api_key:
        return primary

    fallback = ChatGroq(
        model=settings.groq_model,
        api_key=settings.groq_api_key,
        temperature=temperature,
        max_retries=1,
    )
    return primary.with_fallbacks([fallback])


def is_gemini_available() -> bool:
    return bool(settings.gemini_api_key)
