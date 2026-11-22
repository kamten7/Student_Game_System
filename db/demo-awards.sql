-- 补充有获奖人员和团队名单的演示赛事。先导入基础演示数据和公告名单迁移。
USE student_game_system;
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';
SET @demo_date = UTC_TIMESTAMP(6);
SET @demo_owner = (SELECT id FROM user_account WHERE username='demo.organizer');
SET @demo_pdf_sha = 'bec3f158a7f2e7c1958ec763fcde69f8cb84bd71fc76ebe9886610ed5a805499';

CREATE TEMPORARY TABLE demo_award_events (
    code VARCHAR(16) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    mode VARCHAR(12) NOT NULL,
    days_ago INT NOT NULL,
    rules_text TEXT NOT NULL
);
INSERT INTO demo_award_events VALUES
    ('DEMO-11', '校园数据可视化赛', 'INDIVIDUAL', 18, '利用校园服务公开数据制作图表和叙事作品，说明数据来源、设计取舍和主要结论。'),
    ('DEMO-12', '校园公共空间微更新赛', 'TEAM', 8, '两人组队观察公共空间的使用问题，结合用户访谈提出微更新设计和实施计划。');

CREATE TEMPORARY TABLE demo_award_rows (
    registration_no VARCHAR(32) PRIMARY KEY,
    competition_no VARCHAR(16) NOT NULL,
    captain_username VARCHAR(48) NOT NULL,
    partner_username VARCHAR(48),
    team_name VARCHAR(80),
    display_name VARCHAR(80) NOT NULL,
    recipients JSON NOT NULL,
    title VARCHAR(120) NOT NULL,
    judge_username VARCHAR(48) NOT NULL,
    score INT NOT NULL,
    rank_no INT NOT NULL,
    prize VARCHAR(60) NOT NULL,
    file_no INT NOT NULL
);
INSERT INTO demo_award_rows VALUES
    ('DEMO-R14','DEMO-11','demo.student01',NULL,NULL,'个人参赛',JSON_ARRAY('林予'),'一天中的校园出行节奏','demo.judge01',94,1,'一等奖',8),
    ('DEMO-R15','DEMO-11','demo.student03',NULL,NULL,'个人参赛',JSON_ARRAY('陈曦'),'图书馆借阅主题地图','demo.judge02',91,2,'二等奖',9),
    ('DEMO-R16','DEMO-11','demo.student05',NULL,NULL,'个人参赛',JSON_ARRAY('方屿'),'校园服务等待时间观察','demo.judge01',88,3,'三等奖',10),
    ('DEMO-R17','DEMO-12','demo.student02','demo.student04','折线设计组','折线设计组',JSON_ARRAY('周禾','许宁'),'雨天连廊停留节点设计','demo.judge02',93,1,'一等奖',11),
    ('DEMO-R18','DEMO-12','demo.student01','demo.student05','邻里空间组','邻里空间组',JSON_ARRAY('林予','方屿'),'共享学习角与夜间导视','demo.judge01',89,2,'二等奖',12);

START TRANSACTION;

INSERT INTO competition(competition_no,name,edition,rules_text,organizer_id,entry_mode,min_team_size,max_team_size,
    registration_opens_at,registration_closes_at,correction_closes_at,qualification_closes_at,
    submission_opens_at,submission_closes_at,judging_opens_at,judging_closes_at,scoring_rubric,required_judges,status)
SELECT code,name,'校园开放季',rules_text,@demo_owner,mode,IF(mode='TEAM',2,1),IF(mode='TEAM',2,1),
       DATE_SUB(@demo_date,INTERVAL (days_ago+27) DAY),
       DATE_SUB(@demo_date,INTERVAL (days_ago+21) DAY),
       DATE_SUB(@demo_date,INTERVAL (days_ago+19) DAY),
       DATE_SUB(@demo_date,INTERVAL (days_ago+17) DAY),
       DATE_SUB(@demo_date,INTERVAL (days_ago+16) DAY),
       DATE_SUB(@demo_date,INTERVAL (days_ago+9) DAY),
       DATE_SUB(@demo_date,INTERVAL (days_ago+8) DAY),
       DATE_SUB(@demo_date,INTERVAL days_ago DAY),
       JSON_ARRAY(JSON_OBJECT('id','insight','label','问题洞察','weight',40),
           JSON_OBJECT('id','proposal','label','方案质量','weight',35),
           JSON_OBJECT('id','delivery','label','落地可行性','weight',25)),
       1,'PUBLISHED'
FROM demo_award_events ORDER BY code;

