package org.jingxu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jingxu.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.jingxu.filter.AccessInterceptor.actor;

/** 提供个人报名、团队邀请及资格审核接口。 */
@RestController
@RequestMapping("/api/v1")
public class RegistrationApiController {
    private final RegistrationService registrations;

    /** 接入报名服务处理各端操作。 */
    public RegistrationApiController(RegistrationService registrations) {
        this.registrations = registrations;
    }

    /** 分页查询学生参与的报名。 */
    @GetMapping("/student/registrations")
    public Map<String, Object> studentList(HttpServletRequest request, @RequestParam Map<String, String> query) {
        return registrations.studentList(actor(request), query);
    }

    /** 为已核验学生创建个人或团队报名草稿。 */
    @PostMapping("/student/competitions/{id}/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(HttpServletRequest request, @PathVariable long id,
                                      @RequestBody(required = false) Map<String, Object> values) {
        return registrations.create(actor(request), id, values == null ? Map.of() : values);
    }

    /** 读取当前学生有权访问的报名详情。 */
    @GetMapping("/student/registrations/{id}")
    public Map<String, Object> detail(HttpServletRequest request, @PathVariable long id) {
        return registrations.studentDetail(actor(request), id);
    }

    /** 修改报名草稿或待补正报名的团队名称。 */
    @PatchMapping("/student/registrations/{id}")
    public Map<String, Object> rename(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return registrations.rename(actor(request), id, values);
    }

    /** 删除队长可编辑且尚未正式提交的草稿。 */
    @DeleteMapping("/student/registrations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(HttpServletRequest request, @PathVariable long id) {
        registrations.delete(actor(request), id);
    }

    /** 列出当前学生收到的组队邀请。 */
    @GetMapping("/student/invitations")
    public List<Map<String, Object>> invitations(HttpServletRequest request) {
        return registrations.invitations(actor(request));
    }

    /** 按学生邀请码添加待确认的成员。 */
    @PostMapping("/student/registrations/{id}/invitations")
    public Map<String, Object> invite(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return registrations.invite(actor(request), id, values);
    }

    /** 将指定成员从可编辑的团队报名中移除。 */
    @DeleteMapping("/student/registrations/{id}/members/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(HttpServletRequest request, @PathVariable long id, @PathVariable long studentId) {
        registrations.removeMember(actor(request), id, studentId);
    }

    /** 处理受邀人对组队邀请的确认或拒绝。 */
    @PostMapping("/student/invitations/{registrationId}/decision")
    public Map<String, Object> answer(HttpServletRequest request, @PathVariable long registrationId,
                                     @RequestBody Map<String, Object> values) {
        return registrations.answer(actor(request), registrationId, values);
    }

    /** 将报名草稿正式提交资格审核。 */
    @PostMapping("/student/registrations/{id}/submit")
    public Map<String, Object> submit(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return registrations.submit(actor(request), id, values, false);
    }

    /** 在补正期间重新提交已退回的报名。 */
    @PostMapping("/student/registrations/{id}/resubmit")
    public Map<String, Object> resubmit(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return registrations.submit(actor(request), id, values, true);
    }

    /** 在规则允许期间撤回已正式提交的报名。 */
    @PostMapping("/student/registrations/{id}/withdraw")
    public Map<String, Object> withdraw(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return registrations.withdraw(actor(request), id, values);
    }

    /** 查看当前学生所属报名的已发布结果。 */
    @GetMapping("/student/registrations/{id}/result")
    public Map<String, Object> result(HttpServletRequest request, @PathVariable long id) {
        return registrations.result(actor(request), id);
    }

    /** 按赛事和条件筛选管理端报名记录。 */
    @GetMapping("/admin/competitions/{id}/registrations")
    public Map<String, Object> adminList(HttpServletRequest request, @PathVariable long id,
                                         @RequestParam Map<String, String> query) {
        return registrations.adminList(actor(request), id, query);
    }

    /** 读取管理端报名详情和状态历史。 */
    @GetMapping("/admin/registrations/{id}")
    public Map<String, Object> adminDetail(HttpServletRequest request, @PathVariable long id) {
        return registrations.adminDetail(actor(request), id);
    }

    /** 处理通过、退回补正或拒绝决定。 */
    @PostMapping("/admin/registrations/{id}/decision")
    public Map<String, Object> decide(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return registrations.decide(actor(request), id, values);
    }
}
