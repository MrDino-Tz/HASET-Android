import SwiftUI
import CoreImage.CIFilterBuiltins
import UIKit
import UniformTypeIdentifiers

struct SixDigitMFAInput: View {
    @Binding var code: String
    let isInvalid: Bool
    let isVerified: Bool
    let onComplete: () -> Void
    @FocusState private var focused: Bool

    var body: some View {
        ZStack {
            HStack(spacing: 8) {
                ForEach(0..<6, id: \.self) { index in
                    Text(maskedDigit(at: index))
                        .font(.system(size: 20, weight: .semibold, design: .rounded))
                        .foregroundStyle(HASETTheme.textPrimary)
                        .frame(width: 40, height: 50)
                        .overlay {
                            RoundedRectangle(cornerRadius: 6, style: .continuous)
                                .stroke(borderColor(at: index), lineWidth: isActive(index) ? 2 : 1)
                        }
                }
            }
            TextField("", text: $code)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .focused($focused)
                .foregroundStyle(Color.clear)
                .tint(Color.clear)
                .frame(maxWidth: .infinity, minHeight: 56)
                .onChange(of: code) { value in
                    let sanitized = String(value.filter(\.isNumber).prefix(6))
                    if sanitized != value { code = sanitized }
                    if sanitized.count == 6 { onComplete() }
                }
        }
        .contentShape(Rectangle())
        .onTapGesture { focused = true }
        .accessibilityLabel("Six digit verification code")
    }

    private func maskedDigit(at index: Int) -> String { index < code.count ? "•" : "" }

    private func isActive(_ index: Int) -> Bool {
        focused && code.count < 6 && index == code.count
    }

    private func borderColor(at index: Int) -> Color {
        if isInvalid { return .red }
        if isVerified || isActive(index) { return HASETTheme.greenPrimary }
        return HASETTheme.textSecondary.opacity(0.3)
    }
}

struct LanguageToggle: View {
    @EnvironmentObject private var appViewModel: AppViewModel

    var body: some View {
        HStack(spacing: 8) {
            toggleItem(code: "en", label: "EN")
            toggleItem(code: "sw", label: "SW")
        }
        .padding(4)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color.white)
        )
    }

    private func toggleItem(code: String, label: String) -> some View {
        Button {
            appViewModel.changeLanguage(code)
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "globe")
                    .foregroundStyle(HASETTheme.greenPrimary)
                Text(label)
                    .font(HASETTheme.font(.medium, 13))
                    .foregroundStyle(HASETTheme.textSecondary)
            }
            .padding(.horizontal, 12)
            .frame(height: 40)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(appViewModel.selectedLanguage == code ? HASETTheme.backgroundPrimary : Color.clear)
            )
        }
        .buttonStyle(.plain)
    }
}

struct RoundedInputField: View {
    let title: String
    let systemImage: String
    @Binding var text: String
    var isSecure = false
    var prefix: String?
    var keyboardType: UIKeyboardType = .default

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: systemImage)
                .foregroundStyle(HASETTheme.greenPrimary)
                .frame(width: 20)

            if let prefix {
                Text(prefix)
                    .font(HASETTheme.font(.regular, 14))
                    .foregroundStyle(HASETTheme.textSecondary)
            }

            Group {
                if isSecure {
                    SecureField(title, text: $text)
                } else {
                    TextField(title, text: $text)
                        .keyboardType(keyboardType)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
            }
            .font(HASETTheme.font(.regular, 14))
            .foregroundStyle(HASETTheme.textPrimary)
        }
        .padding(.horizontal, 18)
        .frame(height: 58)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color.white)
                .shadow(color: HASETTheme.greenPrimary.opacity(0.08), radius: 10, x: 0, y: 6)
        )
    }
}

