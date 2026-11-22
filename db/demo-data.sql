-- 校内竞赛演示数据。由本地开发库加载，所有身份、赛项及成绩均为合成数据。
-- 保留已有业务记录。DEMO 编号和 demo. 账户仅用于这批种子数据。
USE student_game_system;
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';
SET @seed_now = UTC_TIMESTAMP(6);
SET @demo_password_hash = '$2a$10$uvmksaPUTTLQRP5Y0pRcBOvb5Nk/3WESMvUnF8FeniJmA5LPPUv2q';
SET @demo_pdf_sha = 'bec3f158a7f2e7c1958ec763fcde69f8cb84bd71fc76ebe9886610ed5a805499';

CREATE TEMPORARY TABLE demo_accounts (
    username VARCHAR(48) PRIMARY KEY,
    role_code VARCHAR(16) NOT NULL,
    staff_name VARCHAR(60)
);
INSERT INTO demo_accounts VALUES
    ('demo.admin', 'ADMIN', '沈知远'),
    ('demo.organizer', 'ORGANIZER', '唐婧'),
    ('demo.judge01', 'JUDGE', '顾砚'),
    ('demo.judge02', 'JUDGE', '叶澄'),
    ('demo.student01', 'STUDENT', NULL),
    ('demo.student02', 'STUDENT', NULL),
    ('demo.student03', 'STUDENT', NULL),
    ('demo.student04', 'STUDENT', NULL),
    ('demo.student05', 'STUDENT', NULL),
    ('demo.student06', 'STUDENT', NULL);

CREATE TEMPORARY TABLE demo_events (
    code VARCHAR(12) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    entry_mode VARCHAR(12) NOT NULL,
    min_size SMALLINT NOT NULL,
    max_size SMALLINT NOT NULL,
    status VARCHAR(12) NOT NULL,
    reg_start SMALLINT NOT NULL,
    reg_end SMALLINT NOT NULL,
    correction_end SMALLINT NOT NULL,
    review_end SMALLINT NOT NULL,
    work_start SMALLINT NOT NULL,
    work_end SMALLINT NOT NULL,
    judge_start SMALLINT NOT NULL,
    judge_end SMALLINT NOT NULL,
    rules_text TEXT NOT NULL
);
INSERT INTO demo_events VALUES
    ('DEMO-01', '校园创新挑战赛', 'TEAM', 2, 4, 'PUBLISHED', -2, 12, 14, 16, 17, 24, 25, 35, '从校园生活中选择一个可观察的问题，提交匿名 PDF 方案，说明需求、实施步骤和验证方式。'),
    ('DEMO-02', '数据叙事作品赛', 'INDIVIDUAL', 1, 1, 'PUBLISHED', -12, -1, 5, 8, 9, 18, 19, 28, '围绕公开数据制作一份数据叙事作品，说明数据来源、图表选择和主要结论。'),
    ('DEMO-03', '智慧校园应用挑战赛', 'INDIVIDUAL', 1, 1, 'PUBLISHED', -23, -11, -8, -5, -3, 10, 11, 23, '为图书借阅、场地预约或校园咨询设计一项数字服务，提交匿名原型说明和测试计划。'),
    ('DEMO-04', '低碳校园提案赛', 'TEAM', 2, 4, 'PUBLISHED', -35, -26, -23, -21, -19, -7, -4, 10, '以用能、通勤或废弃物管理为主题，给出可计算的低碳目标和试点计划。'),
    ('DEMO-05', '校园无障碍设计赛', 'INDIVIDUAL', 1, 1, 'PUBLISHED', -44, -33, -31, -29, -27, -16, -14, -2, '选取一处校园公共空间，说明无障碍问题、改良方案和验收标准。'),
    ('DEMO-06', '绿色校园行动设计赛', 'TEAM', 2, 4, 'PUBLISHED', -63, -52, -50, -48, -45, -32, -30, -18, '围绕校园可持续行动设计试点。评审关注问题证据、方案可行性和传播方式。'),
    ('DEMO-07', '校园文创设计赛', 'TEAM', 2, 3, 'PUBLISHED', 4, 17, 19, 21, 22, 32, 33, 45, '以校园公共文化为创作主题，提交作品展示和设计说明，注明素材来源。'),
    ('DEMO-08', '校园服务流程优化赛', 'INDIVIDUAL', 1, 1, 'PUBLISHED', -1, 13, 15, 17, 18, 28, 29, 40, '调研一项校内服务，绘制现状流程和优化后的操作流程。'),
    ('DEMO-09', '社区科普作品赛', 'INDIVIDUAL', 1, 1, 'ARCHIVED', -80, -69, -67, -65, -63, -51, -49, -35, '用适合非专业读者的方式解释一个科学概念。该届赛事已归档，可查看公告。'),
    ('DEMO-10', '图书馆导览方案赛', 'TEAM', 2, 4, 'DRAFT', 7, 21, 24, 26, 27, 38, 39, 50, '设计面向新生的图书馆导览方案，包含动线、标识和信息获取方式。');

