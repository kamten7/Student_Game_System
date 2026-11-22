package org.jingxu.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;

/** 集中定义 JSON 序列化和持久化字段解析。 */
public final class JsonSupport {
    /** 限制工具类被实例化。 */
    private JsonSupport() {}

    /** 创建支持时间类型和 ISO 格式的 JSON 映射器。 */
    public static ObjectMapper create() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** 将业务数据编码为 JSON 文本。 */
    public static String encode(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize business data", exception);
        }
    }

    /** 还原公告快照等持久化字段里的字符串列表。 */
    public static List<String> stringList(ObjectMapper mapper, Object value) {
        if (value == null) return List.of();
        try {
            return mapper.readValue(value.toString(), new TypeReference<List<String>>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored list is invalid", exception);
        }
    }
}
