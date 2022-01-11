package com.example;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class EntryPoint {
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
        @Advice.OnMethodExit()
        public static void getenv(@Advice.Return(readOnly = false) Map<String, String> envMap) {
            Map<String, String> newResult = new HashMap<>(envMap);
            newResult.put("WEDNESDAY", "WHAT A WEEK");
            //noinspection UnusedAssignment
            envMap = newResult;
        }
    }

    public static class IntegerAdvice {
        @Advice.OnMethodExit
        public static void getEnter(@Advice.Argument(0) String key, @Advice.Return Integer value) {
            Interceptor.getInstance().onSystemPropertyRead(key, value);
        }
    }
}
