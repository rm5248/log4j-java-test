#!/bin/sh -e
#
# Removes unwanted content from the upstream sources.
# Called by uscan with '--upstream-version' <version> <file>
#

VERSION=$2
TAR=../apache-log4j2_$VERSION.orig.tar.xz
DIR=apache-log4j2-$VERSION
TAG=$(echo "log4j-$VERSION" | sed -re's/~/-/')

svn export http://svn.apache.org/repos/asf/logging/log4j/log4j2/tags/${TAG}/ $DIR
XZ_OPT=--best tar -c -J -f $TAR --exclude '*.jar' --exclude '*.class' $DIR
rm -rf $DIR ../$TAG

# move to directory 'tarballs'
if [ -r .svn/deb-layout ]; then
  . .svn/deb-layout
  mv $TAR $origDir && echo "moved $TAR to $origDir"
fi
