#!/bin/sh

gw -q shadowJar  || exit 1

AGENT_JAR=build/libs/jdk-instrument-1.0-SNAPSHOT-all.jar
java \
  -Xbootclasspath/a:$AGENT_JAR \
  -jar testapp/build/libs/testapp-1.0-SNAPSHOT-all.jar
