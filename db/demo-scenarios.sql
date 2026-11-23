-- 补充各赛事阶段的合成案例，覆盖组队、交件、评审和发布。
-- 第 1 步 定义合成账号和赛程，以首次导入时刻为基准。
USE student_game_system;
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';
SET @seed_at = UTC_TIMESTAMP(6);
SET @sample_hash = '$2a$10$uvmksaPUTTLQRP5Y0pRcBOvb5Nk/3WESMvUnF8FeniJmA5LPPUv2q';
SET @sample_pdf = 'bec3f158a7f2e7c1958ec763fcde69f8cb84bd71fc76ebe9886610ed5a805499';

DROP TEMPORARY TABLE IF EXISTS scenario_people, scenario_events, scenario_entries, scenario_reviews, scenario_notices;
CREATE TEMPORARY TABLE scenario_people (
    username VARCHAR(48) PRIMARY KEY, full_name VARCHAR(60) NOT NULL, role_code VARCHAR(16) NOT NULL,
    number_suffix CHAR(2), verification VARCHAR(12), account_status VARCHAR(12) NOT NULL DEFAULT 'ACTIVE'
);
INSERT INTO scenario_people VALUES
    ('student07','顾言溪','STUDENT','07','VERIFIED','ACTIVE'),
    ('student08','程书宁','STUDENT','08','VERIFIED','ACTIVE'),
    ('student09','温静禾','STUDENT','09','VERIFIED','ACTIVE'),
    ('student10','陆行远','STUDENT','10','VERIFIED','ACTIVE'),
    ('student11','吴予安','STUDENT','11','VERIFIED','ACTIVE'),
    ('student12','薛知微','STUDENT','12','VERIFIED','ACTIVE'),
    ('student13','李清越','STUDENT','13','VERIFIED','ACTIVE'),
    ('student14','孟见山','STUDENT','14','VERIFIED','ACTIVE'),
    ('student15','唐思齐','STUDENT','15','VERIFIED','ACTIVE'),
    ('student16','徐明序','STUDENT','16','VERIFIED','ACTIVE'),
    ('student17','宋映竹','STUDENT','17','VERIFIED','ACTIVE'),
    ('student18','周惟一','STUDENT','18','VERIFIED','ACTIVE'),
    ('student19','谢之遥','STUDENT','19','PENDING','ACTIVE'),
    ('student20','罗嘉宁','STUDENT','20','REJECTED','ACTIVE'),
    ('student21','沈庭树','STUDENT','21','VERIFIED','DISABLED'),
    ('student22','丁意白','STUDENT','22','VERIFIED','DELETED'),
    ('judge03','季怀川','JUDGE',NULL,NULL,'ACTIVE'),
    ('judge04','虞晓岚','JUDGE',NULL,NULL,'ACTIVE'),
    ('judge05','秦嘉悦','JUDGE',NULL,NULL,'ACTIVE'),
    ('judge06','许成岳','JUDGE',NULL,NULL,'ACTIVE');

