package org.jingxu.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 提供业务字段转换、输入校验和分页结构。 */
public final class Data {
    /** 限制工具类被实例化。 */
    private Data() {}

    /** 将成对的字段名和字段值构造成有序映射。 */
    public static Map<String, Object> fields(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put((String) pairs[i], pairs[i + 1]);
        }
        return values;
    }

    /** 读取字段文本，缺失时返回空字符串。 */
    public static String text(Map<String, ?> values, String field) {
        Object value = values.get(field);
        return value == null ? "" : value.toString();
    }

    /** 去除字段首尾空白并校验最大长度。 */
    public static String optional(Map<String, ?> values, String field, int max) {
        String value = text(values, field).strip();
        if (value.length() > max) throw WebProblem.invalid(field + " 超出长度限制。");
        return value;
    }

    /** 校验必填文本并返回归一化值。 */
    public static String required(Map<String, ?> values, String field, int max) {
        String value = optional(values, field, max);
        if (value.isEmpty()) throw WebProblem.invalid("请填写 " + field + "。");
        return value;
    }

    /** 将字段解析为整数值。 */
    public static long number(Map<String, ?> values, String field) {
        try {
            return Long.parseLong(text(values, field));
        } catch (NumberFormatException exception) {
            throw WebProblem.invalid(field + " 必须是整数。");
        }
    }

    /** 读取具有取值范围的整数参数。 */
    public static int integer(Map<String, ?> values, String field, int min, int max) {
        long value = number(values, field);
        if (value < min || value > max) throw WebProblem.invalid(field + " 超出允许范围。");
        return (int) value;
    }

    /** 读取带默认值和上限的分页参数。 */
    public static int queryNumber(Map<String, ?> values, String field, int fallback, int max) {
        return text(values, field).isBlank() ? fallback : integer(values, field, 1, max);
    }

    /** 将带时区偏移的输入转为可存储的 UTC 时间。 */
    public static LocalDateTime utcInput(Map<String, ?> values, String field) {
        try {
            LocalDateTime utc = OffsetDateTime.parse(required(values, field, 40))
                    .withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MICROS);
            if (utc.getYear() < 1000 || utc.getYear() > 9999) throw WebProblem.invalid(field + " 超出可存储的日期范围。");
            return utc;
        } catch (DateTimeParseException exception) {
            throw WebProblem.invalid(field + " 需要包含时区偏移。");
        }
    }

    /** 读取映射中的日期时间字段。 */
    public static LocalDateTime date(Map<String, ?> row, String field) {
        Object value = row.get(field);
        return value == null ? null : asDate(value);
    }

    /** 将 JDBC 返回的时间类型统一为本地时间对象。 */
    private static LocalDateTime asDate(Object value) {
        if (value instanceof LocalDateTime time) return time;
        if (value instanceof Timestamp time) return time.toLocalDateTime();
        throw new IllegalStateException("Unexpected datetime type: " + value.getClass());
    }

    /** 为数据库时间附上 UTC 偏移以便传输。 */
    public static OffsetDateTime withOffset(Map<String, ?> row, String field) {
        LocalDateTime value = date(row, field);
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    /** 组装条目列表及页码、每页条数和总数。 */
    public static Map<String, Object> page(List<?> items, int page, int size, long total) {
        return fields("items", items, "page", page, "size", size, "total", total);
    }

    /** 将数据库布尔值或数字状态转成布尔值。 */
    public static boolean flag(Object value) {
        return Boolean.TRUE.equals(value) || value instanceof Number number && number.intValue() == 1;
    }

    /** 生成带业务前缀的随机编号。 */
    public static String identifier(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    /** 校验客户端看到的记录版本，阻止覆盖并发修改。 */
    public static void matchVersion(Map<String, ?> row, Map<String, ?> values) {
        if (number(row, "rowVersion") != integer(values, "rowVersion", 0, Integer.MAX_VALUE)) {
            throw WebProblem.conflict("记录已更新，请刷新后重试。");
        }
    }
}
