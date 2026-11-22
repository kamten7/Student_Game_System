package org.jingxu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jingxu.util.WebProblem;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.util.Map;
import java.util.UUID;

/** 将请求失败统一呈现为错误页面或结构化接口响应。 */
@ControllerAdvice
public class ProblemController {
    private static final Log LOGGER = LogFactory.getLog(ProblemController.class);

    /** 为业务错误分配本次请求的追踪编号。 */
    @ExceptionHandler(WebProblem.class)
    public ModelAndView problem(WebProblem problem, HttpServletRequest request) {
        return render(problem, request, UUID.randomUUID().toString());
    }

    /** 根据请求入口选择页面或 JSON，并保留错误状态码。 */
    private ModelAndView render(WebProblem problem, HttpServletRequest request, String traceId) {
        ModelAndView view;
        if (request.getServletPath().startsWith("/api/v1")) {
            view = new ModelAndView(new MappingJackson2JsonView(), Map.of("code", problem.code(),
                    "message", problem.getMessage(), "traceId", traceId));
        } else {
            view = new ModelAndView("problem");
            view.addObject("message", problem.getMessage());
        }
        view.setStatus(org.springframework.http.HttpStatusCode.valueOf(problem.status()));
        return view;
    }

    /** 统一处理请求字段缺失、类型不符及解析失败。 */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class})
    public ModelAndView invalid(HttpServletRequest request) {
        return problem(WebProblem.invalid("请求参数格式不正确。"), request);
    }

    /** 为超出文件限制的交件返回明确提示。 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView uploadTooLarge(HttpServletRequest request) {
        return problem(new WebProblem(413, "FILE_TOO_LARGE", "PDF 大小不得超过 20 MiB。"), request);
    }

    /** 记录服务端异常并向用户返回简短提示。 */
    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    public ModelAndView internalFailure(RuntimeException failure, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        LOGGER.error("Business request failed, trace " + traceId, failure);
        return render(new WebProblem(500, "INTERNAL_ERROR", "服务暂时不可用，请稍后重试。"), request, traceId);
    }
}