CREATE TEMPORARY TABLE scenario_events (
    code VARCHAR(16) PRIMARY KEY, title VARCHAR(120) NOT NULL, mode VARCHAR(12) NOT NULL,
    status VARCHAR(12) NOT NULL, judge_min SMALLINT NOT NULL,
    reg_open SMALLINT NOT NULL, reg_close SMALLINT NOT NULL, correction_close SMALLINT NOT NULL,
    qualification_close SMALLINT NOT NULL, work_open SMALLINT NOT NULL, work_close SMALLINT NOT NULL,
    judge_open SMALLINT NOT NULL, judge_close SMALLINT NOT NULL, rules_text TEXT NOT NULL
);
INSERT INTO scenario_events VALUES
    ('DEMO-S01','无纸化办事原型赛','INDIVIDUAL','DRAFT',1,3,12,14,16,18,26,27,34,'选择一个校内办事流程，提交匿名原型和一份可执行的验证方案。'),
    ('DEMO-S02','校园公共安全创意赛','TEAM','PUBLISHED',2,4,18,20,23,26,37,39,47,'观察校园公共空间，在不采集个人数据的前提下提出安全改进方案。'),
    ('DEMO-S03','校园数字服务创新赛','TEAM','PUBLISHED',2,-2,12,14,16,18,27,29,36,'围绕场地预约、服务导航或社团活动提出数字服务方案，团队限两至四人。'),
    ('DEMO-S04','无障碍导视设计赛','TEAM','PUBLISHED',1,-18,-2,3,6,8,20,21,29,'提出适合多类使用者的校园导视设计，提供动线验证与实施成本估算。'),
    ('DEMO-S05','节水实践方案赛','INDIVIDUAL','PUBLISHED',1,-20,-10,-8,-2,5,17,19,27,'对宿舍或教学楼的用水行为提出试点设计并说明测量方案。'),
    ('DEMO-S06','校内公益项目策划赛','TEAM','PUBLISHED',1,-26,-17,-15,-12,-6,9,11,20,'团队选择一个实际公益需求，提交活动计划、风险分析和复盘指标。'),
    ('DEMO-S07','共享学习空间改造赛','INDIVIDUAL','PUBLISHED',1,-35,-24,-22,-18,-14,-3,5,11,'为自习或讨论场景设计小范围空间改造方案，使用匿名 PDF 交件。'),
    ('DEMO-S08','校园节能监测挑战赛','TEAM','PUBLISHED',2,-42,-31,-29,-25,-21,-8,-4,12,'基于公共环境的可观察指标设计节能监测方案，作品接受匿名评审。'),
    ('DEMO-S09','绿色出行调研作品赛','INDIVIDUAL','PUBLISHED',1,-55,-42,-39,-35,-30,-15,-12,-4,'调查校园通勤选择，提出可测试的改善方案并说明数据来源。'),
    ('DEMO-S10','循环物资与共享设计赛','TEAM','PUBLISHED',1,-63,-51,-49,-45,-41,-27,-24,-15,'为校内闲置物品设计共享与流转流程，公告公布获奖作品及获奖团队。'),
    ('DEMO-S11','校园信息可视化创作赛','TEAM','CANCELLED',1,-3,9,11,14,17,25,27,34,'以校内服务数据为题进行信息设计，展示图表与说明。'),
    ('DEMO-S12','教学楼舒适度观察赛','INDIVIDUAL','ARCHIVED',1,-70,-56,-54,-50,-46,-33,-29,-21,'从光照、噪声或通风选一个问题，提交观察结果和改进建议。'),
    ('DEMO-S13','校园微调研写作赛','INDIVIDUAL','PUBLISHED',1,-2,15,17,20,23,35,37,46,'围绕一个校园服务问题撰写调研短文，提供观察记录和结论。');

