package com.example.classloaderinstrumentation;

import com.example.interceptor.advices.UnifiedMapAdvice;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RewritingClassloader extends ClassLoader {
    private final ClassFileLocator allClasspath;
    private final TypePool allClasspathPool;
    private final URLClassLoader innerClassLoader;

    private final Map<ProtectionDomain, ProtectionDomain> pdCache = new ConcurrentHashMap<>();

    public RewritingClassloader(URL[] urls) {
        super();

        this.innerClassLoader = new URLClassLoader(urls);

        this.allClasspath = ClassFileLocator.ForClassLoader.of(innerClassLoader);
        this.allClasspathPool = TypePool.Default.of(this.allClasspath);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Due to the way ClassLoader works, findClass is only invoked for classes not in the parent classloader.
        TypePool.Resolution r = allClasspathPool.describe(name);
        if (!r.isResolved()) {
            throw new ClassNotFoundException("Failed to load " + name);
        }
        // Resolve class in the inner class loader first, to verify signatures and obtain a ProtectionDomain.
        // We cannot extend URLClassLoader and override findClass: we have to invoke super.findClass to get a proper
        // ProtectionDomain but after that it is too late to redefine the class with the instrumented version in the
        // classloader as the super.findClass defines the class. Unfortunately, URLClassLoader doesn't expose anything
        // related to CodeSource set up for us to reuse. The base SecureClassLoader has defineClass(byte[], CodeSource)
        // which are perfect to add instrumentation into, but these are final.
        //
        // We cannot return the inner class, as it might use the classes that we want to instrument, so it will
        // reference non-instrumented classes loaded in the innerClassLoader. Therefore, the only option is to reload
        // the class later.
        Class<?> innerClass = innerClassLoader.loadClass(name);
        ProtectionDomain pd = getMyProtectionDomain(innerClass.getProtectionDomain());
        if (!UnifiedMapAdvice.TARGET_CLASS_NAME.equals(name)) {
            // The class needs no rewrite. Define it in this classloader.
            try {
                byte[] classBytes = allClasspath.locate(name).resolve();
                return defineClass(name, classBytes, 0, classBytes.length, pd);
            } catch (IOException e) {
                throw new ClassNotFoundException("Failed to load " + name, e);
            }
        }
        // This class should be rewritten.
        byte[] redefinedClass = new ByteBuddy()
                .redefine(innerClass, allClasspath)
                .visit(Advice.to(UnifiedMapAdvice.class).on(ElementMatchers.isConstructor()))
                .make()
                .getBytes();
        return defineClass(name, redefinedClass, 0, redefinedClass.length, pd);
    }

    private ProtectionDomain getMyProtectionDomain(ProtectionDomain originalDomain) {
        return pdCache.computeIfAbsent(originalDomain, this::makeProtectionDomain);
    }

    private ProtectionDomain makeProtectionDomain(ProtectionDomain d) {
        return new ProtectionDomain(d.getCodeSource(), d.getPermissions(), this, d.getPrincipals());
    }
}
