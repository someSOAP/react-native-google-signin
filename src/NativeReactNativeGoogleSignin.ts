import { TurboModuleRegistry, type TurboModule } from 'react-native';

export interface GetGoogleCredentialsConfigs {
  serverClientId: string;
  /** iOS OAuth client ID. Falls back to GIDClientID / GoogleService-Info.plist. */
  iosClientId?: string;
  /** Forwarded unchanged. The consuming backend must verify this nonce. */
  nonce?: string;
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
