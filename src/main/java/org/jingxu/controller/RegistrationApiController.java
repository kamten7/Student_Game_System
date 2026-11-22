package org.jingxu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jingxu.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.jingxu.filter.AccessInterceptor.actor;

@RestController
@RequestMapping("/api/v1")
public class RegistrationApiController {
    private final RegistrationService registrations;

    public RegistrationApiController(RegistrationService registrations) {
        this.registrations = registrations;
    }

    @GetMapping("/student/registrations")
    public Map<String, Object> studentList(HttpServletRequest request, @RequestParam Map<String, String> query) {
        return registrations.studentList(actor(request), query);
    }

    @PostMapping("/student/competitions/{id}/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(HttpServletRequest request, @PathVariable long id,
                                      @RequestBody(required = false) Map<String, Object> values) {
        return registrations.create(actor(request), id, values == null ? Map.of() : values);
    }

    @GetMapping("/student/registrations/{id}")
    public Map<String, Object> detail(HttpServletRequest request, @PathVariable long id) {
        return registrations.studentDetail(actor(request), id);
    }

    @PatchMapping("/student/registrations/{id}")
    public Map<String, Object> rename(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return registrations.rename(actor(request), id, values);
    }

    @DeleteMapping("/student/registrations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(HttpServletRequest request, @PathVariable long id) {
        registrations.delete(actor(request), id);
    }

    @GetMapping("/student/invitations")
    public List<Map<String, Object>> invitations(HttpServletRequest request) {
        return registrations.invitations(actor(request));
    }

    @PostMapping("/student/registrations/{id}/invitations")
    public Map<String, Object> invite(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return registrations.invite(actor(request), id, values);
    }

    @DeleteMapping("/student/registrations/{id}/members/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(HttpServletRequest request, @PathVariable long id, @PathVariable long studentId) {
        registrations.removeMember(actor(request), id, studentId);
    }

    @PostMapping("/student/invitations/{registrationId}/decision")
    public Map<String, Object> answer(HttpServletRequest request, @PathVariable long registrationId,
                                     @RequestBody Map<String, Object> values) {
        return registrations.answer(actor(request), registrationId, values);
    }

    @PostMapping("/student/registrations/{id}/submit")
    public Map<String, Object> submit(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return registrations.submit(actor(request), id, values, false);
    }

    @PostMapping("/student/registrations/{id}/resubmit")
    public Map<String, Object> resubmit(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return registrations.submit(actor(request), id, values, true);
    }

    @PostMapping("/student/registrations/{id}/withdraw")
    public Map<String, Object> withdraw(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return registrations.withdraw(actor(request), id, values);
    }

    @GetMapping("/student/registrations/{id}/result")
    public Map<String, Object> result(HttpServletRequest request, @PathVariable long id) {
        return registrations.result(actor(request), id);
    }

    @GetMapping("/admin/competitions/{id}/registrations")
    public Map<String, Object> adminList(HttpServletRequest request, @PathVariable long id,
                                         @RequestParam Map<String, String> query) {
        return registrations.adminList(actor(request), id, query);
    }

    @GetMapping("/admin/registrations/{id}")
    public Map<String, Object> adminDetail(HttpServletRequest request, @PathVariable long id) {
        return registrations.adminDetail(actor(request), id);
    }

    @PostMapping("/admin/registrations/{id}/decision")
    public Map<String, Object> decide(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return registrations.decide(actor(request), id, values);
    }
}
