import { registerPlugin } from '@capacitor/core';
const GenericOAuth2 = registerPlugin('GenericOAuth2', {
    web: () => import('./web').then(m => new m.GenericOAuth2Web()),
});
export * from './definitions';
export { GenericOAuth2 };
//# sourceMappingURL=index.js.map