# expo-rotation-module

An Expo native module that controls the Android system rotation settings (ACCELEROMETER_ROTATION and USER_ROTATION).

## Overview
This package exposes a small API to check and request the WRITE_SETTINGS permission and to read/set the global rotation mode. This module is Android-only; iOS and web are no-ops.

## Install (local development with pnpm)

From the module repo:

- `pnpm install`
- (optional) `pnpm build` — runs the expo-module build scripts

From your Expo app project:

- Add the module locally: `pnpm add ../expo-rotation-module` (or `pnpm add file:../expo-rotation-module`)
- IMPORTANT: Add the plugin to your app config so the permission is injected during prebuild:

  In `app.json` or `app.config.js` add the following to your `expo.plugins` array:

  ```json
  "plugins": [
    "expo-rotation-module"
  ]
  ```

- Run `npx expo prebuild` to apply the config plugin and update `AndroidManifest.xml`.
- Rebuild the Android app (use EAS dev client or Android Studio):
  - Recommended: `pnpm add expo-dev-client` then `eas build --profile development --platform android` and run with `expo start --dev-client`.

## API

Import functions:

```ts
import Rotation, { canWrite, requestWritePermission, getRotationState, setRotationState } from 'expo-rotation-module';
```

- `canWrite(): Promise<boolean>` — true if WRITE_SETTINGS is granted (Android M+), otherwise true on older OS.
- `requestWritePermission(): void` — opens Settings where the user can grant WRITE_SETTINGS.
- `getRotationState(): Promise<'AUTOROTATE'|'PORTRAIT'|'LANDSCAPE'>` — reads current rotation state.
- `setRotationState(state): Promise<void>` — sets rotation. Rejects with an Error object that may include a `.code` property.

Error codes on the Error object (if available): `E_PERMISSION`, `E_INVALID_STATE`, `E_SET_ROTATION`, `E_GET_ROTATION`, `E_NO_MODULE`.

## Example

```ts
import * as Rotation from 'expo-rotation-module';

async function example() {
  if (!(await Rotation.canWrite())) {
    Rotation.requestWritePermission();
    return;
  }

  try {
    await Rotation.setRotationState('PORTRAIT');
  } catch (e: any) {
    if (e.code === 'E_PERMISSION') {
      console.warn('Permission missing');
    }
  }
}
```

## Plugin
The package contains `plugin/index.js` that injects `android.permission.WRITE_SETTINGS` into the host app's AndroidManifest during `expo prebuild`.

## Package id and Android settings
The Android package/namespace used within module sources is `ktsierra.expo.rotationmodule`.

## Contributing & tests
If you want CI or tests added (TS checks, build smoke tests), I can add a GitHub Actions workflow that runs `pnpm install` and `pnpm build`.

---
