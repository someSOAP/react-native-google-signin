import ReactNativeGoogleSignin from './NativeReactNativeGoogleSignin';

export const getGoogleSignInToken =
  ReactNativeGoogleSignin.getGoogleCredentials;

export const ErrorCodes = {
  GET_CREDENTIALS_ERROR: 'GET_CREDENTIALS_ERROR',
  CANCELLATION_ERROR: 'CANCELLATION_ERROR',
} as const;
