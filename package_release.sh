#!/bin/bash

# START.
if [ $# -ne 1 ]; then
	echo "This script packages a release of BasicRtc which can be distributed as a single ZIP file."
	echo "Missing argument: RELEASE_NAME"
	exit 1
fi
RELEASE_NAME="$1"

echo "Building clean..."
mvn clean install
if [ $? -ne 0 ]; then
	echo "Failure!"
	echo 2
fi

echo "Cleaning packaging environment..."
rm -rf dist

echo "Populating packaging environment..."
mkdir dist
mkdir dist/BasicRtc
mkdir dist/BasicRtc/www
mkdir dist/BasicRtc/www/chat
cp index.html dist/BasicRtc/www/chat/
cp target/BasicRtc-0.0-SNAPSHOT-jar-with-dependencies.jar "dist/BasicRtc/BasicRtc-$RELEASE_NAME.jar"

echo "Creating zip..."
cd dist
zip -r "BasicRtc-$RELEASE_NAME.zip" BasicRtc

echo "Package created: \"dist/BasicRtc-$RELEASE_NAME.zip\""

