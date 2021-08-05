#!/bin/bash

# `-d64` option was removed in Java 10, this handles these versions accordingly
JAVA_FLAGS=""
if (( $(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1) < 10 )); then
    JAVA_FLAGS="-d64"
fi

DO_RAMDISK=0
if [[ $(cat server-setup-config.yaml | grep 'ramDisk:' | awk 'BEGIN {FS=":"}{print $2}') =~ "yes" ]]; then
    SAVE_DIR=$(cat server.properties | grep 'level-name' | awk 'BEGIN {FS="="}{print $2}')
    mv $SAVE_DIR "${SAVE_DIR}_backup"
    mkdir $SAVE_DIR
    sudo mount -t tmpfs -o size=2G tmpfs $SAVE_DIR
    DO_RAMDISK=1
fi
if [ -f serverstarter-@@serverstarter-libVersion@@.jar ]; then
    echo "Skipping download. Using existing serverstarter-@@serverstarter-libVersion@@.jar"
    java $JAVA_FLAGS -jar serverstarter-@@serverstarter-libVersion@@.jar
    if [[ $DO_RAMDISK -eq 1 ]]; then
        sudo umount $SAVE_DIR
        rm -rf $SAVE_DIR
        mv "${SAVE_DIR}_backup" $SAVE_DIR
    fi
    exit 0
else
    export URL="https://github.com/BloodyMods/ServerStarter/releases/download/v@@serverstarter-libVersion@@/serverstarter-@@serverstarter-libVersion@@.jar"
fi
echo $URL
which wget >> /dev/null
if [ $? -eq 0 ]; then
    echo "DEBUG: (wget) Downloading ${URL}"
    wget -O serverstarter-@@serverstarter-libVersion@@.jar "${URL}"
else
    which curl >> /dev/null
    if [ $? -eq 0 ]; then
        echo "DEBUG: (curl) Downloading ${URL}"
        curl -o serverstarter-@@serverstarter-libVersion@@.jar -L "${URL}"
    else
        echo "Neither wget or curl were found on your system. Please install one and try again"
    fi
fi
java $JAVA_FLAGS -jar serverstarter-@@serverstarter-libVersion@@.jar
if [[ $DO_RAMDISK -eq 1 ]]; then
    sudo umount $SAVE_DIR
    rm -rf $SAVE_DIR
    mv "${SAVE_DIR}_backup" $SAVE_DIR
fi
