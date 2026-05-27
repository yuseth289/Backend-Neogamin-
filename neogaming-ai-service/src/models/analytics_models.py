from datetime import date
from typing import Literal
from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class AnalyticsRequest(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    query: str
    admin_id: str
    date_from: date | None = None
    date_to: date | None = None
    report_type: Literal["adhoc", "daily", "weekly", "monthly"] = "adhoc"


class KPIResult(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    name: str
    value: float | int | str
    unit: str
    period: str
    variation_pct: float | None = None
    trend: Literal["up", "down", "stable"]
    is_alert: bool = False


class ExecutiveSummary(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    title: str
    period: str
    highlights: list[str]
    kpis: list[KPIResult]
    top_products: list[dict]
    alerts: list[str]
    recommendations: list[str]
    chart_data: dict


class AnalyticsResponse(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    narrative: str
    summary: ExecutiveSummary
    query_intent: str
    processing_time_ms: int
