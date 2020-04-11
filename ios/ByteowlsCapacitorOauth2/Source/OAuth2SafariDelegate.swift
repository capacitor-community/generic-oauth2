//
//  OAuth2SafariDelegate.swift
//  ByteowlsCapacitorOauth2
//
//  Created by Michael Oberwasserlechner on 11.04.20.
//  Copyright © 2020 BYTEOWLS Software & Consulting. All rights reserved.
//

import Foundation
import Capacitor
import SafariServices

class OAuth2SafariDelegate: NSObject, SFSafariViewControllerDelegate {
    
    var pluginCall: CAPPluginCall
    
    init(_ call: CAPPluginCall) {
        self.pluginCall = call
    }
    
    func safariViewControllerDidFinish(_ controller: SFSafariViewController) {
        self.pluginCall.reject(OAuth2ClientPlugin.SharedConstants.ERR_USER_CANCELLED)
    }

}
