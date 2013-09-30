#!/usr/bin/env bash
#
# TODO: You must run this script from this directory
# every time you login as follows:
#
#   source setup.sh
#

export LANG=en_US.utf8

#
# DO NOT CHANGE THIS PATH!
#
export CORENLP="/afs/ir/class/cs224n/bin/corenlp"


#
# Setup the CLASSPATH for Java and the PATH
# for Phrasal scripts
#
BASEDIR=`pwd`
export CLASSPATH="${CORENLP}/*:${BASEDIR}/code/lib/*:${BASEDIR}/code/classes"
export PATH=${PATH}:${BASEDIR}/code/scripts

