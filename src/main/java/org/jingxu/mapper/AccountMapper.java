package org.jingxu.mapper;

import java.util.List;
import java.util.Map;

/** 访问账号、学生档案、角色和操作审计。 */
public interface AccountMapper {
    /** 按用户名读取可登录账号的凭据信息。 */
    Map<String, Object> credentials(String username);
    /** 读取状态正常的账号以验证会话。 */
    Map<String, Object> activeAccount(long id);
    /** 读取账号与可选学生档案。 */
    Map<String, Object> account(long id);
    /** 读取与档案类型匹配的有效角色。 */
    List<String> roles(long id);
    /** 锁定管理员角色行以串行化关键账号变更。 */
    long lockAdminRole();
    /** 统计可用的工作人员管理员账号。 */
    int activeAdmins();
    /** 保存新账号并回填生成的账号编号。 */
    void insertAccount(Map<String, Object> row);
    /** 为注册学生保存申报学号和个人资料。 */
    void insertProfile(Map<String, Object> row);
    /** 向指定账号增加一个已知角色。 */
    void addRole(Map<String, Object> row);
    /** 清除现有角色关联以便在同一事务中重新授权。 */
    void removeRoles(long id);
    /** 更新账号状态和逻辑删除时间。 */
    void updateAccount(Map<String, Object> row);
    /** 保存工作人员姓名，不写入学生档案。 */
    void updateStaffName(Map<String, Object> row);
    /** 更新唯一登录名，保持账号编号稳定。 */
    void updateUsername(Map<String, Object> row);
    /** 保存学生个人资料并保留已核验姓名。 */
    void updateProfile(Map<String, Object> row);
    /** 记录正式学号、核验结果及操作者。 */
    void verifyProfile(Map<String, Object> row);
    /** 更新已核验学生的组队邀请码。 */
    void rotateInvite(Map<String, Object> row);
    /** 按筛选条件分页读取账号及有效角色。 */
    List<Map<String, Object>> listAccounts(Map<String, Object> query);
    /** 统计相同筛选条件下的账号总数。 */
    long countAccounts(Map<String, Object> query);
    /** 保存关键操作及结构化摘要。 */
    void audit(Map<String, Object> row);
    /** 分页读取审计记录及显示所需的关联信息。 */
    List<Map<String, Object>> listAudit(Map<String, Object> query);
    /** 统计审计筛选条件匹配的记录数。 */
    long countAudit(Map<String, Object> query);
}
