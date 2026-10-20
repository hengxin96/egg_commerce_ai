package com.egg.commerce.system.service.ai.knowledge;

import com.egg.commerce.system.domain.ai.knowledge.AiKnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档向量化服务契约。
 *
 * <p>负责把文档内容解析、切片并写入向量库，处理结果会回写到知识库文档记录。</p>
 */
public interface AiKnowledgeVectorizationService
{
    /** 使用上传请求中的文件内容启动向量化，适用于首次上传后的异步处理。 */
    void startVectorization(String documentId, MultipartFile file);

    /** 使用文档记录中的对象存储地址启动向量化，适用于失败后的后台重试。 */
    void startVectorization(AiKnowledgeDocument document);
}