struct SplashView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var typedTitle = ""

    var body: some View {
        ZStack {
            HASETTheme.backgroundPrimary
                .ignoresSafeArea()

            VStack {
                Spacer()

                VStack(spacing: 16) {
                    Image("SplashLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 140, height: 140)

                    Text(typedTitle)
                        .font(HASETTheme.font(.black, 37))
                        .foregroundStyle(HASETTheme.textPrimary)
                        .onAppear {
                            startTypewriterAnimation()
                        }
                }
                .frame(maxWidth: .infinity)

                Spacer()

                Text(appViewModel.tr("tagline"))
                    .font(HASETTheme.font(.regular, 20))
                    .foregroundStyle(HASETTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                    .padding(.bottom, 48)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func startTypewriterAnimation() {
        guard typedTitle.isEmpty else { return }
        let fullTitle = appViewModel.tr("app_name")
        for (index, _) in fullTitle.enumerated() {
            let delay = Double(index) * 0.15
            DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
                typedTitle = String(fullTitle.prefix(index + 1))
            }
        }
    }
}

struct OnboardingView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var page = 0

    private let pages: [(image: String, title: String, description: String)] = [
        ("OnboardingOne", "welcome", "welcome_desc"),
        ("OnboardingTwo", "chat_securely", "chat_securely_desc"),
        ("OnboardingThree", "health_at_hand", "health_at_hand_desc")
    ]

    var body: some View {
        VStack(spacing: 24) {
            HStack {
                Spacer()
                LanguageToggle()
            }
            .padding(.horizontal, 24)
            .padding(.top, 20)

            TabView(selection: $page) {
                ForEach(Array(pages.enumerated()), id: \.offset) { index, item in
                    VStack(spacing: 24) {
                        Image(item.image)
                            .resizable()
                            .scaledToFit()
                            .frame(maxHeight: 320)
                            .padding(.horizontal, 24)

                        Text(appViewModel.tr(item.title))
                            .font(HASETTheme.font(.medium, 28))
                            .foregroundStyle(HASETTheme.textPrimary)

                        Text(appViewModel.tr(item.description))
                            .font(HASETTheme.font(.regular, 16))
                            .foregroundStyle(HASETTheme.textSecondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 40)
                    }
                    .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .always))

            Button(page == pages.count - 1 ? appViewModel.tr("get_started") : appViewModel.tr("next")) {
                if page == pages.count - 1 {
                    appViewModel.completeOnboarding()
                } else {
                    withAnimation {
                        page += 1
                    }
                }
            }
            .buttonStyle(PrimaryButtonStyle())
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
    }
}

struct LoginView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var email = ""
    @State private var password = ""
    @State private var rememberMe = false
    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                HStack {
                    Spacer()
                    LanguageToggle()
                }
                .padding(.top, 16)

                Image("BrandLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: 400, maxHeight: 100)
                    .padding(.top, 40)

                Text(appViewModel.tr("login"))
                    .font(HASETTheme.font(.medium, 28))
                    .foregroundStyle(HASETTheme.textPrimary)
                    .padding(.top, 20)
                    .padding(.bottom, 24)

                if let registrationSuccess = appViewModel.registrationSuccessMessage, !registrationSuccess.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(appViewModel.tr("registration_successful"))
                            .font(HASETTheme.font(.medium, 15))
                            .foregroundStyle(HASETTheme.greenPrimary)
                        Text(registrationSuccess)
                            .font(HASETTheme.font(.regular, 13))
                            .foregroundStyle(HASETTheme.textSecondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(14)
                    .background(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(HASETTheme.greenPrimary.opacity(0.08))
                    )
                    .padding(.bottom, 16)
                }

                VStack(spacing: 16) {
                    RoundedInputField(title: appViewModel.tr("email"), systemImage: "envelope", text: $email, keyboardType: .emailAddress)
                        .onChange(of: email) { _ in appViewModel.loginErrorMessage = nil }
                    RoundedInputField(title: appViewModel.tr("password"), systemImage: "lock", text: $password, isSecure: true)
                        .onChange(of: password) { _ in appViewModel.loginErrorMessage = nil }

                    if let loginError = appViewModel.loginErrorMessage, !loginError.isEmpty {
                        Text(loginError)
                            .font(HASETTheme.font(.regular, 13))
                            .foregroundStyle(.red)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    if appViewModel.showUnpaidDoctorMessage {
                        Text(appViewModel.tr("doctor_reg_payment_required"))
                            .font(HASETTheme.font(.regular, 13))
                            .foregroundStyle(.red)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .onAppear {
                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                                    appViewModel.showUnpaidDoctorMessage = false
                                }
                            }
                    }

                    HStack {
                        Toggle(isOn: $rememberMe) {
                            Text(appViewModel.tr("remember_me"))
                                .font(HASETTheme.font(.regular, 14))
                                .foregroundStyle(HASETTheme.textSecondary)
                        }
                        .toggleStyle(.checkbox)

                        Spacer()

                        Button(appViewModel.tr("forgot_password")) {
                            appViewModel.pendingResetEmail = email
                            appViewModel.showForgotPassword()
                        }
                        .font(HASETTheme.font(.medium, 14))
                        .foregroundStyle(HASETTheme.greenPrimary)
                    }

                    Button(appViewModel.tr("login")) {
                        appViewModel.login(email: email, password: password)
                    }
                    .buttonStyle(PrimaryButtonStyle())
                    .padding(.bottom, 24)

                    HStack(spacing: 4) {
                        Text(appViewModel.tr("dont_have_account"))
                            .font(HASETTheme.font(.regular, 14))
                            .foregroundStyle(HASETTheme.textSecondary)
                        Button(appViewModel.tr("sign_in_here")) {
                            appViewModel.showRoleSelection()
                        }
                        .font(HASETTheme.font(.medium, 14))
                        .foregroundStyle(HASETTheme.greenPrimary)
                    }

                    HStack(spacing: 12) {
                        Link(appViewModel.tr("privacy_policy"), destination: URL(string: HASETConstants.privacyPolicyURL)!)
                        Text("•")
                        Link(appViewModel.tr("terms_of_service"), destination: URL(string: HASETConstants.termsURL)!)
                    }
                    .font(HASETTheme.font(.regular, 12))
                    .foregroundStyle(HASETTheme.textSecondary)
                    .padding(.top, 24)
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .scrollDismissesKeyboard(.interactively)
    }
}

struct MFAChallengeView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var code = ""
    @State private var invalid = false
    @State private var useRecoveryCode = false
    var body: some View {
        VStack(spacing: 24) {
            Text("Verify your identity").font(.title2.bold())
            Text(useRecoveryCode
                 ? "Enter one of the 10-character recovery codes saved during MFA setup. It can be used only once."
                 : "Enter the six-digit code from your authenticator app.")
                .multilineTextAlignment(.center)
            if useRecoveryCode {
                SecureField("Recovery code", text: $code)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
                    .textFieldStyle(.roundedBorder)
                    .onChange(of: code) { value in
                        code = String(value.uppercased().filter { "0123456789ABCDEF".contains($0) }.prefix(10))
                        invalid = false
                    }
            } else {
                SixDigitMFAInput(code: $code, isInvalid: invalid, isVerified: false) {
                    if code.count == 6 { appViewModel.verifyLoginMFA(code: code) }
                }
            }
            Button("Verify") { appViewModel.verifyLoginMFA(code: code) }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(code.count != (useRecoveryCode ? 10 : 6))
            if let error = appViewModel.mfaError, !error.isEmpty {
                Text(error)
                    .font(HASETTheme.font(.regular, 14))
                    .foregroundStyle(.red)
                    .multilineTextAlignment(.center)
            }
            Button(useRecoveryCode ? "Use authenticator code" : "Use a recovery code") {
                code = ""
                invalid = false
                appViewModel.mfaError = nil
                useRecoveryCode.toggle()
            }
            Button("Cancel") { appViewModel.cancelMFALogin() }
        }.padding(24).onChange(of: appViewModel.mfaError) { _ in invalid = true }
    }
}

struct MFAEnrollmentView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.scenePhase) private var scenePhase
    var onComplete: (() -> Void)?
    var onCancel: (() -> Void)?
    @State private var setup: MobileMFASetupResponse?
    @State private var code = ""
    @State private var acknowledged = false
    @State private var loading = false
    @State private var error: String?
    @State private var didRequest = false
    @State private var privacyCovered = false
    @State private var screenCaptured = false
    @State private var showManualKey = false

    init(onComplete: (() -> Void)? = nil, onCancel: (() -> Void)? = nil) {
        self.onComplete = onComplete
        self.onCancel = onCancel
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                Text("Set up two-factor authentication")
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)
                    .padding(.top, 16)
                Text("Scan the QR code using Google Authenticator, Microsoft Authenticator, or another authenticator app.")
                    .foregroundStyle(HASETTheme.textSecondary)
                    .multilineTextAlignment(.center)
                if let setup {
                    if let qrCode = qrImage(from: setup.otpauthURI) {
                        qrCode
                            .resizable()
                            .interpolation(.none)
                            .scaledToFit()
                            .frame(width: 220, height: 220)
                            .padding(18)
                            .background(RoundedRectangle(cornerRadius: 18).fill(Color.white))
                            .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color.black.opacity(0.08)))
                            .accessibilityLabel("Authenticator setup QR code")
                    } else {
                        Text("The QR code could not be generated. Retry setup or use the setup key below.")
                            .font(.caption)
                            .foregroundStyle(.red)
                            .multilineTextAlignment(.center)
                    }
                    Button(showManualKey ? "Hide setup key" : "Can't scan? Use a setup key") {
                        withAnimation { showManualKey.toggle() }
                    }
                    .buttonStyle(.bordered)
                    if showManualKey {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Manual setup key").font(.caption).foregroundStyle(HASETTheme.textSecondary)
                            Text(formattedSecret(setup.secret))
                                .font(.system(.body, design: .monospaced).weight(.semibold))
                                .minimumScaleFactor(0.72)
                                .lineLimit(2)
                                .textSelection(.enabled)
                            Button("Copy setup key") { copySetupKey(setup.secret) }
                                .font(.caption.bold())
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(16)
                        .background(RoundedRectangle(cornerRadius: 14).fill(HASETTheme.backgroundPrimary))
                    }
                    SixDigitMFAInput(code: $code, isInvalid: error != nil, isVerified: false) { confirm() }
                    Button(loading ? "Confirming…" : "Confirm authenticator") { confirm() }.buttonStyle(PrimaryButtonStyle()).disabled(loading || code.count != 6)
                    if !setup.recoveryCodes.isEmpty { Text("Recovery codes will be shown after confirmation.").font(.caption) }
                } else if loading { ProgressView("Preparing setup…") } else { Button("Retry setup") { requestSetup() }.buttonStyle(PrimaryButtonStyle()) }
                if let error { Text(error).foregroundStyle(.red).multilineTextAlignment(.center) }
                if let onCancel {
                    Button("Cancel", action: onCancel)
                } else {
                    Button("Log out") { appViewModel.logout() }
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .privacySensitive()
        .overlay { if privacyCovered || screenCaptured { Color.black.ignoresSafeArea().overlay(Text("Sensitive MFA information hidden").foregroundStyle(.white)) } }
        .task { requestSetup() }
        .onChange(of: scenePhase) { phase in privacyCovered = phase != .active; if phase != .active { code = "" } }
        .onReceive(NotificationCenter.default.publisher(for: UIScreen.capturedDidChangeNotification)) { _ in screenCaptured = UIScreen.main.isCaptured }
        .sheet(isPresented: Binding(get: { setup != nil && acknowledged }, set: { _ in })) { EmptyView() }
        .alert("Recovery codes", isPresented: Binding(get: { setup != nil && acknowledged }, set: { _ in })) {
            Button("Copy all") { if let codes = setup?.recoveryCodes { UIPasteboard.general.string = codes.joined(separator: "\n") } }
            Button("I saved them") {
                if let onComplete { onComplete() }
                else { Task { await appViewModel.completeMFAEnrollment() } }
            }
        } message: { Text(setup?.recoveryCodes.joined(separator: "\n") ?? "") }
    }

    private func requestSetup() {
        guard !didRequest, setup == nil else { return }; didRequest = true; loading = true
        guard let session = appViewModel.pendingMFASession ?? appViewModel.activeSession ?? SessionStore().loadSession() else {
            error = "Authentication expired. Please log in again."
            loading = false
            didRequest = false
            return
        }
        Task {
            do {
                let service = AuthService()
                let freshSession = (try? await service.refreshSessionIfNeeded(session)) ?? session
                if freshSession.idToken != session.idToken || freshSession.refreshToken != session.refreshToken {
                    SessionStore().saveSession(freshSession)
                }
                appViewModel.activeSession = freshSession
                setup = try await service.setupMobileMFA(idToken: freshSession.idToken)
                loading = false
            } catch let setupError {
                loading = false
                error = setupError.localizedDescription
                didRequest = false
            }
        }
    }
    private func confirm() {
        guard code.count == 6 else { error = "Enter all six digits from your authenticator app."; return }
        guard let session = appViewModel.pendingMFASession ?? appViewModel.activeSession ?? SessionStore().loadSession(), code.count == 6, !loading else { return }
        loading = true; error = nil
        Task { do { try await AuthService().confirmMobileMFA(code: code, idToken: session.idToken); loading = false; acknowledged = true; code = "" } catch let confirmationError { loading = false; error = confirmationError.localizedDescription; code = "" } }
    }
    private func qrImage(from value: String) -> Image? {
        guard !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(value.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage else { return nil }
        let context = CIContext(options: [.useSoftwareRenderer: false])
        guard let cgImage = context.createCGImage(output, from: output.extent) else { return nil }
        return Image(uiImage: UIImage(cgImage: cgImage))
    }

    private func formattedSecret(_ secret: String) -> String {
        stride(from: 0, to: secret.count, by: 4).map { offset in
            let start = secret.index(secret.startIndex, offsetBy: offset)
            let end = secret.index(start, offsetBy: min(4, secret.distance(from: start, to: secret.endIndex)))
            return String(secret[start..<end])
        }.joined(separator: " ")
    }

    private func copySetupKey(_ secret: String) {
        UIPasteboard.general.setItems(
            [[UTType.plainText.identifier: secret]],
            options: [.localOnly: true, .expirationDate: Date().addingTimeInterval(120)]
        )
    }
}

