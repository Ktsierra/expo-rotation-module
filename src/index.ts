// Reexport the native module. On web, it will be resolved to ExpoRotationModule.web.ts
// and on native platforms to ExpoRotationModule.ts
export { default } from './ExpoRotationModule';
export { default as ExpoRotationModuleView } from './ExpoRotationModuleView';
export * from  './ExpoRotationModule.types';
