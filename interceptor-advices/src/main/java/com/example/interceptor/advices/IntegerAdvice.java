package com.example.interceptor.advices;

import com.example.interceptor.Interceptor;
import net.bytebuddy.asm.Advice;

public class IntegerAdvice {
    // Advice can be applied to multiple methods, if the signatures are compatible. Here we intercept all three:
    // Integer.getInteger(String)
    // Integer.getInteger(String, int)
    // Integer.getInteger(String, Integer)
    @Advice.OnMethodExit
    public static void exitGetInteger(@Advice.Argument(0) String key, @Advice.Return Integer value) {
        Interceptor.getInstance().onSystemPropertyRead(key, value);
    }
}
