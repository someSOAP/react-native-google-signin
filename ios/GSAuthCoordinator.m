#import "GSAuthCoordinator.h"

@interface GSAuthRequest : NSObject
@property(nonatomic, copy) void (^resolve)(id);
@property(nonatomic, copy) void (^reject)(NSString *);
@property(nonatomic, strong) NSTimer *timer;
@property(nonatomic) BOOL providerCompleted;
@end
@implementation GSAuthRequest
@end

@interface GSAuthCoordinator ()
@property(nonatomic, strong) id<GSAuthProvider> provider;
@property(nonatomic, copy) NSDictionary *info;
@property(nonatomic, copy) NSString *fallbackClientID;
@property(nonatomic, strong) GSAuthRequest *pending;
@property(nonatomic) BOOL invalidated;
@property(nonatomic) BOOL providerBusy;
- (void)discardProviderResult;
@end

@implementation GSAuthCoordinator
- (instancetype)initWithProvider:(id<GSAuthProvider>)provider info:(NSDictionary *)info
                fallbackClientID:(NSString *)fallbackClientID {
  if ((self = [super init])) {
    _provider = provider; _info = [info copy]; _fallbackClientID = [fallbackClientID copy];
    _timeoutInterval = 300;
  }
  return self;
}

static BOOL GSClientID(id value) {
  if (![value isKindOfClass:NSString.class]) return NO;
  return [value rangeOfString:@"^[A-Za-z0-9_-]+\\.apps\\.googleusercontent\\.com$"
                     options:NSRegularExpressionSearch].location != NSNotFound;
}

static BOOL GSToken(id token) {
  if (![token isKindOfClass:NSString.class]) return NO;
  NSArray *parts = [token componentsSeparatedByString:@"."];
  if (parts.count != 3) return NO;
  for (NSString *part in parts) {
    if ([part rangeOfString:@"^[A-Za-z0-9_-]+$" options:NSRegularExpressionSearch].location == NSNotFound) return NO;
  }
  for (NSUInteger i = 0; i < 2; i++) {
    NSString *part = [parts[i] stringByReplacingOccurrencesOfString:@"-" withString:@"+"];
    part = [part stringByReplacingOccurrencesOfString:@"_" withString:@"/"];
    while (part.length % 4) part = [part stringByAppendingString:@"="];
    NSData *data = [[NSData alloc] initWithBase64EncodedString:part options:0];
    id object = data ? [NSJSONSerialization JSONObjectWithData:data options:0 error:nil] : nil;
    if (![object isKindOfClass:NSDictionary.class] || [object count] == 0) return NO;
  }
  return YES; // Only structural screening; the consuming backend verifies authenticity.
}

- (GSAuthRequest *)begin:(void (^)(id))resolve reject:(void (^)(NSString *))reject {
  if (self.invalidated) { reject(@"MODULE_DESTROYED"); return nil; }
  if (self.pending || self.providerBusy) { reject(@"IN_PROGRESS"); return nil; }
  GSAuthRequest *request = [GSAuthRequest new]; request.resolve = resolve; request.reject = reject;
  self.pending = request;
  return request;
}

- (void)finish:(GSAuthRequest *)request result:(id)result code:(NSString *)code {
  if (self.pending != request) return;
  self.pending = nil;
  [request.timer invalidate]; request.timer = nil;
  void (^resolve)(id) = request.resolve; void (^reject)(NSString *) = request.reject;
  request.resolve = nil; request.reject = nil;
  if (code) reject(code); else resolve(result);
}

