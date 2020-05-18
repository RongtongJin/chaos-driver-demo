#!/bin/bash

main=$1
keyword=${main##*.}
echo $keyword
PROG_NAME=$0
ACTION=$2



usage() {
    echo "Usage: $PROG_NAME {start|stop|restart}"
    exit 2 # bad usage
}

if [ $# -lt 1 ]; then
    usage
    exit 2 # bad usage
fi

APP_HOME=$(cd $(dirname $0)/..; pwd)

#source "$APP_HOME/bin/setenv.sh"

startjava() {
    echo "[ 1/10] -- start java process"
    echo "[ 1/10] -- main class: $main"
    cd ${EXPLODED_TARGET} || exit 1
    echo "        -- java stdout log: ${JAVA_OUT}"
    echo "[ 2/10] -- start java process"
    echo "[ 2/10] -- $JAVA_HOME/bin/java -cp $CLASSPATH $main &"
    $JAVA $JAVA_OPTS -cp $CLASSPATH $main $3 --driver conf/rocketmq.yaml --agent
    echo "[ 3/10] -- check health for java application"
    #. "$APP_HOME/bin/preload.sh"
    [[ $? -ne 0 ]] && ( echo "check heath failed, exit" && exit 1 )
}

stopjava() {
    echo "[ 1/10] -- stop java process"
    echo "[ 1/10] -- keyword: $keyword"
    times=60
    for e in $(seq 60)
    do
        sleep 1
        COSTTIME=$(($times - $e ))
        checkjavapida=`ps -ef|grep java|grep $keyword|grep -v appctl.sh|grep -v jbossctl| grep -v restart.sh |grep -v grep`
        if [[ $checkjavapida ]];then
                checkjavapid=`ps -ef|grep java|grep $keyword|grep -v appctl.sh|grep -v jbossctl | grep -v restart.sh |grep -v grep|awk '{print $2}'`
                kill -9 $checkjavapid
                echo -n -e  "\r        -- stopping java lasts `expr $COSTTIME` seconds."
        else
                break;
        fi
    done
    echo ""
}

case "$ACTION" in
    start)
        stopjava
        startjava
    ;;
    stop)
        stopjava
    ;;
    *)
        usage
    ;;
esac