import NativeGoogleSignin from './NativeReactNativeGoogleSignin';
import type {
  GetGoogleCredentialsConfigs,
  GetGoogleCredentialsResponse,
} from './NativeReactNativeGoogleSignin';

export type { GetGoogleCredentialsConfigs, GetGoogleCredentialsResponse };

export const ErrorCodes = {
  GET_CREDENTIALS_ERROR: 'GET_CREDENTIALS_ERROR',
  CANCELLATION_ERROR: 'CANCELLATION_ERROR',
  NO_CREDENTIALS_ERROR: 'NO_CREDENTIALS_ERROR',
  UNKNOWN_CREDENTIALS_TYPE: 'UNKNOWN_CREDENTIALS_TYPE',
  ERR_ACTIVITY: 'ERR_ACTIVITY',
  CONFIGURATION_ERROR: 'CONFIGURATION_ERROR',
  PROVIDER_UNAVAILABLE: 'PROVIDER_UNAVAILABLE',
  NETWORK_ERROR: 'NETWORK_ERROR',
  INVALID_TOKEN_ERROR: 'INVALID_TOKEN_ERROR',
  IN_PROGRESS: 'IN_PROGRESS',
  MODULE_DESTROYED: 'MODULE_DESTROYED',
  SIGN_OUT_ERROR: 'SIGN_OUT_ERROR',
  TIMEOUT_ERROR: 'TIMEOUT_ERROR',
} as const;

export type GoogleSignInErrorCode =
  (typeof ErrorCodes)[keyof typeof ErrorCodes];

export class GoogleSignInError extends Error {
  readonly code: GoogleSignInErrorCode;
  constructor(code: GoogleSignInErrorCode) {
    // Keep provider descriptions out of logs: they can contain credentials.
    super(`Google authentication failed (${code}).`);
    this.name = 'GoogleSignInError';
    this.code = code;
  }
}

export function isGoogleSignInError(
  error: unknown
): error is GoogleSignInError {
  return error instanceof GoogleSignInError;
}

function normalizeError(error: unknown, fallback: GoogleSignInErrorCode) {
  const code = (error as { code?: unknown } | null)?.code;
  return new GoogleSignInError(
    Object.values(ErrorCodes).includes(code as GoogleSignInErrorCode)
      ? (code as GoogleSignInErrorCode)
      : fallback
  );
}

function validClientId(value: unknown): value is string {
  return (
    typeof value === 'string' &&
    /^[A-Za-z0-9_-]+\.apps\.googleusercontent\.com$/.test(value)
  );
}

/** Interactive Google button flow. Google owns consent and reauthentication. */
export async function getGoogleSignInToken(
  configs: GetGoogleCredentialsConfigs
): Promise<GetGoogleCredentialsResponse> {
  if (
    !configs ||
    !validClientId(configs.serverClientId) ||
    (configs.iosClientId !== undefined &&
      !validClientId(configs.iosClientId)) ||
    (configs.nonce !== undefined &&
      (typeof configs.nonce !== 'string' || !configs.nonce.trim()))
  ) {
    throw new GoogleSignInError(ErrorCodes.CONFIGURATION_ERROR);
  }
  try {
    const result = await NativeGoogleSignin.getGoogleCredentials({
      serverClientId: configs.serverClientId,
      ...(configs.iosClientId !== undefined && {
        iosClientId: configs.iosClientId,
      }),
      ...(configs.nonce !== undefined && { nonce: configs.nonce }),
    });
    if (
      !result ||
      typeof result.idToken !== 'string' ||
      !result.idToken.trim()
    ) {
      throw new GoogleSignInError(ErrorCodes.INVALID_TOKEN_ERROR);
    }
    return result;
  } catch (error) {
    throw normalizeError(error, ErrorCodes.GET_CREDENTIALS_ERROR);
  }
}

/** Clears Google provider state. Does not revoke consent or end your app session. */
export async function signOut(): Promise<void> {
  try {
    await NativeGoogleSignin.signOut();
  } catch (error) {
    throw normalizeError(error, ErrorCodes.SIGN_OUT_ERROR);
  }
}
