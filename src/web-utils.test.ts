/* eslint-disable @typescript-eslint/no-non-null-assertion */
import type { OAuth2AuthenticateOptions } from './definitions';
import { CryptoUtils, WebUtils } from './web-utils';

const mGetRandomValues = jest.fn().mockReturnValueOnce(new Uint32Array(10));
Object.defineProperty(window, 'crypto', {
  value: { getRandomValues: mGetRandomValues },
});
let store: {
  [k: string]: string;
} = {};
const sessionStorageMock = {
  getItem: jest.fn().mockImplementation((key: string) => store[key] ?? null),
  setItem: jest
    .fn()
    .mockImplementation((key: string, value: string) => (store[key] = value)),
  removeItem: jest.fn().mockImplementation((key: string) => delete store[key]),
  clear: jest.fn().mockImplementation(() => (store = {})),
};

Object.defineProperty(window, 'sessionStorage', {
  value: sessionStorageMock,
});

const googleOptions: OAuth2AuthenticateOptions = {
  appId: 'appId',
  authorizationBaseUrl: 'https://accounts.google.com/o/oauth2/auth',
  accessTokenEndpoint: 'https://www.googleapis.com/oauth2/v4/token',
  scope: 'email profile',
  resourceUrl: 'https://www.googleapis.com/userinfo/v2/me',
  pkceEnabled: false,
  web: {
    accessTokenEndpoint: '',
    redirectUrl: 'https://oauth2.byteowls.com/authorize',
    appId: 'webAppId',
    pkceEnabled: true,
  },
  android: {
    responseType: 'code',
  },
  ios: {
    responseType: 'code',
  },
};

const oneDriveOptions: OAuth2AuthenticateOptions = {
  appId: 'appId',
  authorizationBaseUrl:
    'https://login.microsoftonline.com/common/oauth2/v2.0/authorize',
  accessTokenEndpoint:
    'https://login.microsoftonline.com/common/oauth2/v2.0/token',
  scope: 'files.readwrite offline_access',
  responseType: 'code',
  additionalParameters: {
    willbeoverwritten: 'foobar',
  },
  web: {
    redirectUrl: 'https://oauth2.byteowls.com/authorize',
    pkceEnabled: false,
    additionalParameters: {
      'resource': 'resource_id',
      'emptyParam': null!,
      ' ': 'test',
      'nonce': WebUtils.randomString(10),
    },
  },
  android: {
    redirectUrl: 'com.byteowls.oauth2://authorize',
  },
  ios: {
    redirectUrl: 'com.byteowls.oauth2://authorize',
  },
};

const implicitFlowOptions: OAuth2AuthenticateOptions = {
  ...oneDriveOptions,
  pkceEnabled: true,
  web: {
    ...oneDriveOptions.web,
    pkceEnabled: true,
  },
};

const redirectUrlOptions: OAuth2AuthenticateOptions = {
  appId: 'appId',
  authorizationBaseUrl:
    'https://login.microsoftonline.com/common/oauth2/v2.0/authorize',
  responseType: 'code',
  redirectUrl: 'https://mycompany.server.com/oauth',
  scope: 'files.readwrite offline_access',
  additionalParameters: {
    willbeoverwritten: 'foobar',
  },
  web: {},
  android: {
    redirectUrl: 'com.byteowls.oauth2://authorize',
  },
  ios: {
    redirectUrl: 'com.byteowls.oauth2://authorize',
  },
};

