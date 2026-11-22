package org.jingxu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jingxu.filter.AccessInterceptor;
import org.jingxu.pojo.Actor;
import org.jingxu.service.AccountService;
import org.jingxu.service.CompetitionService;
import org.jingxu.service.RegistrationService;
import org.jingxu.service.ResultService;
import org.jingxu.service.ReviewService;
import org.jingxu.service.SubmissionService;
import org.jingxu.util.AuditDisplay;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.jingxu.filter.AccessInterceptor.actor;
import static org.jingxu.util.Data.*;

/** 组装公开端、学生端、评委端和管理端的 JSP 页面数据。 */
@Controller
public class PageController {
    private static final Map<String, String> LABELS = Map.ofEntries(
            entry("DRAFT", "草稿"), entry("UPCOMING", "即将开放"), entry("REGISTRATION", "报名中"),
            entry("QUALIFICATION", "资格审核"), entry("AWAITING_SUBMISSION", "等待交件"),
            entry("SUBMISSION", "作品提交中"), entry("AWAITING_JUDGING", "等待评审"), entry("JUDGING", "评审中"),
            entry("AWAITING_RESULTS", "等待公布"), entry("RESULTS", "结果已发布"), entry("ARCHIVED", "已归档"),
            entry("PUBLISHED", "已发布"), entry("CANCELLED", "已取消"), entry("PENDING", "待处理"),
            entry("APPROVED", "审核通过"), entry("CHANGES_REQUESTED", "待补正"), entry("REJECTED", "未通过"),
            entry("WITHDRAWN", "已撤回"), entry("EXPIRED", "逾期未通过"), entry("VERIFIED", "已核验"),
            entry("CONFIRMED", "已确认"), entry("INVITED", "等待确认"), entry("DECLINED", "已拒绝"),
            entry("ACTIVE", "正常"), entry("DISABLED", "已停用"), entry("DELETED", "已删除"),
            entry("STUDENT", "学生"), entry("JUDGE", "评委"), entry("ORGANIZER", "赛事负责人"), entry("ADMIN", "平台管理员"),
            entry("TEAM", "团队赛"), entry("INDIVIDUAL", "个人赛"), entry("NO_WORK", "未提交作品"),
            entry("RANKED", "已排名"), entry("EXCLUDED", "取消参评"), entry("ELIGIBLE", "符合参评条件"),
            entry("ASSIGNED", "待评分"), entry("SUBMITTED", "已提交"), entry("RECUSED", "已回避"), entry("REVOKED", "已撤销"));
    private final AccountService accounts;
    private final CompetitionService competitions;
    private final RegistrationService registrations;
    private final SubmissionService submissions;
    private final ReviewService reviews;
    private final ResultService results;
    /** 接入四端页面所需的领域服务。 */
    public PageController(AccountService accounts, CompetitionService competitions, RegistrationService registrations,
                          SubmissionService submissions, ReviewService reviews, ResultService results) {
        this.accounts = accounts;
        this.competitions = competitions;
        this.registrations = registrations;
        this.submissions = submissions;
        this.reviews = reviews;
        this.results = results;
    }

    /** 为所有视图提供中文标签和表单校验令牌。 */
    @ModelAttribute
    public void common(HttpServletRequest request, Model model) {
        model.addAttribute("csrfToken", AccessInterceptor.token(request));
        model.addAttribute("labels", LABELS);
    }

    /** 按筛选条件打开公开赛事大厅。 */
    @GetMapping("/")
    public String home(@RequestParam Map<String, String> query, Model model) {
        model.addAttribute("events", competitions.list(null, query));
        model.addAttribute("query", query);
        return "home";
    }

    /** 展示公开赛事详情、状态和赛程。 */
    @GetMapping("/competitions/{id}")
    public String competition(@PathVariable long id, Model model) {
        model.addAttribute("event", competitions.publicView(id));
        return "competition";
    }

    /** 展示单场赛事的当前获奖公告。 */
    @GetMapping("/competitions/{id}/announcement")
    public String announcement(@PathVariable long id, Model model) {
        model.addAttribute("event", competitions.publicView(id));
        model.addAttribute("announcement", competitions.announcement(id));
        return "announcement";
    }

