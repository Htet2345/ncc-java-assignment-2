-- Edit the password below BEFORE executing. Use the same password in config.properties.
CREATE USER IF NOT EXISTS 'exam_app'@'localhost' IDENTIFIED BY 'CHANGE_ME_DATABASE_PASSWORD';
GRANT SELECT, INSERT ON online_exam.* TO 'exam_app'@'localhost';
-- If exam_app already exists, change its password separately with ALTER USER.
-- This application does not need DROP, DELETE, UPDATE, CREATE or GRANT privileges.
