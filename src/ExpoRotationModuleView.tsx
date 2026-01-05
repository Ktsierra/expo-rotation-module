import { requireNativeView } from 'expo';
import * as React from 'react';

import { ExpoRotationModuleViewProps } from './ExpoRotationModule.types';

const NativeView: React.ComponentType<ExpoRotationModuleViewProps> =
  requireNativeView('ExpoRotationModule');

export default function ExpoRotationModuleView(props: ExpoRotationModuleViewProps) {
  return <NativeView {...props} />;
}
