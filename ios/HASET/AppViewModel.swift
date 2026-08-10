import Foundation

@MainActor
final class AppViewModel: ObservableObject {
    @Published var route: AppRoute = .splash
    @Published var currentUser: UserProfile?
    @Published var activeSession: StoredSession?
    @Published var isLoading = false
    @Published var alertState: AlertState?
    @Published var selectedLanguage: String
    @Published var pendingResetEmail = ""
    @Published var notificationEnabled: Bool
    @Published var themeMode: ThemeMode
    @Published var locationEnabled: Bool
    @Published var patientHomeHighlights: [HomeHighlight] = []
    @Published var patientPopularArticles: [ArticleSummary] = []
    @Published var doctors: [DoctorSummary] = []
    @Published var appointments: [AppointmentSummary] = []
    @Published var conversations: [ConversationSummary] = []
    @Published var notifications: [NotificationSummary] = []
    @Published var doctorWallet: DoctorWalletSummary?
    @Published var doctorPresence: DoctorPresenceSummary?
    @Published var doctorRegistrationFee: Double = 500
    @Published var doctorWithdrawals: [DoctorWithdrawalSummary] = []
    @Published var doctorWalletLoading = false
    @Published var doctorWalletError: String?
    @Published var mfaError: String?

    private let sessionStore = SessionStore()
    private let authService = AuthService()
    private let permissionService = PermissionService.shared
    private var didLoadPatientHomeContent = false
    private var didLoadDoctors = false
    private var didLoadAppointments = false
    private var didLoadConversations = false
    private var didLoadNotifications = false

    init() {
        selectedLanguage = sessionStore.languageCode
        notificationEnabled = sessionStore.notificationEnabled
        themeMode = sessionStore.themeMode
        locationEnabled = sessionStore.locationEnabled
        Task {
            await bootstrap()
        }
    }

    func tr(_ key: String) -> String {
        L10n.tr(key, languageCode: selectedLanguage)
    }

    func bootstrap() async {
        // Match Android: first launch goes straight to onboarding instead of
        // waiting on remote configuration behind the splash screen.
        if !sessionStore.onboardingSeen {
            route = .onboarding
            return
        }
        do {
            let config = try await authService.fetchAppConfig()
            try? await Task.sleep(for: .milliseconds(2500))

            if let config {
                doctorRegistrationFee = config.doctorRegistrationFee ?? doctorRegistrationFee
            }

            if let config, config.maintenanceMode {
                alertState = AlertState(
                    title: "Maintenance Mode",
                    message: config.maintenanceMessage ?? "HASET App is undergoing maintenance. Please try again later."
                )
                return
            }

            if let storedSession = sessionStore.loadSession() {
                let session = (try? await authService.refreshSessionIfNeeded(storedSession)) ?? storedSession
                sessionStore.saveSession(session)
                activeSession = session
                currentUser = try? await authService.restoreProfile(session: session)
                if currentUser == nil {
                    currentUser = UserProfile(
                        userId: session.userId,
                        email: session.email,
                        fullName: session.userName,
                        phone: session.phone,
                        role: session.role,
                        profileImage: "",
                        createdAt: Date().timeIntervalSince1970 * 1000,
                        regNo: nil,
                        gender: nil,
                        age: nil,
                        location: nil,
                        bio: nil,
                        specialization: nil,
                        consultationFee: nil,
                        availableTimes: nil,
                        verified: nil
                    )
                }
                if session.role == .patient {
                    await loadPatientHomeContent(force: false)
                } else if session.role == .doctor {
                    await loadDoctorWallet(force: false)
                    await loadDoctorPresence(force: false)
                }
                await loadDoctors(force: false)
                await loadAppointments(force: false)
                await loadConversations(force: false)
                await loadNotifications(force: false)
                syncLocationPermission()
                route = .dashboard(session.role)
            } else {
                route = .login
            }
        } catch {
            try? await Task.sleep(for: .milliseconds(2500))
            route = sessionStore.onboardingSeen ? .login : .onboarding
        }
    }

