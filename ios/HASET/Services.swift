import CoreLocation
import Foundation
import Security
import UserNotifications

enum HASETConstants {
    static let firebaseAPIKey = "AIzaSyB6XncMhXdlT0fScdU6Fq7Nw_toPmf-tRU"
    static let firebaseDatabaseURL = "https://hasetapp-4eeba-default-rtdb.europe-west1.firebasedatabase.app"
    static let productionAPIURL = "https://payments.hasethospital.or.tz/public/api/"
    static let developmentAPIURL = "http://192.168.1.126:8000/api/"
    static let privacyPolicyURL = "https://hasethospital.or.tz/legal/privacy-policy"
    static let termsURL = "https://hasethospital.or.tz/legal/terms"
    static let supportURL = "https://hasethospital.or.tz/contact"
    static let appConfigPath = "app_config"
}

final class SessionStore {
    private let defaults = UserDefaults.standard
    private let onboardingSeenKey = "onboarding_seen"
    private let languageKey = "language"
    private let sessionKey = "ios_port_session"
    private let notificationEnabledKey = "notification_enabled"
    private let themeKey = "app_theme"
    private let locationEnabledKey = "location_enabled"
    private let keychain = KeychainSessionStore()

    var onboardingSeen: Bool {
        get { defaults.bool(forKey: onboardingSeenKey) }
        set { defaults.set(newValue, forKey: onboardingSeenKey) }
    }

    var languageCode: String {
        get { defaults.string(forKey: languageKey) ?? "en" }
        set { defaults.set(newValue, forKey: languageKey) }
    }

    var notificationEnabled: Bool {
        get { defaults.object(forKey: notificationEnabledKey) as? Bool ?? true }
        set { defaults.set(newValue, forKey: notificationEnabledKey) }
    }

    var themeMode: ThemeMode {
        get {
            guard let rawValue = defaults.string(forKey: themeKey), let value = ThemeMode(rawValue: rawValue) else {
                return .system
            }
            return value
        }
        set { defaults.set(newValue.rawValue, forKey: themeKey) }
    }

    var locationEnabled: Bool {
        get { defaults.object(forKey: locationEnabledKey) as? Bool ?? false }
        set { defaults.set(newValue, forKey: locationEnabledKey) }
    }

    func saveSession(_ session: StoredSession) {
        keychain.save(session, for: sessionKey)
    }

    func loadSession() -> StoredSession? {
        keychain.load(StoredSession.self, for: sessionKey)
    }

    func clearSession() {
        keychain.delete(for: sessionKey)
    }
}

private final class KeychainSessionStore {
    func save<T: Codable>(_ value: T, for key: String) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        let query = baseQuery(for: key)
        SecItemDelete(query as CFDictionary)
        let attributes = query.merging([kSecValueData as String: data]) { current, _ in current }
        SecItemAdd(attributes as CFDictionary, nil)
    }

    func load<T: Codable>(_ type: T.Type, for key: String) -> T? {
        var query = baseQuery(for: key)
        query[kSecReturnData as String] = kCFBooleanTrue
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var item: AnyObject?
        guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess else { return nil }
        guard let data = item as? Data else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }

    func delete(for key: String) {
        SecItemDelete(baseQuery(for: key) as CFDictionary)
    }

    private func baseQuery(for key: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "com.haset.hasetapp.session",
            kSecAttrAccount as String: key
        ]
    }
}

enum ValidationService {
    static func isValidEmail(_ email: String) -> Bool {
        let regex = #"^[A-Z0-9a-z._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$"#
        return email.range(of: regex, options: .regularExpression) != nil
    }

    static func isValidPassword(_ password: String) -> Bool {
        password.count >= 6
    }

    static func isValidPhone(_ phone: String) -> Bool {
        phone.range(of: #"^\+[0-9]{12,}$"#, options: .regularExpression) != nil
    }

    static func isValidName(_ name: String) -> Bool {
        name.trimmingCharacters(in: .whitespacesAndNewlines).count >= 2
    }
}

enum StaticContentService {
    static let specialties = [
        "General Physician",
        "Cardiologist",
        "Dermatologist",
        "Pediatrician",
        "Orthopedic",
        "Neurologist",
        "Psychiatrist",
        "Gynecologist",
        "Dentist",
        "ENT Specialist"
    ]

    static let timeSlots = [
        "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM",
        "11:00 AM", "11:30 AM", "12:00 PM", "12:30 PM",
        "02:00 PM", "02:30 PM", "03:00 PM", "03:30 PM",
        "04:00 PM", "04:30 PM", "05:00 PM", "05:30 PM",
        "06:00 PM", "06:30 PM", "07:00 PM", "07:30 PM"
    ]

    static let doctors: [DoctorSummary] = [
        DoctorSummary(id: "doc-1", name: "Dr. Asha Salim", specialty: "General Physician", hospital: "HASET Hospital", phoneNumber: "+255712345678", email: "asha.salim@hasetapp.com", address: "HASET Hospital", bio: "Experienced physician providing patient-centered general care.", rating: 4.9, experienceYears: 8, verified: true, consultationFee: "TZS 20,000", availableToday: true, profileImage: nil, availableTimes: Array(timeSlots.prefix(4))),
        DoctorSummary(id: "doc-2", name: "Dr. Joseph Mrema", specialty: "Cardiologist", hospital: "Aga Khan Clinic", phoneNumber: "+255754987654", email: "joseph.mrema@hasetapp.com", address: "Aga Khan Clinic", bio: "Cardiology specialist focused on preventive heart health and ongoing treatment plans.", rating: 4.8, experienceYears: 11, verified: true, consultationFee: "TZS 35,000", availableToday: true, profileImage: nil, availableTimes: Array(timeSlots.prefix(4))),
        DoctorSummary(id: "doc-3", name: "Dr. Neema Kweka", specialty: "Dermatologist", hospital: "Muhimbili", phoneNumber: "+255713333222", email: "neema.kweka@hasetapp.com", address: "Muhimbili", bio: "Dermatology expert helping patients manage skin, hair, and nail conditions.", rating: 4.7, experienceYears: 6, verified: false, consultationFee: "TZS 25,000", availableToday: false, profileImage: nil, availableTimes: Array(timeSlots.prefix(4)))
    ]

