"""Mock provider for tests — no Gemini API key required."""
import time
from src.models.seller_models import (
    EnhancedImageResult,
    EnhancementOperation,
    ImageEnhancementRequest,
    ImageEnhancementResponse,
)


class MockProvider:
    async def is_available(self) -> bool:
        return True

    async def enhance(self, request: ImageEnhancementRequest) -> ImageEnhancementResponse:
        start = time.monotonic()
        results = [
            EnhancedImageResult(
                original_index=i,
                enhanced_base64=b64,
                quality_before=60.0,
                quality_after=90.0,
                operations_applied=request.operations,
                modification_summary="Mock: " + ", ".join(op.value for op in request.operations),
            )
            for i, b64 in enumerate(request.images_base64)
        ]
        elapsed_ms = int((time.monotonic() - start) * 1000)
        return ImageEnhancementResponse(
            enhanced_images=results,
            promotional_image_base64=request.images_base64[0] if request.generate_promotional and request.images_base64 else None,
            total_processing_time_ms=elapsed_ms,
            provider_used="mock",
            overall_quality_improvement=30.0,
        )
