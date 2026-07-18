package com.egg.commerce.system.service.ai.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.system.service.ai.product.impl.AiJava2DProductImageCompositionServiceImpl;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AiJava2DProductImageCompositionServiceImplTest
{
    @Test
    void compositionUsesStyleAdaptiveTypographyMetadataAndProducesPng() throws Exception
    {
        AiJava2DProductImageCompositionServiceImpl service =
                new AiJava2DProductImageCompositionServiceImpl(new MockEnvironment());
        JSONObject overlay = new JSONObject();
        overlay.put("enabled", true);
        overlay.put("title", "清透亮泽");
        overlay.put("subtitle", "轻盈水润");
        overlay.put("sellingPoints", new JSONArray());
        JSONObject typography = new JSONObject();
        typography.put("position", "bottom-right");
        typography.put("color", "#F8F1E7");
        typography.put("shadow", "#1A1A1A");
        typography.put("stroke", "#2E1F16");
        typography.put("opacity", "0.9");
        overlay.put("typography", typography);
        JSONObject plan = new JSONObject();
        plan.put("textOverlay", overlay);

        AiProductImageCompositionResult result = service.compose(baseImage(), plan);

        assertTrue(result.isApplied());
        assertEquals("png", result.getExtension());
        assertEquals("bottom-right", result.getMetadata().getJSONObject("typography").getString("position"));
        assertEquals("style_adaptive_text_overlay", result.getMetadata().getString("template"));
        assertNotNull(ImageIO.read(new ByteArrayInputStream(result.getImageBytes())));
    }

    private byte[] baseImage() throws Exception
    {
        BufferedImage image = new BufferedImage(640, 853, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(80, 110, 120));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(220, 60, 60));
        graphics.fillOval(220, 260, 200, 220);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
