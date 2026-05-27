from src.core.config import settings
from src.services.image_enhancement.provider import ImageEnhancementProvider


def get_provider() -> ImageEnhancementProvider:
    name = settings.image_enhancement_provider
    if name == "mock":
        from src.services.image_enhancement.mock_provider import MockProvider
        return MockProvider()  # type: ignore[return-value]
    if name == "pillow":
        from src.services.image_enhancement.pillow_provider import PillowProvider
        return PillowProvider()  # type: ignore[return-value]
    # nano_banana — con fallback automático a pillow cuando no hay cuota
    from src.services.image_enhancement.nano_banana_provider import NanoBananaProvider
    return NanoBananaProvider()  # type: ignore[return-value]