CREATE TEMPORARY TABLE demo_registrations (
    registration_no VARCHAR(32) PRIMARY KEY,
    competition_no VARCHAR(12) NOT NULL,
    captain_username VARCHAR(48) NOT NULL,
    team_name VARCHAR(80),
    status VARCHAR(20) NOT NULL
);
INSERT INTO demo_registrations VALUES
    ('DEMO-R01', 'DEMO-01', 'demo.student01', '萤火工坊', 'PENDING'),
    ('DEMO-R02', 'DEMO-01', 'demo.student03', '方舟小组', 'DRAFT'),
    ('DEMO-R03', 'DEMO-02', 'demo.student03', NULL, 'PENDING'),
    ('DEMO-R04', 'DEMO-02', 'demo.student04', NULL, 'CHANGES_REQUESTED'),
    ('DEMO-R05', 'DEMO-03', 'demo.student01', NULL, 'APPROVED'),
    ('DEMO-R06', 'DEMO-03', 'demo.student02', NULL, 'APPROVED'),
    ('DEMO-R07', 'DEMO-04', 'demo.student01', '绿光行动组', 'APPROVED'),
    ('DEMO-R08', 'DEMO-04', 'demo.student03', '清风小组', 'APPROVED'),
    ('DEMO-R09', 'DEMO-05', 'demo.student05', NULL, 'APPROVED'),
    ('DEMO-R10', 'DEMO-05', 'demo.student04', NULL, 'APPROVED'),
    ('DEMO-R11', 'DEMO-06', 'demo.student01', '循环设计组', 'APPROVED'),
    ('DEMO-R12', 'DEMO-06', 'demo.student02', '青芽行动组', 'APPROVED'),
    ('DEMO-R13', 'DEMO-09', 'demo.student04', NULL, 'APPROVED');

CREATE TEMPORARY TABLE demo_members (
    registration_no VARCHAR(32) NOT NULL,
    username VARCHAR(48) NOT NULL,
    invitation_status VARCHAR(12) NOT NULL,
    PRIMARY KEY (registration_no, username)
);
INSERT INTO demo_members VALUES
    ('DEMO-R01', 'demo.student01', 'CONFIRMED'),
    ('DEMO-R01', 'demo.student02', 'CONFIRMED'),
    ('DEMO-R02', 'demo.student03', 'CONFIRMED'),
    ('DEMO-R02', 'demo.student04', 'INVITED'),
    ('DEMO-R03', 'demo.student03', 'CONFIRMED'),
    ('DEMO-R04', 'demo.student04', 'CONFIRMED'),
    ('DEMO-R05', 'demo.student01', 'CONFIRMED'),
    ('DEMO-R06', 'demo.student02', 'CONFIRMED'),
    ('DEMO-R07', 'demo.student01', 'CONFIRMED'),
    ('DEMO-R07', 'demo.student02', 'CONFIRMED'),
    ('DEMO-R08', 'demo.student03', 'CONFIRMED'),
    ('DEMO-R08', 'demo.student04', 'CONFIRMED'),
    ('DEMO-R09', 'demo.student05', 'CONFIRMED'),
    ('DEMO-R10', 'demo.student04', 'CONFIRMED'),
    ('DEMO-R11', 'demo.student01', 'CONFIRMED'),
    ('DEMO-R11', 'demo.student03', 'CONFIRMED'),
    ('DEMO-R12', 'demo.student02', 'CONFIRMED'),
    ('DEMO-R12', 'demo.student05', 'CONFIRMED'),
    ('DEMO-R13', 'demo.student04', 'CONFIRMED');

START TRANSACTION;

INSERT INTO user_account(username, password_hash, staff_name)
SELECT username, @demo_password_hash, staff_name FROM demo_accounts ORDER BY username;

INSERT INTO user_role(user_id, role_id)
SELECT a.id, r.id
FROM demo_accounts d JOIN user_account a ON a.username=d.username JOIN role r ON r.role_code=d.role_code;

