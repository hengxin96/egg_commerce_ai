package com.egg.commerce.system.service.ai.knowledge;

import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDownloadUrl;
import java.util.Collection;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档服务契约。
 *
 * <p>面向后台“我的知识库”管理入口，隐藏对象存储、文档解析和向量库实现细节。</p>
 */
public interface AiKnowledgeDocumentService
{
    /** 按查询条件返回知识库文档列表，供后台分页展示。 */
    List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument query);

    /** 根据文档 ID 查询单条知识库文档记录。 */
    AiKnowledgeDocument selectAiKnowledgeDocumentById(String id);

    /** 上传知识库源文件，并触发异步向量化任务。 */
    AiKnowledgeDocument upload(MultipartFile file, Long createdBy);

    /** 校验请求中的每个文件后，从后台页面批量上传知识库文件。 */
    List<AiKnowledgeDocument> uploadBatch(MultipartFile[] files, Long createdBy);

    /** 返回所选文档的 OSS 下载地址，不向前端暴露完整元数据记录。 */
    List<AiKnowledgeDownloadUrl> selectDownloadUrls(String[] ids);

    /** 批量更新知识库文档可访问角色；admin 会被强制保留。 */
    int updateAccessRoles(String[] ids, List<String> accessRoles);

    /** 根据当前用户角色查询可用于 RAG 检索的已完成文档 ID。 */
    List<String> selectAccessibleCompletedDocumentIds(Collection<String> roleKeys);

    /** 批量删除文档，并清理对应对象存储文件和向量库记录。 */
    int deleteAiKnowledgeDocumentByIds(String[] ids);

    /** 重新触发指定文档的切片和向量化流程。 */
    void retryVectorization(String id);
}
