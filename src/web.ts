import { WebPlugin } from '@capacitor/core';
import {OAuth2AuthenticateOptions, OAuth2AuthenticateResult, OAuth2ClientPlugin} from "./definitions";

export class OAuth2ClientPluginWeb extends WebPlugin implements OAuth2ClientPlugin {
    constructor() {
        super({
            name: 'OAuth2ClientPluginWeb',
            platforms: ['web']
        });
    }

    async authenticate(options: OAuth2AuthenticateOptions): Promise<OAuth2AuthenticateResult> {
        // open window
        let windowHandle = window.open(options.authorizationBaseUrl, "OAuth2");
        // windowHandle.addEventListener(()=>{});
        // wait for redirect and resolve the

        return undefined;
    }

}

const oAuth2ClientPluginWeb = new OAuth2ClientPluginWeb();

export { oAuth2ClientPluginWeb };
