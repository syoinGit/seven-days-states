ALTER TABLE T_AI_COMMENT ADD COLUMN diary_date DATE;

CREATE UNIQUE INDEX uq_t_ai_comment_diary_date
    ON T_AI_COMMENT (diary_date);
