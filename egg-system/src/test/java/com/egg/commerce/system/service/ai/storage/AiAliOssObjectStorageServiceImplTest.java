package com.egg.commerce.system.service.ai.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.egg.commerce.system.service.ai.storage.impl.AiAliOssObjectStorageServiceImpl;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class AiAliOssObjectStorageServiceImplTest
{
    @Test
    void objectStorageInterfaceExposesProductImageMethods()
    {
        assertTrue(hasMethod("uploadProductSourceImage"));
        assertTrue(hasMethod("uploadGeneratedImage"));
        assertTrue(hasMethod("copyProductImageObject"));
        assertTrue(hasMethod("listProductImageObjects"));
        assertTrue(hasMethod("downloadObject"));
        assertTrue(hasMethod("deleteObject"));
        assertTrue(hasMethod("presignObjectUrl"));
    }

    @Test
    void generatedImageUploadInfersMimeTypeFromExtension() throws Exception
    {
        Method method = AiAliOssObjectStorageServiceImpl.class
                .getDeclaredMethod("generatedImageMimeType", String.class);
        method.setAccessible(true);

        assertEquals("image/png", method.invoke(null, "png"));
        assertEquals("image/png", method.invoke(null, ".PNG"));
        assertEquals("image/jpeg", method.invoke(null, "jpg"));
        assertEquals("image/jpeg", method.invoke(null, "jpeg"));
        assertEquals("image/webp", method.invoke(null, "webp"));
    }

    private boolean hasMethod(String name)
    {
        return Arrays.stream(AiObjectStorageService.class.getMethods())
                .map(Method::getName)
                .anyMatch(name::equals);
    }
}
