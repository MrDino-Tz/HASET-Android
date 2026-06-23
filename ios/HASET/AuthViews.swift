import SwiftUI

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

private struct SocialBadge: View {
    var body: some View {
        Image("SplashLogo")
            .resizable()
            .scaledToFit()
            .frame(width: 56, height: 56)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(HASETTheme.divider, lineWidth: 1)
            )
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

                VStack(spacing: 18) {
                    Image("SplashLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 180, height: 180)

                    Text(typedTitle)
                        .font(HASETTheme.font(.black, 37))
                        .foregroundStyle(HASETTheme.textPrimary)
                        .onAppear {
                            startTypewriterAnimation()
                        }
                }
                .frame(maxWidth: .infinity)

                Spacer()

                HStack(spacing: 6) {
                    Text(appViewModel.selectedLanguage == "sw" ? "Kutoka" : "From")
                    Image("BrandLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 17, height: 17)
                    Text("HASET Hospital")
                }
                .font(HASETTheme.font(.regular, 15))
                .foregroundStyle(HASETTheme.textSecondary)
                .padding(.bottom, 48)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func startTypewriterAnimation() {
        guard typedTitle.isEmpty else { return }
        let fullTitle = "HASET"
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

            Button(page == pages.count - 1 ? appViewModel.tr("sign_in") : appViewModel.tr("next")) {
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
                    .frame(width: 320, height: 100)
                    .padding(.top, 40)

                Text(appViewModel.tr("login"))
                    .font(HASETTheme.font(.medium, 28))
                    .foregroundStyle(HASETTheme.textPrimary)
                    .padding(.top, 20)
                    .padding(.bottom, 24)

                VStack(spacing: 16) {
                    RoundedInputField(title: appViewModel.tr("email"), systemImage: "envelope", text: $email, keyboardType: .emailAddress)
                    RoundedInputField(title: appViewModel.tr("password"), systemImage: "lock", text: $password, isSecure: true)

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

                    Text(appViewModel.tr("or"))
                        .font(HASETTheme.font(.medium, 14))
                        .foregroundStyle(HASETTheme.textSecondary)
                        .padding(.vertical, 8)

                    SocialBadge()
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
    @State private var password = ""

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
                            title: "Tanzanian Medical Council (MCT) Reg. No. (e.g. MCT3625)",
                            systemImage: "checkmark.seal",
                            text: $regNo
                        )
                    }

                    RoundedInputField(title: appViewModel.tr("password"), systemImage: "lock", text: $password, isSecure: true)

                    Button(appViewModel.tr("sign_up")) {
                        appViewModel.register(
                            fullName: fullName,
                            email: email,
                            phoneDigits: phone,
                            password: password,
                            role: role,
                            regNo: regNo
                        )
                    }
                    .buttonStyle(PrimaryButtonStyle())

                    Text(appViewModel.tr("or"))
                        .font(HASETTheme.font(.medium, 14))
                        .foregroundStyle(HASETTheme.textSecondary)
                        .padding(.vertical, 8)

                    SocialBadge()
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
    @State private var email = ""

    var body: some View {
        ScrollView {
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

                    Text(appViewModel.tr("forgot_password"))
                        .font(HASETTheme.font(.medium, 20))
                        .foregroundStyle(HASETTheme.greenPrimary)
                        .frame(maxWidth: .infinity)

                    Color.clear.frame(width: 34, height: 34)
                }
                .padding(.bottom, 16)

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
                        appViewModel.showLogin()
                    }
                    .font(HASETTheme.font(.medium, 14))
                    .foregroundStyle(HASETTheme.greenPrimary)
                }
            }
            .padding(24)
        }
    }
}
