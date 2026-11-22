package org.jingxu.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jingxu.pojo.PdfAttachment;
import org.jingxu.util.WebProblem;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/** 校验和保存私有作品 PDF，并构造下载文件。 */
@Component
public class PdfStorage {
    private final Path root;

    /** 解析附件目录，缺省时使用容器实例的私有目录。 */
    public PdfStorage() {
        String value = System.getenv("STUDENT_ATTACHMENTS_DIR");
        this.root = (value == null || value.isBlank()
                ? Path.of(System.getProperty("catalina.base", System.getProperty("java.io.tmpdir")), "data", "jingxu")
                : Path.of(value)).toAbsolutePath().normalize();
    }

    /** 保存临时文件位置及其内容摘要。 */
    public record StagedPdf(Path path, String sha256) implements AutoCloseable {
        /** 清理尚未提交的临时文件。 */
        @Override
        public void close() {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not clear staged PDF", exception);
            }
        }
    }

    /** 验证上传内容并将其暂存，同时计算 SHA-256 摘要。 */
    public StagedPdf stage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw WebProblem.invalid("请选择匿名 PDF。");
        if (file.getSize() > 20L * 1024 * 1024) throw new WebProblem(413, "FILE_TOO_LARGE", "PDF 不得超过 20 MiB。");
        Path staged = null;
        try {
            Files.createDirectories(root);
            staged = Files.createTempFile(root, ".pending-", ".pdf");
            file.transferTo(staged.toFile());
            verifyPdf(staged);
            try (InputStream input = Files.newInputStream(staged)) {
                MessageDigest digest = sha256();
                byte[] block = new byte[16_384];
                for (int read; (read = input.read(block)) >= 0;) digest.update(block, 0, read);
                return new StagedPdf(staged, HexFormat.of().formatHex(digest.digest()));
            }
        } catch (IOException | RuntimeException exception) {
            if (staged != null) {
                try { Files.deleteIfExists(staged); }
                catch (IOException suppressed) { exception.addSuppressed(suppressed); }
            }
            if (exception instanceof WebProblem problem) throw problem;
            throw new IllegalStateException("Could not stage PDF", exception);
        }
    }

    /** 检查 PDF 文件头、页数和加密状态。 */
    private static void verifyPdf(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            byte[] header = input.readNBytes(5);
            if (!java.util.Arrays.equals(header, new byte[] {'%', 'P', 'D', 'F', '-'})) throw invalidPdf();
            try (PDDocument pdf = Loader.loadPDF(path.toFile())) {
                if (pdf.isEncrypted() || pdf.getNumberOfPages() < 1) throw invalidPdf();
            }
        } catch (IOException exception) {
            throw invalidPdf();
        }
    }

    /** 为无法处理的 PDF 生成一致的错误。 */
    private static WebProblem invalidPdf() {
        return new WebProblem(415, "INVALID_PDF", "文件须为结构完整且未加密的 PDF。");
    }

    /** 将已暂存文件原子移动到正式存储位置。 */
    public String commit(StagedPdf staged) {
        String key = UUID.randomUUID().toString().replace("-", "") + ".pdf";
        try {
            Files.move(staged.path(), root.resolve(key), StandardCopyOption.ATOMIC_MOVE);
            return key;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store PDF", exception);
        }
    }

    /** 验证存储键和文件存在性后构造下载对象。 */
    public PdfAttachment attachment(String key, String displayName) {
        if (!key.matches("[a-f0-9]{32}\\.pdf")) throw WebProblem.missing();
        Path file = root.resolve(key);
        if (!Files.isRegularFile(file)) throw WebProblem.missing();
        return new PdfAttachment(file, displayName + ".pdf");
    }

    /** 创建用于计算文件摘要的 SHA-256 实例。 */
    public static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
