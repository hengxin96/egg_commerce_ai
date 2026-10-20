package com.egg.commerce.system.mapper.ai.knowledge;

import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 知识库文档数据访问接口，对应知识库文档元数据表。
 *
 * <p>这里只保存文件和向量化状态的元数据；对象存储文件和向量库数据由 service 层按字段引用清理。</p>
 */
public interface AiKnowledgeDocumentMapper
{
    /** 根据文档 ID 查询单条记录。 */
    AiKnowledgeDocument selectAiKnowledgeDocumentById(String id);

    /** 根据多个文档 ID 查询记录，删除前用于定位外部资源。 */
    List<AiKnowledgeDocument> selectAiKnowledgeDocumentByIds(String[] ids);

    /** 按条件查询文档列表。 */
    List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument query);

    /** 新增文档元数据记录。 */
    int insertAiKnowledgeDocument(AiKnowledgeDocument document);

    /** 更新文档状态、向量 ID、失败原因等元数据。 */
    int updateAiKnowledgeDocument(AiKnowledgeDocument document);

    /** 批量更新知识库文档可访问角色。 */
    int updateAiKnowledgeDocumentAccessRoles(@Param("ids") String[] ids, @Param("accessRoles") String accessRoles);

    /** 查询所有已完成向量化的文档 ID，供 admin 检索。 */
    List<String> selectCompletedDocumentIds();

    /** 按角色 key 查询已完成且可访问的文档 ID，供 RAG 检索前授权过滤。 */
    List<String> selectCompletedDocumentIdsByAccessRoles(@Param("roleKeys") List<String> roleKeys);

    /** 批量删除文档元数据记录。 */
    int deleteAiKnowledgeDocumentByIds(String[] ids);
}
