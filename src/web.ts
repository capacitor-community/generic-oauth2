import { WebPlugin } from '@capacitor/core';
import {OAuth2AuthenticateOptions, OAuth2AuthenticateResult, OAuth2ClientPlugin} from "./definitions";

export class OAuth2ClientPluginWeb extends WebPlugin implements OAuth2ClientPlugin {
    constructor() {
        super({
            name: 'OAuth2Client',
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

const OAuth2Client = new OAuth2ClientPluginWeb();

export { OAuth2Client };
