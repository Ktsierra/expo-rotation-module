import { registerWebModule, NativeModule } from 'expo';

import { ExpoRotationModuleEvents } from './ExpoRotationModule.types';

class ExpoRotationModule extends NativeModule<ExpoRotationModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
}

export default registerWebModule(ExpoRotationModule, 'ExpoRotationModule');
