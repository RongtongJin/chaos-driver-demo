#!/bin/bash

export MAIN_CLASS=io.openmessaging.chaos.driver.demo.Main

pid=`ps ax | grep -i ${MAIN_CLASS} |grep java | grep -v grep | awk '{print $1}'`

if [ -z "$pid" ] ; then
        echo "No program running."
        exit -1;
fi

echo "The program (${pid}) is running..."

kill ${pid}

echo "Send shutdown request to program (${pid}) OK"