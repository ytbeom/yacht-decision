#!/bin/bash

BUILD_FOLDER="build\libs"
JAR_NAME="yacht-decision-0.0.1-SNAPSHOT.jar"

gradle bootjar
cd "$BUILD_FOLDER"
java -jar "$JAR_NAME"