SET @demo_organizer = (SELECT id FROM user_account WHERE username='demo.organizer');
SET @demo_admin = (SELECT id FROM user_account WHERE username='demo.admin');

INSERT INTO student_profile(user_id, claimed_student_no, student_no, full_name, department, major,
    verification_status, verified_by, verified_at, invite_code)
SELECT id, 'D20260001', 'D20260001', '林予', '计算机学院', '软件工程',
       'VERIFIED', @demo_admin, DATE_SUB(@seed_now, INTERVAL 90 DAY), 'jingxu-demo-invite-01'
FROM user_account WHERE username='demo.student01'
UNION ALL
SELECT id, 'D20260002', 'D20260002', '周禾', '设计学院', '视觉传达设计',
       'VERIFIED', @demo_admin, DATE_SUB(@seed_now, INTERVAL 90 DAY), 'jingxu-demo-invite-02'
FROM user_account WHERE username='demo.student02'
UNION ALL
SELECT id, 'D20260003', 'D20260003', '陈曦', '统计学院', '数据科学',
       'VERIFIED', @demo_admin, DATE_SUB(@seed_now, INTERVAL 90 DAY), 'jingxu-demo-invite-03'
FROM user_account WHERE username='demo.student03'
UNION ALL
SELECT id, 'D20260004', 'D20260004', '许宁', '建筑学院', '城乡规划',
       'VERIFIED', @demo_admin, DATE_SUB(@seed_now, INTERVAL 90 DAY), 'jingxu-demo-invite-04'
FROM user_account WHERE username='demo.student04'
UNION ALL
SELECT id, 'D20260005', 'D20260005', '方屿', '公共管理学院', '公共事业管理',
       'VERIFIED', @demo_admin, DATE_SUB(@seed_now, INTERVAL 90 DAY), 'jingxu-demo-invite-05'
FROM user_account WHERE username='demo.student05'
UNION ALL
SELECT id, 'D20260006', NULL, '程砚', '信息工程学院', '信息管理',
       'PENDING', NULL, NULL, NULL
FROM user_account WHERE username='demo.student06';

INSERT INTO competition(competition_no, name, edition, rules_text, organizer_id, entry_mode,
    min_team_size, max_team_size, registration_opens_at, registration_closes_at, correction_closes_at,
    qualification_closes_at, submission_opens_at, submission_closes_at, judging_opens_at,
    judging_closes_at, scoring_rubric, required_judges, status)
SELECT code, name, '校园开放季', rules_text, @demo_organizer, entry_mode, min_size, max_size,
       DATE_ADD(@seed_now, INTERVAL reg_start DAY), DATE_ADD(@seed_now, INTERVAL reg_end DAY),
       DATE_ADD(@seed_now, INTERVAL correction_end DAY), DATE_ADD(@seed_now, INTERVAL review_end DAY),
       DATE_ADD(@seed_now, INTERVAL work_start DAY), DATE_ADD(@seed_now, INTERVAL work_end DAY),
       DATE_ADD(@seed_now, INTERVAL judge_start DAY), DATE_ADD(@seed_now, INTERVAL judge_end DAY),
       JSON_ARRAY(JSON_OBJECT('id','insight','label','问题洞察','weight',40), JSON_OBJECT('id','proposal','label','方案质量','weight',35),
           JSON_OBJECT('id','delivery','label','落地可行性','weight',25)), 1, status
FROM demo_events ORDER BY code;

INSERT INTO registration(registration_no, competition_id, captain_id, team_display_name, status, row_version, submitted_at)
SELECT d.registration_no, c.id, a.id, d.team_name, d.status,
       CASE WHEN d.status='DRAFT' THEN 0 WHEN d.status='PENDING' THEN 1 ELSE 2 END,
       CASE WHEN d.status='DRAFT' THEN NULL
            ELSE DATE_SUB(LEAST(@seed_now,c.registration_closes_at), INTERVAL 12 HOUR) END
FROM demo_registrations d JOIN competition c ON c.competition_no=d.competition_no
JOIN user_account a ON a.username=d.captain_username ORDER BY d.registration_no;

INSERT INTO registration_member(registration_id, student_id, is_captain, invitation_status, confirmed_at)
SELECT r.id, a.id, a.id=r.captain_id, d.invitation_status,
       IF(d.invitation_status='CONFIRMED', DATE_SUB(COALESCE(r.submitted_at,@seed_now), INTERVAL 2 HOUR), NULL)
FROM demo_members d JOIN registration r ON r.registration_no=d.registration_no
JOIN user_account a ON a.username=d.username;

