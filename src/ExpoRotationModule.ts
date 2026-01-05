import { NativeModule, requireNativeModule } from 'expo';

import { ExpoRotationModuleEvents } from './ExpoRotationModule.types';

declare class ExpoRotationModule extends NativeModule<ExpoRotationModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoRotationModule>('ExpoRotationModule');
