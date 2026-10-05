import { copyFileSync, existsSync } from 'node:fs';
const destination = new URL('../example/src/config.local.json', import.meta.url);
if (!existsSync(destination)) {
  copyFileSync(new URL('../example/src/config.example.json', import.meta.url), destination);
}
