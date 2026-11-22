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

/** 负责学生交件、文件下载和作品管理入口。 */
@RestController
@RequestMapping("/api/v1")
public class SubmissionApiController {
    private final SubmissionService submissions;

    /** 接入交件服务处理作品和文件版本。 */
    public SubmissionApiController(SubmissionService submissions) {
        this.submissions = submissions;
    }

    /** 查看本人报名关联的作品与版本。 */
    @GetMapping("/student/registrations/{id}/submission")
    public Map<String, Object> studentWork(HttpServletRequest request, @PathVariable long id) {
        return submissions.studentView(actor(request), id);
    }

    /** 接收匿名 PDF 及用于识别重试的请求标识。 */
    @PostMapping(value = "/student/registrations/{id}/submission/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(HttpServletRequest request, @PathVariable long id, @RequestParam String title,
                                      @RequestParam String requestId, @RequestParam MultipartFile file) {
        return submissions.upload(actor(request), id, title, requestId, file);
    }

    /** 下载学生有权访问的指定历史版本。 */
    @GetMapping("/student/registrations/{id}/submission/versions/{versionNo}/file")
    public ResponseEntity<FileSystemResource> studentFile(HttpServletRequest request, @PathVariable long id, @PathVariable int versionNo) {
        return download(submissions.studentFile(actor(request), id, versionNo));
    }

    /** 分页查询负责人管理的赛事作品。 */
    @GetMapping("/admin/competitions/{id}/submissions")
    public Map<String, Object> list(HttpServletRequest request, @PathVariable long id, @RequestParam Map<String, String> query) {
        return submissions.list(actor(request), id, query);
    }

    /** 读取管理端作品详情和版本。 */
    @GetMapping("/admin/submissions/{id}")
    public Map<String, Object> view(HttpServletRequest request, @PathVariable long id) {
        return submissions.adminView(actor(request), id);
    }

    /** 下载指定作品版本供赛事管理使用。 */
    @GetMapping("/admin/submissions/{id}/versions/{versionNo}/file")
    public ResponseEntity<FileSystemResource> adminFile(HttpServletRequest request, @PathVariable long id, @PathVariable int versionNo) {
        return download(submissions.adminFile(actor(request), id, versionNo));
    }

    /** 更新作品参评资格及原因。 */
    @PostMapping("/admin/submissions/{id}/eligibility")
    public Map<String, Object> eligibility(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> values) {
        return submissions.eligibility(actor(request), id, values);
    }

    /** 以 PDF 响应返回授权文件及建议下载名称。 */
    static ResponseEntity<FileSystemResource> download(PdfAttachment file) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.name()).build().toString())
                .cacheControl(CacheControl.noStore().cachePrivate()).body(new FileSystemResource(file.path()));
    }
}
