package org.jingxu.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Map.entry;
import static org.jingxu.util.Data.text;

/** 把结构化审计记录转换成可查阅的中文描述。 */
public final class AuditDisplay {
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<String, String> ACTIONS = Map.ofEntries(
            entry("BOOTSTRAP_ADMIN", "创建首个管理员"), entry("ACCOUNT_STATE", "调整账户状态"),
            entry("ACCOUNT_ROLES", "调整账户角色"), entry("ACCOUNT_NAME", "修改账户姓名"),
            entry("ACCOUNT_USERNAME", "修改用户名"), entry("ACCOUNT_STAFF_CREATED", "创建工作人员账号"),
            entry("STUDENT_VERIFICATION", "审核学生身份"),
            entry("COMPETITION_CREATED", "新建赛事草稿"), entry("COMPETITION_EDITED", "编辑赛事草稿"),
            entry("COMPETITION_DELETED", "删除赛事草稿"), entry("COMPETITION_PUBLISHED", "发布赛事"),
            entry("COMPETITION_CANCELLED", "取消赛事"), entry("COMPETITION_ARCHIVED", "归档赛事"),
            entry("SCHEDULE_EXTENDED", "延长赛程"), entry("REGISTRATION_REVIEWED", "审核参赛报名"),
            entry("SUBMISSION_ELIGIBILITY", "调整作品参评资格"), entry("JUDGE_ASSIGNED", "分派评委"),
            entry("JUDGE_REVOKED", "撤销评委分派"), entry("SCORE_REOPENED", "重开评委评分"),
            entry("RESULT_FIRST", "首次发布获奖公告"), entry("RESULT_CORRECTION", "更正获奖公告"));
    private static final Map<String, String> ROLES = Map.of("ADMIN", "平台管理员", "ORGANIZER", "赛事负责人",
            "JUDGE", "评委", "STUDENT", "学生");
    private static final Map<String, String> STAGES = Map.of("registrationClosesAt", "报名",
            "correctionClosesAt", "报名补正", "qualificationClosesAt", "资格审核",
            "submissionClosesAt", "作品提交", "judgingClosesAt", "评审");

    /** 限制工具类被实例化。 */
    private AuditDisplay() {}

    /** 返回供审计页面筛选使用的操作名称。 */
    public static Map<String, String> actions() {
        return new TreeMap<>(ACTIONS);
    }

    /** 填充操作人、对象、操作说明和本地显示时间。 */
    public static void describe(Map<String, Object> row, ObjectMapper json) {
        JsonNode details = details(json, text(row, "changeSummary"));
        String action = text(row, "action");
        String title = ACTIONS.getOrDefault(action, "其他操作");
        String name = text(row, "targetName");
        if (name.isBlank()) name = details.path("targetName").asText("");
        String target = switch (text(row, "targetType")) {
            case "ACCOUNT" -> "账号";
            case "COMPETITION" -> "赛事";
            default -> "业务对象";
        };
        String actor = text(row, "actorName");
        if (actor.isBlank()) {
            actor = row.get("actorId") == null ? "系统自动执行" : "账号编号 " + row.get("actorId");
        }
        LocalDateTime time = Data.date(row, "createdAt");
        row.put("displayTime", time == null ? "时间未记录" : display(time.atOffset(ZoneOffset.UTC)));
        row.put("displayAction", title);
        row.put("displayActor", actor);
        row.put("displayTarget", name.isBlank() ? target : name);
        row.put("displayTargetMeta", target + "编号 " + text(row, "targetKey"));
        row.put("displaySummary", summary(action, row, details));
    }