INSERT INTO registration(registration_no,competition_id,captain_id,team_display_name,status,row_version,submitted_at)
SELECT d.registration_no,c.id,a.id,d.team_name,'APPROVED',2,DATE_SUB(c.registration_closes_at,INTERVAL 1 DAY)
FROM demo_award_rows d JOIN competition c ON c.competition_no=d.competition_no
JOIN user_account a ON a.username=d.captain_username;

INSERT INTO registration_member(registration_id,student_id,is_captain,invitation_status,confirmed_at)
SELECT r.id,r.captain_id,TRUE,'CONFIRMED',DATE_SUB(r.submitted_at,INTERVAL 2 HOUR)
FROM demo_award_rows d JOIN registration r ON r.registration_no=d.registration_no;

INSERT INTO registration_member(registration_id,student_id,is_captain,invitation_status,confirmed_at)
SELECT r.id,a.id,FALSE,'CONFIRMED',DATE_SUB(r.submitted_at,INTERVAL 1 HOUR)
FROM demo_award_rows d JOIN registration r ON r.registration_no=d.registration_no
JOIN user_account a ON a.username=d.partner_username;

INSERT INTO participation_claim(competition_id,student_id,registration_id,claimed_at)
SELECT r.competition_id,m.student_id,r.id,r.submitted_at
FROM demo_award_rows d JOIN registration r ON r.registration_no=d.registration_no
JOIN registration_member m ON m.registration_id=r.id;

INSERT INTO registration_review_log(registration_id,actor_id,from_status,to_status,roster_snapshot,created_at)
SELECT r.id,r.captain_id,'DRAFT','PENDING',
       (SELECT JSON_ARRAYAGG(JSON_OBJECT('studentId',m.student_id,'fullName',p.full_name,'isCaptain',m.is_captain))
        FROM registration_member m JOIN student_profile p ON p.user_id=m.student_id WHERE m.registration_id=r.id),
       r.submitted_at
FROM demo_award_rows d JOIN registration r ON r.registration_no=d.registration_no;

INSERT INTO registration_review_log(registration_id,actor_id,from_status,to_status,reason,created_at)
SELECT r.id,@demo_owner,'PENDING','APPROVED','已完成参赛资格审核。',DATE_ADD(r.submitted_at,INTERVAL 1 DAY)
FROM demo_award_rows d JOIN registration r ON r.registration_no=d.registration_no;

INSERT INTO submission(registration_id,anonymous_code)
SELECT r.id,CONCAT('A-DEMO-',LPAD(d.file_no+6,3,'0'))
FROM demo_award_rows d JOIN registration r ON r.registration_no=d.registration_no;

INSERT INTO submission_version(submission_id,version_no,request_id,request_hash,title,storage_key,file_sha256,submitted_by,submitted_at)
SELECT s.id,1,CONCAT('DEMO-WORK-',d.file_no),SHA2(CONCAT(@demo_pdf_sha,CHAR(10),d.title),256),d.title,
       CONCAT(LPAD(CONCAT('d3a0',d.file_no),32,'0'),'.pdf'),@demo_pdf_sha,r.captain_id,
       DATE_ADD(c.submission_opens_at,INTERVAL 1 DAY)
FROM demo_award_rows d JOIN registration r ON r.registration_no=d.registration_no
JOIN submission s ON s.registration_id=r.id JOIN competition c ON c.id=r.competition_id;

UPDATE submission s JOIN registration r ON r.id=s.registration_id
JOIN demo_award_rows d ON d.registration_no=r.registration_no
JOIN competition c ON c.id=r.competition_id JOIN submission_version v ON v.submission_id=s.id AND v.version_no=1
SET s.locked_version_id=v.id,s.locked_at=c.submission_closes_at,s.row_version=1;

INSERT INTO review_assignment(submission_version_id,judge_id,status,assigned_at)
SELECT s.locked_version_id,a.id,'SUBMITTED',DATE_ADD(c.judging_opens_at,INTERVAL 1 HOUR)
FROM demo_award_rows d JOIN registration r ON r.registration_no=d.registration_no
JOIN submission s ON s.registration_id=r.id JOIN competition c ON c.id=r.competition_id
JOIN user_account a ON a.username=d.judge_username;

INSERT INTO review_score(assignment_id,criterion_scores,feedback,row_version,submitted_at)
SELECT ra.id,JSON_OBJECT('insight',d.score,'proposal',d.score,'delivery',d.score),'选题明确，资料能够支持所提出的方案。',2,
       DATE_ADD(ra.assigned_at,INTERVAL 1 DAY)