struct RoleSelectionView: View {
    @EnvironmentObject private var appViewModel: AppViewModel

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button {
                    appViewModel.showLogin()
                } label: {
                    Image(systemName: "chevron.left")
                        .foregroundStyle(HASETTheme.greenPrimary)
                        .frame(width: 40, height: 40)
                        .background(Circle().fill(Color.white))
                }

                Spacer()
                LanguageToggle()
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)

            ScrollView {
                VStack(spacing: 24) {
                    Image("OnboardingOne")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 320, height: 320)
                        .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))

                    Text(appViewModel.tr("role_selection_title"))
                        .font(HASETTheme.font(.medium, 24))
                        .foregroundStyle(HASETTheme.textPrimary)

                    HStack(spacing: 20) {
                        roleCard(appViewModel.tr("im_patient"), imageName: "OnboardingOne", color: HASETTheme.greenLight, role: .patient)
                        roleCard(appViewModel.tr("im_doctor"), imageName: "OnboardingTwo", color: HASETTheme.redPrimary, role: .doctor)
                    }
                    .padding(.horizontal, 17)
                }
                .padding(.top, 12)
                .padding(.bottom, 40)
            }
        }
    }

    private func roleCard(_ title: String, imageName: String, color: Color, role: UserRole) -> some View {
        Button {
            appViewModel.showRegister(role: role)
        } label: {
            VStack(spacing: 8) {
                Image(imageName)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 62, height: 62)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

                Text(title)
                    .font(HASETTheme.font(.medium, 16))
                    .foregroundStyle(Color.white)
                    .multilineTextAlignment(.center)
            }
            .padding(10)
            .frame(maxWidth: .infinity)
            .frame(height: 132)
            .background(RoundedRectangle(cornerRadius: 28, style: .continuous).fill(color))
        }
        .buttonStyle(.plain)
    }
}

