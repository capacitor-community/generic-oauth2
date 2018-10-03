#ifdef __OBJC__
#import <UIKit/UIKit.h>
#else
#ifndef FOUNDATION_EXPORT
#if defined(__cplusplus)
#define FOUNDATION_EXPORT extern "C"
#else
#define FOUNDATION_EXPORT extern
#endif
#endif
#endif

#import "CDV.h"
#import "CDVAvailability.h"
#import "CDVCommandDelegate.h"
#import "CDVCommandDelegateImpl.h"
#import "CDVConfigParser.h"
#import "CDVInvokedUrlCommand.h"
#import "CDVPlugin.h"
#import "CDVPluginManager.h"
#import "CDVPluginResult.h"
#import "CDVScreenOrientationDelegate.h"
#import "CDVUIWebViewDelegate.h"
#import "CDVUserAgentUtil.h"
#import "CDVViewController.h"

FOUNDATION_EXPORT double CordovaVersionNumber;
FOUNDATION_EXPORT const unsigned char CordovaVersionString[];

