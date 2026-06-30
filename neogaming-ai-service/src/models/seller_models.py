from enum import Enum
from typing import Literal
from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class RawProductData(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    name: str
    category: str | None = None
    brand: str | None = None
    model: str | None = None
    price_cop: int | None = None
    raw_description: str | None = None
    features: list[str] = []
    images_base64: list[str] = []


class OptimizedContent(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    seo_title: str
    commercial_description: str
    key_benefits: list[str]
    seo_keywords: list[str]
    tags: list[str]


class ImageAnalysisResult(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    image_index: int
    quality_score: float
    issues: list[str]
    recommendations: list[str]
    background_type: Literal["white", "colored", "transparent", "lifestyle", "other"]
    needs_background_removal: bool
    lighting_quality: Literal["excellent", "good", "poor"]
    sharpness: Literal["sharp", "acceptable", "blurry"]


class ListingQualityScore(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    total_score: float
    content_score: float
    completeness_score: float
    seo_score: float
    image_score: float
    missing_fields: list[str]
    improvement_suggestions: list[str]


class SellerAssistRequest(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    product_data: RawProductData
    seller_id: str
    instruction: str | None = None


class SellerAssistResponse(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    optimized_content: OptimizedContent
    listing_score: ListingQualityScore
    image_analysis: list[ImageAnalysisResult]
    processing_time_ms: int


# ── Seller BI — Business Intelligence queries ───────────────────────────────

class SellerBIRequest(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    query: str
    seller_id: str


class SellerBIKPI(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    name: str
    value: float | int | str
    unit: str
    period: str
    variation_pct: float | None = None
    trend: Literal["up", "down", "stable"]
    is_alert: bool = False


class SellerBIResponse(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    narrative: str
    kpis: list[SellerBIKPI]
    recommendations: list[str]
    processing_time_ms: int


# ── Nano Banana — Image Enhancement ────────────────────────────────────────

class EnhancementOperation(str, Enum):
    BACKGROUND_REMOVAL = "background_removal"
    WHITE_BACKGROUND = "white_background"
    UPSCALING = "upscaling"
    COLOR_CORRECTION = "color_correction"
    NOISE_REDUCTION = "noise_reduction"
    SHARPENING = "sharpening"
    SMART_CROP = "smart_crop"
    PROMOTIONAL_IMAGE = "promotional_image"


class ImageEnhancementRequest(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    images_base64: list[str]
    operations: list[EnhancementOperation] = [
        EnhancementOperation.BACKGROUND_REMOVAL,
        EnhancementOperation.WHITE_BACKGROUND,
        EnhancementOperation.COLOR_CORRECTION,
        EnhancementOperation.SHARPENING,
    ]
    product_name: str | None = None
    generate_promotional: bool = False


class EnhancedImageResult(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    original_index: int
    enhanced_base64: str
    quality_before: float
    quality_after: float
    operations_applied: list[EnhancementOperation]
    modification_summary: str


class ImageEnhancementResponse(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    enhanced_images: list[EnhancedImageResult]
    promotional_image_base64: str | None = None
    total_processing_time_ms: int
    provider_used: str
    overall_quality_improvement: float
