package org.jingxu.mapper;

import java.util.List;
import java.util.Map;

public interface AccountMapper {
    Map<String, Object> credentials(String username);
    Map<String, Object> activeAccount(long id);
    Map<String, Object> account(long id);
    List<String> roles(long id);
    long lockAdminRole();
    int activeAdmins();
    void insertAccount(Map<String, Object> row);
    void insertProfile(Map<String, Object> row);
    void addRole(Map<String, Object> row);
    void removeRoles(long id);
    void updateAccount(Map<String, Object> row);
    void updateProfile(Map<String, Object> row);
    void verifyProfile(Map<String, Object> row);
    void rotateInvite(Map<String, Object> row);
    List<Map<String, Object>> listAccounts(Map<String, Object> query);
    long countAccounts(Map<String, Object> query);
    void audit(Map<String, Object> row);
    List<Map<String, Object>> listAudit(Map<String, Object> query);
    long countAudit(Map<String, Object> query);
}
