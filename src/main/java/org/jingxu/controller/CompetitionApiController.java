package org.jingxu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jingxu.service.CompetitionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.jingxu.filter.AccessInterceptor.actor;

@RestController
@RequestMapping("/api/v1")
public class CompetitionApiController {
    private final CompetitionService competitions;

    public CompetitionApiController(CompetitionService competitions) {
        this.competitions = competitions;
    }

    @GetMapping("/public/competitions")
    public Map<String, Object> publicList(@RequestParam Map<String, String> query) {
        return competitions.list(null, query);
    }

    @GetMapping("/public/competitions/{id}")
    public Map<String, Object> publicDetail(@PathVariable long id) {
        return competitions.publicView(id);
    }

    @GetMapping("/public/competitions/{id}/announcement")
    public Map<String, Object> announcement(@PathVariable long id) {
        return competitions.announcement(id);
    }

    @GetMapping("/admin/competitions")
    public Map<String, Object> adminList(HttpServletRequest request, @RequestParam Map<String, String> query) {
        return competitions.list(actor(request), query);
    }

    @PostMapping("/admin/competitions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(HttpServletRequest request, @RequestBody Map<String, Object> values) {
        return competitions.create(actor(request), values);
    }

    @GetMapping("/admin/competitions/{id}")
    public Map<String, Object> adminDetail(HttpServletRequest request, @PathVariable long id) {
        return competitions.adminView(actor(request), id);
    }

    @PatchMapping("/admin/competitions/{id}")
    public Map<String, Object> edit(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return competitions.edit(actor(request), id, values);
    }

    @DeleteMapping("/admin/competitions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(HttpServletRequest request, @PathVariable long id) {
        competitions.delete(actor(request), id);
    }

    @PostMapping("/admin/competitions/{id}/publish")
    public Map<String, Object> publish(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return competitions.publish(actor(request), id, values);
    }

    @PostMapping("/admin/competitions/{id}/schedule-extension")
    public Map<String, Object> extend(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return competitions.extend(actor(request), id, values);
    }

    @PostMapping("/admin/competitions/{id}/cancel")
    public Map<String, Object> cancel(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return competitions.cancel(actor(request), id, values);
    }

    @PostMapping("/admin/competitions/{id}/archive")
    public Map<String, Object> archive(HttpServletRequest request, @PathVariable long id) {
        return competitions.archive(actor(request), id);
    }

    @GetMapping("/admin/competitions/{id}/overview")
    public Map<String, Object> overview(HttpServletRequest request, @PathVariable long id) {
        return competitions.overview(actor(request), id);
    }
}
