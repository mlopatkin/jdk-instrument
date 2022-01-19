package com.example.interceptor.advices;

import com.example.interceptor.Interceptor;
import net.bytebuddy.asm.Advice;

import java.io.File;

public class FisFileAdvice {
    @Advice.OnMethodEnter
    public static void constructorExit(@Advice.Argument(0) File file) {
        Interceptor interceptor = Interceptor.getInstance();
        interceptor.onFileOpened(file);
    }
}
