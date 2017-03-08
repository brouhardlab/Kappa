#!/usr/bin/env sh
set -e

# Define some variables
export USER="Kappa_site"
export UPDATE_SITE="Kappa_site"

export IJ_PATH="$HOME/Fiji.app"
export URL="http://sites.imagej.net/$UPDATE_SITE/"
export IJ_LAUNCHER="$IJ_PATH/ImageJ-linux64"
export PATH="$IJ_PATH:$PATH"

# Install ImageJ
mkdir -p $IJ_PATH/
cd $HOME/
wget --no-check-certificate https://downloads.imagej.net/fiji/latest/fiji-linux64.zip
unzip fiji-linux64.zip

# Install the package
cd $TRAVIS_BUILD_DIR/
mvn clean install -Dimagej.app.directory=$IJ_PATH -Ddelete.other.versions=true

# Deploy the package
$IJ_LAUNCHER --update edit-update-site $UPDATE_SITE $URL "webdav:$USER:$WIKI_UPLOAD_PASS" .
$IJ_LAUNCHER --update upload --update-site $UPDATE_SITE --force-shadow jars/kappa.jar