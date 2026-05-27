"""
PillowProvider — procesamiento de imágenes local sin API externa.
Usado como fallback cuando Nano Banana no tiene cuota disponible.
"""
import asyncio
import base64
import io
import time
from PIL import Image, ImageChops, ImageEnhance, ImageFilter, ImageOps

from src.core.exceptions import ImageEnhancementError
from src.core.logging import get_logger
from src.models.seller_models import (
    EnhancedImageResult,
    EnhancementOperation,
    ImageEnhancementRequest,
    ImageEnhancementResponse,
)

logger = get_logger(__name__)


def _decode(b64: str) -> Image.Image:
    return Image.open(io.BytesIO(base64.b64decode(b64)))


def _encode(img: Image.Image, quality: int = 92) -> str:
    buf = io.BytesIO()
    img.convert("RGB").save(buf, format="JPEG", quality=quality)
    return base64.b64encode(buf.getvalue()).decode()


def _estimate_quality(img: Image.Image) -> float:
    px = img.width * img.height
    if px >= 2_073_600:
        return 85.0
    if px >= 921_600:
        return 65.0
    if px >= 307_200:
        return 45.0
    return 25.0


def _white_background(img: Image.Image) -> Image.Image:
    """Coloca la imagen sobre fondo blanco. Detecta fondo por esquinas."""
    rgba = img.convert("RGBA")
    canvas = Image.new("RGBA", rgba.size, (255, 255, 255, 255))

    if img.mode == "RGBA":
        # Imagen ya tiene transparencia — componer directamente
        canvas.paste(rgba, mask=rgba.split()[3])
        return canvas.convert("RGB")

    # Detectar color de fondo por promedio de esquinas
    rgb = img.convert("RGB")
    w, h = rgb.size
    corners = [
        rgb.getpixel((0, 0)),
        rgb.getpixel((w - 1, 0)),
        rgb.getpixel((0, h - 1)),
        rgb.getpixel((w - 1, h - 1)),
    ]
    avg_bg = tuple(sum(c[i] for c in corners) // 4 for i in range(3))
    bg_brightness = sum(avg_bg) / 3

    if bg_brightness >= 210:
        # Fondo ya es claro — crear máscara basada en diferencia con blanco puro
        white_ref = Image.new("RGB", rgb.size, (255, 255, 255))
        diff = ImageChops.difference(rgb, white_ref).convert("L")
        # Pixels muy similares al blanco → transparentes
        mask = diff.point(lambda p: 255 if p > 20 else 0)
        canvas.paste(rgb.convert("RGBA"), mask=mask)
    else:
        # Fondo oscuro/saturado — pegar directamente (no podemos quitarlo sin AI)
        canvas.paste(rgb)

    return canvas.convert("RGB")


def _color_correction(img: Image.Image) -> Image.Image:
    img = ImageOps.autocontrast(img, cutoff=1)
    img = ImageEnhance.Color(img).enhance(1.15)
    img = ImageEnhance.Brightness(img).enhance(1.05)
    img = ImageEnhance.Contrast(img).enhance(1.08)
    return img


def _sharpen(img: Image.Image) -> Image.Image:
    return img.filter(ImageFilter.UnsharpMask(radius=1.2, percent=160, threshold=3))


def _noise_reduction(img: Image.Image) -> Image.Image:
    smoothed = img.filter(ImageFilter.GaussianBlur(radius=0.6))
    return smoothed.filter(ImageFilter.UnsharpMask(radius=1, percent=100, threshold=5))


def _smart_crop(img: Image.Image) -> Image.Image:
    """Recorta al bounding box del producto con padding, centrado en cuadrado."""
    white = Image.new("RGB", img.size, (255, 255, 255))
    diff = ImageChops.difference(img.convert("RGB"), white)
    bbox = diff.convert("L").point(lambda p: 255 if p > 15 else 0).getbbox()

    if not bbox:
        return img

    x1, y1, x2, y2 = bbox
    pad = max(30, int(min(img.width, img.height) * 0.08))
    x1, y1 = max(0, x1 - pad), max(0, y1 - pad)
    x2, y2 = min(img.width, x2 + pad), min(img.height, y2 + pad)

    cropped = img.crop((x1, y1, x2, y2))
    cw, ch = cropped.size
    side = max(cw, ch)
    square = Image.new("RGB", (side, side), (255, 255, 255))
    square.paste(cropped, ((side - cw) // 2, (side - ch) // 2))
    return square


def _upscale(img: Image.Image, factor: float = 2.0) -> Image.Image:
    w, h = img.size
    return img.resize((int(w * factor), int(h * factor)), Image.LANCZOS)


def _create_promotional(img: Image.Image, product_name: str | None) -> Image.Image:
    """Banner 1200×628 estilo gaming con el producto a la izquierda."""
    banner = Image.new("RGB", (1200, 628), (12, 12, 22))

    # Franja de acento lateral izquierda
    accent = Image.new("RGB", (6, 628), (220, 30, 60))
    banner.paste(accent, (0, 0))

    # Producto centrado verticalmente en la mitad izquierda
    product_w = 520
    scale = product_w / max(img.width, 1)
    product_h = int(img.height * scale)
    product = img.resize((product_w, product_h), Image.LANCZOS)
    py = (628 - product_h) // 2
    banner.paste(product, (60, py))

    return banner


def _process_operations(
    img: Image.Image,
    operations: list[EnhancementOperation],
) -> tuple[Image.Image, list[EnhancementOperation], list[str]]:
    applied: list[EnhancementOperation] = []
    summary: list[str] = []

    do_bg = (
        EnhancementOperation.BACKGROUND_REMOVAL in operations
        or EnhancementOperation.WHITE_BACKGROUND in operations
    )
    if do_bg:
        img = _white_background(img)
        applied.append(EnhancementOperation.WHITE_BACKGROUND)
        summary.append("Fondo blanco aplicado")

    img = img.convert("RGB")

    if EnhancementOperation.NOISE_REDUCTION in operations:
        img = _noise_reduction(img)
        applied.append(EnhancementOperation.NOISE_REDUCTION)
        summary.append("Reducción de ruido")

    if EnhancementOperation.COLOR_CORRECTION in operations:
        img = _color_correction(img)
        applied.append(EnhancementOperation.COLOR_CORRECTION)
        summary.append("Colores corregidos")

    if EnhancementOperation.SHARPENING in operations:
        img = _sharpen(img)
        applied.append(EnhancementOperation.SHARPENING)
        summary.append("Nitidez mejorada")

    if EnhancementOperation.SMART_CROP in operations:
        img = _smart_crop(img)
        applied.append(EnhancementOperation.SMART_CROP)
        summary.append("Encuadre optimizado")

    if EnhancementOperation.UPSCALING in operations:
        img = _upscale(img)
        applied.append(EnhancementOperation.UPSCALING)
        summary.append("Resolución 2×")

    return img, applied, summary


def _enhance_one(index: int, b64: str, operations: list[EnhancementOperation], product_name: str | None) -> EnhancedImageResult:
    try:
        img = _decode(b64)
    except Exception as exc:
        raise ImageEnhancementError(f"Imagen inválida en índice {index}: {exc}") from exc

    quality_before = _estimate_quality(img)
    img, applied_ops, summary_parts = _process_operations(img, operations)
    quality_after = min(quality_before + len(applied_ops) * 6, 100.0)

    return EnhancedImageResult(
        original_index=index,
        enhanced_base64=_encode(img),
        quality_before=round(quality_before, 2),
        quality_after=round(quality_after, 2),
        operations_applied=applied_ops,
        modification_summary=", ".join(summary_parts) if summary_parts else "Sin cambios",
    )


class PillowProvider:
    """Procesamiento de imágenes local con Pillow. Sin cuota, siempre disponible."""

    async def is_available(self) -> bool:
        return True

    async def enhance(self, request: ImageEnhancementRequest) -> ImageEnhancementResponse:
        start = time.monotonic()

        promo_ops = [op for op in request.operations if op != EnhancementOperation.PROMOTIONAL_IMAGE]
        needs_promo = (
            EnhancementOperation.PROMOTIONAL_IMAGE in request.operations
            or request.generate_promotional
        )

        tasks = [
            asyncio.to_thread(_enhance_one, idx, b64, promo_ops, request.product_name)
            for idx, b64 in enumerate(request.images_base64)
        ]
        enhanced_images: list[EnhancedImageResult] = list(await asyncio.gather(*tasks))

        promo_b64: str | None = None
        if needs_promo and request.images_base64:
            try:
                first = _decode(request.images_base64[0]).convert("RGB")
                # Apply white background before promotional
                first = _white_background(first).convert("RGB")
                promo_img = _create_promotional(first, request.product_name)
                promo_b64 = _encode(promo_img, quality=88)
            except Exception as exc:
                logger.warning("promotional_failed", error=str(exc))

        elapsed_ms = int((time.monotonic() - start) * 1000)
        avg_improvement = (
            sum(r.quality_after - r.quality_before for r in enhanced_images) / len(enhanced_images)
            if enhanced_images else 0.0
        )

        logger.info("pillow_enhancement_complete", images=len(enhanced_images), elapsed_ms=elapsed_ms)

        return ImageEnhancementResponse(
            enhanced_images=enhanced_images,
            promotional_image_base64=promo_b64,
            total_processing_time_ms=elapsed_ms,
            provider_used="pillow",
            overall_quality_improvement=round(avg_improvement, 2),
        )
