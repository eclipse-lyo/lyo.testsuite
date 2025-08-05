#!/bin/bash

# Simple script to refactor SimpleSelector pattern using sed
# Usage: ./simple-refactor.sh

set -e

echo "Starting SimpleSelector pattern refactoring..."

# Function to process a single file
process_file() {
    local file="$1"
    local backup_file="${file}.backup"

    echo "Processing: $file"

    # Create backup
    cp "$file" "$backup_file"

    # Read the file into memory and process it
    python3 << 'EOF' - "$file" "$backup_file"
import sys
import re

def process_file(input_file, backup_file):
    with open(backup_file, 'r') as f:
        lines = f.readlines()

    result_lines = []
    i = 0
    changes_made = False

    while i < len(lines):
        line = lines[i]

        # Check for SimpleSelector pattern
        selector_match = re.search(r'(\s*)Selector\s+(\w+)\s*=\s*new\s+SimpleSelector\s*\(\s*null\s*,\s*(\w+)\s*,\s*\(RDFNode\)\s*null\s*\)\s*;', line)

        if selector_match and i + 1 < len(lines):
            indent = selector_match.group(1)
            selector_var = selector_match.group(2)
            property_var = selector_match.group(3)
            next_line = lines[i + 1]

            # Check if next line uses this selector
            stmt_match = re.search(rf'(\s*)StmtIterator\s+(\w+)\s*=\s*(\w+)\.listStatements\s*\(\s*{selector_var}\s*\)\s*;', next_line)

            if stmt_match:
                next_indent = stmt_match.group(1)
                stmt_var = stmt_match.group(2)
                model_var = stmt_match.group(3)

                # Create the replacement line
                replacement = f"{next_indent}StmtIterator {stmt_var} = {model_var}.listStatements(null, {property_var}, (RDFNode) null);\n"
                result_lines.append(replacement)
                changes_made = True
                i += 2  # Skip both lines
                continue

        result_lines.append(line)
        i += 1

    if changes_made:
        with open(input_file, 'w') as f:
            f.writelines(result_lines)
        return True
    return False

if __name__ == "__main__":
    input_file = sys.argv[1]
    backup_file = sys.argv[2]
    if process_file(input_file, backup_file):
        print(f"  ✓ Refactored {input_file}")
    else:
        print(f"  - No changes needed in {input_file}")
        import os
        os.remove(backup_file)

EOF
}

# Find and process all Java files containing the pattern
find . -name "*.java" -type f -exec grep -l "new SimpleSelector" {} \; | while read -r file; do
    process_file "$file"
done

echo "Refactoring complete!"
echo "Backup files (.backup) have been created for modified files."
echo "Review the changes and remove backup files when satisfied."
