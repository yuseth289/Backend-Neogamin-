IMAGE_ANALYSIS_PROMPT = """
You are an e-commerce product photography expert analyzing images for a Colombian gaming marketplace.

Analyze image #{index} and return a JSON object:
{{
  "image_index": {index},
  "quality_score": <float 0-100>,
  "issues": ["list of quality issues found"],
  "recommendations": ["actionable improvements in Spanish"],
  "background_type": "white" | "colored" | "transparent" | "lifestyle" | "other",
  "needs_background_removal": true | false,
  "lighting_quality": "excellent" | "good" | "poor",
  "sharpness": "sharp" | "acceptable" | "blurry"
}}

Return only valid JSON.
"""

CONTENT_OPTIMIZATION_PROMPT = """
You are an expert copywriter for a Colombian gaming marketplace (NeoGaming).
Create optimized product listing content in Spanish.

Product data:
{product_data}

Category guides: {category_guides}
{instruction_block}
Return JSON:
{{
  "seo_title": "<max 80 chars, includes brand+model+key feature>",
  "commercial_description": "<200-400 words, gamer tone, highlights benefits, mention COP pricing if available>",
  "key_benefits": ["<5-8 short benefit bullets in Spanish>"],
  "seo_keywords": ["<5-8 relevant search terms>"],
  "tags": ["<10-15 tags for marketplace filtering>"]
}}

Rules:
- Title: brand + model + key differentiator
- Description: conversational, gamer-friendly, Colombian Spanish
- No invented specs — only based on provided data
- If the seller gave a specific request above, prioritize it over the general rules
"""

LISTING_SCORE_PROMPT = """
Score this gaming marketplace product listing from 0-100 on each dimension.

Product data: {product_data}
Optimized content: {content}
Image analysis: {image_analysis}

Return JSON:
{{
  "total_score": <float 0-100>,
  "content_score": <float>,
  "completeness_score": <float>,
  "seo_score": <float>,
  "image_score": <float>,
  "missing_fields": ["list of missing important fields"],
  "improvement_suggestions": ["top 3 actionable improvements in Spanish"]
}}
"""

SELLER_BI_PROMPT = """
You are a business intelligence advisor for a Colombian gaming marketplace seller.
The seller asked: "{query}"

Their real-time sales data:
{dashboard_data}

Answer their question with specific numbers in COP, concrete insights, and actionable advice.
Be conversational, direct, and encouraging. Use Colombian Spanish.

Then return a JSON object with this exact structure:
{{
  "narrative": "<3-5 sentences answering the question with specific numbers and insights, ending with one short friendly question inviting the seller to ask for more detail>",
  "kpis": [
    {{
      "name": "<KPI name in Spanish>",
      "value": <number or string>,
      "unit": "COP" | "%" | "unidades" | "órdenes" | "⭐",
      "period": "Este mes" | "Mes anterior" | "Total" | "Últimos 30 días",
      "variation_pct": <float comparing to previous period, or null>,
      "trend": "up" | "down" | "stable",
      "is_alert": <true if needs attention>
    }}
  ],
  "recommendations": ["<3-4 actionable recommendations in Spanish>"]
}}

Base KPIs on the actual data. Include at minimum: revenue this month, orders, units sold, and top product.
If data shows growth, highlight it. If there are issues (pending orders, low rating), flag as alerts.
Return ONLY valid JSON.
"""

CATEGORY_GUIDES: dict[str, str] = {
    "mouse": "Highlight DPI, sensor type, weight, polling rate, cable vs wireless. Gaming mice need to emphasize precision and response time.",
    "teclado": "Highlight switch type (mecánico/membrana), layout (TKL/full), backlighting, anti-ghosting. Mechanical keyboards need switch details.",
    "headset": "Highlight driver size, frequency response, mic quality, surround sound. Gaming headsets need comfort and audio clarity details.",
    "gpu": "Highlight VRAM, CUDA cores / stream processors, TDP, supported games. Gaming GPUs need to specify performance benchmarks.",
    "monitor": "Highlight refresh rate, response time, panel type (IPS/VA/TN), resolution, adaptive sync. Gaming monitors need blur and tear details.",
    "default": "Highlight technical specs, compatibility, brand reputation, warranty. Include relevant gaming use cases.",
}
