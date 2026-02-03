#!/bin/bash

# Path to the global version.properties
VERSION_FILE="../../version.properties"

if [ -f "$VERSION_FILE" ]; then
    VERSION_CODE=$(grep 'VERSION_CODE=' "$VERSION_FILE" | cut -d'=' -f2)
    VERSION_NAME=$(grep 'VERSION_NAME=' "$VERSION_FILE" | cut -d'=' -f2)

    echo "Updating iOS version to Name: $VERSION_NAME, Code: $VERSION_CODE"

    # Note: In a real Xcode project, we would use agvtool or /usr/libexec/PlistBuddy to update Info.plist
    # Example (commented out until .xcodeproj exists):
    # xcrun agvtool new-marketing-version $VERSION_NAME
    # xcrun agvtool new-version -all $VERSION_CODE
else
    echo "Error: version.properties not found at $VERSION_FILE"
    exit 1
fi