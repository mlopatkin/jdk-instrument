package com.example;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class InstrumentingAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemError())
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
                .with(AgentBuilder.InstallationListener.StreamWriting.toSystemError())
                .ignore(none())
                .ignore(
                        nameStartsWith("net.bytebuddy.")
                                .or(nameStartsWith("jdk.internal.reflect."))
                                .or(nameStartsWith("java.lang.invoke."))
                                .or(nameStartsWith("com.sun.proxy."))
                )
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                // type - transform go in pair. First matching type predicate wins and applies its transform.
                .type(ElementMatchers.named("java.io.FileInputStream"))
                .transform((builder, type, classLoader, module) ->
                        builder
                                .visit(Advice.to(FisStringAdvice.class).on(
                                        isConstructor().and(takesArguments(String.class))))
                                .visit(Advice.to(FisFileAdvice.class).on(
                                        isConstructor().and(takesArguments(File.class)))))
                .type(ElementMatchers.named("java.lang.System"))
                .transform((builder, type, classLoader, module) ->
                        builder
                                .visit(Advice.to(SystemAdvice.class).on(named("getenv").and(takesArguments(0)))))
                .type(ElementMatchers.named("java.lang.Integer"))
                .transform((builder, type, classLoader, module) ->
                        builder
                                .visit(Advice.to(IntegerAdvice.class).on(named("getInteger"))))
                .installOn(inst);
    }

    // Advice classes are defining the template but there is no way to say what method should be instrumented. This
    // info has to be provided when configuring builder in the transform above. This means that we'll have quite a lot
    // of Advice classes, almost one per instrumented method.
    public static class FisStringAdvice {
        @Advice.OnMethodEnter
        public static void constructorExit(@Advice.Argument(0) String path) {
            Interceptor interceptor = Interceptor.getInstance();
            interceptor.onFileOpened(path);
        }
    }

    public static class FisFileAdvice {
        @Advice.OnMethodEnter
        public static void constructorExit(@Advice.Argument(0) File file) {
            Interceptor interceptor = Interceptor.getInstance();
            interceptor.onFileOpened(file);
        }
    }

    public static class SystemAdvice {
        // Advices can replace the return value of the method
        @Advice.OnMethodExit
        public static void getenv(@Advice.Return(readOnly = false) Map<String, String> envMap) {
            //noinspection UnusedAssignment
            envMap = new HashMap<>(envMap) {
                @Override
                public String get(Object key) {
                    if (key instanceof String) {
                        Interceptor.getInstance().onEnvVarRead((String) key);
                    }
                    return super.get(key);
                }
            };
        }
    }

    public static class IntegerAdvice {
        // Advice can be applied to multiple methods, if the signatures are compatible. Here we intercept all three:
        // Integer.getInteger(String)
        // Integer.getInteger(String, int)
        // Integer.getInteger(String, Integer)
        @Advice.OnMethodExit
        public static void exitGetInteger(@Advice.Argument(0) String key, @Advice.Return Integer value) {
            Interceptor.getInstance().onSystemPropertyRead(key, value);
        }
    }
}
