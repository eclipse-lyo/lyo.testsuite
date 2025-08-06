#!/usr/bin/env bash
set -euo pipefail

# Function to handle errors with descriptive messages
error_exit() {
  echo "Error: ${1:-Unknown error}" >&2
  exit 1
}

# Trap errors and display a user-friendly message
trap 'error_exit "An error occurred in script at line: ${LINENO}, command: ${BASH_COMMAND}"' ERR

# Get the parent directory of the current file
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PARENT_DIR="$(dirname "$SCRIPT_DIR")"

# Change to the parent directory
pushd "${PARENT_DIR}" || error_exit "Failed to change directory to ${PARENT_DIR}"

# Build the project
echo "Building the project..."
mvn -B -f org.eclipse.lyo.testsuite.build/pom.xml clean package || error_exit "Maven build failed"

# Run the tests
echo "Running tests..."
mvn -B -f org.eclipse.lyo.testsuite.server/pom.xml clean test -DskipTests=false -Dtest=org.eclipse.lyo.testsuite.DynamicSuiteBuilder -Dprops=config/refimpl2020/cm/suiteconfig.properties -fae || error_exit "Tests failed"

# Return to the original directory
popd || error_exit "Failed to return to original directory"

echo "Script completed successfully"
