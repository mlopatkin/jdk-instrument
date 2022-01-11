Intercepting JDK method calls with agent-based instrumentation
==============================================================

This project investigates how to use ByteBuddy to instrument core JDK classes to intercept certain "interesting"
methods.

Use `./run_app_with_agent.sh` to see it in action.

Root project is the Java agent that instruments the bootstrap class loader. The testapp is the app that both calls 
instrumented methods and handles interceptions. 