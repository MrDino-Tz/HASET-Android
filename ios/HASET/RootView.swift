import SwiftUI

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
        .overlay {
            if appViewModel.isLoading {
                ZStack {
                    Color.black.opacity(0.18).ignoresSafeArea()
                    ProgressView("Loading...")
                        .tint(HASETTheme.greenPrimary)
                        .padding(24)
                        .background(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .fill(Color.white)
                        )
                }
            }
        }
    }
}
