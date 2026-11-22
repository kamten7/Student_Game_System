package org.jingxu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jingxu.service.ResultService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.jingxu.filter.AccessInterceptor.actor;

/** 提供结果预览、发布、更正和历史批次接口。 */
@RestController
@RequestMapping("/api/v1/admin/competitions/{id}/results")
public class ResultApiController {
    private final ResultService results;

    /** 接入结果服务处理发布及历史读取。 */
    public ResultApiController(ResultService results) {
        this.results = results;
    }

    /** 生成包含校验结果的发布预览。 */
    @PostMapping("/preview")
    public Map<String, Object> preview(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return results.preview(actor(request), id, values);
    }

    /** 根据有效预览首次发布赛事结果。 */
    @PostMapping("/publications")
    public Map<String, Object> publish(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return results.publish(actor(request), id, "FIRST", values);
    }

    /** 基于当前发布批次提交更正。 */
    @PostMapping("/corrections")
    public Map<String, Object> correct(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return results.publish(actor(request), id, "CORRECTION", values);
    }

    /** 列出指定赛事的全部发布批次。 */
    @GetMapping("/publications")
    public List<Map<String, Object>> batches(HttpServletRequest request, @PathVariable long id) {
        return results.batches(actor(request), id);
    }

    /** 查看指定批次中的结果记录。 */
    @GetMapping("/publications/{publicationId}")
    public Map<String, Object> batch(HttpServletRequest request, @PathVariable long id, @PathVariable long publicationId) {
        return results.batch(actor(request), id, publicationId);
    }
}