struct RegisterView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    let role: UserRole

    @State private var fullName = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var regNo = ""
    @State private var nin = ""
    @State private var password = ""
    @State private var ninDocumentData: Data?
    @State private var mctCertificateData: Data?
    @State private var showingNinImporter = false
    @State private var showingMctImporter = false
    @State private var uploadingDocuments = false

    private var canSubmitRegistration: Bool {
        let phoneDigits = phone.filter(\.isNumber)
        let baseValid = ValidationService.isValidName(fullName)
            && ValidationService.isValidEmail(email)
            && phoneDigits.count == 9
            && ValidationService.isStrongPassword(password)
        if role != .doctor { return baseValid }
        return baseValid
            && !regNo.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && ValidationService.isValidNin(nin)
            && ninDocumentData != nil
            && mctCertificateData != nil
            && !uploadingDocuments
    }

    private var passwordHintText: String {
        if password.isEmpty { return appViewModel.tr("strong_password_required") }
        return ValidationService.isStrongPassword(password)
            ? appViewModel.tr("password_auth_ok")
            : appViewModel.tr("strong_password_required")
    }

    private var passwordHintColor: Color {
        if password.isEmpty { return HASETTheme.textSecondary }
        return ValidationService.isStrongPassword(password) ? HASETTheme.greenPrimary : .red
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                HStack {
                    Spacer()
                    LanguageToggle()
                }
                .padding(.top, 16)

                Image(role == .doctor ? "OnboardingTwo" : "OnboardingOne")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 170, height: 120)
                    .padding(.top, 20)

                Text(appViewModel.tr("register"))
                    .font(HASETTheme.font(.medium, 28))
                    .foregroundStyle(HASETTheme.textPrimary)
                    .padding(.top, 9)
                    .padding(.bottom, 8)

                Text(role.localizedDisplayName(languageCode: appViewModel.selectedLanguage))
                    .font(HASETTheme.font(.medium, 16))
                    .foregroundStyle(HASETTheme.textSecondary)
                    .padding(.bottom, 20)

                VStack(spacing: 16) {
                    RoundedInputField(title: appViewModel.tr("full_name"), systemImage: "person", text: $fullName, keyboardType: .namePhonePad)
                    RoundedInputField(title: appViewModel.tr("email"), systemImage: "envelope", text: $email, keyboardType: .emailAddress)
                    RoundedInputField(title: appViewModel.tr("phone_number"), systemImage: "phone", text: $phone, prefix: "+255", keyboardType: .phonePad)

                    if role == .doctor {
                        RoundedInputField(
                            title: appViewModel.tr("hint_mct_reg_no"),
                            systemImage: "checkmark.seal",
                            text: $regNo
                        )
                        RoundedInputField(
                            title: appViewModel.tr("hint_nin"),
                            systemImage: "person.text.rectangle",
                            text: $nin,
                            keyboardType: .numberPad
                        )

                        documentPickerRow(
                            title: appViewModel.tr("upload_nin_document"),
                            selected: ninDocumentData != nil
                        ) {
                            showingNinImporter = true
                        }
                        documentPickerRow(
                            title: appViewModel.tr("upload_mct_certificate"),
                            selected: mctCertificateData != nil
                        ) {
                            showingMctImporter = true
                        }
                    }

                    RoundedInputField(title: appViewModel.tr("password"), systemImage: "lock", text: $password, isSecure: true)

                    Text(passwordHintText)
                        .font(HASETTheme.font(.regular, 12))
                        .foregroundStyle(passwordHintColor)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Button(uploadingDocuments ? appViewModel.tr("uploading_documents") : appViewModel.tr("sign_up")) {
                        guard canSubmitRegistration else {
                            appViewModel.alertState = AlertState(
                                title: appViewModel.tr("error"),
                                message: role == .doctor
                                    ? appViewModel.tr("doctor_registration_pdf_details_required")
                                    : appViewModel.tr("registration_fields_required")
                            )
                            return
                        }
                        Task { await submitRegistration() }
                    }
                    .buttonStyle(PrimaryButtonStyle())
                    .disabled(!canSubmitRegistration)
                    .opacity(canSubmitRegistration ? 1 : 0.55)
                    .padding(.bottom, 24)

                    HStack(spacing: 4) {
                        Text(appViewModel.tr("already_have_account"))
                            .font(HASETTheme.font(.regular, 14))
                            .foregroundStyle(HASETTheme.textSecondary)
                        Button(appViewModel.tr("sign_in")) {
                            appViewModel.showLogin()
                        }
                        .font(HASETTheme.font(.medium, 14))
                        .foregroundStyle(HASETTheme.greenPrimary)
                    }

                    HStack(spacing: 12) {
                        Link(appViewModel.tr("privacy_policy"), destination: URL(string: HASETConstants.privacyPolicyURL)!)
                        Text("•")
                        Link(appViewModel.tr("terms_of_service"), destination: URL(string: HASETConstants.termsURL)!)
                    }
                    .font(HASETTheme.font(.regular, 12))
                    .foregroundStyle(HASETTheme.textSecondary)
                    .padding(.top, 24)
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .scrollDismissesKeyboard(.interactively)
        .fileImporter(
            isPresented: $showingNinImporter,
            allowedContentTypes: [.pdf],
            allowsMultipleSelection: false
        ) { result in
            importPdfDocument(from: result) { ninDocumentData = $0 }
        }
        .fileImporter(
            isPresented: $showingMctImporter,
            allowedContentTypes: [.pdf],
            allowsMultipleSelection: false
        ) { result in
            importPdfDocument(from: result) { mctCertificateData = $0 }
        }
    }

    private func importPdfDocument(from result: Result<[URL], Error>, assign: (Data?) -> Void) {
        switch result {
        case .success(let urls):
            guard let url = urls.first else { return }
            guard url.startAccessingSecurityScopedResource() else {
                appViewModel.alertState = AlertState(
                    title: appViewModel.tr("error"),
                    message: appViewModel.tr("document_pdf_only")
                )
                assign(nil)
                return
            }
            defer { url.stopAccessingSecurityScopedResource() }
            guard let data = try? Data(contentsOf: url), ValidationService.isPdfData(data) else {
                assign(nil)
                appViewModel.alertState = AlertState(
                    title: appViewModel.tr("error"),
                    message: appViewModel.tr("document_pdf_only")
                )
                return
            }
            assign(data)
        case .failure(let error):
            assign(nil)
            appViewModel.alertState = AlertState(
                title: appViewModel.tr("error"),
                message: error.localizedDescription
            )
        }
    }

    @ViewBuilder
    private func documentPickerRow(title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Image(systemName: selected ? "checkmark.circle.fill" : "doc.badge.plus")
                    .foregroundStyle(selected ? HASETTheme.greenPrimary : HASETTheme.textSecondary)
                Text(title)
                    .font(HASETTheme.font(.regular, 14))
                    .foregroundStyle(HASETTheme.textPrimary)
                Spacer()
                Text(selected ? appViewModel.tr("document_selected") : appViewModel.tr("document_upload"))
                    .font(HASETTheme.font(.medium, 13))
                    .foregroundStyle(HASETTheme.greenPrimary)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Color.white)
            )
        }
        .buttonStyle(.plain)
    }

    private func submitRegistration() async {
        uploadingDocuments = true
        defer { uploadingDocuments = false }

        var ninUrl: String?
        var mctUrl: String?
        if role == .doctor {
            let authService = AuthService()
            do {
                if let ninDocumentData {
                    guard ValidationService.isPdfData(ninDocumentData) else {
                        appViewModel.alertState = AlertState(
                            title: appViewModel.tr("error"),
                            message: appViewModel.tr("document_pdf_only")
                        )
                        return
                    }
                    ninUrl = try await authService.uploadVerificationDocument(
                        ninDocumentData,
                        fileName: "nin_document.pdf",
                        mimeType: "application/pdf"
                    )
                }
                if let mctCertificateData {
                    guard ValidationService.isPdfData(mctCertificateData) else {
                        appViewModel.alertState = AlertState(
                            title: appViewModel.tr("error"),
                            message: appViewModel.tr("document_pdf_only")
                        )
                        return
                    }
                    mctUrl = try await authService.uploadVerificationDocument(
                        mctCertificateData,
                        fileName: "mct_certificate.pdf",
                        mimeType: "application/pdf"
                    )
                }
            } catch {
                appViewModel.alertState = AlertState(
                    title: appViewModel.tr("registration_failed"),
                    message: error.localizedDescription
                )
                return
            }
        }

        appViewModel.register(
            fullName: fullName,
            email: email,
            phoneDigits: phone,
            password: password,
            role: role,
            regNo: regNo,
            nin: nin,
            ninDocumentUrl: ninUrl,
            mctCertificateUrl: mctUrl
        )
    }
}