    func refreshCurrentUser() async {
        guard let session = sessionStore.loadSession() else { return }
        do {
            currentUser = try await authService.restoreProfile(session: session)
        } catch {
            // Keep the existing in-memory profile if the refresh fails.
        }
    }

    func currentIdToken() -> String? {
        sessionStore.loadSession()?.idToken
    }

    func completeOnboarding() {
        sessionStore.onboardingSeen = true
        route = .login
    }

    func changeLanguage(_ code: String) {
        selectedLanguage = code
        sessionStore.languageCode = code
    }

    func setNotificationEnabled(_ enabled: Bool) {
        if enabled {
            Task {
                let granted = await permissionService.requestNotifications()
                notificationEnabled = granted
                sessionStore.notificationEnabled = granted
                if !granted {
                    alertState = AlertState(title: tr("permission_required"), message: tr("notifications_permission_message"))
                }
            }
        } else {
            notificationEnabled = false
            sessionStore.notificationEnabled = false
        }
    }

    func setThemeMode(_ mode: ThemeMode) {
        themeMode = mode
        sessionStore.themeMode = mode
    }

    func setLocationEnabled(_ enabled: Bool) {
        if enabled {
            Task {
                let granted = await permissionService.requestLocation()
                locationEnabled = granted
                sessionStore.locationEnabled = granted
                if !granted {
                    alertState = AlertState(title: tr("permission_required"), message: tr("location_permission_message"))
                }
            }
        } else {
            locationEnabled = false
            sessionStore.locationEnabled = false
        }
    }

    func syncLocationPermission() {
        let granted = permissionService.currentLocationEnabled()
        locationEnabled = granted
        sessionStore.locationEnabled = granted
    }

    func showRegister(role: UserRole) {
        route = .register(role)
    }

    func showForgotPassword() {
        route = .forgotPassword
    }

    func showRoleSelection() {
        route = .roleSelection
    }

    func showLogin() {
        route = .login
    }

    func login(email: String, password: String) {
        guard ValidationService.isValidEmail(email) else {
            alertState = AlertState(title: tr("error"), message: tr("valid_email_required"))
            return
        }
        guard ValidationService.isValidPassword(password) else {
            alertState = AlertState(title: tr("error"), message: tr("password_too_short"))
            return
        }

        isLoading = true
        Task {
            defer { isLoading = false }
            do {
                let result = try await authService.signIn(email: email, password: password)
                // Keep the freshly authenticated Firebase session available to
                // the MFA route even if SwiftUI recreates the enrollment view.
                activeSession = result.0
                let mfaEnabled = try await authService.mobileMFAStatus(idToken: result.0.idToken)
                sessionStore.saveSession(result.0)
                activeSession = result.0
                if mfaEnabled { pendingMFASession = result.0; pendingMFAProfile = result.1; route = .mfaChallenge; return }
                currentUser = result.1
                if result.1.role == .patient { await loadPatientHomeContent(force: true) }
                await loadDoctors(force: true); await loadAppointments(force: true); await loadConversations(force: true); await loadNotifications(force: true)
                route = .dashboard(result.1.role)
            } catch {
                alertState = AlertState(title: tr("login_failed"), message: error.localizedDescription)
            }
        }
    }

    @Published var pendingMFASession: StoredSession?
    @Published var pendingMFAProfile: UserProfile?
    func verifyLoginMFA(code: String) {
        guard let session = pendingMFASession, let profile = pendingMFAProfile else { route = .login; return }
        guard code.count == 6 || code.count >= 8 else { mfaError = "Enter a valid authenticator or recovery code."; return }
        isLoading = true
        Task { defer { isLoading = false }; do { _ = try await authService.verifyMobileMFA(code: code, idToken: session.idToken); currentUser = profile; pendingMFASession = nil; pendingMFAProfile = nil; route = .dashboard(profile.role) } catch { mfaError = error.localizedDescription } }
    }

