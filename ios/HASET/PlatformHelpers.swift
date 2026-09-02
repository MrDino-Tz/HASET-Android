import Foundation
import SwiftUI
import UIKit
import UserNotifications

// MARK: - Audit Logger

final class AuditLogger {
    static let shared = AuditLogger()
    private let authService = AuthService()

    private init() {}

    func logAction(
        action: String,
        description: String,
        entityType: String,
        entityId: String?,
        profile: UserProfile?,
        idToken: String?
    ) {
        guard let profile else { return }
        Task {
            try? await authService.writeAuditLog(
                action: action,
                description: description,
                entityType: entityType,
                entityId: entityId,
                profile: profile,
                idToken: idToken
            )
        }
    }

    func logLogin(profile: UserProfile, idToken: String?) {
        logAction(
            action: "LOGIN",
            description: "User logged in successfully",
            entityType: "USER",
            entityId: profile.userId,
            profile: profile,
            idToken: idToken
        )
    }

    func logRegistration(profile: UserProfile, idToken: String?) {
        logAction(
            action: "REGISTER",
            description: "User registered a new account",
            entityType: "USER",
            entityId: profile.userId,
            profile: profile,
            idToken: idToken
        )
    }

    func logAppointmentBooked(profile: UserProfile, appointmentId: String, idToken: String?) {
        logAction(
            action: "BOOK_APPOINTMENT",
            description: "User booked an appointment",
            entityType: "APPOINTMENT",
            entityId: appointmentId,
            profile: profile,
            idToken: idToken
        )
    }

    func logOpenPharmacy(profile: UserProfile?, idToken: String?) {
        logAction(
            action: "OPEN_PHARMACY",
            description: "User opened Pharmacy section",
            entityType: "PHARMACY",
            entityId: nil,
            profile: profile,
            idToken: idToken
        )
    }
}

// MARK: - Appointment Reminders

enum AppointmentReminderService {
    private static let center = UNUserNotificationCenter.current()

    static func scheduleReminders(for appointment: AppointmentSummary, doctorName: String) {
        guard SessionStore().notificationEnabled else { return }
        guard let fireDate = appointmentDateTime(date: appointment.date, time: appointment.time),
              fireDate > Date() else { return }

        let offsets: [(hours: Int, minutes: Int)] = [(24, 0), (2, 0), (0, 30)]
        for offset in offsets {
            let triggerDate = fireDate.addingTimeInterval(-Double(offset.hours * 3600 + offset.minutes * 60))
            guard triggerDate > Date() else { continue }

            let content = UNMutableNotificationContent()
            content.title = "Appointment Reminder"
            if offset.hours >= 24 {
                content.body = "You have an appointment with \(doctorName) tomorrow at \(appointment.time)."
            } else if offset.hours >= 2 {
                content.body = "Your appointment with \(doctorName) is in 2 hours (\(appointment.time))."
            } else {
                content.body = "Your appointment with \(doctorName) starts in 30 minutes."
            }
            content.sound = .default
            content.userInfo = [
                "navigate_to": "appointments",
                "appointment_id": appointment.id
            ]

            let components = Calendar.current.dateComponents(
                [.year, .month, .day, .hour, .minute, .second],
                from: triggerDate
            )
            let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
            let identifier = "appointment-\(appointment.id)-\(offset.hours)-\(offset.minutes)"
            center.add(UNNotificationRequest(identifier: identifier, content: content, trigger: trigger))
        }
    }

    private static func appointmentDateTime(date: String, time: String) -> Date? {
        let formats = ["dd MMM yyyy", "yyyy-MM-dd", "dd/MM/yyyy"]
        let timeFormats = ["hh:mm a", "h:mm a", "HH:mm"]
        for dateFormat in formats {
            for timeFormat in timeFormats {
                let formatter = DateFormatter()
                formatter.locale = Locale(identifier: "en_US_POSIX")
                formatter.dateFormat = "\(dateFormat) \(timeFormat)"
                if let parsed = formatter.date(from: "\(date) \(time)") {
                    return parsed
                }
            }
        }
        return nil
    }
}

// MARK: - Health Tips Scheduler

enum HealthTipsScheduler {
    private static let center = UNUserNotificationCenter.current()
    private static let tipTimes: [(hour: Int, minute: Int, key: String)] = [
        (8, 0, "morning"),
        (12, 0, "midday"),
        (15, 0, "afternoon"),
        (18, 0, "evening"),
        (21, 0, "bedtime")
    ]

    static func rescheduleDailyTips(tips: [HealthTipSummary]) {
        guard SessionStore().notificationEnabled else { return }
        guard !tips.isEmpty else { return }

        center.getPendingNotificationRequests { requests in
            let stale = requests.map(\.identifier).filter { $0.hasPrefix("health-tip-") }
            center.removePendingNotificationRequests(withIdentifiers: stale)

            for slot in tipTimes {
                let tip = tips.randomElement() ?? tips[0]
                let content = UNMutableNotificationContent()
                content.title = "Health Tip"
                content.body = tip.text
                content.sound = .default
                content.userInfo = [
                    "navigate_to": "health_tip",
                    "health_tip_title": "Health Tip",
                    "health_tip_text": tip.text
                ]

                var components = DateComponents()
                components.hour = slot.hour
                components.minute = slot.minute
                let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
                center.add(
                    UNNotificationRequest(
                        identifier: "health-tip-\(slot.key)",
                        content: content,
                        trigger: trigger
                    )
                )
            }
        }
    }

    static func cancelAll() {
        center.getPendingNotificationRequests { requests in
            let ids = requests.map(\.identifier).filter { $0.hasPrefix("health-tip-") }
            center.removePendingNotificationRequests(withIdentifiers: ids)
        }
    }
}

// MARK: - Screenshot Protection

struct ScreenshotProtectionModifier: ViewModifier {
    func body(content: Content) -> some View {
        content.background(ScreenshotProtectionRepresentable())
    }
}

extension View {
    func screenshotProtected() -> some View {
        modifier(ScreenshotProtectionModifier())
    }
}

private struct ScreenshotProtectionRepresentable: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        view.isUserInteractionEnabled = false
        DispatchQueue.main.async {
            guard let host = view.superview?.superview else { return }
            makeSecure(host)
        }
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}

    private func makeSecure(_ view: UIView) {
        let field = UITextField()
        field.isSecureTextEntry = true
        field.isUserInteractionEnabled = false
        view.addSubview(field)
        field.centerYAnchor.constraint(equalTo: view.centerYAnchor).isActive = true
        field.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        view.layer.superlayer?.addSublayer(field.layer)
        field.layer.sublayers?.first?.addSublayer(view.layer)
    }
}

enum PasswordResetLinkParser {
    static func extractOobCode(from input: String) -> String? {
        let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }

        if let range = trimmed.range(of: #"oobCode=([A-Za-z0-9_-]+)"#, options: .regularExpression) {
            let match = String(trimmed[range])
            if let value = match.split(separator: "=").last {
                return String(value)
            }
        }

        if trimmed.range(of: #"^[A-Za-z0-9_-]{20,}$"#, options: .regularExpression) != nil {
            return trimmed
        }
        return nil
    }

    static func isResetPasswordLink(_ url: URL) -> Bool {
        url.host == HASETConstants.firebaseAuthDomain
            && (url.path.contains("/__/auth/action") || url.query?.contains("mode=resetPassword") == true)
    }
}
