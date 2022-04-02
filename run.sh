#!/bin/bash

BUILD_FOLDER="build\libs"
JAR_NAME="yacht-decision-0.0.1-SNAPSHOT.jar"

if [ ! -d "$BUILD_FOLDER" ] ; then
 gradle bootjar
fi

cd "$BUILD_FOLDER"
java -jar "$JAR_NAME"