#!/bin/sh

FULLPATH="`pwd`/$0"
DIR=`dirname "$FULLPATH"`

src="$DIR"/src
#lib="$DIR"/lib
build="$DIR"/_build
archive="$DIR"/archive
#doc="$DIR"/doc

#linux / osx different mktemp call
#TMPFILE=`mktemp 2>/dev/null || mktemp -t /tmp`

#NOW=`date +"%s"`

jsource=1.6
jtarget=1.6

JAVAC="javac -source $jsource -target $jtarget -nowarn"

jetty_dist_name=jetty-distribution-9.2.10.v20150310
#jetty_dist_name=jetty-distribution-9.3.8.v20160314

jetty_tarball="$jetty_dist_name".tar.gz

osm_renderer_build_dir=../java_osm_renderer/_build

#========================================================================
checkAvail()
{
	which "$1" >/dev/null 2>&1
	ret=$?
	if [ $ret -ne 0 ]
	then
		echo "tool \"$1\" not found. please install"
		exit 1
	fi
}

#========================================================================
compile()
{
	echo "building..."
	echo "==========="

	cp "$archive"/"$jetty_tarball" "$build"
	cd "$build"
	tar xf "$jetty_tarball"
	cd "$DIR"
	jetty_home="$build"/"$jetty_dist_name"
	jetty_libs=`echo $(ls -1 "$jetty_home"/lib/*.jar) | sed 's/ /:/g'`

	$JAVAC -classpath "$build":"$jetty_libs":"$osm_renderer_build_dir" -sourcepath "$src" -d "$build" "$src"/*.java "$src"/handlers/*.java 
}

#========================================================================
run()
{
	echo "running WebServer"
	echo "================="
	jetty_home="$build"/"$jetty_dist_name"
	jetty_libs=`echo $(ls -1 "$jetty_home"/lib/*.jar) | sed 's/ /:/g'`":"`echo $(ls -1 "$jetty_home"/lib/websocket/*.jar) | sed 's/ /:/g'`

	java -Xms3000M -Xmx3000M -classpath "$build":"$jetty_libs":"$osm_renderer_build_dir" WebServer

}

for tool in java javac jar javadoc; \
	do checkAvail "$tool"; done

mkdir -p "$build"
rm -rf "$build"/*

compile
run