private struct CheckboxToggleStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        Button {
            configuration.isOn.toggle()
        } label: {
            HStack(spacing: 8) {
                Image(systemName: configuration.isOn ? "checkmark.square.fill" : "square")
                    .foregroundStyle(configuration.isOn ? HASETTheme.greenPrimary : HASETTheme.textSecondary)
                configuration.label
            }
        }
        .buttonStyle(.plain)
    }
}

extension ToggleStyle where Self == CheckboxToggleStyle {
    static var checkbox: CheckboxToggleStyle { CheckboxToggleStyle() }
}

struct ForgotPasswordView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var email = ""
    /// When opened from login, show an explicit back control. Settings uses NavigationStack back.
    var useAuthBackNavigation: Bool = true

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                if useAuthBackNavigation {
                    HStack(spacing: 16) {
                        Button(action: goBack) {
                            Image(systemName: "chevron.left")
                                .foregroundStyle(HASETTheme.greenPrimary)
                                .frame(width: 40, height: 40)
                                .background(Circle().fill(Color.white))
                        }

                        Text(appViewModel.tr("forgot_password"))
                            .font(HASETTheme.font(.medium, 20))
                            .foregroundStyle(HASETTheme.greenPrimary)
                            .frame(maxWidth: .infinity)

                        Color.clear
                            .frame(width: 40, height: 40)
                    }
                    .padding(.bottom, 16)
                } else {
                    Text(appViewModel.tr("forgot_password"))
                        .font(HASETTheme.font(.medium, 20))
                        .foregroundStyle(HASETTheme.greenPrimary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.bottom, 16)
                }

                Text(appViewModel.tr("forgot_password_description"))
                    .font(HASETTheme.font(.regular, 14))
                    .foregroundStyle(HASETTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.bottom, 32)

                RoundedInputField(title: appViewModel.tr("email"), systemImage: "envelope", text: $email, keyboardType: .emailAddress)
                    .padding(.bottom, 24)
                    .onAppear { email = appViewModel.pendingResetEmail }

                Button(appViewModel.tr("send_reset_link")) {
                    appViewModel.sendResetEmail(email)
                }
                .buttonStyle(PrimaryButtonStyle())
                .padding(.bottom, 24)

                HStack(spacing: 4) {
                    Text(appViewModel.tr("remember_password"))
                        .font(HASETTheme.font(.regular, 14))
                        .foregroundStyle(HASETTheme.textSecondary)
                    Button(appViewModel.tr("sign_in")) {
                        goBack()
                    }
                    .font(HASETTheme.font(.medium, 14))
                    .foregroundStyle(HASETTheme.greenPrimary)
                }
            }
            .padding(24)
        }
    }

    private func goBack() {
        if useAuthBackNavigation {
            appViewModel.showLogin()
        } else {
            dismiss()
        }
    }
}