    static let homeHighlights: [HomeHighlight] = [
        HomeHighlight(
            id: "hero-1",
            titleLine1: "Up to",
            titleLine2: "50% OFF",
            badge: "Flash Sale",
            buttonText: "Shop Now",
            imageName: "OnboardingOne",
            imageURL: nil,
            bannerType: "PHARMACY",
            targetAction: nil
        ),
        HomeHighlight(
            id: "hero-2",
            titleLine1: "Online",
            titleLine2: "Consultation",
            badge: "Live Now",
            buttonText: "Chat Now",
            imageName: "OnboardingTwo",
            imageURL: nil,
            bannerType: "MESSAGING",
            targetAction: nil
        ),
        HomeHighlight(
            id: "hero-3",
            titleLine1: "Book Expert",
            titleLine2: "Care Today",
            badge: "Verified",
            buttonText: "Book Now",
            imageName: "OnboardingThree",
            imageURL: nil,
            bannerType: "APPOINTMENT",
            targetAction: nil
        )
    ]

    static let articles: [ArticleSummary] = [
        ArticleSummary(
            id: "art-1",
            title: "Understanding Blood Pressure",
            author: "HASET Cardiac Team",
            authorImage: nil,
            category: "Heart Health",
            excerpt: "Simple daily steps that help you monitor and improve your blood pressure before complications begin.",
            imageName: "OnboardingOne",
            imageURL: nil,
            timestamp: 1_718_982_000_000,
            readTime: "4 min",
            content: [
                "Blood pressure is one of the most important signs of your heart health. When it stays too high for a long time, it can quietly damage the heart, brain, and kidneys.",
                "Adults should check their blood pressure regularly, especially if they have headaches, dizziness, chest discomfort, diabetes, or a family history of hypertension.",
                "Small habits make a real difference. Reduce salt, avoid smoking, walk often, drink enough water, and take prescribed medicines consistently. If readings stay high, book a doctor review early."
            ],
            viewCount: 1250,
            likeCount: 138,
            commentCount: 22,
            shareCount: 17,
            type: "image"
        ),
        ArticleSummary(
            id: "art-2",
            title: "Healthy Nutrition for Families",
            author: "Dr. Neema Kweka",
            authorImage: nil,
            category: "Nutrition",
            excerpt: "A practical guide to affordable meals and balanced habits for every home, including children and older adults.",
            imageName: "OnboardingTwo",
            imageURL: nil,
            timestamp: 1_718_881_000_000,
            readTime: "5 min",
            content: [
                "Healthy nutrition does not need to be expensive. A strong family meal plan can be built from vegetables, fruits, beans, whole grains, eggs, fish, and other local foods.",
                "Try to balance each plate with energy foods, body-building foods, and protective foods. Limit sugary drinks and processed snacks when possible.",
                "Children, pregnant women, and older adults need special attention. Eating at regular times and choosing fresh ingredients can improve energy, immunity, and long-term health."
            ],
            viewCount: 980,
            likeCount: 96,
            commentCount: 15,
            shareCount: 11,
            type: "image"
        ),
        ArticleSummary(
            id: "art-3",
            title: "When to Book a Doctor Online",
            author: "HASET Telehealth Desk",
            authorImage: nil,
            category: "Telehealth",
            excerpt: "Recognize the right moment to move from self-care to a medical appointment instead of waiting too long.",
            imageName: "OnboardingThree",
            imageURL: nil,
            timestamp: 1_718_780_000_000,
            readTime: "3 min",
            content: [
                "Online doctor booking helps when symptoms are continuing, medicine is not helping, or you need professional guidance quickly without delays.",
                "You should not wait if you have repeated fever, breathing problems, severe pain, persistent vomiting, worsening skin reactions, or uncontrolled blood sugar or pressure.",
                "HASET makes it easier to review available doctors, schedule a visit, and keep follow-up care organized. Early action often prevents bigger medical problems later."
            ],
            viewCount: 820,
            likeCount: 84,
            commentCount: 12,
            shareCount: 9,
            type: "image"
        ),
        ArticleSummary(
            id: "art-4",
            title: "Daily Habits for Diabetes Prevention",
            author: "Community Wellness Unit",
            authorImage: nil,
            category: "Prevention",
            excerpt: "Simple lifestyle choices can lower diabetes risk and help families stay active, informed, and healthier over time.",
            imageName: nil,
            imageURL: nil,
            timestamp: 1_718_650_000_000,
            readTime: "4 min",
            content: [
                "Diabetes risk increases with inactivity, weight gain, unhealthy diet, and family history, but prevention usually starts with ordinary daily decisions.",
                "Aim for consistent movement, smaller sugar portions, more vegetables, and regular health checks if you notice unusual thirst, frequent urination, or fatigue.",
                "Preventive care works best when families do it together. Shared walks, healthier cooking, and routine screening can help catch problems early."
            ],
            viewCount: 640,
            likeCount: 70,
            commentCount: 8,
            shareCount: 6,
            type: "text"
        )
    ]

    static let patientAppointments: [AppointmentSummary] = [
        AppointmentSummary(id: "appt-1", title: "Dr. Asha Salim", subtitle: "General Physician", dateText: "Today, 10:30 AM", status: .approved),
        AppointmentSummary(id: "appt-2", title: "Dr. Joseph Mrema", subtitle: "Cardiology Follow-up", dateText: "Wed, 12:00 PM", status: .pending),
        AppointmentSummary(id: "appt-3", title: "Dr. Neema Kweka", subtitle: "Skin Consultation", dateText: "Fri, 3:00 PM", status: .cancelled)
    ]

    static let doctorAppointments: [AppointmentSummary] = [
        AppointmentSummary(id: "d-appt-1", title: "Anna Mgosi", subtitle: "General checkup", dateText: "Today, 9:00 AM", status: .pending),
        AppointmentSummary(id: "d-appt-2", title: "Thomas Peter", subtitle: "Follow-up review", dateText: "Today, 11:30 AM", status: .approved),
        AppointmentSummary(id: "d-appt-3", title: "Fatma Suleiman", subtitle: "Prescription update", dateText: "Yesterday, 4:00 PM", status: .completed)
    ]

