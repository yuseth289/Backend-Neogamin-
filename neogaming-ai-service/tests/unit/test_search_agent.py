"""Unit tests for SearchAgent — all Gemini calls are mocked."""
import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from src.models.search_models import SearchRequest, SearchResponse, ProductRecommendation, ExtractedEntities


@pytest.fixture
def search_request() -> SearchRequest:
    return SearchRequest(
        query="mouse gamer para FPS con menos de 150 mil",
        user_id="user-123",
        session_id="session-abc",
    )


@pytest.fixture
def mock_product_list() -> list[dict]:
    return [
        {
            "id": "prod-1",
            "name": "Mouse Logitech G502 X",
            "price": 145000,
            "priceFormatted": "$145.000",
            "stock": 10,
            "category": "Periféricos",
        },
        {
            "id": "prod-2",
            "name": "Mouse Razer DeathAdder V3",
            "price": 138000,
            "priceFormatted": "$138.000",
            "stock": 5,
            "category": "Periféricos",
        },
    ]


class TestSearchAgentModels:
    def test_search_request_camel_case_alias(self):
        data = {"userId": "u1", "sessionId": "s1", "query": "mouse gaming"}
        req = SearchRequest.model_validate(data)
        assert req.user_id == "u1"
        assert req.session_id == "s1"

    def test_search_response_serializes_to_camel(self):
        rec = ProductRecommendation(
            product_id="p1",
            product_name="Mouse X",
            relevance_score=0.92,
            explanation="Excelente opción para FPS",
            price=145000,
            price_formatted="$145.000",
            stock_available=True,
        )
        resp = SearchResponse(
            recommendations=[rec],
            needs_clarification=False,
            clarification_question=None,
            intent_classified="product_search",
            processing_time_ms=1200,
        )
        dumped = resp.model_dump(by_alias=True)
        assert "needsClarification" in dumped
        assert "clarificationQuestion" in dumped
        assert "intentClassified" in dumped
        assert "processingTimeMs" in dumped
        assert dumped["recommendations"][0]["productId"] == "p1"

    def test_extracted_entities_budget_parsing(self):
        entities = ExtractedEntities(
            budget_min_cop=None,
            budget_max_cop=150000,
            category="Periféricos",
            brand=None,
            use_case="FPS gaming",
            game_mentioned="Fortnite",
            technical_specs=[],
            compatibility_with=None,
        )
        assert entities.budget_max_cop == 150000
        assert entities.use_case == "FPS gaming"


class TestSearchAgentNeedsClarification:
    def test_vague_query_should_need_clarification(self):
        """A single-word query like 'mouse' is ambiguous and should trigger clarification."""
        from src.agents.search.search_tools import needs_clarification
        assert needs_clarification("mouse") is True

    def test_specific_query_should_not_need_clarification(self):
        from src.agents.search.search_tools import needs_clarification
        assert needs_clarification(
            "mouse gamer para FPS logitech con presupuesto 150 mil"
        ) is False


@pytest.mark.asyncio
class TestSearchAgentFlow:
    @patch("src.agents.search.search_agent.httpx.AsyncClient")
    @patch("src.agents.search.search_agent.get_chat_model")
    async def test_run_search_agent_returns_recommendations(
        self, mock_get_model, mock_httpx_class, search_request, mock_product_list
    ):
        # Mock Gemini structured output
        mock_model = MagicMock()
        mock_model.with_structured_output.return_value.ainvoke = AsyncMock(
            return_value=ExtractedEntities(
                budget_max_cop=150000,
                category="Periféricos",
                use_case="FPS gaming",
                budget_min_cop=None,
                brand=None,
                game_mentioned=None,
                technical_specs=[],
                compatibility_with=None,
            )
        )
        mock_model.ainvoke = AsyncMock(return_value=MagicMock(content="Estas opciones son ideales."))
        mock_get_model.return_value = mock_model

        # Mock httpx call to /internal/products/search
        mock_client = AsyncMock()
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=False)
        mock_client.get = AsyncMock(
            return_value=MagicMock(
                status_code=200,
                json=MagicMock(return_value={"data": {"content": mock_product_list}}),
            )
        )
        mock_httpx_class.return_value = mock_client

        from src.agents.search.search_agent import run_search_agent
        result: SearchResponse = await run_search_agent(search_request)

        assert isinstance(result, SearchResponse)
        assert result.intent_classified is not None
        assert result.processing_time_ms >= 0
