#!/bin/sh

if [ -z "$UPDATE_SITE_NAME" ]; then
	# Silently exit if UPDATE_SITE_NAME is not set.
	exit 0
fi

if [ -n "$TRAVIS_TAG" ]; then

	echo "== Travis tag detected. Start uploading the specified update site =="

	if [ -z "$UPDATE_SITE_PASSWORD" ]; then
		echo "The variable UPDATE_SITE_PASSWORD is not set. You need to set it in the Travis configuration."
		exit -1
	fi

	if [ -z "$UPLOAD_WITH_DEPENDENCIES" ]; then
		echo "The variable UPLOAD_WITH_DEPENDENCIES is not set. You need to set it in the Travis configuration."
		echo "It can be either 'true' or 'false'."
		exit -1
	fi

	echo "Setup variables."
	URL="http://sites.imagej.net/$UPDATE_SITE_NAME/"
	IJ_PATH="$HOME/Fiji.app"
	IJ_LAUNCHER="$IJ_PATH/ImageJ-linux64"

	echo "Installing Fiji."
	mkdir -p $IJ_PATH/
	cd $HOME/
	wget -q --no-check-certificate "https://downloads.imagej.net/fiji/latest/fiji-linux64.zip"
	unzip -q fiji-linux64.zip

	echo "Updating Fiji."
	$IJ_LAUNCHER --update update-force-pristine

	echo "Install project to Fiji directory."
	cd $TRAVIS_BUILD_DIR/
	mvn clean install -Dimagej.app.directory=$IJ_PATH -Ddelete.other.versions=true

	echo "Gather some project informations."
	VERSION=`mvn help:evaluate -Dexpression=project.version | grep -e '^[^\[]'`
	NAME=`mvn help:evaluate -Dexpression=project.name | grep -e '^[^\[]'`

	echo "Adding $URL as an Update Site."
	$IJ_LAUNCHER --update edit-update-site $UPDATE_SITE_NAME $URL "webdav:$UPDATE_SITE_NAME:$UPDATE_SITE_PASSWORD" .

	if [ "$UPLOAD_WITH_DEPENDENCIES" = false ]; then
	    echo "Upload only \"jars/$NAME.jar\"."
	    $IJ_LAUNCHER --update upload --update-site "$UPDATE_SITE_NAME" --force-shadow --forget-missing-dependencies "jars/$NAME.jar"
	else
		echo "Upload $NAME with its dependencies."
		$IJ_LAUNCHER --update upload-complete-site --force-shadow "$UPDATE_SITE_NAME"
	fi
else
	echo "== Travis tag not detected. Don't perform upload to update site =="
fi