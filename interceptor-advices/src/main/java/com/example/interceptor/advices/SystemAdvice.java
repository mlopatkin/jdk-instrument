package com.example.interceptor.advices;

import com.example.interceptor.TrackHashMap;
import net.bytebuddy.asm.Advice;

import java.util.Map;

public class SystemAdvice {
    // Advices can replace the return value of the method
    @Advice.OnMethodExit
    public static void getenv(@Advice.Return(readOnly = false) Map<String, String> envMap) {
        //noinspection UnusedAssignment
        envMap = new TrackHashMap(envMap);
    }

}
