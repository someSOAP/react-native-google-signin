#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Pass the same simulator selected in Mobile Dev; CI can use a destination name.
xcodebuild test -project tests/ios/AuthTests.xcodeproj -scheme AuthTests \
  -destination "${IOS_TEST_DESTINATION:-platform=iOS Simulator,name=iPhone 16e}" \
  -derivedDataPath tests/ios/build -enableCodeCoverage YES CODE_SIGNING_ALLOWED=NO
