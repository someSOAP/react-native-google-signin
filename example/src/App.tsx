import { useState } from 'react';
import { View, StyleSheet, Button, Text } from 'react-native';
import {
  getGoogleSignInToken,
  signOut,
  isGoogleSignInError,
  ErrorCodes,
} from '@somesoap/react-native-google-signin';
import config from './config.local.json';
import type { GetGoogleCredentialsResponse } from '@somesoap/react-native-google-signin';

interface Actions {
  signIn(): Promise<GetGoogleCredentialsResponse>;
  signOut(): Promise<void>;
}
const realActions: Actions = {
  signIn: () => getGoogleSignInToken(config),
  signOut,
};

export default function App() {
  return <AuthScreen actions={realActions} />;
}

export function AuthScreen({ actions }: { actions: Actions }) {
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState('Ready');
  async function run(action: 'signIn' | 'signOut') {
    setBusy(true);
    setStatus('Working');
    try {
      if (action === 'signOut') {
        await actions.signOut();
        setStatus('Google provider signed out');
      } else {
        const credentials = await actions.signIn();
        // Exchange credentials.idToken with your backend here. Never log it.
        setStatus(
          credentials.email
            ? `Google credentials received for ${credentials.email}`
            : 'Google credentials received'
        );
      }
    } catch (error) {
      setStatus(
        isGoogleSignInError(error)
          ? error.code === ErrorCodes.CANCELLATION_ERROR
            ? 'Cancelled; ready to retry'
            : error.code
          : 'Unexpected error'
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <View style={styles.container}>
      <Text accessibilityRole="header">Google authentication verification</Text>
      <Button
        testID="sign-in"
        title="Sign in with Google"
        disabled={busy}
        onPress={() => run('signIn')}
      />
      <Button
        testID="sign-out"
        title="Sign out of Google"
        disabled={busy}
        onPress={() => run('signOut')}
      />
      <Text testID="auth-status" accessibilityLiveRegion="polite">
        {status}
      </Text>
    </View>
  );
}
const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 20,
  },
});
