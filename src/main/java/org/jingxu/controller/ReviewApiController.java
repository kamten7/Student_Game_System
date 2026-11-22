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

@RestController
@RequestMapping("/api/v1")
public class ReviewApiController {
    private final ReviewService reviews;

    public ReviewApiController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/judge/assignments")
    public Map<String, Object> list(HttpServletRequest request, @RequestParam Map<String, String> query) {
        return reviews.judgeList(actor(request), query);
    }

    @GetMapping("/judge/assignments/{id}")
    public Map<String, Object> view(HttpServletRequest request, @PathVariable long id) {
        return reviews.judgeView(actor(request), id);
    }

    @GetMapping("/judge/assignments/{id}/file")
    public ResponseEntity<FileSystemResource> file(HttpServletRequest request, @PathVariable long id) {
        return SubmissionApiController.download(reviews.judgeFile(actor(request), id));
    }

    @PutMapping("/judge/assignments/{id}/score")
    public Map<String, Object> save(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return reviews.score(actor(request), id, values);
    }

    @PostMapping("/judge/assignments/{id}/submit")
    public Map<String, Object> submit(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return reviews.submit(actor(request), id, values);
    }

    @PostMapping("/judge/assignments/{id}/recuse")
    public Map<String, Object> recuse(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return reviews.recuse(actor(request), id, required(values, "reason", 400));
    }

    @GetMapping("/admin/competitions/{id}/judge-candidates")
    public List<Map<String, Object>> candidates(HttpServletRequest request, @PathVariable long id) {
        return reviews.candidates(actor(request), id);
    }

    @GetMapping("/admin/submissions/{id}/assignments")
    public List<Map<String, Object>> assignments(HttpServletRequest request, @PathVariable long id) {
        return reviews.assignments(actor(request), id);
    }

    @PostMapping("/admin/submissions/{id}/assignments")
    public Map<String, Object> assign(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return reviews.assign(actor(request), id, number(values, "judgeId"));
    }

    @PostMapping("/admin/assignments/{id}/revoke")
    public Map<String, Object> revoke(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return reviews.revoke(actor(request), id, required(values, "reason", 400));
    }

    @PostMapping("/admin/assignments/{id}/reopen")
    public Map<String, Object> reopen(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return reviews.reopen(actor(request), id, required(values, "reason", 400));
    }
}
