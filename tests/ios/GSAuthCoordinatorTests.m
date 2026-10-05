#import <XCTest/XCTest.h>
#import "GSAuthCoordinator.h"

@interface GSFakeProvider : NSObject <GSAuthProvider>
@property(nonatomic, copy) void (^completion)(NSDictionary *, NSError *);
@property(nonatomic, copy) NSDictionary *arguments;
@property(nonatomic) NSInteger starts;
@property(nonatomic) NSInteger signOuts;
@property(nonatomic) BOOL failSignOut;
@property(nonatomic) BOOL throwOnStart;
@property(nonatomic) BOOL throwOnSignOut;
@end
@implementation GSFakeProvider
- (void)startWithClientID:(NSString *)clientID serverClientID:(NSString *)serverClientID nonce:(NSString *)nonce
               presenter:(id)presenter completion:(void (^)(NSDictionary *, NSError *))completion {
  if (self.throwOnStart) @throw [NSException exceptionWithName:@"FakeException" reason:@"sensitive text" userInfo:nil];
  self.starts++; self.arguments = @{@"iosClientId": clientID, @"serverClientId": serverClientID, @"nonce": nonce ?: NSNull.null};
  self.completion = completion;
}
- (BOOL)signOut:(NSError **)error {
  if (self.throwOnSignOut) @throw [NSException exceptionWithName:@"FakeException" reason:@"sensitive text" userInfo:nil];
  self.signOuts++; return !self.failSignOut; }
@end
@interface GSOutcome : NSObject
@property(nonatomic) NSInteger settles;
@property(nonatomic, copy) NSString *code;
@property(nonatomic, strong) id result;
@end
@implementation GSOutcome
@end

