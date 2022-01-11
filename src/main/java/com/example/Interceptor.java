package com.example;

import java.io.Closeable;
import java.io.File;

/**
 * An entry point to be notified about "interesting methods being called". Its methods are called to actually broadcast
 * these events too.
 */
public class Interceptor implements InterceptListener {
    private static final InterceptListener NO_OP = new InterceptListener() {
    };

    private static volatile InterceptListener sPrimaryListener = NO_OP;

    private final static ThreadLocal<Interceptor> sInterceptor = ThreadLocal.withInitial(Interceptor::new);

    public static void installListener(InterceptListener listener) {
        sPrimaryListener = listener;
    }

    public static void discardListener() {
        sPrimaryListener = NO_OP;
    }

    private boolean enableInterception = true;

    private Interceptor() {
    }

    public static Interceptor getInstance() {
        return sInterceptor.get();
    }

    @Override
    public void onFileOpened(File file) {
        if (enableInterception) {
            sPrimaryListener.onFileOpened(file);
        }
    }

    @Override
    public void onFileOpened(String path) {
        if (enableInterception) {
            sPrimaryListener.onFileOpened(path);
        }
    }

    @Override
    public void onEnvVarRead(String name) {
        if (enableInterception) {
            sPrimaryListener.onEnvVarRead(name);
        }
    }

    @Override
    public void onSystemPropertyRead(String name, Object value) {
        if (enableInterception) {
            sPrimaryListener.onSystemPropertyRead(name, value);
        }
    }

    /**
     * A try-with-resources helper to call instrumented method without it being intercepted.
     */
    public DisabledInterceptionScope withInterceptionDisabled() {
        return new DisabledInterceptionScope();
    }

    public class DisabledInterceptionScope implements Closeable {

        private DisabledInterceptionScope() {
            enableInterception = false;
        }

        @Override
        public void close() {
            enableInterception = true;
        }
    }
}