INSERT INTO participation_claim(competition_id, student_id, registration_id, claimed_at)
SELECT r.competition_id, m.student_id, r.id, r.submitted_at
FROM demo_registrations d JOIN registration r ON r.registration_no=d.registration_no
JOIN registration_member m ON m.registration_id=r.id
WHERE r.status IN ('PENDING','CHANGES_REQUESTED','APPROVED') AND m.invitation_status='CONFIRMED';

INSERT INTO registration_review_log(registration_id, actor_id, from_status, to_status, roster_snapshot, created_at)
SELECT r.id, r.captain_id, 'DRAFT', 'PENDING',
       (SELECT JSON_ARRAYAGG(JSON_OBJECT('studentId',m.student_id,'fullName',p.full_name,'isCaptain',m.is_captain))
        FROM registration_member m JOIN student_profile p ON p.user_id=m.student_id
        WHERE m.registration_id=r.id AND m.invitation_status='CONFIRMED'),
       r.submitted_at
FROM demo_registrations d JOIN registration r ON r.registration_no=d.registration_no WHERE r.submitted_at IS NOT NULL;

INSERT INTO registration_review_log(registration_id, actor_id, from_status, to_status, reason, created_at)
SELECT r.id, @demo_organizer, 'PENDING', r.status,
       IF(r.status='APPROVED', '校内身份与参赛人数符合要求。', '请确认报名信息后在补正截止前重新提交。'),
       DATE_ADD(r.submitted_at, INTERVAL 4 HOUR)
FROM demo_registrations d JOIN registration r ON r.registration_no=d.registration_no
WHERE r.status IN ('APPROVED','CHANGES_REQUESTED');

CREATE TEMPORARY TABLE demo_works (
    registration_no VARCHAR(32) NOT NULL,
    anon_code VARCHAR(32) NOT NULL,
    version_no INT NOT NULL,
    file_no INT NOT NULL,
    title VARCHAR(120) NOT NULL,
    PRIMARY KEY (registration_no, version_no)
);
INSERT INTO demo_works VALUES
    ('DEMO-R06', 'A-DEMO-006', 1, 1, '自习空间预约流程'),
    ('DEMO-R07', 'A-DEMO-007', 1, 2, '教学楼节能巡检方案'),
    ('DEMO-R07', 'A-DEMO-007', 2, 3, '教学楼节能巡检方案'),
    ('DEMO-R08', 'A-DEMO-008', 1, 4, '低碳通勤行动提案'),
    ('DEMO-R09', 'A-DEMO-009', 1, 5, '教学楼无障碍地图'),
    ('DEMO-R11', 'A-DEMO-011', 1, 6, '校园闲置物品循环站'),
    ('DEMO-R12', 'A-DEMO-012', 1, 7, '宿舍节水共同行动');

INSERT INTO submission(registration_id, anonymous_code)
SELECT r.id, w.anon_code FROM demo_works w JOIN registration r ON r.registration_no=w.registration_no
WHERE w.version_no=1;

INSERT INTO submission_version(submission_id, version_no, request_id, request_hash, title, storage_key, file_sha256, submitted_by, submitted_at)
SELECT s.id, w.version_no, CONCAT('DEMO-WORK-',w.file_no), SHA2(CONCAT(@demo_pdf_sha,CHAR(10),w.title),256), w.title,
       CONCAT(LPAD(CONCAT('d3a0',w.file_no),32,'0'),'.pdf'), @demo_pdf_sha, r.captain_id,
       DATE_ADD(c.submission_opens_at, INTERVAL w.version_no HOUR)
FROM demo_works w JOIN registration r ON r.registration_no=w.registration_no
JOIN competition c ON c.id=r.competition_id JOIN submission s ON s.registration_id=r.id;

UPDATE submission s JOIN registration r ON r.id=s.registration_id
JOIN demo_registrations d ON d.registration_no=r.registration_no JOIN competition c ON c.id=r.competition_id
SET s.locked_version_id=(SELECT v.id FROM submission_version v WHERE v.submission_id=s.id ORDER BY v.version_no DESC LIMIT 1),
    s.locked_at=c.submission_closes_at, s.row_version=1
WHERE c.competition_no IN ('DEMO-04','DEMO-05','DEMO-06');

CREATE TEMPORARY TABLE demo_judging (
    registration_no VARCHAR(32) PRIMARY KEY,
    username VARCHAR(48) NOT NULL,
    score_a INT,
    score_b INT,
    score_c INT
);
INSERT INTO demo_judging VALUES
    ('DEMO-R07', 'demo.judge01', NULL, NULL, NULL),
    ('DEMO-R08', 'demo.judge02', 84, 86, 82),
    ('DEMO-R09', 'demo.judge01', 92, 88, 84),
    ('DEMO-R11', 'demo.judge01', 94, 92, 90),
    ('DEMO-R12', 'demo.judge02', 90, 88, 86);

