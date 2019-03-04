import {OAuth2AuthenticateOptions} from "./definitions";
import {WebUtils} from "./web-utils";

const googleOptions: OAuth2AuthenticateOptions = {
    appId: "appId",
    authorizationBaseUrl: "https://accounts.google.com/o/oauth2/auth",
    accessTokenEndpoint: "https://www.googleapis.com/oauth2/v4/token",
    scope: "email profile",
    resourceUrl: "https://www.googleapis.com/userinfo/v2/me",
    pkceDisabled: false,
    web: {
        redirectUrl: "https://oauth2.byteowls.com/authorize",
        appId: "webAppId",
        pkceDisabled: true
    },
    android: {
        responseType: "code",
    },
    ios: {
        responseType: "code",
    }
};

const oneDriveOptions: OAuth2AuthenticateOptions = {
    appId: "appId",
    authorizationBaseUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
    accessTokenEndpoint: "https://login.microsoftonline.com/common/oauth2/v2.0/token",
    scope: "files.readwrite offline_access",
    responseType: "code",
    additionalParameters: {
      "willbeoverwritten": "foobar"
    },
    web: {
        redirectUrl: "https://oauth2.byteowls.com/authorize",
        pkceDisabled: true,
        additionalParameters: {
            "resource": "resource_id",
            "emptyParam": null,
            " ": "test",
            "nonce": WebUtils.randomString(10)
        }
    },
    android: {
        customScheme: "com.byteowls.oauth2://authorize"
    },
    ios: {
        customScheme: "com.byteowls.oauth2://authorize"
    }
};

describe('base options processing', () => {

    it('should build a nested appId', () => {
        const appId = WebUtils.getAppId(googleOptions);
        expect(appId).toEqual("webAppId");
    });

    it('should build a overwritable string value', () => {
        const appId = WebUtils.getOverwritableValue<string>(googleOptions, "appId");
        expect(appId).toEqual("webAppId");
    });

    it('should build a overwritable boolean value', () => {
        const pkceDisabled = WebUtils.getOverwritableValue<boolean>(googleOptions, "pkceDisabled");
        expect(pkceDisabled).toBeTruthy();
    });

    it('should build a overwritable additional parameters map', () => {
        const additionalParameters = WebUtils.getOverwritableValue<{[key: string]: string}>(oneDriveOptions, "additionalParameters");
        expect(additionalParameters).not.toBeUndefined();
        expect(additionalParameters["resource"]).toEqual("resource_id");
    });

    it('must not contain overwritten additional parameters', () => {
        const additionalParameters = WebUtils.getOverwritableValue<{[key: string]: string}>(oneDriveOptions, "additionalParameters");
        expect(additionalParameters["willbeoverwritten"]).toBeUndefined();
    });
});

describe('web options', () => {
    const webOptions = WebUtils.buildWebOptions(oneDriveOptions);
    // console.log(webOptions);

    it('should build web options', () => {
        expect(webOptions).not.toBeNull();
    });

    it('should not have a code verifier', () => {
        expect(webOptions.pkceCodeVerifier).toBeUndefined();
    });

    it('must not contain empty additional parameter', () => {
        expect(webOptions.additionalParameters[" "]).toBeUndefined();
        expect(webOptions.additionalParameters["emptyParam"]).toBeUndefined();
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

describe("Authorization url building", () => {
    const webOptions = WebUtils.buildWebOptions(oneDriveOptions);
    const authorizationUrl = WebUtils.getAuthorizationUrl(webOptions);

    it('should contain a nonce param', () => {
        expect(authorizationUrl).toContain("nonce");
    });

    it('should contain a resource param', () => {
        expect(authorizationUrl).toContain("nonce");
    });

    //console.log("Authorization url:", authorizationUrl);

});

