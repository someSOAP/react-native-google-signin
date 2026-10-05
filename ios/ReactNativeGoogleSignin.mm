#import "ReactNativeGoogleSignin.h"
#import "GSAuthCoordinator.h"
#import <GoogleSignIn/GoogleSignIn.h>
#import <UIKit/UIKit.h>

@interface GSGoogleProvider : NSObject <GSAuthProvider>
@property(nonatomic) BOOL busy;
@end
@implementation GSGoogleProvider
+ (instancetype)shared {
  static GSGoogleProvider *provider; static dispatch_once_t once;
  dispatch_once(&once, ^{ provider = [GSGoogleProvider new]; }); return provider;
}
- (void)startWithClientID:(NSString *)clientID serverClientID:(NSString *)serverClientID
                   nonce:(NSString *)nonce presenter:(id)presenter
              completion:(void (^)(NSDictionary *, NSError *))completion {
  if (self.busy) { completion(nil, [NSError errorWithDomain:@"GSAuthProviderBusy" code:1 userInfo:nil]); return; }
  GIDSignIn.sharedInstance.configuration = [[GIDConfiguration alloc] initWithClientID:clientID serverClientID:serverClientID];
  self.busy = YES;
  @try {
    [GIDSignIn.sharedInstance signInWithPresentingViewController:presenter hint:nil additionalScopes:nil nonce:nonce
      completion:^(GIDSignInResult *result, NSError *error) {
        self.busy = NO;
        if (error || !result.user) { completion(nil, error); return; }
        GIDGoogleUser *user = result.user;
        NSMutableDictionary *response = [NSMutableDictionary new];
        if (user.idToken.tokenString) response[@"idToken"] = user.idToken.tokenString;
        if (user.profile.givenName) response[@"givenName"] = user.profile.givenName;
        if (user.profile.familyName) response[@"familyName"] = user.profile.familyName;
        if (user.profile.email) response[@"email"] = user.profile.email;
        NSURL *photo = user.profile.hasImage ? [user.profile imageURLWithDimension:120] : nil;
        if (photo.absoluteString) response[@"profilePictureUri"] = photo.absoluteString;
        completion(response, nil);
      }];
  } @catch (NSException *exception) { self.busy = NO; @throw; }
}
- (BOOL)signOut:(NSError **)error {
  if (self.busy) return NO;
  [GIDSignIn.sharedInstance signOut]; return YES;
}
@end

static UIViewController *GSForegroundPresenter(void) {
  for (UIScene *scene in UIApplication.sharedApplication.connectedScenes) {
    if (scene.activationState != UISceneActivationStateForegroundActive || ![scene isKindOfClass:UIWindowScene.class]) continue;
    for (UIWindow *window in ((UIWindowScene *)scene).windows) {
      if (!window.isKeyWindow) continue;
      UIViewController *controller = window.rootViewController;
      while (controller) {
        if (controller.presentedViewController && !controller.presentedViewController.isBeingDismissed) controller = controller.presentedViewController;
        else if ([controller isKindOfClass:UINavigationController.class]) controller = ((UINavigationController *)controller).visibleViewController;
        else if ([controller isKindOfClass:UITabBarController.class]) controller = ((UITabBarController *)controller).selectedViewController;
        else break;
      }
      return controller.view.window ? controller : nil;
    }
  }
  return nil;
}

@implementation ReactNativeGoogleSignin {
  GSAuthCoordinator *_coordinator;
  BOOL _invalidated;
}
+ (NSString *)moduleName { return @"ReactNativeGoogleSignin"; }
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params {
  return std::make_shared<facebook::react::NativeReactNativeGoogleSigninSpecJSI>(params);
}
- (GSAuthCoordinator *)coordinator {
  if (!_coordinator) {
    NSURL *url = [NSBundle.mainBundle URLForResource:@"GoogleService-Info" withExtension:@"plist"];
    NSDictionary *googleInfo = url ? [NSDictionary dictionaryWithContentsOfURL:url] : nil;
    _coordinator = [[GSAuthCoordinator alloc] initWithProvider:GSGoogleProvider.shared
      info:NSBundle.mainBundle.infoDictionary ?: @{} fallbackClientID:googleInfo[@"CLIENT_ID"]];
    if (_invalidated) [_coordinator invalidate];
  }
  return _coordinator;
}
- (void)getGoogleCredentials:(JS::NativeReactNativeGoogleSignin::GetGoogleCredentialsConfigs &)configs
                     resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  NSMutableDictionary *config = [NSMutableDictionary new];
  if (configs.serverClientId()) config[@"serverClientId"] = configs.serverClientId();
  if (configs.iosClientId()) config[@"iosClientId"] = configs.iosClientId();
  if (configs.nonce()) config[@"nonce"] = configs.nonce();
  dispatch_async(dispatch_get_main_queue(), ^{
    [self.coordinator signIn:config presenter:GSForegroundPresenter() resolve:resolve reject:^(NSString *code) {
      reject(code, [NSString stringWithFormat:@"Google authentication failed (%@).", code], nil);
    }];
  });
}
- (void)signOut:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  dispatch_async(dispatch_get_main_queue(), ^{
    [self.coordinator signOutWithResolve:resolve reject:^(NSString *code) {
      reject(code, [NSString stringWithFormat:@"Google authentication failed (%@).", code], nil);
    }];
  });
}
- (void)invalidate {
  dispatch_async(dispatch_get_main_queue(), ^{ self->_invalidated = YES; [self->_coordinator invalidate]; });
}
@end
