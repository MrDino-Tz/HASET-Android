import CoreLocation
import Foundation
import Security
import UserNotifications

enum HASETConstants {
    static let firebaseAPIKey = "AIzaSyB6XncMhXdlT0fScdU6Fq7Nw_toPmf-tRU"
    static let firebaseDatabaseURL = "https://hasetapp-4eeba-default-rtdb.europe-west1.firebasedatabase.app"
    static let productionAPIURL = "https://payments.hasethospital.or.tz/public/api/"
    static let privacyPolicyURL = "https://hasethospital.or.tz/legal/privacy-policy"
    static let termsURL = "https://hasethospital.or.tz/legal/terms"
    static let supportURL = "https://hasethospital.or.tz/contact"
    static let appConfigPath = "app_config"
    static let cloudinaryCloudName = "divky8yna"
    static let cloudinaryUploadPreset = "haset_mobile_unsigned"
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
            kSecAttrAccount as String: key,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
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

    static func isStrongPassword(_ password: String) -> Bool {
        password.count >= 12
            && password.rangeOfCharacter(from: .lowercaseLetters) != nil
            && password.rangeOfCharacter(from: .uppercaseLetters) != nil
            && password.rangeOfCharacter(from: .decimalDigits) != nil
    }

    static func isValidPhone(_ phone: String) -> Bool {
        phone.range(of: #"^\+[0-9]{12,}$"#, options: .regularExpression) != nil
    }

    static func isValidName(_ name: String) -> Bool {
        name.trimmingCharacters(in: .whitespacesAndNewlines).count >= 2
    }
}

enum StaticContentService {
    static let specialties: [String] = []
    static let timeSlots: [String] = [
        "06:00",
        "07:00",
        "08:00",
        "09:00",
        "10:00",
        "11:00",
        "12:00",
        "13:00",
        "14:00",
        "15:00",
        "16:00",
        "17:00",
        "18:00",
        "19:00",
        "20:00",
        "21:00"
    ]
    static let doctors: [DoctorSummary] = []
    static let homeHighlights: [HomeHighlight] = []
    static let articles: [ArticleSummary] = []
    static let patientAppointments: [AppointmentSummary] = []
    static let doctorAppointments: [AppointmentSummary] = []
    static let adminMetrics: [AdminMetric] = []
    static let conversations: [UserRole: [ConversationSummary]] = [:]
    static let hospitals: [HospitalSummary] = []
    static let pharmacyCategories: [PharmacyCategory] = []
    static let recentNotifications: [String] = []

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

    @MainActor
    func currentLocationEnabled() -> Bool {
        let status = locationManager.authorizationStatus
        return status == .authorizedAlways || status == .authorizedWhenInUse
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
        // Anonymous Firebase sign-up responses do not include an email field.
        let email: String?
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

    /// Creates the same short-lived Firebase anonymous payment session used by
    /// Android for doctor registration before a credentialed account exists.
    /// The session is only used to authenticate the hosted payment request; the
    /// real doctor account is created after payment confirmation.
    func signInAnonymously() async throws -> StoredSession {
        let identity = try await performIdentityRequest(
            path: "accounts:signUp",
            payload: ["returnSecureToken": true]
        )
        return StoredSession(
            userId: identity.localId,
            idToken: identity.idToken,
            refreshToken: identity.refreshToken,
            role: .patient,
            userName: "",
            email: "",
            phone: ""
        )
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
            consultationFee: nil,
            availableTimes: role == .doctor ? nil : nil,
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
        if profile.role == .doctor {
            let doctorURL = try databaseURL(path: "doctors/\(profile.userId)", authToken: idToken)
            var doctorUpdates: [String: Any] = [
                "doctorId": profile.userId,
                "profileImage": profile.profileImage,
                "lastUpdated": Int(Date().timeIntervalSince1970 * 1000)
            ]
            if let specialization = profile.specialization { doctorUpdates["specialty"] = specialization }
            if let consultationFee = profile.consultationFee { doctorUpdates["consultationFee"] = consultationFee }
            if let availableTimes = profile.availableTimes { doctorUpdates["availableTimes"] = availableTimes }
            try await patch(doctorUpdates, url: doctorURL)
        }
    }

    func uploadProfileImage(_ imageData: Data, userId: String) async throws -> String {
        guard !imageData.isEmpty else {
            throw ServiceError.message("The selected profile photo is empty.")
        }

        let endpoint = "https://api.cloudinary.com/v1_1/\(HASETConstants.cloudinaryCloudName)/image/upload"
        guard let url = URL(string: endpoint) else { throw ServiceError.invalidResponse }

        let boundary = "HASET-\(UUID().uuidString)"
        var body = Data()
        func appendField(name: String, value: String) {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"\(name)\"\r\n\r\n".data(using: .utf8)!)
            body.append("\(value)\r\n".data(using: .utf8)!)
        }

        appendField(name: "upload_preset", value: HASETConstants.cloudinaryUploadPreset)
        appendField(name: "folder", value: "profile_photos")
        appendField(name: "public_id", value: "\(userId)_profile_\(UUID().uuidString)")
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"profile.jpg\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
        body.append(imageData)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        request.httpBody = body

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse,
              (200 ... 299).contains(httpResponse.statusCode),
              let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let secureURL = stringValue(json["secure_url"])?.nonEmpty else {
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let error = json["error"] as? [String: Any],
               let message = stringValue(error["message"])?.nonEmpty {
                throw ServiceError.message("Photo upload failed: \(message)")
            }
            throw ServiceError.message("Photo upload failed. Please try again.")
        }
        return secureURL
    }

