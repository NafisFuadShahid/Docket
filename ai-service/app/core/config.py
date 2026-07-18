from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    model_config = {"env_file": ".env", "extra": "ignore"}

    SPRING_BACKEND_URL: str = "http://localhost:8080"
    AI_API_KEY: str = ""
    AI_API_BASE: str = "https://api.groq.com/openai/v1"
    AI_MODEL: str = "llama-3.3-70b-versatile"
    JINA_API_KEY: str = ""
    STORAGE_PATH: str = "./storage"
    CRAWLER_RATE_LIMIT: float = 2.0
    CRAWLER_TIMEOUT: int = 30
    CRAWLER_ALLOWED_DOMAINS: str = "bb.org.bd,bfiu.org.bd"
    LOG_LEVEL: str = "INFO"

    @property
    def allowed_domains(self) -> list[str]:
        return [d.strip() for d in self.CRAWLER_ALLOWED_DOMAINS.split(",")]

    @property
    def has_ai_key(self) -> bool:
        return bool(self.AI_API_KEY)

    @property
    def has_jina_key(self) -> bool:
        return bool(self.JINA_API_KEY)


settings = Settings()
