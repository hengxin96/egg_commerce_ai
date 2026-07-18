package com.egg.commerce.system.service.ai.product.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.service.ai.product.AiProductImageCompositionResult;
import com.egg.commerce.system.service.ai.product.AiProductImageCompositionService;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Java2D 商品图文字合成实现。
 *
 * <p>图片模型负责无字底图，后端只把规划白名单里的短文案画到安全区域，避免模型乱写中文。</p>
 */
@Service
public class AiJava2DProductImageCompositionServiceImpl implements AiProductImageCompositionService
{
    private final Environment environment;

    public AiJava2DProductImageCompositionServiceImpl(Environment environment)
    {
        this.environment = environment;
    }

    @Override
    public AiProductImageCompositionResult compose(byte[] baseImageBytes, JSONObject imagePlan)
    {
        if (baseImageBytes == null || baseImageBytes.length == 0)
        {
            throw new ServiceException("待合成图片不能为空");
        }
        JSONObject textOverlay = imagePlan == null ? null : imagePlan.getJSONObject("textOverlay");
        if (textOverlay == null || !Boolean.TRUE.equals(textOverlay.getBoolean("enabled")))
        {
            return original(baseImageBytes);
        }

        String title = StringUtils.trimToEmpty(textOverlay.getString("title"));
        String subtitle = StringUtils.trimToEmpty(textOverlay.getString("subtitle"));
        String sellingPoint = firstSellingPoint(textOverlay.getJSONArray("sellingPoints"));
        if (StringUtils.isAllBlank(title, subtitle, sellingPoint))
        {
            return original(baseImageBytes);
        }

        try
        {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(baseImageBytes));
            if (source == null)
            {
                return original(baseImageBytes);
            }
            BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = output.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, null);
            drawTextOverlay(graphics, output, title, subtitle, sellingPoint, textOverlay);
            graphics.dispose();

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(output, "png", bytes);
            AiProductImageCompositionResult result = new AiProductImageCompositionResult();
            result.setImageBytes(bytes.toByteArray());
            result.setExtension("png");
            result.setApplied(true);
            JSONObject metadata = new JSONObject();
            metadata.put("template", StringUtils.defaultIfBlank(textOverlay.getString("template"),
                    "style_adaptive_text_overlay"));
            metadata.put("textItems", java.util.List.of(title, subtitle, sellingPoint).stream()
                    .filter(StringUtils::isNotBlank).toList());
            metadata.put("typography", textOverlay.getJSONObject("typography"));
            result.setMetadata(metadata);
            return result;
        }
        catch (Exception e)
        {
            throw new ServiceException("商品图文字合成失败：" + e.getMessage());
        }
    }

    private AiProductImageCompositionResult original(byte[] baseImageBytes)
    {
        AiProductImageCompositionResult result = new AiProductImageCompositionResult();
        result.setImageBytes(baseImageBytes);
        result.setExtension("png");
        result.setApplied(false);
        JSONObject metadata = new JSONObject();
        metadata.put("template", "none");
        result.setMetadata(metadata);
        return result;
    }

    private void drawTextOverlay(Graphics2D graphics, BufferedImage output, String title, String subtitle,
            String sellingPoint, JSONObject textOverlay)
    {
        int width = output.getWidth();
        int height = output.getHeight();
        int padding = Math.max(24, width / 24);
        JSONObject typography = textOverlay.getJSONObject("typography");
        String position = firstNonBlank(textOverlay.getString("position"), value(typography, "position"), "top-left");

        String fontName = environment.getProperty("egg.ai.product-image.composition.font", "Microsoft YaHei");
        int titleSize = Math.max(28, width / 17);
        int subtitleSize = Math.max(18, width / 32);
        int maxWidth = textMaxWidth(width, padding, position);
        int blockHeight = (StringUtils.isNotBlank(title) ? titleSize + 8 : 0)
                + (StringUtils.isNotBlank(StringUtils.defaultIfBlank(subtitle, sellingPoint)) ? subtitleSize + 8 : 0);
        int x = textX(width, padding, maxWidth, position);
        int y = textStartBaseline(height, padding, blockHeight, titleSize, position);
        Color titleColor = parseColor(firstNonBlank(textOverlay.getString("titleColor"), value(typography, "color")),
                Color.WHITE);
        Color subtitleColor = parseColor(firstNonBlank(textOverlay.getString("subtitleColor"),
                value(typography, "subtitleColor"), value(typography, "color")), new Color(248, 248, 245));
        Color shadowColor = parseColor(firstNonBlank(textOverlay.getString("shadowColor"), value(typography, "shadow")),
                new Color(0, 0, 0, 150));
        Color strokeColor = parseColor(firstNonBlank(textOverlay.getString("strokeColor"), value(typography, "stroke")),
                new Color(0, 0, 0, 120));
        float opacity = opacity(textOverlay, typography);
        Composite originalComposite = graphics.getComposite();
        graphics.setComposite(AlphaComposite.SrcOver.derive(opacity));
        drawOptionalPanel(graphics, textOverlay, typography, x, y, maxWidth, blockHeight, padding);
        if (StringUtils.isNotBlank(title))
        {
            graphics.setFont(new Font(fontName, Font.BOLD, titleSize));
            y = drawSingleLine(graphics, title, x, y, maxWidth, titleColor, shadowColor, strokeColor);
        }
        String secondLine = StringUtils.defaultIfBlank(subtitle, sellingPoint);
        if (StringUtils.isNotBlank(secondLine))
        {
            graphics.setFont(new Font(fontName, Font.PLAIN, subtitleSize));
            drawSingleLine(graphics, secondLine, x, y + subtitleSize + 8, maxWidth, subtitleColor, shadowColor,
                    strokeColor);
        }
        graphics.setComposite(originalComposite);
    }

    private int drawSingleLine(Graphics2D graphics, String text, int x, int baseline, int maxWidth, Color textColor,
            Color shadowColor, Color strokeColor)
    {
        Font font = graphics.getFont();
        FontMetrics metrics = graphics.getFontMetrics(font);
        while (metrics.stringWidth(text) > maxWidth && font.getSize() > 14)
        {
            font = font.deriveFont((float) font.getSize() - 2);
            graphics.setFont(font);
            metrics = graphics.getFontMetrics(font);
        }
        drawTextEffects(graphics, text, x, baseline, textColor, shadowColor, strokeColor);
        return baseline;
    }

    private void drawTextEffects(Graphics2D graphics, String text, int x, int baseline, Color textColor,
            Color shadowColor, Color strokeColor)
    {
        if (shadowColor != null)
        {
            graphics.setColor(shadowColor);
            graphics.drawString(text, x + 3, baseline + 4);
            graphics.drawString(text, x + 1, baseline + 2);
        }
        if (strokeColor != null)
        {
            graphics.setColor(strokeColor);
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dy = -1; dy <= 1; dy++)
                {
                    if (dx != 0 || dy != 0)
                    {
                        graphics.drawString(text, x + dx, baseline + dy);
                    }
                }
            }
        }
        graphics.setColor(textColor);
        graphics.drawString(text, x, baseline);
    }

    private String firstSellingPoint(JSONArray sellingPoints)
    {
        if (sellingPoints == null || sellingPoints.isEmpty())
        {
            return "";
        }
        return StringUtils.trimToEmpty(sellingPoints.getString(0));
    }

    private Color parseColor(String hex, Color fallback)
    {
        if (StringUtils.isBlank(hex))
        {
            return fallback;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("#?[0-9a-fA-F]{6}").matcher(hex);
        if (!matcher.find())
        {
            return fallback;
        }
        String value = matcher.group();
        return Color.decode(value.startsWith("#") ? value : "#" + value);
    }

    private String value(JSONObject object, String key)
    {
        return object == null ? null : object.getString(key);
    }

    private String firstNonBlank(String... values)
    {
        for (String value : values)
        {
            if (StringUtils.isNotBlank(value))
            {
                return value;
            }
        }
        return "";
    }

    private int textMaxWidth(int width, int padding, String position)
    {
        if (containsAny(position, "left", "right", "左", "右"))
        {
            return Math.max(width / 3, width / 2 - padding * 2);
        }
        return width - padding * 4;
    }

    private int textX(int width, int padding, int maxWidth, String position)
    {
        if (containsAny(position, "right", "右"))
        {
            return width - padding - maxWidth;
        }
        if (containsAny(position, "center", "middle", "中"))
        {
            return Math.max(padding, (width - maxWidth) / 2);
        }
        return padding * 2;
    }

    private int textStartBaseline(int height, int padding, int blockHeight, int titleSize, String position)
    {
        if (containsAny(position, "bottom", "下"))
        {
            return Math.max(padding + titleSize, height - padding - blockHeight + titleSize);
        }
        if (containsAny(position, "center", "middle", "中") && !containsAny(position, "top", "bottom", "上", "下"))
        {
            return Math.max(padding + titleSize, (height - blockHeight) / 2 + titleSize);
        }
        return padding + titleSize;
    }

    private void drawOptionalPanel(Graphics2D graphics, JSONObject textOverlay, JSONObject typography, int x,
            int baseline, int maxWidth, int blockHeight, int padding)
    {
        String panel = firstNonBlank(textOverlay.getString("panel"), value(typography, "panel"),
                value(typography, "background"));
        String backgroundColor = firstNonBlank(textOverlay.getString("backgroundColor"),
                value(typography, "backgroundColor"));
        if (StringUtils.isBlank(backgroundColor) && !containsAny(panel, "card", "panel", "卡", "底"))
        {
            return;
        }
        Color background = parseColor(backgroundColor, new Color(255, 255, 255));
        Composite originalComposite = graphics.getComposite();
        graphics.setComposite(AlphaComposite.SrcOver.derive(0.28f));
        graphics.setColor(background);
        graphics.fillRoundRect(x - padding / 2, baseline - padding - Math.max(28, blockHeight / 3),
                maxWidth + padding, blockHeight + padding, 18, 18);
        graphics.setComposite(originalComposite);
    }

    private float opacity(JSONObject textOverlay, JSONObject typography)
    {
        String raw = firstNonBlank(textOverlay.getString("opacity"), value(typography, "opacity"));
        if (StringUtils.isBlank(raw))
        {
            return 0.96f;
        }
        try
        {
            float value = raw.trim().endsWith("%")
                    ? Float.parseFloat(StringUtils.removeEnd(raw.trim(), "%")) / 100f
                    : Float.parseFloat(raw.trim());
            return Math.max(0.35f, Math.min(1f, value));
        }
        catch (NumberFormatException e)
        {
            return 0.96f;
        }
    }

    private boolean containsAny(String value, String... needles)
    {
        for (String needle : needles)
        {
            if (StringUtils.containsIgnoreCase(value, needle))
            {
                return true;
            }
        }
        return false;
    }
}
