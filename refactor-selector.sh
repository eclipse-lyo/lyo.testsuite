#!/bin/bash

# Script to refactor SimpleSelector pattern
# Usage: ./refactor-selector.sh

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting SimpleSelector pattern refactoring...${NC}"

# Find all Java files with the pattern
echo -e "${YELLOW}Finding files with SimpleSelector pattern...${NC}"

# Function to process a single file
process_file() {
    local file="$1"
    local backup_file="${file}.backup"

    echo "Processing: $file"

    # Create backup
    cp "$file" "$backup_file"

    # Use awk to process the file
    awk '
    BEGIN {
        selector_line = ""
        property_var = ""
        selector_var = ""
    }

    # Look for SimpleSelector declaration
    /Selector.*=.*new SimpleSelector\(null,.*\(RDFNode\) null\);/ {
        # Extract variable name and property
        if (match($0, /Selector ([a-zA-Z_][a-zA-Z0-9_]*) = new SimpleSelector\(null, ([a-zA-Z_][a-zA-Z0-9_]*), \(RDFNode\) null\);/)) {
            # Use split to get the parts
            split($0, parts, " ")
            for (i in parts) {
                if (parts[i] ~ /^[a-zA-Z_][a-zA-Z0-9_]*$/ && parts[i-1] == "Selector") {
                    selector_var = parts[i]
                    break
                }
            }
            # Extract property variable from the SimpleSelector constructor
            if (match($0, /new SimpleSelector\(null, ([a-zA-Z_][a-zA-Z0-9_]*),/)) {
                prop_match = substr($0, RSTART, RLENGTH)
                if (match(prop_match, /([a-zA-Z_][a-zA-Z0-9_]*)/)) {
                    property_var = substr(prop_match, RSTART, RLENGTH)
                }
            }
            if (selector_var != "" && property_var != "") {
                next  # Skip this line
            }
        }
    }

    # Look for the corresponding listStatements call on the next line
    selector_var != "" && /StmtIterator.*=.*\.listStatements\([^)]*\);/ {
        if (index($0, selector_var) > 0) {
            # Extract variable names
            split($0, parts, " ")
            for (i in parts) {
                if (parts[i] ~ /^[a-zA-Z_][a-zA-Z0-9_]*$/ && parts[i-1] == "StmtIterator") {
                    output_var = parts[i]
                    break
                }
            }
            # Extract model variable
            if (match($0, /([a-zA-Z_][a-zA-Z0-9_]*)\.listStatements/)) {
                model_var = substr($0, RSTART, RLENGTH-13) # Remove ".listStatements"
            }

            if (output_var != "" && model_var != "") {
                # Generate the replacement line with proper indentation
                if (match($0, /^[ \t]*/)) {
                    indent = substr($0, RSTART, RLENGTH)
                }
                print indent "StmtIterator " output_var " = " model_var ".listStatements(null, " property_var ", (RDFNode) null);"

                # Reset state
                selector_var = ""
                property_var = ""
                output_var = ""
                model_var = ""
                next
            }
        }
    }

    # Print all other lines
    { print }
    ' "$backup_file" > "$file"

    # Check if any changes were made
    if ! cmp -s "$file" "$backup_file"; then
        echo -e "${GREEN}  ✓ Refactored $file${NC}"
        # Show the diff
        echo -e "${YELLOW}Changes made:${NC}"
        diff -u "$backup_file" "$file" || true
        echo ""
    else
        echo -e "${YELLOW}  - No changes needed in $file${NC}"
        rm "$backup_file"
    fi
}

# Find and process all Java files containing the pattern
find . -name "*.java" -type f -exec grep -l "new SimpleSelector" {} \; | while read -r file; do
    process_file "$file"
done

echo -e "${GREEN}Refactoring complete!${NC}"
echo -e "${YELLOW}Backup files (.backup) have been created for modified files.${NC}"
echo -e "${YELLOW}Review the changes and remove backup files when satisfied.${NC}"
