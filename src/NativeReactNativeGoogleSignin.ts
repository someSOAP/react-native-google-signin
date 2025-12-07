import { TurboModuleRegistry, type TurboModule } from 'react-native';
import type { UnsafeObject } from 'react-native/Libraries/Types/CodegenTypesNamespace';

export interface Spec extends TurboModule {
  getGoogleSignInToken(configs: UnsafeObject): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'ReactNativeGoogleSignin'
);
