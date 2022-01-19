package com.example.interceptor;

import java.io.File;

public interface InterceptListener {
    default void onFileOpened(File file) {}
    default void onFileOpened(String path) {}
    default void onEnvVarRead(String name) {}
    default void onSystemPropertyRead(String name, Object value) {}
    default void onUnifiedMapCreated() {}
}
