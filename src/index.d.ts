export type RotationState = 'AUTOROTATE' | 'PORTRAIT' | 'LANDSCAPE';

export function canWrite(): Promise<boolean>;
export function requestWritePermission(): void;
export function getRotationState(): Promise<RotationState>;
export function setRotationState(state: RotationState): Promise<void>;
export function getPackageName(): Promise<string>;

declare const _default: {
  canWrite: typeof canWrite;
  requestWritePermission: typeof requestWritePermission;
  getRotationState: typeof getRotationState;
  setRotationState: typeof setRotationState;
  getPackageName: typeof getPackageName;
};

export default _default;