    func completeMFAEnrollment() async {
        guard let session = pendingMFASession, let profile = pendingMFAProfile else { route = .login; return }
        do {
            guard try await authService.mobileMFAStatus(idToken: session.idToken) else { throw ServiceError.message("MFA enrollment was not confirmed.") }
            currentUser = profile
            if profile.role == .patient { await loadPatientHomeContent(force: true) }
            await loadDoctors(force: true); await loadAppointments(force: true); await loadConversations(force: true); await loadNotifications(force: true)
            pendingMFASession = nil; pendingMFAProfile = nil; route = .dashboard(profile.role)
        } catch { alertState = AlertState(title: "MFA enrollment", message: error.localizedDescription) }
    }

    func loadDoctorWithdrawals() async {
        guard let session = sessionStore.loadSession() else { return }
        doctorWalletLoading = true; doctorWalletError = nil
        do {
            let rows = try await authService.fetchDoctorWithdrawals(idToken: session.idToken)
            doctorWithdrawals = rows.compactMap { row in
                guard let id = row["request_id"] as? String, let amount = (row["amount"] as? NSNumber)?.doubleValue ?? (row["amount"] as? Double) else { return nil }
                let status = row["status"] as? String ?? "unknown"
                let date = (row["created_at"] as? String).flatMap { ISO8601DateFormatter().date(from: $0) }
                return DoctorWithdrawalSummary(id: id, amount: amount, status: status, createdAt: date, failureReason: row["failure_reason"] as? String)
            }
            await loadDoctorWallet(force: true)
        } catch { doctorWalletError = error.localizedDescription }
        doctorWalletLoading = false
    }

    func register(fullName: String, email: String, phoneDigits: String, password: String, role: UserRole, regNo: String) {
        let formattedPhone = phoneDigits.hasPrefix("+255") ? phoneDigits : "+255\(phoneDigits)"

        guard ValidationService.isValidName(fullName) else {
            alertState = AlertState(title: tr("error"), message: tr("name_required"))
            return
        }
        guard ValidationService.isValidEmail(email) else {
            alertState = AlertState(title: tr("error"), message: tr("valid_email_required"))
            return
        }
        guard ValidationService.isValidPhone(formattedPhone) else {
            alertState = AlertState(title: tr("error"), message: tr("valid_phone_required"))
            return
        }
        guard ValidationService.isStrongPassword(password) else {
            alertState = AlertState(title: tr("error"), message: tr("strong_password_required"))
            return
        }
        if role == .doctor && regNo.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            alertState = AlertState(title: tr("error"), message: tr("mct_required"))
            return
        }

