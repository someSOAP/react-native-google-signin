#import "ReactNativeGoogleSignin.h"
#import <React/RCTUtils.h>
#import <GoogleSignIn/GoogleSignIn.h>

@implementation ReactNativeGoogleSignin

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeReactNativeGoogleSigninSpecJSI>(params);
}

+ (NSString *)moduleName
{
  return @"ReactNativeGoogleSignin";
}


- (void) configure:(NSString *)serverClientId {    
    NSDictionary *plist = self.plist;
  
    if(plist == nil) {
      NSError *error;
    
      NSURL *plistPath = [[NSBundle mainBundle] URLForResource:@"GoogleService-Info" withExtension:@"plist"];
      plist = [[NSDictionary alloc] initWithContentsOfURL:plistPath error:&error];
      
      if (error) {
        @throw [NSException exceptionWithName:@"RNGoogleSignin" reason:@"Failed to read GoogleService-Info.plist." userInfo:nil];
      }
      self.plist = plist;
    }
  
    NSString* clientId = plist[@"CLIENT_ID"];

    GIDConfiguration* config = [[GIDConfiguration alloc] initWithClientID:clientId serverClientID:serverClientId];
    GIDSignIn.sharedInstance.configuration = config;
}


- (void)handleError:(NSError *)error
             reject:(nonnull RCTPromiseRejectBlock)reject
{
  if(error.code == -5) {
    reject(@"CANCELLATION_ERROR", error.localizedDescription, nil);
    return;
  }
  
  reject(@"GET_CREDENTIALS_ERROR", error.localizedDescription, nil);
}

- (void)getGoogleCredentials:(JS::NativeReactNativeGoogleSignin::GetGoogleCredentialsConfigs &)configs
                     resolve:(nonnull RCTPromiseResolveBlock)resolve
                      reject:(nonnull RCTPromiseRejectBlock)reject
{
  
  [self configure:configs.serverClientId()];
  
  NSString *nonce = configs.nonce();
  dispatch_async(dispatch_get_main_queue(), ^{
    [
      GIDSignIn.sharedInstance
      signInWithPresentingViewController:RCTPresentedViewController()
      hint:nil
      additionalScopes:nil
      nonce:nonce
      completion:^(GIDSignInResult * _Nullable signInResult, NSError * _Nullable error) {
        if(error != nil) {
          [self handleError:error reject:reject];
        } else {
          
          GIDGoogleUser *user = signInResult.user;
          
          NSURL *profilePictureUri = nil;
          
          if(user.profile.hasImage) {
            profilePictureUri = [user.profile imageURLWithDimension:120];
          }
          
          NSDictionary *response = @{
            @"idToken": user.idToken.tokenString,
            @"givenName": user.profile.givenName,
            @"familyName": user.profile.familyName,
            @"email": user.profile.email,
            @"profilePictureUri": profilePictureUri.absoluteString
          };
          
          resolve(response);
        }
      }
    ];
  });
}

@end