CREATE TEMPORARY TABLE scenario_entries (
    no INT PRIMARY KEY, event_no CHAR(2) NOT NULL, captain VARCHAR(48) NOT NULL,
    partner VARCHAR(48), team VARCHAR(80), status VARCHAR(20) NOT NULL,
    work_title VARCHAR(120), excluded BOOLEAN NOT NULL DEFAULT FALSE
);
INSERT INTO scenario_entries VALUES
    (1,'03','student07','student08','星港服务组','DRAFT',NULL,FALSE),
    (2,'03','student10','student11','知行工作室','PENDING',NULL,FALSE),
    (3,'03','student12','student13','远山研究组','WITHDRAWN',NULL,FALSE),
    (4,'03','student18',NULL,'格点设计组','DRAFT',NULL,FALSE),
    (5,'04','student07','student09','晴窗导视组','PENDING',NULL,FALSE),
    (6,'04','student08','student10','引路设计组','CHANGES_REQUESTED',NULL,FALSE),
    (7,'04','student11','student12','方圆标识组','REJECTED',NULL,FALSE),
    (8,'04','student13','student14','山径设计组','APPROVED',NULL,FALSE),
    (9,'05','student15',NULL,NULL,'APPROVED',NULL,FALSE),
    (10,'05','student16',NULL,NULL,'EXPIRED',NULL,FALSE),
    (11,'06','student11','student12','星桥公益组','APPROVED','校园公益活动报名与回访机制',FALSE),
    (12,'06','student13','student14','向日服务组','APPROVED',NULL,FALSE),
    (13,'07','student07',NULL,NULL,'APPROVED','连廊自习与小组讨论空间分区',FALSE),
    (14,'08','student08','student09','节拍监测组','APPROVED','教学楼能耗监测与反馈看板',FALSE),
    (15,'08','student10','student11','栖光实验组','APPROVED','夜间教室照明巡检机制',FALSE),
    (16,'09','student12',NULL,NULL,'APPROVED','校内步行与骑行换乘点观察',FALSE),
    (17,'09','student13',NULL,NULL,'APPROVED','校门通勤流量记录',TRUE),
    (18,'09','student14',NULL,NULL,'APPROVED',NULL,FALSE),
    (19,'10','student15','student16','循环伙伴组','APPROVED','毕业季闲置物资共享站',FALSE),
    (20,'10','student17','student18','栖木设计组','APPROVED','宿舍物资互助清单',FALSE),
    (21,'12','student07',NULL,NULL,'APPROVED',NULL,FALSE),
    (22,'12','student08',NULL,NULL,'APPROVED','午间教室通风观察',TRUE),
    (23,'13','student18',NULL,NULL,'DRAFT',NULL,FALSE),
    (24,'13','student17',NULL,NULL,'PENDING',NULL,FALSE);

CREATE TEMPORARY TABLE scenario_reviews (
    entry_no INT NOT NULL, judge_name VARCHAR(48) NOT NULL, status VARCHAR(12) NOT NULL,
    score_value SMALLINT, reason VARCHAR(400), PRIMARY KEY (entry_no,judge_name)
);
INSERT INTO scenario_reviews VALUES
    (14,'judge03','ASSIGNED',NULL,NULL),
    (14,'judge04','SUBMITTED',90,NULL),
    (15,'judge03','ASSIGNED',NULL,NULL),
    (15,'judge05','RECUSED',NULL,'曾参与类似项目的前期指导，主动申请回避。'),
    (15,'judge06','REVOKED',NULL,'根据评审排班调整，该任务已收回。'),
    (16,'judge04','SUBMITTED',88,NULL),
    (19,'judge03','SUBMITTED',94,NULL),
    (20,'judge04','SUBMITTED',87,NULL);

CREATE TEMPORARY TABLE scenario_notices (
    event_no CHAR(2) PRIMARY KEY, no_awards BOOLEAN NOT NULL
);
INSERT INTO scenario_notices VALUES ('10',FALSE),('12',TRUE);

-- 第 2 步 更新基础示例登录名，入口账号和业务主键继续有效。
START TRANSACTION;
SET @sample_owner = (SELECT id FROM user_account WHERE username='demo.organizer');
SET @sample_admin = (SELECT id FROM user_account WHERE username='demo.admin');

UPDATE user_account a JOIN student_profile p ON p.user_id=a.id
JOIN user_role ur ON ur.user_id=a.id JOIN role role_row ON role_row.id=ur.role_id AND role_row.role_code='STUDENT'
LEFT JOIN user_account occupied ON occupied.username=SUBSTRING(a.username,6)
SET a.username=SUBSTRING(a.username,6)
WHERE a.username REGEXP '^demo[.]student0[1-6]$' AND p.claimed_student_no=CONCAT('D202600',RIGHT(a.username,2))
    AND occupied.id IS NULL;

