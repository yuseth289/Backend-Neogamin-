from typing import Protocol, runtime_checkable
from src.models.seller_models import ImageEnhancementRequest, ImageEnhancementResponse


@runtime_checkable
class ImageEnhancementProvider(Protocol):
    async def enhance(self, request: ImageEnhancementRequest) -> ImageEnhancementResponse: ...

    async def is_available(self) -> bool: ...