    func deleteCurrentAccount(userId: String, role: UserRole, idToken: String?) async throws {
        let userURL = try databaseURL(path: "users/\(userId)", authToken: idToken)
        try await delete(url: userURL)
        if role == .doctor {
            let doctorURL = try databaseURL(path: "doctors/\(userId)", authToken: idToken)
            try await delete(url: doctorURL)
            let walletURL = try databaseURL(path: "doctor_wallets/\(userId)", authToken: idToken)
            try await delete(url: walletURL)
        }
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
        let doctorsURL = try databaseURL(path: "doctors", authToken: idToken)
        let (doctorsData, doctorsRawResponse) = try await URLSession.shared.data(from: doctorsURL)
        try validate(response: doctorsRawResponse, data: doctorsData)
        let doctorsJSON = (try? JSONSerialization.jsonObject(with: doctorsData) as? [String: Any]) ?? [:]

        let approvedDoctorIDs = Set(doctorsJSON.compactMap { key, value -> String? in
            guard let item = value as? [String: Any], boolValue(item["approved"]) == true else { return nil }
            return stringValue(item["doctorId"])?.nonEmpty ?? key
        })

        let usersURL = try databaseURL(
            path: "users",
            authToken: idToken,
            queryItems: [
                URLQueryItem(name: "orderBy", value: "\"role\""),
                URLQueryItem(name: "equalTo", value: "\"doctor\"")
            ]
        )
        var usersJSON: [String: Any] = [:]
        if let (usersData, usersRawResponse) = try? await URLSession.shared.data(from: usersURL),
           let response = usersRawResponse as? HTTPURLResponse,
           (200 ... 299).contains(response.statusCode) {
            usersJSON = (try? JSONSerialization.jsonObject(with: usersData) as? [String: Any]) ?? [:]
        }

        if usersJSON.isEmpty, !approvedDoctorIDs.isEmpty {
            let requests = try approvedDoctorIDs.map { doctorId in
                (doctorId, try databaseURL(path: "users/\(doctorId)", authToken: idToken))
            }
            let profileResponses = await withTaskGroup(of: (String, Data?).self) { group in
                for (doctorId, url) in requests {
                    group.addTask {
                        guard let (data, response) = try? await URLSession.shared.data(from: url),
                              let httpResponse = response as? HTTPURLResponse,
                              (200 ... 299).contains(httpResponse.statusCode),
                              data != Data("null".utf8) else { return (doctorId, nil) }
                        return (doctorId, data)
                    }
                }

                var responses: [(String, Data?)] = []
                for await response in group { responses.append(response) }
                return responses
            }
            for (doctorId, data) in profileResponses {
                guard let data,
                      let profile = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { continue }
                usersJSON[doctorId] = profile
            }
        }

        let doctors = approvedDoctorIDs.compactMap { userId -> DoctorSummary? in
            let user = usersJSON[userId] as? [String: Any] ?? [:]

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
                    stringValue(user["specialization"])?.nonEmpty ??
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
        let appointmentRecords: [(String, [String: Any])]

        if role == .admin {
            appointmentRecords = try await fetchAppointmentRecordsFromMainNode(
                userId: userId,
                role: role,
                idToken: idToken
            )
        } else {
            do {
                let indexPath = role == .doctor ? "doctor_appointments/\(userId)" : "patient_appointments/\(userId)"
                let indexURL = try databaseURL(path: indexPath, authToken: idToken)
                let (indexData, indexResponse) = try await URLSession.shared.data(from: indexURL)
                try validate(response: indexResponse, data: indexData)

                if indexData == Data("null".utf8) {
                    appointmentRecords = try await fetchAppointmentRecordsFromMainNode(
                        userId: userId,
                        role: role,
                        idToken: idToken
                    )
                } else {
                    guard let index = try JSONSerialization.jsonObject(with: indexData) as? [String: Any] else {
                        throw ServiceError.invalidResponse
                    }

                    let requests = try index.keys.map { appointmentId in
                        (appointmentId, try databaseURL(path: "appointments/\(appointmentId)", authToken: idToken))
                    }
                    let responses = await withTaskGroup(of: (String, Data?).self) { group in
                        for (appointmentId, url) in requests {
                            group.addTask {
                                guard let (data, response) = try? await URLSession.shared.data(from: url),
                                      let httpResponse = response as? HTTPURLResponse,
                                      (200 ... 299).contains(httpResponse.statusCode),
                                      data != Data("null".utf8) else { return (appointmentId, nil) }
                                return (appointmentId, data)
                            }
                        }

                        var fetched: [(String, Data?)] = []
                        for await response in group { fetched.append(response) }
                        return fetched
                    }
                    let indexedRecords = responses.compactMap { appointmentId, data -> (String, [String: Any])? in
                        guard let data,
                              let item = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return nil }
                        return (appointmentId, item)
                    }
                    if indexedRecords.isEmpty {
                        appointmentRecords = try await fetchAppointmentRecordsFromMainNode(
                            userId: userId,
                            role: role,
                            idToken: idToken
                        )
                    } else {
                        appointmentRecords = indexedRecords
                    }
                }
            } catch {
                appointmentRecords = try await fetchAppointmentRecordsFromMainNode(
                    userId: userId,
                    role: role,
                    idToken: idToken
                )
            }
        }

        let appointments = appointmentRecords.compactMap { key, item -> AppointmentSummary? in
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
                patientId: stringValue(item["patientId"])?.nonEmpty,
                doctorId: stringValue(item["doctorId"])?.nonEmpty,
                title: role == .doctor ? patientName : doctorName,
                subtitle: subtitle,
                date: date,
                time: time,
                dateText: [date, time].filter { !$0.isEmpty }.joined(separator: ", "),
                status: appointmentStatus(from: stringValue(item["status"])),
                appointmentType: appointmentType,
                createdAt: timeIntervalValue(item["createdAt"])
            )
        }

