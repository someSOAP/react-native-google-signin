// Only imported by index.e2e.js, selected explicitly in debug builds.
// Simulated app UI checks. Native provider tests and real OAuth are separate.
import { useState } from 'react';
import { Button, Text, View, StyleSheet } from 'react-native';
import { GoogleSignInError } from '@somesoap/react-native-google-signin';
import { AuthScreen } from './App';

type Scenario = 'success' | 'cancel' | 'failure' | 'logoutFailure';
export default function VerificationApp() {
  const [scenario, setScenario] = useState<Scenario>('success');
  const actions = {
    async signIn() {
      await new Promise<void>((resolve) => setTimeout(resolve, 200));
      if (scenario === 'cancel')
        throw new GoogleSignInError('CANCELLATION_ERROR');
      if (scenario === 'failure') throw new GoogleSignInError('NETWORK_ERROR');
      return { idToken: 'SIMULATED_NON_OAUTH_RESULT' };
    },
    async signOut() {
      if (scenario === 'logoutFailure')
        throw new GoogleSignInError('SIGN_OUT_ERROR');
    },
  };
  return (
    <View style={styles.container}>
      <Text testID="simulation-label">Simulated provider UI verification</Text>
      <Button
        testID="scenario-success"
        title="Next: success"
        onPress={() => setScenario('success')}
      />
      <Button
        testID="scenario-cancel"
        title="Next: cancellation"
        onPress={() => setScenario('cancel')}
      />
      <Button
        testID="scenario-failure"
        title="Next: failure"
        onPress={() => setScenario('failure')}
      />
      <Button
        testID="scenario-logoutFailure"
        title="Next: logout failure"
        onPress={() => setScenario('logoutFailure')}
      />
      <AuthScreen actions={actions} />
    </View>
  );
}

const styles = StyleSheet.create({ container: { flex: 1, paddingTop: 64 } });
