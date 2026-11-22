package org.jingxu.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface RegistrationMapper {
    Map<String, Object> find(long id);
    Map<String, Object> lock(long id);
    List<Map<String, Object>> list(Map<String, Object> query);
    long count(Map<String, Object> query);
    List<Map<String, Object>> members(long id);
    List<Map<String, Object>> history(long id);
    List<Map<String, Object>> invitations(long studentId);
    Long inviteRecipient(String code);
    void insert(Map<String, Object> row);
    void addMember(Map<String, Object> row);
    int changeMember(Map<String, Object> row);
    void purgeDraftMembers(long id);
    void deleteDraft(long id);
    void rename(Map<String, Object> row);
    void advance(Map<String, Object> row);
    void bump(long id);
    void release(long id);
    void claim(Map<String, Object> row);
    void log(Map<String, Object> row);
    List<Long> dueCompetitions(LocalDateTime now);
    List<Map<String, Object>> unsettled(long competitionId);
    Map<String, Object> currentResult(long id);
}
