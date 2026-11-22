package org.jingxu.mapper;

import java.util.List;
import java.util.Map;

public interface SubmissionMapper {
    Map<String, Object> find(long id);
    Map<String, Object> forRegistration(long id);
    Map<String, Object> lock(long id);
    void insert(Map<String, Object> row);
    void addVersion(Map<String, Object> row);
    List<Map<String, Object>> versions(long id);
    Map<String, Object> version(Map<String, Object> filter);
    Map<String, Object> versionById(long id);
    Map<String, Object> latest(long submissionId);
    Map<String, Object> request(Map<String, Object> filter);
    void lockVersion(Map<String, Object> row);
    void eligibility(Map<String, Object> row);
    List<Map<String, Object>> workList(Map<String, Object> query);
    long workCount(Map<String, Object> query);
}
