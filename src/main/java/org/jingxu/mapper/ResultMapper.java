package org.jingxu.mapper;

import java.util.List;
import java.util.Map;

public interface ResultMapper {
    List<Map<String, Object>> eligibleRegistrations(long competitionId);
    List<Map<String, Object>> batches(long competitionId);
    Map<String, Object> batch(Map<String, Object> filter);
    Map<String, Object> request(Map<String, Object> filter);
    List<Map<String, Object>> entries(long publicationId);
    void insertBatch(Map<String, Object> row);
    void insertEntry(Map<String, Object> row);
    void selectBatch(Map<String, Object> row);
}
