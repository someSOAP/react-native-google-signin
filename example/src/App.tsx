import { View, StyleSheet, Button, Alert } from 'react-native';
import { getGoogleSignInToken } from '@someosap/react-native-google-signin';

const serverClientId = ''; // Put your server client id here

export default function App() {
  return (
    <View style={styles.container}>
      <Button
        title="TEST"
        onPress={() => {
          console.log('STARTED');
          getGoogleSignInToken({
            serverClientId,
          })
            .then((res) => {
              Alert.alert('Token:', res);
            })
            .catch(console.error);
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
