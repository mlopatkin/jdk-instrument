package com.example.interceptor.advices;

import com.example.interceptor.Interceptor;
import net.bytebuddy.asm.Advice;

public class UnifiedMapAdvice {
    @Advice.OnMethodEnter
    public static void unifiedMapConstructorEnter() {
        Interceptor.getInstance().onUnifiedMapCreated();
    }
}
