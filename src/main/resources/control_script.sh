#!/bin/bash

# These properties are filled by Maven upon build.
APP_NAME=@project.artifactId@
APP_VERSION=@project.version@
APP_BUILD_TIMESTAMP="@timestamp@"

# These ones are totally environmental
LOG_FOLDER=/var/log/app.services
RUN_FOLDER=/var/run/app.services

PID_FILE=${LOG_FOLDER}/${APP_NAME}.pid
LOG_FILE=${RUN_FOLDER}/${APP_NAME}.log

# And these ones once again are app specific
APP_HOME=/opt/${APP_NAME}
APP_JAR_PATH=${APP_HOME}/${APP_NAME}-${APP_VERSION}.jar
APP_LOG_FILE=${RUN_FOLDER}/${APP_NAME}.out
APP_ERR_FILE=${RUN_FOLDER}/${APP_NAME}.err

JVM_OPTS="-Xms256M -Xmx1024M"
DATE=$(date +%F\ %T)

_log(){
    MESSAGE=$1
    echo "${DATE} - ${MESSAGE}" >> ${LOG_FILE}
}

start(){
    if [ -f ${PID_FILE} ]; then
        echo "${APP_NAME} is still running"
        return 1
    fi

    _log "Starting ${APP_NAME} ${APP_VERSION} built on ${APP_BUILD_TIMESTAMP} with options ${JVM_OPTS}"

    nohup java ${JVM_OPTS} -jar ${APP_JAR_PATH} 1>${APP_LOG_FILE} 2>${APP_ERR_FILE} &

    PID=$!
    echo ${PID} > ${PID_FILE}
    _log "${APP_NAME} started with pid ${PID}"
    return 0
}

stop(){
    if [ ! -f ${PID_FILE} ]; then
        echo "${APP_NAME} is not running"
        return 1
    fi
    PID=$(cat ${PID_FILE})
    _log "Stopping ${APP_NAME} with pid ${PID}"
    sudo kill -TERM ${PID}
    sudo rm ${PID_FILE}
    _log "${APP_NAME} stopped"
    return 0
}

restart(){
    stop
    start
}

status(){
    if [ ! -f ${PID_FILE} ]; then
        echo "${APP_NAME} is not running"
        return 1
    fi
    PID=$(cat ${PID_FILE})
    echo "${APP_NAME} running with pid ${PID}"
    return 0
}

eval $1
exit $?

