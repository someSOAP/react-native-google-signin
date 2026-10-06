#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../example/ios"
# Prefer a booted iOS simulator; IOS_TEST_DESTINATION can override discovery.
destination="$(node ../../scripts/ios-test-destination.mjs)"
xcodebuild test -workspace ReactNativeGoogleSigninExample.xcworkspace -scheme AuthUI \
  -configuration Debug -destination "$destination" \
  -derivedDataPath build CODE_SIGNING_ALLOWED=NO
