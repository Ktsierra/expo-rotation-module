import { registerWebModule, NativeModule } from 'expo';

import { ExpoRotationModuleEvents } from './ExpoRotationModule.types';
import type { RotationState } from './ExpoRotationModule';

class ExpoRotationModule extends NativeModule<ExpoRotationModuleEvents> {
  async canWrite(): Promise<boolean> {
    return true;
  }
  requestWritePermission(): void {
    // no-op on web
  }
  async getRotationState(): Promise<RotationState> {
    return 'AUTOROTATE';
  }
  async setRotationState(state: RotationState): Promise<void> {
    // no-op on web
  }
}

export default registerWebModule(ExpoRotationModule, 'ExpoRotationModule');
