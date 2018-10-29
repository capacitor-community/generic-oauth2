//
//  OAuth2CustomHandler.swift
//  ByteowlsCapacitorOauth2
//
//  Created by Michael Oberwasserlechner on 29.10.18.
//  Copyright © 2018 BYTEOWLS Software & Consulting. All rights reserved.
//

import Foundation
import Capacitor

public protocol OAuth2CustomHandler: NSObjectProtocol {
    
    func getAccessToken(viewController: UIViewController, call: CAPPluginCall, callback: () -> Void) -> Void
}
