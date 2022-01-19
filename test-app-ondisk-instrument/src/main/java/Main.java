import com.example.ondiskinstrumentation.JarTransformer;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("jarcache");
        List<Path> jars = Stream.of(args).map(Path::of).collect(Collectors.toList());
        JarTransformer transformer = new JarTransformer(tempDir, jars);
        URL[] redefinedJars = jars.stream().map(transformer::redefineJar).map(Path::toUri).map((URI uri) -> {
            try {
                return uri.toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toArray(URL[]::new);

        URLClassLoader childClassloader = new URLClassLoader(redefinedJars);
        Class<?> coreClass = childClassloader.loadClass("com.example.MainCore");

        try {
            coreClass.getMethod("doMain").invoke(null);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof SecurityException) {
                // Security exception is expected there because the class is rewritten
                System.err.println(e.getCause().toString());
            } else {
                throw e;
            }
        }
    }
}
