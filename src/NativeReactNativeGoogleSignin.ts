import { TurboModuleRegistry, type TurboModule } from 'react-native';

// Codegen uses a single object; the public API narrows this to a flow union.
export interface NativeAndroidSignInOptions {
  flow: string;
  filterByAuthorizedAccounts?: boolean;
  autoSelect?: boolean;
  hostedDomain?: string;
}

export interface GetGoogleCredentialsConfigs {
  serverClientId: string;
  /** iOS OAuth client ID. Falls back to GIDClientID / GoogleService-Info.plist. */
  iosClientId?: string;
  /** Forwarded unchanged. The consuming backend must verify this nonce. */
  nonce?: string;
  android?: NativeAndroidSignInOptions;
}

export interface GetGoogleCredentialsResponse {
  idToken: string;
  givenName?: string;
  familyName?: string;
  email?: string;
  profilePictureUri?: string;
}

export interface Spec extends TurboModule {
  getGoogleCredentials(
    configs: GetGoogleCredentialsConfigs
  ): Promise<GetGoogleCredentialsResponse>;
  signOut(): Promise<void>;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'ReactNativeGoogleSignin'
);
