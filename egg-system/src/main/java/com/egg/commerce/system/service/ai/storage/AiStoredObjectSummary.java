package com.egg.commerce.system.service.ai.storage;

import java.util.Date;

public class AiStoredObjectSummary
{
    private String bucket;
    private String objectKey;
    private String url;
    private long size;
    private Date lastModified;

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public Date getLastModified() { return lastModified; }
    public void setLastModified(Date lastModified) { this.lastModified = lastModified; }
}
