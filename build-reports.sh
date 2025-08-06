#!/bin/bash

# Function to display usage
usage() {
    echo "Usage: $0 [-f] [all|am|cm|qm|rm]"
    echo "  -f: force clean build"
    echo "  all|am|cm|qm|rm: specific module to build, or all (default)"
    exit 1
}

# Default values
FORCE_CLEAN=false
MODULE="all"

# Parse command-line options
while getopts ":f" opt; do
    case ${opt} in
        f )
            FORCE_CLEAN=true
            ;;
        \? )
            usage
            ;;
    esac
done
shift $((OPTIND -1))

# Get the module argument
if [ -n "$1" ]; then
    MODULE=$1
fi

# Define modules based on input
if [ "$MODULE" = "all" ]; then
    modules="am cm qm rm"
else
    modules=$MODULE
fi

# Get the directory of the script
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
base_dir="$script_dir/org.eclipse.lyo.testsuite.server/assessment"

# Set ANT_OPTS to include the system property
export ANT_OPTS="-Djdk.xml.enableExtensionFunctions=true"

# Track overall build status
overall_success=true

# Loop through the specified modules
for module in $modules; do
    echo "Processing $module module..."
    module_dir="$base_dir/$module"

    if [ ! -d "$module_dir" ]; then
        echo "Module directory $module_dir not found. Skipping."
        continue
    fi

    # Find the ant build file, preferring files with 'rio' in the name
    ant_file=$(find "$module_dir" -maxdepth 1 -type f -name "*assessment-test.xml" | grep 'rio' | head -n 1)
    if [ -z "$ant_file" ]; then
        ant_file=$(find "$module_dir" -maxdepth 1 -type f -name "*assessment-test.xml" | head -n 1)
    fi

    if [ -z "$ant_file" ]; then
        echo "No assessment-test.xml file found in $module_dir. Skipping."
        continue
    fi

    echo "Using Ant file: $ant_file"

    # Check if JUnit test results exist
    junit_dir="$module_dir/junit"
    if [ ! -d "$junit_dir" ] || [ -z "$(find "$junit_dir" -name "TEST-*.xml" -type f 2>/dev/null)" ]; then
        echo "⚠️  No JUnit test results found for $module module."
        echo "   JUnit test XML files are required to generate assessment reports."
        echo ""
        echo "   To generate test results, run one of these commands first:"
        case $module in
            am)
                echo "   # For Asset Management tests:"
                echo "   mvn -B -f org.eclipse.lyo.testsuite.server/pom.xml clean test -DskipTests=false \\"
                echo "       -Dtest=org.eclipse.lyo.testsuite.DynamicSuiteBuilder \\"
                echo "       -Dprops=config/refimpl2020/am/suiteconfig.properties -fae"
                ;;
            cm)
                echo "   # For Change Management tests:"
                echo "   mvn -B -f org.eclipse.lyo.testsuite.server/pom.xml clean test -DskipTests=false \\"
                echo "       -Dtest=org.eclipse.lyo.testsuite.DynamicSuiteBuilder \\"
                echo "       -Dprops=config/refimpl2020/cm/suiteconfig.properties -fae"
                ;;
            qm)
                echo "   # For Quality Management tests:"
                echo "   mvn -B -f org.eclipse.lyo.testsuite.server/pom.xml clean test -DskipTests=false \\"
                echo "       -Dtest=org.eclipse.lyo.testsuite.DynamicSuiteBuilder \\"
                echo "       -Dprops=config/refimpl2020/qm/suiteconfig.properties -fae"
                ;;
            rm)
                echo "   # For Requirements Management tests:"
                echo "   mvn -B -f org.eclipse.lyo.testsuite.server/pom.xml clean test -DskipTests=false \\"
                echo "       -Dtest=org.eclipse.lyo.testsuite.DynamicSuiteBuilder \\"
                echo "       -Dprops=config/refimpl2020/rm/suiteconfig.properties -fae"
                ;;
            *)
                echo "   # Generic test command (adjust config path as needed):"
                echo "   mvn -B -f org.eclipse.lyo.testsuite.server/pom.xml clean test -DskipTests=false \\"
                echo "       -Dtest=org.eclipse.lyo.testsuite.DynamicSuiteBuilder \\"
                echo "       -Dprops=config/refimpl2020/$module/suiteconfig.properties -fae"
                ;;
        esac
        echo ""
        echo "   Or use the legacy Ant-based approach:"
        echo "   cd $module_dir && ant -f $(basename "$ant_file") provider-test"
        echo ""
        echo "❌ Skipping $module module (no test results)."
        overall_success=false
        echo
        continue
    fi

    # Run ant clean if -f is specified
    if [ "$FORCE_CLEAN" = true ]; then
        echo "Forcing clean build..."
        ant -f "$ant_file" clean
    fi

    # Run the junitreport target
    echo "Running junitreport target..."
    ant -f "$ant_file" junitreport
    exit_code=$?

    if [ $exit_code -ne 0 ]; then
        echo "🔴 Ant build failed for $module module."
        overall_success=false
    else
        echo "✅ $module module processed successfully."
    fi
    echo
done

echo "All specified modules processed."

if [ "$overall_success" = false ]; then
    echo "One or more modules failed to build."
    exit 1
fi