INSERT INTO review_assignment(submission_version_id, judge_id, status, assigned_at)
SELECT s.locked_version_id, a.id, IF(j.score_a IS NULL,'ASSIGNED','SUBMITTED'), DATE_ADD(c.judging_opens_at, INTERVAL 1 HOUR)
FROM demo_judging j JOIN registration r ON r.registration_no=j.registration_no
JOIN competition c ON c.id=r.competition_id JOIN submission s ON s.registration_id=r.id
JOIN user_account a ON a.username=j.username;

INSERT INTO review_score(assignment_id, criterion_scores, feedback, row_version, submitted_at)
SELECT ra.id, JSON_OBJECT('insight',j.score_a,'proposal',j.score_b,'delivery',j.score_c),
       '问题描述清晰，建议补充试点的验收指标。', 2, DATE_ADD(ra.assigned_at, INTERVAL 2 HOUR)
FROM demo_judging j JOIN registration r ON r.registration_no=j.registration_no
JOIN submission s ON s.registration_id=r.id
JOIN user_account a ON a.username=j.username
JOIN review_assignment ra ON ra.submission_version_id=s.locked_version_id AND ra.judge_id=a.id
WHERE j.score_a IS NOT NULL;

INSERT INTO result_publication(competition_id, version_no, published_by, published_at, publication_type,
    no_awards, request_id, request_hash, scoring_basis)
SELECT c.id, 1, @demo_organizer, DATE_ADD(c.judging_closes_at, INTERVAL 1 DAY), 'FIRST',
       c.competition_no='DEMO-09', CONCAT('DEMO-RESULT-',c.competition_no), SHA2(c.competition_no,256),
       (SELECT JSON_ARRAYAGG(JSON_OBJECT('registrationId',r.id,'lockedVersionId',s.locked_version_id,
           'outcomeStatus',IF(s.id IS NULL,'NO_WORK','RANKED'),'scores',
           COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('assignmentId',ra.id,'rowVersion',rs.row_version,
           'criterionScores',rs.criterion_scores)) FROM review_assignment ra
           JOIN review_score rs ON rs.assignment_id=ra.id
           WHERE ra.submission_version_id=s.locked_version_id AND ra.status='SUBMITTED'),JSON_ARRAY())))
        FROM registration r LEFT JOIN submission s ON s.registration_id=r.id
        WHERE r.competition_id=c.id AND r.status='APPROVED')
FROM competition c WHERE c.competition_no IN ('DEMO-06','DEMO-09');

INSERT INTO competition_result(publication_id, registration_id, competition_id, work_title_snapshot, entry_display_snapshot,
    outcome_status, total_score, rank_no, award_name)
SELECT p.id, r.id, c.id, v.title, COALESCE(r.team_display_name,r.registration_no),
       IF(s.id IS NULL,'NO_WORK','RANKED'),
       CASE r.registration_no WHEN 'DEMO-R11' THEN 92.30 WHEN 'DEMO-R12' THEN 88.30 END,
       CASE r.registration_no WHEN 'DEMO-R11' THEN 1 WHEN 'DEMO-R12' THEN 2 END,
       CASE r.registration_no WHEN 'DEMO-R11' THEN '一等奖' WHEN 'DEMO-R12' THEN '二等奖' END
FROM competition c JOIN result_publication p ON p.competition_id=c.id
JOIN registration r ON r.competition_id=c.id LEFT JOIN submission s ON s.registration_id=r.id
LEFT JOIN submission_version v ON v.id=s.locked_version_id
WHERE c.competition_no IN ('DEMO-06','DEMO-09');

UPDATE competition c JOIN result_publication p ON p.competition_id=c.id
SET c.current_publication_id=p.id, c.row_version=1
WHERE c.competition_no IN ('DEMO-06','DEMO-09');

INSERT INTO audit_log(actor_id, action, target_type, target_key, change_summary, created_at)
SELECT @demo_organizer, 'RESULT_FIRST', 'COMPETITION', CAST(c.id AS CHAR), JSON_OBJECT('publicationId', p.id), p.published_at
FROM competition c JOIN result_publication p ON p.id=c.current_publication_id
WHERE c.competition_no IN ('DEMO-06','DEMO-09');

COMMIT;
