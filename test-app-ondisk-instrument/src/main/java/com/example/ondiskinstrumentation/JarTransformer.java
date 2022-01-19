package com.example.ondiskinstrumentation;

import com.example.interceptor.advices.UnifiedMapAdvice;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JarTransformer {
    private final Path targetDirectory;
    private final ClassFileLocator allClasspath;
    private final TypePool allClasspathPool;

    public JarTransformer(Path targetDirectory, List<Path> allClasspath) {
        this.targetDirectory = targetDirectory;
        this.allClasspath = new ClassFileLocator.Compound(
                Stream.concat(
                        allClasspath.stream().map(JarTransformer::locatorForJar),
                        Stream.of(ClassFileLocator.ForClassLoader.ofSystemLoader())
                ).collect(Collectors.toList()));
        this.allClasspathPool = TypePool.Default.of(this.allClasspath);
    }

    public Path redefineJar(Path pathToJar) {
        try {
            return redefineJarImpl(pathToJar);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path redefineJarImpl(Path pathToJar) throws IOException {
        if (!isClassInJar(UnifiedMapAdvice.TARGET_CLASS_NAME, pathToJar)) {
            return pathToJar;
        }
        System.err.println("Found UnifiedMap in " + pathToJar.getFileName());
        Path redefinedJar = targetDirectory.resolve(pathToJar.getFileName());
        Files.createDirectories(targetDirectory);
        // Pool with the full classpath is needed to resolve all superclasses and superinterfaces of the target class.
        new ByteBuddy()
                .redefine(allClasspathPool.describe(UnifiedMapAdvice.TARGET_CLASS_NAME).resolve(), allClasspath)
                .visit(Advice.to(UnifiedMapAdvice.class).on(ElementMatchers.isConstructor()))
                .make()
                .inject(pathToJar.toFile(), redefinedJar.toFile());
        return redefinedJar;
    }

    private boolean isClassInJar(String className, Path pathToJar) throws IOException {
        // Use a small pool to check the actual JAR file. It might be easier to just check the UnifiedMap.class though.
        ClassFileLocator jarLocator = ClassFileLocator.ForJarFile.of(pathToJar.toFile());
        TypePool jarTypePool = TypePool.Default.of(jarLocator);
        TypePool.Resolution resolvedType = jarTypePool.describe(className);
        return resolvedType.isResolved();
    }

    private static ClassFileLocator locatorForJar(Path jarPath) {
        try {
            return ClassFileLocator.ForJarFile.of(jarPath.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
