#!/bin/bash

export JAVA_HOME

export JAVA="$JAVA_HOME/bin/java"

export APP_HOME=$(cd $(dirname $0)/..; pwd)

export MAIN_CLASS=io.openmessaging.chaos.driver.demo.Main

export CLASSPATH=.:${APP_HOME}/lib/*:${CLASSPATH}

$JAVA -cp ${CLASSPATH} ${MAIN_CLASS} $@