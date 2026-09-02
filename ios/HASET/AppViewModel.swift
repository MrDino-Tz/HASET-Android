import Foundation
import UIKit

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
    @Published var patientHealthTips: [HealthTipSummary] = []
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
    @Published var pendingDoctorRegistration: PendingDoctorRegistration?
    @Published var blockingDialog: BlockingDialogState?
    @Published var showUnpaidDoctorMessage = false
    @Published var pendingPasswordResetCode: String?

    private let sessionStore = SessionStore()
    private let authService = AuthService()
    private let permissionService = PermissionService.shared
    private var didLoadPatientHomeContent = false
    private var didLoadDoctors = false
    private var didLoadAppointments = false
    private var didLoadConversations = false
    private var didLoadNotifications = false
    private var lastPasswordResetRequestAt: Date?
    private let passwordResetCooldown: TimeInterval = 30
    private var loginAttemptCount = 0
    private var loginLockoutUntil: Date?

    init() {
        selectedLanguage = sessionStore.languageCode
        notificationEnabled = sessionStore.notificationEnabled
        themeMode = sessionStore.themeMode
        locationEnabled = sessionStore.locationEnabled
        NotificationCenter.default.addObserver(
            forName: .hasetFCMTokenUpdated,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            guard let token = notification.object as? String else { return }
            Task { await self?.syncDeviceToken(token) }
        }
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
                blockingDialog = BlockingDialogState(
                    title: "Maintenance Mode",
                    message: config.maintenanceMessage ?? "HASET App is undergoing maintenance. Please try again later.",
                    updateURL: nil
                )
                return
            }

            if let config,
               config.minVersionCode > currentAppVersionCode() {
                blockingDialog = BlockingDialogState(
                    title: "Update Required",
                    message: "A new version of HASET is available. Please update to continue.",
                    updateURL: config.updateUrl
                )
                return
            }

            if let storedSession = sessionStore.loadSession() {
                let session = (try? await authService.refreshSessionIfNeeded(storedSession)) ?? storedSession
                if session.role == .doctor {
                    let pending = (try? await authService.isDoctorRegistrationPending(
                        userId: session.userId,
                        idToken: session.idToken
                    )) ?? false
                    if pending {
                        sessionStore.clearSession()
                        activeSession = nil
                        currentUser = nil
                        showUnpaidDoctorMessage = true
                        route = .login
                        return
                    }
                }
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
        guard let session = activeSession ?? sessionStore.loadSession() else { return }
        do {
            currentUser = try await authService.restoreProfile(session: session)
        } catch {
            // Keep the existing in-memory profile if the refresh fails.
        }
    }

    func currentIdToken() -> String? {
        sessionStore.loadSession()?.idToken
    }

    func refreshDoctorRegistrationFee() async {
        do {
            if let fee = try await authService.fetchAppConfig()?.doctorRegistrationFee,
               fee >= 0 {
                doctorRegistrationFee = fee
            }
        } catch {
            // Keep the most recently loaded amount when temporarily offline.
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
                } else if currentUser?.role == .patient {
                    HealthTipsScheduler.rescheduleDailyTips(tips: patientHealthTips)
                }
            }
        } else {
            notificationEnabled = false
            sessionStore.notificationEnabled = false
            HealthTipsScheduler.cancelAll()
        }
    }

    func handleIncomingURL(_ url: URL) {
        guard PasswordResetLinkParser.isResetPasswordLink(url),
              let code = PasswordResetLinkParser.extractOobCode(from: url.absoluteString) else {
            return
        }
        pendingPasswordResetCode = code
    }

    func openPasswordResetSheet(with linkOrCode: String?) {
        if let linkOrCode, let code = PasswordResetLinkParser.extractOobCode(from: linkOrCode) {
            pendingPasswordResetCode = code
        } else {
            pendingPasswordResetCode = ""
        }
    }

    func confirmPasswordReset(oobCode: String, newPassword: String, confirmPassword: String) async -> Bool {
        let code = PasswordResetLinkParser.extractOobCode(from: oobCode) ?? oobCode
        guard ValidationService.isStrongPassword(newPassword) else {
            alertState = AlertState(title: tr("error"), message: tr("strong_password_required"))
            return false
        }
        guard newPassword == confirmPassword else {
            alertState = AlertState(title: tr("error"), message: tr("passwords_do_not_match"))
            return false
        }
        isLoading = true
        defer { isLoading = false }
        do {
            _ = try await authService.verifyPasswordResetCode(code)
            try await authService.confirmPasswordReset(oobCode: code, newPassword: newPassword)
            pendingPasswordResetCode = nil
            alertState = AlertState(title: tr("success"), message: tr("password_reset_success"))
            route = .login
            return true
        } catch {
            alertState = AlertState(title: tr("error"), message: error.localizedDescription)
            return false
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
        if isLoginLocked() {
            alertState = AlertState(title: tr("error"), message: loginLockoutMessage())
            return
        }

        let resolvedEmail = HASETConstants.resolveLoginEmail(email)
        guard ValidationService.isValidEmail(resolvedEmail) else {
            alertState = AlertState(title: tr("error"), message: tr("valid_email_required"))
            return
        }
        guard ValidationService.isStrongPassword(password) else {
            alertState = AlertState(title: tr("error"), message: tr("strong_password_required"))
            return
        }

        isLoading = true
        Task {
            defer { isLoading = false }
            do {
                let result = try await authService.signIn(email: resolvedEmail, password: password)
                loginAttemptCount = 0
                loginLockoutUntil = nil
                activeSession = result.0
                let mfaEnabled = try await authService.mobileMFAStatus(idToken: result.0.idToken)
                sessionStore.saveSession(result.0)
                activeSession = result.0
                if mfaEnabled {
                    pendingMFASession = result.0
                    pendingMFAProfile = result.1
                    route = .mfaChallenge
                    return
                }
                try await completeAuthenticatedLogin(session: result.0, profile: result.1)
            } catch ServiceError.emailNotVerified {
                loginAttemptCount = 0
                sessionStore.clearSession()
                activeSession = nil
                currentUser = nil
                alertState = AlertState(title: tr("login_failed"), message: tr("verify_email_before_login"))
            } catch {
                loginAttemptCount += 1
                if loginAttemptCount >= HASETConstants.maxLoginAttempts {
                    loginLockoutUntil = Date().addingTimeInterval(HASETConstants.loginLockoutSeconds)
                    loginAttemptCount = 0
                }
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
        Task {
            defer { isLoading = false }
            do {
                _ = try await authService.verifyMobileMFA(code: code, idToken: session.idToken)
                pendingMFASession = nil
                pendingMFAProfile = nil
                try await completeAuthenticatedLogin(session: session, profile: profile)
            } catch {
                mfaError = error.localizedDescription
            }
        }
    }

    func completeMFAEnrollment() async {
        guard let session = pendingMFASession, let profile = pendingMFAProfile else { route = .login; return }
        do {
            guard try await authService.mobileMFAStatus(idToken: session.idToken) else {
                throw ServiceError.message("MFA enrollment was not confirmed.")
            }
            pendingMFASession = nil
            pendingMFAProfile = nil
            try await completeAuthenticatedLogin(session: session, profile: profile)
        } catch {
            alertState = AlertState(title: "MFA enrollment", message: error.localizedDescription)
        }
    }

    func completeDoctorRegistrationPayment() async {
        guard let pending = pendingDoctorRegistration,
              let session = activeSession ?? sessionStore.loadSession() else { return }
        do {
            try await authService.markDoctorRegistrationPaid(userId: session.userId, idToken: session.idToken)
            pendingDoctorRegistration = nil
            if let profile = currentUser {
                try await finishDashboardEntry(session: session, profile: profile)
            }
        } catch {
            alertState = AlertState(title: tr("error"), message: error.localizedDescription)
        }
    }

    func cancelDoctorRegistrationPayment() {
        pendingDoctorRegistration = nil
        sessionStore.clearSession()
        activeSession = nil
        currentUser = nil
        showUnpaidDoctorMessage = true
        route = .login
    }

    func loadDoctorWithdrawals() async {
        guard let session = sessionStore.loadSession() else { return }
        doctorWalletLoading = true; doctorWalletError = nil
        do {
            let rows = try await authService.fetchDoctorWithdrawals(idToken: session.idToken)
            doctorWithdrawals = rows.compactMap { row in
                guard let id = row["request_id"] as? String, let amount = (row["amount"] as? NSNumber)?.doubleValue ?? (row["amount"] as? Double) else { return nil }
                let feeAmount = (row["fee_amount"] as? NSNumber)?.doubleValue ?? (row["fee_amount"] as? Double) ?? 0
                let status = row["status"] as? String ?? "unknown"
                let date = (row["created_at"] as? String).flatMap { ISO8601DateFormatter().date(from: $0) }
                return DoctorWithdrawalSummary(id: id, amount: amount, feeAmount: feeAmount, status: status, createdAt: date, failureReason: row["failure_reason"] as? String)
            }
            await loadDoctorWallet(force: true)
        } catch { doctorWalletError = error.localizedDescription }
        doctorWalletLoading = false
    }

    func register(
        fullName: String,
        email: String,
        phoneDigits: String,
        password: String,
        role: UserRole,
        regNo: String,
        nin: String = "",
        ninDocumentUrl: String? = nil,
        mctCertificateUrl: String? = nil
    ) {
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
        if role == .doctor {
            let trimmedNin = nin.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmedNin.isEmpty {
                alertState = AlertState(title: tr("error"), message: tr("nin_required"))
                return
            }
            if !ValidationService.isValidNin(trimmedNin) {
                alertState = AlertState(title: tr("error"), message: tr("valid_nin_required"))
                return
            }
            if ninDocumentUrl?.isEmpty != false {
                alertState = AlertState(title: tr("error"), message: tr("nin_document_required"))
                return
            }
            if mctCertificateUrl?.isEmpty != false {
                alertState = AlertState(title: tr("error"), message: tr("mct_certificate_required"))
                return
            }
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
                    regNo: role == .doctor ? regNo : nil,
                    nin: role == .doctor ? nin : nil,
                    ninDocumentUrl: role == .doctor ? ninDocumentUrl : nil,
                    mctCertificateUrl: role == .doctor ? mctCertificateUrl : nil
                )
                AuditLogger.shared.logRegistration(profile: result.1, idToken: result.0.idToken)
                try? await authService.sendEmailVerificationViaSmtp(idToken: result.0.idToken)
                sessionStore.clearSession()
                activeSession = nil
                currentUser = nil
                alertState = AlertState(title: tr("registration_successful"), message: tr("verify_email_after_registration"))
                route = .login
            } catch {
                alertState = AlertState(title: tr("registration_failed"), message: error.localizedDescription)
            }
        }
    }

    func sendResetEmail(_ email: String) {
        let resolvedEmail = HASETConstants.resolveLoginEmail(email)
        guard ValidationService.isValidEmail(resolvedEmail) else {
            alertState = AlertState(title: tr("error"), message: tr("valid_email_required"))
            return
        }
        if let lastPasswordResetRequestAt,
           Date().timeIntervalSince(lastPasswordResetRequestAt) < passwordResetCooldown {
            alertState = AlertState(title: tr("success"), message: tr("reset_email_sent"))
            return
        }

        lastPasswordResetRequestAt = Date()
        isLoading = true
        Task {
            defer { isLoading = false }
            do {
                let message = try await authService.sendPasswordReset(email: resolvedEmail)
                alertState = AlertState(title: tr("success"), message: message)
            } catch {
                alertState = AlertState(title: tr("error"), message: error.localizedDescription)
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
        let trimmedAge = age.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedGender = gender.trimmingCharacters(in: .whitespacesAndNewlines)

        guard ValidationService.isValidName(trimmedName) else {
            alertState = AlertState(title: tr("error"), message: tr("name_required"))
            return false
        }
        guard trimmedPhone.isEmpty || ValidationService.isValidPhone(trimmedPhone) else {
            alertState = AlertState(title: tr("error"), message: tr("valid_phone_required"))
            return false
        }
        if !trimmedAge.isEmpty, !(1 ... 120).contains(Int(trimmedAge) ?? 0) {
            alertState = AlertState(title: tr("error"), message: "Enter a valid age between 1 and 120.")
            return false
        }

        profile.fullName = trimmedName
        // Phone is optional for existing profiles; changing age or professional
        // fields must not fail just because the legacy record has no phone.
        profile.phone = trimmedPhone
        profile.age = trimmedAge.isEmpty ? nil : trimmedAge
        profile.gender = trimmedGender.isEmpty ? nil : trimmedGender
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
            // Prefer the session already used by the running app. Refreshing on
            // every profile edit can reject an otherwise valid active token.
            var session = activeSession ?? storedSession
            if let profileImageData {
                profile.profileImage = try await authService.uploadProfileImage(
                    profileImageData,
                    userId: profile.userId
                )
            } else if profile.profileImage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                      let existingImage = try? await authService.fetchProfileImage(userId: profile.userId, idToken: session.idToken),
                      !existingImage.isEmpty {
                // Do not erase a previously uploaded avatar when editing an
                // unrelated field such as age or phone.
                profile.profileImage = existingImage
            }
            do {
                try await authService.updateUserProfile(profile, idToken: session.idToken)
            } catch {
                // If the active token really expired, refresh once and retry the
                // exact same write before asking the user to sign in again.
                let refreshed = try await authService.refreshSessionIfNeeded(storedSession)
                session = refreshed
                sessionStore.saveSession(refreshed)
                activeSession = refreshed
                try await authService.updateUserProfile(profile, idToken: refreshed.idToken)
            }
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
        appointmentType: String,
        paymentConfirmed: Bool = false
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
                guard let storedSession = sessionStore.loadSession() else {
                    throw ServiceError.message("Authentication expired. Please sign in again.")
                }
                let session = try await authService.refreshSessionIfNeeded(storedSession)
                sessionStore.saveSession(session)
                activeSession = session
                let appointmentId = try await authService.createAppointment(
                    patient: currentUser,
                    doctor: doctor,
                    date: trimmedDate,
                    time: trimmedTime,
                    reason: trimmedReason,
                    appointmentType: appointmentType,
                    paymentConfirmed: paymentConfirmed,
                    idToken: session.idToken
                )
                AuditLogger.shared.logAppointmentBooked(
                    profile: currentUser,
                    appointmentId: appointmentId,
                    idToken: session.idToken
                )
                let booked = AppointmentSummary(
                    id: appointmentId,
                    patientId: currentUser.userId,
                    doctorId: doctor.id,
                    title: doctor.name,
                    subtitle: trimmedReason,
                    date: trimmedDate,
                    time: trimmedTime,
                    dateText: trimmedDate,
                    status: .pending,
                    appointmentType: appointmentType,
                    profileImage: doctor.profileImage,
                    createdAt: Date().timeIntervalSince1970 * 1000
                )
                AppointmentReminderService.scheduleReminders(for: booked, doctorName: doctor.name)
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

        do {
            patientHealthTips = try await authService.fetchHealthTips(idToken: idToken)
            if notificationEnabled, currentUser?.role == .patient {
                HealthTipsScheduler.rescheduleDailyTips(tips: patientHealthTips)
            }
        } catch {
            patientHealthTips = []
        }
    }

    func loadDoctorWallet(force: Bool) async {
        guard currentUser?.role == .doctor else { return }
        if doctorWallet != nil && !force { return }

        do {
            guard let session = sessionStore.loadSession() else { doctorWallet = nil; return }
            let freshSession = try await authService.refreshSessionIfNeeded(session)
            sessionStore.saveSession(freshSession)
            activeSession = freshSession
            doctorWallet = try await authService.fetchDoctorWallet(
                doctorId: currentUser?.userId ?? freshSession.userId,
                idToken: freshSession.idToken
            )
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

        guard let storedSession = activeSession ?? sessionStore.loadSession() else {
            alertState = AlertState(title: tr("error"), message: "Authentication expired. Please sign in again.")
            return
        }
        do {
            let userId = storedSession.userId
            do {
                try await authService.updateDoctorPresence(doctorId: userId, online: online, idToken: storedSession.idToken)
            } catch {
                let refreshed = try await authService.refreshSessionIfNeeded(storedSession)
                sessionStore.saveSession(refreshed)
                activeSession = refreshed
                try await authService.updateDoctorPresence(doctorId: userId, online: online, idToken: refreshed.idToken)
            }
            doctorPresence = DoctorPresenceSummary(
                doctorId: userId,
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
            // The Firebase auth UID is authoritative for database rules. Older
            // profile records can contain a stale userId, which causes REST
            // appointment queries to be rejected with Permission denied.
            let authenticatedUserId: String
            if let session = activeSession ?? sessionStore.loadSession() {
                // Use the token obtained by the current login. Refreshing it
                // again immediately can replace a valid token with a failed
                // refresh response and makes Firebase reject the query.
                idToken = session.idToken
                authenticatedUserId = session.userId
            } else {
                idToken = nil
                authenticatedUserId = currentUser.userId
            }
            let result = try await withThrowingTaskGroup(of: [AppointmentSummary].self) { group in
                group.addTask {
                    try await self.authService.fetchAppointments(
                        userId: authenticatedUserId,
                        role: currentUser.role,
                        idToken: idToken
                    )
                }
                group.addTask {
                    try await Task.sleep(nanoseconds: 15_000_000_000)
                    throw ServiceError.message("Appointment loading timed out. Pull to retry.")
                }
                defer { group.cancelAll() }
                return try await group.next()!
            }
            appointments = result
        } catch {
            // Keep already-loaded appointments visible during a transient
            // network failure instead of replacing them with a spinner/blank.
            if appointments.isEmpty {
                appointments = []
            }
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
        let session = activeSession ?? sessionStore.loadSession()
        let userId = session?.userId ?? currentUser.userId
        let idToken = session?.idToken
        do {
            let result = try await authService.fetchConversations(userId: userId, idToken: idToken)
            conversations = result
        } catch {
            conversations = []
        }
    }

    func loadChatMessages(chatRoomId: String, currentUserId: String) async -> [ChatMessageSummary] {
        let idToken = (activeSession ?? sessionStore.loadSession())?.idToken
        do {
            return try await authService.fetchChatMessages(chatRoomId: chatRoomId, currentUserId: currentUserId, idToken: idToken)
        } catch {
            return []
        }
    }

    func loadChatSendAccess(chatRoomId: String, otherUserId: String, role: UserRole) async -> ChatSendAccessSummary {
        let session = activeSession ?? sessionStore.loadSession()
        let currentUserId = session?.userId ?? currentUser?.userId ?? ""
        guard !currentUserId.isEmpty else {
            return ChatSendAccessSummary(canSend: false, message: "Authentication expired. Please sign in again.")
        }
        do {
            return try await authService.fetchChatSendAccess(
                chatRoomId: chatRoomId,
                currentUserId: currentUserId,
                otherUserId: otherUserId,
                role: role,
                idToken: session?.idToken
            )
        } catch {
            return ChatSendAccessSummary(canSend: false, message: error.localizedDescription)
        }
    }

    func loadProfileImage(userId: String) async -> String? {
        let session = activeSession ?? sessionStore.loadSession()
        guard let session else { return nil }
        return try? await authService.fetchProfileImage(userId: userId, idToken: session.idToken)
    }

    func sendChatMessage(chatRoomId: String, receiverId: String, receiverName: String, message: String, messageType: String = "text", attachmentURL: String? = nil, attachmentFileName: String? = nil, replyToMessageID: String? = nil, replyToText: String? = nil) async throws {
        guard let currentUser else { return }
        guard let session = activeSession ?? sessionStore.loadSession() else {
            throw ServiceError.message("Authentication expired. Please sign in again.")
        }
        // Use the authenticated UID for the sender field. Firebase rules
        // reject writes when an old profile record contains a stale userId.
        var sender = currentUser
        sender.userId = session.userId
        let idToken = session.idToken
        try await authService.sendChatMessage(
            chatRoomId: chatRoomId,
            sender: sender,
            receiverId: receiverId,
            receiverName: receiverName,
            message: message,
            messageType: messageType,
            attachmentURL: attachmentURL,
            attachmentFileName: attachmentFileName,
            replyToMessageID: replyToMessageID,
            replyToText: replyToText,
            idToken: idToken
        )
    }

    func uploadChatAttachment(_ data: Data, fileName: String, mimeType: String) async throws -> String {
        try await authService.uploadChatAttachment(data, fileName: fileName, mimeType: mimeType)
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

    func deleteChatMessage(chatRoomId: String, messageId: String) async throws {
        guard let session = activeSession ?? sessionStore.loadSession() else {
            throw ServiceError.message("Authentication expired. Please sign in again.")
        }
        try await authService.deleteChatMessage(chatRoomId: chatRoomId, messageId: messageId, idToken: session.idToken)
    }

    func loadNotifications(force: Bool) async {
        guard let currentUser else { return }
        if didLoadNotifications && !force { return }
        didLoadNotifications = true
        let idToken = sessionStore.loadSession()?.idToken
        do {
            notifications = try await authService.fetchNotifications(userId: currentUser.userId, idToken: idToken)
            await MainActor.run {
                UIApplication.shared.applicationIconBadgeNumber = notifications.filter { !$0.isRead }.count
            }
        } catch {
            notifications = []
        }
    }

    func logout() {
        UIApplication.shared.applicationIconBadgeNumber = 0
        sessionStore.clearSession()
        activeSession = nil
        pendingMFASession = nil
        pendingMFAProfile = nil
        pendingDoctorRegistration = nil
        currentUser = nil
        didLoadPatientHomeContent = false
        didLoadDoctors = false
        didLoadAppointments = false
        didLoadConversations = false
        didLoadNotifications = false
        patientHomeHighlights = []
        patientPopularArticles = []
        patientHealthTips = []
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
                logout()
            } catch {
                alertState = AlertState(title: tr("error"), message: "Unable to delete account.")
            }
        }
    }

    private func completeAuthenticatedLogin(session: StoredSession, profile: UserProfile) async throws {
        activeSession = session
        currentUser = profile
        sessionStore.saveSession(session)

        if profile.role == .doctor {
            let pending = try await authService.isDoctorRegistrationPending(
                userId: session.userId,
                idToken: session.idToken
            )
            if pending {
                await refreshDoctorRegistrationFee()
                if doctorRegistrationFee <= 0 {
                    try await authService.markDoctorRegistrationPaid(userId: session.userId, idToken: session.idToken)
                    try await finishDashboardEntry(session: session, profile: profile)
                    return
                }
                pendingDoctorRegistration = PendingDoctorRegistration(
                    doctor: DoctorSummary(
                        id: "doctor_registration",
                        name: profile.fullName,
                        specialty: "Doctor Registration",
                        hospital: "HASET Hospital",
                        phoneNumber: profile.phone,
                        email: profile.email,
                        address: nil,
                        bio: nil,
                        rating: 0,
                        experienceYears: nil,
                        verified: false,
                        isDemo: false,
                        consultationFee: "TZS \(Int(doctorRegistrationFee))",
                        availableToday: false,
                        profileImage: profile.profileImage,
                        availableTimes: nil
                    ),
                    amount: doctorRegistrationFee
                )
                return
            }
        }

        try await finishDashboardEntry(session: session, profile: profile)
    }

    private func finishDashboardEntry(session: StoredSession, profile: UserProfile) async throws {
        currentUser = profile
        AuditLogger.shared.logLogin(profile: profile, idToken: session.idToken)
        await syncDeviceToken(PushNotificationService.currentFCMToken())
        if profile.role == .patient {
            await loadPatientHomeContent(force: true)
            if notificationEnabled {
                HealthTipsScheduler.rescheduleDailyTips(tips: patientHealthTips)
            }
        } else if profile.role == .doctor {
            await loadDoctorWallet(force: true)
            await loadDoctorPresence(force: true)
        }
        await loadDoctors(force: true)
        await loadAppointments(force: true)
        await loadConversations(force: true)
        await loadNotifications(force: true)
        syncLocationPermission()
        route = .dashboard(profile.role)
    }

    private func syncDeviceToken(_ token: String?) async {
        guard let token, !token.isEmpty,
              let profile = currentUser,
              let session = activeSession ?? sessionStore.loadSession() else { return }
        try? await authService.syncDeviceToken(
            userId: profile.userId,
            token: token,
            idToken: session.idToken
        )
    }

    private func currentAppVersionCode() -> Int {
        Int(Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "0") ?? 0
    }

    private func isLoginLocked() -> Bool {
        guard let loginLockoutUntil else { return false }
        return Date() < loginLockoutUntil
    }

    private func loginLockoutMessage() -> String {
        guard let loginLockoutUntil else { return tr("login_failed") }
        let minutes = max(1, Int(ceil(loginLockoutUntil.timeIntervalSinceNow / 60)))
        return tr("login_locked").replacingOccurrences(of: "%1$d", with: String(minutes))
    }
}