describe('base options processing', () => {
  it('should build a nested appId', () => {
    const appId = WebUtils.getAppId(googleOptions);
    expect(appId).toEqual('webAppId');
  });

  it('should build a overwritable string value', () => {
    const appId = WebUtils.getOverwritableValue<string>(googleOptions, 'appId');
    expect(appId).toEqual('webAppId');
  });

  it('should build a overwritable boolean value', () => {
    const pkceEnabled = WebUtils.getOverwritableValue<boolean>(
      googleOptions,
      'pkceEnabled',
    );
    expect(pkceEnabled).toBeTruthy();
  });

  it('should build a overwritable additional parameters map', () => {
    const additionalParameters = WebUtils.getOverwritableValue<{
      [key: string]: string;
    }>(oneDriveOptions, 'additionalParameters');
    expect(additionalParameters).not.toBeUndefined();
    expect(additionalParameters['resource']).toEqual('resource_id');
  });

  it('must not contain overwritten additional parameters', () => {
    const additionalParameters = WebUtils.getOverwritableValue<{
      [key: string]: string;
    }>(oneDriveOptions, 'additionalParameters');
    expect(additionalParameters['willbeoverwritten']).toBeUndefined();
  });

  it('must have a base redirect url', () => {
    const redirectUrl = WebUtils.getOverwritableValue<string>(
      redirectUrlOptions,
      'redirectUrl',
    );
    expect(redirectUrl).toBeDefined();
  });

  it('must be overwritten by empty string from web section', () => {
    const accessTokenEndpoint = WebUtils.getOverwritableValue<string>(
      googleOptions,
      'accessTokenEndpoint',
    );
    expect(accessTokenEndpoint).toStrictEqual('');
  });

  it('must not be overwritten if no key exists in web section', () => {
    const accessTokenEndpoint = WebUtils.getOverwritableValue<string>(
      googleOptions,
      'scope',
    );
    expect(accessTokenEndpoint).toStrictEqual('email profile');
  });
});

describe('web options', () => {
  it('should build web options', async () => {
    WebUtils.buildWebOptions(oneDriveOptions).then(webOptions => {
      expect(webOptions).not.toBeNull();
    });
  });

  it('should not have a code verifier', async () => {
    WebUtils.buildWebOptions(oneDriveOptions).then(webOptions => {
      expect(webOptions.pkceCodeVerifier).toBeUndefined();
    });
  });

  it('must not contain empty additional parameter', async () => {
    WebUtils.buildWebOptions(oneDriveOptions).then(webOptions => {
      expect(webOptions.additionalParameters[' ']).toBeUndefined();
      expect(webOptions.additionalParameters['emptyParam']).toBeUndefined();
    });
  });

  it('must have the sendCacheControlHeader enabled by default', async () => {
    WebUtils.buildWebOptions(oneDriveOptions).then(webOptions => {
      expect(webOptions.sendCacheControlHeader).toBeTruthy();
    });
  });

  it('must allow the sendCacheControlHeader to be set to false', async () => {
    WebUtils.buildWebOptions({
      web: {
        sendCacheControlHeader: false,
      },
    }).then(webOptions => {
      expect(webOptions.sendCacheControlHeader).toBeFalsy();
    });
  });

  describe('if pkceCode enabled', () => {
    beforeEach(() => {
      sessionStorageMock.clear();
    });
    describe('if a code exists in sessionStorage', () => {
      beforeEach(() => {
        const code = 'DEMO_CODE';
        WebUtils.setCodeVerifier(code);
      });
      it('should get the code correctly', async () => {
        const spy = jest.spyOn(WebUtils, 'getCodeVerifier');
        const webOptions = await WebUtils.buildWebOptions(implicitFlowOptions);
        expect(spy).toHaveBeenCalled();
        expect(webOptions.pkceCodeVerifier).toBe('DEMO_CODE');
      });
    });
    describe("if a code doesn't exist in sessionStorage", () => {
      it('should set the code', async () => {
        const spy = jest.spyOn(WebUtils, 'setCodeVerifier');
        const webOptions = await WebUtils.buildWebOptions(implicitFlowOptions);
        expect(webOptions.pkceCodeVerifier).toBeDefined();
        expect(spy).toHaveBeenCalled();
      });
    });
  });
});

