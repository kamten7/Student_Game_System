package org.jingxu.mapper;

import java.util.List;
import java.util.Map;

/** 读写匿名作品、上传版本和参评决定。 */
public interface SubmissionMapper {
    /** 按作品编号读取交件记录。 */
    Map<String, Object> find(long id);
    /** 按报名编号读取其作品记录。 */
    Map<String, Object> forRegistration(long id);
    /** 锁定作品行以串行化版本和资格修改。 */
    Map<String, Object> lock(long id);
    /** 为首次交件新建匿名作品记录。 */
    void insert(Map<String, Object> row);
    /** 保存一个 PDF 交件版本及请求标识。 */
    void addVersion(Map<String, Object> row);
    /** 读取指定作品的历史版本。 */
    List<Map<String, Object>> versions(long id);
    /** 按作品和版本号查找唯一文件记录。 */
    Map<String, Object> version(Map<String, Object> filter);
    /** 按版本主键查找文件记录。 */
    Map<String, Object> versionById(long id);
    /** 读取当前最新的交件版本。 */
    Map<String, Object> latest(long submissionId);
    /** 按请求标识查找已接收过的版本。 */
    Map<String, Object> request(Map<String, Object> filter);
    /** 将选定的交件版本冻结为评审版本。 */
    void lockVersion(Map<String, Object> row);
    /** 保存作品参评资格和操作说明。 */
    void eligibility(Map<String, Object> row);
    /** 分页查询管理端的作品和交件状态。 */
    List<Map<String, Object>> workList(Map<String, Object> query);
    /** 统计作品筛选条件匹配的条数。 */
    long workCount(Map<String, Object> query);
}
