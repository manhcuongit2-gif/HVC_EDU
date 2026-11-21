UPDATE ai_settings
SET max_response_tokens = 16384
WHERE id = 1
  AND (max_response_tokens IS NULL OR max_response_tokens = 8192);
