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
                onPaymentConfirmed: {
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
