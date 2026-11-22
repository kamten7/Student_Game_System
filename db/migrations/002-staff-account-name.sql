USE student_game_system;

-- 仅在旧库缺少工作人员姓名字段时扩展账户表。
SET @ddl = IF(EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='user_account' AND column_name='staff_name'
), 'DO 0', 'ALTER TABLE user_account ADD COLUMN staff_name VARCHAR(60) AFTER password_hash');
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;
