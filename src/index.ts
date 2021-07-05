import { registerPlugin } from '@capacitor/core';

import type { OAuth2ClientPlugin } from './definitions';

const OAuth2Client = registerPlugin<OAuth2ClientPlugin>('OAuth2Client', {
    web: () => import('./web').then(m => new m.OAuth2ClientPluginWeb()),
    // electron: () => ("./electron").then(m => new m.OAuth2ClientPluginElectron())
});

export * from './definitions';
export { OAuth2Client };