UPDATE user_account a JOIN user_role ur ON ur.user_id=a.id
JOIN role role_row ON role_row.id=ur.role_id AND role_row.role_code='JUDGE'
LEFT JOIN student_profile p ON p.user_id=a.id
LEFT JOIN user_account occupied ON occupied.username=SUBSTRING(a.username,6)
SET a.username=SUBSTRING(a.username,6)
WHERE a.username IN ('demo.judge01','demo.judge02') AND a.password_hash=@sample_hash
    AND p.user_id IS NULL AND occupied.id IS NULL;

-- 第 3 步 先创建账号和赛事，再组装报名、名单及资格占用。
SET @add_scenarios = @sample_owner IS NOT NULL AND @sample_admin IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM competition WHERE competition_no='DEMO-S01'
);
INSERT INTO user_account(username,password_hash,staff_name,status,deleted_at)
SELECT username,@sample_hash,IF(role_code='JUDGE',full_name,NULL),account_status,
       IF(account_status='DELETED',@seed_at,NULL)
FROM scenario_people WHERE @add_scenarios ORDER BY username;

INSERT INTO user_role(user_id,role_id)
SELECT a.id,r.id FROM scenario_people d JOIN user_account a ON a.username=d.username JOIN role r ON r.role_code=d.role_code
WHERE @add_scenarios;

INSERT INTO student_profile(user_id,claimed_student_no,student_no,full_name,department,major,
    verification_status,verified_by,verified_at,invite_code)
SELECT a.id,CONCAT('D202600',d.number_suffix),IF(d.verification='VERIFIED',CONCAT('D202600',d.number_suffix),NULL),
       d.full_name,ELT(MOD(CAST(d.number_suffix AS UNSIGNED),4)+1,'计算机学院','设计学院','公共管理学院','环境学院'),
       ELT(MOD(CAST(d.number_suffix AS UNSIGNED),4)+1,'软件工程','视觉传达设计','公共事业管理','环境科学'),
       d.verification,IF(d.verification='PENDING',NULL,@sample_admin),
       IF(d.verification='PENDING',NULL,DATE_SUB(@seed_at,INTERVAL 90 DAY)),
       IF(d.verification='VERIFIED',REPLACE(UUID(),'-',''),NULL)
FROM scenario_people d JOIN user_account a ON a.username=d.username WHERE @add_scenarios AND d.role_code='STUDENT';

INSERT INTO competition(competition_no,name,edition,rules_text,organizer_id,entry_mode,min_team_size,max_team_size,
    registration_opens_at,registration_closes_at,correction_closes_at,qualification_closes_at,submission_opens_at,
    submission_closes_at,judging_opens_at,judging_closes_at,scoring_rubric,required_judges,status,cancellation_reason)
SELECT code,title,'校园实践季',rules_text,@sample_owner,mode,IF(mode='TEAM',2,1),IF(mode='TEAM',4,1),
       DATE_ADD(@seed_at,INTERVAL reg_open DAY),DATE_ADD(@seed_at,INTERVAL reg_close DAY),
       DATE_ADD(@seed_at,INTERVAL correction_close DAY),DATE_ADD(@seed_at,INTERVAL qualification_close DAY),
       DATE_ADD(@seed_at,INTERVAL work_open DAY),DATE_ADD(@seed_at,INTERVAL work_close DAY),
       DATE_ADD(@seed_at,INTERVAL judge_open DAY),DATE_ADD(@seed_at,INTERVAL judge_close DAY),
       JSON_ARRAY(JSON_OBJECT('id','evidence','label','问题证据','weight',30),
                  JSON_OBJECT('id','design','label','方案设计','weight',45),
                  JSON_OBJECT('id','delivery','label','实施可行性','weight',25)),
       judge_min,status,IF(status='CANCELLED','主办方调整了校内活动日程，本届赛事取消。',NULL)
FROM scenario_events WHERE @add_scenarios ORDER BY code;

