package com.egg.commerce.system.service.ai.storage;

import java.util.Date;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 对象存储服务接口，统一知识库源文件上传和清理动作。
 */
public interface AiObjectStorageService
{
    /** 上传知识库源文件并返回对象存储元数据。 */
    AiStoredObject uploadKnowledgeFile(MultipartFile file);

    /** 上传用户选择的商品源图并返回对象存储元数据。 */
    AiStoredObject uploadProductSourceImage(MultipartFile file, Long userId);

    /** 上传 AI 生成图片字节并返回对象存储元数据。 */
    AiStoredObject uploadGeneratedImage(byte[] bytes, String extension, Long userId);

    /** 复制商品图 OSS 对象到当前用户的新商品草稿目录。 */
    AiStoredObject copyProductImageObject(String sourceObjectKey, Long userId);

    /** 列出指定时间前的商品图 OSS 对象，供定时清理孤儿文件。 */
    List<AiStoredObjectSummary> listProductImageObjects(Date olderThan);

    /** 下载指定 OSS 对象内容。 */
    byte[] downloadObject(String objectKey);

    /** 删除对象存储中的指定文件。 */
    void deleteObject(String objectKey);
}
