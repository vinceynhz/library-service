#!/usr/bin/env bash

# These properties are filled by Maven upon build.
APP_NAME=@project.artifactId@
APP_VERSION=@project.version@
APP_BUILD_TIMESTAMP="@timestamp@"

# These ones are system environmental
LOG_FOLDER=/var/log/app.services
RUN_FOLDER=/var/run/app.services

PID_FILE=${LOG_FOLDER}/${APP_NAME}.pid
LOG_FILE=${RUN_FOLDER}/${APP_NAME}.log

# Deployment variables
GIT_REPO_URL=https://github.com/vinceynhz/library-service.git
INSTALL_PATH=~/library-service
APP_HOME=/opt/library-service

# And these ones are app execution specific
APP_JAR_PATH=${APP_HOME}/${APP_NAME}-${APP_VERSION}.jar
APP_LOG_FILE=${RUN_FOLDER}/${APP_NAME}.out
APP_ERR_FILE=${RUN_FOLDER}/${APP_NAME}.err

JVM_OPTS="-Xms256M -Xmx1024M"
DATE=$(date +%F\ %T)

if [ ! -f ${LOG_FILE} ]; then
    touch ${LOG_FILE}
fi

# Wrappers
pushd_s () {
    command pushd "$@" > /dev/null
}

popd_s () {
    command popd "$@" > /dev/null
}

_log(){
    MESSAGE=$1
    echo "${DATE} - ${MESSAGE}" >> ${LOG_FILE}
}

_clone_repo(){
    # This will clone the repository
    pushd_s ~
    git clone ${GIT_REPO_URL} 1>>${LOG_FILE} 2>&1
    rc=$?
    popd_s
    return ${rc}
}

_update_repo(){
    # This one will go to the installation folder
    pushd_s ${INSTALL_PATH}
    # and pull from github
    git pull --all 1>>${LOG_FILE} 2>&1
    rc=$?
    popd_s
    return ${rc}
}

install(){
    # First we check if the installation folder exists or not
    if [ ! -d ${INSTALL_PATH} ]; then
        _log "Installation folder doesn't exist. ${INSTALL_PATH}"
        _log "Cloning repository"
        _clone_repo
    else
        # Here we will just update the repo
        _update_repo
    fi
    if [ $? -ne 0 ]; then
        _log "Failed getting repo information"
        return 1
    fi

    # Build the new jar
    _log "Building ${APP_NAME}"
    python3 ${INSTALL_PATH}/bin/build.py

    if [ $? -ne 0 ]; then
        _log "Build failed"
        return 2
    fi

    # We get the new app's name (in case it has changed)
    app_name=$(python3 ${INSTALL_PATH}/bin/jarname.py name)
    if [ $? -ne 0 ]; then
        _log "Unable to read app name"
        return 2
    fi
    app_home=/opt/${app_name}
    if [ ! -d  ${app_home} ]; then
        _log "New APP_HOME is ${app_home}"
        mkdir ${app_home}
        if [ $? -ne 0 ]; then
            _log "Unable to create new APP_HOME: ${app_home}"
            return 3
        fi
    fi

    # We need to copy the new jar and the new control file
    jar_name=$(python3 ${INSTALL_PATH}/bin/jarname.py)
    if [ $? -ne 0 ]; then
        _log "Unable to read jar name"
        return 2
    fi
    cp ${INSTALL_PATH}/target/${jar_name} ${app_home}
    if [ $? -ne 0 ]; then
        _log "Unable to copy jar file into ${app_home}"
        return 4
    fi
    cp ${INSTALL_PATH}/target/classes/control_script.sh ${app_home}
    if [ $? -ne 0 ]; then
        _log "Unable to copy new control_script into ${app_home}"
        return 4
    fi
    # This will stop current execution
    stop
    # After the install is complete, it's expected that whoever issued it, also calls start to run the new version
    # We can't do it here since the app name or version may have changed
    return 0
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

