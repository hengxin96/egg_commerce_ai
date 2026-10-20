package com.egg.commerce.system.service.ai.product;

import com.alibaba.fastjson2.JSONObject;

/** Runs rule-based and vision-model quality checks after product images are generated. */
public interface AiProductImageAuditService
{
    JSONObject audit(AiProductImageAuditRequest request);
}
