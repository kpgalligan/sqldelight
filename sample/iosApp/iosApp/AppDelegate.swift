//
//  AppDelegate.swift
//  iosApp
//
//  Copyright © 2019 Square, Inc.. All rights reserved.
//

import UIKit
import main

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let db = Db()
        db.defaultDriver()
        db.myQuery()

        return true
    }
}