        return appointments.sorted {
            if ($0.createdAt ?? 0) == ($1.createdAt ?? 0) { return $0.id > $1.id }
            return ($0.createdAt ?? 0) > ($1.createdAt ?? 0)
        }
    }

    private func fetchAppointmentRecordsFromMainNode(
        userId: String,
        role: UserRole,
        idToken: String?
    ) async throws -> [(String, [String: Any])] {
        let queryItems: [URLQueryItem] = role == .admin ? [] : [
            URLQueryItem(name: "orderBy", value: role == .doctor ? "\"doctorId\"" : "\"patientId\""),
            URLQueryItem(name: "equalTo", value: "\"\(userId)\"")
        ]
        let url = try databaseURL(path: "appointments", authToken: idToken, queryItems: queryItems)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        guard data != Data("null".utf8) else { return [] }
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ServiceError.invalidResponse
        }
        let records: [(String, [String: Any])] = json.compactMap { key, value in
            guard let item = value as? [String: Any] else { return nil }
            return (key, item)
        }
        guard role != .admin else { return records }
        return records.filter { _, item in
            let ownerId = role == .doctor ? stringValue(item["doctorId"]) : stringValue(item["patientId"])
            return ownerId == userId
        }
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

        let patientIndexURL = try databaseURL(path: "patient_appointments/\(patient.userId)/\(appointmentId)", authToken: idToken)
        try await put(true, url: patientIndexURL)
        let doctorIndexURL = try databaseURL(path: "doctor_appointments/\(doctor.id)/\(appointmentId)", authToken: idToken)
        try await put(true, url: doctorIndexURL)