    static let adminMetrics: [AdminMetric] = [
        AdminMetric(id: "m-1", title: "Users", value: "1,248"),
        AdminMetric(id: "m-2", title: "Doctors", value: "86"),
        AdminMetric(id: "m-3", title: "Pending", value: "14"),
        AdminMetric(id: "m-4", title: "Reports", value: "7")
    ]

    static let conversations: [UserRole: [ConversationSummary]] = [
        .patient: [
            ConversationSummary(id: "c-1", name: "Dr. Asha Salim", lastMessage: "Please take the medicine after food.", lastMessageTimestamp: 1_718_982_000_000, unreadCount: 2, isOnline: true, archived: false, profileImage: nil),
            ConversationSummary(id: "c-2", name: "HASET Admin", lastMessage: "Your account was updated successfully.", lastMessageTimestamp: 1_718_881_000_000, unreadCount: 0, isOnline: false, archived: true, profileImage: nil)
        ],
        .doctor: [
            ConversationSummary(id: "c-3", name: "Anna Mgosi", lastMessage: "Can I reschedule for tomorrow?", lastMessageTimestamp: 1_718_982_000_000, unreadCount: 1, isOnline: true, archived: false, profileImage: nil),
            ConversationSummary(id: "c-4", name: "Thomas Peter", lastMessage: "Thank you doctor.", lastMessageTimestamp: 1_718_881_000_000, unreadCount: 0, isOnline: false, archived: true, profileImage: nil)
        ],
        .admin: [
            ConversationSummary(id: "c-5", name: "Support Ticket #148", lastMessage: "Waiting for admin review", lastMessageTimestamp: 1_718_982_000_000, unreadCount: 3, isOnline: true, archived: false, profileImage: nil),
            ConversationSummary(id: "c-6", name: "Doctor Approval", lastMessage: "Registration documents uploaded", lastMessageTimestamp: 1_718_881_000_000, unreadCount: 1, isOnline: false, archived: true, profileImage: nil)
        ]
    ]

    static let hospitals: [HospitalSummary] = [
        HospitalSummary(id: "h-1", name: "HASET Hospital", location: "Dar es Salaam", distance: "1.2 km"),
        HospitalSummary(id: "h-2", name: "Muhimbili National Hospital", location: "Upanga", distance: "4.6 km"),
        HospitalSummary(id: "h-3", name: "Aga Khan Hospital", location: "Ocean Road", distance: "6.3 km")
    ]

    static let pharmacyCategories: [PharmacyCategory] = [
        PharmacyCategory(id: "p-1", title: "Supplements", subtitle: "Vitamins and wellness"),
        PharmacyCategory(id: "p-2", title: "Personal Care", subtitle: "Daily care essentials"),
        PharmacyCategory(id: "p-3", title: "Children's Corner", subtitle: "Family health support"),
        PharmacyCategory(id: "p-4", title: "Health Devices", subtitle: "Monitors and accessories")
    ]

    static let recentNotifications = [
        "Appointment booked successfully",
        "Registration successful!",
        "Article liked",
        "Your profile was updated"
    ]

    static let supportPhone = "+255754501671"
    static let supportWhatsAppURL = "https://api.whatsapp.com/send?phone=255754501671"
}

final class PermissionService: NSObject, CLLocationManagerDelegate {
    static let shared = PermissionService()

    private let locationManager = CLLocationManager()
    private var locationContinuation: CheckedContinuation<Bool, Never>?

    override private init() {
        super.init()
        locationManager.delegate = self
    }

    @MainActor
    func requestNotifications() async -> Bool {
        let center = UNUserNotificationCenter.current()
        if let settings = await notificationSettings(), settings.authorizationStatus == .authorized {
            return true
        }
        return (try? await center.requestAuthorization(options: [.alert, .badge, .sound])) ?? false
    }

    @MainActor
    func currentNotificationEnabled() async -> Bool {
        guard let settings = await notificationSettings() else { return false }
        return settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
    }

    @MainActor
    func requestLocation() async -> Bool {
        let status = locationManager.authorizationStatus
        switch status {
        case .authorizedAlways, .authorizedWhenInUse:
            return true
        case .denied, .restricted:
            return false
        case .notDetermined:
            return await withCheckedContinuation { continuation in
                locationContinuation = continuation
                locationManager.requestWhenInUseAuthorization()
            }
        @unknown default:
            return false
        }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let granted = manager.authorizationStatus == .authorizedAlways || manager.authorizationStatus == .authorizedWhenInUse
        Task { @MainActor in
            guard let continuation = self.locationContinuation else { return }
            self.locationContinuation = nil
            continuation.resume(returning: granted)
        }
    }

    @MainActor
    private func notificationSettings() async -> UNNotificationSettings? {
        await withCheckedContinuation { continuation in
            UNUserNotificationCenter.current().getNotificationSettings { settings in
                continuation.resume(returning: settings)
            }
        }
    }
}

enum ServiceError: LocalizedError {
    case invalidResponse
    case message(String)

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Something went wrong"
        case .message(let message):
            return message
        }
    }
}

final class AuthService {
    private struct IdentityResponse: Decodable {
        let localId: String
        let idToken: String
        let refreshToken: String?
        let email: String
    }

    private struct RefreshTokenResponse: Decodable {
        let idToken: String
        let refreshToken: String
        let userId: String

        enum CodingKeys: String, CodingKey {
            case idToken = "id_token"
            case refreshToken = "refresh_token"
            case userId = "user_id"
        }
    }

    private struct IdentityErrorEnvelope: Decodable {
        struct IdentityError: Decodable {
            let message: String
        }
        let error: IdentityError
    }

    private let decoder = JSONDecoder()

    func fetchAppConfig() async throws -> RemoteAppConfig? {
        let url = try databaseURL(path: HASETConstants.appConfigPath, authToken: nil)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        if data == Data("null".utf8) {
            return nil
        }
        return try decoder.decode(RemoteAppConfig.self, from: data)
    }

