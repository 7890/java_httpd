#!/bin/sh

FULLPATH="`pwd`/$0"
DIR=`dirname "$FULLPATH"`

src="$DIR"/src
#lib="$DIR"/lib
build="$DIR"/_build
archive="$DIR"/archive
#doc="$DIR"/doc

MCKOI="$archive/mckoidb.jar"
ST="$archive/ST-4.0.9.jar"
ANTLR="$archive/antlr-3.5.2-complete.jar"

#linux / osx different mktemp call
#TMPFILE=`mktemp 2>/dev/null || mktemp -t /tmp`

#NOW=`date +"%s"`

jsource=1.6
jtarget=1.6

JAVAC="javac -source $jsource -target $jtarget -nowarn"
JAVA="java -Xms500M -Xmx1000M "

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

#echo	$JAVAC -classpath .:"$build":"$jetty_libs":"$ST":"$osm_renderer_build_dir" -sourcepath "$src" -d "$build" "$src"/*.java "$src"/handlers/*.java "$src"/interfaces/*.java "$src"/hooks/*.java "$src"/util/*.java
	$JAVAC -classpath .:"$build":"$jetty_libs":"$MCKOI":"$ST":"$osm_renderer_build_dir" -sourcepath "$src" -d "$build" "$src"/*.java "$src"/handlers/*.java "$src"/interfaces/*.java "$src"/hooks/*.java "$src"/util/*.java
}

#========================================================================
run()
{
	echo "running WebServer"
	echo "================="
	jetty_home="$build"/"$jetty_dist_name"
#	jetty_libs=`echo $(ls -1 "$jetty_home"/lib/*.jar) | sed 's/ /:/g'`":"`echo $(ls -1 "$jetty_home"/lib/websocket/*.jar) | sed 's/ /:/g'`

	jlib="$jetty_home"/lib
	jetty_libs=""
	jetty_libs="${jetty_libs}:"
	jetty_libs="${jetty_libs}:"${jlib}/jetty-http-9.2.10.v20150310.jar
	jetty_libs="${jetty_libs}:"${jlib}/jetty-io-9.2.10.v20150310.jar
	jetty_libs="${jetty_libs}:"${jlib}/jetty-server-9.2.10.v20150310.jar
	jetty_libs="${jetty_libs}:"${jlib}/jetty-util-9.2.10.v20150310.jar
	jetty_libs="${jetty_libs}:"${jlib}/servlet-api-3.1.jar

echo	$JAVA -classpath .:"$build":"$jetty_libs":"$MCKOI":"$ST":"$ANTLR":"$osm_renderer_build_dir" WebServer
	$JAVA -classpath .:"$build":"$jetty_libs":"$MCKOI":"$ST":"$ANTLR":"$osm_renderer_build_dir" WebServer
#	java -verbose:class ... >/tmp/out.txt 2>&1
#	cat /tmp/out.txt | grep "\[Loaded" | grep "\.jar" | rev | cut -d"/" -f1 | rev | sort | uniq | cut -d"]" -f1 >/tmp/jars.txt
}

for tool in java javac jar javadoc; \
	do checkAvail "$tool"; done

mkdir -p "$build"
rm -rf "$build"/*

compile
run
