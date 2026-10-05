import { cpSync, existsSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const destination = process.argv[2];
const version = process.argv[3] || '0.86.3';
if (!destination || existsSync(destination) || !/^0\.86\.\d+$/.test(version)) {
  throw new Error('Usage: node scripts/prepare-rn-compat.mjs /absolute/new-directory 0.86.3');
}
const excluded = new Set([
  '.git', 'node_modules', 'Pods', 'build', '.gradle', '.cxx', 'lib', 'coverage',
  'config.local.json', 'GoogleSignIn.local.xcconfig', 'google-services.json', 'GoogleService-Info.plist',
]);
cpSync(root, destination, {
  recursive: true,
  filter: (source) => !excluded.has(path.basename(source)),
});
for (const filename of ['package.json', 'example/package.json']) {
  const target = path.join(destination, filename);
  const manifest = JSON.parse(readFileSync(target, 'utf8'));
  for (const section of ['dependencies', 'devDependencies']) {
    for (const name of Object.keys(manifest[section] || {})) {
      if (name.startsWith('@react-native/') && name !== '@react-native/eslint-config') {
        manifest[section][name] = version;
      }
      if (name === 'react-native') manifest[section][name] = version;
      if (name === 'react') manifest[section][name] = '19.2.3';
    }
  }
  if (filename === 'package.json') {
    manifest.devDependencies['@react-native/jest-preset'] = version;
    manifest.jest.preset = '@react-native/jest-preset';
  }
  writeFileSync(target, `${JSON.stringify(manifest, null, 2)}\n`);
}
cpSync(path.join(destination, 'example/src/config.example.json'), path.join(destination, 'example/src/config.local.json'));
console.log(destination);
