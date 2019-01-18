import {OAuth2AuthenticateOptions} from "./definitions";
import {WebUtils} from "./web-utils";

const googleOptions: OAuth2AuthenticateOptions = {
    appId: "appId",
    authorizationBaseUrl: "https://accounts.google.com/o/oauth2/auth",
    accessTokenEndpoint: "https://www.googleapis.com/oauth2/v4/token",
    scope: "email profile",
    resourceUrl: "https://www.googleapis.com/userinfo/v2/me",
    web: {
        appId: "webAppId",
        redirectUrl: "https://github.com/moberwasserlechner"
    },
    android: {
        responseType: "code",
    },
    ios: {
        responseType: "code",
        // because I used the android bundle id in a not paid apple account I need to choose another one
    }
};

describe('Options processing', () => {

    it('should build a nested appId', () => {
        const appId = WebUtils.getAppId(googleOptions);
        expect(appId).toEqual("webAppId");
    });
});

describe("Url param extraction", () => {

    it('should return null on null url', () => {
        const paramObj = WebUtils.getUrlParams(null);
        expect(paramObj).toBeUndefined();
    });

    it('should return null on empty url', () => {
        const paramObj = WebUtils.getUrlParams("");
        expect(paramObj).toBeUndefined();
    });

    it('should return null on url with spaces', () => {
        const paramObj = WebUtils.getUrlParams("    ");
        expect(paramObj).toBeUndefined();
    });

    it('should return null if no params in url', () => {
        const paramObj = WebUtils.getUrlParams("https://app.example.com/");
        expect(paramObj).toBeUndefined();
    });

    it('should return null if no params in url', () => {
        const paramObj = WebUtils.getUrlParams("https://app.example.com?");
        expect(paramObj).toBeUndefined();
    });

    it('should remove invalid combinations one param', () => {
        const paramObj = WebUtils.getUrlParams("https://app.example.com?=test");
        expect(paramObj).toBeUndefined();
    });

    it('should remove invalid combinations multiple param', () => {
        const paramObj = WebUtils.getUrlParams("https://app.example.com?=test&key1=param1");
        expect(paramObj).toEqual({key1: "param1"});
    });

    it('should extract work with a single param', () => {
        const paramObj = WebUtils.getUrlParams("https://app.example.com?access_token=testtoken");
        expect(paramObj["access_token"]).toStrictEqual("testtoken");
    });

    it('should extract a uuid state param', () => {
        const state = WebUtils.randomString();
        const paramObj = WebUtils.getUrlParams("https://app.example.com?state=" + state + "&access_token=testtoken");
        expect(paramObj["state"]).toStrictEqual(state);
    });

});

describe("Random string gen", () => {
    it('should generate a 10 letter string', () => {
        const expected = 10;
        const random = WebUtils.randomString(expected);
        expect(random.length).toStrictEqual(expected);
    });

    it('should generate a 43 letter string as this is the minimum for PKCE', () => {
        const expected = 43;
        const random = WebUtils.randomString(expected);
        expect(random.length).toStrictEqual(expected);
    });
});

