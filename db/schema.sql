-- 初始化竞赛管理数据库、表结构和基础角色。
CREATE DATABASE IF NOT EXISTS student_game_system
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE student_game_system;

CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(48) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    staff_name VARCHAR(60),
    status VARCHAR(12) NOT NULL DEFAULT 'ACTIVE',
    deleted_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS student_profile (
    user_id BIGINT PRIMARY KEY,
    claimed_student_no VARCHAR(32) NOT NULL,
    student_no VARCHAR(32) UNIQUE,
    full_name VARCHAR(60) NOT NULL,
    department VARCHAR(80) NOT NULL DEFAULT '',
    major VARCHAR(80) NOT NULL DEFAULT '',
    verification_status VARCHAR(12) NOT NULL DEFAULT 'PENDING',
    verified_by BIGINT,
    verified_at DATETIME(6),
    invite_code VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin UNIQUE,
    FOREIGN KEY (user_id) REFERENCES user_account(id),
    FOREIGN KEY (verified_by) REFERENCES user_account(id),
    CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    CHECK ((verification_status = 'VERIFIED') = (student_no IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(16) NOT NULL UNIQUE,
    role_name VARCHAR(32) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES user_account(id),
    FOREIGN KEY (role_id) REFERENCES role(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS competition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    competition_no VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    edition VARCHAR(40) NOT NULL,
    rules_text TEXT NOT NULL,
    organizer_id BIGINT NOT NULL,
    entry_mode VARCHAR(12) NOT NULL,
    min_team_size SMALLINT NOT NULL,
    max_team_size SMALLINT NOT NULL,
    registration_opens_at DATETIME(6) NOT NULL,
    registration_closes_at DATETIME(6) NOT NULL,
    correction_closes_at DATETIME(6) NOT NULL,
    qualification_closes_at DATETIME(6) NOT NULL,
    submission_opens_at DATETIME(6) NOT NULL,
    submission_closes_at DATETIME(6) NOT NULL,
    judging_opens_at DATETIME(6) NOT NULL,
    judging_closes_at DATETIME(6) NOT NULL,
    scoring_rubric JSON NOT NULL,
    required_judges SMALLINT NOT NULL,
    status VARCHAR(12) NOT NULL DEFAULT 'DRAFT',
    row_version INT NOT NULL DEFAULT 0,
    cancellation_reason VARCHAR(400),
    current_publication_id BIGINT,
    FOREIGN KEY (organizer_id) REFERENCES user_account(id),
    INDEX ix_competition_owner (organizer_id, status, id),
    INDEX ix_competition_discovery (status, registration_closes_at, id),
    CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'ARCHIVED')),
    CHECK ((entry_mode = 'INDIVIDUAL' AND min_team_size = 1 AND max_team_size = 1)
        OR (entry_mode = 'TEAM' AND min_team_size >= 2 AND max_team_size >= min_team_size)),
    CHECK (required_judges >= 1),
    CHECK (registration_opens_at < registration_closes_at
        AND registration_closes_at <= correction_closes_at
        AND correction_closes_at <= qualification_closes_at
        AND qualification_closes_at <= submission_opens_at
        AND submission_opens_at < submission_closes_at
        AND submission_closes_at <= judging_opens_at
        AND judging_opens_at < judging_closes_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS registration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    registration_no VARCHAR(32) NOT NULL UNIQUE,
    competition_id BIGINT NOT NULL,
    captain_id BIGINT NOT NULL,
    team_display_name VARCHAR(80),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    row_version INT NOT NULL DEFAULT 0,
    submitted_at DATETIME(6),
    UNIQUE KEY uq_registration_competition (competition_id, id),
    INDEX ix_registration_review (competition_id, status, id),
    FOREIGN KEY (competition_id) REFERENCES competition(id),
    FOREIGN KEY (captain_id) REFERENCES user_account(id),
    CHECK (status IN ('DRAFT','PENDING','APPROVED','CHANGES_REQUESTED','REJECTED','WITHDRAWN','EXPIRED'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS registration_member (
    registration_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    is_captain BOOLEAN NOT NULL DEFAULT FALSE,
    invitation_status VARCHAR(12) NOT NULL,
    confirmed_at DATETIME(6),
    PRIMARY KEY (registration_id, student_id),
    INDEX ix_member_inbox (student_id, invitation_status, registration_id),
    FOREIGN KEY (registration_id) REFERENCES registration(id),
    FOREIGN KEY (student_id) REFERENCES student_profile(user_id),
    CHECK (invitation_status IN ('INVITED','CONFIRMED','DECLINED','REMOVED'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS participation_claim (
    competition_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    claimed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (competition_id, student_id),
    FOREIGN KEY (competition_id, registration_id) REFERENCES registration(competition_id, id),
    FOREIGN KEY (registration_id, student_id) REFERENCES registration_member(registration_id, student_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS registration_review_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    registration_id BIGINT NOT NULL,
    actor_id BIGINT,
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    reason VARCHAR(400),
    roster_snapshot JSON,
    created_at DATETIME(6) NOT NULL,
    INDEX ix_registration_history (registration_id, created_at, id),
    FOREIGN KEY (registration_id) REFERENCES registration(id),
    FOREIGN KEY (actor_id) REFERENCES user_account(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    registration_id BIGINT NOT NULL UNIQUE,
    anonymous_code VARCHAR(32) NOT NULL UNIQUE,
    locked_version_id BIGINT,
    locked_at DATETIME(6),
    eligibility_status VARCHAR(12) NOT NULL DEFAULT 'ELIGIBLE',
    eligibility_reason VARCHAR(400),
    row_version INT NOT NULL DEFAULT 0,
    FOREIGN KEY (registration_id) REFERENCES registration(id),
    CHECK (eligibility_status IN ('ELIGIBLE', 'EXCLUDED')),
    CHECK ((locked_version_id IS NULL) = (locked_at IS NULL))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS submission_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    request_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_hash CHAR(64) CHARACTER SET ascii NOT NULL,
    title VARCHAR(120) NOT NULL,
    storage_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
    file_sha256 CHAR(64) CHARACTER SET ascii NOT NULL,
    submitted_by BIGINT NOT NULL,
    submitted_at DATETIME(6) NOT NULL,
    UNIQUE KEY uq_version_owner (submission_id, id),
    UNIQUE KEY uq_version_number (submission_id, version_no),
    UNIQUE KEY uq_version_request (submission_id, request_id),
    FOREIGN KEY (submission_id) REFERENCES submission(id),
    FOREIGN KEY (submitted_by) REFERENCES user_account(id),
    CHECK (version_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS review_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_version_id BIGINT NOT NULL,
    judge_id BIGINT NOT NULL,
    status VARCHAR(12) NOT NULL DEFAULT 'ASSIGNED',
    reason VARCHAR(400),
    assigned_at DATETIME(6) NOT NULL,
    UNIQUE KEY uq_review_judge (submission_version_id, judge_id),
    INDEX ix_judge_tasks (judge_id, status, id),
    FOREIGN KEY (submission_version_id) REFERENCES submission_version(id),
    FOREIGN KEY (judge_id) REFERENCES user_account(id),
    CHECK (status IN ('ASSIGNED','RECUSED','REVOKED','SUBMITTED'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS review_score (
    assignment_id BIGINT PRIMARY KEY,
    criterion_scores JSON NOT NULL,
    feedback VARCHAR(1200) NOT NULL DEFAULT '',
    row_version INT NOT NULL DEFAULT 0,
    submitted_at DATETIME(6),
    FOREIGN KEY (assignment_id) REFERENCES review_assignment(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS result_publication (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    competition_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    published_by BIGINT NOT NULL,
    published_at DATETIME(6) NOT NULL,
    correction_reason VARCHAR(400),
    publication_type VARCHAR(12) NOT NULL,
    no_awards BOOLEAN NOT NULL,
    request_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_hash CHAR(64) CHARACTER SET ascii NOT NULL,
    scoring_basis JSON NOT NULL,
    UNIQUE KEY uq_publication_owner (competition_id, id),
    UNIQUE KEY uq_publication_version (competition_id, version_no),
    UNIQUE KEY uq_publication_request (competition_id, request_id),
    FOREIGN KEY (competition_id) REFERENCES competition(id),
    FOREIGN KEY (published_by) REFERENCES user_account(id),
    CHECK (publication_type IN ('FIRST','CORRECTION')),
    CHECK (version_no >= 1)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS competition_result (
    publication_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    competition_id BIGINT NOT NULL,
    work_title_snapshot VARCHAR(120),
    entry_display_snapshot VARCHAR(80) NOT NULL,
    outcome_status VARCHAR(12) NOT NULL,
    outcome_reason VARCHAR(400),
    total_score DECIMAL(5,2),
    rank_no INT,
    award_name VARCHAR(60),
    recipient_names_snapshot JSON,
    PRIMARY KEY (publication_id, registration_id),
    INDEX ix_public_result (publication_id, rank_no),
    FOREIGN KEY (competition_id, publication_id) REFERENCES result_publication(competition_id, id),
    FOREIGN KEY (competition_id, registration_id) REFERENCES registration(competition_id, id),
    CHECK (outcome_status IN ('RANKED','NO_WORK','EXCLUDED')),
    CHECK ((outcome_status = 'RANKED' AND total_score IS NOT NULL AND total_score BETWEEN 0 AND 100
        AND rank_no IS NOT NULL AND rank_no >= 1)
        OR (outcome_status <> 'RANKED' AND total_score IS NULL AND rank_no IS NULL AND award_name IS NULL))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT,
    action VARCHAR(48) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_key VARCHAR(64) NOT NULL,
    change_summary JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX ix_audit_target (target_type, target_key, id),
    INDEX ix_audit_actor (actor_id, created_at, id),
    FOREIGN KEY (actor_id) REFERENCES user_account(id)
) ENGINE=InnoDB;

-- 在两端表创建后绑定当前批次与锁定版本的所属对象。
SET @ddl = IF(EXISTS(
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND constraint_name = 'fk_competition_current_publication'
), 'DO 0', 'ALTER TABLE competition ADD CONSTRAINT fk_competition_current_publication
    FOREIGN KEY (id, current_publication_id) REFERENCES result_publication(competition_id, id)');
PREPARE schema_stmt FROM @ddl;
EXECUTE schema_stmt;
DEALLOCATE PREPARE schema_stmt;

SET @ddl = IF(EXISTS(
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND constraint_name = 'fk_submission_locked_version'
), 'DO 0', 'ALTER TABLE submission ADD CONSTRAINT fk_submission_locked_version
    FOREIGN KEY (id, locked_version_id) REFERENCES submission_version(submission_id, id)');
PREPARE schema_stmt FROM @ddl;
EXECUTE schema_stmt;
DEALLOCATE PREPARE schema_stmt;

INSERT INTO role (role_code, role_name)
VALUES ('STUDENT','学生'), ('JUDGE','评委'), ('ORGANIZER','赛事负责人'), ('ADMIN','平台管理员')
ON DUPLICATE KEY UPDATE role_code = role.role_code;
