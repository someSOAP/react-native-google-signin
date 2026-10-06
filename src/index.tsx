import NativeGoogleSignin from './NativeReactNativeGoogleSignin';
import type {
  GetGoogleCredentialsResponse,
  NativeAndroidSignInOptions,
} from './NativeReactNativeGoogleSignin';
import type { GetGoogleCredentialsConfigs } from './types';

export type {
  GetGoogleCredentialsConfigs,
  AndroidGoogleSignInOptions,
} from './types';
export type { GetGoogleCredentialsResponse };

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

function validAndroidOptions(
  value: unknown
): value is NativeAndroidSignInOptions {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false;
  const options = value as NativeAndroidSignInOptions;
  if (options.flow !== 'button' && options.flow !== 'bottomSheet') return false;
  if (
    (options.filterByAuthorizedAccounts !== undefined &&
      typeof options.filterByAuthorizedAccounts !== 'boolean') ||
    (options.autoSelect !== undefined &&
      typeof options.autoSelect !== 'boolean') ||
    (options.flow === 'button' &&
      (options.filterByAuthorizedAccounts !== undefined ||
        options.autoSelect !== undefined))
  )
    return false;
  if (options.hostedDomain !== undefined) {
    const domain = options.hostedDomain;
    if (
      typeof domain !== 'string' ||
      domain.length > 253 ||
      !domain.includes('.') ||
      !domain
        .split('.')
        .every((label) =>
          /^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$/.test(label)
        )
    )
      return false;
  }
  return true;
}

/** Google owns presentation, consent and reauthentication for the selected flow. */
export async function getGoogleSignInToken(
  configs: GetGoogleCredentialsConfigs
): Promise<GetGoogleCredentialsResponse> {
  if (
    !configs ||
    !validClientId(configs.serverClientId) ||
    (configs.iosClientId !== undefined &&
      !validClientId(configs.iosClientId)) ||
    (configs.nonce !== undefined &&
      (typeof configs.nonce !== 'string' || !configs.nonce.trim())) ||
    (configs.android !== undefined && !validAndroidOptions(configs.android))
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
      ...(configs.android !== undefined && {
        android: {
          flow: configs.android.flow,
          ...(configs.android.hostedDomain !== undefined && {
            hostedDomain: configs.android.hostedDomain,
          }),
          ...(configs.android.filterByAuthorizedAccounts !== undefined && {
            filterByAuthorizedAccounts:
              configs.android.filterByAuthorizedAccounts,
          }),
          ...(configs.android.autoSelect !== undefined && {
            autoSelect: configs.android.autoSelect,
          }),
        },
      }),
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
