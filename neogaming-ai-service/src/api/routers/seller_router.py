import base64
from fastapi import APIRouter, File, Form, UploadFile
from src.agents.seller.seller_agent import run_seller_agent, run_seller_bi_query
from src.core.exceptions import AIServiceError, ImageEnhancementError
from src.models.seller_models import (
    EnhancementOperation,
    ImageEnhancementRequest,
    ImageEnhancementResponse,
    RawProductData,
    SellerAssistRequest,
    SellerAssistResponse,
    SellerBIRequest,
    SellerBIResponse,
)
from src.services.image_enhancement import get_provider

router = APIRouter(prefix="/api/v1/ai/seller", tags=["Seller"])


@router.post("/assist", response_model=SellerAssistResponse)
async def seller_assist(request: SellerAssistRequest) -> SellerAssistResponse:
    try:
        return await run_seller_agent(request)
    except AIServiceError:
        raise
    except Exception as exc:
        raise AIServiceError(f"Seller agent error: {exc}") from exc


@router.post("/bi-query", response_model=SellerBIResponse)
async def seller_bi_query(request: SellerBIRequest) -> SellerBIResponse:
    try:
        return await run_seller_bi_query(request)
    except AIServiceError:
        raise
    except Exception as exc:
        raise AIServiceError(f"Seller BI query error: {exc}") from exc


@router.post("/analyze-image", response_model=SellerAssistResponse)
async def analyze_image(
    seller_id: str = Form(...),
    name: str = Form(...),
    category: str | None = Form(default=None),
    brand: str | None = Form(default=None),
    images: list[UploadFile] = File(default=[]),
) -> SellerAssistResponse:
    images_b64 = []
    for img in images:
        raw = await img.read()
        images_b64.append(base64.b64encode(raw).decode())

    product_data = RawProductData(name=name, category=category, brand=brand, images_base64=images_b64)
    request = SellerAssistRequest(product_data=product_data, seller_id=seller_id)
    try:
        return await run_seller_agent(request)
    except AIServiceError:
        raise
    except Exception as exc:
        raise AIServiceError(f"Image analysis error: {exc}") from exc


@router.post("/enhance-images", response_model=ImageEnhancementResponse)
async def enhance_images(
    product_name: str | None = Form(default=None),
    generate_promotional: bool = Form(default=False),
    operations: list[str] = Form(
        default=["background_removal", "white_background", "color_correction", "sharpening"]
    ),
    images: list[UploadFile] = File(...),
) -> ImageEnhancementResponse:
    if not images:
        raise ImageEnhancementError("At least one image is required")

    images_b64 = []
    for img in images:
        raw = await img.read()
        images_b64.append(base64.b64encode(raw).decode())

    parsed_ops = []
    for op_str in operations:
        try:
            parsed_ops.append(EnhancementOperation(op_str))
        except ValueError:
            pass
    if not parsed_ops:
        parsed_ops = [
            EnhancementOperation.BACKGROUND_REMOVAL,
            EnhancementOperation.WHITE_BACKGROUND,
            EnhancementOperation.COLOR_CORRECTION,
            EnhancementOperation.SHARPENING,
        ]

    request = ImageEnhancementRequest(
        images_base64=images_b64,
        operations=parsed_ops,
        product_name=product_name,
        generate_promotional=generate_promotional,
    )
    provider = get_provider()
    try:
        return await provider.enhance(request)
    except ImageEnhancementError:
        raise
    except Exception as exc:
        raise ImageEnhancementError(str(exc)) from exc
