from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class SearchRequest(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    query: str
    user_id: str = "anonymous"
    session_id: str = "default"
    context: dict | None = None
    clarification: str | None = None


class ExtractedEntities(BaseModel):
    budget_min_cop: int | None = None
    budget_max_cop: int | None = None
    category: str | None = None
    brand: str | None = None
    use_case: str | None = None
    game_mentioned: str | None = None
    technical_specs: dict[str, str] = {}
    compatibility_with: list[str] = []


class ProductRecommendation(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    product_id: str
    relevance_score: float
    explanation: str
    compatibility_notes: str | None = None
    price_fit: bool


class SearchResponse(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    greeting: str | None = None
    recommendations: list[ProductRecommendation]
    structured_filters: dict
    needs_clarification: bool
    clarification_question: str | None = None
    intent_classified: str
    processing_time_ms: int