    /** 展示已发布的获奖公告列表。 */
    @GetMapping("/announcements")
    public String announcements(@RequestParam Map<String, String> query, Model model) {
        model.addAttribute("announcements", competitions.announcements(query));
        return "announcements";
    }

    /** 根据入口显示登录或学生注册表单。 */
    @GetMapping({"/login", "/register"})
    public String auth(HttpServletRequest request, Model model) {
        model.addAttribute("register", request.getServletPath().equals("/register"));
        return "auth";
    }

    /** 加载学生的报名、邀请和核验提示。 */
    @GetMapping("/student")
    public String student(HttpServletRequest request, @RequestParam Map<String, String> query, Model model) {
        Actor user = actor(request);
        studentArea(model);
        model.addAttribute("profile", accounts.profile(user));
        model.addAttribute("registrations", registrations.studentList(user, query));
        model.addAttribute("invitations", registrations.invitations(user));
        return "student";
    }

    /** 打开当前学生的个人资料。 */
    @GetMapping("/student/profile")
    public String profile(HttpServletRequest request, Model model) {
        studentArea(model);
        model.addAttribute("profile", accounts.profile(actor(request)));
        return "profile";
    }

    /** 加载报名状态、成员、文件版本和个人结果。 */
    @GetMapping("/student/registrations/{id}")
    public String registration(HttpServletRequest request, @PathVariable long id, Model model) {
        Actor user = actor(request);
        studentArea(model);
        Map<String, Object> registration = registrations.studentDetail(user, id);
        Map<String, Object> event = competitions.publicView(number(registration, "competitionId"));
        model.addAttribute("registration", registration);
        model.addAttribute("event", event);
        model.addAttribute("captain", user.id() == number(registration, "captainId"));
        model.addAttribute("requestId", identifier("REQ-"));
        model.addAttribute("submission", submissions.studentView(user, id));
        if (Boolean.TRUE.equals(event.get("hasAnnouncement")) && "APPROVED".equals(registration.get("status"))) {
            model.addAttribute("result", registrations.result(user, id));
        }
        return "registration";
    }

    /** 展示当前账号能管理的赛事。 */
    @GetMapping("/admin")
    public String admin(HttpServletRequest request, @RequestParam Map<String, String> query, Model model) {
        adminArea(model);
        model.addAttribute("events", competitions.list(actor(request), query));
        return "admin";
    }

    /** 为新建赛事或编辑草稿准备表单。 */
    @GetMapping({"/admin/competitions/new", "/admin/competitions/{id}/edit"})
    public String eventForm(HttpServletRequest request, @PathVariable(required = false) Long id, Model model) {
        adminArea(model);
        if (id != null) {
            Map<String, Object> event = competitions.adminView(actor(request), id);
            model.addAttribute("event", event);
            model.addAttribute("criteria", event.get("scoringRubric"));
        } else {
            model.addAttribute("criteria", List.of(fields("id", "proposal", "label", "方案质量", "weight", 70),
                    fields("id", "delivery", "label", "实施可行性", "weight", 30)));
        }
        model.addAttribute("dateFields", List.of("报名开始", "报名截止", "补正截止", "资格审核截止",
                "作品提交开始", "作品提交截止", "评审开始", "评审截止"));
        model.addAttribute("dateKeys", CompetitionService.DATES);
        return "competition-form";
    }

    /** 加载赛事概览和报名审核列表。 */
    @GetMapping("/admin/competitions/{id}")
    public String manageEvent(HttpServletRequest request, @PathVariable long id,
                              @RequestParam Map<String, String> query, Model model) {
        Actor user = actor(request);
        adminArea(model);
        model.addAttribute("event", competitions.overview(user, id));
        model.addAttribute("registrations", registrations.adminList(user, id, query));
        model.addAttribute("query", query);
        return "admin-event";
    }

    /** 展示管理端的名单快照和报名审核历史。 */
    @GetMapping("/admin/registrations/{id}")
    public String reviewRegistration(HttpServletRequest request, @PathVariable long id, Model model) {
        Actor user = actor(request);
        adminArea(model);
        Map<String, Object> registration = registrations.adminDetail(user, id);
        model.addAttribute("registration", registration);
        model.addAttribute("event", competitions.adminView(user, number(registration, "competitionId")));
        return "admin-registration";
    }

