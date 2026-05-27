class AIServiceError(Exception):
    def __init__(self, message: str, status_code: int = 500):
        super().__init__(message)
        self.status_code = status_code


class AgentTimeoutError(AIServiceError):
    def __init__(self, agent: str):
        super().__init__(f"Agent '{agent}' timed out", status_code=504)


class GeminiUnavailableError(AIServiceError):
    def __init__(self):
        super().__init__("Gemini API unavailable", status_code=503)


class SpringBootUnreachableError(AIServiceError):
    def __init__(self):
        super().__init__("Spring Boot service unreachable", status_code=503)


class ImageEnhancementError(AIServiceError):
    def __init__(self, message: str):
        super().__init__(f"Image enhancement failed: {message}", status_code=422)


class ValidationError(AIServiceError):
    def __init__(self, message: str):
        super().__init__(message, status_code=422)
