#!/usr/bin/env bash

set -euo pipefail
set -E -o functrace

trap 'failure "LINENO" "BASH_LINENO" "${BASH_COMMAND}" "${?}"' ERR


cd org.eclipse.lyo.testsuite.recipes
mvn clean install
cd ../
cd org.eclipse.lyo.testsuite.server
git checkout -- src/main/java/org/eclipse/lyo/testsuite/oslcv2

# mvn org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.activeRecipes=org.eclipse.lyo.testsuite.refactor.SimplifySelectorPattern
mvn org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.activeRecipes=org.eclipse.lyo.testsuite.JenaUpgrade

git diff src/main/java/org/eclipse/lyo/testsuite/oslcv2
cd ..


cd org.eclipse.lyo.testsuite.trs
mvn org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.activeRecipes=org.eclipse.lyo.testsuite.JenaUpgrade
