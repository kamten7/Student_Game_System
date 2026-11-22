package org.jingxu.pojo;

import java.nio.file.Path;

/** 携带已授权文件的本地路径和对外下载名称。 */
public record PdfAttachment(Path path, String name) {}