- (void)signIn:(NSDictionary *)config presenter:(id)presenter
      resolve:(void (^)(id))resolve reject:(void (^)(NSString *))reject {
  NSAssert(NSThread.isMainThread, @"Authentication must run on the main thread");
  GSAuthRequest *request = [self begin:resolve reject:reject]; if (!request) return;
  id clientID = config[@"iosClientId"] ?: self.info[@"GIDClientID"] ?: self.fallbackClientID;
  id serverID = config[@"serverClientId"]; id nonce = config[@"nonce"];
  if (!GSClientID(clientID) || !GSClientID(serverID) ||
      (nonce && (![nonce isKindOfClass:NSString.class] ||
                 ![nonce stringByTrimmingCharactersInSet:NSCharacterSet.whitespaceAndNewlineCharacterSet].length))) {
    [self finish:request result:nil code:@"CONFIGURATION_ERROR"]; return;
  }
  NSString *scheme = [[[clientID componentsSeparatedByString:@"."] reverseObjectEnumerator].allObjects componentsJoinedByString:@"."];
  BOOL hasScheme = NO;
  id types = self.info[@"CFBundleURLTypes"];
  if ([types isKindOfClass:NSArray.class]) for (id type in types) {
    if (![type isKindOfClass:NSDictionary.class]) continue;
    id schemes = type[@"CFBundleURLSchemes"];
    if ([schemes isKindOfClass:NSArray.class] && [schemes containsObject:scheme]) hasScheme = YES;
  }
  if (!hasScheme) { [self finish:request result:nil code:@"CONFIGURATION_ERROR"]; return; }
  if (!presenter) { [self finish:request result:nil code:@"ERR_ACTIVITY"]; return; }
  self.providerBusy = YES;
  __weak GSAuthCoordinator *weakSelf = self;
  request.timer = [NSTimer scheduledTimerWithTimeInterval:self.timeoutInterval repeats:NO block:^(NSTimer *timer) {
    // GIDSignIn has no public cancel API. Quarantine the SDK until its callback drains.
    [weakSelf finish:request result:nil code:@"TIMEOUT_ERROR"];
  }];
  @try {
    [self.provider startWithClientID:clientID serverClientID:serverID nonce:nonce presenter:presenter
                        completion:^(NSDictionary *result, NSError *error) {
      // An SDK fake or future implementation can deliver off-main or duplicate callbacks.
      dispatch_async(dispatch_get_main_queue(), ^{
        GSAuthCoordinator *coordinator = self;
        if (request.providerCompleted) return;
        request.providerCompleted = YES;
        GSAuthCoordinator *self = coordinator;
        self.providerBusy = NO;
        if (self.invalidated) {
          if (result) [self discardProviderResult];
          return;
        }
        if (self.pending != request) {
          if (result) [self discardProviderResult];
          return;
        }
        if (error) {
          NSString *code = @"GET_CREDENTIALS_ERROR";
          if ([error.domain isEqualToString:@"com.google.GIDSignIn"] && error.code == -5) code = @"CANCELLATION_ERROR";
          else if ([error.domain isEqualToString:NSURLErrorDomain]) code = @"NETWORK_ERROR";
          else if ([error.domain isEqualToString:@"GSAuthProviderBusy"]) code = @"IN_PROGRESS";
          [self finish:request result:nil code:code]; return;
        }
        if (!GSToken(result[@"idToken"])) { [self finish:request result:nil code:@"INVALID_TOKEN_ERROR"]; return; }
        NSMutableDictionary *response = [@{@"idToken": result[@"idToken"]} mutableCopy];
        for (NSString *key in @[@"givenName", @"familyName", @"email", @"profilePictureUri"]) {
          if ([result[key] isKindOfClass:NSString.class]) response[key] = result[key];
        }
        [self finish:request result:response code:nil];
      });
    }];
  } @catch (NSException *exception) {
    request.providerCompleted = YES;
    self.providerBusy = NO;
    [self finish:request result:nil code:@"GET_CREDENTIALS_ERROR"];
  }
}

- (void)signOutWithResolve:(void (^)(id))resolve reject:(void (^)(NSString *))reject {
  NSAssert(NSThread.isMainThread, @"Authentication must run on the main thread");
  GSAuthRequest *request = [self begin:resolve reject:reject]; if (!request) return;
  @try {
    NSError *error = nil;
    BOOL success = [self.provider signOut:&error];
    [self finish:request result:nil code:success ? nil : @"SIGN_OUT_ERROR"];
  } @catch (NSException *exception) { [self finish:request result:nil code:@"SIGN_OUT_ERROR"]; }
}
- (void)discardProviderResult {
  // The original promise has already settled. Cleanup failures must not crash the host.
  @try { [self.provider signOut:nil]; } @catch (NSException *exception) { }
}
- (void)invalidate {
  self.invalidated = YES;
  if (self.pending) [self finish:self.pending result:nil code:@"MODULE_DESTROYED"];
}
@end
