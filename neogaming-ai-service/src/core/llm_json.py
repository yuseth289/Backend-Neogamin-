import json


def extract_text(content: str | list) -> str:
    if isinstance(content, list):
        return "".join(
            part.get("text", "") if isinstance(part, dict) else str(part)
            for part in content
        )
    return content


def parse_json_safely(text: str | list) -> dict | list:
    """Extrae y parsea el JSON de una respuesta LLM.

    Modelos como Groq/Llama suelen anteponer o agregar texto alrededor del
    JSON (saludos, explicaciones) incluso cuando el prompt pide "solo JSON",
    a diferencia de Gemini que casi siempre cumple. En vez de asumir que la
    respuesta completa es JSON valido, se busca el primer bloque {...} o
    [...] balanceado (respetando comillas) y se parsea solo eso.
    """
    cleaned = extract_text(text).strip()
    if cleaned.startswith("```"):
        cleaned = cleaned.strip("`")
        if cleaned[:4].lower() == "json":
            cleaned = cleaned[4:]
        cleaned = cleaned.strip()

    start_candidates = [i for i in (cleaned.find("{"), cleaned.find("[")) if i != -1]
    if not start_candidates:
        return json.loads(cleaned)

    start = min(start_candidates)
    opening = cleaned[start]
    closing = "}" if opening == "{" else "]"

    depth = 0
    in_string = False
    escape = False
    end = len(cleaned)
    for i in range(start, len(cleaned)):
        ch = cleaned[i]
        if in_string:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
        elif ch == opening:
            depth += 1
        elif ch == closing:
            depth -= 1
            if depth == 0:
                end = i + 1
                break

    return json.loads(cleaned[start:end])
