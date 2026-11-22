package org.jingxu.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 读写参赛报名、成员、资格占用和状态历史。 */
public interface RegistrationMapper {
    /** 按编号查找报名及关联赛事信息。 */
    Map<String, Object> find(long id);
    /** 锁定报名行用于后续写操作。 */
    Map<String, Object> lock(long id);
    /** 分页读取指定权限范围内的报名。 */
    List<Map<String, Object>> list(Map<String, Object> query);
    /** 统计筛选条件对应的报名总数。 */
    long count(Map<String, Object> query);
    /** 读取报名成员及其身份和邀请状态。 */
    List<Map<String, Object>> members(long id);
    /** 按时间读取报名状态流转记录。 */
    List<Map<String, Object>> history(long id);
    /** 查询学生收到的组队邀请。 */
    List<Map<String, Object>> invitations(long studentId);
    /** 按邀请码查找仍可受邀的已核验学生。 */
    Long inviteRecipient(String code);
    /** 新建报名草稿并回填报名编号。 */
    void insert(Map<String, Object> row);
    /** 将学生加入报名成员名单。 */
    void addMember(Map<String, Object> row);
    /** 更新受邀成员的确认状态和确认时间。 */
    int changeMember(Map<String, Object> row);
    /** 删除准备清理的草稿成员关系。 */
    void purgeDraftMembers(long id);
    /** 删除尚未提交的报名草稿。 */
    void deleteDraft(long id);
    /** 更新队名及报名记录版本。 */
    void rename(Map<String, Object> row);
    /** 更新报名状态、版本及提交时间。 */
    void advance(Map<String, Object> row);
    /** 递增报名版本以反映成员或状态变更。 */
    void bump(long id);
    /** 释放该报名占用的参赛资格。 */
    void release(long id);
    /** 为一名成员占用本届赛事唯一的参赛资格。 */
    void claim(Map<String, Object> row);
    /** 保存报名状态变化及操作者。 */
    void log(Map<String, Object> row);
    /** 查询资格审核截止后仍需处理的赛事。 */
    List<Long> dueCompetitions(LocalDateTime now);
    /** 查找赛事内需要按截止规则处理的报名。 */
    List<Map<String, Object>> unsettled(long competitionId);
    /** 读取报名对应的当前发布结果。 */
    Map<String, Object> currentResult(long id);
}
