package com.egg.commerce.system.service.ai.storage.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.common.utils.DateUtils;
import com.egg.commerce.system.service.ai.storage.AiObjectStorageService;
import com.egg.commerce.system.service.ai.storage.AiStoredObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiAliOssObjectStorageServiceImpl implements AiObjectStorageService
{
    private final Environment environment;

    /**
     * 注入环境配置读取器，OSS 连接信息由运行环境提供。
     */
    public AiAliOssObjectStorageServiceImpl(Environment environment)
    {
        this.environment = environment;
    }

    /**
     * 上传知识库源文件到阿里 OSS，并返回可持久化的对象元数据。
     */
    @Override
    public AiStoredObject uploadKnowledgeFile(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("知识库文件不能为空");
        }

        AiAliOssSettings oss = requireAliOssConfig();
        String extension = StringUtils.substringAfterLast(StringUtils.defaultString(file.getOriginalFilename()), ".");
        String objectKey = buildKnowledgeObjectKey(oss.getObjectPrefix(), extension);
        OSS client = new OSSClientBuilder().build(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret());

        try (InputStream inputStream = file.getInputStream())
        {
            // 知识库文件持久化到这里，后续 RAG 入库可以追溯原始源对象。
            client.putObject(oss.getBucket(), objectKey, inputStream);
        }
        catch (IOException e)
        {
            throw new ServiceException("知识库文件上传 OSS 失败");
        }
        catch (Exception e)
        {
            throw new ServiceException("知识库文件上传 OSS 失败");
        }
        finally
        {
            client.shutdown();
        }

        AiStoredObject storedObject = new AiStoredObject();
        storedObject.setBucket(oss.getBucket());
        storedObject.setObjectKey(objectKey);
        storedObject.setUrl(buildObjectUrl(oss, objectKey));
        storedObject.setSize(file.getSize());
        storedObject.setMimeType(file.getContentType());
        return storedObject;
    }

    /**
     * 从阿里 OSS 删除指定对象，配合知识库文档删除清理源文件。
     */
    @Override
    public void deleteObject(String objectKey)
    {
        if (StringUtils.isBlank(objectKey))
        {
            throw new ServiceException("OSS 对象标识不能为空");
        }

        AiAliOssSettings oss = requireAliOssConfig();
        OSS client = new OSSClientBuilder().build(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret());
        try
        {
            client.deleteObject(oss.getBucket(), objectKey);
        }
        catch (Exception e)
        {
            throw new ServiceException("知识库文件删除 OSS 失败");
        }
        finally
        {
            client.shutdown();
        }
    }

    /**
     * 读取并校验阿里 OSS 必要配置，避免缺配置时继续发起远程调用。
     */
    private AiAliOssSettings requireAliOssConfig()
    {
        AiAliOssSettings settings = new AiAliOssSettings();
        settings.setEndpoint(environment.getProperty("egg.ai.storage.alioss.endpoint"));
        settings.setBucket(environment.getProperty("egg.ai.storage.alioss.bucket"));
        settings.setAccessKeyId(environment.getProperty("egg.ai.storage.alioss.access-key-id"));
        settings.setAccessKeySecret(environment.getProperty("egg.ai.storage.alioss.access-key-secret"));
        settings.setObjectPrefix(environment.getProperty("egg.ai.storage.alioss.object-prefix", "ai"));

        if (StringUtils.isAnyBlank(settings.getEndpoint(), settings.getBucket(), settings.getAccessKeyId(),
                settings.getAccessKeySecret()))
        {
            throw new ServiceException("阿里 OSS 配置不完整");
        }
        return settings;
    }

    /**
     * 生成知识库文件对象键，按日期目录和随机 ID 避免文件名冲突。
     */
    private String buildKnowledgeObjectKey(String objectPrefix, String extension)
    {
        String normalizedPrefix = StringUtils.strip(StringUtils.defaultIfBlank(objectPrefix, "ai"), "/");
        StringBuilder objectKey = new StringBuilder();
        objectKey.append(normalizedPrefix)
                .append("/knowledge/")
                .append(DateUtils.datePath())
                .append("/")
                .append(UUID.randomUUID());
        if (StringUtils.isNotBlank(extension))
        {
            objectKey.append(".").append(extension);
        }
        return objectKey.toString();
    }

    /**
     * 拼装 OSS 对象访问地址，用于后台下载和后续重试向量化。
     */
    private String buildObjectUrl(AiAliOssSettings oss, String objectKey)
    {
        String endpoint = StringUtils.removeStartIgnoreCase(oss.getEndpoint(), "https://");
        endpoint = StringUtils.removeStartIgnoreCase(endpoint, "http://");
        String scheme = StringUtils.startsWithIgnoreCase(oss.getEndpoint(), "http://") ? "http://" : "https://";
        return scheme + oss.getBucket() + "." + endpoint + "/" + objectKey;
    }

    private static class AiAliOssSettings
    {
        private String endpoint;

        private String bucket;

        private String accessKeyId;

        private String accessKeySecret;

        private String objectPrefix;

        public String getEndpoint()
        {
            return endpoint;
        }

        public void setEndpoint(String endpoint)
        {
            this.endpoint = endpoint;
        }

        public String getBucket()
        {
            return bucket;
        }

        public void setBucket(String bucket)
        {
            this.bucket = bucket;
        }

        public String getAccessKeyId()
        {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId)
        {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret()
        {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret)
        {
            this.accessKeySecret = accessKeySecret;
        }

        public String getObjectPrefix()
        {
            return objectPrefix;
        }

        public void setObjectPrefix(String objectPrefix)
        {
            this.objectPrefix = objectPrefix;
        }
    }
}
