import type { GetGoogleCredentialsConfigs as NativeConfigs } from './NativeReactNativeGoogleSignin';

export type AndroidGoogleSignInOptions =
  | {
      flow: 'button';
      hostedDomain?: string;
      filterByAuthorizedAccounts?: never;
      autoSelect?: never;
    }
  | {
      flow: 'bottomSheet';
      /** Defaults to true: only accounts that previously authorized the app. */
      filterByAuthorizedAccounts?: boolean;
      /** Defaults to false. Google decides whether automatic selection is eligible. */
      autoSelect?: boolean;
      hostedDomain?: string;
    };

export type GetGoogleCredentialsConfigs = Omit<NativeConfigs, 'android'> & {
  /** Android only; omitted options preserve the explicit button flow. */
  android?: AndroidGoogleSignInOptions;
};
