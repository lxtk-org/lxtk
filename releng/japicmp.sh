#!/bin/bash

###############################################################################
# Copyright (c) 2022 1C-Soft LLC and others.
#
# This program and the accompanying materials are made available under
# the terms of the Eclipse Public License 2.0 which is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Vladimir Piskarev (1C) - initial API and implementation
###############################################################################

# JApiCmp script
#
# This script invokes JApiCmp (https://siom79.github.io/japicmp/CliTool.html)
# to report API changes for all jars in the 'new' subdirectory against the
# corresponding jars in the 'old' subdirectory.
#
# Requirements:
# * japicmp-0.17.1-jar-with-dependencies.jar in the current directory
# * new jars in the 'new' subdirectory
# * old jars in the 'old' subdirectory
#
# Usage: ./japicmp.sh

function join() {
    local IFS=$1
    shift
    echo "$*"
}

java -jar japicmp-0.17.1-jar-with-dependencies.jar --old $(join ';' old/*.jar) --new $(join ';' new/*.jar) --exclude '*.internal.*' --ignore-missing-classes --only-modified --html-file japicmp.html > japicmp.diff
