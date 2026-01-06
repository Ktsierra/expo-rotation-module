import { NativeModule, requireNativeModule } from 'expo';
import { Platform } from 'react-native';

import { ExpoRotationModuleEvents } from './ExpoRotationModule.types';

export type RotationState = 'AUTOROTATE' | 'PORTRAIT' | 'LANDSCAPE';

declare class ExpoRotationModule extends NativeModule<ExpoRotationModuleEvents> {
  canWrite(): Promise<boolean>;
  requestWritePermission(): void;
  getRotationState(): Promise<RotationState>;
  setRotationState(state: RotationState): Promise<void>;
  getPackageName(): Promise<string>;
}

// This call loads the native module object from the JSI on native platforms.
const isAndroid = Platform.OS === 'android';
const nativeModule = isAndroid ? requireNativeModule<ExpoRotationModule>('ExpoRotationModule') : null;

export async function canWrite(): Promise<boolean> {
  if (!isAndroid || !nativeModule || !nativeModule.canWrite) return false;
  try {
    return await nativeModule.canWrite();
  } catch (e: any) {
    throw normalizeError(e);
  }
}

export function requestWritePermission(): void {
  if (!isAndroid || !nativeModule || !nativeModule.requestWritePermission) return;
  try {
    nativeModule.requestWritePermission();
  } catch (e: any) {
    throw normalizeError(e);
  }
}

export async function getPackageName(): Promise<string> {
  if (!isAndroid || !nativeModule || !nativeModule.getPackageName) return '';
  try {
    return await nativeModule.getPackageName();
  } catch (e: any) {
    throw normalizeError(e);
  }
}

export async function getRotationState(): Promise<RotationState> {
  if (!isAndroid || !nativeModule || !nativeModule.getRotationState) return 'AUTOROTATE';
  try {
    return await nativeModule.getRotationState();
  } catch (e: any) {
    throw normalizeError(e);
  }
}

function normalizeError(e: any): Error & { code?: string } {
  const err = new Error(e?.message || String(e));
  // Try to extract code from messages like "E_PERMISSION: ..."
  const m = (e?.message || '').match(/^([A-Z_]+):\s*(.*)$/);
  if (m) {
    (err as any).code = m[1];
    err.message = m[2] || err.message;
  }
  return err as Error & { code?: string };
}

export async function setRotationState(state: RotationState): Promise<void> {
  if (!isAndroid || !nativeModule || !nativeModule.setRotationState) {
    const err = new Error('Rotation native module not available') as Error & { code?: string };
    err.code = 'E_NO_MODULE';
    throw err;
  }
  try {
    return await nativeModule.setRotationState(state);
  } catch (e: any) {
    throw normalizeError(e);
  }
}

export default {
  canWrite,
  requestWritePermission,
  getRotationState,
  setRotationState,
  getPackageName,
};
