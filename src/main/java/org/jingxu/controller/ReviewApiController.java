package org.jingxu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jingxu.service.ReviewService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.jingxu.filter.AccessInterceptor.actor;
import static org.jingxu.util.Data.number;
import static org.jingxu.util.Data.required;

/** 提供评委评分和管理端评委分派接口。 */
@RestController
@RequestMapping("/api/v1")
public class ReviewApiController {
    private final ReviewService reviews;

    /** 接入评审服务处理分派和评分。 */
    public ReviewApiController(ReviewService reviews) {
        this.reviews = reviews;
    }

    /** 查询当前评委的评审任务。 */
    @GetMapping("/judge/assignments")
    public Map<String, Object> list(HttpServletRequest request, @RequestParam Map<String, String> query) {
        return reviews.judgeList(actor(request), query);
    }

    /** 展示本人任务的作品和评分信息。 */
    @GetMapping("/judge/assignments/{id}")
    public Map<String, Object> view(HttpServletRequest request, @PathVariable long id) {
        return reviews.judgeView(actor(request), id);
    }

    /** 返回经授权的匿名评审文件。 */
    @GetMapping("/judge/assignments/{id}/file")
    public ResponseEntity<FileSystemResource> file(HttpServletRequest request, @PathVariable long id) {
        return SubmissionApiController.download(reviews.judgeFile(actor(request), id));
    }

    /** 保存可继续修改的评分草稿。 */
    @PutMapping("/judge/assignments/{id}/score")
    public Map<String, Object> save(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return reviews.score(actor(request), id, values);
    }

    /** 正式提交当前评委的完整评分。 */
    @PostMapping("/judge/assignments/{id}/submit")
    public Map<String, Object> submit(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return reviews.submit(actor(request), id, values);
    }

    /** 登记评委回避原因并释放分派。 */
    @PostMapping("/judge/assignments/{id}/recuse")
    public Map<String, Object> recuse(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return reviews.recuse(actor(request), id, required(values, "reason", 400));
    }

    /** 查询管理者可选用的候选评委。 */
    @GetMapping("/admin/competitions/{id}/judge-candidates")
    public List<Map<String, Object>> candidates(HttpServletRequest request, @PathVariable long id) {
        return reviews.candidates(actor(request), id);
    }

    /** 列出指定作品的评委分派记录。 */
    @GetMapping("/admin/submissions/{id}/assignments")
    public List<Map<String, Object>> assignments(HttpServletRequest request, @PathVariable long id) {
        return reviews.assignments(actor(request), id);
    }

    /** 为作品锁定版本分派指定评委。 */
    @PostMapping("/admin/submissions/{id}/assignments")
    public Map<String, Object> assign(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return reviews.assign(actor(request), id, number(values, "judgeId"));
    }

    /** 撤销尚未提交正式评分的分派。 */
    @PostMapping("/admin/assignments/{id}/revoke")
    public Map<String, Object> revoke(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return reviews.revoke(actor(request), id, required(values, "reason", 400));
    }

    /** 重开指定任务的正式评分。 */
    @PostMapping("/admin/assignments/{id}/reopen")
    public Map<String, Object> reopen(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return reviews.reopen(actor(request), id, required(values, "reason", 400));
    }
}
