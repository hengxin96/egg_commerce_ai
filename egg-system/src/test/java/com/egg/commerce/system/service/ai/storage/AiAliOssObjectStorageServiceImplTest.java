package com.egg.commerce.system.service.ai.storage;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
    }

    private boolean hasMethod(String name)
    {
        return Arrays.stream(AiObjectStorageService.class.getMethods())
                .map(Method::getName)
                .anyMatch(name::equals);
    }
}