describe('Url param extraction', () => {
  it('should return undefined on null url', () => {
    const paramObj = WebUtils.getUrlParams(null!);
    expect(paramObj).toBeUndefined();
  });

  it('should return undefined on empty url', () => {
    const paramObj = WebUtils.getUrlParams('');
    expect(paramObj).toBeUndefined();
  });

  it('should return undefined on url with spaces', () => {
    const paramObj = WebUtils.getUrlParams('    ');
    expect(paramObj).toBeUndefined();
  });

  it('should return undefined if no params in url', () => {
    const paramObj = WebUtils.getUrlParams('https://app.example.com/');
    expect(paramObj).toBeUndefined();
  });

  it('should return undefined if no params in url search', () => {
    const paramObj = WebUtils.getUrlParams('https://app.example.com?');
    expect(paramObj).toBeUndefined();
  });

  it('should return undefined if no params in url hash', () => {
    const paramObj = WebUtils.getUrlParams('https://app.example.com#');
    expect(paramObj).toBeUndefined();
  });

  it('should remove invalid combinations one param', () => {
    const paramObj = WebUtils.getUrlParams('https://app.example.com?=test');
    expect(paramObj).toBeUndefined();
  });

  it('should remove invalid combinations multiple param', () => {
    const paramObj = WebUtils.getUrlParams(
      'https://app.example.com?=test&key1=param1',
    );
    expect(paramObj).toEqual({ key1: 'param1' });
  });

  it('should extract work with a single param', () => {
    const paramObj = WebUtils.getUrlParams(
      'https://app.example.com?access_token=testtoken',
    );
    expect(paramObj!['access_token']).toStrictEqual('testtoken');
  });

  it('should extract a uuid state param', () => {
    const state = WebUtils.randomString();
    const paramObj = WebUtils.getUrlParams(
      `https://app.example.com?state=${state}&access_token=testtoken`,
    );
    expect(paramObj!['state']).toStrictEqual(state);
  });

  it('should use query flag and ignore hash flag', () => {
    const random = WebUtils.randomString();
    const foo = WebUtils.randomString();
    const paramObj = WebUtils.getUrlParams(
      `https://app.example.com?random=${random}&foo=${foo}#ignored`,
    );
    expect(paramObj!['random']).toStrictEqual(random);
    expect(paramObj!['foo']).toStrictEqual(foo);
  });

  it('should use query flag with another question mark in a param', () => {
    const random = WebUtils.randomString();
    const foo = WebUtils.randomString();
    const paramObj = WebUtils.getUrlParams(
      `https://app.example.com?random=${random}&foo=${foo}?questionmark`,
    );
    expect(paramObj!['random']).toStrictEqual(random);
    expect(paramObj!['foo']).toStrictEqual(`${foo}?questionmark`);
  });

  it('should use hash flag and ignore query flag', () => {
    const random = WebUtils.randomString();
    const foo = WebUtils.randomString();
    const paramObj = WebUtils.getUrlParams(
      `https://app.example.com#random=${random}&foo=${foo}?ignored`,
    );
    expect(paramObj!['random']).toStrictEqual(random);
    expect(paramObj!['foo']).toStrictEqual(`${foo}?ignored`);
  });

  it('should use hash flag with another hash in a param', () => {
    const random = WebUtils.randomString();
    const foo = WebUtils.randomString();
    const paramObj = WebUtils.getUrlParams(
      `https://app.example.com#random=${random}&foo=${foo}#hash`,
    );
    expect(paramObj!['random']).toStrictEqual(random);
    expect(paramObj!['foo']).toStrictEqual(`${foo}#hash`);
  });

  it('should extract hash params correctly', () => {
    const random = WebUtils.randomString(20);
    const url = `http://localhost:4200/#state=${random}&access_token=ya29.a0ARrdaM-sdfsfsdfsdfsdfs-YGFHwg_lM6dePPaT_TunbpsdfsdfsdfsEG6vTVLsLJDDW
        tv5m1Q8_g3hXraaoELYGsjl53&token_type=Bearer&expires_in=3599&scope=email%20profile%20openid%20
        https://www.googleapis.com/auth/userinfo.email%20https://www.googleapis.com/auth/userinfo.profile&authuser=0&prompt=none`;
    const paramObj = WebUtils.getUrlParams(url);
    expect(paramObj!['access_token']).toBeDefined();
    expect(paramObj!['token_type']).toStrictEqual('Bearer');
    expect(paramObj!['prompt']).toBeDefined();
    expect(paramObj!['state']).toStrictEqual(random);
  });

  it('should extract hash params if search param indicator present', () => {
    const token = 'sldfskdjflsdf12302';
    const url = `http://localhost:3000/login?#access_token=${token}`;
    const paramObj = WebUtils.getUrlParams(url);
    expect(paramObj!['access_token']).toStrictEqual(token);
  });
});

