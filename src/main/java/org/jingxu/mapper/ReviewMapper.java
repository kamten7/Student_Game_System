package org.jingxu.mapper;

import java.util.List;
import java.util.Map;

/** 访问评委候选名单、分派任务和评分记录。 */
public interface ReviewMapper {
    /** 筛选角色有效且符合利益冲突规则的候选评委。 */
    List<Map<String, Object>> candidateList(long competitionId);
    /** 读取单条评审任务及锁定版本信息。 */
    Map<String, Object> find(long id);
    /** 锁定评审任务以安全修改评分或状态。 */
    Map<String, Object> lock(long id);
    /** 查找指定评委对同一版本的既有分派。 */
    Map<String, Object> byJudgeAndVersion(Map<String, Object> query);
    /** 统计同一版本已占用的有效评委名额。 */
    int activeCount(long versionId);
    /** 为锁定版本创建一条评审任务。 */
    void insertAssignment(Map<String, Object> row);
    /** 更新分派状态及操作原因。 */
    void changeAssignment(Map<String, Object> row);
    /** 读取某个作品的全部评委任务。 */
    List<Map<String, Object>> listForSubmission(long submissionId);
    /** 分页查询当前评委的任务。 */
    List<Map<String, Object>> judgeList(Map<String, Object> query);
    /** 统计当前评委筛选后的任务总数。 */
    long judgeCount(Map<String, Object> query);
    /** 读取分派下保存的评分内容。 */
    Map<String, Object> score(long id);
    /** 首次为任务保存评分记录。 */
    void insertScore(Map<String, Object> row);
    /** 更新尚可编辑的评分数据。 */
    void saveScore(Map<String, Object> row);
    /** 登记正式评分的提交时间。 */
    void finishScore(Map<String, Object> row);
    /** 读取可纳入排名计算的有效正式评分。 */
    List<Map<String, Object>> activeScores(long versionId);
}
