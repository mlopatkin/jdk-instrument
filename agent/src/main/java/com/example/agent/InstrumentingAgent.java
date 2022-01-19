package com.example.agent;

import com.example.interceptor.advices.*;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class InstrumentingAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) throws IOException {
        inst.appendToBootstrapClassLoaderSearch(new JarFile(System.getProperty("com.example.bootstrap")));
        doMain(inst);
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        doMain(inst);
    }

    public static void doMain(Instrumentation inst) {
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
                .type(named("java.io.FileInputStream"))
                .transform((builder, type, classLoader, module) ->
                        builder
                                .visit(Advice.to(FisStringAdvice.class).on(
                                        isConstructor().and(takesArguments(String.class))))
                                .visit(Advice.to(FisFileAdvice.class).on(
                                        isConstructor().and(takesArguments(File.class)))))
                .type(named("java.lang.System"))
                .transform((builder, type, classLoader, module) ->
                        builder
                                .visit(Advice.to(SystemAdvice.class).on(named("getenv").and(takesArguments(0)))))
                .type(named("java.lang.Integer"))
                .transform((builder, type, classLoader, module) ->
                        builder
                                .visit(Advice.to(IntegerAdvice.class).on(named("getInteger"))))
                .type(named("org.eclipse.collections.impl.map.mutable.UnifiedMap"))
                .transform((builder, type, classLoader, module) ->
                        builder
                                .visit(Advice.to(UnifiedMapAdvice.class).on(isConstructor())))
                .installOn(inst);
    }

}
