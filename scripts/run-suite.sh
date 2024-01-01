#!/usr/bin/env bash

# Get the parent directory of the current file
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PARENT_DIR="$(dirname "$SCRIPT_DIR")"

# echo "Parent directory: $PARENT_DIR"
pushd "${PARENT_DIR}" || exit 1

mvn -B -f org.eclipse.lyo.testsuite.build/pom.xml clean package

mvn -B -f org.eclipse.lyo.testsuite.server/pom.xml clean test -DskipTests=false -Dtest=org.eclipse.lyo.testsuite.DynamicSuiteBuilder -Dprops=config/refimpl2020/cm/suiteconfig.properties -fae

popd || exit