INSERT INTO registration(registration_no,competition_id,captain_id,team_display_name,status,row_version,submitted_at)
SELECT CONCAT('DEMO-SR',LPAD(d.no,2,'0')),c.id,a.id,d.team,d.status,
       CASE d.status WHEN 'DRAFT' THEN 0 WHEN 'PENDING' THEN 1 ELSE 2 END,
       IF(d.status='DRAFT',NULL,LEAST(DATE_SUB(@seed_at,INTERVAL 1 DAY),DATE_SUB(c.registration_closes_at,INTERVAL 1 DAY)))
FROM scenario_entries d JOIN competition c ON c.competition_no=CONCAT('DEMO-S',d.event_no)
JOIN user_account a ON a.username=d.captain WHERE @add_scenarios;

INSERT INTO registration_member(registration_id,student_id,is_captain,invitation_status,confirmed_at)
SELECT r.id,r.captain_id,TRUE,'CONFIRMED',COALESCE(DATE_SUB(r.submitted_at,INTERVAL 2 HOUR),@seed_at)
FROM scenario_entries d JOIN registration r ON r.registration_no=CONCAT('DEMO-SR',LPAD(d.no,2,'0'))
WHERE @add_scenarios;

INSERT INTO registration_member(registration_id,student_id,is_captain,invitation_status,confirmed_at)
SELECT r.id,a.id,FALSE,IF(d.status='DRAFT','INVITED','CONFIRMED'),DATE_SUB(r.submitted_at,INTERVAL 1 HOUR)
FROM scenario_entries d JOIN registration r ON r.registration_no=CONCAT('DEMO-SR',LPAD(d.no,2,'0'))
JOIN user_account a ON a.username=d.partner WHERE @add_scenarios;

INSERT INTO registration_member(registration_id,student_id,is_captain,invitation_status)
SELECT r.id,a.id,FALSE,CASE a.username WHEN 'student09' THEN 'DECLINED' ELSE 'REMOVED' END
FROM registration r JOIN user_account a ON a.username IN ('student09','student10')
WHERE @add_scenarios AND r.registration_no='DEMO-SR01';

INSERT INTO participation_claim(competition_id,student_id,registration_id,claimed_at)
SELECT r.competition_id,m.student_id,r.id,r.submitted_at
FROM scenario_entries d JOIN registration r ON r.registration_no=CONCAT('DEMO-SR',LPAD(d.no,2,'0'))
JOIN registration_member m ON m.registration_id=r.id AND m.invitation_status='CONFIRMED'
WHERE @add_scenarios AND r.status IN ('PENDING','CHANGES_REQUESTED','APPROVED');

INSERT INTO registration_review_log(registration_id,actor_id,from_status,to_status,roster_snapshot,created_at)
SELECT r.id,r.captain_id,'DRAFT','PENDING',
       (SELECT JSON_ARRAYAGG(JSON_OBJECT('studentId',m.student_id,'fullName',p.full_name,'isCaptain',m.is_captain))
        FROM registration_member m JOIN student_profile p ON p.user_id=m.student_id
        WHERE m.registration_id=r.id AND m.invitation_status='CONFIRMED'),
       r.submitted_at
FROM scenario_entries d JOIN registration r ON r.registration_no=CONCAT('DEMO-SR',LPAD(d.no,2,'0'))
WHERE @add_scenarios AND r.submitted_at IS NOT NULL;

INSERT INTO registration_review_log(registration_id,actor_id,from_status,to_status,reason,created_at)
SELECT r.id,CASE r.status WHEN 'EXPIRED' THEN NULL WHEN 'WITHDRAWN' THEN r.captain_id ELSE @sample_owner END,
       'PENDING',r.status,
       CASE r.status WHEN 'APPROVED' THEN '参赛信息及团队成员资格已核对。'
            WHEN 'CHANGES_REQUESTED' THEN '请补充队伍联系人分工和校内调研安排。'
            WHEN 'REJECTED' THEN '参赛声明中的原创性材料不符合本届要求。'
            WHEN 'WITHDRAWN' THEN '报名负责人撤回。'
            WHEN 'EXPIRED' THEN '资格审核截止时尚未通过。' END,
       IF(r.status='EXPIRED',c.qualification_closes_at,DATE_ADD(r.submitted_at,INTERVAL 6 HOUR))
