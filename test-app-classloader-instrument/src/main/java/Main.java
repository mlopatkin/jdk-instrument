import com.example.classloaderinstrumentation.RewritingClassloader;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws Exception {
        URL[] jars = Stream.of(args).map(Path::of).map(Path::toUri).map((URI uri) -> {
            try {
                return uri.toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toArray(URL[]::new);

        ClassLoader childClassloader = new RewritingClassloader(jars);
        Class<?> coreClass = childClassloader.loadClass("com.example.MainCore");

        coreClass.getMethod("doMain").invoke(null);
    }
}
