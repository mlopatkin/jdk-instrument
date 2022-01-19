package com.example.interceptor.advices;

import com.example.interceptor.Interceptor;
import net.bytebuddy.asm.Advice;

public class UnifiedMapAdvice {
    public static final String TARGET_CLASS_NAME = "org.eclipse.collections.impl.map.mutable.UnifiedMap";
    @Advice.OnMethodEnter
    public static void unifiedMapConstructorEnter() {
        Interceptor.getInstance().onUnifiedMapCreated();
    }
}
