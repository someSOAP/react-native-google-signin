# react-native-google-signin

This package provides bare minimum required for retrieving google credentials for OAuth authentication.

Under the hood this packages uses modern [Credential Manager](https://developers.google.com/identity/android-credential-manager)
for Android and [Google Sign-In](https://developers.google.com/identity/sign-in/ios/start-integrating) for iOS.

## Installation

```sh
npm install @somesoap/react-native-google-signin
```

or

```sh
yarn add install @somesoap/react-native-google-signin
```

## Usage

### Retrieve idToken from Google credentials

The most common use for this package is to receive `idToken` for further usage for authentication via your
backend services or Firebase Auth.

```tsx
import { getGoogleSignInToken } from '@somesoap/react-native-google-signin';

export function getIdToken() {
  return getGoogleSignInToken({
    serverClientId: "{SERVER_CLEINT_ID}", // Usualy received from Firebase or Google Console
  })
    .then((credentials) => credentials.idToken)
}

```

### Authenticate via Firebase

Purposed usage is for OAuth authentication in conjunction with Firebase.

```tsx
import { Button } from 'react-native';

import { getGoogleSignInToken, ErrorCodes } from '@somesoap/react-native-google-signin';
import { GoogleAuthProvider, getAuth, signInWithCredential, onAuthStateChanged } from '@react-native-firebase/auth';

const handleSignInPress = async () => {
  try {
    const googleCredentials = await getGoogleSignInToken({
      serverClientId: "{SET_YOUR_SERVER_CLIENT_ID}",
    });

    // Create a Google credential with the token
    const googleCredential = GoogleAuthProvider.credential(googleCredentials.idToken);

    console.log({googleCredential, googleCredentials});
    // Sign-in the user with the credential
    await signInWithCredential(getAuth(), googleCredential);
  } catch (err: any) {
    if (err.code === ErrorCodes.CANCELLATION_ERROR) {
      // do nothing, user cancelled the action
      return;
    }
    console.error(err);
  }
};

const handleLogOutPress = async () => {
  try {
    await signOut(getAuth());
  } catch (err) {
    console.error(err);
  }
};


const SignInTest = () => {
  const [user, setUser] = React.useState<any>(null);

  React.useEffect(() => {
    return onAuthStateChanged(getAuth(), setUser); // unsubscribe on unmount
  }, []);

  if (!user) {
    return (
      <View>
        <Button text="Sign In" onPress={handleSignInPress} />
      </View>
    );
  }

  return (
    <View>
      <Text>Welcome {user.email}</Text>
      <Button text="Sign out" onPress={handleLogOutPress} />
    </View>
  );
};
```




## Contributing

This library is containing bare minimal functionality to provide authentication via Google OAuth.

For features request contact me via [email](mailto:andrey.meg@gmail.com).


If you are willing to contribute, check the guides below:

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)



## License

MIT
