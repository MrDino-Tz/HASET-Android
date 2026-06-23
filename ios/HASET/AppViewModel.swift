import Foundation

@MainActor
final class AppViewModel: ObservableObject {
    @Published var route: AppRoute = .splash
    @Published var currentUser: UserProfile?
    @Published var isLoading = false
    @Published var alertState: AlertState?
    @Published var selectedLanguage: String
    @Published var pendingResetEmail = ""
    @Published var notificationEnabled: Bool
    @Published var themeMode: ThemeMode
    @Published var locationEnabled: Bool
    @Published var patientHomeHighlights: [HomeHighlight] = StaticContentService.homeHighlights
    @Published var patientPopularArticles: [ArticleSummary] = []
    @Published var doctors: [DoctorSummary] = []
    @Published var appointments: [AppointmentSummary] = []
    @Published var conversations: [ConversationSummary] = []
    @Published var notifications: [NotificationSummary] = []

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
        do {
            let config = try await authService.fetchAppConfig()
            try? await Task.sleep(for: .milliseconds(2500))

            if let config, config.maintenanceMode {
                alertState = AlertState(
                    title: "Maintenance Mode",
                    message: config.maintenanceMessage ?? "HASET App is undergoing maintenance. Please try again later."
                )
                return
            }

            if !sessionStore.onboardingSeen {
                route = .onboarding
                return
            }

            if let session = sessionStore.loadSession() {
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
                }
                await loadDoctors(force: false)
                await loadAppointments(force: false)
                await loadConversations(force: false)
                await loadNotifications(force: false)
                route = .dashboard(session.role)
            } else {
                route = .login
            }
        } catch {
            try? await Task.sleep(for: .milliseconds(2500))
            route = sessionStore.onboardingSeen ? .login : .onboarding
        }
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
                sessionStore.saveSession(result.0)
                currentUser = result.1
                if result.1.role == .patient {
                    await loadPatientHomeContent(force: true)
                }
                await loadDoctors(force: true)
                await loadAppointments(force: true)
                await loadConversations(force: true)
                await loadNotifications(force: true)
                route = .dashboard(result.1.role)
            } catch {
                alertState = AlertState(title: tr("login_failed"), message: error.localizedDescription)
            }
        }
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
        guard ValidationService.isValidPassword(password) else {
            alertState = AlertState(title: tr("error"), message: tr("password_too_short"))
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
        location: String,
        bio: String,
        specialization: String,
        consultationFee: String,
        profileImage: String
    ) {
        guard var profile = currentUser else { return }
        let trimmedName = fullName.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPhone = phone.trimmingCharacters(in: .whitespacesAndNewlines)

        guard ValidationService.isValidName(trimmedName) else {
            alertState = AlertState(title: tr("error"), message: tr("name_required"))
            return
        }
        guard ValidationService.isValidPhone(trimmedPhone) else {
            alertState = AlertState(title: tr("error"), message: tr("valid_phone_required"))
            return
        }

        profile.fullName = trimmedName
        profile.phone = trimmedPhone
        profile.age = age.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : age
        profile.gender = gender.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : gender
        profile.location = location.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : location
        profile.bio = bio.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : bio
        profile.specialization = specialization.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : specialization
        profile.consultationFee = consultationFee.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : consultationFee
        profile.profileImage = profileImage

        isLoading = true
        Task {
            defer { isLoading = false }
            guard let session = sessionStore.loadSession() else {
                currentUser = profile
                alertState = AlertState(title: tr("success"), message: tr("profile_updated_successfully"))
                return
            }

            do {
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
            } catch {
                alertState = AlertState(title: tr("update_failed"), message: error.localizedDescription)
            }
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
            if !banners.isEmpty {
                patientHomeHighlights = banners
            } else {
                patientHomeHighlights = StaticContentService.homeHighlights
            }
            patientPopularArticles = articles
        } catch {
            patientHomeHighlights = StaticContentService.homeHighlights
            patientPopularArticles = []
        }
    }

    func loadDoctors(force: Bool) async {
        if didLoadDoctors && !force { return }
        didLoadDoctors = true
        let idToken = sessionStore.loadSession()?.idToken
        do {
            let result = try await authService.fetchDoctors(idToken: idToken)
            doctors = result
        } catch {
            doctors = []
        }
    }

    func loadAppointments(force: Bool) async {
        guard let currentUser else { return }
        if didLoadAppointments && !force { return }
        didLoadAppointments = true
        let idToken = sessionStore.loadSession()?.idToken
        do {
            let result = try await authService.fetchAppointments(userId: currentUser.userId, role: currentUser.role, idToken: idToken)
            appointments = result
        } catch {
            appointments = []
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

    func loadNotifications(force: Bool) async {
        guard let currentUser else { return }
        if didLoadNotifications && !force { return }
        didLoadNotifications = true
        let idToken = sessionStore.loadSession()?.idToken
        do {
            notifications = try await authService.fetchNotifications(userId: currentUser.userId, idToken: idToken)
        } catch {
            notifications = StaticContentService.recentNotifications.enumerated().map { index, item in
                NotificationSummary(
                    id: "fallback-\(index)",
                    title: item,
                    message: item,
                    type: "general",
                    isRead: false,
                    timestamp: Date().timeIntervalSince1970 * 1000 - Double(index * 1000)
                )
            }
        }
    }

    func logout() {
        sessionStore.clearSession()
        currentUser = nil
        didLoadPatientHomeContent = false
        didLoadDoctors = false
        didLoadAppointments = false
        didLoadConversations = false
        didLoadNotifications = false
        patientHomeHighlights = StaticContentService.homeHighlights
        patientPopularArticles = []
        doctors = StaticContentService.doctors
        appointments = []
        conversations = []
        notifications = []
        route = .login
    }
}
