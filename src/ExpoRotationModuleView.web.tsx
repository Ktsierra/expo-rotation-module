import * as React from 'react';

import { ExpoRotationModuleViewProps } from './ExpoRotationModule.types';

export default function ExpoRotationModuleView(props: ExpoRotationModuleViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
