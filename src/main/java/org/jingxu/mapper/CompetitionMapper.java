package org.jingxu.mapper;

import java.util.List;
import java.util.Map;

public interface CompetitionMapper {
    List<Map<String, Object>> list(Map<String, Object> filter);
    long count(Map<String, Object> filter);
    Map<String, Object> find(long id);
    Map<String, Object> lock(long id);
    void insert(Map<String, Object> row);
    void updateDraft(Map<String, Object> row);
    void updateSchedule(Map<String, Object> row);
    void updateState(Map<String, Object> row);
    void deleteDraft(long id);
    boolean judgeInvolved(Map<String, Object> query);
    Map<String, Object> overview(long id);
    Map<String, Object> announcement(long id);
    List<Map<String, Object>> announcements(Map<String, Object> filter);
    long countAnnouncements();
    List<Map<String, Object>> awards(List<Long> publicationIds);
}
