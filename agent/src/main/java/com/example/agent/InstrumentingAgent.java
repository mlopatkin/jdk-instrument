package com.example.agent;

import com.example.interceptor.Interceptor;
import com.example.interceptor.TrackHashMap;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class InstrumentingAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
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
                .installOn(inst);
    }

    // Advice classes are defining the template but there is no way to say what method should be instrumented. This
    // info has to be provided when configuring builder in the transform above. This means that we'll have quite a lot
    // of Advice classes, almost one per instrumented method.
    public static class FisStringAdvice {
        @Advice.OnMethodEnter
        public static void constructorExit(@Advice.Argument(0) String path) {
            // One pitfall there is that we are going to get interception twice, because FileInputStream(String)
            // delegates to FileInputStream(File) internally.
            // We could in theory report and disable interception in the beginning of the constructor and then re-enable
            // interception again at the end, but exceptions complicate this. We cannot wrap the actual constructor code
            // in try(withInterceptionDisabled()) {}, which is necessary to reliably restore the interception.
            // See https://github.com/raphw/byte-buddy/issues/375
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
            envMap = new TrackHashMap(envMap);
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