Intercepting JDK method calls with agent-based instrumentation
==============================================================

This project investigates how to use ByteBuddy to instrument core JDK classes to intercept certain "interesting"
methods.

Run `./gradlew test-app-premain-init:run` to see it in action.

The project is structured as follows:
 - `agent` - the Java agent implemented with ByteBuddy that performs instrumentation
 - `buildSrc` - a set of convention plugins to set up stuff
 - `interceptor` - a library that goes into bootclasspath. The instrumented code calls into it. The code that wants to 
    observe the instrumented calls registers callbacks into it too.
 - `test-app-premain-init` - a launcher that configures agent and bootclasspath with command-line switches
 - `test-core` - a core implementation of the launcher, independent of the way agent is set up.
