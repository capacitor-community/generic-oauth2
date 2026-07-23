import { WebPlugin } from '@capacitor/core';

import type { GenericOAuth2Plugin, OAuth2AuthenticateResult } from './definitions';

export class GenericOAuth2Web extends WebPlugin implements GenericOAuth2Plugin {
  async authenticate(): Promise<OAuth2AuthenticateResult> {
    throw this.unavailable('not available for web');
  }
}
