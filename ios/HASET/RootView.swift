import SwiftUI

private struct ResetPasswordSheetToken: Identifiable {
    let code: String
    var id: String { code.isEmpty ? "manual-reset" : code }
}

struct RootView: View {
    @EnvironmentObject private var appViewModel: AppViewModel

    var body: some View {
        Group {
            switch appViewModel.route {
            case .splash:
                SplashView()
            case .onboarding:
                OnboardingView()
            case .login:
                LoginView()
            case .mfaChallenge:
                MFAChallengeView()
            case .mfaEnrollment:
                MFAEnrollmentView()
            case .forgotPassword:
                ForgotPasswordView()
            case .roleSelection:
                RoleSelectionView()
            case .register(let role):
                RegisterView(role: role)
            case .dashboard(let role):
                DashboardRootView(role: role)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(HASETTheme.backgroundPrimary.ignoresSafeArea())
        .environment(\.locale, Locale(identifier: appViewModel.selectedLanguage))
        .id(appViewModel.selectedLanguage)
        .preferredColorScheme({
            switch appViewModel.themeMode {
            case .light:
                return .light
            case .dark:
                return .dark
            case .system:
                return nil
            }
        }())
        .alert(item: $appViewModel.alertState) { state in
            Alert(title: Text(state.title), message: Text(state.message), dismissButton: .default(Text("OK")))
        }
        .alert(item: $appViewModel.blockingDialog) { state in
            if let urlString = state.updateURL, let url = URL(string: urlString) {
                Alert(
                    title: Text(state.title),
                    message: Text(state.message),
                    primaryButton: .default(Text("Update Now")) {
                        UIApplication.shared.open(url)
                    },
                    secondaryButton: .destructive(Text("Exit")) {
                        exit(0)
                    }
                )
            } else {
                Alert(
                    title: Text(state.title),
                    message: Text(state.message),
                    dismissButton: .destructive(Text("Exit")) {
                        exit(0)
                    }
                )
            }
        }
        .sheet(item: $appViewModel.pendingDoctorRegistration) { pending in
            PaymentCheckoutView(
                doctor: pending.doctor,
                amount: pending.amount,
                initialMethod: .mobileMoney,
                onPaymentConfirmed: { _ in
                    Task { await appViewModel.completeDoctorRegistrationPayment() }
                },
                onCancelRegistrationPayment: {
                    appViewModel.cancelDoctorRegistrationPayment()
                }
            )
            .environmentObject(appViewModel)
            .interactiveDismissDisabled(true)
        }
        .sheet(item: Binding(
            get: {
                appViewModel.pendingPasswordResetCode.map { ResetPasswordSheetToken(code: $0) }
            },
            set: { newValue in
                appViewModel.pendingPasswordResetCode = newValue?.code
            }
        )) { token in
            ResetPasswordConfirmView(initialCode: token.code)
                .environmentObject(appViewModel)
        }
        .overlay {
            RatingPromptPresenter()
                .environmentObject(appViewModel)
        }
        .overlay(alignment: .bottom) {
            if !appViewModel.isOnline {
                OfflineBannerView()
                    .environmentObject(appViewModel)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.25), value: appViewModel.isOnline)
        .overlay {
            if appViewModel.isLoading {
                ZStack {
                    Color.black.opacity(0.18).ignoresSafeArea()
                    ProgressView("Loading...")
                        .tint(HASETTheme.greenPrimary)
                        .padding(24)
                }
            }
        }
    }
}

private struct RatingPromptPresenter: View {
    @EnvironmentObject private var appViewModel: AppViewModel

    var body: some View {
        Color.clear
            .frame(width: 1, height: 1)
            .accessibilityHidden(true)
            .sheet(isPresented: $appViewModel.showRatingPrompt) {
                RateAppView()
                    .environmentObject(appViewModel)
            }
    }
}

struct OfflineBannerView: View {
    @EnvironmentObject private var appViewModel: AppViewModel

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "wifi.slash")
                .font(.system(size: 18))
                .foregroundStyle(.white)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                Text(appViewModel.tr("no_internet_title"))
                    .font(HASETTheme.font(.medium, 14))
                Text(appViewModel.tr("no_internet_message"))
                    .font(HASETTheme.font(.regular, 12))
                    .lineLimit(2)
            }
            .foregroundStyle(.white)

            Spacer(minLength: 8)

            Button(appViewModel.tr("retry")) {
                Task { await appViewModel.refreshAfterReconnect() }
            }
            .font(HASETTheme.font(.medium, 14))
            .foregroundStyle(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Capsule().fill(Color.white.opacity(0.22)))
            .accessibilityLabel(appViewModel.tr("retry"))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(HASETTheme.redPrimary)
                .shadow(color: .black.opacity(0.2), radius: 10, y: 4)
        )
        .padding(.horizontal, 14)
        .padding(.bottom, 10)
        .accessibilityElement(children: .combine)
    }
}
