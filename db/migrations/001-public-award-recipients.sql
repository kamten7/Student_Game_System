USE student_game_system;

SET @ddl = IF(EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='competition_result' AND column_name='recipient_names_snapshot'
), 'DO 0', 'ALTER TABLE competition_result ADD COLUMN recipient_names_snapshot JSON AFTER award_name');
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;
