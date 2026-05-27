from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    gemini_api_key: str = ""
    ai_internal_secret: str = "change-me-in-env"
    spring_boot_base_url: str = "http://localhost:8080"

    gemini_model: str = "gemini-2.0-flash"
    nano_banana_model: str = "gemini-2.0-flash-exp"
    image_enhancement_provider: str = "nano_banana"

    langsmith_api_key: str = ""
    langsmith_tracing: bool = False

    port: int = 8001
    log_level: str = "INFO"


settings = Settings()