FROM scenario_entries d JOIN registration r ON r.registration_no=CONCAT('DEMO-SR',LPAD(d.no,2,'0'))
JOIN competition c ON c.id=r.competition_id
WHERE @add_scenarios AND r.status IN ('APPROVED','CHANGES_REQUESTED','REJECTED','WITHDRAWN','EXPIRED');

-- 第 4 步 生成作品版本和评审任务，已截止的作品冻结用于评审。
INSERT INTO submission(registration_id,anonymous_code,eligibility_status,eligibility_reason)
SELECT r.id,CONCAT('A-SCENE-',LPAD(d.no,3,'0')),IF(d.excluded,'EXCLUDED','ELIGIBLE'),
       IF(d.excluded,'作品缺少可公开引用的授权说明。',NULL)
FROM scenario_entries d JOIN registration r ON r.registration_no=CONCAT('DEMO-SR',LPAD(d.no,2,'0'))
WHERE @add_scenarios AND d.work_title IS NOT NULL;

INSERT INTO submission_version(submission_id,version_no,request_id,request_hash,title,storage_key,file_sha256,submitted_by,submitted_at)
SELECT s.id,1,CONCAT('DEMO-WORK-S',d.no),SHA2(CONCAT(@sample_pdf,CHAR(10),d.work_title),256),d.work_title,
       CONCAT(LPAD(CONCAT('d3b0',d.no),32,'0'),'.pdf'),@sample_pdf,r.captain_id,DATE_ADD(c.submission_opens_at,INTERVAL 1 DAY)
FROM scenario_entries d JOIN registration r ON r.registration_no=CONCAT('DEMO-SR',LPAD(d.no,2,'0'))
JOIN submission s ON s.registration_id=r.id JOIN competition c ON c.id=r.competition_id WHERE @add_scenarios;

UPDATE submission s JOIN registration r ON r.id=s.registration_id
JOIN scenario_entries d ON r.registration_no=CONCAT('DEMO-SR',LPAD(d.no,2,'0'))
JOIN competition c ON c.id=r.competition_id JOIN submission_version v ON v.submission_id=s.id AND v.version_no=1
SET s.locked_version_id=v.id,s.locked_at=c.submission_closes_at,s.row_version=1
WHERE @add_scenarios AND c.submission_closes_at<=@seed_at;

INSERT INTO review_assignment(submission_version_id,judge_id,status,reason,assigned_at)
SELECT s.locked_version_id,a.id,d.status,d.reason,DATE_ADD(c.judging_opens_at,INTERVAL 1 HOUR)
FROM scenario_reviews d JOIN registration r ON r.registration_no=CONCAT('DEMO-SR',LPAD(d.entry_no,2,'0'))
JOIN submission s ON s.registration_id=r.id JOIN competition c ON c.id=r.competition_id
JOIN user_account a ON a.username=d.judge_name WHERE @add_scenarios;

INSERT INTO review_score(assignment_id,criterion_scores,feedback,row_version,submitted_at)
SELECT ra.id,JSON_OBJECT('evidence',d.score_value,'design',d.score_value,'delivery',d.score_value),
       '方案聚焦明确，材料能支撑主要结论。',2,DATE_ADD(ra.assigned_at,INTERVAL 1 HOUR)
FROM scenario_reviews d JOIN registration r ON r.registration_no=CONCAT('DEMO-SR',LPAD(d.entry_no,2,'0'))
JOIN submission s ON s.registration_id=r.id JOIN user_account a ON a.username=d.judge_name
JOIN review_assignment ra ON ra.submission_version_id=s.locked_version_id AND ra.judge_id=a.id
WHERE @add_scenarios AND d.status='SUBMITTED';

