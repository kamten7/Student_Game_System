package org.jingxu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jingxu.pojo.PdfAttachment;
import org.jingxu.service.SubmissionService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.jingxu.filter.AccessInterceptor.actor;

@RestController
@RequestMapping("/api/v1")
public class SubmissionApiController {
    private final SubmissionService submissions;

    public SubmissionApiController(SubmissionService submissions) {
        this.submissions = submissions;
    }

    @GetMapping("/student/registrations/{id}/submission")
    public Map<String, Object> studentWork(HttpServletRequest request, @PathVariable long id) {
        return submissions.studentView(actor(request), id);
    }

    @PostMapping(value = "/student/registrations/{id}/submission/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(HttpServletRequest request, @PathVariable long id, @RequestParam String title,
                                      @RequestParam String requestId, @RequestParam MultipartFile file) {
        return submissions.upload(actor(request), id, title, requestId, file);
    }

    @GetMapping("/student/registrations/{id}/submission/versions/{versionNo}/file")
    public ResponseEntity<FileSystemResource> studentFile(HttpServletRequest request, @PathVariable long id, @PathVariable int versionNo) {
        return download(submissions.studentFile(actor(request), id, versionNo));
    }

    @GetMapping("/admin/competitions/{id}/submissions")
    public Map<String, Object> list(HttpServletRequest request, @PathVariable long id, @RequestParam Map<String, String> query) {
        return submissions.list(actor(request), id, query);
    }

    @GetMapping("/admin/submissions/{id}")
    public Map<String, Object> view(HttpServletRequest request, @PathVariable long id) {
        return submissions.adminView(actor(request), id);
    }

    @GetMapping("/admin/submissions/{id}/versions/{versionNo}/file")
    public ResponseEntity<FileSystemResource> adminFile(HttpServletRequest request, @PathVariable long id, @PathVariable int versionNo) {
        return download(submissions.adminFile(actor(request), id, versionNo));
    }

    @PostMapping("/admin/submissions/{id}/eligibility")
    public Map<String, Object> eligibility(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return submissions.eligibility(actor(request), id, values);
    }

    static ResponseEntity<FileSystemResource> download(PdfAttachment file) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.name()).build().toString())
                .cacheControl(CacheControl.noStore().cachePrivate()).body(new FileSystemResource(file.path()));
    }
}
