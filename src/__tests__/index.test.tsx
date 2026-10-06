import { TurboModuleRegistry } from 'react-native';

const native = {
  getGoogleCredentials: jest.fn(),
  signOut: jest.fn(),
};
jest
  .spyOn(TurboModuleRegistry, 'getEnforcing')
  .mockReturnValue(native as never);
const api = require('../index') as typeof import('../index');
const config = { serverClientId: '123-test.apps.googleusercontent.com' };

beforeEach(() => jest.clearAllMocks());

test('forwards server ID and nonce unchanged and preserves native result', async () => {
  const response = { idToken: 'provider-token', email: 'test@example.com' };
  native.getGoogleCredentials.mockResolvedValue(response);
  expect(
    await api.getGoogleSignInToken({
      ...config,
      nonce: 'caller nonce',
      iosClientId: 'ios.apps.googleusercontent.com',
    })
  ).toBe(response);
  expect(native.getGoogleCredentials).toHaveBeenCalledWith({
    ...config,
    nonce: 'caller nonce',
    iosClientId: 'ios.apps.googleusercontent.com',
  });
});
test('omitted nonce remains omitted; absent optional profile is valid', async () => {
  native.getGoogleCredentials.mockResolvedValue({ idToken: 'token' });
  expect(await api.getGoogleSignInToken(config)).toEqual({ idToken: 'token' });
  expect(native.getGoogleCredentials).toHaveBeenCalledWith(config);
});
test.each([
  { flow: 'button', hostedDomain: 'example.com' },
  { flow: 'bottomSheet' },
  {
    flow: 'bottomSheet',
    filterByAuthorizedAccounts: false,
    autoSelect: false,
    hostedDomain: 'example.com',
  },
  { flow: 'bottomSheet', filterByAuthorizedAccounts: true, autoSelect: true },
])(
  'forwards Android settings and preserves explicit false: %p',
  async (android) => {
    native.getGoogleCredentials.mockResolvedValue({ idToken: 'token' });
    await api.getGoogleSignInToken({ ...config, android } as never);
    expect(native.getGoogleCredentials).toHaveBeenCalledWith({
      ...config,
      android,
    });
  }
);
test.each([
  null,
  [],
  'bottomSheet',
  {},
  { flow: 'unknown' },
  { flow: 'button', autoSelect: false },
  { flow: 'button', filterByAuthorizedAccounts: false },
  { flow: 'bottomSheet', autoSelect: 'false' },
  { flow: 'bottomSheet', filterByAuthorizedAccounts: null },
  { flow: 'button', hostedDomain: null },
  { flow: 'bottomSheet', hostedDomain: '' },
  { flow: 'bottomSheet', hostedDomain: 'https://example.com' },
  { flow: 'bottomSheet', hostedDomain: '*.example.com' },
  { flow: 'bottomSheet', hostedDomain: ' example.com' },
  { flow: 'bottomSheet', hostedDomain: 'example..com' },
  { flow: 'bottomSheet', hostedDomain: '-example.com' },
])('invalid Android settings reject before native: %p', async (android) => {
  await expect(
    api.getGoogleSignInToken({ ...config, android } as never)
  ).rejects.toMatchObject({
    code: api.ErrorCodes.CONFIGURATION_ERROR,
  });
  expect(native.getGoogleCredentials).not.toHaveBeenCalled();
});
test.each([
  null,
  {},
  { serverClientId: '' },
  { serverClientId: 'bad' },
  { ...config, nonce: '' },
  { ...config, nonce: 123 },
  { ...config, iosClientId: 'bad' },
])('bad configuration rejects before invoking native: %p', async (value) => {
  await expect(api.getGoogleSignInToken(value as never)).rejects.toMatchObject({
    code: api.ErrorCodes.CONFIGURATION_ERROR,
  });
  expect(native.getGoogleCredentials).not.toHaveBeenCalled();
});
test.each([null, {}, { idToken: '' }, { idToken: ' ' }, { idToken: 123 }])(
  'invalid successful bridge result is rejected: %p',
  async (result) => {
    native.getGoogleCredentials.mockResolvedValue(result);
    await expect(api.getGoogleSignInToken(config)).rejects.toMatchObject({
      code: api.ErrorCodes.INVALID_TOKEN_ERROR,
    });
  }
);
test.each(Object.values(api.ErrorCodes))(
  'native error %s maps to a typed sanitized error',
  async (code) => {
    native.getGoogleCredentials.mockRejectedValue({
      code,
      message: 'secret token',
    });
    const error = await api
      .getGoogleSignInToken(config)
      .catch((value: unknown) => value);
    expect(api.isGoogleSignInError(error)).toBe(true);
    expect(error).toMatchObject({ code, name: 'GoogleSignInError' });
    expect((error as Error).message).not.toContain('secret');
    expect(native.getGoogleCredentials).toHaveBeenCalledTimes(1);
  }
);
test.each([new Error('secret'), null, { code: 'unexpected' }])(
  'unknown failure becomes stable error',
  async (failure) => {
    native.getGoogleCredentials.mockRejectedValue(failure);
    await expect(api.getGoogleSignInToken(config)).rejects.toMatchObject({
      code: api.ErrorCodes.GET_CREDENTIALS_ERROR,
    });
  }
);
test('retry is caller-controlled after cancellation', async () => {
  native.getGoogleCredentials
    .mockRejectedValueOnce({ code: 'CANCELLATION_ERROR' })
    .mockResolvedValueOnce({ idToken: 'token' });
  await expect(api.getGoogleSignInToken(config)).rejects.toMatchObject({
    code: 'CANCELLATION_ERROR',
  });
  expect(native.getGoogleCredentials).toHaveBeenCalledTimes(1);
  await expect(api.getGoogleSignInToken(config)).resolves.toEqual({
    idToken: 'token',
  });
});
test('provider sign-out resolves void and preserves typed failures', async () => {
  native.signOut.mockResolvedValue(null);
  await expect(api.signOut()).resolves.toBeUndefined();
  native.signOut.mockRejectedValue({ code: 'IN_PROGRESS' });
  await expect(api.signOut()).rejects.toMatchObject({ code: 'IN_PROGRESS' });
  native.signOut.mockRejectedValue(new Error('secret'));
  await expect(api.signOut()).rejects.toMatchObject({ code: 'SIGN_OUT_ERROR' });
});
test('synchronous native failure is caught', async () => {
  native.getGoogleCredentials.mockImplementationOnce(() => {
    throw new Error('secret');
  });
  await expect(api.getGoogleSignInToken(config)).rejects.toMatchObject({
    code: 'GET_CREDENTIALS_ERROR',
  });
});
