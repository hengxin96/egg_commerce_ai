package com.egg.commerce.system.service.ai.product;

import com.alibaba.fastjson2.JSONObject;

/** 商品图后端文字合成服务，负责把规划中的短文案确定性排到最终图上。 */
public interface AiProductImageCompositionService
{
    AiProductImageCompositionResult compose(byte[] baseImageBytes, JSONObject imagePlan);
}
