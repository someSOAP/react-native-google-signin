import ReactNativeGoogleSignin from './NativeReactNativeGoogleSignin';

interface SignInConfig {
  serverClientId: string;
  autoSelectEnabled?: boolean;
  filterByAuthorizedAccounts?: boolean;
  requestVerifiedPhoneNumber?: boolean;
  nonce?: string;
}

export function getGoogleSignInToken(config: SignInConfig): Promise<string> {
  return ReactNativeGoogleSignin.getGoogleSignInToken(config);
}
