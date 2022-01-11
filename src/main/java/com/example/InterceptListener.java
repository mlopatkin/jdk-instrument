package com.example;

import java.io.File;

public interface InterceptListener {
    default void onFileOpened(File file) {}
    default void onFileOpened(String path) {}
    default void onSystemPropertyRead(String name, Object value) {}
}
