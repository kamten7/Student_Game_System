package org.jingxu.mapper;

import java.util.List;
import java.util.Map;

/** 读写赛事配置、赛程和公开获奖数据。 */
public interface CompetitionMapper {
    /** 分页查询匹配的赛事及所处阶段。 */
    List<Map<String, Object>> list(Map<String, Object> filter);
    /** 统计匹配筛选条件的赛事总数。 */
    long count(Map<String, Object> filter);
    /** 按编号读取赛事。 */
    Map<String, Object> find(long id);
    /** 对赛事行加锁以串行化关键变更。 */
    Map<String, Object> lock(long id);
    /** 创建赛事草稿并回填编号。 */
    void insert(Map<String, Object> row);
    /** 更新赛事草稿内容和版本。 */
    void updateDraft(Map<String, Object> row);
    /** 保存调整后的赛程。 */
    void updateSchedule(Map<String, Object> row);
    /** 更新赛事状态及取消原因等状态数据。 */
    void updateState(Map<String, Object> row);
    /** 按编号删除赛事草稿。 */
    void deleteDraft(long id);
    /** 检查指定账号是否参与了该赛事的评审。 */
    boolean judgeInvolved(Map<String, Object> query);
    /** 读取赛事管理概览使用的业务统计。 */
    Map<String, Object> overview(long id);
    /** 读取赛事当前生效的公告批次。 */
    Map<String, Object> announcement(long id);
    /** 分页读取公开获奖公告及赛事信息。 */
    List<Map<String, Object>> announcements(Map<String, Object> filter);
    /** 统计可公开的公告数量。 */
    long countAnnouncements();
    /** 按批次查找具备公开奖项的结果条目。 */
    List<Map<String, Object>> awards(List<Long> publicationIds);
}
