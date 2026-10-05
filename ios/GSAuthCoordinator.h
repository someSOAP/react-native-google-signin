#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN
@protocol GSAuthProvider <NSObject>
- (void)startWithClientID:(NSString *)clientID serverClientID:(NSString *)serverClientID
                   nonce:(nullable NSString *)nonce presenter:(id)presenter
              completion:(void (^)(NSDictionary *_Nullable, NSError *_Nullable))completion;
- (BOOL)signOut:(NSError *_Nullable *_Nullable)error;
@end

/// Main-thread confined request state. No app session or custom credential storage.
@interface GSAuthCoordinator : NSObject
- (instancetype)initWithProvider:(id<GSAuthProvider>)provider info:(NSDictionary *)info
                 fallbackClientID:(nullable NSString *)fallbackClientID;
@property(nonatomic) NSTimeInterval timeoutInterval;
- (void)signIn:(NSDictionary *)config presenter:(nullable id)presenter
      resolve:(void (^)(id _Nullable))resolve reject:(void (^)(NSString *))reject;
- (void)signOutWithResolve:(void (^)(id _Nullable))resolve reject:(void (^)(NSString *))reject;
- (void)invalidate;
@end
NS_ASSUME_NONNULL_END
