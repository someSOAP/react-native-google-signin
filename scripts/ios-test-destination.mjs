import { execFileSync } from 'node:child_process';

if (process.env.IOS_TEST_DESTINATION) {
  console.log(process.env.IOS_TEST_DESTINATION);
} else {
  const { devices } = JSON.parse(
    execFileSync(
      'xcrun',
      ['simctl', 'list', 'devices', 'available', '--json'],
      {
        encoding: 'utf8',
      }
    )
  );
  const available = Object.entries(devices)
    .filter(([runtime]) => runtime.includes('.iOS-'))
    .flatMap(([, entries]) => entries)
    .filter((device) => device.isAvailable && device.udid);
  const selected =
    available.find((device) => device.state === 'Booted') ?? available[0];
  if (!selected) {
    throw new Error(
      'No available iOS simulator. Install a runtime or set IOS_TEST_DESTINATION.'
    );
  }
  console.log(`platform=iOS Simulator,id=${selected.udid}`);
}
