package com.egg.commerce.system.service.ai.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * AI 对象存储服务接口，统一知识库源文件上传和清理动作。
 */
public interface AiObjectStorageService
{
    /** 上传知识库源文件并返回对象存储元数据。 */
    AiStoredObject uploadKnowledgeFile(MultipartFile file);

    /** 删除对象存储中的指定文件。 */
    void deleteObject(String objectKey);
}
