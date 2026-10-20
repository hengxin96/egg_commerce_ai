package com.egg.commerce.system.domain.ai.product;

import java.io.Serializable;

/** 商品资产重命名请求。 */
public class AiProductAssetRenameRequest implements Serializable
{
    private static final long serialVersionUID = 1L;
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
