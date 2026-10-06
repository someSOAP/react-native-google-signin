#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Prefer a booted iOS simulator; IOS_TEST_DESTINATION can override discovery.
destination="$(node scripts/ios-test-destination.mjs)"
xcodebuild test -project tests/ios/AuthTests.xcodeproj -scheme AuthTests \
  -destination "$destination" \
  -derivedDataPath tests/ios/build -enableCodeCoverage YES CODE_SIGNING_ALLOWED=NO
