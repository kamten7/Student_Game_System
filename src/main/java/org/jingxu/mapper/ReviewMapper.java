package org.jingxu.mapper;

import java.util.List;
import java.util.Map;

public interface ReviewMapper {
    List<Map<String, Object>> candidateList(long competitionId);
    Map<String, Object> find(long id);
    Map<String, Object> lock(long id);
    Map<String, Object> byJudgeAndVersion(Map<String, Object> query);
    int activeCount(long versionId);
    void insertAssignment(Map<String, Object> row);
    void changeAssignment(Map<String, Object> row);
    List<Map<String, Object>> listForSubmission(long submissionId);
    List<Map<String, Object>> judgeList(Map<String, Object> query);
    long judgeCount(Map<String, Object> query);
    Map<String, Object> score(long id);
    void insertScore(Map<String, Object> row);
    void saveScore(Map<String, Object> row);
    void finishScore(Map<String, Object> row);
    List<Map<String, Object>> activeScores(long versionId);
}
