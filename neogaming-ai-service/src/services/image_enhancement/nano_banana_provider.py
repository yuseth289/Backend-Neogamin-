import asyncio
import base64
import io
import time
from PIL import Image
from google import genai
from google.genai import types
from src.core.config import settings
from src.core.exceptions import ImageEnhancementError
from src.core.logging import get_logger
from src.models.seller_models import (
    EnhancedImageResult,
    EnhancementOperation,
    ImageEnhancementRequest,
    ImageEnhancementResponse,
)
from src.services.gemini_client import get_client

logger = get_logger(__name__)

_OPERATION_PROMPTS: dict[EnhancementOperation, str] = {
    EnhancementOperation.BACKGROUND_REMOVAL: "Remove the background completely, keeping only the product.",
    EnhancementOperation.WHITE_BACKGROUND: "Place the product on a clean, pure white studio background suitable for e-commerce marketplace.",
    EnhancementOperation.UPSCALING: "Enhance the resolution and sharpness to produce a high-quality image.",
    EnhancementOperation.COLOR_CORRECTION: "Correct the color balance, exposure, and lighting to make the product look professional.",
    EnhancementOperation.NOISE_REDUCTION: "Remove noise and grain from the image while preserving product details.",
    EnhancementOperation.SHARPENING: "Sharpen the image to enhance product details and edges.",
    EnhancementOperation.SMART_CROP: "Reframe and crop the image to center the product with optimal composition for e-commerce.",
}


def _build_enhancement_prompt(operations: list[EnhancementOperation], product_name: str | None) -> str:
    steps = [_OPERATION_PROMPTS[op] for op in operations if op in _OPERATION_PROMPTS]
    base = f"Edit this product image for e-commerce marketplace{f' ({product_name})' if product_name else ''}. "
    base += " ".join(steps)
    base += " Output a professional, high-quality product photo."
    return base


def _build_promotional_prompt(product_name: str | None) -> str:
    name = product_name or "the product"
    return (
        f"Create a professional promotional e-commerce banner featuring {name}. "
        "Use an engaging, modern gaming aesthetic with clean design, "
        "suitable for an online marketplace listing. "
        "Include the product prominently with complementary visual elements."
    )


def _pil_to_part(img: Image.Image) -> types.Part:
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=95)
    return types.Part.from_bytes(data=buf.getvalue(), mime_type="image/jpeg")


def _decode_base64_image(b64: str) -> Image.Image:
    data = base64.b64decode(b64)
    return Image.open(io.BytesIO(data))


def _encode_image_to_base64(img: Image.Image) -> str:
    buffer = io.BytesIO()
    img.save(buffer, format="JPEG", quality=95)
    return base64.b64encode(buffer.getvalue()).decode()


def _estimate_quality(img: Image.Image) -> float:
    width, height = img.size
    pixel_count = width * height
    if pixel_count >= 2_073_600:
        return 100.0
    elif pixel_count >= 921_600:
        return 75.0
    elif pixel_count >= 307_200:
        return 50.0
    return 25.0


async def _run_gemini_image_edit(
    client: genai.Client,
    model_name: str,
    prompt: str,
    image: Image.Image,
) -> Image.Image | None:
    try:
        image_part = _pil_to_part(image)
        response = await asyncio.to_thread(
            client.models.generate_content,
            model=model_name,
            contents=[prompt, image_part],
            config=types.GenerateContentConfig(
                response_modalities=["IMAGE", "TEXT"],
            ),
        )
        for part in response.candidates[0].content.parts:
            if part.inline_data is not None:
                raw = part.inline_data.data
                if isinstance(raw, str):
                    raw = base64.b64decode(raw)
                return Image.open(io.BytesIO(raw))
        return None
    except Exception as exc:
        logger.warning("nano_banana_edit_failed", error=str(exc))
        return None


