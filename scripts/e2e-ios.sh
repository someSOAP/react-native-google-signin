#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../example/ios"
xcodebuild test -workspace ReactNativeGoogleSigninExample.xcworkspace -scheme AuthUI \
  -configuration Debug -destination "${IOS_TEST_DESTINATION:-platform=iOS Simulator,name=iPhone 16e}" \
  -derivedDataPath build CODE_SIGNING_ALLOWED=NO