FROM demo_award_rows d JOIN registration r ON r.registration_no=d.registration_no
JOIN submission s ON s.registration_id=r.id JOIN review_assignment ra ON ra.submission_version_id=s.locked_version_id;

INSERT INTO result_publication(competition_id,version_no,published_by,published_at,publication_type,no_awards,request_id,request_hash,scoring_basis)
SELECT c.id,1,@demo_owner,DATE_ADD(c.judging_closes_at,INTERVAL 1 DAY),'FIRST',FALSE,
       CONCAT('DEMO-RESULT-',c.competition_no),SHA2(c.competition_no,256),
       (SELECT JSON_ARRAYAGG(JSON_OBJECT('registrationId',r.id,'lockedVersionId',s.locked_version_id,'outcomeStatus','RANKED','scores',
           JSON_ARRAY(JSON_OBJECT('assignmentId',ra.id,'rowVersion',rs.row_version,'criterionScores',rs.criterion_scores))))
        FROM registration r JOIN submission s ON s.registration_id=r.id
        JOIN review_assignment ra ON ra.submission_version_id=s.locked_version_id
        JOIN review_score rs ON rs.assignment_id=ra.id WHERE r.competition_id=c.id)
FROM demo_award_events d JOIN competition c ON c.competition_no=d.code;

INSERT INTO competition_result(publication_id,registration_id,competition_id,work_title_snapshot,entry_display_snapshot,
    outcome_status,total_score,rank_no,award_name,recipient_names_snapshot)
SELECT p.id,r.id,c.id,d.title,d.display_name,'RANKED',d.score,d.rank_no,d.prize,d.recipients
FROM demo_award_rows d JOIN registration r ON r.registration_no=d.registration_no
JOIN competition c ON c.id=r.competition_id
JOIN result_publication p ON p.competition_id=c.id AND p.request_id=CONCAT('DEMO-RESULT-',c.competition_no);

UPDATE competition c JOIN demo_award_events d ON d.code=c.competition_no
JOIN result_publication p ON p.competition_id=c.id AND p.request_id=CONCAT('DEMO-RESULT-',c.competition_no)
SET c.current_publication_id=p.id,c.row_version=1;

-- 只给仍使用原始演示公告的赛事追加名单批次，保留旧快照和手工更正。
SET @original_batch = (SELECT p.id FROM competition c JOIN result_publication p ON p.id=c.current_publication_id
    WHERE c.competition_no='DEMO-06' AND p.request_id='DEMO-RESULT-DEMO-06' AND p.version_no=1);
INSERT INTO result_publication(competition_id,version_no,published_by,published_at,correction_reason,
    publication_type,no_awards,request_id,request_hash,scoring_basis)
SELECT competition_id,version_no+1,@demo_owner,@demo_date,'补充已确认可公开的获奖人员名单。','CORRECTION',
       no_awards,'DEMO-RESULT-DEMO-06-NAMES',SHA2('DEMO-RESULT-DEMO-06-NAMES',256),scoring_basis
FROM result_publication WHERE id=@original_batch;

SET @names_batch = (SELECT id FROM result_publication WHERE request_id='DEMO-RESULT-DEMO-06-NAMES');
INSERT INTO competition_result(publication_id,registration_id,competition_id,work_title_snapshot,entry_display_snapshot,
    outcome_status,outcome_reason,total_score,rank_no,award_name,recipient_names_snapshot)
SELECT @names_batch,x.registration_id,x.competition_id,x.work_title_snapshot,x.entry_display_snapshot,
       x.outcome_status,x.outcome_reason,x.total_score,x.rank_no,x.award_name,
       CASE r.registration_no WHEN 'DEMO-R11' THEN JSON_ARRAY('林予','陈曦')
            WHEN 'DEMO-R12' THEN JSON_ARRAY('周禾','方屿') END
FROM competition_result x JOIN registration r ON r.id=x.registration_id WHERE x.publication_id=@original_batch;

UPDATE competition SET current_publication_id=@names_batch,row_version=row_version+1
WHERE competition_no='DEMO-06' AND current_publication_id=@original_batch;

INSERT INTO audit_log(actor_id,action,target_type,target_key,change_summary,created_at)
SELECT @demo_owner,IF(p.publication_type='FIRST','RESULT_FIRST','RESULT_CORRECTION'),'COMPETITION',
       CAST(c.id AS CHAR),JSON_OBJECT('publicationId',p.id),p.published_at
FROM result_publication p JOIN competition c ON c.id=p.competition_id
WHERE p.request_id IN ('DEMO-RESULT-DEMO-11','DEMO-RESULT-DEMO-12')
   OR (p.id=@names_batch AND @original_batch IS NOT NULL);

COMMIT;
