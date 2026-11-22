package org.jingxu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jingxu.service.CompetitionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.jingxu.filter.AccessInterceptor.actor;

/** 提供公开赛事浏览和管理端赛事操作接口。 */
@RestController
@RequestMapping("/api/v1")
public class CompetitionApiController {
    private final CompetitionService competitions;

    /** 接入赛事服务执行查询和状态变更。 */
    public CompetitionApiController(CompetitionService competitions) {
        this.competitions = competitions;
    }

    /** 按照筛选条件返回公开赛事。 */
    @GetMapping("/public/competitions")
    public Map<String, Object> publicList(@RequestParam Map<String, String> query) {
        return competitions.list(null, query);
    }

    /** 读取已发布赛事的详情及赛程。 */
    @GetMapping("/public/competitions/{id}")
    public Map<String, Object> publicDetail(@PathVariable long id) {
        return competitions.publicView(id);
    }

    /** 查询赛事当前公开的获奖公告。 */
    @GetMapping("/public/competitions/{id}/announcement")
    public Map<String, Object> announcement(@PathVariable long id) {
        return competitions.announcement(id);
    }

    /** 分页获取当前操作者可管理的赛事。 */
    @GetMapping("/admin/competitions")
    public Map<String, Object> adminList(HttpServletRequest request, @RequestParam Map<String, String> query) {
        return competitions.list(actor(request), query);
    }

    /** 按提交的赛程和规则创建赛事草稿。 */
    @PostMapping("/admin/competitions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(HttpServletRequest request, @RequestBody Map<String, Object> values) {
        return competitions.create(actor(request), values);
    }

    /** 查询管理端的赛事详情。 */
    @GetMapping("/admin/competitions/{id}")
    public Map<String, Object> adminDetail(HttpServletRequest request, @PathVariable long id) {
        return competitions.adminView(actor(request), id);
    }

    /** 保存未发布赛事的规则或赛程。 */
    @PatchMapping("/admin/competitions/{id}")
    public Map<String, Object> edit(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return competitions.edit(actor(request), id, values);
    }

    /** 删除当前仍处于草稿状态的赛事。 */
    @DeleteMapping("/admin/competitions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(HttpServletRequest request, @PathVariable long id) {
        competitions.delete(actor(request), id);
    }

    /** 将赛事草稿发布到公开大厅。 */
    @PostMapping("/admin/competitions/{id}/publish")
    public Map<String, Object> publish(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return competitions.publish(actor(request), id, values);
    }

    /** 延长指定阶段并按需顺移后续赛程。 */
    @PostMapping("/admin/competitions/{id}/schedule-extension")
    public Map<String, Object> extend(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return competitions.extend(actor(request), id, values);
    }

    /** 按记录原因取消已发布赛事。 */
    @PostMapping("/admin/competitions/{id}/cancel")
    public Map<String, Object> cancel(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return competitions.cancel(actor(request), id, values);
    }

    /** 归档已经发布结果的赛事。 */
    @PostMapping("/admin/competitions/{id}/archive")
    public Map<String, Object> archive(HttpServletRequest request, @PathVariable long id) {
        return competitions.archive(actor(request), id);
    }

    /** 查询赛事管理概览中的汇总数据。 */
    @GetMapping("/admin/competitions/{id}/overview")
    public Map<String, Object> overview(HttpServletRequest request, @PathVariable long id) {
        return competitions.overview(actor(request), id);
    }
}
