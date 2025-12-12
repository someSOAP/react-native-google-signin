import { TurboModuleRegistry, type TurboModule } from 'react-native';

export interface GetGoogleCredentialsConfigs {
  serverClientId: string;
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
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'ReactNativeGoogleSignin'
);
