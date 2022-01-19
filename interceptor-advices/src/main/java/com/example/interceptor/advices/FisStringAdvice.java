package com.example.interceptor.advices;

import com.example.interceptor.Interceptor;
import net.bytebuddy.asm.Advice;

// Advice classes are defining the template but there is no way to say what method should be instrumented. This
// info has to be provided when configuring builder in the transform above. This means that we'll have quite a lot
// of Advice classes, almost one per instrumented method.
public class FisStringAdvice {
    @Advice.OnMethodEnter
    public static void constructorExit(@Advice.Argument(0) String path) {
        // One pitfall there is that we are going to get interception twice, because FileInputStream(String)
        // delegates to FileInputStream(File) internally.
        // We could in theory report and disable interception in the beginning of the constructor and then re-enable
        // interception again at the end, but exceptions complicate this. We cannot wrap the actual constructor code
        // in try(withInterceptionDisabled()) {}, which is necessary to reliably restore the interception.
        // See https://github.com/raphw/byte-buddy/issues/375
        Interceptor interceptor = Interceptor.getInstance();
        interceptor.onFileOpened(path);
    }
}