    /** 展示分派给当前评委的任务。 */
    @GetMapping("/judge")
    public String judge(HttpServletRequest request, @RequestParam Map<String, String> query, Model model) {
        model.addAttribute("area", "judge");
        model.addAttribute("assignments", reviews.judgeList(actor(request), query));
        return "judge";
    }

    /** 打开评委可访问的匿名作品和评分表。 */
    @GetMapping("/judge/assignments/{id}")
    public String judgeTask(HttpServletRequest request, @PathVariable long id, Model model) {
        model.addAttribute("area", "judge");
        model.addAttribute("assignment", reviews.judgeView(actor(request), id));
        return "judge-task";
    }

    /** 列出赛事作品、参评状态和有效评分数。 */
    @GetMapping("/admin/competitions/{id}/works")
    public String works(HttpServletRequest request, @PathVariable long id,
                        @RequestParam Map<String, String> query, Model model) {
        Actor user = actor(request);
        adminArea(model);
        model.addAttribute("event", competitions.adminView(user, id));
        model.addAttribute("works", submissions.list(user, id, query));
        return "admin-works";
    }

    /** 加载作品版本、评委分派记录及候选人。 */
    @GetMapping("/admin/submissions/{id}")
    public String manageWork(HttpServletRequest request, @PathVariable long id, Model model) {
        Actor user = actor(request);
        adminArea(model);
        Map<String, Object> work = submissions.adminView(user, id);
        long eventId = number(work, "competitionId");
        model.addAttribute("work", work);
        model.addAttribute("event", competitions.adminView(user, eventId));
        model.addAttribute("assignments", reviews.assignments(user, id));
        model.addAttribute("candidates", reviews.candidates(user, eventId));
        return "admin-work";
    }

    /** 为赛事负责人准备发布或更正结果的页面。 */
    @GetMapping("/admin/competitions/{id}/results")
    public String results(HttpServletRequest request, @PathVariable long id, Model model) {
        Actor user = actor(request);
        adminArea(model);
        Map<String, Object> event = competitions.adminView(user, id);
        Object baseId = event.get("currentPublicationId");
        model.addAttribute("event", event);
        model.addAttribute("requestId", identifier("PUB-"));
        model.addAttribute("batches", results.batches(user, id));
        model.addAttribute("preview", results.preview(user, id, baseId == null
                ? fields("mode", "FIRST")
                : fields("mode", "CORRECTION", "currentPublicationId", baseId, "reason", "结果核对")));
        return "admin-results";
    }

    /** 展示指定发布批次的历史结果。 */
    @GetMapping("/admin/competitions/{id}/results/{publicationId}")
    public String resultBatch(HttpServletRequest request, @PathVariable long id, @PathVariable long publicationId, Model model) {
        adminArea(model);
        model.addAttribute("batch", results.batch(actor(request), id, publicationId));
        return "admin-result-batch";
    }

    /** 按筛选条件展示账号、角色和核验状态。 */
    @GetMapping("/admin/accounts")
    public String adminAccounts(@RequestParam Map<String, String> query, Model model) {
        adminArea(model);
        model.addAttribute("accounts", accounts.list(query));
        model.addAttribute("query", query);
        return "admin-accounts";
    }

    /** 展示单个账号的角色、姓名和身份管理入口。 */
    @GetMapping("/admin/accounts/{id}")
    public String adminAccount(@PathVariable long id, Model model) {
        adminArea(model);
        model.addAttribute("account", accounts.adminAccount(id));
        return "admin-account";
    }

    /** 展示平台操作审计及中文筛选选项。 */
    @GetMapping("/admin/audit")
    public String audit(@RequestParam Map<String, String> query, Model model) {
        adminArea(model);
        model.addAttribute("records", accounts.audits(query));
        model.addAttribute("auditActions", AuditDisplay.actions());
        return "audit";
    }

    /** 使用学生工作台的侧栏与布局。 */
    private void studentArea(Model model) { model.addAttribute("area", "student"); }
    /** 使用管理工作台的侧栏与布局。 */
    private void adminArea(Model model) { model.addAttribute("area", "admin"); }
}