        let timestamp = Int(Date().timeIntervalSince1970 * 1000)
        let patientConversation: [String: Any] = [
            "otherUserId": doctor.id,
            "otherUserName": doctor.name,
            "lastMessage": appointmentType == "Online Chat" ? "Chat appointment booked" : "Appointment booked",
            "lastMessageTimestamp": timestamp,
            "lastMessageSenderId": patient.userId,
            "archived": false
        ]
        let doctorConversation: [String: Any] = [
            "otherUserId": patient.userId,
            "otherUserName": patient.fullName,
            "lastMessage": appointmentType == "Online Chat" ? "Chat appointment booked" : "Appointment booked",
            "lastMessageTimestamp": timestamp,
            "lastMessageSenderId": patient.userId,
            "archived": false
        ]
        let patientConvURL = try databaseURL(path: "user_conversations/\(patient.userId)/\(doctor.id)", authToken: idToken)
        try await put(patientConversation, url: patientConvURL)
        let doctorConvURL = try databaseURL(path: "user_conversations/\(doctor.id)/\(patient.userId)", authToken: idToken)
        try await put(doctorConversation, url: doctorConvURL)
    }

    func updateAppointmentStatus(
        appointmentId: String,
        status: String,
        idToken: String
    ) async throws {
        let url = try databaseURL(path: "appointments/\(appointmentId)", authToken: idToken)
        try await patch([
            "status": status,
            "updatedAt": Int(Date().timeIntervalSince1970 * 1000)
        ], url: url)
    }

    func rescheduleAppointment(
        appointmentId: String,
        date: String,
        time: String,
        rescheduledBy userId: String,
        idToken: String
    ) async throws {
        let url = try databaseURL(path: "appointments/\(appointmentId)", authToken: idToken)
        try await patch([
            "date": date,
            "time": time,
            "status": "pending",
            "lastUpdated": Int(Date().timeIntervalSince1970 * 1000),
            "rescheduledBy": userId
        ], url: url)
    }

    func initiatePayment(
        user: UserProfile,
        doctor: DoctorSummary,
        consultationId: String,
        idempotencyKey: String,
        amount: Double,
        paymentMethod: String,
        provider: String,
        paymentAccount: String,
        idToken: String?
    ) async throws -> PaymentInitiationResponse {
        let url = URL(string: "\(HASETConstants.productionAPIURL)mobile/payment/initiate")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(idempotencyKey, forHTTPHeaderField: "Idempotency-Key")
        if let idToken, !idToken.isEmpty {
            request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        }

        var payload: [String: Any] = [
            "user_id": user.userId,
            "doctor_id": doctor.id,
            "consultation_id": consultationId,
            "amount": Int(amount.rounded()),
            "payment_method": paymentMethod
        ]
        if paymentMethod == "mobile_money" {
            payload["provider"] = provider
            payload["payment_account"] = paymentAccount
        } else {
            let nameParts = user.fullName.split(separator: " ").map(String.init)
            payload["redirect_url"] = "https://hasethospital.or.tz/payment/success"
            payload["cancel_url"] = "https://hasethospital.or.tz/payment/cancel"
            payload["customer"] = [
                "firstname": nameParts.first ?? "HASET",
                "lastname": nameParts.dropFirst().joined(separator: " ").isEmpty ? "Customer" : nameParts.dropFirst().joined(separator: " "),
                "email": user.email,
                "address": "HASET Hospital",
                "city": "Dar es Salaam",
                "state": "Dar es Salaam",
                "postcode": "14101",
                "country": "TZ",
                "phone": user.phone
            ]
        }
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
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let idToken, !idToken.isEmpty {
            request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response: response, data: data)
        return try decoder.decode(PaymentStatusEnvelope.self, from: data)
    }

    func cancelPayment(transactionId: Int, idToken: String?) async throws {
        let url = URL(string: "\(HASETConstants.productionAPIURL)mobile/payment/cancel")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let idToken, !idToken.isEmpty {
            request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        }
        request.httpBody = try JSONSerialization.data(withJSONObject: ["transaction_id": transactionId])
        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response: response, data: data)
    }

    func mobileMFAStatus(idToken: String) async throws -> Bool {
        let url = URL(string: "\(HASETConstants.productionAPIURL)mobile/mfa/status")!
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.timeoutInterval = 15
        request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        let (data, response) = try await URLSession.shared.data(for: request); try validate(response: response, data: data)
        let object = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        return object?["two_factor_enabled"] as? Bool ?? false
    }

    func setupMobileMFA(idToken: String) async throws -> MobileMFASetupResponse {
        let url = URL(string: "\(HASETConstants.productionAPIURL)mobile/mfa/setup")!
        var request = URLRequest(url: url); request.httpMethod = "POST"; request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        let (data, response) = try await URLSession.shared.data(for: request); try validate(response: response, data: data)
        return try decoder.decode(MobileMFASetupResponse.self, from: data)
    }

    func confirmMobileMFA(code: String, idToken: String) async throws {
        let url = URL(string: "\(HASETConstants.productionAPIURL)mobile/mfa/confirm")!
        var request = URLRequest(url: url); request.httpMethod = "POST"; request.setValue("application/json", forHTTPHeaderField: "Content-Type"); request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["code": code])
        let (data, response) = try await URLSession.shared.data(for: request); try validate(response: response, data: data)
    }

    func verifyMobileMFA(code: String, idToken: String) async throws -> String {
        let url = URL(string: "\(HASETConstants.productionAPIURL)mobile/mfa/verify")!
        var request = URLRequest(url: url); request.httpMethod = "POST"; request.setValue("application/json", forHTTPHeaderField: "Content-Type"); request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["code": code])
        let (data, response) = try await URLSession.shared.data(for: request); try validate(response: response, data: data)
        guard let object = try JSONSerialization.jsonObject(with: data) as? [String: Any], let token = object["mfa_action_token"] as? String else { throw ServiceError.invalidResponse }
        return token
    }

    func disableMobileMFA(code: String, idToken: String) async throws {
        let url = URL(string: "\(HASETConstants.productionAPIURL)mobile/mfa/disable")!
        var request = URLRequest(url: url); request.httpMethod = "POST"; request.setValue("application/json", forHTTPHeaderField: "Content-Type"); request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["code": code])
        let (data, response) = try await URLSession.shared.data(for: request); try validate(response: response, data: data)
    }

    func requestDoctorWithdrawal(amount: Int, reason: String, idToken: String, mfaActionToken: String) async throws {
        let url = URL(string: "\(HASETConstants.productionAPIURL)mobile/doctor/withdrawals")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        request.setValue(mfaActionToken, forHTTPHeaderField: "X-MFA-Action-Token")
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "request_id": "WR-\(UUID().uuidString.replacingOccurrences(of: "-", with: "").prefix(24))",
            "amount": amount,
            "reason": reason
        ])
        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response: response, data: data)
    }

    func fetchDoctorWithdrawals(idToken: String) async throws -> [[String: Any]] {
        let url = URL(string: "\(HASETConstants.productionAPIURL)mobile/doctor/withdrawals")!
        var request = URLRequest(url: url); request.httpMethod = "GET"; request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        let (data, response) = try await URLSession.shared.data(for: request); try validate(response: response, data: data)
        let object = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        return object?["withdrawals"] as? [[String: Any]] ?? []
    }

    func fetchDoctorWallet(doctorId: String, idToken: String?) async throws -> DoctorWalletSummary? {
        let url = URL(string: "\(HASETConstants.productionAPIURL)mobile/doctor/wallet")!
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let idToken, !idToken.isEmpty {
            request.setValue("Bearer \(idToken)", forHTTPHeaderField: "Authorization")
        }
        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response: response, data: data)
        guard let envelope = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let wallet = envelope["wallet"] as? [String: Any] else { return nil }
        return DoctorWalletSummary(
            doctorId: stringValue(wallet["doctor_id"])?.nonEmpty ?? doctorId,
            balance: doubleValue(wallet["available_balance"]) ?? 0,
            totalEarnings: doubleValue(wallet["paid_out_balance"]),
            lastUpdated: timeIntervalValue(wallet["updated_at"])
        )
    }

    func fetchDoctorPresence(doctorId: String, idToken: String?) async throws -> DoctorPresenceSummary? {
        let path = "doctors/\(doctorId)"
        let url = try databaseURL(path: path, authToken: idToken)
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        guard data != Data("null".utf8) else { return nil }
        let json = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
        return DoctorPresenceSummary(
            doctorId: stringValue(json["doctorId"])?.nonEmpty ?? doctorId,
            online: boolValue(json["online"]) ?? false,
            lastUpdated: timeIntervalValue(json["lastUpdated"])
        )
    }

    func updateDoctorPresence(doctorId: String, online: Bool, idToken: String?) async throws {
        let path = "doctors/\(doctorId)"
        let url = try databaseURL(path: path, authToken: idToken)
        let payload: [String: Any] = [
            "doctorId": doctorId,
            "online": online,
            "onlineStatus": online ? "online" : "offline",
            "lastUpdated": Int(Date().timeIntervalSince1970 * 1000)
        ]
        try await patch(payload, url: url)
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

    func fetchChatMessages(chatRoomId: String, currentUserId: String, idToken: String?) async throws -> [ChatMessageSummary] {
        let json = try await fetchParticipantChatMessages(
            chatRoomId: chatRoomId,
            currentUserId: currentUserId,
            idToken: idToken
        )

        let messages = json.compactMap { key, value -> ChatMessageSummary? in
            guard let item = value as? [String: Any] else { return nil }
            let senderId = stringValue(item["senderId"])?.nonEmpty ?? ""
            let receiverId = stringValue(item["receiverId"])?.nonEmpty ?? ""
            let text = stringValue(item["message"])?.nonEmpty ?? ""
            return ChatMessageSummary(
                id: stringValue(item["messageId"])?.nonEmpty ?? key,
                senderId: senderId,
                receiverId: receiverId,
                message: text,
                timestamp: timeIntervalValue(item["timestamp"]) ?? 0,
                isRead: boolValue(item["isRead"]) ?? false,
                isOutgoing: senderId == currentUserId
            )
        }

        return messages.sorted { $0.timestamp < $1.timestamp }
    }

    func sendChatMessage(chatRoomId: String, sender: UserProfile, receiverId: String, receiverName: String, message: String, idToken: String?) async throws {
        let messageId = UUID().uuidString
        let timestamp = Int(Date().timeIntervalSince1970 * 1000)
        let payload: [String: Any] = [
            "messageId": messageId,
            "senderId": sender.userId,
            "senderName": sender.fullName,
            "receiverId": receiverId,
            "receiverName": receiverName,
            "message": message,
            "timestamp": timestamp,
            "isRead": false,
            "messageType": "text",
            "messageStatus": "sent"
        ]
        let url = try databaseURL(path: "messages/\(chatRoomId)/\(messageId)", authToken: idToken)
        try await put(payload, url: url)

        let conversationUpdate: [String: Any] = [
            "otherUserId": receiverId,
            "otherUserName": receiverName,
            "lastMessage": message,
            "lastMessageTimestamp": timestamp,
            "lastMessageSenderId": sender.userId,
            "archived": false
        ]
        let conversationsURL = try databaseURL(path: "user_conversations/\(sender.userId)/\(receiverId)", authToken: idToken)
        try await patch(conversationUpdate, url: conversationsURL)
        let reverseURL = try databaseURL(path: "user_conversations/\(receiverId)/\(sender.userId)", authToken: idToken)
        try await patch(conversationUpdate, url: reverseURL)
    }

    func markChatMessagesRead(chatRoomId: String, currentUserId: String, idToken: String?) async throws {
        let url = try databaseURL(
            path: "messages/\(chatRoomId)",
            authToken: idToken,
            queryItems: [
                URLQueryItem(name: "orderBy", value: "\"receiverId\""),
                URLQueryItem(name: "equalTo", value: "\"\(currentUserId)\"")
            ]
        )
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response: response, data: data)
        guard data != Data("null".utf8) else { return }
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ServiceError.invalidResponse
        }

        for (messageId, value) in json {
            guard let item = value as? [String: Any] else { continue }
            let receiverId = stringValue(item["receiverId"])
            let isRead = boolValue(item["isRead"]) ?? false
            if receiverId == currentUserId && !isRead {
                let messageURL = try databaseURL(path: "messages/\(chatRoomId)/\(messageId)", authToken: idToken)
                try await patch(["isRead": true], url: messageURL)
            }
        }
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
        let encoded = try JSONEncoder().encode(profile)
        guard let fields = try JSONSerialization.jsonObject(with: encoded) as? [String: Any] else {
            throw ServiceError.invalidResponse
        }
        try await patch(fields, url: url)
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

    private func patch(_ dictionary: [String: Any], url: URL) async throws {
        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: dictionary)
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
        let url = try databaseURL(
            path: "messages/\(chatRoomId)",
            authToken: idToken,
            queryItems: [
                URLQueryItem(name: "orderBy", value: "\"receiverId\""),
                URLQueryItem(name: "equalTo", value: "\"\(currentUserId)\"")
            ]
        )
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

    private func fetchParticipantChatMessages(
        chatRoomId: String,
        currentUserId: String,
        idToken: String?
    ) async throws -> [String: Any] {
        let sentURL = try databaseURL(
            path: "messages/\(chatRoomId)",
            authToken: idToken,
            queryItems: [
                URLQueryItem(name: "orderBy", value: "\"senderId\""),
                URLQueryItem(name: "equalTo", value: "\"\(currentUserId)\"")
            ]
        )
        let receivedURL = try databaseURL(
            path: "messages/\(chatRoomId)",
            authToken: idToken,
            queryItems: [
                URLQueryItem(name: "orderBy", value: "\"receiverId\""),
                URLQueryItem(name: "equalTo", value: "\"\(currentUserId)\"")
            ]
        )

        async let sentResponse = URLSession.shared.data(from: sentURL)
        async let receivedResponse = URLSession.shared.data(from: receivedURL)
        let (sentResult, receivedResult) = try await (sentResponse, receivedResponse)
        try validate(response: sentResult.1, data: sentResult.0)
        try validate(response: receivedResult.1, data: receivedResult.0)

        var messages: [String: Any] = [:]
        if sentResult.0 != Data("null".utf8),
           let sent = try JSONSerialization.jsonObject(with: sentResult.0) as? [String: Any] {
            messages.merge(sent) { _, new in new }
        }
        if receivedResult.0 != Data("null".utf8),
           let received = try JSONSerialization.jsonObject(with: receivedResult.0) as? [String: Any] {
            messages.merge(received) { _, new in new }
        }
        return messages
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

    private func databaseURL(
        path: String,
        authToken: String?,
        queryItems: [URLQueryItem] = []
    ) throws -> URL {
        var components = URLComponents(string: "\(HASETConstants.firebaseDatabaseURL)/\(path).json")
        var items = queryItems
        if let authToken {
            items.append(URLQueryItem(name: "auth", value: authToken))
        }
        if !items.isEmpty { components?.queryItems = items }
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
