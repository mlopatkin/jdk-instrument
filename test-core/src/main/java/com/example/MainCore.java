package com.example;

import com.example.interceptor.InterceptListener;
import com.example.interceptor.Interceptor;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Map;

public class MainCore {
    private static final InterceptListener listener = new InterceptListener() {
        @Override
        public void onFileOpened(File file) {
            System.err.println("Opened a file " + file);
            new Exception().printStackTrace();
        }

        @Override
        public void onFileOpened(String path) {
            System.err.println("Opened a file " + path);
            new Exception().printStackTrace();
        }

        @Override
        public void onEnvVarRead(String name) {
            System.err.println("Reading environment variable " + name);
            new Exception().printStackTrace();
        }

        @Override
        public void onSystemPropertyRead(String name, Object value) {
            System.err.println("Reading system property " + name);
            new Exception().printStackTrace();
        }

        @Override
        public void onUnifiedMapCreated() {
            System.err.println("Creaing unified map");
            new Exception().printStackTrace();
        }
    };

    public static void doMain(String[] args) throws Exception {
        Interceptor.installListener(listener);

        try (FileInputStream fis = new FileInputStream("/dev/null")) {
            BufferedInputStream in = new BufferedInputStream(fis);
            in.transferTo(System.out);
        }

        System.out.println("PATH=" + System.getenv().get("PATH"));

        System.out.println("some.int = " + Integer.getInteger("some.int"));

        try (var ignored = Interceptor.getInstance().withInterceptionDisabled()) {
            System.out.println("sneaky.int = " + Integer.getInteger("sneaky.int", -1));
        }

        System.err.println("FIS classloader=" + FileInputStream.class.getClassLoader());

        Map<String, String> map = new UnifiedMap<>();
        map.put("some", "value");
        System.err.println("UnifiedMap classloader=" + UnifiedMap.class.getClassLoader());
        System.err.println("UnifiedMap signature=" + Arrays.toString(
                UnifiedMap.class.getProtectionDomain().getCodeSource().getCodeSigners()));
        System.err.println("UnifiedMap some=" + map.get("some"));
    }
}
