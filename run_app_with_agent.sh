#!/bin/sh

gw -q shadowJar  || exit 1

java \
  -Xbootclasspath/a:build/libs/bytebuddy-playground-1.0-SNAPSHOT-all.jar \
  -javaagent:build/libs/bytebuddy-playground-1.0-SNAPSHOT-all.jar \
  -jar testapp/build/libs/testapp-1.0-SNAPSHOT-all.jar
