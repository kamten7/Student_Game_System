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

public final class Data {
    private Data() {}

    public static Map<String, Object> fields(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put((String) pairs[i], pairs[i + 1]);
        }
        return values;
    }

    public static String text(Map<String, ?> values, String field) {
        Object value = values.get(field);
        return value == null ? "" : value.toString();
    }

    public static String optional(Map<String, ?> values, String field, int max) {
        String value = text(values, field).strip();
        if (value.length() > max) throw WebProblem.invalid(field + " 超出长度限制。");
        return value;
    }

    public static String required(Map<String, ?> values, String field, int max) {
        String value = optional(values, field, max);
        if (value.isEmpty()) throw WebProblem.invalid("请填写 " + field + "。");
        return value;
    }

    public static long number(Map<String, ?> values, String field) {
        try {
            return Long.parseLong(text(values, field));
        } catch (NumberFormatException exception) {
            throw WebProblem.invalid(field + " 必须是整数。");
        }
    }

    public static int integer(Map<String, ?> values, String field, int min, int max) {
        long value = number(values, field);
        if (value < min || value > max) throw WebProblem.invalid(field + " 超出允许范围。");
        return (int) value;
    }

    public static int queryNumber(Map<String, ?> values, String field, int fallback, int max) {
        return text(values, field).isBlank() ? fallback : integer(values, field, 1, max);
    }

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

    public static LocalDateTime date(Map<String, ?> row, String field) {
        Object value = row.get(field);
        return value == null ? null : asDate(value);
    }

    private static LocalDateTime asDate(Object value) {
        if (value instanceof LocalDateTime time) return time;
        if (value instanceof Timestamp time) return time.toLocalDateTime();
        throw new IllegalStateException("Unexpected datetime type: " + value.getClass());
    }

    public static OffsetDateTime withOffset(Map<String, ?> row, String field) {
        LocalDateTime value = date(row, field);
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    public static Map<String, Object> page(List<?> items, int page, int size, long total) {
        return fields("items", items, "page", page, "size", size, "total", total);
    }

    public static boolean flag(Object value) {
        return Boolean.TRUE.equals(value) || value instanceof Number number && number.intValue() == 1;
    }

    public static String identifier(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    public static void matchVersion(Map<String, ?> row, Map<String, ?> values) {
        if (number(row, "rowVersion") != integer(values, "rowVersion", 0, Integer.MAX_VALUE)) {
            throw WebProblem.conflict("记录已更新，请刷新后重试。");
        }
    }
}
