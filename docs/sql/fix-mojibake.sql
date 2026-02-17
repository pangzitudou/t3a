-- Purpose:
-- 1) Force utf8mb4 connection/session charset
-- 2) Backup suspicious rows
-- 3) Repair common UTF-8<->latin1 mojibake strings

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
USE t3a_quiz;

-- Backup tables (idempotent)
CREATE TABLE IF NOT EXISTS t_user_charset_backup LIKE t_user;
CREATE TABLE IF NOT EXISTS t_question_bank_charset_backup LIKE t_question_bank;
CREATE TABLE IF NOT EXISTS t_question_charset_backup LIKE t_question;

-- Backup only suspicious rows
INSERT INTO t_user_charset_backup
SELECT *
FROM t_user
WHERE nickname REGEXP '[ÃÂÐÑåçæéïð]'
  AND id NOT IN (SELECT id FROM t_user_charset_backup);

INSERT INTO t_question_bank_charset_backup
SELECT *
FROM t_question_bank
WHERE (
    name REGEXP '[ÃÂÐÑåçæéïð]'
    OR description REGEXP '[ÃÂÐÑåçæéïð]'
    OR category REGEXP '[ÃÂÐÑåçæéïð]'
  )
  AND id NOT IN (SELECT id FROM t_question_bank_charset_backup);

INSERT INTO t_question_charset_backup
SELECT *
FROM t_question
WHERE (
    content REGEXP '[ÃÂÐÑåçæéïð]'
    OR options REGEXP '[ÃÂÐÑåçæéïð]'
    OR correct_answer REGEXP '[ÃÂÐÑåçæéïð]'
    OR explanation REGEXP '[ÃÂÐÑåçæéïð]'
    OR tags REGEXP '[ÃÂÐÑåçæéïð]'
  )
  AND id NOT IN (SELECT id FROM t_question_charset_backup);

-- Repair t_user
UPDATE t_user
SET nickname = CONVERT(BINARY CONVERT(nickname USING latin1) USING utf8mb4)
WHERE nickname REGEXP '[ÃÂÐÑåçæéïð]';

-- Repair t_question_bank
UPDATE t_question_bank
SET
  name = CASE
    WHEN name REGEXP '[ÃÂÐÑåçæéïð]'
    THEN CONVERT(BINARY CONVERT(name USING latin1) USING utf8mb4)
    ELSE name
  END,
  description = CASE
    WHEN description REGEXP '[ÃÂÐÑåçæéïð]'
    THEN CONVERT(BINARY CONVERT(description USING latin1) USING utf8mb4)
    ELSE description
  END,
  category = CASE
    WHEN category REGEXP '[ÃÂÐÑåçæéïð]'
    THEN CONVERT(BINARY CONVERT(category USING latin1) USING utf8mb4)
    ELSE category
  END
WHERE name REGEXP '[ÃÂÐÑåçæéïð]'
   OR description REGEXP '[ÃÂÐÑåçæéïð]'
   OR category REGEXP '[ÃÂÐÑåçæéïð]';

-- Repair t_question text fields
UPDATE t_question
SET
  content = CASE
    WHEN content REGEXP '[ÃÂÐÑåçæéïð]'
    THEN CONVERT(BINARY CONVERT(content USING latin1) USING utf8mb4)
    ELSE content
  END,
  options = CASE
    WHEN options REGEXP '[ÃÂÐÑåçæéïð]'
    THEN CONVERT(BINARY CONVERT(options USING latin1) USING utf8mb4)
    ELSE options
  END,
  correct_answer = CASE
    WHEN correct_answer REGEXP '[ÃÂÐÑåçæéïð]'
    THEN CONVERT(BINARY CONVERT(correct_answer USING latin1) USING utf8mb4)
    ELSE correct_answer
  END,
  explanation = CASE
    WHEN explanation REGEXP '[ÃÂÐÑåçæéïð]'
    THEN CONVERT(BINARY CONVERT(explanation USING latin1) USING utf8mb4)
    ELSE explanation
  END,
  tags = CASE
    WHEN tags REGEXP '[ÃÂÐÑåçæéïð]'
    THEN CONVERT(BINARY CONVERT(tags USING latin1) USING utf8mb4)
    ELSE tags
  END
WHERE content REGEXP '[ÃÂÐÑåçæéïð]'
   OR options REGEXP '[ÃÂÐÑåçæéïð]'
   OR correct_answer REGEXP '[ÃÂÐÑåçæéïð]'
   OR explanation REGEXP '[ÃÂÐÑåçæéïð]'
   OR tags REGEXP '[ÃÂÐÑåçæéïð]';
