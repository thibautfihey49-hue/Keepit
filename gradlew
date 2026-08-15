#!/bin/sh
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  PRG=`dirname "$PRG"`/"$link"
done
APP_HOME="`cd \"\`dirname \\\"$PRG\\\"\`\" && pwd`"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
JAVA_EXE="java"
[ -n "$JAVA_HOME" ] && JAVA_EXE="$JAVA_HOME/bin/java"
exec "$JAVA_EXE" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