@interface GSAuthCoordinatorTests : XCTestCase
@property(nonatomic, strong) GSFakeProvider *provider;
@property(nonatomic, strong) GSAuthCoordinator *auth;
@property(nonatomic, copy) NSDictionary *config;
@property(nonatomic, copy) NSDictionary *info;
@end
@implementation GSAuthCoordinatorTests
- (void)setUp {
  self.provider = [GSFakeProvider new];
  self.info = @{@"GIDClientID": @"ios.apps.googleusercontent.com", @"CFBundleURLTypes": @[@{@"CFBundleURLSchemes": @[@"com.googleusercontent.apps.ios"]}]};
  self.auth = [[GSAuthCoordinator alloc] initWithProvider:self.provider info:self.info fallbackClientID:nil];
  self.config = @{@"serverClientId": @"web.apps.googleusercontent.com", @"nonce": @"caller nonce"};
}
- (void)tearDown { [self.auth invalidate]; self.provider.completion = nil; }
- (GSOutcome *)start:(NSDictionary *)config presenter:(id)presenter {
  GSOutcome *outcome = [GSOutcome new];
  [self.auth signIn:config presenter:presenter resolve:^(id result) { outcome.settles++; outcome.result = result; }
    reject:^(NSString *code) { outcome.settles++; outcome.code = code; }];
  return outcome;
}
- (GSOutcome *)start { return [self start:self.config presenter:[NSObject new]]; }
- (GSOutcome *)logout {
  GSOutcome *outcome = [GSOutcome new];
  [self.auth signOutWithResolve:^(id result) { outcome.settles++; outcome.result = result; }
                        reject:^(NSString *code) { outcome.settles++; outcome.code = code; }]; return outcome;
}
- (void)drain {
  XCTestExpectation *done = [self expectationWithDescription:@"main callback drained"];
  dispatch_async(dispatch_get_main_queue(), ^{ [done fulfill]; });
  [self waitForExpectations:@[done] timeout:1];
}
- (NSString *)token { return @"eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxMjMifQ.c2ln"; }
- (void)success { self.provider.completion(@{@"idToken": self.token}, nil); [self drain]; }
- (void)testFirstAndReturningSignInForwardClientIDsAndNonce {
  GSOutcome *first = [self start]; [self success];
  XCTAssertEqualObjects(first.result[@"idToken"], self.token);
  XCTAssertEqualObjects(self.provider.arguments, (@{@"iosClientId": @"ios.apps.googleusercontent.com", @"serverClientId": @"web.apps.googleusercontent.com", @"nonce": @"caller nonce"}));
  GSOutcome *returning = [self start]; [self success]; XCTAssertEqual(returning.settles, 1); XCTAssertEqual(self.provider.starts, 2);
}
- (void)testMissingAndOptionalProfileFields {
  GSOutcome *outcome = [self start]; [self success]; XCTAssertNil(outcome.result[@"profilePictureUri"]);
  outcome = [self start]; self.provider.completion(@{@"idToken": self.token, @"givenName": @"Test", @"familyName": NSNull.null, @"email": @"test@example.com", @"profilePictureUri": @"https://example.com/photo"}, nil); [self drain];
  XCTAssertEqualObjects(outcome.result[@"givenName"], @"Test"); XCTAssertNil(outcome.result[@"familyName"]);
  XCTAssertEqualObjects(outcome.result[@"email"], @"test@example.com");
}
- (void)testConfigurationAndMissingPresenterNeverLaunchUI {
  for (id value in @[@"", @"bad", NSNull.null, @123]) {
    GSOutcome *outcome = [self start:@{@"serverClientId": value} presenter:[NSObject new]];
    XCTAssertEqualObjects(outcome.code, @"CONFIGURATION_ERROR");
  }
  GSOutcome *outcome = [self start:self.config presenter:nil]; XCTAssertEqualObjects(outcome.code, @"ERR_ACTIVITY");
  outcome = [self start:@{@"serverClientId": @"web.apps.googleusercontent.com", @"nonce": @" "} presenter:[NSObject new]];
  XCTAssertEqualObjects(outcome.code, @"CONFIGURATION_ERROR"); XCTAssertEqual(self.provider.starts, 0);
}
- (void)testMissingSchemeAndClientIDReject {
  self.auth = [[GSAuthCoordinator alloc] initWithProvider:self.provider info:@{} fallbackClientID:nil];
  XCTAssertEqualObjects([self start].code, @"CONFIGURATION_ERROR");
  self.auth = [[GSAuthCoordinator alloc] initWithProvider:self.provider info:@{@"GIDClientID": @"ios.apps.googleusercontent.com"} fallbackClientID:nil];
  XCTAssertEqualObjects([self start].code, @"CONFIGURATION_ERROR");
}
- (void)testClientIDOverrideAndPlistFallback {
  self.auth = [[GSAuthCoordinator alloc] initWithProvider:self.provider info:@{@"CFBundleURLTypes": self.info[@"CFBundleURLTypes"]} fallbackClientID:@"ios.apps.googleusercontent.com"];
  GSOutcome *outcome = [self start]; [self success]; XCTAssertEqual(outcome.settles, 1);
  outcome = [self start:@{@"serverClientId": @"web.apps.googleusercontent.com", @"iosClientId": @"ios.apps.googleusercontent.com"} presenter:[NSObject new]];
  [self success]; XCTAssertEqual(self.provider.arguments[@"nonce"], NSNull.null); XCTAssertEqual(outcome.settles, 1);
}
- (void)testCancellationHasNoRetryAndRequiresGoogleDomain {
  GSOutcome *outcome = [self start]; self.provider.completion(nil, [NSError errorWithDomain:@"com.google.GIDSignIn" code:-5 userInfo:nil]); [self drain];
  XCTAssertEqualObjects(outcome.code, @"CANCELLATION_ERROR"); XCTAssertEqual(self.provider.starts, 1);
  outcome = [self start]; self.provider.completion(nil, [NSError errorWithDomain:@"other" code:-5 userInfo:nil]); [self drain];
  XCTAssertEqualObjects(outcome.code, @"GET_CREDENTIALS_ERROR");
  outcome = [self start]; [self success]; XCTAssertEqual(outcome.settles, 1);
}
- (void)testNetworkAndReauthenticationErrorsAllowCallerRetry {
  for (NSString *domain in @[NSURLErrorDomain, @"com.google.GIDSignIn"]) {
    GSOutcome *outcome = [self start]; self.provider.completion(nil, [NSError errorWithDomain:domain code:-4 userInfo:nil]); [self drain];
    XCTAssertEqualObjects(outcome.code, [domain isEqual:NSURLErrorDomain] ? @"NETWORK_ERROR" : @"GET_CREDENTIALS_ERROR");
    outcome = [self start]; [self success]; XCTAssertEqual(outcome.settles, 1);
  }
}
- (void)testMissingEmptyAndMalformedTokensFail {
  for (id value in @[NSNull.null, @"", @" ", @"a.b.c", @"e30.e30.c2ln", @123]) {
    GSOutcome *outcome = [self start]; self.provider.completion(@{@"idToken": value}, nil); [self drain];
    XCTAssertEqualObjects(outcome.code, @"INVALID_TOKEN_ERROR"); XCTAssertEqual(outcome.settles, 1);
  }
  GSOutcome *outcome = [self start]; self.provider.completion(nil, nil); [self drain]; XCTAssertEqualObjects(outcome.code, @"INVALID_TOKEN_ERROR");
}
- (void)testConcurrentCallsAndDuplicateStaleCallbacks {
  GSOutcome *first = [self start]; void (^old)(NSDictionary *, NSError *) = self.provider.completion;
  XCTAssertEqualObjects([self start].code, @"IN_PROGRESS"); XCTAssertEqualObjects([self logout].code, @"IN_PROGRESS");
  [self success]; old(nil, [NSError errorWithDomain:@"com.google.GIDSignIn" code:-5 userInfo:nil]); [self drain]; XCTAssertEqual(first.settles, 1);
  GSOutcome *second = [self start]; old(@{@"idToken": self.token}, nil); [self drain]; XCTAssertEqual(second.settles, 0);
  [self success]; XCTAssertEqual(second.settles, 1);
}
- (void)testTeardownSettlesAndClearsLateProviderSuccess {
  GSOutcome *outcome = [self start]; [self.auth invalidate]; XCTAssertEqualObjects(outcome.code, @"MODULE_DESTROYED");
  [self success]; XCTAssertEqual(outcome.settles, 1); XCTAssertEqual(self.provider.signOuts, 1);
  XCTAssertEqualObjects([self start].code, @"MODULE_DESTROYED");
}
- (void)testLateTeardownCleanupExceptionDoesNotCrash {
  GSOutcome *outcome = [self start]; [self.auth invalidate];
  self.provider.throwOnSignOut = YES; [self success];
  XCTAssertEqualObjects(outcome.code, @"MODULE_DESTROYED"); XCTAssertEqual(outcome.settles, 1);
}
- (void)testLogoutSuccessFailureAndNewSignIn {
  XCTAssertNil([self logout].code); XCTAssertEqual(self.provider.signOuts, 1);
  self.provider.failSignOut = YES; XCTAssertEqualObjects([self logout].code, @"SIGN_OUT_ERROR");
  self.provider.failSignOut = NO; GSOutcome *outcome = [self start]; [self success]; XCTAssertEqual(outcome.settles, 1);
}
- (void)testSynchronousExceptionsSettle {
  self.provider.throwOnStart = YES; XCTAssertEqualObjects([self start].code, @"GET_CREDENTIALS_ERROR");
  self.provider.throwOnStart = NO; GSOutcome *outcome = [self start]; [self success]; XCTAssertEqual(outcome.settles, 1);
}
- (void)testSignOutExceptionAndRetry {
  self.provider.throwOnSignOut = YES; XCTAssertEqualObjects([self logout].code, @"SIGN_OUT_ERROR");
  self.provider.throwOnSignOut = NO; XCTAssertNil([self logout].code);
}
- (void)testTimeoutQuarantinesSDKUntilCallbackReturns {
  self.auth.timeoutInterval = 0.01; GSOutcome *outcome = [self start];
  XCTestExpectation *timeout = [self expectationWithDescription:@"timeout"];
  dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 50 * NSEC_PER_MSEC), dispatch_get_main_queue(), ^{ [timeout fulfill]; });
  [self waitForExpectations:@[timeout] timeout:1];
  XCTAssertEqualObjects(outcome.code, @"TIMEOUT_ERROR"); XCTAssertEqualObjects([self start].code, @"IN_PROGRESS");
  [self success]; XCTAssertEqual(outcome.settles, 1); XCTAssertEqual(self.provider.signOuts, 1);
  outcome = [self start]; [self success]; XCTAssertEqual(outcome.settles, 1);
}
@end
