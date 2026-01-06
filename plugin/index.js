let withAndroidManifest;
try {
  // Prefer the project's installed config-plugins so resolution works from the app
  withAndroidManifest = require(process.cwd() + '/node_modules/@expo/config-plugins').withAndroidManifest;
} catch (e) {
  // Fallback to resolving from this package (useful for local dev)
  withAndroidManifest = require('@expo/config-plugins').withAndroidManifest;
}

function addWriteSettingsPermission(androidManifest) {
  const usesPermissions = androidManifest.manifest['uses-permission'] || [];
  const exists = usesPermissions.some(
    (p) => p.$['android:name'] === 'android.permission.WRITE_SETTINGS'
  );
  if (!exists) {
    usesPermissions.push({
      $: {
        'android:name': 'android.permission.WRITE_SETTINGS',
      },
    });
    androidManifest.manifest['uses-permission'] = usesPermissions;
  }
  return androidManifest;
}

module.exports = function withWriteSettingsPermission(config) {
  return withAndroidManifest(config, (config) => {
    config.modResults = addWriteSettingsPermission(config.modResults);
    return config;
  });
};

module.exports.plugin = module.exports;
