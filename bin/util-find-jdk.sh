#!/bin/sh -f
############################################################################################################################
#
# jdksetup script (to be sourced from some other 'sh' or 'bash' script)
#
# Searches for `java' in "standard" locations, then sets and returns ${JAVA} and ${JAVA_HOME}
# ${JAVA}       --> path to the executable
# ${JAVA_HOME}  --> path to the JDK installation
#
# Searches the following locations (trying in that order):
#
# 1. ${JAVA_HOME}/bin/java                                        if environment variable JAVA_HOME is defined
# 2. <mydir>/../../${JDK_OS}/jre/bin/java                         <mydir> is dir containing this script itself (auto detected) 
#                                                                 if JDK_OS is not defined --> auto detected on-the-fly
#                                                                 if JDK_VERSION is not defined -> defaults to 'jdk-1.4'
# 3. ${JAVA_BASE}/${JDK_OS}/jdk/${JDK_VERSION}/bin/java      if JAVA_BASE is not defined -> defaults to '/home/g5/users/hoschek/java'
# 4. java                                                         if all above fails, hoping that some 'java' is in the PATH
#
# author:    whoschek@lbl.gov
# version:   1.0.0 - 05 June 2003
# changelog: no changes so far
#
############################################################################################################################

# detect the directory containing this file (the root of this installation)
_MYDIR="`dirname $0`"
#echo _MYDIR=$_MYDIR

# detect operating system
if [ -z "$JDK_OS" ] ; then
	JDK_OS=`uname -s` > /dev/null 2>&1
	case "$JDK_OS" in
		Linux*)   JDK_OS="i386_redhat61";;
		SunOS*)   JDK_OS="sparc_solaris26";;
		Darwin*)  JDK_OS="darwin";;
		FreeBSD*) JDK_OS="freebsd";;
		HP-UX*)   JDK_OS="hp_ux102";;
		AIX*)     JDK_OS="rs_aix43";;
		OSF*)     JDK_OS="alpha_dux40";;
		IRIX*)    JDK_OS="sgi_64";;
		*)        JDK_OS="undefined";;
	esac
fi

# try various paths for java installation

# try this path
jpath="/usr/j2se"
if [ -x "$jpath/bin/java" ]; then
	jhome=$jpath
fi

# try this path
jpath="/afs/cern.ch/sw/java/$JDK_OS/jdk/jdk-1.5"
if [ -x "$jpath/bin/java" ]; then
	jhome=$jpath
fi

# try this path
jpath="/usr/local/java2/jdk/jdk-1.5"
if [ -x "$jpath/bin/java" ]; then
	jhome=$jpath
fi

# try this path
jpath="/home/dsd/java2/jdk/jdk-1.5"
if [ -x "$jpath/bin/java" ]; then
	jhome=$jpath
fi

#
# if you want, add other "standard" locations here
#

# try this path
jpath="$_MYDIR/../../$JDK_OS/jre"
if [ -x "$jpath/bin/java" ]; then
	jhome=$jpath
fi

# try this path
if [ ! -z "$JAVA_HOME" ] ; then
	jpath="$JAVA_HOME"
	if [ -x "$jpath/bin/java" ]; then
		jhome=$jpath
	fi
fi

# try to find in PATH, unless already found elsewhere above
if [ -z "$jhome" ] ; then
	JAVA="`which java 2>&1`"
	# strip away trailing '/bin/java'
	jhome=`echo $JAVA | sed "s/\/bin\/java//g"`
fi


JAVA_HOME="$jhome"
JAVA="$jhome/bin/java"

if [ ! -x "$JAVA" ] ; then
	echo "Cannot find or execute 'java' from $JAVA. Please check file permissions, or set your PATH or JAVA_HOME."
	echo "(last tried JAVA=$JAVA)"
	echo "(last tried JAVA_HOME=$JAVA_HOME)"
	exit 1
fi


export JAVA
export JAVA_HOME
export JDK_OS

#echo "javahome is $JAVA_HOME"
