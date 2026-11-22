package org.jingxu.mapper;

import java.util.List;
import java.util.Map;

/** 读写结果发布批次、结果快照和当前批次指针。 */
public interface ResultMapper {
    /** 列出纳入结果处理的已通过报名及作品。 */
    List<Map<String, Object>> eligibleRegistrations(long competitionId);
    /** 查询指定赛事的发布历史。 */
    List<Map<String, Object>> batches(long competitionId);
    /** 读取指定赛事中的某个发布批次。 */
    Map<String, Object> batch(Map<String, Object> filter);
    /** 按请求标识查找已经处理过的发布。 */
    Map<String, Object> request(Map<String, Object> filter);
    /** 读取批次中保存的结果快照。 */
    List<Map<String, Object>> entries(long publicationId);
    /** 插入结果发布批次并回填编号。 */
    void insertBatch(Map<String, Object> row);
    /** 将结果条目写入指定批次。 */
    void insertEntry(Map<String, Object> row);
    /** 切换赛事对外生效的发布批次。 */
    void selectBatch(Map<String, Object> row);
}
