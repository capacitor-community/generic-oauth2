import { registerPlugin } from '@capacitor/core';

import type { GenericOAuth2Plugin } from './definitions';

const GenericOAuth2 = registerPlugin<GenericOAuth2Plugin>('GenericOAuth2', {
  web: () => import('./web').then(m => new m.GenericOAuth2Web()),
});

export * from './definitions';
export { GenericOAuth2 };
