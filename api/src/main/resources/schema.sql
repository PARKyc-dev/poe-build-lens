CREATE TABLE IF NOT EXISTS openai_daily_usage (
    usage_date DATE PRIMARY KEY,
    request_count INTEGER NOT NULL
);
