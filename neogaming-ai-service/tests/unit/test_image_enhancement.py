"""Unit tests for ImageEnhancement — uses MockProvider (no GEMINI_API_KEY required)."""
import pytest
import base64
from unittest.mock import patch
from src.models.seller_models import (
    ImageEnhancementRequest,
    EnhancementOperation,
    ImageEnhancementResponse,
)
from src.services.image_enhancement.mock_provider import MockProvider

TINY_PNG_B64 = (
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDQABGQGB0I1CAAAAABJRU5ErkJggg=="
)


@pytest.fixture
def single_image_request() -> ImageEnhancementRequest:
    return ImageEnhancementRequest(
        images_base64=[TINY_PNG_B64],
        operations=[
            EnhancementOperation.BACKGROUND_REMOVAL,
            EnhancementOperation.WHITE_BACKGROUND,
        ],
        product_name="Mouse gamer X",
        generate_promotional=False,
    )


@pytest.fixture
def multi_image_request() -> ImageEnhancementRequest:
    return ImageEnhancementRequest(
        images_base64=[TINY_PNG_B64, TINY_PNG_B64],
        operations=[EnhancementOperation.COLOR_CORRECTION, EnhancementOperation.SHARPENING],
        generate_promotional=True,
    )


class TestMockProvider:
    @pytest.mark.asyncio
    async def test_enhance_returns_correct_image_count(self, single_image_request):
        provider = MockProvider()
        result: ImageEnhancementResponse = await provider.enhance(single_image_request)
        assert len(result.enhanced_images) == 1

    @pytest.mark.asyncio
    async def test_enhance_multi_image(self, multi_image_request):
        provider = MockProvider()
        result: ImageEnhancementResponse = await provider.enhance(multi_image_request)
        assert len(result.enhanced_images) == 2

    @pytest.mark.asyncio
    async def test_enhanced_images_have_valid_base64(self, single_image_request):
        provider = MockProvider()
        result = await provider.enhance(single_image_request)
        for img in result.enhanced_images:
            decoded = base64.b64decode(img.enhanced_base64)
            assert len(decoded) > 0

    @pytest.mark.asyncio
    async def test_quality_scores_in_range(self, single_image_request):
        provider = MockProvider()
        result = await provider.enhance(single_image_request)
        for img in result.enhanced_images:
            assert 0 <= img.quality_before <= 100
            assert 0 <= img.quality_after <= 100

    @pytest.mark.asyncio
    async def test_operations_applied_match_request(self, single_image_request):
        provider = MockProvider()
        result = await provider.enhance(single_image_request)
        for img in result.enhanced_images:
            assert EnhancementOperation.BACKGROUND_REMOVAL in img.operations_applied
            assert EnhancementOperation.WHITE_BACKGROUND in img.operations_applied

    @pytest.mark.asyncio
    async def test_promotional_image_when_requested(self, multi_image_request):
        provider = MockProvider()
        result = await provider.enhance(multi_image_request)
        assert result.promotional_image_base64 is not None
        assert len(result.promotional_image_base64) > 0

    @pytest.mark.asyncio
    async def test_no_promotional_when_not_requested(self, single_image_request):
        provider = MockProvider()
        result = await provider.enhance(single_image_request)
        assert result.promotional_image_base64 is None

    @pytest.mark.asyncio
    async def test_provider_used_is_mock(self, single_image_request):
        provider = MockProvider()
        result = await provider.enhance(single_image_request)
        assert result.provider_used == "mock"

    @pytest.mark.asyncio
    async def test_is_available(self):
        provider = MockProvider()
        assert await provider.is_available() is True


class TestProviderFactory:
    def test_get_provider_returns_mock_when_configured(self):
        with patch("src.services.image_enhancement.settings") as mock_settings:
            mock_settings.image_enhancement_provider = "mock"
            from src.services.image_enhancement import get_provider
            from src.services.image_enhancement.mock_provider import MockProvider
            provider = get_provider("mock")
            assert isinstance(provider, MockProvider)


class TestEnhancementOperationEnum:
    def test_all_operations_have_string_values(self):
        for op in EnhancementOperation:
            assert isinstance(op.value, str)

    def test_camel_alias_in_serialization(self):
        req = ImageEnhancementRequest(
            images_base64=[TINY_PNG_B64],
            operations=[EnhancementOperation.BACKGROUND_REMOVAL],
        )
        dumped = req.model_dump(by_alias=True)
        assert "imagesBase64" in dumped
        assert "generatePromotional" in dumped