    func signIn(email: String, password: String) async throws -> (StoredSession, UserProfile) {
        let identity = try await performIdentityRequest(
            path: "accounts:signInWithPassword",
            payload: ["email": email, "password": password, "returnSecureToken": true]
        )
        let profile = try await fetchUserProfile(
            userId: identity.localId,
            idToken: identity.idToken,
            fallbackEmail: identity.email,
            fallbackName: nil,
            fallbackPhone: nil
        )
        let session = StoredSession(
            userId: identity.localId,
            idToken: identity.idToken,
            refreshToken: identity.refreshToken,
            role: profile.role,
            userName: profile.fullName,
            email: profile.email,
            phone: profile.phone
        )
        return (session, profile)
    }

    func register(email: String, password: String, fullName: String, phone: String, role: UserRole, regNo: String?) async throws -> (StoredSession, UserProfile) {
        let identity = try await performIdentityRequest(
            path: "accounts:signUp",
            payload: ["email": email, "password": password, "returnSecureToken": true]
        )

        let profile = UserProfile(
            userId: identity.localId,
            email: email,
            fullName: fullName,
            phone: phone,
            role: role,
            profileImage: "",
            createdAt: Date().timeIntervalSince1970 * 1000,
            regNo: regNo,
            gender: nil,
            age: nil,
            location: nil,
            bio: nil,
            specialization: role == .doctor ? StaticContentService.specialties.first : nil,
            consultationFee: role == .doctor ? "TZS 20,000" : nil,
            availableTimes: role == .doctor ? Array(StaticContentService.timeSlots.prefix(4)) : nil,
            verified: role == .doctor ? false : nil
        )

        try await saveUserProfile(profile, idToken: identity.idToken)
        if role == .doctor {
            try await saveDoctorBootstrap(profile: profile, idToken: identity.idToken)
        }

        let session = StoredSession(
            userId: identity.localId,
            idToken: identity.idToken,
            refreshToken: identity.refreshToken,
            role: role,
            userName: fullName,
            email: email,
            phone: phone
        )
        return (session, profile)
    }

    func refreshSessionIfNeeded(_ session: StoredSession) async throws -> StoredSession {
        guard let refreshToken = session.refreshToken, !refreshToken.isEmpty else {
            return session
        }

        let url = URL(string: "https://securetoken.googleapis.com/v1/token?key=\(HASETConstants.firebaseAPIKey)")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        let body = "grant_type=refresh_token&refresh_token=\(refreshToken)"
        request.httpBody = body.data(using: .utf8)

        let (data, response) = try await URLSession.shared.data(for: request)
        if let httpResponse = response as? HTTPURLResponse, !(200 ... 299).contains(httpResponse.statusCode) {
            return session
        }

        let refreshed = try decoder.decode(RefreshTokenResponse.self, from: data)
        return StoredSession(
            userId: refreshed.userId,
            idToken: refreshed.idToken,
            refreshToken: refreshed.refreshToken,
            role: session.role,
            userName: session.userName,
            email: session.email,
            phone: session.phone
        )
    }

    func sendPasswordReset(email: String) async throws {
        _ = try await performIdentityRequest(
            path: "accounts:sendOobCode",
            payload: ["requestType": "PASSWORD_RESET", "email": email]
        )
    }

    func restoreProfile(session: StoredSession) async throws -> UserProfile {
        try await fetchUserProfile(
            userId: session.userId,
            idToken: session.idToken,
            fallbackEmail: session.email,
            fallbackName: session.userName,
            fallbackPhone: session.phone
        )
    }

    func updateUserProfile(_ profile: UserProfile, idToken: String) async throws {
        try await saveUserProfile(profile, idToken: idToken)
    }

    func fetchPopularArticles(idToken: String?) async throws -> [ArticleSummary] {
        let url = try databaseURL(path: "article_posts", authToken: idToken)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        guard data != Data("null".utf8) else {
            return []
        }
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ServiceError.invalidResponse
        }

        let articles = json.compactMap { key, value -> ArticleSummary? in
            guard let item = value as? [String: Any] else { return nil }
            let status = stringValue(item["status"])?.lowercased()
            if let status, status != "published" {
                return nil
            }

            let title = stringValue(item["title"])?.nonEmpty ?? ""
            let description = stringValue(item["description"])?.nonEmpty ?? ""
            let imageURL = stringValue(item["imageUrl"])?.nonEmpty ?? stringValue(item["imagePath"])?.nonEmpty
            if title.isEmpty && description.isEmpty && imageURL == nil {
                return nil
            }
            let views = intValue(item["views"]) ?? 0
            let type = stringValue(item["type"])?.lowercased() ?? (imageURL == nil ? "text" : "image")
            let author = stringValue(item["profileName"])?.nonEmpty ?? "HASET"
            let tags = stringValue(item["tags"])?.nonEmpty ?? ""
            let content = description.isEmpty ? [title] : [description]

            return ArticleSummary(
                id: stringValue(item["postId"])?.nonEmpty ?? key,
                title: title,
                author: author,
                authorImage: stringValue(item["profileImage"])?.nonEmpty,
                category: tags.isEmpty ? "Article" : tags,
                excerpt: description,
                imageName: nil,
                imageURL: imageURL,
                timestamp: timeIntervalValue(item["createdAt"]) ?? timeIntervalValue(item["timestamp"]) ?? 0,
                readTime: estimatedReadTime(from: description),
                content: content,
                viewCount: views,
                likeCount: intValue(item["likes"]) ?? 0,
                commentCount: intValue(item["comments"]) ?? 0,
                shareCount: intValue(item["shares"]) ?? 0,
                type: type
            )
        }

