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
              console.log({ res });
              Alert.alert('Token:', JSON.stringify(res));
            })
            .catch((err) => {
              if (err.code === 'CANCELLATION_ERR') {
                return;
              }
              console.error(err);
            });
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
