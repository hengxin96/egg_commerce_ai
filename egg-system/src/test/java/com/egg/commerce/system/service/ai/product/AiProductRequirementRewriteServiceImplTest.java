package com.egg.commerce.system.service.ai.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.egg.commerce.system.domain.ai.product.AiRequirementRewriteRequest;
import com.egg.commerce.system.domain.ai.product.AiRequirementRewriteResult;
import com.egg.commerce.system.domain.ai.prompt.AiPromptTemplate;
import com.egg.commerce.system.mapper.ai.product.AiProductAssetMapper;
import com.egg.commerce.system.mapper.ai.prompt.AiPromptTemplateMapper;
import com.egg.commerce.system.service.ai.model.AiTextGenerationResult;
import com.egg.commerce.system.service.ai.model.AiTextGenerationRequest;
import com.egg.commerce.system.service.ai.model.AiTextGenerationService;
import com.egg.commerce.system.service.ai.product.impl.AiProductRequirementRewriteServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiProductRequirementRewriteServiceImplTest
{
    @Test
    void rewriteParsesOptimizedRequirementFromTextModelJson()
    {
        AiProductAssetMapper assetMapper = mock(AiProductAssetMapper.class);
        AiPromptTemplateMapper promptTemplateMapper = mock(AiPromptTemplateMapper.class);
        AiTextGenerationService textGenerationService = mock(AiTextGenerationService.class);
        AiPromptTemplate template = new AiPromptTemplate();
        template.setContent("{\"format\":\"markdown\",\"body\":\"rewrite {targetPlatform} {userRequirement}\"}");
        when(promptTemplateMapper.selectPublishedTemplateByNameAndCategory("AI帮写优化提示词", "AI优化提示词"))
                .thenReturn(template);

        AiTextGenerationResult textResult = new AiTextGenerationResult();
        textResult.setContent("{\"optimizedRequirement\":\"优化后的主图要求\",\"displayTitle\":\"优化后的主图要求\",\"imagePromptReady\":true,\"missingQuestions\":[],\"safetyNotes\":[]}");
        when(textGenerationService.generate(any())).thenReturn(textResult);

        AiProductRequirementRewriteService service = new AiProductRequirementRewriteServiceImpl(assetMapper,
                promptTemplateMapper, textGenerationService);
        AiRequirementRewriteRequest request = new AiRequirementRewriteRequest();
        request.setTargetPlatform("抖音");
        request.setTargetLanguage("zh-CN");
        request.setTextEnabled(false);
        request.setUserRequirement("突出产品卖点");

        AiRequirementRewriteResult result = service.rewrite(1L, request);

        assertEquals("优化后的主图要求", result.getOptimizedRequirement());
        verify(promptTemplateMapper).selectPublishedTemplateByNameAndCategory(eq("AI帮写优化提示词"), eq("AI优化提示词"));
    }

    @Test
    void rewriteUsesOnlyRequirementAndPlatformContext()
    {
        AiProductAssetMapper assetMapper = mock(AiProductAssetMapper.class);
        AiPromptTemplateMapper promptTemplateMapper = mock(AiPromptTemplateMapper.class);
        AiTextGenerationService textGenerationService = mock(AiTextGenerationService.class);
        AiPromptTemplate template = new AiPromptTemplate();
        template.setContent("{\"format\":\"markdown\",\"body\":\"rewrite {targetPlatform} {platformRules} {userRequirement}\"}");
        when(promptTemplateMapper.selectPublishedTemplateByNameAndCategory("AI帮写优化提示词", "AI优化提示词"))
                .thenReturn(template);

        AiTextGenerationResult textResult = new AiTextGenerationResult();
        textResult.setContent("{\"optimizedRequirement\":\"优化后的要求\",\"displayTitle\":\"优化后的要求\",\"imagePromptReady\":true}");
        when(textGenerationService.generate(any())).thenReturn(textResult);

        AiProductRequirementRewriteService service = new AiProductRequirementRewriteServiceImpl(assetMapper,
                promptTemplateMapper, textGenerationService);
        AiRequirementRewriteRequest request = new AiRequirementRewriteRequest();
        request.setProductAssetId("asset-1");
        request.setTargetPlatform("抖音");
        request.setTargetLanguage("en");
        request.setTextEnabled(true);
        request.setUserRequirement("make it premium");

        service.rewrite(1L, request);

        ArgumentCaptor<AiTextGenerationRequest> captor = ArgumentCaptor.forClass(AiTextGenerationRequest.class);
        verify(textGenerationService).generate(captor.capture());
        String prompt = captor.getValue().getPrompt();
        assertTrue(prompt.contains("rewrite 抖音"));
        assertTrue(prompt.contains("make it premium"));
        assertTrue(prompt.contains("抖音商品主图"));
        assertFalse(prompt.contains("en"));
        assertFalse(prompt.contains("textEnabled"));
        assertFalse(prompt.contains("allow concise in-image text"));
        verifyNoInteractions(assetMapper);
    }

    @Test
    void rewriteStreamEmitsOnlyOptimizedRequirementDeltas()
    {
        AiProductAssetMapper assetMapper = mock(AiProductAssetMapper.class);
        AiPromptTemplateMapper promptTemplateMapper = mock(AiPromptTemplateMapper.class);
        AiTextGenerationService textGenerationService = mock(AiTextGenerationService.class);
        AiPromptTemplate template = new AiPromptTemplate();
        template.setContent("{\"format\":\"json\",\"body\":\"rewrite {targetPlatform} {userRequirement}\"}");
        when(promptTemplateMapper.selectPublishedTemplateByNameAndCategory("AI帮写优化提示词", "AI优化提示词"))
                .thenReturn(template);

        String firstChunk = "{\"optimizedRequirement\":\"优";
        String secondChunk = "化后的主图要求\",\"displayTitle\":\"优化后的主图要求\",\"imagePromptReady\":true}";
        AiTextGenerationResult textResult = new AiTextGenerationResult();
        textResult.setContent(firstChunk + secondChunk);
        when(textGenerationService.generateStream(any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<String> onDelta = invocation.getArgument(1, Consumer.class);
            onDelta.accept(firstChunk);
            onDelta.accept(secondChunk);
            return textResult;
        });

        AiProductRequirementRewriteService service = new AiProductRequirementRewriteServiceImpl(assetMapper,
                promptTemplateMapper, textGenerationService);
        AiRequirementRewriteRequest request = new AiRequirementRewriteRequest();
        request.setTargetPlatform("抖音");
        request.setUserRequirement("突出产品卖点");
        List<String> deltas = new ArrayList<>();

        AiRequirementRewriteResult result = service.rewriteStream(1L, request, deltas::add);

        assertEquals("优化后的主图要求", result.getOptimizedRequirement());
        assertEquals(List.of("优", "化后的主图要求"), deltas);
    }
}
