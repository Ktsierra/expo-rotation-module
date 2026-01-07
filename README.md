# expo-rotation-module

A small Expo native module that lets an Android app read and control the system rotation settings (ACCELEROMETER_ROTATION and USER_ROTATION) and request the `WRITE_SETTINGS` permission.

This module exposes a tiny JS/TS API and includes Android and iOS native source. The runtime behavior is Android-only; iOS/web are no-ops.

---

## Usage

### Install

- From your app project (local development):
  - `pnpm add ../path/to/expo-rotation-module` (or use a published package: `pnpm add expo-rotation-module`)

- IMPORTANT: add the config plugin to your app config so the permission is injected during `prebuild`.

  In `app.json` or `app.config.js` add the package name to `expo.plugins`:

  ```json
  {
    "expo": {
      "plugins": [
        "expo-rotation-module"
      ]
    }
  }
  ```

  - The plugin is implemented in `plugin/index.js` and will insert the `WRITE_SETTINGS` permission into your `AndroidManifest.xml` during `expo prebuild`.

- Apply native changes and build the app:
  - `npx expo prebuild` (or `expo run:android` which runs prebuild automatically)
  - Rebuild/install the app on device/emulator (use a custom dev client or a standalone build for WRITE_SETTINGS tests — Expo Go is not suitable).

### API

Import the module in JS/TS:

```ts
import Rotation, {
  canWrite,
  requestWritePermission,
  getRotationState,
  setRotationState,
  getPackageName,
} from 'expo-rotation-module';
```

- `canWrite(): Promise<boolean>` — returns true if the app has `WRITE_SETTINGS` granted (Android M+). Returns `true` on older OS versions.
- `requestWritePermission(): void` — opens the Android Settings screen where the user can grant the permission for your app.
- `getRotationState(): Promise<'AUTOROTATE'|'PORTRAIT'|'LANDSCAPE'>` — reads the current global rotation state. The module reports the coarse axis (PORTRAIT or LANDSCAPE); within those axes the system sensor still allows regular or inverted orientations when auto-rotate is off.
- `setRotationState(state): Promise<void>` — sets the rotation state. Rejects with an Error that may contain a `code` property (e.g. `E_PERMISSION`).
- `getPackageName(): Promise<string>` — returns the package name the native module is using (useful for diagnostics).

All functions are Android-only. On non-Android platforms the functions are no-ops or return safe defaults.

### Example

```ts
import Rotation from 'expo-rotation-module';

async function ensureAndSet() {
  const hasPermission = await Rotation.canWrite();
  if (!hasPermission) {
    // Opens Settings where the user can grant WRITE_SETTINGS for your app
    Rotation.requestWritePermission();
    return;
  }

  await Rotation.setRotationState('PORTRAIT');
}
```

### Troubleshooting

- WRITE_SETTINGS switch is greyed out in the Settings screen:
  - Ensure your app has the `WRITE_SETTINGS` permission declared in the manifest. The config plugin adds this when you run `expo prebuild`.
  - Do not test this inside Expo Go — the Settings screen will target the Expo Go package. Build a custom dev client or standalone build.
  - Confirm the package name by calling `Rotation.getPackageName()` and check device logs for the `Opening WRITE_SETTINGS for package: <pkg>` message.

- ESLint/type errors in consuming projects:
  - Ensure your app can resolve the package. For local linking with pnpm workspaces, `pnpm install` in the app and a restart of the editor/TS server is sometimes required.

---

## Development

This section explains how to develop, test and publish this module.

### Repo layout (important files)

- `src/` — TypeScript JS wrapper and types.
- `android/` — Android native sources (Kotlin) and Gradle files.
- `ios/` — iOS native sources (Swift) and podspec.
- `plugin/` — config plugin that injects the `WRITE_SETTINGS` permission.
- `app.plugin.js` — plugin entry point for Expo to discover the config plugin.
- `index.js` — package entry that re-exports the JS wrapper.
- `package.json` — package metadata, scripts and publish config.

### Local development

1. Install dependencies in the module repo:
   - `pnpm install`

2. Make changes to `src/` or native code.

3. Build/prepare artifacts (some scripts expect `tsc`/expo-module-scripts):
   - `pnpm run prepare` (runs `expo-module prepare`) or `pnpm run build` (if you prefer).

4. Test in an app:
   - Create or use an example app. From the app root:
     - Add the local package: `pnpm add ../path/to/expo-rotation-module` (or `pnpm add file:...`)
     - Ensure `app.json` includes the plugin (see Usage).
     - Run `npx expo prebuild` and verify `android/app/src/main/AndroidManifest.xml` contains `<uses-permission android:name="android.permission.WRITE_SETTINGS" />`.
     - Build and run the app on device/emulator (`expo run:android` or a dev client build).

5. Faster iteration (optional):
   - Instead of building the package, map Metro to the module `src` in the app `metro.config.js` during development:
     ```js
     // example: app/metro.config.js
     const path = require('path');
     module.exports = {
       resolver: {
         extraNodeModules: {
           'expo-rotation-module': path.resolve(__dirname, '../path/to/expo-rotation-module/src'),
         },
       },
       watchFolders: [path.resolve(__dirname, '..')],
     };
     ```
   - This lets the app load the TypeScript source directly without rebuilding the package on every change.

### Publishing

We publish the source package (native sources included). Before publishing, ensure:

- `package.json` fields are correct (`name`, `version`, `main`, `types`, `files`).
- `devDependencies` contains `typescript` so `expo-module prepare` runs in CI.
- The plugin and native sources are included in the published package (use the `files` array in `package.json` to control this).

Manual publish (local):

1. Prepare the package locally:
   - `pnpm install`
   - `pnpm run prepare`

2. Create a tarball to test what will be published:
   - `npm pack`
   - Install the tarball into a test app: `pnpm add ../expo-rotation-module-<version>.tgz`
   - Run `npx expo prebuild` in the test app to confirm the plugin and native sources are applied.

3. Publish:
   - If your environment has an npm automation token configured in CI, run `npm publish --access public` from the package root.
   - If `prepublishOnly` scripts fail in your environment, you can publish with scripts ignored after preparing locally:
     - `npm publish --access public --ignore-scripts`

### Continuous Integration / GitHub Actions

- Recommended CI flow:
  - `pnpm install --frozen-lockfile`
  - `pnpm run prepare` (ensure `typescript` is installed in `devDependencies` so `tsc` is available)
  - Run tests/lint (optional)
  - Bump version, push tags
  - Authenticate to npm (use an automation token or GitHub OIDC if your npm org supports trusted publishers)
  - `npm publish --access public` or `pnpm publish --access public`

- Example workflow (summary):
  - Checkout, setup Node + pnpm, `pnpm install`, run `pnpm run prepare`, bump version, publish.

---

If you find anything missing or unclear (examples, API signatures, or CI details) open an issue or submit a PR. Contributions welcome.