-- 第 5 步 保存公告批次和获奖快照，分别覆盖有获奖和无获奖。
INSERT INTO result_publication(competition_id,version_no,published_by,published_at,publication_type,no_awards,
    request_id,request_hash,scoring_basis)
SELECT c.id,1,@sample_owner,DATE_ADD(c.judging_closes_at,INTERVAL 1 DAY),'FIRST',n.no_awards,
       CONCAT('DEMO-RESULT-',c.competition_no),SHA2(c.competition_no,256),
       (SELECT JSON_ARRAYAGG(JSON_OBJECT('registrationId',r.id,'lockedVersionId',s.locked_version_id,
                    'outcomeStatus',CASE WHEN s.id IS NULL THEN 'NO_WORK' WHEN s.eligibility_status='EXCLUDED' THEN 'EXCLUDED' ELSE 'RANKED' END,
                    'scores',IF(rs.assignment_id IS NULL,JSON_ARRAY(),JSON_ARRAY(JSON_OBJECT('assignmentId',ra.id,
                         'rowVersion',rs.row_version,'criterionScores',rs.criterion_scores)))))
        FROM registration r LEFT JOIN submission s ON s.registration_id=r.id
        LEFT JOIN review_assignment ra ON ra.submission_version_id=s.locked_version_id AND ra.status='SUBMITTED'
        LEFT JOIN review_score rs ON rs.assignment_id=ra.id WHERE r.competition_id=c.id)
FROM scenario_notices n JOIN competition c ON c.competition_no=CONCAT('DEMO-S',n.event_no)
WHERE @add_scenarios;

INSERT INTO competition_result(publication_id,registration_id,competition_id,work_title_snapshot,entry_display_snapshot,
    outcome_status,outcome_reason,total_score,rank_no,award_name,recipient_names_snapshot)
SELECT publication.id,r.id,c.id,d.work_title,COALESCE(d.team,s.anonymous_code,r.registration_no),
       CASE WHEN s.id IS NULL THEN 'NO_WORK' WHEN d.excluded THEN 'EXCLUDED' ELSE 'RANKED' END,
       IF(d.excluded,s.eligibility_reason,NULL),
       CASE d.no WHEN 19 THEN 94 WHEN 20 THEN 87 END,CASE d.no WHEN 19 THEN 1 WHEN 20 THEN 2 END,
       CASE d.no WHEN 19 THEN '一等奖' WHEN 20 THEN '二等奖' END,
       CASE WHEN d.no IN (19,20) THEN
           (SELECT JSON_ARRAYAGG(p.full_name) FROM registration_member m JOIN student_profile p ON p.user_id=m.student_id
            WHERE m.registration_id=r.id AND m.invitation_status='CONFIRMED') END
FROM scenario_entries d JOIN registration r ON r.registration_no=CONCAT('DEMO-SR',LPAD(d.no,2,'0'))
JOIN competition c ON c.id=r.competition_id
JOIN scenario_notices n ON n.event_no=d.event_no
JOIN result_publication publication ON publication.competition_id=c.id AND publication.version_no=1
LEFT JOIN submission s ON s.registration_id=r.id WHERE @add_scenarios;

UPDATE competition c JOIN scenario_notices n ON c.competition_no=CONCAT('DEMO-S',n.event_no)
JOIN result_publication p ON p.competition_id=c.id AND p.version_no=1
SET c.current_publication_id=p.id,c.row_version=1 WHERE @add_scenarios;

INSERT INTO audit_log(actor_id,action,target_type,target_key,change_summary,created_at)
SELECT @sample_owner,'RESULT_FIRST','COMPETITION',CAST(c.id AS CHAR),
       JSON_OBJECT('publicationId',p.id),p.published_at
FROM scenario_notices n JOIN competition c ON c.competition_no=CONCAT('DEMO-S',n.event_no)
JOIN result_publication p ON p.id=c.current_publication_id WHERE @add_scenarios;

COMMIT;