        return articles
            .sorted { lhs, rhs in
                let leftTime = lhs.timestamp
                let rightTime = rhs.timestamp
                if leftTime == rightTime {
                    return lhs.viewCount > rhs.viewCount
                }
                return leftTime > rightTime
            }
    }

    func fetchSavedArticleIDs(userId: String, idToken: String?) async throws -> Set<String> {
        let url = try databaseURL(path: "saved_articles/\(userId)", authToken: idToken)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        guard data != Data("null".utf8) else { return [] }
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        return Set(json.keys)
    }

    func isArticleLiked(postId: String, userId: String, idToken: String?) async throws -> Bool {
        let url = try databaseURL(path: "post_likes/\(postId)/\(userId)", authToken: idToken)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        guard data != Data("null".utf8) else { return false }
        if let boolValue = try? JSONDecoder().decode(Bool.self, from: data) {
            return boolValue
        }
        return false
    }

    func toggleSavedArticle(postId: String, userId: String, idToken: String?) async throws -> Bool {
        let path = "saved_articles/\(userId)/\(postId)"
        let url = try databaseURL(path: path, authToken: idToken)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)

        if data == Data("null".utf8) {
            try await put(Int(Date().timeIntervalSince1970 * 1000), url: url)
            return true
        }

        try await delete(url: url)
        return false
    }

    func toggleArticleLike(postId: String, userId: String, idToken: String?) async throws -> Bool {
        let likeURL = try databaseURL(path: "post_likes/\(postId)/\(userId)", authToken: idToken)
        let countURL = try databaseURL(path: "article_posts/\(postId)/likes", authToken: idToken)

        let (likeData, likeResponse) = try await URLSession.shared.data(from: likeURL)
        try validate(response: likeResponse, data: likeData)
        let currentlyLiked = likeData != Data("null".utf8)

        let (countData, countResponse) = try await URLSession.shared.data(from: countURL)
        try validate(response: countResponse, data: countData)
        let currentCount = (try? JSONDecoder().decode(Int.self, from: countData)) ?? 0

        if currentlyLiked {
            try await delete(url: likeURL)
            try await put(max(0, currentCount - 1), url: countURL)
            return false
        } else {
            try await put(true, url: likeURL)
            try await put(currentCount + 1, url: countURL)
            return true
        }
    }

    func incrementArticleShares(postId: String, idToken: String?) async throws -> Int {
        try await incrementCounter(path: "article_posts/\(postId)/shares", idToken: idToken)
    }

    func incrementArticleViews(postId: String, idToken: String?) async throws -> Int {
        try await incrementCounter(path: "article_posts/\(postId)/views", idToken: idToken)
    }

    func fetchArticleComments(postId: String, idToken: String?) async throws -> [ArticleComment] {
        let url = try databaseURL(path: "post_comments/\(postId)", authToken: idToken)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        guard data != Data("null".utf8) else { return [] }
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }

        return json.compactMap { key, value in
            guard let item = value as? [String: Any] else { return nil }
            return ArticleComment(
                id: key,
                userId: stringValue(item["userId"]) ?? "",
                userName: stringValue(item["userName"])?.nonEmpty ?? "HASET User",
                userImage: stringValue(item["userImage"])?.nonEmpty,
                text: stringValue(item["commentText"])?.nonEmpty ??
                    stringValue(item["comment"])?.nonEmpty ??
                    stringValue(item["text"])?.nonEmpty ?? "",
                timestamp: timeIntervalValue(item["timestamp"]) ?? 0
            )
        }
        .sorted { $0.timestamp < $1.timestamp }
    }

    func addArticleComment(postId: String, user: UserProfile, text: String, idToken: String?) async throws -> ArticleComment {
        let commentId = UUID().uuidString
        let timestamp = Date().timeIntervalSince1970 * 1000
        let payload: [String: Any] = [
            "commentId": commentId,
            "userId": user.userId,
            "userName": user.fullName,
            "userImage": user.profileImage,
            "commentText": text,
            "timestamp": Int(timestamp)
        ]
        let commentURL = try databaseURL(path: "post_comments/\(postId)/\(commentId)", authToken: idToken)
        try await put(payload, url: commentURL)
        _ = try await incrementCounter(path: "article_posts/\(postId)/comments", idToken: idToken)
        return ArticleComment(
            id: commentId,
            userId: user.userId,
            userName: user.fullName,
            userImage: user.profileImage.isEmpty ? nil : user.profileImage,
            text: text,
            timestamp: timestamp
        )
    }

    func fetchPromotionalBanners(idToken: String?) async throws -> [HomeHighlight] {
        let url = try databaseURL(path: "promotional_banners", authToken: idToken)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        guard data != Data("null".utf8) else {
            return []
        }
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ServiceError.invalidResponse
        }

        return json.compactMap { key, value -> HomeHighlight? in
            guard let item = value as? [String: Any] else { return nil }
            return HomeHighlight(
                id: key,
                titleLine1: stringValue(item["titleLine1"]) ?? "",
                titleLine2: stringValue(item["titleLine2"]) ?? "",
                badge: stringValue(item["discount"]) ?? "",
                buttonText: stringValue(item["buttonText"]) ?? "",
                imageName: nil,
                imageURL: stringValue(item["imageUrl"])?.nonEmpty,
                bannerType: stringValue(item["bannerType"]) ?? "IMAGE_BANNER",
                targetAction: stringValue(item["targetAction"])
            )
        }
    }

    func fetchDoctors(idToken: String?) async throws -> [DoctorSummary] {
        let usersURL = try databaseURL(path: "users", authToken: idToken)
        let doctorsURL = try databaseURL(path: "doctors", authToken: idToken)

        async let usersResponse = URLSession.shared.data(from: usersURL)
        async let doctorsResponse = URLSession.shared.data(from: doctorsURL)
        let ((usersData, usersRawResponse), (doctorsData, doctorsRawResponse)) = try await (usersResponse, doctorsResponse)

        try validate(response: usersRawResponse, data: usersData)
        try validate(response: doctorsRawResponse, data: doctorsData)

        let usersJSON = (try? JSONSerialization.jsonObject(with: usersData) as? [String: Any]) ?? [:]
        let doctorsJSON = (try? JSONSerialization.jsonObject(with: doctorsData) as? [String: Any]) ?? [:]

        let approvedDoctorIDs = Set(doctorsJSON.compactMap { key, value -> String? in
            guard let item = value as? [String: Any] else { return nil }
            let isApproved = boolValue(item["approved"]) ?? false
            let doctorId = stringValue(item["doctorId"])?.nonEmpty ?? key
            return isApproved ? doctorId : nil
        })

        let useApprovedOnly = !approvedDoctorIDs.isEmpty

        let doctors = usersJSON.compactMap { key, value -> DoctorSummary? in
            guard let user = value as? [String: Any] else { return nil }
            let role = stringValue(user["role"])?.lowercased()
            guard role == "doctor" else { return nil }

            let userId = stringValue(user["userId"])?.nonEmpty ?? key
            if useApprovedOnly, !approvedDoctorIDs.contains(userId) {
                return nil
            }

            let doctorNode = doctorsJSON[userId] as? [String: Any]
            let approved = boolValue(doctorNode?["approved"])

            return DoctorSummary(
                id: stringValue(doctorNode?["doctorId"])?.nonEmpty ?? userId,
                name: stringValue(user["fullName"])?.nonEmpty ??
                    stringValue(user["name"])?.nonEmpty ??
                    stringValue(doctorNode?["doctorName"])?.nonEmpty ??
                    "Doctor",
                specialty: stringValue(doctorNode?["specialty"])?.nonEmpty ??
                    stringValue(user["specialty"])?.nonEmpty ??
                    "Medical Doctor",
                hospital: stringValue(doctorNode?["hospital"])?.nonEmpty ??
                    stringValue(user["hospital"])?.nonEmpty ??
                    "HASET Hospital",
                phoneNumber: stringValue(user["phone"])?.nonEmpty ??
                    stringValue(doctorNode?["phone"])?.nonEmpty,
                email: stringValue(user["email"])?.nonEmpty ??
                    stringValue(doctorNode?["email"])?.nonEmpty,
                address: stringValue(user["location"])?.nonEmpty ??
                    stringValue(doctorNode?["location"])?.nonEmpty ??
                    stringValue(doctorNode?["address"])?.nonEmpty,
                bio: stringValue(user["bio"])?.nonEmpty ??
                    stringValue(user["about"])?.nonEmpty ??
                    stringValue(doctorNode?["about"])?.nonEmpty ??
                    stringValue(doctorNode?["bio"])?.nonEmpty,
                rating: doubleValue(doctorNode?["averageRating"]) ??
                    doubleValue(doctorNode?["rating"]) ??
                    doubleValue(user["rating"]) ??
                    doubleValue(user["averageRating"]) ??
                    0,
                experienceYears: intValue(doctorNode?["experience"]) ??
                    intValue(doctorNode?["experienceYears"]) ??
                    intValue(user["experience"]) ??
                    intValue(user["experienceYears"]),
                verified: boolValue(doctorNode?["verified"]) ?? approved ?? false,
                consultationFee: consultationFeeValue(doctorNode?["consultationFee"]) ??
                    consultationFeeValue(user["consultationFee"]) ??
                    "TZS 0",
                availableToday: boolValue(doctorNode?["isAvailable"]) ??
                    boolValue(user["isAvailable"]) ??
                    true,
                profileImage: stringValue(user["profileImage"])?.nonEmpty ??
                    stringValue(doctorNode?["profileImage"])?.nonEmpty,
                availableTimes: stringArrayValue(doctorNode?["availableTimes"]) ??
                    stringArrayValue(user["availableTimes"])
            )
        }

        return doctors.sorted { lhs, rhs in
            if lhs.rating == rhs.rating {
                return lhs.name.localizedCaseInsensitiveCompare(rhs.name) == .orderedAscending
            }
            return lhs.rating > rhs.rating
        }
    }

    func fetchAppointments(userId: String, role: UserRole, idToken: String?) async throws -> [AppointmentSummary] {
        let url = try databaseURL(path: "appointments", authToken: idToken)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        guard data != Data("null".utf8) else { return [] }
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ServiceError.invalidResponse
        }

        let appointments = json.compactMap { key, value -> AppointmentSummary? in
            guard let item = value as? [String: Any] else { return nil }
            let ownerId = role == .doctor ? stringValue(item["doctorId"]) : stringValue(item["patientId"])
            guard ownerId == userId else { return nil }

            let doctorName = stringValue(item["doctorName"])?.nonEmpty ?? "Doctor"
            let patientName = stringValue(item["patientName"])?.nonEmpty ?? "Patient"
            let specialty = stringValue(item["doctorSpecialty"])?.nonEmpty
            let reason = stringValue(item["reason"])?.nonEmpty
            let appointmentType = stringValue(item["appointmentType"])?.nonEmpty
            let date = stringValue(item["date"])?.nonEmpty ?? ""
            let time = stringValue(item["time"])?.nonEmpty ?? ""
            let subtitle = role == .doctor ? (reason ?? appointmentType ?? "") : (specialty ?? reason ?? appointmentType ?? "")

            return AppointmentSummary(
                id: stringValue(item["appointmentId"])?.nonEmpty ?? key,
                title: role == .doctor ? patientName : doctorName,
                subtitle: subtitle,
                dateText: [date, time].filter { !$0.isEmpty }.joined(separator: ", "),
                status: appointmentStatus(from: stringValue(item["status"]))
            )
        }

        return appointments.sorted { $0.id > $1.id }
    }

    func createAppointment(
        patient: UserProfile,
        doctor: DoctorSummary,
        date: String,
        time: String,
        reason: String,
        appointmentType: String,
        idToken: String?
    ) async throws {
        let appointmentId = UUID().uuidString
        let payload: [String: Any] = [
            "appointmentId": appointmentId,
            "patientId": patient.userId,
            "doctorId": doctor.id,
            "patientName": patient.fullName,
            "doctorName": doctor.name,
            "doctorSpecialty": doctor.specialty,
            "date": date,
            "time": time,
            "reason": reason,
            "status": "pending",
            "appointmentType": appointmentType,
            "createdAt": Int(Date().timeIntervalSince1970 * 1000)
        ]
        let url = try databaseURL(path: "appointments/\(appointmentId)", authToken: idToken)
        try await put(payload, url: url)
    }

    func initiatePayment(
        user: UserProfile,
        doctor: DoctorSummary,
        amount: Double,
        provider: String,
        paymentAccount: String,
        idToken: String?
    ) async throws -> PaymentInitiationResponse {
        let url = URL(string: "\(HASETConstants.productionAPIURL)mobile/payment/initiate")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let idToken, !idToken.isEmpty {
            request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        }

        let payload: [String: Any] = [
            "user_id": user.userId,
            "doctor_id": doctor.id,
            "amount": amount,
            "provider": provider,
            "payment_account": paymentAccount,
            "webhook_url": "\(HASETConstants.productionAPIURL)payment/callback",
            "buyer_email": user.email,
            "buyer_name": user.fullName,
            "buyer_phone": user.phone,
            "order_id": "HASET-\(Int(Date().timeIntervalSince1970 * 1000))"
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: payload)

        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response: response, data: data)
        return try decoder.decode(PaymentInitiationResponse.self, from: data)
    }

    func checkPaymentStatus(transactionId: Int, idToken: String?) async throws -> PaymentStatusEnvelope {
        var components = URLComponents(string: "\(HASETConstants.productionAPIURL)mobile/payment/status")!
        components.queryItems = [URLQueryItem(name: "transaction_id", value: String(transactionId))]
        guard let url = components.url else {
            throw ServiceError.invalidResponse
        }

        var request = URLRequest(url: url)
        if let idToken, !idToken.isEmpty {
            request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response: response, data: data)
        return try decoder.decode(PaymentStatusEnvelope.self, from: data)
    }

    func fetchConversations(userId: String, idToken: String?) async throws -> [ConversationSummary] {
        let url = try databaseURL(path: "user_conversations/\(userId)", authToken: idToken)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        guard data != Data("null".utf8) else { return [] }
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ServiceError.invalidResponse
        }

        var conversations: [ConversationSummary] = []
        for (otherUserId, value) in json {
            guard let item = value as? [String: Any] else { continue }
            let chatRoomId = generateChatRoomId(userId, otherUserId)
            let unreadCount = (try? await fetchUnreadMessageCount(chatRoomId: chatRoomId, currentUserId: userId, idToken: idToken)) ?? 0
            let timestamp = timeIntervalValue(item["lastMessageTimestamp"]) ?? 0
            conversations.append(
                ConversationSummary(
                    id: chatRoomId,
                    name: stringValue(item["otherUserName"])?.nonEmpty ?? "Conversation",
                    lastMessage: stringValue(item["lastMessage"])?.nonEmpty ?? "",
                    lastMessageTimestamp: timestamp,
                    unreadCount: unreadCount,
                    isOnline: false,
                    archived: boolValue(item["archived"]) ?? false,
                    profileImage: nil
                )
            )
        }

        return conversations.sorted { $0.lastMessageTimestamp > $1.lastMessageTimestamp }
    }

    func fetchNotifications(userId: String, idToken: String?) async throws -> [NotificationSummary] {
        let url = try databaseURL(path: "notifications/\(userId)", authToken: idToken)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        guard data != Data("null".utf8) else { return [] }
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ServiceError.invalidResponse
        }

        return json.compactMap { key, value -> NotificationSummary? in
            guard let item = value as? [String: Any] else { return nil }
            return NotificationSummary(
                id: stringValue(item["notificationId"])?.nonEmpty ?? key,
                title: stringValue(item["title"])?.nonEmpty ?? "Notification",
                message: stringValue(item["message"])?.nonEmpty ?? "",
                type: stringValue(item["type"])?.nonEmpty ?? "general",
                isRead: boolValue(item["isRead"]) ?? false,
                timestamp: timeIntervalValue(item["timestamp"]) ?? 0
            )
        }
        .sorted { $0.timestamp > $1.timestamp }
    }

    private func fetchUserProfile(
        userId: String,
        idToken: String,
        fallbackEmail: String?,
        fallbackName: String?,
        fallbackPhone: String?
    ) async throws -> UserProfile {
        let url = try databaseURL(path: "users/\(userId)", authToken: idToken)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        if data == Data("null".utf8) {
            throw ServiceError.message("User profile not found")
        }
        return try makeUserProfile(
            from: data,
            fallbackUserId: userId,
            fallbackEmail: fallbackEmail,
            fallbackName: fallbackName,
            fallbackPhone: fallbackPhone
        )
    }

    private func saveUserProfile(_ profile: UserProfile, idToken: String) async throws {
        let url = try databaseURL(path: "users/\(profile.userId)", authToken: idToken)
        try await put(profile, url: url)
    }

    private func saveDoctorBootstrap(profile: UserProfile, idToken: String) async throws {
        let payload: [String: Any] = [
            "doctorId": profile.userId,
            "regNo": profile.regNo ?? "",
            "approved": false,
            "verified": false
        ]
        let url = try databaseURL(path: "doctors/\(profile.userId)", authToken: idToken)
        try await put(payload, url: url)
    }

    private func performIdentityRequest(path: String, payload: [String: Any]) async throws -> IdentityResponse {
        let url = URL(string: "https://identitytoolkit.googleapis.com/v1/\(path)?key=\(HASETConstants.firebaseAPIKey)")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: payload)

        let (data, response) = try await URLSession.shared.data(for: request)
        if let httpResponse = response as? HTTPURLResponse, !(200 ... 299).contains(httpResponse.statusCode) {
            if let serviceError = try? decoder.decode(IdentityErrorEnvelope.self, from: data) {
                throw ServiceError.message(mapIdentityError(serviceError.error.message))
            }
            throw ServiceError.invalidResponse
        }
        return try decoder.decode(IdentityResponse.self, from: data)
    }

    private func put<T: Encodable>(_ value: T, url: URL) async throws {
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(value)
        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response: response, data: data)
    }

    private func delete(url: URL) async throws {
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response: response, data: data)
    }

    private func incrementCounter(path: String, idToken: String?) async throws -> Int {
        let url = try databaseURL(path: path, authToken: idToken)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        let currentValue = (try? JSONDecoder().decode(Int.self, from: data)) ?? 0
        let newValue = currentValue + 1
        try await put(newValue, url: url)
        return newValue
    }

    private func put(_ dictionary: [String: Any], url: URL) async throws {
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: dictionary)
        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response: response, data: data)
    }

    private func makeUserProfile(
        from data: Data,
        fallbackUserId: String,
        fallbackEmail: String?,
        fallbackName: String?,
        fallbackPhone: String?
    ) throws -> UserProfile {
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ServiceError.invalidResponse
        }

        let userId = stringValue(json["userId"])?.nonEmpty ?? fallbackUserId
        let email = stringValue(json["email"])?.nonEmpty ?? fallbackEmail?.nonEmpty
        let fullName = stringValue(json["fullName"])?.nonEmpty ?? fallbackName?.nonEmpty
        let phone = stringValue(json["phone"]) ?? fallbackPhone ?? ""
        let role = UserRole(rawValue: stringValue(json["role"]) ?? "") ?? .patient

        guard let email, let fullName else {
            throw ServiceError.message("User profile is incomplete")
        }

        return UserProfile(
            userId: userId,
            email: email,
            fullName: fullName,
            phone: phone,
            role: role,
            profileImage: stringValue(json["profileImage"]) ?? "",
            createdAt: timeIntervalValue(json["createdAt"]) ?? Date().timeIntervalSince1970 * 1000,
            regNo: stringValue(json["regNo"]),
            gender: stringValue(json["gender"]),
            age: stringValue(json["age"]),
            location: stringValue(json["location"]),
            bio: stringValue(json["bio"]),
            specialization: stringValue(json["specialization"]) ?? stringValue(json["specialty"]),
            consultationFee: consultationFeeValue(json["consultationFee"]),
            availableTimes: stringArrayValue(json["availableTimes"]),
            verified: boolValue(json["verified"])
        )
    }

    private func stringValue(_ value: Any?) -> String? {
        switch value {
        case let string as String:
            return string
        case let number as NSNumber:
            return number.stringValue
        default:
            return nil
        }
    }

    private func timeIntervalValue(_ value: Any?) -> TimeInterval? {
        switch value {
        case let number as NSNumber:
            return number.doubleValue
        case let string as String:
            return Double(string)
        default:
            return nil
        }
    }

    private func intValue(_ value: Any?) -> Int? {
        switch value {
        case let number as NSNumber:
            return number.intValue
        case let string as String:
            return Int(string)
        default:
            return nil
        }
    }

    private func fetchUnreadMessageCount(chatRoomId: String, currentUserId: String, idToken: String?) async throws -> Int {
        let url = try databaseURL(path: "messages/\(chatRoomId)", authToken: idToken)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        guard data != Data("null".utf8) else { return 0 }
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return 0
        }

        return json.values.reduce(into: 0) { total, value in
            guard let item = value as? [String: Any] else { return }
            let receiverId = stringValue(item["receiverId"])
            let isRead = boolValue(item["isRead"]) ?? false
            if receiverId == currentUserId && !isRead {
                total += 1
            }
        }
    }

    private func appointmentStatus(from rawValue: String?) -> AppointmentSummary.Status {
        switch rawValue?.lowercased() {
        case "approved":
            return .approved
        case "completed":
            return .completed
        case "cancelled", "canceled", "declined":
            return .cancelled
        default:
            return .pending
        }
    }

    private func doubleValue(_ value: Any?) -> Double? {
        switch value {
        case let number as NSNumber:
            return number.doubleValue
        case let string as String:
            return Double(string)
        default:
            return nil
        }
    }

    private func generateChatRoomId(_ userId1: String, _ userId2: String) -> String {
        userId1 < userId2 ? "\(userId1)_\(userId2)" : "\(userId2)_\(userId1)"
    }

    private func boolValue(_ value: Any?) -> Bool? {
        switch value {
        case let bool as Bool:
            return bool
        case let number as NSNumber:
            return number.boolValue
        case let string as String:
            return ["true", "1", "yes"].contains(string.lowercased())
        default:
            return nil
        }
    }

    private func stringArrayValue(_ value: Any?) -> [String]? {
        if let array = value as? [String] {
            return array
        }
        if let array = value as? [Any] {
            let strings = array.compactMap { stringValue($0)?.nonEmpty }
            return strings.isEmpty ? nil : strings
        }
        if let string = value as? String {
            let separators = CharacterSet(charactersIn: ",")
            let parts = string
                .components(separatedBy: separators)
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
            if !parts.isEmpty {
                return parts
            }
            return string.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : [string]
        }
        return nil
    }

    private func consultationFeeValue(_ value: Any?) -> String? {
        switch value {
        case let string as String:
            return string
        case let number as NSNumber:
            return "TZS \(number.intValue)"
        default:
            return nil
        }
    }

    private func estimatedReadTime(from text: String) -> String {
        let wordCount = text.split { $0.isWhitespace || $0.isNewline }.count
        let minutes = max(1, Int(ceil(Double(wordCount) / 180.0)))
        return "\(minutes) min"
    }

    private func validate(response: URLResponse, data: Data) throws {
        guard let httpResponse = response as? HTTPURLResponse else {
            throw ServiceError.invalidResponse
        }
        guard (200 ... 299).contains(httpResponse.statusCode) else {
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                if let message = json["message"] as? String, !message.isEmpty {
                    throw ServiceError.message(message)
                }
                if let error = json["error"] as? String, !error.isEmpty {
                    throw ServiceError.message(error)
                }
                if
                    let errors = json["errors"] as? [String: Any],
                    let firstValue = errors.values.first
                {
                    if let firstArrayValue = firstValue as? [String], let firstMessage = firstArrayValue.first, !firstMessage.isEmpty {
                        throw ServiceError.message(firstMessage)
                    }
                    if let firstMessage = firstValue as? String, !firstMessage.isEmpty {
                        throw ServiceError.message(firstMessage)
                    }
                }
                if let raw = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines), !raw.isEmpty {
                    throw ServiceError.message(raw)
                }
            }
            throw ServiceError.invalidResponse
        }
    }

    private func databaseURL(path: String, authToken: String?) throws -> URL {
        var components = URLComponents(string: "\(HASETConstants.firebaseDatabaseURL)/\(path).json")
        if let authToken {
            components?.queryItems = [URLQueryItem(name: "auth", value: authToken)]
        }
        guard let url = components?.url else {
            throw ServiceError.invalidResponse
        }
        return url
    }

    private func mapIdentityError(_ message: String) -> String {
        switch message {
        case "EMAIL_NOT_FOUND":
            return "No account found with that email"
        case "INVALID_PASSWORD":
            return "Incorrect password"
        case "EMAIL_EXISTS":
            return "An account with this email already exists"
        case "WEAK_PASSWORD : Password should be at least 6 characters":
            return "Password must be at least 6 characters"
        default:
            return message.replacingOccurrences(of: "_", with: " ").capitalized
        }
    }
}

private extension String {
    var nonEmpty: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
