package com.egg.commerce.system.service.ai.storage;

import org.springframework.web.multipart.MultipartFile;

public interface AiObjectStorageService
{
    AiStoredObject uploadKnowledgeFile(MultipartFile file);

    void deleteObject(String objectKey);
}