describe('Random string gen', () => {
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

describe('Authorization url building', () => {
  it('should contain a nonce param', async () => {
    WebUtils.buildWebOptions(oneDriveOptions).then(webOptions => {
      const authorizationUrl = WebUtils.getAuthorizationUrl(webOptions);
      expect(authorizationUrl).toContain('nonce');
    });
  });
});

describe('Crypto utils', () => {
  it('base 64 simple', () => {
    const arr: Uint8Array = CryptoUtils.toUint8Array('tester');
    const expected = CryptoUtils.toBase64(arr);
    expect(expected).toEqual('dGVzdGVy');
  });

  it('base 64 special char', () => {
    const arr: Uint8Array = CryptoUtils.toUint8Array('testerposfieppw2874929');
    const expected = CryptoUtils.toBase64(arr);
    expect(expected).toEqual('dGVzdGVycG9zZmllcHB3Mjg3NDkyOQ==');
  });

  it('base 64 with space', () => {
    const arr: Uint8Array = CryptoUtils.toUint8Array('base64 encoder');
    const expected = CryptoUtils.toBase64(arr);
    expect(expected).toEqual('YmFzZTY0IGVuY29kZXI=');
  });

  it('base64url safe all base64 special chars included', () => {
    const expected = CryptoUtils.toBase64Url('YmFz+TY0IG/uY29kZXI=');
    expect(expected).toEqual('YmFz-TY0IG_uY29kZXI');
  });
});

describe('additional resource headers', () => {
  const headerKey = 'Access-Control-Allow-Origin';

  const options: OAuth2AuthenticateOptions = {
    appId: 'appId',
    authorizationBaseUrl:
      'https://login.microsoftonline.com/common/oauth2/v2.0/authorize',
    accessTokenEndpoint:
      'https://login.microsoftonline.com/common/oauth2/v2.0/token',
    scope: 'files.readwrite offline_access',
    responseType: 'code',
    additionalResourceHeaders: {
      'Access-Control-Allow-Origin': 'will-be-overwritten',
    },
    web: {
      redirectUrl: 'https://oauth2.byteowls.com/authorize',
      pkceEnabled: false,
      additionalResourceHeaders: {
        'Access-Control-Allow-Origin': '*',
      },
    },
  };

  it('should be defined', async () => {
    const webOptions = await WebUtils.buildWebOptions(options);
    expect(webOptions.additionalResourceHeaders[headerKey]).toBeDefined();
  });

  it('should equal *', async () => {
    const webOptions = await WebUtils.buildWebOptions(options);
    expect(webOptions.additionalResourceHeaders[headerKey]).toEqual('*');
  });
});

describe('implicit redirect authentication flow helpers', () => {
  beforeEach(() => {
    sessionStorageMock.clear();
  });

  it('should set code in session storage', () => {
    const code = 'DEMO_CODE';
    const codeSet = WebUtils.setCodeVerifier(code);
    expect(window.sessionStorage.setItem).toHaveBeenCalledWith(
      `I_Capacitor_GenericOAuth2Plugin_PKCE`,
      code,
    );
    expect(codeSet).toEqual(true);
  });

  it('should get code if it exists in sessionStorage', () => {
    const code = 'DEMO_CODE';
    WebUtils.setCodeVerifier(code);
    const readCode = WebUtils.getCodeVerifier();
    expect(readCode).toBe(code);
    expect(window.sessionStorage.getItem).toHaveBeenCalledWith(
      `I_Capacitor_GenericOAuth2Plugin_PKCE`,
    );
  });

  it("should get null if code doesn't exist in sessionStorage", () => {
    const readCode = WebUtils.getCodeVerifier();
    expect(readCode).toBeNull();
    expect(window.sessionStorage.getItem).toHaveBeenCalledWith(
      `I_Capacitor_GenericOAuth2Plugin_PKCE`,
    );
  });

  it('should remove the code from sessionStorage', () => {
    WebUtils.clearCodeVerifier();
    expect(window.sessionStorage.removeItem).toHaveBeenCalledWith(
      `I_Capacitor_GenericOAuth2Plugin_PKCE`,
    );
  });
});