    /** 按操作类型解释变更内容，并附上可用的原因。 */
    private static String summary(String action, Map<String, ?> row, JsonNode details) {
        String description = switch (action) {
            case "BOOTSTRAP_ADMIN" -> "系统初始化时创建了首个平台管理员。";
            case "ACCOUNT_STATE" -> switch (details.path("status").asText("")) {
                case "ACTIVE" -> "已启用该账号。";
                case "DISABLED" -> "已停用该账号。";
                case "DELETED" -> "已删除该账号，历史操作记录仍可查询。";
                default -> "已调整该账号的状态。";
            };
            case "ACCOUNT_ROLES" -> roles(details.path("roles"));
            case "ACCOUNT_NAME" -> "已保存该账户的姓名。";
            case "ACCOUNT_USERNAME" -> "已更新该账户的登录用户名。";
            case "ACCOUNT_STAFF_CREATED" -> "已创建工作人员账号。" + roles(details.path("roles"));
            case "STUDENT_VERIFICATION" -> switch (details.path("decision").asText("")) {
                case "APPROVE" -> "学生身份核验通过，已绑定正式学号。";
                case "REJECT" -> "学生身份核验未通过。";
                default -> "已处理学生身份核验。";
            };
            case "COMPETITION_CREATED" -> "已保存新的赛事草稿。";
            case "COMPETITION_EDITED" -> "已保存赛事草稿的配置修改。";
            case "COMPETITION_DELETED" -> "已删除未发布的赛事草稿。";
            case "COMPETITION_PUBLISHED" -> "赛事已公开，可在赛事大厅查看。";
            case "COMPETITION_CANCELLED" -> "赛事已取消，停止接受后续业务操作。";
            case "COMPETITION_ARCHIVED" -> "赛事已归档，仍可查阅已发布结果。";
            case "SCHEDULE_EXTENDED" -> {
                String stage = STAGES.getOrDefault(details.path("stage").asText(""), "赛事阶段");
                String deadline = displayDeadline(details.path("deadline").asText(""));
                yield deadline.isEmpty() ? stage + "截止时间已延长。" : stage + "截止时间已延长至 " + deadline + "。";
            }
            case "REGISTRATION_REVIEWED" -> {
                String decision = switch (details.path("decision").asText("")) {
                    case "APPROVED" -> "已通过审核。";
                    case "CHANGES_REQUESTED" -> "已退回补正。";
                    case "REJECTED" -> "审核未通过。";
                    default -> "已处理审核。";
                };
                yield reference(details, "registrationId", "报名记录") + decision;
            }
            case "SUBMISSION_ELIGIBILITY" -> reference(details, "submissionId", "作品") +
                    switch (details.path("status").asText("")) {
                        case "EXCLUDED" -> "已被取消参评资格。";
                        case "ELIGIBLE" -> "已恢复参评资格。";
                        default -> "的参评资格已调整。";
                    };
            case "JUDGE_ASSIGNED" -> "已分派" + reference(details, "assignmentId", "评审任务")
                    + "给" + reference(details, "judgeId", "评委") + "。";
            case "JUDGE_REVOKED" -> "已撤销" + reference(details, "assignmentId", "评审任务") + "的评委分派。";
            case "SCORE_REOPENED" -> reference(details, "assignmentId", "评审任务") + "已重开，原评委可重新提交评分。";
            case "RESULT_FIRST" -> "已首次公布" + publication(row, details) + "。";
            case "RESULT_CORRECTION" -> "已发布" + publication(row, details) + "，原公告版本已保留。";
            default -> "系统已记录这次操作。";
        };
        String reason = details.path("reason").asText("");
        if (reason.isBlank()) reason = text(row, "publicationReason");
        return reason.isBlank() ? description : description + " 原因是" + reason.strip();
    }

    /** 按公告版本或批次编号描述发布记录。 */
    private static String publication(Map<String, ?> row, JsonNode details) {
        String version = text(row, "publicationVersion");
        if (!version.isBlank()) return "第 " + version + " 版获奖公告";
        String id = details.path("publicationId").asText("");
        return id.isBlank() ? "获奖公告" : "获奖公告，批次编号 " + id;
    }

    /** 把相关编号格式化为用户能识别的业务对象引用。 */
    private static String reference(JsonNode details, String field, String noun) {
        String id = details.path(field).asText("");
        return id.isBlank() ? "该" + noun : noun + "（编号 " + id + "）";
    }

    /** 将保存的角色代码转成中文授权说明。 */
    private static String roles(JsonNode values) {
        if (!values.isArray()) return "已更新该账号的角色。";
        var names = new ArrayList<String>();
        for (JsonNode value : values) names.add(ROLES.getOrDefault(value.asText(), "其他角色"));
        return names.isEmpty() ? "已移除该账号的全部角色。" : "账号角色已设为" + String.join("、", names) + "。";
    }

    /** 解析历史变更摘要，遇到无效数据时降级为缺省内容。 */
    private static JsonNode details(ObjectMapper json, String source) {
        try {
            JsonNode parsed = json.readTree(source);
            return parsed == null ? MissingNode.getInstance() : parsed;
        } catch (JsonProcessingException exception) {
            return MissingNode.getInstance();
        }
    }

    /** 将记录里的 UTC 截止时间转为显示时间。 */
    private static String displayDeadline(String value) {
        try {
            return display(LocalDateTime.parse(value).atOffset(ZoneOffset.UTC));
        } catch (DateTimeParseException exception) {
            return "";
        }
    }

    /** 以中国标准时区和统一格式显示时间。 */
    private static String display(OffsetDateTime value) {
        return value.atZoneSameInstant(DISPLAY_ZONE).format(DISPLAY_TIME);
    }
}