struct ResetPasswordConfirmView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    let initialCode: String
    @State private var linkInput: String
    @State private var newPassword = ""
    @State private var confirmPassword = ""
    @State private var verifiedEmail: String?
    @State private var showPasteField: Bool

    init(initialCode: String) {
        self.initialCode = initialCode
        _linkInput = State(initialValue: initialCode)
        _showPasteField = State(initialValue: initialCode.isEmpty)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    if showPasteField {
                        RoundedInputField(
                            title: appViewModel.tr("paste_reset_link"),
                            systemImage: "link",
                            text: $linkInput
                        )
                    }

                    if let verifiedEmail {
                        Text("Reset code verified for \(verifiedEmail)")
                            .font(HASETTheme.font(.regular, 14))
                            .foregroundStyle(HASETTheme.textSecondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    RoundedInputField(
                        title: appViewModel.tr("reset_new_password"),
                        systemImage: "lock",
                        text: $newPassword,
                        isSecure: true
                    )
                    RoundedInputField(
                        title: appViewModel.tr("reset_confirm_password"),
                        systemImage: "lock",
                        text: $confirmPassword,
                        isSecure: true
                    )

                    Button(appViewModel.tr("reset_password_action")) {
                        Task {
                            let code = PasswordResetLinkParser.extractOobCode(from: linkInput) ?? linkInput
                            if verifiedEmail == nil {
                                do {
                                    let email = try await AuthService().verifyPasswordResetCode(code)
                                    verifiedEmail = email
                                    linkInput = code
                                    return
                                } catch {
                                    appViewModel.alertState = AlertState(title: appViewModel.tr("error"), message: error.localizedDescription)
                                    return
                                }
                            }
                            if await appViewModel.confirmPasswordReset(
                                oobCode: code,
                                newPassword: newPassword,
                                confirmPassword: confirmPassword
                            ) {
                                dismiss()
                            }
                        }
                    }
                    .buttonStyle(PrimaryButtonStyle())
                }
                .padding(24)
            }
            .navigationTitle(appViewModel.tr("forgot_password"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(appViewModel.tr("close")) {
                        appViewModel.pendingPasswordResetCode = nil
                        dismiss()
                    }
                }
            }
            .task {
                guard !initialCode.isEmpty else { return }
                if let email = try? await AuthService().verifyPasswordResetCode(initialCode) {
                    verifiedEmail = email
                }
            }
        }
    }
}
