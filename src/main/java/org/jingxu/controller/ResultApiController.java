package org.jingxu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jingxu.service.ResultService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.jingxu.filter.AccessInterceptor.actor;

@RestController
@RequestMapping("/api/v1/admin/competitions/{id}/results")
public class ResultApiController {
    private final ResultService results;

    public ResultApiController(ResultService results) {
        this.results = results;
    }

    @PostMapping("/preview")
    public Map<String, Object> preview(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return results.preview(actor(request), id, values);
    }

    @PostMapping("/publications")
    public Map<String, Object> publish(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return results.publish(actor(request), id, "FIRST", values);
    }

    @PostMapping("/corrections")
    public Map<String, Object> correct(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return results.publish(actor(request), id, "CORRECTION", values);
    }

    @GetMapping("/publications")
    public List<Map<String, Object>> batches(HttpServletRequest request, @PathVariable long id) {
        return results.batches(actor(request), id);
    }

    @GetMapping("/publications/{publicationId}")
    public Map<String, Object> batch(HttpServletRequest request, @PathVariable long id, @PathVariable long publicationId) {
        return results.batch(actor(request), id, publicationId);
    }
}
