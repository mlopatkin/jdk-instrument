Intercepting JDK method calls with agent-based instrumentation
==============================================================

This project investigates how to use ByteBuddy to instrument core JDK classes and signed JARs to intercept certain
"interesting" methods.

Run `./gradlew run` to see it in action.

The project is structured as follows:
 - `agent` - the Java agent implemented with ByteBuddy that performs instrumentation.
 - `buildSrc` - a set of convention plugins to set up stuff.
 - `interceptor` - a library that goes into bootclasspath. The instrumented code calls into it. The code that wants to 
    observe the instrumented calls registers callbacks into it too.
 - `interceptor-advices` - ByteBuddy advices used to declare how to instrument classes. These are used in both agent
    based and classloader-based instrumentations, though the latter cannot instrument JDK classes.
 - `test-app-premain-init` - a launcher that configures agent and bootclasspath with command-line switches.
 - `test-app-runtime-init` - a launcher that installs agent and bootclasspath at runtime.
 - `test-app-ondisk-instument` - instruments JAR files on disk and then loads them at runtime. As the signed JAR is
    instrumented, the run fails with `SecurityException`.
 - `test-core` - a core implementation of the launcher, independent of the way agent is set up.