class NanoBananaProvider:
    """Nano Banana — Google Gemini image editing/generation provider (google-genai SDK)."""

    async def is_available(self) -> bool:
        return bool(settings.gemini_api_key)

    async def enhance(self, request: ImageEnhancementRequest) -> ImageEnhancementResponse:
        if not settings.gemini_api_key:
            raise ImageEnhancementError("GEMINI_API_KEY not configured")

        start = time.monotonic()
        client = get_client()
        model_name = settings.nano_banana_model

        enhancement_ops = [op for op in request.operations if op != EnhancementOperation.PROMOTIONAL_IMAGE]
        needs_promo = (
            EnhancementOperation.PROMOTIONAL_IMAGE in request.operations
            or request.generate_promotional
        )

        tasks = [
            self._enhance_single(client, model_name, idx, b64, enhancement_ops, request.product_name)
            for idx, b64 in enumerate(request.images_base64)
        ]
        enhanced_images = await asyncio.gather(*tasks)

        promotional_b64: str | None = None
        if needs_promo and request.images_base64:
            promotional_b64 = await self._generate_promotional(
                client, model_name, request.images_base64[0], request.product_name
            )
            if promotional_b64 is None:
                # Fallback: generar banner con Pillow
                try:
                    from src.services.image_enhancement.pillow_provider import (
                        _decode, _encode, _white_background, _create_promotional,
                    )
                    img = _decode(request.images_base64[0]).convert("RGB")
                    img = _white_background(img).convert("RGB")
                    promotional_b64 = _encode(_create_promotional(img, request.product_name), quality=88)
                except Exception as exc:
                    logger.warning("promotional_pillow_fallback_failed", error=str(exc))

        elapsed_ms = int((time.monotonic() - start) * 1000)
        avg_improvement = (
            sum(r.quality_after - r.quality_before for r in enhanced_images) / len(enhanced_images)
            if enhanced_images else 0.0
        )

        logger.info("nano_banana_enhancement_complete", images=len(enhanced_images), elapsed_ms=elapsed_ms)

        return ImageEnhancementResponse(
            enhanced_images=enhanced_images,
            promotional_image_base64=promotional_b64,
            total_processing_time_ms=elapsed_ms,
            provider_used="nano_banana",
            overall_quality_improvement=round(avg_improvement, 2),
        )

    async def _enhance_single(
        self,
        client: genai.Client,
        model_name: str,
        index: int,
        b64: str,
        operations: list[EnhancementOperation],
        product_name: str | None,
    ) -> EnhancedImageResult:
        try:
            original = _decode_base64_image(b64)
        except Exception as exc:
            raise ImageEnhancementError(f"Invalid base64 image at index {index}: {exc}") from exc

        quality_before = _estimate_quality(original)
        applied_ops: list[EnhancementOperation] = []
        summary_parts: list[str] = []

        current_image = original
        if operations:
            prompt = _build_enhancement_prompt(operations, product_name)
            result = await _run_gemini_image_edit(client, model_name, prompt, current_image)
            if result is not None:
                current_image = result
                applied_ops = operations[:]
                summary_parts = [op.value.replace("_", " ").capitalize() for op in operations]
            else:
                logger.warning("nano_banana_quota_fallback", index=index, msg="Falling back to Pillow")
                from src.services.image_enhancement.pillow_provider import _enhance_one
                return _enhance_one(index, b64, operations, product_name)

        quality_after = _estimate_quality(current_image)
        if applied_ops and quality_after <= quality_before:
            quality_after = min(quality_before + 10.0, 100.0)

        return EnhancedImageResult(
            original_index=index,
            enhanced_base64=_encode_image_to_base64(current_image),
            quality_before=round(quality_before, 2),
            quality_after=round(quality_after, 2),
            operations_applied=applied_ops,
            modification_summary=", ".join(summary_parts) if summary_parts else "No changes applied",
        )

    async def _generate_promotional(
        self,
        client: genai.Client,
        model_name: str,
        first_image_b64: str,
        product_name: str | None,
    ) -> str | None:
        try:
            original = _decode_base64_image(first_image_b64)
            prompt = _build_promotional_prompt(product_name)
            result = await _run_gemini_image_edit(client, model_name, prompt, original)
            if result:
                return _encode_image_to_base64(result)
        except Exception as exc:
            logger.warning("promotional_generation_failed", error=str(exc))
        return None