        isLoading = true
        Task {
            defer { isLoading = false }
            do {
                let result = try await authService.register(
                    email: email,
                    password: password,
                    fullName: fullName,
                    phone: formattedPhone,
                    role: role,
                    regNo: role == .doctor ? regNo : nil
                )
                sessionStore.saveSession(result.0)
                currentUser = result.1
                if role == .patient {
                    await loadPatientHomeContent(force: true)
                } else if role == .doctor {
                    await loadDoctorWallet(force: true)
                    await loadDoctorPresence(force: true)
                }
                await loadDoctors(force: true)
                await loadAppointments(force: true)
                await loadConversations(force: true)
                await loadNotifications(force: true)
                alertState = AlertState(title: tr("registration_successful"), message: "Welcome to HASET!")
                route = .dashboard(role)
            } catch {
                alertState = AlertState(title: tr("registration_failed"), message: error.localizedDescription)
            }
        }
    }

    func sendResetEmail(_ email: String) {
        guard ValidationService.isValidEmail(email) else {
            alertState = AlertState(title: tr("error"), message: tr("valid_email_required"))
            return
        }

        isLoading = true
        Task {
            defer { isLoading = false }
            do {
                try await authService.sendPasswordReset(email: email)
                alertState = AlertState(title: tr("success"), message: tr("reset_email_sent"))
            } catch {
                alertState = AlertState(title: tr("reset_failed"), message: error.localizedDescription)
            }
        }
    }

    func saveProfile(
        fullName: String,
        phone: String,
        age: String,
        gender: String,
        bio: String,
        specialization: String,
        consultationFee: String,
        availableTimes: [String],
        profileImageData: Data?
    ) async -> Bool {
        guard var profile = currentUser else { return false }
        let trimmedName = fullName.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPhone = phone.trimmingCharacters(in: .whitespacesAndNewlines)

        guard ValidationService.isValidName(trimmedName) else {
            alertState = AlertState(title: tr("error"), message: tr("name_required"))
            return false
        }
        guard ValidationService.isValidPhone(trimmedPhone) else {
            alertState = AlertState(title: tr("error"), message: tr("valid_phone_required"))
            return false
        }

        profile.fullName = trimmedName
        profile.phone = trimmedPhone
        profile.age = age.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : age
        profile.gender = gender.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : gender
        profile.bio = bio.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : bio
        profile.specialization = specialization.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : specialization
        profile.consultationFee = consultationFee.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : consultationFee
        profile.availableTimes = availableTimes.isEmpty ? nil : availableTimes

        isLoading = true
        defer { isLoading = false }
        guard let storedSession = sessionStore.loadSession() else {
            alertState = AlertState(title: tr("update_failed"), message: "Authentication expired. Please sign in again.")
            return false
        }

        do {
            let session = try await authService.refreshSessionIfNeeded(storedSession)
            sessionStore.saveSession(session)
            activeSession = session
            if let profileImageData {
                profile.profileImage = try await authService.uploadProfileImage(
                    profileImageData,
                    userId: profile.userId
                )
            }
            try await authService.updateUserProfile(profile, idToken: session.idToken)
            currentUser = profile
            sessionStore.saveSession(
                StoredSession(
                    userId: session.userId,
                    idToken: session.idToken,
                    refreshToken: session.refreshToken,
                    role: session.role,
                    userName: profile.fullName,
                    email: session.email,
                    phone: profile.phone
                )
            )
            alertState = AlertState(title: tr("success"), message: tr("profile_updated_successfully"))
            return true
        } catch {
            alertState = AlertState(title: tr("update_failed"), message: error.localizedDescription)
            return false
        }
    }

    func submitBugReport(_ message: String) {
        let trimmed = message.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            alertState = AlertState(title: tr("error"), message: tr("describe_issue_first"))
            return
        }
        alertState = AlertState(title: tr("report_sent"), message: tr("report_recorded"))
    }

    func bookAppointment(
        doctor: DoctorSummary,
        date: String,
        time: String,
        reason: String,
        appointmentType: String
    ) {
        guard let currentUser else { return }
        let trimmedDate = date.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedTime = time.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedReason = reason.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedDate.isEmpty else {
            alertState = AlertState(title: tr("error"), message: tr("select_date_required"))
            return
        }
        guard !trimmedTime.isEmpty else {
            alertState = AlertState(title: tr("error"), message: tr("select_time_required"))
            return
        }

        isLoading = true
        Task {
            defer { isLoading = false }
            do {
                let idToken = sessionStore.loadSession()?.idToken
                try await authService.createAppointment(
                    patient: currentUser,
                    doctor: doctor,
                    date: trimmedDate,
                    time: trimmedTime,
                    reason: trimmedReason,
                    appointmentType: appointmentType,
                    idToken: idToken
                )
                await loadAppointments(force: true)
                alertState = AlertState(
                    title: tr("booking_successful"),
                    message: appointmentType == "Online Chat"
                        ? tr("chat_booking_success")
                        : tr("appointment_booking_success")
                )
            } catch {
                alertState = AlertState(title: tr("booking_failed"), message: error.localizedDescription)
            }
        }
    }

    func loadPatientHomeContent(force: Bool) async {
        guard currentUser?.role == .patient else { return }
        if didLoadPatientHomeContent && !force { return }
        didLoadPatientHomeContent = true

        let idToken = sessionStore.loadSession()?.idToken

        do {
            async let bannersTask = authService.fetchPromotionalBanners(idToken: idToken)
            async let articlesTask = authService.fetchPopularArticles(idToken: idToken)

            let (banners, articles) = try await (bannersTask, articlesTask)
            patientHomeHighlights = banners
            patientPopularArticles = articles
        } catch {
            patientHomeHighlights = []
            patientPopularArticles = []
        }
    }

    func loadDoctorWallet(force: Bool) async {
        guard currentUser?.role == .doctor else { return }
        if doctorWallet != nil && !force { return }

        let idToken = sessionStore.loadSession()?.idToken
        do {
            doctorWallet = try await authService.fetchDoctorWallet(doctorId: currentUser?.userId ?? "", idToken: idToken)
        } catch {
            doctorWallet = nil
        }
    }

    func loadDoctorPresence(force: Bool) async {
        guard currentUser?.role == .doctor else { return }
        if doctorPresence != nil && !force { return }

        let idToken = sessionStore.loadSession()?.idToken
        do {
            doctorPresence = try await authService.fetchDoctorPresence(doctorId: currentUser?.userId ?? "", idToken: idToken)
        } catch {
            doctorPresence = nil
        }
    }

    func setDoctorPresence(online: Bool) async {
        guard currentUser?.role == .doctor else { return }

        guard let storedSession = sessionStore.loadSession() else {
            alertState = AlertState(title: tr("error"), message: "Authentication expired. Please sign in again.")
            return
        }
        do {
            let session = try await authService.refreshSessionIfNeeded(storedSession)
            sessionStore.saveSession(session)
            activeSession = session
            try await authService.updateDoctorPresence(doctorId: currentUser?.userId ?? "", online: online, idToken: session.idToken)
            doctorPresence = DoctorPresenceSummary(
                doctorId: currentUser?.userId ?? "",
                online: online,
                lastUpdated: Date().timeIntervalSince1970 * 1000
            )
        } catch {
            alertState = AlertState(title: tr("error"), message: "Unable to update doctor status.")
        }
    }

    func loadDoctors(force: Bool) async {
        if didLoadDoctors && !force { return }
        didLoadDoctors = true
        do {
            let idToken: String?
            if let storedSession = sessionStore.loadSession() {
                let session = try await authService.refreshSessionIfNeeded(storedSession)
                sessionStore.saveSession(session)
                activeSession = session
                idToken = session.idToken
            } else {
                idToken = nil
            }
            let result = try await authService.fetchDoctors(idToken: idToken)
            doctors = result
        } catch {
            doctors = []
            didLoadDoctors = false
        }
    }

    func loadAppointments(force: Bool) async {
        guard let currentUser else { return }
        if didLoadAppointments && !force { return }
        didLoadAppointments = true
        do {
            let idToken: String?
            if let storedSession = sessionStore.loadSession() {
                let session = try await authService.refreshSessionIfNeeded(storedSession)
                sessionStore.saveSession(session)
                activeSession = session
                idToken = session.idToken
            } else {
                idToken = nil
            }
            let result = try await authService.fetchAppointments(
                userId: currentUser.userId,
                role: currentUser.role,
                idToken: idToken
            )
            appointments = result
        } catch {
            appointments = []
            didLoadAppointments = false
            alertState = AlertState(title: tr("error"), message: "Unable to load appointments: \(error.localizedDescription)")
        }
    }

    func updateAppointmentStatus(appointmentId: String, status: String) async -> Bool {
        do {
            guard let storedSession = sessionStore.loadSession() else {
                throw ServiceError.message("Authentication expired. Please sign in again.")
            }
            let session = try await authService.refreshSessionIfNeeded(storedSession)
            sessionStore.saveSession(session)
            activeSession = session
            try await authService.updateAppointmentStatus(
                appointmentId: appointmentId,
                status: status,
                idToken: session.idToken
            )
            await loadAppointments(force: true)
            return true
        } catch {
            alertState = AlertState(title: tr("update_failed"), message: error.localizedDescription)
            return false
        }
    }

    func rescheduleAppointment(appointmentId: String, date: String, time: String) async -> Bool {
        guard let currentUser else { return false }
        do {
            guard let storedSession = sessionStore.loadSession() else {
                throw ServiceError.message("Authentication expired. Please sign in again.")
            }
            let session = try await authService.refreshSessionIfNeeded(storedSession)
            sessionStore.saveSession(session)
            activeSession = session
            try await authService.rescheduleAppointment(
                appointmentId: appointmentId,
                date: date,
                time: time,
                rescheduledBy: currentUser.userId,
                idToken: session.idToken
            )
            await loadAppointments(force: true)
            return true
        } catch {
            alertState = AlertState(title: tr("update_failed"), message: error.localizedDescription)
            return false
        }
    }

    func loadConversations(force: Bool) async {
        guard let currentUser else { return }
        if didLoadConversations && !force { return }
        didLoadConversations = true
        let idToken = sessionStore.loadSession()?.idToken
        do {
            let result = try await authService.fetchConversations(userId: currentUser.userId, idToken: idToken)
            conversations = result
        } catch {
            conversations = []
        }
    }

    func loadChatMessages(chatRoomId: String, currentUserId: String) async -> [ChatMessageSummary] {
        let idToken = sessionStore.loadSession()?.idToken
        do {
            return try await authService.fetchChatMessages(chatRoomId: chatRoomId, currentUserId: currentUserId, idToken: idToken)
        } catch {
            return []
        }
    }

    func sendChatMessage(chatRoomId: String, receiverId: String, receiverName: String, message: String) async throws {
        guard let currentUser else { return }
        let idToken = sessionStore.loadSession()?.idToken
        try await authService.sendChatMessage(
            chatRoomId: chatRoomId,
            sender: currentUser,
            receiverId: receiverId,
            receiverName: receiverName,
            message: message,
            idToken: idToken
        )
    }

    func markChatMessagesRead(chatRoomId: String) async {
        guard let currentUser else { return }
        let idToken = sessionStore.loadSession()?.idToken
        do {
            try await authService.markChatMessagesRead(chatRoomId: chatRoomId, currentUserId: currentUser.userId, idToken: idToken)
        } catch {
            // Ignore read-receipt sync failures.
        }
    }

    func loadNotifications(force: Bool) async {
        guard let currentUser else { return }
        if didLoadNotifications && !force { return }
        didLoadNotifications = true
        let idToken = sessionStore.loadSession()?.idToken
        do {
            notifications = try await authService.fetchNotifications(userId: currentUser.userId, idToken: idToken)
        } catch {
            notifications = []
        }
    }

    func logout() {
        sessionStore.clearSession()
        activeSession = nil
        pendingMFASession = nil
        pendingMFAProfile = nil
        currentUser = nil
        didLoadPatientHomeContent = false
        didLoadDoctors = false
        didLoadAppointments = false
        didLoadConversations = false
        didLoadNotifications = false
        patientHomeHighlights = []
        patientPopularArticles = []
        doctors = []
        appointments = []
        conversations = []
        notifications = []
        route = .login
    }

    func deleteAccount() {
        guard let user = currentUser else { return }
        let idToken = sessionStore.loadSession()?.idToken
        Task {
            do {
                try await authService.deleteCurrentAccount(userId: user.userId, role: user.role, idToken: idToken)
            } catch {
                alertState = AlertState(title: tr("error"), message: "Unable to delete account.")
            }
            logout()
        }
    }
}
