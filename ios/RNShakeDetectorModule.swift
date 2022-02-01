//
//  RNShakeDetectorModule.swift
//  RNShakeDetectorModule
//
//  Copyright © 2022 Fernando Mumbach. All rights reserved.
//

import Foundation

@objc(RNShakeDetectorModule)
class RNShakeDetectorModule: NSObject {
  @objc
  func constantsToExport() -> [AnyHashable : Any]! {
    return ["count": 1]
  }

  @objc
  static func requiresMainQueueSetup() -> Bool {
    return true
  }
}
