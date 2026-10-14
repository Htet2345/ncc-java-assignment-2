-- Run in MySQL Workbench as your database administrator.
-- Requires MySQL 8.0.16+ (CHECK constraints are enforced).
CREATE DATABASE IF NOT EXISTS online_exam CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE online_exam;

CREATE TABLE IF NOT EXISTS users (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 username VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL,
 role ENUM('admin','student') NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS questions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 question_text VARCHAR(1000) NOT NULL,
 option1 VARCHAR(255) NOT NULL, option2 VARCHAR(255) NOT NULL,
 option3 VARCHAR(255) NOT NULL, option4 VARCHAR(255) NOT NULL,
 correct_option TINYINT NOT NULL,
 created_by BIGINT NOT NULL,
 CONSTRAINT ck_question_correct CHECK (correct_option BETWEEN 1 AND 4),
 CONSTRAINT fk_question_creator FOREIGN KEY(created_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS exams (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL UNIQUE,
 created_by BIGINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_exam_creator FOREIGN KEY(created_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS exam_questions (
 exam_id BIGINT NOT NULL, question_id BIGINT NOT NULL, position INT NOT NULL,
 PRIMARY KEY(exam_id,question_id), UNIQUE KEY uq_exam_position(exam_id,position),
 CONSTRAINT ck_exam_position CHECK(position > 0),
 CONSTRAINT fk_eq_exam FOREIGN KEY(exam_id) REFERENCES exams(id),
 CONSTRAINT fk_eq_question FOREIGN KEY(question_id) REFERENCES questions(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS attempts (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, exam_id BIGINT NOT NULL,
 score INT NOT NULL, total_questions INT NOT NULL,
 submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY ix_attempt_user_id(user_id,id),
 CONSTRAINT ck_attempt_score CHECK(total_questions > 0 AND score BETWEEN 0 AND total_questions),
 CONSTRAINT fk_attempt_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_attempt_exam FOREIGN KEY(exam_id) REFERENCES exams(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS attempt_answers (
 attempt_id BIGINT NOT NULL, question_id BIGINT NOT NULL,
 selected_option TINYINT NOT NULL, is_correct BOOLEAN NOT NULL,
 PRIMARY KEY(attempt_id,question_id),
 CONSTRAINT ck_answer_option CHECK(selected_option BETWEEN 1 AND 4),
 CONSTRAINT fk_answer_attempt FOREIGN KEY(attempt_id) REFERENCES attempts(id),
 CONSTRAINT fk_answer_question FOREIGN KEY(question_id) REFERENCES questions(id)
) ENGINE=InnoDB;
