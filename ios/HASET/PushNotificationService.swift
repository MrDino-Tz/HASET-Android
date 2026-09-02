import Foundation
import FirebaseCore
import FirebaseMessaging
import UIKit

enum PushNotificationService {
    static let fcmTokenDefaultsKey = "fcm_device_token"

    static func configure() {
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        Messaging.messaging().delegate = PushMessagingDelegate.shared
    }

    static func setAPNSToken(_ deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    static func refreshFCMToken() {
        Task {
            do {
                let token = try await Messaging.messaging().token()
                storeFCMToken(token)
            } catch {
                NSLog("FCM token fetch failed: %@", error.localizedDescription)
            }
        }
    }

    static func storeFCMToken(_ token: String) {
        let trimmed = token.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        UserDefaults.standard.set(trimmed, forKey: fcmTokenDefaultsKey)
        NotificationCenter.default.post(name: .hasetFCMTokenUpdated, object: trimmed)
    }

    static func currentFCMToken() -> String? {
        UserDefaults.standard.string(forKey: fcmTokenDefaultsKey)
    }
}

private final class PushMessagingDelegate: NSObject, MessagingDelegate {
    static let shared = PushMessagingDelegate()

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        PushNotificationService.storeFCMToken(fcmToken)
    }
}

extension Notification.Name {
    static let hasetFCMTokenUpdated = Notification.Name("haset.fcm.token.updated")
}
