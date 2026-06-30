INTENT_CLASSIFICATION_PROMPT = """
Classify this admin analytics query for a Colombian gaming marketplace.

Query: {query}

Return exactly one of:
- "sales"      → revenue, income, payments, billing
- "inventory"  → products, stock, catalog, pending approval
- "users"      → users, sellers, registrations, accounts
- "top_sellers" → which stores, best sellers, top vendors, ranking of stores
- "trending"   → trending products, most sold, hot items, popular this week/month
- "categories" → categories, which category dominates, revenue by category, genre
- "report"     → comprehensive report, everything, overview, full analysis

Respond with just the intent word.
"""

NARRATIVE_PROMPT = """
You are a business intelligence expert for NeoGaming, a Colombian gaming marketplace.

The admin asked: "{query}"

Analytics data:
{data}

Write a clear, executive-level narrative insight in Spanish (3-5 sentences).
Focus on the most important trends, anomalies, and actionable recommendations.
Use specific numbers. Mention COP currency where relevant.
Be direct and strategic — this is for the marketplace owner.
End with one short, friendly question inviting the admin to ask for more detail or another report.
"""

KPI_EXTRACTION_PROMPT = """
Extract KPIs from this analytics data for a Colombian gaming marketplace dashboard.
Data: {data}

Return a JSON array of KPI objects:
[{{
  "name": "KPI name in Spanish",
  "value": <number or string>,
  "unit": "COP" | "%" | "unidades" | "usuarios" | "tiendas" | "órdenes",
  "period": "Este mes" | "Hoy" | "Total" | "Últimos 30 días",
  "variation_pct": <float or null>,
  "trend": "up" | "down" | "stable",
  "is_alert": <true if metric needs immediate attention>
}}]

Include at least 4 KPIs based on the data. Flag alerts if: revenue down >10%, orders declining, or anomalies.
Return only the JSON array.
"""

RECOMMENDATIONS_PROMPT = """
Based on this analytics data from a Colombian gaming marketplace, provide 3-5 actionable recommendations in Spanish.
Focus on: revenue growth, inventory optimization, seller acquisition, user retention, category expansion.
Be specific — reference actual numbers from the data if present.

Data: {data}

Return a JSON array of recommendation strings. Each recommendation should start with a verb.
"""

TOP_SELLERS_NARRATIVE_PROMPT = """
You are a marketplace analytics expert for NeoGaming (Colombia).

The admin asked: "{query}"

Top sellers data (ranked by revenue):
{data}

Write an executive insight in Spanish (3-5 sentences) analyzing:
- Who are the top performers and their revenue
- Revenue concentration (is one seller dominating?)
- Growth opportunities and recommendations
Use specific numbers and store names from the data.
End with one short, friendly question inviting the admin to ask for more detail or another report.
"""

TRENDING_NARRATIVE_PROMPT = """
You are a product trend analyst for NeoGaming, a Colombian gaming marketplace.

The admin asked: "{query}"

Trending products data (most sold recently):
{data}

Write an executive insight in Spanish (3-5 sentences) covering:
- Which products are trending and why
- Revenue impact of trending items
- Inventory and supply recommendations
- Cross-selling opportunities
End with one short, friendly question inviting the admin to ask for more detail or another report.
"""

CATEGORIES_NARRATIVE_PROMPT = """
You are a category strategy expert for NeoGaming, a Colombian gaming marketplace.

The admin asked: "{query}"

Revenue by category:
{data}

Write an executive insight in Spanish (3-5 sentences) covering:
- Which categories generate most revenue
- Underperforming categories with growth potential
- Strategic recommendations for category mix
- Seasonality or trend observations
End with one short, friendly question inviting the admin to ask for more detail or another report.
"""
