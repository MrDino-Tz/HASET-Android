import PhotosUI
import SwiftUI
import UIKit
import WebKit

private struct HostedCheckoutDestination: Identifiable {
    let id = UUID()
    let url: URL
}

private struct HostedCheckoutWebView: UIViewRepresentable {
    let url: URL
    let onCallback: () -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onCallback: onCallback) }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = true
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.uiDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = true
        webView.load(URLRequest(url: url))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {}

    final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        private let onCallback: () -> Void

        init(onCallback: @escaping () -> Void) { self.onCallback = onCallback }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            guard let target = navigationAction.request.url else {
                decisionHandler(.cancel)
                return
            }
            if target.host?.lowercased() == "hasethospital.or.tz",
               (target.path.hasPrefix("/payment/success") || target.path.hasPrefix("/payment/cancel")) {
                onCallback()
                decisionHandler(.cancel)
                return
            }
            if let scheme = target.scheme?.lowercased(), scheme != "https" && scheme != "http" {
                UIApplication.shared.open(target)
                decisionHandler(.cancel)
                return
            }
            decisionHandler(.allow)
        }

        func webView(
            _ webView: WKWebView,
            createWebViewWith configuration: WKWebViewConfiguration,
            for navigationAction: WKNavigationAction,
            windowFeatures: WKWindowFeatures
        ) -> WKWebView? {
            if navigationAction.targetFrame == nil, let target = navigationAction.request.url {
                webView.load(URLRequest(url: target))
            }
            return nil
        }
    }
}

struct DashboardRootView: View {
    let role: UserRole
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var selectedTab: DashboardTab = .home

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack { RoleHomeView(role: role) }
                .tabItem { Label(appViewModel.tr("home"), systemImage: "house") }
                .tag(DashboardTab.home)

            NavigationStack { AppointmentsOverviewView(role: role) }
                .tabItem { Label(appViewModel.tr("appointments"), systemImage: "calendar") }
                .tag(DashboardTab.appointments)

            NavigationStack { ChatListScreen(role: role) }
                .tabItem { Label(appViewModel.tr("chat"), systemImage: "message") }
                .tag(DashboardTab.chat)

            NavigationStack { ProfileScreen() }
                .tabItem { Label(appViewModel.tr("profile"), systemImage: "person") }
                .tag(DashboardTab.profile)
        }
        .tint(HASETTheme.greenPrimary)
    }
}

struct RoleHomeView: View {
    let role: UserRole
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.scenePhase) private var scenePhase
    @State private var selectedHeroIndex = 0
    @State private var showingChildrensCorner = false
    @State private var doctorBalanceHidden = true
    @State private var selectedDoctorStatus: AppointmentSummary.Status?
    @State private var showingDoctorFilterOptions = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                if role == .patient {
                    patientTopHeader
                    patientHeroCard
                } else if role == .doctor {
                    doctorTopHeader
                    doctorHeroCard
                } else {
                    headerCard
                }

                switch role {
                case .patient:
                    patientContent
                case .doctor:
                    doctorContent
                case .admin:
                    adminContent
                }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .refreshable {
            if role == .doctor { await appViewModel.loadDoctorWallet(force: true) }
            await appViewModel.loadAppointments(force: true)
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            if role == .patient {
                await appViewModel.loadPatientHomeContent(force: false)
            } else if role == .doctor {
                await appViewModel.loadDoctorWallet(force: true)
                await appViewModel.loadDoctorPresence(force: false)
            }
        }
        .onChange(of: scenePhase) { phase in
            guard phase == .active, role == .doctor else { return }
            Task { await appViewModel.loadDoctorWallet(force: true) }
        }
    }

    private var doctorTopHeader: some View {
        HStack(spacing: 14) {
            NavigationLink {
                ProfileScreen()
            } label: {
                HStack(spacing: 12) {
                    ProfileAvatarView(
                        imageSource: appViewModel.currentUser?.profileImage ?? "",
                        initials: doctorInitials,
                        size: 48,
                        fontSize: 16
                    )
                    .shadow(color: HASETTheme.greenPrimary.opacity(0.10), radius: 10, x: 0, y: 6)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(timeGreeting)
                            .font(HASETTheme.font(.regular, 12))
                            .foregroundStyle(HASETTheme.textSecondary)
                        Text(displayName(for: appViewModel.currentUser, fallback: "Dr"))
                            .font(HASETTheme.font(.medium, 16))
                            .foregroundStyle(HASETTheme.textPrimary)
                            .lineLimit(1)
                    }
                }
            }
            .buttonStyle(.plain)

            Spacer()

            Button {
                Task {
                    let newValue = !(appViewModel.doctorPresence?.online ?? false)
                    await appViewModel.setDoctorPresence(online: newValue)
                }
            } label: {
                HStack(spacing: 8) {
                    Circle()
                        .fill((appViewModel.doctorPresence?.online ?? false) ? HASETTheme.greenPrimary : Color.gray.opacity(0.45))
                        .frame(width: 8, height: 8)
                    Text((appViewModel.doctorPresence?.online ?? false) ? "Online" : "Offline")
                        .font(HASETTheme.font(.medium, 12))
                        .foregroundStyle(HASETTheme.textPrimary)
                }
                .padding(.horizontal, 14)
                .frame(height: 38)
                .background(
                    Capsule()
                        .fill(Color.white)
                        .shadow(color: HASETTheme.divider.opacity(0.35), radius: 8, x: 0, y: 4)
                )
                .overlay(
                    Capsule().stroke(HASETTheme.divider.opacity(0.85), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)

            NavigationLink {
                NotificationsView()
            } label: {
                ZStack(alignment: .topTrailing) {
                    Circle()
                        .fill(Color.white)
                        .frame(width: 44, height: 44)
                        .shadow(color: HASETTheme.divider.opacity(0.35), radius: 8, x: 0, y: 4)
                        .overlay(
                            Image(systemName: "bell")
                                .font(.system(size: 18, weight: .regular))
                                .foregroundStyle(HASETTheme.greenPrimary)
                        )
                    if unreadNotificationCount > 0 {
                        Text("\(unreadNotificationCount)")
                            .font(HASETTheme.font(.medium, 9))
                            .foregroundStyle(.white)
                            .frame(width: 16, height: 16)
                            .background(Circle().fill(HASETTheme.redPrimary))
                            .offset(x: 2, y: -2)
                    }
                }
            }
            .buttonStyle(.plain)
        }
        .padding(.top, 2)
    }

    private var timeGreeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        if hour < 12 { return appViewModel.tr("good_morning") }
        if hour < 18 { return appViewModel.selectedLanguage == "sw" ? "Habari za mchana" : "Good afternoon" }
        return appViewModel.selectedLanguage == "sw" ? "Habari za jioni" : "Good evening"
    }

    private var doctorHeroCard: some View {
        CardContainer(fill: .white, shadowColor: HASETTheme.greenPrimary.opacity(0.08), cornerRadius: 16, padding: 16) {
            HStack(spacing: 12) {
                Image(systemName: "calendar")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(HASETTheme.greenPrimary)
                Text(todayDateString)
                    .font(HASETTheme.font(.medium, 14))
                    .foregroundStyle(HASETTheme.textPrimary)
                Spacer()
                HStack(spacing: 8) {
                    Button {
                        doctorBalanceHidden.toggle()
                    } label: {
                        Image(systemName: doctorBalanceHidden ? "eye.slash" : "eye")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(HASETTheme.greenPrimary)
                            .padding(4)
                    }
                    .buttonStyle(.plain)

                    NavigationLink {
                        DoctorWalletView(isHidden: doctorBalanceHidden)
                    } label: {
                        HStack(spacing: 8) {
                            Image("icons8_coins")
                                .resizable()
                                .renderingMode(.original)
                                .scaledToFit()
                                .frame(width: 22, height: 22)
                            Text(walletDisplayText)
                                .font(HASETTheme.font(.medium, 14))
                                .foregroundStyle(HASETTheme.textPrimary)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var patientTopHeader: some View {
        HStack(spacing: 14) {
            NavigationLink {
                ProfileScreen()
            } label: {
                HStack(spacing: 12) {
                    ProfileAvatarView(
                        imageSource: appViewModel.currentUser?.profileImage ?? "",
                        initials: userInitials,
                        size: 48,
                        fontSize: 16
                    )
                    .shadow(color: HASETTheme.greenPrimary.opacity(0.10), radius: 10, x: 0, y: 6)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(appViewModel.tr("hello"))
                            .font(HASETTheme.font(.regular, 12))
                            .foregroundStyle(HASETTheme.textSecondary)
                        Text(appViewModel.currentUser?.fullName ?? "")
                            .font(HASETTheme.font(.medium, 16))
                            .foregroundStyle(HASETTheme.greenPrimary)
                            .lineLimit(1)
                    }
                }
            }
            .buttonStyle(.plain)

            Spacer()

            NavigationLink {
                DoctorsCatalogView()
            } label: {
                headerIconButton("magnifyingglass")
            }
            .buttonStyle(.plain)

            NavigationLink {
                NotificationsView()
            } label: {
                ZStack(alignment: .topTrailing) {
                    headerIconButton("bell")
                    if unreadNotificationCount > 0 {
                        Text("\(unreadNotificationCount)")
                            .font(HASETTheme.font(.medium, 9))
                            .foregroundStyle(.white)
                            .frame(width: 16, height: 16)
                            .background(Circle().fill(HASETTheme.redPrimary))
                            .offset(x: 2, y: -2)
                    }
                }
            }
            .buttonStyle(.plain)
        }
    }

    private var patientHeroCard: some View {
        TabView(selection: $selectedHeroIndex) {
            ForEach(Array(appViewModel.patientHomeHighlights.enumerated()), id: \.offset) { index, item in
                ZStack(alignment: .bottomLeading) {
                    RemoteOrAssetImage(urlString: item.imageURL, assetName: item.imageName)
                        .frame(height: 200)
                        .frame(maxWidth: .infinity)
                        .clipped()

                    if item.isImageOnly {
                        LinearGradient(
                            colors: [Color.clear, Color.black.opacity(0.42)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    } else {
                        VStack {
                            HStack {
                                Spacer()
                                Text(item.badge)
                                    .font(HASETTheme.font(.medium, 11))
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Capsule().fill(HASETTheme.greenPrimary.opacity(0.88)))
                            }
                            Spacer()
                            HStack(alignment: .bottom) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(item.titleLine1)
                                        .font(HASETTheme.font(.medium, 16))
                                        .foregroundStyle(.white.opacity(0.92))
                                    Text(item.titleLine2)
                                        .font(HASETTheme.font(.medium, 24))
                                        .foregroundStyle(.white)
                                }
                                Spacer()
                            }
                            HStack {
                                Text(item.buttonText)
                                    .font(HASETTheme.font(.medium, 11))
                                    .foregroundStyle(HASETTheme.textPrimary)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(
                                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                                            .fill(Color.white.opacity(0.92))
                                    )
                                Spacer()
                            }
                        }
                        .padding(16)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
                .tag(index)
                .padding(.horizontal, 1)
            }
        }
        .frame(height: 200)
        .tabViewStyle(.page(indexDisplayMode: .automatic))
        .onReceive(Timer.publish(every: 3.5, on: .main, in: .common).autoconnect()) { _ in
            guard role == .patient, appViewModel.patientHomeHighlights.count > 1 else { return }
            withAnimation(.easeInOut(duration: 0.4)) {
                selectedHeroIndex = (selectedHeroIndex + 1) % appViewModel.patientHomeHighlights.count
            }
        }
    }

    private var headerCard: some View {
        CardContainer {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(role == .doctor ? appViewModel.tr("doctor_dashboard") : role == .admin ? appViewModel.tr("administrator_dashboard") : appViewModel.tr("patient_dashboard"))
                        .font(HASETTheme.font(.medium, 20))
                        .foregroundStyle(HASETTheme.textPrimary)
                    Text(appViewModel.currentUser?.fullName ?? "")
                        .font(HASETTheme.font(.regular, 14))
                        .foregroundStyle(HASETTheme.textSecondary)
                    Text(role == .doctor ? "Review patients, schedule, and approvals." : appViewModel.tr("your_health_our_priority"))
                        .font(HASETTheme.font(.medium, 14))
                        .foregroundStyle(HASETTheme.greenPrimary)
                        .padding(.top, 4)
                }
                Spacer()
                Circle()
                    .fill(HASETTheme.greenPrimary.opacity(0.12))
                    .frame(width: 56, height: 56)
                    .overlay(
                        Image(systemName: role == .doctor ? "stethoscope" : role == .admin ? "person.3" : "heart.text.square")
                            .foregroundStyle(HASETTheme.greenPrimary)
                    )
            }
        }
    }

    private var patientContent: some View {
        Group {
            SectionTitle(appViewModel.tr("services"))
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 4), spacing: 14) {
                PatientServiceShortcut(title: appViewModel.tr("popular_doctors"), systemImage: "stethoscope", destination: AnyView(DoctorsCatalogView()))
                PatientServiceShortcut(title: appViewModel.tr("childrens_corner"), systemImage: "figure.2.and.child.holdinghands") {
                    showingChildrensCorner = true
                }
                PatientServiceShortcut(title: appViewModel.tr("articles"), systemImage: "newspaper", destination: AnyView(ArticlesView(role: .patient)))
                PatientServiceShortcut(title: appViewModel.tr("find_hospital"), systemImage: "cross.case", destination: AnyView(HospitalListView()))
            }

            HStack {
                SectionTitle(appViewModel.tr("popular_articles"))
                Spacer()
                NavigationLink(appViewModel.tr("view_all")) {
                    ArticlesView(role: .patient)
                }
                .font(HASETTheme.font(.medium, 12))
                .foregroundStyle(HASETTheme.greenPrimary)
            }
            VStack(spacing: 0) {
                ForEach(Array(appViewModel.patientPopularArticles.prefix(4).enumerated()), id: \.element.id) { index, article in
                    NavigationLink {
                        ArticlesView(role: .patient)
                    } label: {
                        PatientArticleCard(article: article, compact: false, showDivider: index < min(appViewModel.patientPopularArticles.count, 4) - 1)
                    }
                    .buttonStyle(.plain)
                }
            }
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(Color.white)
            )
        }
        .sheet(isPresented: $showingChildrensCorner) {
            ComingSoonSheet(title: appViewModel.tr("childrens_corner"))
        }
    }

    private var doctorContent: some View {
        Group {
            SectionTitle(appViewModel.tr("today_s_overview"))
            doctorOverviewStats

            SectionTitle(appViewModel.tr("quick_actions"))
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 3), spacing: 12) {
                NavigationLink { ScheduleView() } label: {
                    doctorActionCard(title: appViewModel.tr("schedule"), systemImage: "calendar")
                }
                .buttonStyle(.plain)

                NavigationLink { DoctorPatientsView() } label: {
                    doctorActionCard(title: appViewModel.tr("patients"), systemImage: "person.2.fill")
                }
                .buttonStyle(.plain)

                NavigationLink { ArticlesView(role: .doctor) } label: {
                    doctorActionCard(title: appViewModel.tr("articles"), systemImage: "newspaper")
                }
                .buttonStyle(.plain)
            }

            HStack(spacing: 8) {
                SectionTitle(appViewModel.tr("recent_appointments"))
                Spacer()
                NavigationLink {
                    AppointmentsOverviewView(role: .doctor)
                } label: {
                    Text(appViewModel.tr("view_all"))
                        .font(HASETTheme.font(.medium, 12))
                        .foregroundStyle(HASETTheme.greenPrimary)
                        .padding(.horizontal, 14)
                        .frame(height: 36)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(HASETTheme.greenPrimary, lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)

                Button {
                    showingDoctorFilterOptions = true
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "line.3.horizontal.decrease.circle")
                            .font(.system(size: 12, weight: .semibold))
                        Text(appViewModel.tr("filter"))
                    }
                    .font(HASETTheme.font(.medium, 12))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 14)
                    .frame(height: 36)
                    .background(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(HASETTheme.greenPrimary)
                    )
                }
                .buttonStyle(.plain)
            }

            if recentDoctorAppointments.isEmpty {
                VStack(spacing: 18) {
                    Image(systemName: "calendar")
                        .font(.system(size: 54, weight: .light))
                        .foregroundStyle(HASETTheme.textSecondary.opacity(0.8))
                        .frame(maxWidth: .infinity)
                        .padding(.top, 24)

                    Text("No Pending Appointments")
                        .font(HASETTheme.font(.medium, 16))
                        .foregroundStyle(HASETTheme.textPrimary)

                    Text("You're all caught up! New appointments will appear here.")
                        .font(HASETTheme.font(.regular, 12))
                        .foregroundStyle(HASETTheme.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 28)
                }
            } else {
                VStack(spacing: 12) {
                    ForEach(recentDoctorAppointments) { appointment in
                        doctorAppointmentCard(appointment)
                    }
                }
            }
        }
        .confirmationDialog("Filter appointments", isPresented: $showingDoctorFilterOptions, titleVisibility: .visible) {
            Button("Pending") { selectedDoctorStatus = .pending }
            Button("Approved") { selectedDoctorStatus = .approved }
            Button("Completed") { selectedDoctorStatus = .completed }
            Button("Cancelled") { selectedDoctorStatus = .cancelled }
            Button("Clear Filter", role: .destructive) { selectedDoctorStatus = nil }
        } message: {
            Text("Show recent appointments by status.")
        }
    }

    private var doctorOverviewStats: some View {
        HStack(spacing: 10) {
            NavigationLink { AppointmentsOverviewView(role: .doctor, initialStatus: .pending) } label: {
                doctorStatCard(title: appViewModel.tr("pending"), value: "\(pendingAppointmentCount)", icon: "clock", background: Color(red: 0.95, green: 0.63, blue: 0.07))
            }
            .buttonStyle(.plain)
            NavigationLink { AppointmentsOverviewView(role: .doctor, initialStatus: .completed) } label: {
                doctorStatCard(title: appViewModel.tr("completed"), value: "\(completedAppointmentCount)", icon: "checkmark.circle.fill", background: Color(red: 0.11, green: 0.77, blue: 0.50))
            }
            .buttonStyle(.plain)
            NavigationLink { AppointmentsOverviewView(role: .doctor, initialStatus: .cancelled) } label: {
                doctorStatCard(title: appViewModel.tr("cancelled"), value: "\(cancelledAppointmentCount)", icon: "xmark", background: Color(red: 0.92, green: 0.23, blue: 0.27))
            }
            .buttonStyle(.plain)
        }
    }

    private var pendingAppointmentCount: Int {
        appViewModel.appointments.filter { $0.status == .pending }.count
    }

    private var completedAppointmentCount: Int {
        appViewModel.appointments.filter { $0.status == .completed }.count
    }

    private var cancelledAppointmentCount: Int {
        appViewModel.appointments.filter { $0.status == .cancelled }.count
    }

    private var recentDoctorAppointments: [AppointmentSummary] {
        let sorted = appViewModel.appointments.sorted { lhs, rhs in
            if lhs.dateText == rhs.dateText { return lhs.id > rhs.id }
            return lhs.dateText > rhs.dateText
        }
        let filtered = selectedDoctorStatus.map { status in
            sorted.filter { $0.status == status }
        } ?? sorted
        return Array(filtered.prefix(3))
    }

    private func doctorStatCard(title: String, value: String, icon: String, background: Color) -> some View {
        VStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(.white)
            Text(value)
                .font(HASETTheme.font(.black, 22))
                .foregroundStyle(.white)
            Text(title)
                .font(HASETTheme.font(.medium, 12))
                .foregroundStyle(.white.opacity(0.92))
        }
        .frame(maxWidth: .infinity)
        .frame(height: 120)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(background)
                .shadow(color: background.opacity(0.9), radius: 10, x: 0, y: 0)
        )
    }

    private func doctorActionCard(title: String, systemImage: String) -> some View {
        VStack(spacing: 10) {
            ZStack {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(Color.white)
                    .frame(width: 60, height: 60)
                    .shadow(color: HASETTheme.greenPrimary.opacity(0.14), radius: 8, x: 0, y: 4)
                Image(systemName: systemImage)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(HASETTheme.greenPrimary)
            }
            Text(title)
                .font(HASETTheme.font(.regular, 10))
                .foregroundStyle(HASETTheme.textPrimary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
        .frame(maxWidth: .infinity)
    }

    private func doctorAppointmentCard(_ appointment: AppointmentSummary) -> some View {
        CardContainer(fill: .white, shadowColor: HASETTheme.greenPrimary.opacity(0.08), cornerRadius: 18) {
            HStack(alignment: .top, spacing: 14) {
                Circle()
                    .fill(colorForAppointment(appointment.status).opacity(0.12))
                    .frame(width: 44, height: 44)
                    .overlay(
                        Image(systemName: iconForAppointment(appointment.status))
                            .foregroundStyle(colorForAppointment(appointment.status))
                    )

                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(appointment.title)
                            .font(HASETTheme.font(.medium, 15))
                            .foregroundStyle(HASETTheme.textPrimary)
                        Spacer()
                        Text(appointment.status.localizedLabel(languageCode: appViewModel.selectedLanguage))
                            .font(HASETTheme.font(.medium, 11))
                            .foregroundStyle(colorForAppointment(appointment.status))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(
                                Capsule().fill(colorForAppointment(appointment.status).opacity(0.12))
                            )
                    }

                    Text(appointment.subtitle)
                        .font(HASETTheme.font(.regular, 13))
                        .foregroundStyle(HASETTheme.textSecondary)

                    Text(appointment.dateText)
                        .font(HASETTheme.font(.regular, 12))
                        .foregroundStyle(HASETTheme.greenPrimary)
                }
            }
        }
    }

    private func iconForAppointment(_ status: AppointmentSummary.Status) -> String {
        switch status {
        case .pending: return "clock"
        case .approved: return "checkmark.circle.fill"
        case .completed: return "checkmark.seal.fill"
        case .cancelled: return "xmark.circle.fill"
        }
    }

    private func colorForAppointment(_ status: AppointmentSummary.Status) -> Color {
        switch status {
        case .pending: return HASETTheme.redPrimary
        case .approved: return HASETTheme.greenPrimary
        case .completed: return HASETTheme.textPrimary
        case .cancelled: return HASETTheme.redPrimary
        }
    }

    private var doctorInitials: String {
        let parts = (appViewModel.currentUser?.fullName ?? "")
            .split(separator: " ")
            .prefix(2)
            .compactMap { $0.first }
        let initials = String(parts)
        return initials.isEmpty ? "H" : initials.uppercased()
    }

    private var todayDateString: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEEE, MMM d"
        return formatter.string(from: Date())
    }

    private var walletDisplayText: String {
        if doctorBalanceHidden { return "•••••• TZS" }
        guard let balance = appViewModel.doctorWallet?.balance else { return "0 TZS" }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 0
        formatter.groupingSeparator = ","
        let value = formatter.string(from: NSNumber(value: balance)) ?? String(Int(balance))
        return "\(value) TZS"
    }

    private func displayName(for user: UserProfile?, fallback: String) -> String {
        guard let user else { return fallback }
        let name = user.fullName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return fallback }
        if user.role == .doctor {
            return name.lowercased().hasPrefix("dr.") || name.lowercased().hasPrefix("dr ") ? name : "Dr. \(name)"
        }
        return name
    }

    private var adminContent: some View {
        Group {
            SectionTitle(appViewModel.tr("system_metrics"))
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(StaticContentService.adminMetrics) { metric in
                    CardContainer {
                        Text(metric.value)
                            .font(HASETTheme.font(.medium, 22))
                            .foregroundStyle(HASETTheme.greenPrimary)
                        Text(metric.title)
                            .font(HASETTheme.font(.regular, 14))
                            .foregroundStyle(HASETTheme.textSecondary)
                            .padding(.top, 4)
                    }
                }
            }

            NavigationLink { UsersView() } label: {
                FeatureStackCard(title: appViewModel.tr("all_users"), subtitle: "Patients, doctors, and administrators.")
            }
            .buttonStyle(.plain)

            NavigationLink { ActivityLogsView() } label: {
                FeatureStackCard(title: "Audit Logs", subtitle: "Registration, notification, and account activity.")
            }
            .buttonStyle(.plain)

            NavigationLink { NotificationsView() } label: {
                FeatureStackCard(title: appViewModel.tr("notifications"), subtitle: "Operational alerts and approvals.")
            }
            .buttonStyle(.plain)
        }
    }

    private func metricsRow(items: [(String, String)]) -> some View {
        HStack(spacing: 12) {
            ForEach(items, id: \.0) { item in
                CardContainer {
                    Text(item.1)
                        .font(HASETTheme.font(.medium, 20))
                        .foregroundStyle(HASETTheme.greenPrimary)
                    Text(item.0)
                        .font(HASETTheme.font(.regular, 13))
                        .foregroundStyle(HASETTheme.textSecondary)
                }
            }
        }
    }

    private var userInitials: String {
        let parts = (appViewModel.currentUser?.fullName ?? "")
            .split(separator: " ")
            .prefix(2)
            .compactMap { $0.first }
        let initials = String(parts)
        return initials.isEmpty ? "HU" : initials.uppercased()
    }

    private var unreadNotificationCount: Int {
        min(appViewModel.notifications.filter { !$0.isRead }.count, 9)
    }

    private func headerIconButton(_ systemImage: String) -> some View {
        Image(systemName: systemImage)
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(HASETTheme.greenPrimary)
            .frame(width: 40, height: 40)
            .background(
                Circle()
                    .fill(Color.white)
                    .shadow(color: HASETTheme.greenPrimary.opacity(0.10), radius: 10, x: 0, y: 6)
            )
    }
}

private struct HomeShortcut: View {
    let title: String
    let systemImage: String
    let destination: AnyView

    init(_ title: String, _ systemImage: String, _ destination: AnyView) {
        self.title = title
        self.systemImage = systemImage
        self.destination = destination
    }

    var body: some View {
        NavigationLink {
            destination
        } label: {
            CardContainer {
                VStack(alignment: .leading, spacing: 14) {
                    Image(systemName: systemImage)
                        .foregroundStyle(HASETTheme.greenPrimary)
                        .font(.system(size: 24))
                    Text(title)
                        .font(HASETTheme.font(.medium, 15))
                        .foregroundStyle(HASETTheme.textPrimary)
                }
            }
        }
        .buttonStyle(.plain)
    }
}

private struct PatientServiceShortcut: View {
    let title: String
    let systemImage: String
    let destination: AnyView?
    let action: (() -> Void)?

    init(title: String, systemImage: String, destination: AnyView) {
        self.title = title
        self.systemImage = systemImage
        self.destination = destination
        self.action = nil
    }

    init(title: String, systemImage: String, action: @escaping () -> Void) {
        self.title = title
        self.systemImage = systemImage
        self.destination = nil
        self.action = action
    }

    var body: some View {
        Group {
            if let destination {
                NavigationLink {
                    destination
                } label: {
                    shortcutContent
                }
            } else {
                Button(action: { action?() }) {
                    shortcutContent
                }
            }
        }
        .buttonStyle(.plain)
    }

    private var shortcutContent: some View {
        VStack(spacing: 8) {
            Circle()
                .fill(Color.white)
                .frame(width: 58, height: 58)
                .overlay(
                    Image(systemName: systemImage)
                        .font(.system(size: 20, weight: .medium))
                        .foregroundStyle(HASETTheme.greenPrimary)
                )
                .shadow(color: HASETTheme.greenPrimary.opacity(0.10), radius: 12, x: 0, y: 7)
            Text(title)
                .font(HASETTheme.font(.regular, 10))
                .foregroundStyle(HASETTheme.textPrimary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct ComingSoonSheet: View {
    let title: String
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 22) {
                Spacer()
                Image(systemName: "sparkles.rectangle.stack")
                    .font(.system(size: 64, weight: .medium))
                    .foregroundStyle(HASETTheme.greenPrimary)
                    .frame(width: 160, height: 160)
                    .background(
                        RoundedRectangle(cornerRadius: 32, style: .continuous)
                            .fill(HASETTheme.greenPrimary.opacity(0.10))
                    )

                Text(title)
                    .font(HASETTheme.font(.medium, 24))
                    .foregroundStyle(HASETTheme.textPrimary)

                Text(appViewModel.tr("coming_soon_message"))
                    .font(HASETTheme.font(.regular, 14))
                    .foregroundStyle(HASETTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 28)

                Button(appViewModel.tr("sounds_good")) {
                    dismiss()
                }
                .buttonStyle(PrimaryButtonStyle())
                .padding(.horizontal, 20)
                Spacer()
            }
        }
        .presentationDetents([.medium])
    }
}

private struct PatientArticleCard: View {
    let article: ArticleSummary
    var compact: Bool = true
    var showDivider: Bool = false

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                if article.hasVisual {
                    RemoteOrAssetImage(urlString: article.imageURL, assetName: article.imageName)
                        .frame(width: 72, height: 72)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text(article.title)
                        .font(HASETTheme.font(.medium, article.hasVisual ? 16 : 17))
                        .foregroundStyle(HASETTheme.textPrimary)
                        .lineLimit(article.hasVisual ? 2 : 3)
                    if !article.hasVisual && !article.excerpt.isEmpty {
                        Text(article.excerpt)
                            .font(HASETTheme.font(.regular, 13))
                            .foregroundStyle(HASETTheme.textSecondary)
                            .lineLimit(2)
                    }
                    HStack(spacing: 6) {
                        Image(systemName: "eye")
                            .font(.system(size: 12, weight: .medium))
                        Text(formattedViews)
                            .font(HASETTheme.font(.regular, 12))
                    }
                    .foregroundStyle(HASETTheme.textSecondary)
                }

                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(HASETTheme.textSecondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)

            if showDivider {
                Divider()
                    .overlay(HASETTheme.divider)
                    .padding(.leading, article.hasVisual ? 102 : 16)
            }
        }
    }

    private var formattedViews: String {
        let views = article.viewCount
        if views < 1000 { return "\(views) views" }
        if views < 1_000_000 {
            return String(format: "%.1fk views", Double(views) / 1000.0)
        }
        return String(format: "%.1fM views", Double(views) / 1_000_000.0)
    }
}

private struct FeedArticleCard: View {
    let article: ArticleSummary
    let isSaved: Bool
    let isLiked: Bool
    let onOpen: () -> Void
    let onToggleLike: () -> Void
    let onComment: () -> Void
    let onShare: () -> Void
    let onToggleSave: () -> Void

    var body: some View {
        Button(action: onOpen) {
            VStack(spacing: 0) {
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 12) {
                        ProfileAvatarView(
                            imageSource: article.authorImage ?? "",
                            initials: article.author.split(separator: " ").prefix(2).compactMap(\.first).map(String.init).joined().uppercased(),
                            size: 40,
                            fontSize: 13
                        )

                        VStack(alignment: .leading, spacing: 2) {
                            Text(article.author)
                                .font(HASETTheme.font(.medium, 15))
                                .foregroundStyle(HASETTheme.textPrimary)
                            Text(relativeTime)
                                .font(HASETTheme.font(.regular, 12))
                                .foregroundStyle(HASETTheme.textSecondary)
                        }

                        Spacer()

                        Button(action: onToggleSave) {
                            Image(systemName: isSaved ? "bookmark.fill" : "bookmark")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(isSaved ? HASETTheme.greenPrimary : HASETTheme.textSecondary)
                                .frame(width: 30, height: 30)
                        }
                        .buttonStyle(.plain)
                    }

                    if article.hasVisual {
                        RemoteOrAssetImage(urlString: article.imageURL, assetName: article.imageName)
                            .frame(height: 220)
                            .frame(maxWidth: .infinity)
                            .clipped()
                    }

                    if !article.title.isEmpty {
                        Text(article.title)
                            .font(HASETTheme.font(.medium, 18))
                            .foregroundStyle(HASETTheme.textPrimary)
                            .multilineTextAlignment(.leading)
                    }

                    Text(article.excerpt)
                        .font(HASETTheme.font(.regular, 14))
                        .foregroundStyle(HASETTheme.textPrimary)
                        .lineLimit(5)
                        .multilineTextAlignment(.leading)

                    HStack(spacing: 18) {
                        Button(action: onToggleLike) {
                            Label("\(article.likeCount) Likes", systemImage: isLiked ? "heart.fill" : "heart")
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(isLiked ? HASETTheme.redPrimary : HASETTheme.textSecondary)

                        Button(action: onComment) {
                            Label("\(article.commentCount) Comments", systemImage: "message")
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(HASETTheme.textSecondary)

                        Button(action: onShare) {
                            Label("\(article.shareCount) Shares", systemImage: "square.and.arrow.up")
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(HASETTheme.textSecondary)
                    }
                    .font(HASETTheme.font(.regular, 12))
                }
                .padding(16)
            }
            .background(HASETTheme.backgroundCard)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .shadow(color: HASETTheme.greenPrimary.opacity(0.08), radius: 10, x: 0, y: 6)
        }
        .buttonStyle(.plain)
    }

    private var relativeTime: String {
        guard article.timestamp > 0 else { return article.readTime }
        let seconds = article.timestamp > 1_000_000_000_000 ? article.timestamp / 1000.0 : article.timestamp
        let interval = max(0, Date().timeIntervalSince1970 - seconds)
        let hours = Int(interval / 3600)
        if hours < 1 { return "Just now" }
        if hours < 24 { return "\(hours)h ago" }
        let days = hours / 24
        if days < 7 { return "\(days)d ago" }
        return article.readTime
    }
}

private struct RemoteOrAssetImage: View {
    let urlString: String?
    let assetName: String?

    var body: some View {
        if let urlString, let url = URL(string: urlString), !urlString.isEmpty {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                default:
                    placeholder
                }
            }
        } else if let assetName, !assetName.isEmpty {
            Image(assetName)
                .resizable()
                .scaledToFill()
        } else {
            placeholder
        }
    }

    private var placeholder: some View {
        ZStack {
            Rectangle()
                .fill(HASETTheme.greenPrimary.opacity(0.12))
            Image(systemName: "newspaper")
                .font(.system(size: 30))
                .foregroundStyle(HASETTheme.greenPrimary)
        }
    }
}

private struct SectionTitle: View {
    let text: String

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text)
            .font(HASETTheme.font(.medium, 18))
            .foregroundStyle(HASETTheme.textPrimary)
    }
}

private struct DoctorWalletView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    let isHidden: Bool
    @State private var showWithdraw = false
    @State private var amount = ""
    @State private var mfaCode = ""
    @State private var submitting = false
    @State private var message: String?
    @State private var showNoBalanceAlert = false
    @State private var checkingMFA = false
    @State private var showMFARequired = false
    @State private var showMFAEnrollment = false
    @State private var payoutMethod = "mobile_money"
    @State private var showPayoutAccounts = false
    @State private var payoutSetupAfterMFA = false
    @State private var destinationType = "mobile_money"
    @State private var destinationProvider = ""
    @State private var destinationPhone = ""
    @State private var destinationBank = ""
    @State private var destinationAccount = ""
    @State private var destinationMFA = ""
    @State private var destinationMessage: String?
    @State private var savingDestination = false

    private var balanceText: String {
        if isHidden { return "•••••• TZS" }
        guard let balance = appViewModel.doctorWallet?.balance else { return "0 TZS" }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 0
        formatter.groupingSeparator = ","
        let value = formatter.string(from: NSNumber(value: balance)) ?? String(Int(balance))
        return "\(value) TZS"
    }

    private var earningsText: String {
        guard let earnings = appViewModel.doctorWallet?.totalEarnings else { return "—" }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 0
        formatter.groupingSeparator = ","
        let value = formatter.string(from: NSNumber(value: earnings)) ?? String(Int(earnings))
        return "\(value) TZS"
    }

    private var updatedText: String {
        guard let updated = appViewModel.doctorWallet?.lastUpdated else { return "Unknown" }
        let date = Date(timeIntervalSince1970: updated / 1000)
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                SectionTitle("Wallet")
                CardContainer {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Image("icons8_coins")
                                .resizable()
                                .renderingMode(.original)
                                .scaledToFit()
                                .frame(width: 22, height: 22)
                            Spacer()
                            Text("Updated \(updatedText)")
                                .font(HASETTheme.font(.regular, 12))
                                .foregroundStyle(HASETTheme.textSecondary)
                        }

                        Text(balanceText)
                            .font(HASETTheme.font(.black, 28))
                            .foregroundStyle(HASETTheme.textPrimary)

                        HStack(spacing: 12) {
                            CardContainer(fill: HASETTheme.greenPrimary.opacity(0.08), shadowColor: .clear, cornerRadius: 14, padding: 14) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Total Earnings")
                                        .font(HASETTheme.font(.regular, 12))
                                        .foregroundStyle(HASETTheme.textSecondary)
                                    Text(earningsText)
                                        .font(HASETTheme.font(.medium, 16))
                                        .foregroundStyle(HASETTheme.textPrimary)
                                }
                            }

                            CardContainer(fill: Color.orange.opacity(0.10), shadowColor: .clear, cornerRadius: 14, padding: 14) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Doctor ID")
                                        .font(HASETTheme.font(.regular, 12))
                                        .foregroundStyle(HASETTheme.textSecondary)
                                    Text(appViewModel.doctorWallet?.doctorId ?? appViewModel.currentUser?.userId ?? "—")
                                        .font(HASETTheme.font(.medium, 16))
                                        .foregroundStyle(HASETTheme.textPrimary)
                                        .lineLimit(1)
                                }
                            }
                        }
                    }
                }
                Button(checkingMFA ? "Checking security…" : "Withdraw") {
                    if (appViewModel.doctorWallet?.balance ?? 0) > 0 {
                        checkMfaThenWithdraw()
                    } else {
                        showNoBalanceAlert = true
                    }
                }
                    .buttonStyle(PrimaryButtonStyle())
                    .disabled(checkingMFA)
                Button("Payout accounts") { checkMfaThenConfigurePayout() }
                    .buttonStyle(.bordered)
                    .disabled(checkingMFA)
                SectionTitle("Withdrawal history")
                if appViewModel.doctorWalletLoading && appViewModel.doctorWithdrawals.isEmpty { ProgressView() }
                else if let error = appViewModel.doctorWalletError { VStack { Text(error); Button("Retry") { Task { await appViewModel.loadDoctorWithdrawals() } } } }
                else if appViewModel.doctorWithdrawals.isEmpty { Text("No withdrawals yet.").foregroundStyle(HASETTheme.textSecondary) }
                else { ForEach(appViewModel.doctorWithdrawals) { item in
                    CardContainer {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.id).font(.caption)
                                Text(String(format: "%,.0f TZS", item.amount)).font(.headline)
                                if item.feeAmount > 0 {
                                    Text(String(format: "Provider fee: %,.0f TZS", item.feeAmount))
                                        .font(.caption)
                                        .foregroundStyle(HASETTheme.textSecondary)
                                }
                            }
                            Spacer()
                            Text(item.status.capitalized)
                                .foregroundStyle(item.status.lowercased() == "paid" || item.status.lowercased() == "completed" ? HASETTheme.greenPrimary : .orange)
                        }
                    }
                } }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle("Wallet")
        .navigationBarTitleDisplayMode(.inline)
        .task { await appViewModel.loadDoctorWithdrawals() }
        .refreshable { await appViewModel.loadDoctorWithdrawals() }
        .alert("Withdrawal unavailable", isPresented: $showNoBalanceAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("No balance is available for withdrawal.")
        }
        .alert("Enable MFA to withdraw", isPresented: $showMFARequired) {
            Button("Cancel", role: .cancel) {}
            Button("Enable MFA") { showMFAEnrollment = true }
        } message: {
            Text("Withdrawals require multi-factor authentication. Enable MFA first; you may disable it later from Settings when you are not withdrawing.")
        }
        .sheet(isPresented: $showMFAEnrollment) {
            MFAEnrollmentView(
                onComplete: {
                    showMFAEnrollment = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                        if payoutSetupAfterMFA { showPayoutAccounts = true }
                        else { selectDefaultPayoutMethod(); showWithdraw = true }
                    }
                },
                onCancel: { showMFAEnrollment = false }
            )
            .environmentObject(appViewModel)
        }
        .sheet(isPresented: $showWithdraw) {
            VStack(spacing: 16) {
                Text("Request withdrawal").font(.title3.bold())
                if availablePayoutMethods.isEmpty {
                    Text("No approved payout account is available. Contact finance support.").foregroundStyle(.red)
                } else {
                    Picker("Payout account", selection: $payoutMethod) {
                        ForEach(availablePayoutMethods, id: \.0) { method in Text(method.1).tag(method.0) }
                    }.pickerStyle(.segmented)
                    Text(selectedDestinationLabel).font(.caption).foregroundStyle(HASETTheme.textSecondary)
                }
                TextField("Amount", text: $amount).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
                SixDigitMFAInput(code: $mfaCode, isInvalid: false, isVerified: false) {}
                if let message { Text(message).foregroundStyle(.red) }
                Button(submitting ? "Submitting…" : "Submit") { submitWithdrawal() }.buttonStyle(PrimaryButtonStyle()).disabled(submitting || availablePayoutMethods.isEmpty)
                Button("Cancel") { clearPayoutState(); showWithdraw = false }
            }.padding(24).interactiveDismissDisabled(submitting)
        }
        .sheet(isPresented: $showPayoutAccounts) {
            ScrollView {
                VStack(spacing: 16) {
                    Text("Payout accounts").font(.title3.bold())
                    Text("Choose where you receive payouts. Finance must approve a change before it can be used.")
                        .font(.caption).foregroundStyle(HASETTheme.textSecondary)
                    Picker("Account type", selection: $destinationType) {
                        Text("Mobile Money").tag("mobile_money")
                        Text("Bank Account").tag("bank")
                    }.pickerStyle(.segmented)
                    if destinationType == "bank" {
                        TextField("Bank code or bank name", text: $destinationBank).textFieldStyle(.roundedBorder)
                        TextField("Bank account number", text: $destinationAccount).textFieldStyle(.roundedBorder)
                    } else {
                        TextField("Provider (M-Pesa, Airtel Money, Mixx by Yas)", text: $destinationProvider).textFieldStyle(.roundedBorder)
                        TextField("Mobile number", text: $destinationPhone).keyboardType(.phonePad).textFieldStyle(.roundedBorder)
                    }
                    SixDigitMFAInput(code: $destinationMFA, isInvalid: destinationMessage != nil, isVerified: false) {}
                    if let destinationMessage { Text(destinationMessage).foregroundStyle(.red).font(.caption) }
                    Button(savingDestination ? "Submitting…" : "Submit for approval") { submitPayoutDestination() }
                        .buttonStyle(PrimaryButtonStyle()).disabled(savingDestination)
                    Button("Cancel") { clearDestinationState(); showPayoutAccounts = false }
                }.padding(24)
            }.interactiveDismissDisabled(savingDestination)
        }
    }

    private var availablePayoutMethods: [(String, String)] {
        var methods: [(String, String)] = []
        if appViewModel.doctorWallet?.mobileMoneyDestination?.available == true { methods.append(("mobile_money", "Mobile Money")) }
        if appViewModel.doctorWallet?.bankDestination?.available == true { methods.append(("bank", "Bank")) }
        return methods
    }

    private var selectedDestinationLabel: String {
        payoutMethod == "bank" ? (appViewModel.doctorWallet?.bankDestination?.label ?? "Bank") : (appViewModel.doctorWallet?.mobileMoneyDestination?.label ?? "Mobile Money")
    }

    private func checkMfaThenWithdraw() {
        guard let session = appViewModel.activeSession ?? SessionStore().loadSession() else {
            appViewModel.alertState = AlertState(title: "Authentication", message: "Authentication expired. Please sign in again.")
            return
        }
        checkingMFA = true
        payoutSetupAfterMFA = false
        Task {
            do {
                let service = AuthService()
                let freshSession = try await service.refreshSessionIfNeeded(session)
                SessionStore().saveSession(freshSession)
                appViewModel.activeSession = freshSession
                let enabled = try await service.mobileMFAStatus(idToken: freshSession.idToken)
                checkingMFA = false
                if enabled { selectDefaultPayoutMethod(); showWithdraw = true }
                else { showMFARequired = true }
            } catch {
                checkingMFA = false
                appViewModel.alertState = AlertState(title: "MFA", message: error.localizedDescription)
            }
        }
    }

    private func checkMfaThenConfigurePayout() {
        guard let session = appViewModel.activeSession ?? SessionStore().loadSession() else {
            appViewModel.alertState = AlertState(title: "Authentication", message: "Authentication expired. Please sign in again.")
            return
        }
        checkingMFA = true
        payoutSetupAfterMFA = true
        Task {
            do {
                let service = AuthService()
                let fresh = try await service.refreshSessionIfNeeded(session)
                SessionStore().saveSession(fresh); appViewModel.activeSession = fresh
                let enabled = try await service.mobileMFAStatus(idToken: fresh.idToken)
                checkingMFA = false
                if enabled { showPayoutAccounts = true } else { showMFARequired = true }
            } catch {
                checkingMFA = false
                appViewModel.alertState = AlertState(title: "MFA", message: error.localizedDescription)
            }
        }
    }

    private func selectDefaultPayoutMethod() {
        if !availablePayoutMethods.contains(where: { $0.0 == payoutMethod }) {
            payoutMethod = availablePayoutMethods.first?.0 ?? "mobile_money"
        }
    }

    private func submitWithdrawal() {
        guard let value = Int(amount), value >= 5000, Double(value) <= (appViewModel.doctorWallet?.balance ?? 0) else { message = "Enter a valid amount within your available balance."; return }
        guard mfaCode.count == 6 else { message = "Enter the six-digit MFA code."; return }
        guard let session = SessionStore().loadSession() else { message = "Authentication expired."; return }
        submitting = true; message = nil
        Task { do {
            guard let token = try await AuthService().verifyMobileMFA(code: mfaCode, idToken: session.idToken) else {
                throw ServiceError.message("Payouts require a current authenticator code; recovery codes are for account access only.")
            }
            try await AuthService().requestDoctorWithdrawal(amount: value, reason: "Doctor payout request", payoutMethod: payoutMethod, idToken: session.idToken, mfaActionToken: token)
            await appViewModel.loadDoctorWithdrawals()
            await MainActor.run { clearPayoutState(); showWithdraw = false }
        } catch { await MainActor.run { submitting = false; message = error.localizedDescription; mfaCode = "" } } }
    }
    private func submitPayoutDestination() {
        let provider = destinationProvider.trimmingCharacters(in: .whitespacesAndNewlines)
        let phone = destinationPhone.trimmingCharacters(in: .whitespacesAndNewlines)
        let bank = destinationBank.trimmingCharacters(in: .whitespacesAndNewlines)
        let account = destinationAccount.trimmingCharacters(in: .whitespacesAndNewlines)
        if destinationType == "bank" {
            guard !bank.isEmpty, account.count >= 5 else { destinationMessage = "Enter a valid bank and account number."; return }
        } else {
            guard !provider.isEmpty, phone.range(of: "^(?:0\\d{9}|\\+255\\d{9})$", options: .regularExpression) != nil else { destinationMessage = "Use 07XXXXXXXX or +255XXXXXXXXX."; return }
        }
        guard destinationMFA.count == 6 else { destinationMessage = "Enter the six-digit MFA code."; return }
        guard let session = SessionStore().loadSession() else { destinationMessage = "Authentication expired."; return }
        savingDestination = true; destinationMessage = nil
        Task { do {
            guard let token = try await AuthService().verifyMobileMFA(code: destinationMFA, idToken: session.idToken) else { throw ServiceError.message("A current authenticator code is required.") }
            try await AuthService().updateDoctorPayoutDestination(type: destinationType, provider: provider, phone: phone, bankCode: bank, bankAccount: account, idToken: session.idToken, mfaActionToken: token)
            await MainActor.run {
                clearDestinationState(); showPayoutAccounts = false
                appViewModel.alertState = AlertState(title: "Payout account submitted", message: "Finance approval and the security cooling-off period are required before withdrawal.")
            }
        } catch { await MainActor.run { savingDestination = false; destinationMessage = error.localizedDescription; destinationMFA = "" } } }
    }
    private func clearDestinationState() {
        destinationProvider = ""; destinationPhone = ""; destinationBank = ""; destinationAccount = ""
        destinationMFA = ""; destinationMessage = nil; savingDestination = false
    }
    private func clearPayoutState() { amount = ""; mfaCode = ""; submitting = false; message = nil }
}

private struct FeatureStackCard: View {
    let title: String
    let subtitle: String

    var body: some View {
        CardContainer {
            HStack {
                VStack(alignment: .leading, spacing: 6) {
                    Text(title)
                        .font(HASETTheme.font(.medium, 17))
                        .foregroundStyle(HASETTheme.textPrimary)
                    Text(subtitle)
                        .font(HASETTheme.font(.regular, 13))
                        .foregroundStyle(HASETTheme.textSecondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(HASETTheme.textSecondary)
            }
        }
    }
}

private struct DoctorCardView: View {
    let doctor: DoctorSummary
    @EnvironmentObject private var appViewModel: AppViewModel

    var body: some View {
        CardContainer {
            HStack(alignment: .top) {
                ProfileAvatarView(
                    imageSource: doctor.profileImage ?? "",
                    initials: doctor.name.split(separator: " ").prefix(2).compactMap(\.first).map(String.init).joined().uppercased(),
                    size: 64,
                    fontSize: 20
                )
                VStack(alignment: .leading, spacing: 6) {
                    Text(doctor.name)
                        .font(HASETTheme.font(.medium, 16))
                        .foregroundStyle(HASETTheme.textPrimary)
                    Text(doctor.specialty)
                        .font(HASETTheme.font(.regular, 14))
                        .foregroundStyle(HASETTheme.textSecondary)
                    Text(doctor.hospital)
                        .font(HASETTheme.font(.regular, 13))
                        .foregroundStyle(HASETTheme.textSecondary)
                    HStack(spacing: 10) {
                        Label(String(format: "%.1f", doctor.rating), systemImage: "star.fill")
                        Text(doctor.consultationFee)
                    }
                    .font(HASETTheme.font(.regular, 12))
                    .foregroundStyle(HASETTheme.greenPrimary)
                }
                Spacer()
                Text(doctor.availableToday ? appViewModel.tr("today") : appViewModel.tr("booked"))
                    .font(HASETTheme.font(.medium, 12))
                    .foregroundStyle(doctor.availableToday ? HASETTheme.greenPrimary : HASETTheme.redPrimary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(
                        Capsule().fill((doctor.availableToday ? HASETTheme.greenPrimary : HASETTheme.redPrimary).opacity(0.12))
                    )
            }
        }
    }
}

struct DoctorDetailView: View {
    let doctor: DoctorSummary
    @EnvironmentObject private var appViewModel: AppViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                DoctorCardView(doctor: doctor)
                CardContainer {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("About")
                            .font(HASETTheme.font(.medium, 16))
                        Text((doctor.bio?.isEmpty == false ? doctor.bio! : "No bio available"))
                            .font(HASETTheme.font(.regular, 14))
                            .foregroundStyle(HASETTheme.textSecondary)
                    }
                }
                CardContainer {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Contact Information")
                            .font(HASETTheme.font(.medium, 16))
                        detailRow(systemImage: "phone.fill", value: doctor.phoneNumber ?? "Not available")
                        detailRow(systemImage: "envelope.fill", value: doctor.email ?? "Not available")
                        detailRow(systemImage: "location.fill", value: doctor.address ?? doctor.hospital)
                    }
                }
                CardContainer {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Available & Fees")
                            .font(HASETTheme.font(.medium, 16))
                        detailRow(systemImage: "clock.fill", value: availabilityText)
                        detailRow(systemImage: "banknote.fill", value: doctor.consultationFee)
                    }
                }
                NavigationLink {
                    BookAppointmentView(doctor: doctor)
                } label: {
                    Text(appViewModel.tr("book_appointment"))
                }
                .buttonStyle(PrimaryButtonStyle())
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("doctor_details"))
    }

    private var availabilityText: String {
        let times = doctor.availableTimes ?? Array(StaticContentService.timeSlots.prefix(6))
        guard !times.isEmpty else { return "Contact for availability" }
        if times.count == 1 { return times[0] }
        return "\(times.first ?? "") - \(times.last ?? "")"
    }

    private func detailRow(systemImage: String, value: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: systemImage)
                .foregroundStyle(HASETTheme.greenPrimary)
                .frame(width: 20)
            Text(value)
                .font(HASETTheme.font(.regular, 14))
                .foregroundStyle(HASETTheme.textSecondary)
        }
    }
}

struct BookAppointmentView: View {
    private enum AppointmentMode {
        case instant
        case schedule
    }

    let doctor: DoctorSummary
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var expandedMode: AppointmentMode?
    @State private var appointmentType = "Visit"
    @State private var paymentMethod: PaymentCheckoutView.PaymentMethod = .mobileMoney
    @State private var selectedDate = ""
    @State private var selectedTime = ""
    @State private var reason = ""
    @State private var showDatePicker = false
    @State private var showTimePicker = false
    @State private var showPaymentSheet = false
    @State private var dateValue = Date()
    @State private var timeValue = Date()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                bookingDoctorCard

                Text(appViewModel.tr("choose_appointment_type"))
                    .font(HASETTheme.font(.medium, 18))
                    .foregroundStyle(HASETTheme.textPrimary)

                appointmentSection(
                    title: appViewModel.tr("instant_appointment"),
                    subtitle: appViewModel.tr("connect_immediately"),
                    systemImage: "clock.fill",
                    mode: .instant
                ) {
                    HStack(spacing: 12) {
                        instantOptionCard(
                            title: "Online Chat",
                            systemImage: "message.fill",
                            isSelected: appointmentType == "Online Chat"
                        ) {
                            selectInstantAppointment(type: "Online Chat")
                        }

                        instantOptionCard(
                            title: "Video Call",
                            systemImage: "video.fill",
                            isSelected: appointmentType == "Video Call"
                        ) {
                            selectInstantAppointment(type: "Video Call")
                        }
                    }
                }

                appointmentSection(
                    title: appViewModel.tr("schedule_appointment"),
                    subtitle: appViewModel.tr("schedule_for_future"),
                    systemImage: "calendar",
                    mode: .schedule
                ) {
                    VStack(spacing: 14) {
                        bookingSelector(
                            title: appViewModel.tr("select_date"),
                            systemImage: "calendar",
                            value: selectedDate
                        ) {
                            showDatePicker = true
                        }

                        bookingSelector(
                            title: appViewModel.tr("select_time"),
                            systemImage: "clock",
                            value: selectedTime
                        ) {
                            showTimePicker = true
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text(appViewModel.tr("reason_for_visit"))
                                .font(HASETTheme.font(.medium, 13))
                                .foregroundStyle(HASETTheme.textPrimary)
                            TextField(appViewModel.tr("reason_for_visit"), text: $reason, axis: .vertical)
                                .font(HASETTheme.font(.regular, 14))
                                .padding(16)
                                .background(
                                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                                        .fill(Color.white)
                                        .shadow(color: HASETTheme.greenPrimary.opacity(0.06), radius: 8, x: 0, y: 5)
                                )
                        }
                    }
                }

                Button(confirmButtonTitle) {
                    if requiresPayment {
                        showPaymentSheet = true
                    } else {
                        submitAppointment()
                    }
                }
                .buttonStyle(PrimaryButtonStyle())
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("book_appointment"))
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showDatePicker) {
            bookingDateSheet
        }
        .sheet(isPresented: $showTimePicker) {
            bookingTimeSheet
        }
        .sheet(isPresented: $showPaymentSheet) {
            PaymentCheckoutView(
                doctor: doctor,
                amount: consultationFeeAmount,
                initialMethod: paymentMethod,
                onPaymentConfirmed: {
                    submitAppointment()
                    showPaymentSheet = false
                }
            )
        }
    }

    private var confirmButtonTitle: String {
        appointmentType == "Online Chat" ? appViewModel.tr("book_online_chat") : appViewModel.tr("book_appointment")
    }

    private var consultationFeeAmount: Double {
        let digits = doctor.consultationFee.filter { $0.isNumber || $0 == "." }
        return Double(digits) ?? 0
    }

    private var requiresPayment: Bool {
        consultationFeeAmount > 0
    }

    private var bookingDoctorCard: some View {
        CardContainer {
            HStack(spacing: 14) {
                ZStack(alignment: .bottomTrailing) {
                    ProfileAvatarView(
                        imageSource: doctor.profileImage ?? "",
                        initials: doctor.name.split(separator: " ").prefix(2).compactMap(\.first).map(String.init).joined().uppercased(),
                        size: 60,
                        fontSize: 18
                    )
                    if doctor.verified {
                        Image(systemName: "checkmark.seal.fill")
                            .font(.system(size: 16))
                            .foregroundStyle(HASETTheme.greenPrimary)
                            .padding(2)
                            .background(Circle().fill(Color.white))
                    }
                }

                VStack(alignment: .leading, spacing: 5) {
                    Text(doctor.name)
                        .font(HASETTheme.font(.medium, 17))
                        .foregroundStyle(HASETTheme.textPrimary)
                    Text(doctor.specialty)
                        .font(HASETTheme.font(.medium, 14))
                        .foregroundStyle(HASETTheme.greenPrimary)
                    HStack(spacing: 4) {
                        Text("Fee:")
                            .font(HASETTheme.font(.regular, 13))
                            .foregroundStyle(HASETTheme.textSecondary)
                        Text(doctor.consultationFee)
                            .font(HASETTheme.font(.medium, 14))
                            .foregroundStyle(HASETTheme.greenPrimary)
                    }
                }
                Spacer()
            }
        }
    }

    private func appointmentSection<Content: View>(
        title: String,
        subtitle: String,
        systemImage: String,
        mode: AppointmentMode,
        @ViewBuilder content: () -> Content
    ) -> some View {
        CardContainer {
            VStack(spacing: 0) {
                Button {
                    withAnimation(.easeInOut(duration: 0.25)) {
                        if expandedMode == mode {
                            expandedMode = nil
                        } else {
                            expandedMode = mode
                            if mode == .instant {
                                selectInstantAppointment(type: appointmentType == "Video Call" ? "Video Call" : "Online Chat")
                            } else {
                                appointmentType = "Visit"
                            }
                        }
                    }
                } label: {
                    HStack(spacing: 14) {
                        Circle()
                            .fill(HASETTheme.greenPrimary.opacity(0.12))
                            .frame(width: 48, height: 48)
                            .overlay(
                                Image(systemName: systemImage)
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundStyle(HASETTheme.greenPrimary)
                            )

                        VStack(alignment: .leading, spacing: 2) {
                            Text(title)
                                .font(HASETTheme.font(.medium, 16))
                                .foregroundStyle(HASETTheme.textPrimary)
                            Text(subtitle)
                                .font(HASETTheme.font(.regular, 12))
                                .foregroundStyle(HASETTheme.textSecondary)
                        }

                        Spacer()

                        Image(systemName: "chevron.down")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(HASETTheme.greenPrimary)
                            .rotationEffect(.degrees(expandedMode == mode ? 180 : 0))
                    }
                }
                .buttonStyle(.plain)

                if expandedMode == mode {
                    VStack(spacing: 14) {
                        Divider()
                            .overlay(HASETTheme.divider)
                            .padding(.top, 14)
                        content()
                    }
                }
            }
        }
    }

    private func bookingSelector(title: String, systemImage: String, value: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: systemImage)
                    .foregroundStyle(HASETTheme.textSecondary)
                Text(value.isEmpty ? title : value)
                    .font(HASETTheme.font(.regular, 14))
                    .foregroundStyle(value.isEmpty ? HASETTheme.textSecondary : HASETTheme.textPrimary)
                Spacer()
            }
            .padding(.horizontal, 16)
            .frame(height: 56)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Color.white)
                    .shadow(color: HASETTheme.greenPrimary.opacity(0.06), radius: 8, x: 0, y: 5)
            )
        }
        .buttonStyle(.plain)
    }

    private func instantOptionCard(title: String, systemImage: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(.system(size: 24, weight: .semibold))
                Text(title)
                    .font(HASETTheme.font(.medium, 13))
                    .multilineTextAlignment(.center)
            }
            .foregroundStyle(isSelected ? .white : HASETTheme.textPrimary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(isSelected ? HASETTheme.greenPrimary : HASETTheme.backgroundPrimary)
            )
        }
        .buttonStyle(.plain)
    }

    private var bookingDateSheet: some View {
        NavigationStack {
            VStack {
                DatePicker(
                    appViewModel.tr("select_date"),
                    selection: $dateValue,
                    in: Date()...,
                    displayedComponents: .date
                )
                .datePickerStyle(.graphical)
                .padding()

                Button(appViewModel.tr("use_date")) {
                    let formatter = DateFormatter()
                    formatter.dateFormat = "dd MMM yyyy"
                    selectedDate = formatter.string(from: dateValue)
                    showDatePicker = false
                }
                .buttonStyle(PrimaryButtonStyle())
                .padding(.horizontal, 20)
                Spacer()
            }
            .navigationTitle(appViewModel.tr("select_date"))
            .navigationBarTitleDisplayMode(.inline)
        }
        .presentationDetents([.medium])
    }

    private var bookingTimeSheet: some View {
        NavigationStack {
            VStack {
                DatePicker(
                    appViewModel.tr("select_time"),
                    selection: $timeValue,
                    displayedComponents: .hourAndMinute
                )
                .datePickerStyle(.wheel)
                .labelsHidden()
                .padding()

                Button(appViewModel.tr("use_time")) {
                    let formatter = DateFormatter()
                    formatter.dateFormat = "HH:mm"
                    selectedTime = formatter.string(from: timeValue)
                    showTimePicker = false
                }
                .buttonStyle(PrimaryButtonStyle())
                .padding(.horizontal, 20)
                Spacer()
            }
            .navigationTitle(appViewModel.tr("select_time"))
            .navigationBarTitleDisplayMode(.inline)
        }
        .presentationDetents([.medium])
    }

    private func selectInstantAppointment(type: String) {
        appointmentType = type
        let now = Date()
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "dd MMM yyyy"
        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm"
        selectedDate = dateFormatter.string(from: now)
        selectedTime = timeFormatter.string(from: now)
        dateValue = now
        timeValue = now
    }

    private func submitAppointment() {
        appViewModel.bookAppointment(
            doctor: doctor,
            date: selectedDate,
            time: selectedTime,
            reason: reason,
            appointmentType: appointmentType
        )
    }
}

struct PaymentCheckoutView: View {
    enum PaymentMethod: String {
        case mobileMoney
        case cardPayment
    }

    let doctor: DoctorSummary
    let amount: Double
    let initialMethod: PaymentMethod
    let onPaymentConfirmed: () -> Void

    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var selectedMethod: PaymentMethod
    @State private var selectedProvider = "Vodacom"
    @State private var walletNumber = ""
    @State private var isProcessing = false
    @State private var statusMessage = ""
    @State private var transactionId: Int?
    @State private var canRetryStatus = false
    @State private var pollTask: Task<Void, Never>?
    @State private var processingPulse = false
    @State private var consultationId: String
    @State private var idempotencyKey: String
    @State private var paymentSessionReady = false
    @State private var didRetryPaymentAuth = false
    @State private var paymentAuthSession: StoredSession?
    @State private var showingCancelConfirmation = false
    @State private var hostedCheckout: HostedCheckoutDestination?

    private let paymentService = AuthService()
    private let sessionStore = SessionStore()
    private let mobileProviders: [ProviderOption] = [
        ProviderOption(name: "Vodacom", displayName: "Mpesa", imageName: "a_m_pesa_logo"),
        ProviderOption(name: "Mixx By Yas", displayName: "Mixx By Yas", imageName: "a_mixx_by_yas"),
        ProviderOption(name: "Halotel", displayName: "Halopesa", imageName: "a_halopesa_1"),
        ProviderOption(name: "Airtel", displayName: "Airtel Money", imageName: "a_airtel_money"),
        ProviderOption(name: "Tigo", displayName: "T-Pesa", imageName: "a_ttcl_pesa")
    ]
    private let cardProviders: [ProviderOption] = [
        ProviderOption(name: "CRDB", displayName: "CRDB", imageName: nil),
        ProviderOption(name: "NMB", displayName: "NMB", imageName: nil),
        ProviderOption(name: "TCB", displayName: "TCB", imageName: nil),
        ProviderOption(name: "AKIBA", displayName: "AKIBA", imageName: nil)
    ]

    init(doctor: DoctorSummary, amount: Double, initialMethod: PaymentMethod, onPaymentConfirmed: @escaping () -> Void) {
        self.doctor = doctor
        self.amount = amount
        self.initialMethod = initialMethod
        self.onPaymentConfirmed = onPaymentConfirmed
        _selectedMethod = State(initialValue: initialMethod)
        _consultationId = State(initialValue: "consult-\(UUID().uuidString.lowercased())")
        // Keep margin below Snippe's 30-character processor limit.
        _idempotencyKey = State(initialValue: String(UUID().uuidString.replacingOccurrences(of: "-", with: "").prefix(24)).lowercased())
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    CardContainer {
                        HStack(spacing: 14) {
                            ZStack(alignment: .bottomTrailing) {
                                ProfileAvatarView(
                                    imageSource: doctor.profileImage ?? "",
                                    initials: doctor.name.split(separator: " ").prefix(2).compactMap(\.first).map(String.init).joined().uppercased(),
                                    size: 56,
                                    fontSize: 18
                                )
                                if doctor.verified {
                                    Image(systemName: "checkmark.seal.fill")
                                        .font(.system(size: 15))
                                        .foregroundStyle(HASETTheme.greenPrimary)
                                        .padding(2)
                                        .background(Circle().fill(Color.white))
                                }
                            }

                            VStack(alignment: .leading, spacing: 4) {
                                Text(doctor.name)
                                    .font(HASETTheme.font(.medium, 16))
                                Text(doctor.specialty)
                                    .font(HASETTheme.font(.regular, 13))
                                    .foregroundStyle(HASETTheme.textSecondary)
                            }
                            Spacer()
                        }
                    }

                    CardContainer {
                        VStack(alignment: .leading, spacing: 14) {
                            HStack {
                                Text(appViewModel.tr("payment"))
                                    .font(HASETTheme.font(.medium, 18))
                                Spacer()
                                Text(formattedAmount(amount))
                                    .font(HASETTheme.font(.medium, 18))
                                    .foregroundStyle(HASETTheme.greenPrimary)
                            }

                            VStack(alignment: .leading, spacing: 10) {
                                HStack {
                                    Text(appViewModel.tr("consultation_fee"))
                                        .font(HASETTheme.font(.regular, 15))
                                        .foregroundStyle(HASETTheme.textSecondary)
                                    Spacer()
                                    Text(formattedAmount(amount))
                                        .font(HASETTheme.font(.medium, 18))
                                        .foregroundStyle(HASETTheme.greenPrimary)
                                }
                            }

                            Divider()
                                .overlay(HASETTheme.divider)

                            VStack(alignment: .leading, spacing: 10) {
                                Text(appViewModel.tr("payment_method_label"))
                                    .font(HASETTheme.font(.medium, 13))

                                HStack(spacing: 10) {
                                    paymentMethodCard(
                                        title: appViewModel.tr("mobile_money"),
                                        systemImage: "iphone.gen3.radiowaves.left.and.right",
                                        isSelected: selectedMethod == .mobileMoney
                                    ) {
                                        selectedMethod = .mobileMoney
                                        selectedProvider = mobileProviders.first?.name ?? "Vodacom"
                                        walletNumber = ""
                                    }
                                    .disabled(isProcessing)

                                    paymentMethodCard(
                                        title: appViewModel.tr("card_payment"),
                                        systemImage: "creditcard.fill",
                                        isSelected: selectedMethod == .cardPayment
                                    ) {
                                        selectedMethod = .cardPayment
                                        selectedProvider = cardProviders.first?.name ?? "CRDB"
                                        walletNumber = ""
                                    }
                                    .disabled(isProcessing)
                                }
                            }

                            VStack(alignment: .leading, spacing: 10) {
                                Text(appViewModel.tr("payment_provider"))
                                    .font(HASETTheme.font(.medium, 13))
                                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                                    ForEach(activeProviders) { provider in
                                        PaymentProviderCard(
                                            provider: provider,
                                            isSelected: selectedProvider == provider.name
                                        ) {
                                            selectedProvider = provider.name
                                        }
                                        .disabled(isProcessing)
                                    }
                                }
                            }

                            VStack(alignment: .leading, spacing: 8) {
                                Text(selectedMethod == .mobileMoney ? appViewModel.tr("payment_number") : appViewModel.tr("hosted_card_checkout"))
                                    .font(HASETTheme.font(.medium, 13))
                                if selectedMethod == .mobileMoney {
                                    HStack(spacing: 10) {
                                        Text("+255")
                                            .font(HASETTheme.font(.medium, 14))
                                            .foregroundStyle(HASETTheme.textPrimary)
                                            .padding(.leading, 16)

                                        Rectangle()
                                            .fill(HASETTheme.divider)
                                            .frame(width: 1, height: 24)

                                        TextField("683859574", text: $walletNumber)
                                            .keyboardType(.numberPad)
                                            .textInputAutocapitalization(.never)
                                            .autocorrectionDisabled()
                                            .font(HASETTheme.font(.regular, 14))
                                            .onChange(of: walletNumber) { newValue in
                                                walletNumber = sanitizedWalletSuffix(newValue)
                                            }
                                            .padding(.trailing, 16)
                                            .disabled(isProcessing)
                                    }
                                    .frame(height: 56)
                                    .background(
                                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                                            .fill(HASETTheme.backgroundPrimary)
                                    )
                                } else {
                                    Text(appViewModel.tr("card_checkout_description"))
                                        .font(HASETTheme.font(.regular, 13))
                                        .foregroundStyle(HASETTheme.textSecondary)
                                        .padding(.horizontal, 16)
                                        .frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
                                        .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(HASETTheme.backgroundPrimary))
                                }
                            }

                            if !statusMessage.isEmpty {
                                if isProcessing {
                                    paymentProcessingCard
                                } else {
                                    Text(statusMessage)
                                        .font(HASETTheme.font(.regular, 13))
                                        .foregroundStyle(canRetryStatus ? .orange : HASETTheme.textSecondary)
                                }
                            }

                            if isProcessing {
                                Button(appViewModel.tr("cancel_payment")) {
                                    showingCancelConfirmation = true
                                }
                                .font(HASETTheme.font(.medium, 14))
                                .foregroundStyle(HASETTheme.redPrimary)
                                .frame(maxWidth: .infinity)
                            } else if canRetryStatus, transactionId != nil {
                                Button(appViewModel.tr("check_status")) {
                                    Task { await checkStatus() }
                                }
                                .buttonStyle(PrimaryButtonStyle())
                            } else {
                                Button(appViewModel.tr("pay_now")) {
                                    Task { await startPayment() }
                                }
                                .buttonStyle(PrimaryButtonStyle())
                                .disabled(isProcessing || (selectedMethod == .mobileMoney && normalizedWalletNumber.isEmpty))
                            }

                        }
                    }
                }
                .padding(20)
            }
            .background(HASETTheme.backgroundPrimary)
            .navigationTitle(appViewModel.tr("payment"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(appViewModel.tr("close")) {
                        if isProcessing {
                            showingCancelConfirmation = true
                        } else {
                            dismiss()
                        }
                    }
                }
            }
            .interactiveDismissDisabled(isProcessing)
            .confirmationDialog(
                appViewModel.tr("cancel_payment"),
                isPresented: $showingCancelConfirmation,
                titleVisibility: .visible
            ) {
                Button(appViewModel.tr("keep_waiting"), role: .cancel) {}
                Button(appViewModel.tr("cancel_payment"), role: .destructive) {
                    terminatePaymentFlow()
                }
            } message: {
                Text(appViewModel.tr("cancel_payment_warning"))
            }
            .task {
                await preparePaymentSession()
            }
            .sheet(item: $hostedCheckout, onDismiss: {
                guard transactionId != nil else { return }
                Task { await checkStatus() }
            }) { destination in
                NavigationStack {
                    HostedCheckoutWebView(url: destination.url) {
                        hostedCheckout = nil
                        Task { await checkStatus() }
                    }
                    .ignoresSafeArea(edges: .bottom)
                    .navigationTitle(appViewModel.tr("secure_payment"))
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button(appViewModel.tr("close")) { hostedCheckout = nil }
                        }
                    }
                }
            }
        }
    }

    private func preparePaymentSession() async {
        await MainActor.run {
            statusMessage = appViewModel.tr("preparing_payment")
            canRetryStatus = false
        }
        // Anonymous payment sessions are short-lived. Reuse the signed-in
        // session for normal users, and mint an anonymous one for guest flows.
        if paymentAuthSession != nil {
            await MainActor.run {
                paymentSessionReady = true
                statusMessage = ""
            }
            return
        }
        do {
            let anonymousSession = try await paymentService.signInAnonymously()
            paymentAuthSession = anonymousSession
            await MainActor.run {
                paymentSessionReady = true
                statusMessage = ""
            }
        } catch {
            await MainActor.run {
                paymentSessionReady = false
                statusMessage = error.localizedDescription
            }
        }
    }

    private var activeProviders: [ProviderOption] {
        selectedMethod == .mobileMoney ? mobileProviders : cardProviders
    }

    private func paymentMethodCard(title: String, systemImage: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: systemImage)
                    .font(.system(size: 16, weight: .semibold))
                Text(title)
                    .font(HASETTheme.font(.medium, 13))
                    .lineLimit(2)
                Spacer()
            }
            .foregroundStyle(isSelected ? .white : HASETTheme.textPrimary)
            .padding(.horizontal, 14)
            .frame(height: 52)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(isSelected ? HASETTheme.greenPrimary : HASETTheme.backgroundPrimary)
            )
        }
        .buttonStyle(.plain)
    }

    private var paymentProcessingCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                HStack(spacing: 8) {
                    ForEach(0 ..< 3, id: \.self) { index in
                        Circle()
                            .fill(HASETTheme.greenPrimary.opacity(processingPulse ? 1 : 0.35))
                            .frame(width: 10, height: 10)
                            .scaleEffect(processingPulse ? 1.0 : 0.72)
                            .animation(
                                .easeInOut(duration: 0.7)
                                    .repeatForever()
                                    .delay(Double(index) * 0.16),
                                value: processingPulse
                            )
                    }
                }

                VStack(alignment: .leading, spacing: 3) {
                    Text(appViewModel.tr("payment_in_progress"))
                        .font(HASETTheme.font(.medium, 15))
                        .foregroundStyle(HASETTheme.textPrimary)
                    Text(statusMessage.isEmpty ? appViewModel.tr("confirm_prompt_phone") : statusMessage)
                        .font(HASETTheme.font(.regular, 13))
                        .foregroundStyle(HASETTheme.textSecondary)
                }

                Spacer()
            }

            HStack {
                Label(selectedProvider, systemImage: "iphone.gen3.radiowaves.left.and.right")
                Spacer()
                Text(normalizedWalletNumber)
            }
            .font(HASETTheme.font(.regular, 12))
            .foregroundStyle(HASETTheme.textSecondary)
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(HASETTheme.backgroundPrimary)
        )
        .onAppear {
            processingPulse = true
        }
    }

    private func formattedAmount(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 0
        formatter.groupingSeparator = ","
        let number = formatter.string(from: NSNumber(value: value)) ?? String(Int(value))
        return "\(number) TZS"
    }

    private var normalizedWalletNumber: String {
        let digits = walletNumber.filter(\.isNumber)
        if digits.isEmpty {
            return ""
        }
        if digits.hasPrefix("255") {
            return "+" + digits
        }
        if digits.hasPrefix("0"), digits.count >= 10 {
            return "+255" + digits.dropFirst()
        }
        if digits.count == 9 {
            return "+255" + digits
        }
        return digits
    }

    private func sanitizedWalletSuffix(_ value: String) -> String {
        var digits = value.filter(\.isNumber)
        if digits.hasPrefix("255") {
            digits.removeFirst(min(3, digits.count))
        } else if digits.hasPrefix("0"), digits.count >= 10 {
            digits.removeFirst()
        }
        return String(digits.prefix(9))
    }

    private var backendPaymentAccount: String {
        let digits = walletNumber.filter(\.isNumber)
        if digits.hasPrefix("255"), digits.count == 12 { return "0" + String(digits.dropFirst(3)) }
        if digits.hasPrefix("0"), digits.count == 10 { return digits }
        if digits.count == 9 { return "0" + digits }
        return normalizedWalletNumber
    }

    private func startPayment() async {
        guard paymentSessionReady else {
            await MainActor.run {
                statusMessage = appViewModel.tr("payment_session_unavailable")
                canRetryStatus = true
            }
            await preparePaymentSession()
            return
        }
        let user = appViewModel.currentUser ?? UserProfile(
            userId: sessionStore.loadSession()?.userId ?? "",
            email: "",
            fullName: doctor.name,
            phone: doctor.phoneNumber ?? "",
            role: .patient,
            profileImage: "",
            createdAt: Date().timeIntervalSince1970 * 1000,
            regNo: nil,
            gender: nil,
            age: nil,
            location: nil,
            bio: nil,
            specialization: doctor.specialty,
            consultationFee: nil,
            availableTimes: nil,
            verified: false
        )
        guard selectedMethod == .cardPayment || !normalizedWalletNumber.isEmpty else { return }

        isProcessing = true
        canRetryStatus = false
        statusMessage = appViewModel.tr("waiting_payment_confirmation")

        do {
            let idToken = try await refreshedPaymentToken()
            let response = try await paymentService.initiatePayment(
                user: user,
                doctor: doctor,
                consultationId: consultationId,
                idempotencyKey: idempotencyKey,
                amount: amount,
                paymentMethod: selectedMethod == .cardPayment ? "card" : "mobile_money",
                provider: selectedProvider,
                paymentAccount: selectedMethod == .mobileMoney ? backendPaymentAccount : "",
                idToken: idToken
            )
            guard let responseTransactionId = response.transactionId else {
                isProcessing = false
                canRetryStatus = true
                statusMessage = response.message ?? "Payment response did not include a transaction ID. Please retry."
                return
            }
            transactionId = responseTransactionId
            statusMessage = appViewModel.tr("payment_initiated")
            if response.isSuccess {
                if selectedMethod == .cardPayment {
                    guard let paymentUrl = response.paymentUrl,
                          let url = URL(string: paymentUrl),
                          url.scheme == "https" else {
                        isProcessing = false
                        canRetryStatus = true
                        statusMessage = "The card checkout link was not returned. Please retry or use mobile money."
                        return
                    }
                    await MainActor.run {
                        hostedCheckout = HostedCheckoutDestination(url: url)
                        statusMessage = appViewModel.tr("card_checkout_opened")
                    }
                } else {
                    await MainActor.run {
                        statusMessage = appViewModel.tr("check_phone_complete_payment")
                    }
                }
                startPolling()
            } else {
                isProcessing = false
                canRetryStatus = true
                statusMessage = response.message ?? "Payment initiation failed"
            }
        } catch {
            if !didRetryPaymentAuth,
               case ServiceError.message(let message) = error,
               message.lowercased().contains("unauthorized") {
                didRetryPaymentAuth = true
                paymentAuthSession = nil
                if appViewModel.currentUser == nil { sessionStore.clearSession() }
                paymentSessionReady = false
                await preparePaymentSession()
                if paymentSessionReady {
                    await startPayment()
                    return
                }
            }
            isProcessing = false
            canRetryStatus = true
            statusMessage = error.localizedDescription
        }
    }

    private func startPolling() {
        pollTask?.cancel()
        pollTask = Task {
            // Match Android's six-minute payment confirmation window (60 × 6s).
            // Mobile-money prompts can take longer than a single minute, and the
            // backend remains the source of truth for the transaction state.
            for _ in 0 ..< 60 {
                try? await Task.sleep(nanoseconds: 6_000_000_000)
                if Task.isCancelled { return }
                let success = await checkStatus(silentUntilFailure: true)
                if success { return }
            }
            await MainActor.run {
                isProcessing = false
                canRetryStatus = true
                processingPulse = false
                if statusMessage.isEmpty {
                    statusMessage = appViewModel.tr("payment_initiated")
                }
            }
        }
    }

    @discardableResult
    private func checkStatus(silentUntilFailure: Bool = false) async -> Bool {
        guard let transactionId else { return false }
        do {
            let idToken = try await refreshedPaymentToken()
            let response = try await paymentService.checkPaymentStatus(transactionId: transactionId, idToken: idToken)
            if let transaction = response.transaction {
                if transaction.isSuccess {
                    await MainActor.run {
                        isProcessing = false
                        canRetryStatus = false
                        processingPulse = false
                        statusMessage = appViewModel.tr("payment_confirmed")
                        hostedCheckout = nil
                        onPaymentConfirmed()
                        dismiss()
                    }
                    return true
                }
                if transaction.isFailed {
                    await MainActor.run {
                        isProcessing = false
                        canRetryStatus = true
                        processingPulse = false
                        statusMessage = response.message ?? "Payment was unsuccessful or cancelled."
                    }
                    return false
                }
                if !silentUntilFailure {
                    await MainActor.run {
                        statusMessage = appViewModel.tr("still_waiting_payment_confirmation")
                    }
                }
            }
        } catch {
            await MainActor.run {
                isProcessing = false
                canRetryStatus = true
                processingPulse = false
                statusMessage = error.localizedDescription
            }
        }
        return false
    }

    private func terminatePaymentFlow() {
        pollTask?.cancel()
        processingPulse = false
        isProcessing = false
        canRetryStatus = transactionId != nil
        statusMessage = appViewModel.tr("payment_terminated")
        if let transactionId {
            Task {
                let token = try? await refreshedPaymentToken()
                try? await paymentService.cancelPayment(transactionId: transactionId, idToken: token)
            }
        }
        dismiss()
    }

    private func refreshedPaymentToken() async throws -> String? {
        guard let session = paymentAuthSession ?? sessionStore.loadSession() else { return nil }
        let refreshedSession = try await paymentService.refreshSessionIfNeeded(session)
        if paymentAuthSession != nil {
            paymentAuthSession = refreshedSession
        } else if refreshedSession.idToken != session.idToken || refreshedSession.refreshToken != session.refreshToken {
            sessionStore.saveSession(refreshedSession)
        }
        return refreshedSession.idToken
    }
}

private struct ProviderOption: Identifiable {
    let id = UUID()
    let name: String
    let displayName: String
    let imageName: String?
}

private struct PaymentProviderCard: View {
    let provider: ProviderOption
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                ProviderBadgeView(provider: provider)
                Text(provider.displayName)
                    .font(HASETTheme.font(.medium, 13))
                    .lineLimit(2)
                Spacer(minLength: 0)
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(.white)
                }
            }
            .foregroundStyle(isSelected ? .white : HASETTheme.textPrimary)
            .padding(.horizontal, 14)
            .frame(height: 50)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(isSelected ? HASETTheme.greenPrimary : HASETTheme.backgroundPrimary)
            )
        }
        .buttonStyle(.plain)
    }
}

private struct ProviderBadgeView: View {
    let provider: ProviderOption

    var body: some View {
        Group {
            if let imageName = provider.imageName, let uiImage = UIImage(named: imageName) {
                Image(uiImage: uiImage)
                    .resizable()
                    .scaledToFit()
            } else {
                Image(systemName: "creditcard.fill")
                    .resizable()
                    .scaledToFit()
                    .foregroundStyle(HASETTheme.greenPrimary)
                    .padding(6)
            }
        }
        .frame(width: 34, height: 24)
    }
}

private struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

private struct DoctorPatientItem: Identifiable {
    let id: String
    let name: String
    let appointments: [AppointmentSummary]
}

struct DoctorPatientsView: View {
    @EnvironmentObject private var appViewModel: AppViewModel

    private var patients: [DoctorPatientItem] {
        let appointmentsWithPatients = appViewModel.appointments.filter { $0.patientId?.isEmpty == false }
        return Dictionary(grouping: appointmentsWithPatients, by: { $0.patientId! })
            .map { patientId, appointments in
                DoctorPatientItem(
                    id: patientId,
                    name: appointments.first?.title ?? appViewModel.tr("patient"),
                    appointments: appointments
                )
            }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                if patients.isEmpty {
                    CardContainer {
                        Text(appViewModel.tr("no_appointments_message"))
                            .font(HASETTheme.font(.medium, 15))
                            .foregroundStyle(HASETTheme.textSecondary)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, 12)
                    }
                } else {
                    ForEach(patients) { patient in
                        CardContainer {
                            HStack(spacing: 14) {
                                Image(systemName: "person.crop.circle.fill")
                                    .font(.system(size: 42))
                                    .foregroundStyle(HASETTheme.greenPrimary)
                                VStack(alignment: .leading, spacing: 5) {
                                    Text(patient.name)
                                        .font(HASETTheme.font(.medium, 16))
                                        .foregroundStyle(HASETTheme.textPrimary)
                                    Text("\(patient.appointments.count) \(appViewModel.tr("appointments"))")
                                        .font(HASETTheme.font(.regular, 13))
                                        .foregroundStyle(HASETTheme.textSecondary)
                                }
                                Spacer()
                            }
                        }
                    }
                }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("patients"))
        .task {
            await appViewModel.loadAppointments(force: false)
        }
        .refreshable {
            await appViewModel.loadAppointments(force: true)
        }
    }
}

struct AppointmentsOverviewView: View {
    let role: UserRole
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var selectedStatus: AppointmentSummary.Status?
    @State private var pendingConfirmation: AppointmentConfirmation?
    @State private var appointmentToReschedule: AppointmentSummary?
    @State private var actionInProgressId: String?
    @State private var selectedConversation: ConversationSummary?

    init(role: UserRole, initialStatus: AppointmentSummary.Status? = nil) {
        self.role = role
        _selectedStatus = State(initialValue: initialStatus)
    }

    private var appointments: [AppointmentSummary] {
        appViewModel.appointments
    }

    private var visibleAppointments: [AppointmentSummary] {
        guard let selectedStatus else { return appointments }
        return appointments.filter { $0.status == selectedStatus }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Picker(appViewModel.tr("status"), selection: Binding(
                    get: { selectedStatus ?? .approved },
                    set: { selectedStatus = $0 }
                )) {
                    ForEach(role == .doctor ? AppointmentSummary.Status.allCases : [.approved, .pending, .cancelled], id: \.id) { status in
                        Text(status.localizedLabel(languageCode: appViewModel.selectedLanguage)).tag(status)
                    }
                }
                .pickerStyle(.segmented)
                .onAppear {
                    if selectedStatus == nil {
                        selectedStatus = role == .doctor ? .pending : .approved
                    }
                }

                if visibleAppointments.isEmpty {
                    CardContainer {
                        Text(appViewModel.tr("no_appointments_message"))
                            .font(HASETTheme.font(.medium, 15))
                            .foregroundStyle(HASETTheme.textSecondary)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, 12)
                    }
                } else {
                    ForEach(visibleAppointments) { appointment in
                        CardContainer {
                            VStack(alignment: .leading, spacing: 10) {
                                HStack {
                                    Text(appointment.title)
                                        .font(HASETTheme.font(.medium, 16))
                                    Spacer()
                                    Text(appointment.status.localizedLabel(languageCode: appViewModel.selectedLanguage))
                                        .font(HASETTheme.font(.medium, 12))
                                        .foregroundStyle(statusColor(appointment.status))
                                }
                                Text(appointment.subtitle)
                                    .font(HASETTheme.font(.regular, 14))
                                    .foregroundStyle(HASETTheme.textSecondary)
                                Text(appointment.dateText)
                                    .font(HASETTheme.font(.regular, 13))
                                    .foregroundStyle(HASETTheme.greenPrimary)

                                appointmentActions(for: appointment)

                                if let chatConversation = chatConversation(for: appointment) {
                                    NavigationLink {
                                        ChatThreadView(conversation: chatConversation, role: role)
                                    } label: {
                                        Text(chatButtonTitle(for: appointment))
                                            .font(HASETTheme.font(.medium, 13))
                                            .foregroundStyle(.white)
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 10)
                                            .background(
                                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                                    .fill(HASETTheme.greenPrimary)
                                            )
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }
                }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(role == .patient ? appViewModel.tr("my_appointments") : appViewModel.tr("appointments"))
        .task {
            await appViewModel.loadAppointments(force: false)
        }
        .refreshable {
            await appViewModel.loadAppointments(force: true)
        }
        .navigationDestination(
            isPresented: Binding(
                get: { selectedConversation != nil },
                set: { if !$0 { selectedConversation = nil } }
            )
        ) {
            if let conversation = selectedConversation {
                ChatThreadView(conversation: conversation, role: role)
            }
        }
        .alert(item: $pendingConfirmation) { confirmation in
            let primaryButton: Alert.Button = confirmation.action == .approve
                ? .default(Text(actionLabel(confirmation.action))) {
                    perform(confirmation.action, on: confirmation.appointment)
                }
                : .destructive(Text(actionLabel(confirmation.action))) {
                    perform(confirmation.action, on: confirmation.appointment)
                }
            return Alert(
                title: Text(actionLabel(confirmation.action)),
                message: Text(appViewModel.tr("confirm_appointment_action")),
                primaryButton: primaryButton,
                secondaryButton: .cancel(Text(appViewModel.tr("cancel")))
            )
        }
        .sheet(item: $appointmentToReschedule) { appointment in
            AppointmentRescheduleSheet(appointment: appointment) { date, time in
                await appViewModel.rescheduleAppointment(
                    appointmentId: appointment.id,
                    date: date,
                    time: time
                )
            }
            .environmentObject(appViewModel)
        }
    }

    @ViewBuilder
    private func appointmentActions(for appointment: AppointmentSummary) -> some View {
        if role == .doctor && appointment.status == .pending {
            HStack(spacing: 10) {
                appointmentActionButton(
                    title: appViewModel.tr("decline"),
                    color: HASETTheme.redPrimary,
                    appointment: appointment
                ) {
                    pendingConfirmation = AppointmentConfirmation(appointment: appointment, action: .decline)
                }
                appointmentActionButton(
                    title: appViewModel.tr("approve"),
                    color: HASETTheme.greenPrimary,
                    appointment: appointment
                ) {
                    pendingConfirmation = AppointmentConfirmation(appointment: appointment, action: .approve)
                }
            }
        } else if role == .patient && (appointment.status == .pending || appointment.status == .approved) {
            HStack(spacing: 10) {
                appointmentActionButton(
                    title: appViewModel.tr("cancel"),
                    color: HASETTheme.redPrimary,
                    appointment: appointment
                ) {
                    pendingConfirmation = AppointmentConfirmation(appointment: appointment, action: .cancel)
                }
                appointmentActionButton(
                    title: appViewModel.tr("reschedule"),
                    color: HASETTheme.greenPrimary,
                    appointment: appointment
                ) {
                    appointmentToReschedule = appointment
                }
            }
        }
    }

    private func appointmentActionButton(
        title: String,
        color: Color,
        appointment: AppointmentSummary,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Group {
                if actionInProgressId == appointment.id {
                    ProgressView().tint(.white)
                } else {
                    Text(title).font(HASETTheme.font(.medium, 13))
                }
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(color))
        }
        .buttonStyle(.plain)
        .disabled(actionInProgressId != nil)
    }

    private func perform(_ action: AppointmentAction, on appointment: AppointmentSummary) {
        actionInProgressId = appointment.id
        Task {
            let succeeded = await appViewModel.updateAppointmentStatus(
                appointmentId: appointment.id,
                status: action.firebaseStatus
            )
            actionInProgressId = nil
            if succeeded,
               action == .approve,
               appointment.appointmentType?.lowercased() == "online chat" {
                selectedConversation = conversation(for: appointment)
            }
        }
    }

    private func actionLabel(_ action: AppointmentAction) -> String {
        appViewModel.tr(action.localizationKey)
    }

    private func statusColor(_ status: AppointmentSummary.Status) -> Color {
        switch status {
        case .approved, .completed:
            return HASETTheme.greenPrimary
        case .pending:
            return .orange
        case .cancelled:
            return HASETTheme.redPrimary
        }
    }

    private func chatConversation(for appointment: AppointmentSummary) -> ConversationSummary? {
        guard appointment.appointmentType?.lowercased() == "online chat" else { return nil }
        guard appointment.status == .approved else { return nil }
        return conversation(for: appointment)
    }

    private func conversation(for appointment: AppointmentSummary) -> ConversationSummary? {
        guard let currentUser = appViewModel.currentUser else { return nil }

        let otherUserId: String
        let otherUserName: String
        if role == .doctor {
            otherUserId = appointment.patientId ?? ""
            otherUserName = appointment.title
        } else {
            otherUserId = appointment.doctorId ?? ""
            otherUserName = appointment.title
        }

        guard !otherUserId.isEmpty else { return nil }

        return ConversationSummary(
            id: sortedChatRoomId(currentUser.userId, otherUserId),
            name: otherUserName,
            lastMessage: "Chat appointment",
            lastMessageTimestamp: appointment.createdAt ?? Date().timeIntervalSince1970 * 1000,
            unreadCount: 0,
            isOnline: false,
            archived: false,
            profileImage: nil
        )
    }

    private func chatButtonTitle(for appointment: AppointmentSummary) -> String {
        if role == .doctor && appointment.status == .approved {
            return appViewModel.tr("start_chat")
        }
        return appViewModel.tr("open_chat")
    }

    private func sortedChatRoomId(_ userId1: String, _ userId2: String) -> String {
        userId1 < userId2 ? "\(userId1)_\(userId2)" : "\(userId2)_\(userId1)"
    }
}

private enum AppointmentAction: Equatable {
    case approve
    case decline
    case cancel

    var firebaseStatus: String {
        switch self {
        case .approve: return "approved"
        case .decline: return "declined"
        case .cancel: return "cancelled"
        }
    }

    var localizationKey: String {
        switch self {
        case .approve: return "approve"
        case .decline: return "decline"
        case .cancel: return "cancel_appointment"
        }
    }
}

private struct AppointmentConfirmation: Identifiable {
    let appointment: AppointmentSummary
    let action: AppointmentAction
    var id: String { "\(appointment.id)-\(action.firebaseStatus)" }
}

private struct AppointmentRescheduleSheet: View {
    let appointment: AppointmentSummary
    let onSave: (String, String) async -> Bool

    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var selectedDate: Date
    @State private var selectedTime: Date
    @State private var isSaving = false

    init(appointment: AppointmentSummary, onSave: @escaping (String, String) async -> Bool) {
        self.appointment = appointment
        self.onSave = onSave
        _selectedDate = State(initialValue: Self.parseDate(appointment.date) ?? Date())
        _selectedTime = State(initialValue: Self.parseTime(appointment.time) ?? Date())
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    DatePicker(
                        appViewModel.tr("select_date"),
                        selection: $selectedDate,
                        in: Calendar.current.startOfDay(for: Date())...,
                        displayedComponents: .date
                    )
                    DatePicker(
                        appViewModel.tr("select_time"),
                        selection: $selectedTime,
                        displayedComponents: .hourAndMinute
                    )
                }
            }
            .navigationTitle(appViewModel.tr("reschedule_appointment"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(appViewModel.tr("cancel")) { dismiss() }
                        .disabled(isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(appViewModel.tr("save")) {
                        save()
                    }
                    .disabled(isSaving)
                }
            }
            .overlay {
                if isSaving {
                    ProgressView()
                        .padding(18)
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14))
                }
            }
        }
        .presentationDetents([.medium])
        .interactiveDismissDisabled(isSaving)
    }

    private func save() {
        isSaving = true
        Task {
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "dd MMM yyyy"
            let timeFormatter = DateFormatter()
            timeFormatter.dateFormat = "HH:mm"
            let succeeded = await onSave(
                dateFormatter.string(from: selectedDate),
                timeFormatter.string(from: selectedTime)
            )
            isSaving = false
            if succeeded { dismiss() }
        }
    }

    private static func parseDate(_ value: String) -> Date? {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd MMM yyyy"
        return formatter.date(from: value)
    }

    private static func parseTime(_ value: String) -> Date? {
        for format in ["HH:mm", "h:mm a"] {
            let formatter = DateFormatter()
            formatter.dateFormat = format
            if let date = formatter.date(from: value) { return date }
        }
        return nil
    }
}

struct ChatListScreen: View {
    let role: UserRole
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var selectedFolder = 0

    private var filteredConversations: [ConversationSummary] {
        let items = appViewModel.conversations
        if selectedFolder == 0 {
            return items.filter { !$0.archived }
        }
        return items.filter(\.archived)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                Picker(appViewModel.tr("chat"), selection: $selectedFolder) {
                    Text(appViewModel.tr("inbox")).tag(0)
                    Text(appViewModel.tr("archived")).tag(1)
                }
                .pickerStyle(.segmented)

                if filteredConversations.isEmpty {
                    CardContainer {
                        Text(appViewModel.tr("no_messages_message"))
                            .font(HASETTheme.font(.medium, 15))
                            .foregroundStyle(HASETTheme.textSecondary)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, 12)
                    }
                } else {
                    ForEach(filteredConversations) { conversation in
                        NavigationLink {
                            ChatThreadView(conversation: conversation, role: role)
                        } label: {
                            CardContainer {
                                HStack {
                                    Circle()
                                        .fill(conversation.isOnline ? HASETTheme.greenPrimary.opacity(0.16) : HASETTheme.divider)
                                        .frame(width: 48, height: 48)
                                        .overlay(
                                            Text(String(conversation.name.prefix(1)))
                                                .font(HASETTheme.font(.medium, 18))
                                                .foregroundStyle(HASETTheme.greenPrimary)
                                        )

                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(conversation.name)
                                            .font(HASETTheme.font(.medium, 15))
                                            .foregroundStyle(HASETTheme.textPrimary)
                                        Text(conversation.lastMessage)
                                            .font(HASETTheme.font(.regular, 13))
                                            .foregroundStyle(HASETTheme.textSecondary)
                                            .lineLimit(2)
                                    }
                                    Spacer()
                                    if conversation.unreadCount > 0 {
                                        Text("\(conversation.unreadCount)")
                                            .font(HASETTheme.font(.medium, 12))
                                            .foregroundStyle(.white)
                                            .frame(width: 24, height: 24)
                                            .background(Circle().fill(HASETTheme.redPrimary))
                                    }
                                }
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("chat"))
        .task {
            await appViewModel.loadConversations(force: false)
        }
    }
}

struct ChatThreadView: View {
    let conversation: ConversationSummary
    let role: UserRole
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var messages: [ChatMessageSummary] = []
    @State private var draftMessage = ""
    @State private var isSending = false

    private var currentUserId: String { appViewModel.currentUser?.userId ?? "" }

    private var otherUserId: String {
        let parts = conversation.id.split(separator: "_").map(String.init)
        return parts.first(where: { $0 != currentUserId }) ?? conversation.id
    }

    private var chatRoomId: String {
        conversation.id
    }

    private var otherUserName: String {
        conversation.name
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(messages) { message in
                        HStack {
                            if message.isOutgoing { Spacer(minLength: 32) }
                            HStack(alignment: .bottom, spacing: 8) {
                                Text(message.message)
                                    .font(HASETTheme.font(.regular, 15))
                                    .foregroundStyle(message.isOutgoing ? .white : HASETTheme.textPrimary)
                                    .fixedSize(horizontal: false, vertical: true)

                                if message.isOutgoing {
                                    ZStack(alignment: .trailing) {
                                        Image(systemName: "checkmark")
                                            .font(.system(size: 10, weight: .semibold))
                                            .foregroundStyle(message.isRead ? Color.blue : Color.gray.opacity(0.75))
                                            .offset(x: -3, y: 0)
                                        Image(systemName: "checkmark")
                                            .font(.system(size: 10, weight: .semibold))
                                            .foregroundStyle(message.isRead ? Color.blue : Color.gray.opacity(0.75))
                                            .offset(x: 2, y: 0)
                                    }
                                    .frame(width: 12, height: 12)
                                    .padding(.leading, 4)
                                }
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(
                                RoundedRectangle(cornerRadius: 18, style: .continuous)
                                    .fill(message.isOutgoing ? HASETTheme.greenPrimary : Color.white)
                            )
                            .frame(maxWidth: 280, alignment: message.isOutgoing ? .trailing : .leading)
                            if !message.isOutgoing { Spacer(minLength: 32) }
                        }
                    }
                }
                .padding(20)
            }

            HStack(spacing: 10) {
                TextField("Type a message", text: $draftMessage, axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                    .lineLimit(1...4)
                Button {
                    sendMessage()
                } label: {
                    Image(systemName: "paperplane.fill")
                        .foregroundStyle(.white)
                        .frame(width: 42, height: 42)
                        .background(Circle().fill(HASETTheme.greenPrimary))
                }
                .disabled(draftMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSending)
            }
            .padding(16)
            .background(Color.white)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(conversation.name)
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await loadMessages()
            await appViewModel.markChatMessagesRead(chatRoomId: chatRoomId)
            await appViewModel.loadConversations(force: true)
        }
    }

    private func loadMessages() async {
        guard !currentUserId.isEmpty else { return }
        messages = await appViewModel.loadChatMessages(chatRoomId: chatRoomId, currentUserId: currentUserId)
    }

    private func sendMessage() {
        let text = draftMessage.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        isSending = true
        draftMessage = ""
        Task {
            defer { isSending = false }
            do {
                try await appViewModel.sendChatMessage(
                    chatRoomId: chatRoomId,
                    receiverId: otherUserId,
                    receiverName: otherUserName,
                    message: text
                )
                await loadMessages()
                await appViewModel.markChatMessagesRead(chatRoomId: chatRoomId)
                await appViewModel.loadConversations(force: true)
            } catch {
                draftMessage = text
            }
        }
    }
}

struct ProfileScreen: View {
    @EnvironmentObject private var appViewModel: AppViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Spacer()
                    NavigationLink {
                        SettingsView()
                    } label: {
                        Image(systemName: "gearshape.fill")
                            .foregroundStyle(HASETTheme.greenPrimary)
                            .frame(width: 40, height: 40)
                            .background(Circle().fill(Color.white))
                    }
                    .buttonStyle(.plain)
                }

                if let user = appViewModel.currentUser {
                    VStack(spacing: 10) {
                        ProfileAvatarView(
                            imageSource: user.profileImage,
                            initials: profileInitials(from: user.fullName),
                            size: 100,
                            fontSize: 30
                        )

                        Text(displayName(for: user, fallback: user.fullName))
                            .font(HASETTheme.font(.medium, 20))
                            .foregroundStyle(HASETTheme.textPrimary)

                        if let regNo = user.regNo, user.role == .doctor, !regNo.isEmpty {
                            Text(regNo)
                                .font(HASETTheme.font(.medium, 14))
                                .foregroundStyle(HASETTheme.greenPrimary)
                        }

                        Text(user.email)
                            .font(HASETTheme.font(.regular, 14))
                            .foregroundStyle(HASETTheme.textSecondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 8)

                    NavigationLink {
                        EditProfileView()
                    } label: {
                        Text(appViewModel.tr("edit_profile"))
                    }
                    .buttonStyle(PrimaryButtonStyle())
                }

                if let user = appViewModel.currentUser {
                    sectionHeader(appViewModel.tr("basic"))
                    CardContainer {
                        VStack(spacing: 0) {
                            NavigationLink { EditProfileView() } label: {
                                ProfileValueRow(icon: "phone", title: appViewModel.tr("phone"), value: user.phone.isEmpty ? appViewModel.tr("not_set") : user.phone)
                            }
                            .buttonStyle(.plain)
                            rowDivider()
                            NavigationLink { EditProfileView() } label: {
                                ProfileValueRow(icon: "calendar", title: appViewModel.tr("age"), value: user.age ?? appViewModel.tr("not_set"))
                            }
                            .buttonStyle(.plain)
                            rowDivider()
                            NavigationLink { EditProfileView() } label: {
                                ProfileValueRow(icon: "person", title: appViewModel.tr("gender"), value: user.gender ?? appViewModel.tr("not_set"))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                sectionHeader(appViewModel.tr("general"))
                CardContainer {
                    VStack(spacing: 0) {
                        NavigationLink { AboutUsView() } label: {
                            ProfileOptionRow(icon: "doc.text", title: appViewModel.tr("terms_of_use"), subtitle: appViewModel.tr("terms_of_use_desc"))
                        }
                        .buttonStyle(.plain)
                        rowDivider()
                        NavigationLink { AboutUsView() } label: {
                            ProfileOptionRow(icon: "info.circle", title: appViewModel.tr("about"), subtitle: appViewModel.tr("about_desc"))
                        }
                        .buttonStyle(.plain)
                        rowDivider()
                        Button {} label: {
                            ProfileOptionRow(icon: "hand.thumbsup", title: appViewModel.tr("rate_app"), subtitle: appViewModel.tr("rate_app_desc"))
                        }
                        .buttonStyle(.plain)
                        rowDivider()
                        Button {} label: {
                            ProfileOptionRow(icon: "phone", title: appViewModel.tr("contact_us"), subtitle: appViewModel.tr("contact_us_desc"))
                        }
                        .buttonStyle(.plain)
                    }
                }

                if let user = appViewModel.currentUser, user.role == .doctor {
                    sectionHeader(appViewModel.tr("professional_info"))
                    CardContainer {
                        VStack(spacing: 0) {
                            NavigationLink {
                                EditProfileView()
                            } label: {
                                ProfileValueRow(icon: "stethoscope", title: appViewModel.tr("specialization"), value: user.specialization ?? appViewModel.tr("not_set"))
                            }
                            .buttonStyle(.plain)
                            rowDivider()
                            NavigationLink {
                                EditProfileView()
                            } label: {
                                ProfileValueRow(icon: "coins", title: appViewModel.tr("consultation_fee"), value: user.consultationFee ?? appViewModel.tr("not_set"))
                            }
                            .buttonStyle(.plain)
                            rowDivider()
                            NavigationLink {
                                EditProfileView()
                            } label: {
                                ProfileValueRow(icon: "clock", title: appViewModel.tr("available_times"), value: user.availableTimes?.joined(separator: ", ") ?? appViewModel.tr("not_set"))
                            }
                            .buttonStyle(.plain)
                            rowDivider()
                            NavigationLink {
                                EditProfileView()
                            } label: {
                                ProfileValueRow(icon: "info.circle", title: appViewModel.tr("bio"), value: user.bio ?? appViewModel.tr("not_set"))
                            }
                            .buttonStyle(.plain)
                            rowDivider()
                            NavigationLink {
                                EditProfileView()
                            } label: {
                                ProfileValueRow(
                                    icon: "location",
                                    title: appViewModel.tr("location"),
                                    value: appViewModel.locationEnabled ? "Enabled" : "Disabled"
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    sectionHeader(appViewModel.tr("preferences"))
                    CardContainer {
                        VStack(spacing: 0) {
                            NavigationLink {
                                AboutUsView()
                            } label: {
                                ProfileOptionRow(icon: "doc.text", title: appViewModel.tr("privacy_policy"), subtitle: appViewModel.tr("privacy_policy"))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                CardContainer {
                    Button(role: .destructive) {
                        appViewModel.deleteAccount()
                    } label: {
                        ProfileOptionRow(icon: "trash", title: appViewModel.tr("delete_account"), subtitle: "Delete your account and data")
                    }
                    .buttonStyle(.plain)
                }

                Button {
                    appViewModel.logout()
                } label: {
                    ProfileOptionRow(icon: "rectangle.portrait.and.arrow.right", title: appViewModel.tr("logout"), subtitle: "Sign out of this device")
                }
                .buttonStyle(PrimaryButtonStyle())
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await appViewModel.refreshCurrentUser()
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(HASETTheme.font(.medium, 18))
            .foregroundStyle(HASETTheme.textPrimary)
    }

    private func rowDivider() -> some View {
        Divider()
            .overlay(HASETTheme.divider)
            .padding(.leading, 32)
    }

    private func profileInitials(from name: String) -> String {
        let parts = name.split(separator: " ").prefix(2).compactMap(\.first)
        return String(parts).uppercased()
    }

    private func displayName(for user: UserProfile?, fallback: String) -> String {
        guard let user else { return fallback }
        let name = user.fullName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return fallback }
        if user.role == .doctor {
            return name.lowercased().hasPrefix("dr.") || name.lowercased().hasPrefix("dr ") ? name : "Dr. \(name)"
        }
        return name
    }
}

private struct ProfileLine: View {
    let title: String
    let value: String

    var body: some View {
        HStack {
            Text(title)
                .font(HASETTheme.font(.medium, 14))
            Spacer()
            Text(value)
                .font(HASETTheme.font(.regular, 14))
                .foregroundStyle(HASETTheme.textSecondary)
        }
    }
}

private struct ProfileOptionRow: View {
    let icon: String
    let title: String
    let subtitle: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(HASETTheme.greenPrimary)
                .frame(width: 20)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(HASETTheme.font(.medium, 14))
                    .foregroundStyle(HASETTheme.textPrimary)
                Text(subtitle)
                    .font(HASETTheme.font(.regular, 12))
                    .foregroundStyle(HASETTheme.textSecondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(HASETTheme.textSecondary)
        }
        .padding(.vertical, 10)
    }
}

private struct ProfileValueRow: View {
    let icon: String
    let title: String
    let value: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(HASETTheme.greenPrimary)
                .frame(width: 20)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(HASETTheme.font(.medium, 14))
                    .foregroundStyle(HASETTheme.textPrimary)
                Text(value)
                    .font(HASETTheme.font(.regular, 12))
                    .foregroundStyle(HASETTheme.textSecondary)
                    .multilineTextAlignment(.leading)
            }
            Spacer()
        }
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct ProfileAvatarView: View {
    let imageSource: String
    let initials: String
    let size: CGFloat
    let fontSize: CGFloat

    var body: some View {
        Group {
            if isRemoteURL, let remoteURL {
                AsyncImage(url: remoteURL) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        fallbackAvatar
                    }
                }
            } else if let image = decodedImage {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                fallbackAvatar
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .background(
            Circle()
                .fill(HASETTheme.backgroundCard)
        )
    }

    private var decodedImage: UIImage? {
        guard !imageSource.isEmpty else { return nil }
        if let data = Data(base64Encoded: imageSource), let image = UIImage(data: data) {
            return image
        }
        return nil
    }

    private var isRemoteURL: Bool {
        imageSource.hasPrefix("http://") || imageSource.hasPrefix("https://")
    }

    private var remoteURL: URL? {
        URL(string: imageSource)
    }

    private var fallbackAvatar: some View {
        Circle()
            .fill(HASETTheme.greenPrimary.opacity(0.12))
            .overlay(
                Text(initials)
                    .font(HASETTheme.font(.medium, fontSize))
                    .foregroundStyle(HASETTheme.greenPrimary)
            )
    }
}

struct DoctorsCatalogView: View {
    private enum SortMode {
        case name
        case rating
        case experience
    }

    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var searchText = ""
    @State private var sortMode: SortMode = .rating
    @State private var selectedSpecialty: String?
    @State private var savedDoctorIDs: Set<String> = []
    @State private var selectedDoctor: DoctorSummary?
    @State private var showDoctorDetail = false

    private var visibleDoctors: [DoctorSummary] {
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let specialtyFiltered = appViewModel.doctors.filter { doctor in
            guard let selectedSpecialty, !selectedSpecialty.isEmpty else { return true }
            return doctor.specialty.caseInsensitiveCompare(selectedSpecialty) == .orderedSame
        }
        let searchFiltered = specialtyFiltered.filter { doctor in
            guard !query.isEmpty else { return true }
            return doctor.name.lowercased().contains(query) ||
                doctor.specialty.lowercased().contains(query) ||
                doctor.hospital.lowercased().contains(query)
        }

        switch sortMode {
        case .name:
            return searchFiltered.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
        case .rating:
            return searchFiltered.sorted { $0.rating > $1.rating }
        case .experience:
            return searchFiltered.sorted { ($0.experienceYears ?? 0) > ($1.experienceYears ?? 0) }
        }
    }

    private var availableSpecialties: [String] {
        Array(Set(appViewModel.doctors.map(\.specialty).filter { !$0.isEmpty })).sorted()
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                RoundedInputField(
                    title: appViewModel.tr("search_doctors"),
                    systemImage: "magnifyingglass",
                    text: $searchText
                )

                if visibleDoctors.isEmpty {
                    CardContainer {
                        Text(appViewModel.tr("no_doctors_found"))
                            .font(HASETTheme.font(.medium, 15))
                            .foregroundStyle(HASETTheme.textSecondary)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, 12)
                    }
                } else {
                    VStack(spacing: 12) {
                        ForEach(visibleDoctors) { doctor in
                            DoctorDirectoryCard(
                                doctor: doctor,
                                isSaved: savedDoctorIDs.contains(doctor.id),
                                onOpen: {
                                    selectedDoctor = doctor
                                    showDoctorDetail = true
                                },
                                onToggleSave: { toggleSavedDoctor(doctor.id) }
                            )
                        }
                    }
                }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden()
        .navigationDestination(isPresented: $showDoctorDetail) {
            doctorDetailDestination
        }
        .task {
            await appViewModel.loadDoctors(force: false)
        }
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(HASETTheme.textPrimary)
                }
                .accessibilityLabel("Back")
            }
            ToolbarItem(placement: .principal) {
                Text(appViewModel.tr("find_doctors"))
                    .font(HASETTheme.font(.medium, 18))
                    .foregroundStyle(HASETTheme.textPrimary)
            }
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button(appViewModel.tr("sort_by_name")) {
                        sortMode = .name
                    }
                    Button(appViewModel.tr("sort_by_rating")) {
                        sortMode = .rating
                    }
                    Button(appViewModel.tr("sort_by_experience")) {
                        sortMode = .experience
                    }
                    Menu(appViewModel.tr("filter_by_specialist")) {
                        Button(appViewModel.tr("all_specialists")) {
                            selectedSpecialty = nil
                        }
                        ForEach(availableSpecialties, id: \.self) { specialty in
                            Button(specialty) {
                                selectedSpecialty = specialty
                            }
                        }
                    }
                    Button(appViewModel.tr("refresh")) {
                        Task { await appViewModel.loadDoctors(force: true) }
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(HASETTheme.textPrimary)
                }
                .accessibilityLabel("Doctor list options")
            }
        }
    }

    private func toggleSavedDoctor(_ doctorId: String) {
        if savedDoctorIDs.contains(doctorId) {
            savedDoctorIDs.remove(doctorId)
        } else {
            savedDoctorIDs.insert(doctorId)
        }
    }

    @ViewBuilder
    private var doctorDetailDestination: some View {
        if let selectedDoctor {
            DoctorDetailView(doctor: selectedDoctor)
        } else {
            EmptyView()
        }
    }
}

struct HospitalListView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                CardContainer {
                    Text(appViewModel.tr("no_hospitals_found"))
                        .font(HASETTheme.font(.medium, 15))
                        .foregroundStyle(HASETTheme.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.vertical, 12)
                }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("hospitals"))
    }
}

struct ArticlesView: View {
    private enum ArticleTab: Int, CaseIterable {
        case articles
        case healthTips
        case saved
    }

    let role: UserRole
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var selectedTab: ArticleTab = .articles
    @State private var searchText = ""
    @State private var savedArticleIDs: Set<String> = []
    @State private var likedArticleIDs: Set<String> = []
    @State private var articleOverrides: [String: ArticleSummary] = [:]
    @State private var selectedArticle: ArticleSummary?
    @State private var showArticleDetail = false
    @State private var commentingArticle: ArticleSummary?
    @State private var commentDraft = ""
    @State private var articleComments: [ArticleComment] = []
    @State private var sharePayload: [Any] = []
    @State private var showShareSheet = false

    private let authService = AuthService()

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                RoundedInputField(
                    title: appViewModel.tr("search_articles"),
                    systemImage: "magnifyingglass",
                    text: $searchText
                )

                Picker("", selection: $selectedTab) {
                    Text(appViewModel.tr("articles")).tag(ArticleTab.articles)
                    Text(appViewModel.tr("health_tips")).tag(ArticleTab.healthTips)
                    Text(appViewModel.tr("saved")).tag(ArticleTab.saved)
                }
                .pickerStyle(.segmented)

                if selectedTab == .healthTips, !visibleHealthTips.isEmpty {
                    LazyVStack(spacing: 12) {
                        ForEach(visibleHealthTips) { tip in
                            HealthTipCard(tip: tip)
                        }
                    }
                } else if visibleArticles.isEmpty {
                    CardContainer {
                        Text(selectedTab == .healthTips ? appViewModel.tr("no_health_tips_found") : appViewModel.tr("no_articles_found"))
                            .font(HASETTheme.font(.medium, 15))
                            .foregroundStyle(HASETTheme.textSecondary)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, 12)
                    }
                } else {
                    VStack(spacing: 12) {
                        ForEach(visibleArticles) { article in
                            FeedArticleCard(
                                article: article,
                                isSaved: savedArticleIDs.contains(article.id),
                                isLiked: likedArticleIDs.contains(article.id),
                                onOpen: {
                                    Task { await trackArticleOpen(article) }
                                    selectedArticle = article
                                    showArticleDetail = true
                                },
                                onToggleLike: { Task { await toggleLike(article) } },
                                onComment: { Task { await openComments(for: article) } },
                                onShare: { Task { await shareArticle(article) } },
                                onToggleSave: { toggleSavedArticle(article.id) }
                            )
                        }
                    }
                }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("articles"))
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(isPresented: $showArticleDetail) {
            articleDetailDestination
        }
        .sheet(item: $commentingArticle) { article in
            NavigationStack {
                VStack(spacing: 0) {
                    ScrollView {
                        LazyVStack(spacing: 14) {
                            if articleComments.isEmpty {
                                CardContainer {
                                    Text(appViewModel.tr("no_comments_yet"))
                                        .font(HASETTheme.font(.medium, 15))
                                        .foregroundStyle(HASETTheme.textSecondary)
                                        .frame(maxWidth: .infinity, alignment: .center)
                                }
                            } else {
                                ForEach(articleComments) { comment in
                                    HStack(alignment: .top, spacing: 12) {
                                        ProfileAvatarView(
                                            imageSource: comment.userImage ?? "",
                                            initials: comment.userName.split(separator: " ").prefix(2).compactMap(\.first).map(String.init).joined().uppercased(),
                                            size: 38,
                                            fontSize: 13
                                        )

                                        VStack(alignment: .leading, spacing: 4) {
                                            HStack(spacing: 8) {
                                                Text(comment.userName)
                                                    .font(HASETTheme.font(.medium, 14))
                                                    .foregroundStyle(HASETTheme.textPrimary)
                                                Text(relativeCommentTime(comment.timestamp))
                                                    .font(HASETTheme.font(.regular, 12))
                                                    .foregroundStyle(HASETTheme.textSecondary)
                                            }

                                            Text(comment.text)
                                                .font(HASETTheme.font(.regular, 14))
                                                .foregroundStyle(HASETTheme.textPrimary)
                                                .fixedSize(horizontal: false, vertical: true)
                                        }

                                        Spacer()
                                    }
                                    .padding(14)
                                    .background(
                                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                                            .fill(HASETTheme.backgroundCard)
                                    )
                                }
                            }
                        }
                        .padding(20)
                    }

                    HStack(spacing: 12) {
                        TextField(appViewModel.tr("write_comment"), text: $commentDraft, axis: .vertical)
                            .font(HASETTheme.font(.regular, 14))
                            .padding(.horizontal, 16)
                            .padding(.vertical, 14)
                            .background(
                                RoundedRectangle(cornerRadius: 18, style: .continuous)
                                    .fill(HASETTheme.backgroundCard)
                            )

                        Button(appViewModel.tr("send")) {
                            Task { await submitComment(for: article) }
                        }
                        .font(HASETTheme.font(.medium, 14))
                        .foregroundStyle(Color.white)
                        .frame(width: 64, height: 52)
                        .background(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .fill(commentDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? HASETTheme.textSecondary.opacity(0.35) : HASETTheme.greenPrimary)
                        )
                        .disabled(commentDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                    .padding(20)
                    .background(HASETTheme.backgroundPrimary)
                }
                .background(HASETTheme.backgroundPrimary)
                .navigationTitle(appViewModel.tr("comments"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(appViewModel.tr("close")) {
                            commentingArticle = nil
                        }
                    }
                }
            }
        }
        .sheet(isPresented: $showShareSheet) {
            ShareSheet(items: sharePayload)
        }
        .onAppear {
            showArticleDetail = false
            selectedArticle = nil
            Task {
                await appViewModel.loadPatientHomeContent(force: true)
                await loadArticleInteractionState()
            }
        }
    }

    private var articleItems: [ArticleSummary] {
        appViewModel.patientPopularArticles.map { articleOverrides[$0.id] ?? $0 }
    }

    private var visibleArticles: [ArticleSummary] {
        let base: [ArticleSummary]
        switch selectedTab {
        case .articles:
            base = articleItems
        case .healthTips:
            base = []
        case .saved:
            base = articleItems.filter { savedArticleIDs.contains($0.id) }
        }

        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !query.isEmpty else { return base }
        return base.filter {
            $0.title.lowercased().contains(query) ||
                $0.author.lowercased().contains(query) ||
                $0.excerpt.lowercased().contains(query) ||
                $0.category.lowercased().contains(query)
        }
    }

    private var visibleHealthTips: [HealthTipSummary] {
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !query.isEmpty else { return appViewModel.patientHealthTips }
        return appViewModel.patientHealthTips.filter {
            $0.text.lowercased().contains(query) || $0.author.lowercased().contains(query)
        }
    }

    private func toggleSavedArticle(_ articleID: String) {
        Task {
            guard let session = sessionStore.loadSession() else { return }
            do {
                let refreshed = try await authService.refreshSessionIfNeeded(session)
                sessionStore.saveSession(refreshed)
                let isSaved = try await authService.toggleSavedArticle(postId: articleID, userId: refreshed.userId, idToken: refreshed.idToken)
                await MainActor.run {
                    if isSaved {
                        savedArticleIDs.insert(articleID)
                    } else {
                        savedArticleIDs.remove(articleID)
                    }
                }
            } catch {
                await MainActor.run {
                    appViewModel.alertState = AlertState(title: appViewModel.tr("error"), message: error.localizedDescription)
                }
            }
        }
    }

    private let sessionStore = SessionStore()

    private func loadArticleInteractionState() async {
        guard let session = sessionStore.loadSession() else { return }
        do {
            let refreshed = try await authService.refreshSessionIfNeeded(session)
            sessionStore.saveSession(refreshed)
            let savedIDs = try await authService.fetchSavedArticleIDs(userId: refreshed.userId, idToken: refreshed.idToken)
            var likedIDs = Set<String>()
            for article in appViewModel.patientPopularArticles {
                if try await authService.isArticleLiked(postId: article.id, userId: refreshed.userId, idToken: refreshed.idToken) {
                    likedIDs.insert(article.id)
                }
            }
            await MainActor.run {
                savedArticleIDs = savedIDs
                likedArticleIDs = likedIDs
            }
        } catch {
            return
        }
    }

    private func toggleLike(_ article: ArticleSummary) async {
        guard let session = sessionStore.loadSession() else { return }
        do {
            let refreshed = try await authService.refreshSessionIfNeeded(session)
            sessionStore.saveSession(refreshed)
            let isLiked = try await authService.toggleArticleLike(postId: article.id, userId: refreshed.userId, idToken: refreshed.idToken)
            await MainActor.run {
                if isLiked {
                    likedArticleIDs.insert(article.id)
                } else {
                    likedArticleIDs.remove(article.id)
                }

                let delta = isLiked ? 1 : -1
                articleOverrides[article.id] = ArticleSummary(
                    id: article.id,
                    title: article.title,
                    author: article.author,
                    authorImage: article.authorImage,
                    category: article.category,
                    excerpt: article.excerpt,
                    imageName: article.imageName,
                    imageURL: article.imageURL,
                    timestamp: article.timestamp,
                    readTime: article.readTime,
                    content: article.content,
                    viewCount: article.viewCount,
                    likeCount: max(0, article.likeCount + delta),
                    commentCount: article.commentCount,
                    shareCount: article.shareCount,
                    type: article.type
                )
            }
        } catch {
            await MainActor.run {
                appViewModel.alertState = AlertState(title: appViewModel.tr("error"), message: error.localizedDescription)
            }
        }
    }

    private func openComments(for article: ArticleSummary) async {
        guard let session = sessionStore.loadSession() else { return }
        do {
            let refreshed = try await authService.refreshSessionIfNeeded(session)
            sessionStore.saveSession(refreshed)
            let comments = try await authService.fetchArticleComments(postId: article.id, idToken: refreshed.idToken)
            await MainActor.run {
                articleComments = comments
                commentingArticle = article
                articleOverrides[article.id] = ArticleSummary(
                    id: article.id,
                    title: article.title,
                    author: article.author,
                    authorImage: article.authorImage,
                    category: article.category,
                    excerpt: article.excerpt,
                    imageName: article.imageName,
                    imageURL: article.imageURL,
                    timestamp: article.timestamp,
                    readTime: article.readTime,
                    content: article.content,
                    viewCount: article.viewCount,
                    likeCount: article.likeCount,
                    commentCount: comments.count,
                    shareCount: article.shareCount,
                    type: article.type
                )
            }
        } catch {
            await MainActor.run {
                appViewModel.alertState = AlertState(title: appViewModel.tr("error"), message: error.localizedDescription)
            }
        }
    }

    private func submitComment(for article: ArticleSummary) async {
        guard let session = sessionStore.loadSession(), let user = appViewModel.currentUser else { return }
        let text = commentDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        do {
            let refreshed = try await authService.refreshSessionIfNeeded(session)
            sessionStore.saveSession(refreshed)
            let comment = try await authService.addArticleComment(postId: article.id, user: user, text: text, idToken: refreshed.idToken)
            await MainActor.run {
                articleComments.append(comment)
                commentDraft = ""
                articleOverrides[article.id] = ArticleSummary(
                    id: article.id,
                    title: article.title,
                    author: article.author,
                    authorImage: article.authorImage,
                    category: article.category,
                    excerpt: article.excerpt,
                    imageName: article.imageName,
                    imageURL: article.imageURL,
                    timestamp: article.timestamp,
                    readTime: article.readTime,
                    content: article.content,
                    viewCount: article.viewCount,
                    likeCount: article.likeCount,
                    commentCount: article.commentCount + 1,
                    shareCount: article.shareCount,
                    type: article.type
                )
            }
        } catch {
            await MainActor.run {
                appViewModel.alertState = AlertState(title: appViewModel.tr("error"), message: error.localizedDescription)
            }
        }
    }

    private func shareArticle(_ article: ArticleSummary) async {
        guard let session = sessionStore.loadSession() else { return }
        do {
            let refreshed = try await authService.refreshSessionIfNeeded(session)
            sessionStore.saveSession(refreshed)
            _ = try await authService.incrementArticleShares(postId: article.id, idToken: refreshed.idToken)
            let shareText = [article.title, article.excerpt].filter { !$0.isEmpty }.joined(separator: "\n\n")
            await MainActor.run {
                sharePayload = [shareText]
                showShareSheet = true
                articleOverrides[article.id] = ArticleSummary(
                    id: article.id,
                    title: article.title,
                    author: article.author,
                    authorImage: article.authorImage,
                    category: article.category,
                    excerpt: article.excerpt,
                    imageName: article.imageName,
                    imageURL: article.imageURL,
                    timestamp: article.timestamp,
                    readTime: article.readTime,
                    content: article.content,
                    viewCount: article.viewCount,
                    likeCount: article.likeCount,
                    commentCount: article.commentCount,
                    shareCount: article.shareCount + 1,
                    type: article.type
                )
            }
        } catch {
            await MainActor.run {
                appViewModel.alertState = AlertState(title: appViewModel.tr("error"), message: error.localizedDescription)
            }
        }
    }

    private func trackArticleOpen(_ article: ArticleSummary) async {
        guard let session = sessionStore.loadSession() else { return }
        do {
            let refreshed = try await authService.refreshSessionIfNeeded(session)
            sessionStore.saveSession(refreshed)
            _ = try await authService.incrementArticleViews(postId: article.id, idToken: refreshed.idToken)
            await MainActor.run {
                articleOverrides[article.id] = ArticleSummary(
                    id: article.id,
                    title: article.title,
                    author: article.author,
                    authorImage: article.authorImage,
                    category: article.category,
                    excerpt: article.excerpt,
                    imageName: article.imageName,
                    imageURL: article.imageURL,
                    timestamp: article.timestamp,
                    readTime: article.readTime,
                    content: article.content,
                    viewCount: article.viewCount + 1,
                    likeCount: article.likeCount,
                    commentCount: article.commentCount,
                    shareCount: article.shareCount,
                    type: article.type
                )
            }
        } catch {
            return
        }
    }

    private func relativeCommentTime(_ timestamp: TimeInterval) -> String {
        guard timestamp > 0 else { return appViewModel.selectedLanguage == "sw" ? "Sasa hivi" : "Just now" }
        let seconds = timestamp > 1_000_000_000_000 ? timestamp / 1000.0 : timestamp
        let interval = max(0, Date().timeIntervalSince1970 - seconds)
        let minutes = Int(interval / 60)
        if minutes < 1 { return appViewModel.selectedLanguage == "sw" ? "Sasa hivi" : "Just now" }
        if minutes < 60 { return "\(minutes)m ago" }
        let hours = minutes / 60
        if hours < 24 { return "\(hours)h ago" }
        return "\(hours / 24)d ago"
    }

    @ViewBuilder
    private var articleDetailDestination: some View {
        if let selectedArticle {
            ArticleDetailView(article: selectedArticle)
        } else {
            EmptyView()
        }
    }
}

private struct HealthTipCard: View {
    let tip: HealthTipSummary

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            Image(systemName: "heart.text.square.fill")
                .font(.system(size: 24, weight: .semibold))
                .foregroundStyle(HASETTheme.greenPrimary)
                .frame(width: 48, height: 48)
                .background(HASETTheme.greenPrimary.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

            VStack(alignment: .leading, spacing: 8) {
                Text(tip.text)
                    .font(HASETTheme.font(.medium, 16))
                    .foregroundStyle(HASETTheme.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)

                Text("— \(tip.author)")
                    .font(HASETTheme.font(.regular, 13))
                    .foregroundStyle(HASETTheme.textSecondary)
            }

            Spacer(minLength: 0)
        }
        .padding(16)
        .background(HASETTheme.backgroundCard)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(color: HASETTheme.greenPrimary.opacity(0.07), radius: 8, x: 0, y: 4)
    }
}

struct ArticleDetailView: View {
    let article: ArticleSummary
    @EnvironmentObject private var appViewModel: AppViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                HStack(spacing: 12) {
                    ProfileAvatarView(
                        imageSource: article.authorImage ?? "",
                        initials: article.author.split(separator: " ").prefix(2).compactMap(\.first).map(String.init).joined().uppercased(),
                        size: 42,
                        fontSize: 14
                    )
                    VStack(alignment: .leading, spacing: 2) {
                        Text(article.author)
                            .font(HASETTheme.font(.medium, 15))
                            .foregroundStyle(HASETTheme.textPrimary)
                        Text(relativeArticleTime(article.timestamp))
                            .font(HASETTheme.font(.regular, 12))
                            .foregroundStyle(HASETTheme.textSecondary)
                    }
                    Spacer()
                }

                RemoteOrAssetImage(urlString: article.imageURL, assetName: article.imageName)
                    .frame(height: 220)
                    .frame(maxWidth: .infinity)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))

                VStack(alignment: .leading, spacing: 10) {
                    Text(article.category.uppercased())
                        .font(HASETTheme.font(.medium, 12))
                        .foregroundStyle(HASETTheme.greenPrimary)
                    Text(article.title)
                        .font(HASETTheme.font(.medium, 24))
                        .foregroundStyle(HASETTheme.textPrimary)
                    Text(article.excerpt)
                        .font(HASETTheme.font(.regular, 15))
                        .foregroundStyle(HASETTheme.textSecondary)

                    HStack {
                        Label(article.author, systemImage: "person.crop.circle")
                        Spacer()
                        Label(article.readTime, systemImage: "clock")
                    }
                    .font(HASETTheme.font(.regular, 12))
                    .foregroundStyle(HASETTheme.textSecondary)
                }

                CardContainer {
                    VStack(alignment: .leading, spacing: 14) {
                        ForEach(article.content, id: \.self) { paragraph in
                            Text(paragraph)
                                .font(HASETTheme.font(.regular, 14))
                                .foregroundStyle(HASETTheme.textPrimary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                }

                HStack(spacing: 18) {
                    Label("\(article.likeCount)", systemImage: "heart")
                    Label("\(article.commentCount)", systemImage: "message")
                    Label("\(article.shareCount)", systemImage: "square.and.arrow.up")
                    Spacer()
                    Label(formattedViews(article.viewCount), systemImage: "eye")
                }
                .font(HASETTheme.font(.regular, 12))
                .foregroundStyle(HASETTheme.textSecondary)
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("articles"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func relativeArticleTime(_ timestamp: TimeInterval) -> String {
        guard timestamp > 0 else { return article.readTime }
        let seconds = timestamp > 1_000_000_000_000 ? timestamp / 1000.0 : timestamp
        let interval = max(0, Date().timeIntervalSince1970 - seconds)
        let hours = Int(interval / 3600)
        if hours < 1 { return appViewModel.selectedLanguage == "sw" ? "Sasa hivi" : "Just now" }
        if hours < 24 { return "\(hours)h ago" }
        let days = hours / 24
        if days < 7 { return "\(days)d ago" }
        return article.readTime
    }

    private func formattedViews(_ views: Int) -> String {
        if views < 1000 { return "\(views)" }
        if views < 1_000_000 {
            return String(format: "%.1fk", Double(views) / 1000.0)
        }
        return String(format: "%.1fM", Double(views) / 1_000_000.0)
    }
}

struct PharmacyView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                ForEach(StaticContentService.pharmacyCategories) { category in
                    CardContainer {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(category.title)
                                .font(HASETTheme.font(.medium, 16))
                            Text(category.subtitle)
                                .font(HASETTheme.font(.regular, 13))
                                .foregroundStyle(HASETTheme.textSecondary)
                        }
                    }
                }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.selectedLanguage == "sw" ? "Duka la Dawa" : "Pharmacy")
    }
}

struct ScheduleView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    var body: some View {
        List(StaticContentService.timeSlots, id: \.self) { slot in
            HStack {
                Text(slot)
                    .font(HASETTheme.font(.medium, 15))
                Spacer()
                Text(appViewModel.tr("available"))
                    .font(HASETTheme.font(.regular, 12))
                    .foregroundStyle(HASETTheme.greenPrimary)
            }
            .padding(.vertical, 4)
        }
        .navigationTitle(appViewModel.tr("manage_schedule"))
        .scrollContentBackground(.hidden)
        .background(HASETTheme.backgroundPrimary)
    }
}

struct NotificationsView: View {
    @EnvironmentObject private var appViewModel: AppViewModel

    var body: some View {
        Form {
            Toggle(appViewModel.tr("notification"), isOn: Binding(
                get: { appViewModel.notificationEnabled },
                set: { appViewModel.setNotificationEnabled($0) }
            ))

            Section(appViewModel.tr("recent")) {
                ForEach(appViewModel.notifications) { item in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(item.title)
                            .font(HASETTheme.font(.medium, 14))
                        if !item.message.isEmpty {
                            Text(item.message)
                                .font(HASETTheme.font(.regular, 12))
                                .foregroundStyle(HASETTheme.textSecondary)
                        }
                    }
                }
            }
        }
        .navigationTitle(appViewModel.tr("notifications"))
        .task {
            await appViewModel.loadNotifications(force: false)
        }
    }
}

struct SettingsView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var supportPresented = false
    @State private var languagePickerPresented = false
    @State private var themePickerPresented = false
    @State private var mfaEnabled = false
    @State private var mfaBusy = true
    @State private var mfaEnrollmentPresented = false
    @State private var mfaDisablePresented = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack {
                    Text(appViewModel.tr("settings"))
                        .font(HASETTheme.font(.medium, 20))
                        .foregroundStyle(HASETTheme.greenPrimary)
                    Spacer()
                }

                CardContainer {
                    VStack(spacing: 0) {
                        SettingsRow(icon: "bell", title: appViewModel.tr("notification"), subtitle: "Appointment and account alerts") {
                            Toggle("", isOn: Binding(
                                get: { appViewModel.notificationEnabled },
                                set: { appViewModel.setNotificationEnabled($0) }
                            ))
                            .labelsHidden()
                        }
                        SettingsDivider()
                        SettingsRow(icon: "location", title: appViewModel.tr("location_permission"), subtitle: appViewModel.locationEnabled ? "Enabled" : "Disabled") {
                            Toggle("", isOn: Binding(
                                get: { appViewModel.locationEnabled },
                                set: { appViewModel.setLocationEnabled($0) }
                            ))
                            .labelsHidden()
                        }
                        SettingsDivider()
                        SettingsRow(
                            icon: "mappin.and.ellipse",
                            title: appViewModel.tr("location"),
                            subtitle: appViewModel.currentUser?.location ?? appViewModel.tr("not_set")
                        )
                        SettingsDivider()
                        Button {
                            languagePickerPresented = true
                        } label: {
                            SettingsRow(icon: "globe", title: appViewModel.tr("language"), subtitle: appViewModel.selectedLanguage == "sw" ? appViewModel.tr("kiswahili") : appViewModel.tr("english"))
                        }
                        .buttonStyle(.plain)
                        SettingsDivider()
                        Button {
                            themePickerPresented = true
                        } label: {
                            SettingsRow(icon: "gearshape", title: appViewModel.tr("theme"), subtitle: appViewModel.themeMode.localizedLabel(languageCode: appViewModel.selectedLanguage))
                        }
                        .buttonStyle(.plain)
                        SettingsDivider()
                        SettingsRow(
                            icon: "lock.shield",
                            title: appViewModel.tr("multi_factor_authentication"),
                            subtitle: appViewModel.tr(mfaEnabled ? "mfa_enabled_desc" : "mfa_disabled_desc")
                        ) {
                            Toggle("", isOn: Binding(
                                get: { mfaEnabled },
                                set: { requestedValue in
                                    if requestedValue { mfaEnrollmentPresented = true }
                                    else { mfaDisablePresented = true }
                                }
                            ))
                            .labelsHidden()
                            .disabled(mfaBusy)
                        }
                        SettingsDivider()
                        NavigationLink {
                            ForgotPasswordView()
                        } label: {
                            SettingsRow(icon: "lock", title: appViewModel.tr("change_password"), subtitle: appViewModel.tr("reset_account_password"))
                        }
                        .buttonStyle(.plain)
                        SettingsDivider()
                        Button {
                            supportPresented = true
                        } label: {
                            SettingsRow(icon: "questionmark.circle", title: appViewModel.tr("help_support"), subtitle: appViewModel.tr("whatsapp_phone_support"))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .task { await refreshMFAStatus() }
        .sheet(isPresented: $supportPresented) {
            SupportSheet()
        }
        .sheet(isPresented: $mfaEnrollmentPresented, onDismiss: {
            Task { await refreshMFAStatus() }
        }) {
            MFAEnrollmentView(
                onComplete: {
                    mfaEnabled = true
                    mfaEnrollmentPresented = false
                },
                onCancel: { mfaEnrollmentPresented = false }
            )
            .environmentObject(appViewModel)
        }
        .sheet(isPresented: $mfaDisablePresented) {
            MFADisableView(
                onDisabled: {
                    mfaEnabled = false
                    mfaDisablePresented = false
                },
                onCancel: { mfaDisablePresented = false }
            )
            .environmentObject(appViewModel)
        }
        .confirmationDialog(appViewModel.tr("language"), isPresented: $languagePickerPresented, titleVisibility: .visible) {
            Button(appViewModel.tr("english")) { appViewModel.changeLanguage("en") }
            Button(appViewModel.tr("kiswahili")) { appViewModel.changeLanguage("sw") }
            Button(appViewModel.tr("close"), role: .cancel) {}
        }
        .confirmationDialog(appViewModel.tr("theme"), isPresented: $themePickerPresented, titleVisibility: .visible) {
            Button(ThemeMode.light.localizedLabel(languageCode: appViewModel.selectedLanguage)) { appViewModel.setThemeMode(.light) }
            Button(ThemeMode.dark.localizedLabel(languageCode: appViewModel.selectedLanguage)) { appViewModel.setThemeMode(.dark) }
            Button(ThemeMode.system.localizedLabel(languageCode: appViewModel.selectedLanguage)) { appViewModel.setThemeMode(.system) }
            Button(appViewModel.tr("close"), role: .cancel) {}
        }
    }

    private func refreshMFAStatus() async {
        mfaBusy = true
        defer { mfaBusy = false }
        guard let session = appViewModel.activeSession ?? SessionStore().loadSession() else { return }
        do {
            let service = AuthService()
            let freshSession = try await service.refreshSessionIfNeeded(session)
            SessionStore().saveSession(freshSession)
            appViewModel.activeSession = freshSession
            mfaEnabled = try await service.mobileMFAStatus(idToken: freshSession.idToken)
        } catch {
            appViewModel.alertState = AlertState(title: appViewModel.tr("error"), message: error.localizedDescription)
        }
    }
}

private struct MFADisableView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    let onDisabled: () -> Void
    let onCancel: () -> Void
    @State private var code = ""
    @State private var loading = false
    @State private var error: String?
    @State private var useRecoveryCode = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Image(systemName: "lock.slash")
                    .font(.system(size: 38, weight: .medium))
                    .foregroundStyle(HASETTheme.redPrimary)
                Text("Disable multi-factor authentication?")
                    .font(HASETTheme.font(.medium, 20))
                    .multilineTextAlignment(.center)
                Text(useRecoveryCode
                     ? "Enter one unused 10-character recovery code. This code will be consumed."
                     : "Enter the current six-digit code from your authenticator app to confirm.")
                    .font(HASETTheme.font(.regular, 14))
                    .foregroundStyle(HASETTheme.textSecondary)
                    .multilineTextAlignment(.center)
                if useRecoveryCode {
                    SecureField("Recovery code", text: $code)
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                        .textFieldStyle(.roundedBorder)
                        .onChange(of: code) { value in
                            code = String(value.uppercased().filter { "0123456789ABCDEF".contains($0) }.prefix(10))
                            error = nil
                        }
                } else {
                    SixDigitMFAInput(code: $code, isInvalid: error != nil, isVerified: false) {}
                }
                if let error {
                    Text(error)
                        .font(HASETTheme.font(.regular, 13))
                        .foregroundStyle(HASETTheme.redPrimary)
                        .multilineTextAlignment(.center)
                }
                Button(loading ? "Disabling…" : "Disable MFA") { disableMFA() }
                    .buttonStyle(PrimaryButtonStyle())
                    .disabled(loading || code.count != (useRecoveryCode ? 10 : 6))
                Button(useRecoveryCode ? "Use authenticator code" : "Use a recovery code") {
                    code = ""
                    error = nil
                    useRecoveryCode.toggle()
                }
                .foregroundStyle(HASETTheme.greenPrimary)
                Button(appViewModel.tr("cancel"), action: onCancel)
                    .foregroundStyle(HASETTheme.greenPrimary)
            }
            .padding(24)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(HASETTheme.backgroundPrimary.ignoresSafeArea())
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func disableMFA() {
        guard code.count == (useRecoveryCode ? 10 : 6), !loading else { return }
        guard let session = appViewModel.activeSession ?? SessionStore().loadSession() else {
            error = "Authentication expired. Please sign in again."
            return
        }
        loading = true
        error = nil
        Task {
            do {
                let service = AuthService()
                let freshSession = try await service.refreshSessionIfNeeded(session)
                SessionStore().saveSession(freshSession)
                appViewModel.activeSession = freshSession
                try await service.disableMobileMFA(code: code, idToken: freshSession.idToken)
                loading = false
                code = ""
                onDisabled()
            } catch {
                loading = false
                code = ""
                self.error = error.localizedDescription
            }
        }
    }
}

private struct SupportSheet: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var bugReport = ""

    var body: some View {
        NavigationStack {
            Form {
                Section(appViewModel.tr("contact")) {
                    Link(appViewModel.tr("support_whatsapp"), destination: URL(string: StaticContentService.supportWhatsAppURL)!)
                    Link(appViewModel.tr("call_support"), destination: URL(string: "tel://\(StaticContentService.supportPhone)")!)
                }

                Section(appViewModel.tr("bug_report")) {
                    TextEditor(text: $bugReport)
                        .frame(minHeight: 120)
                    Button(appViewModel.tr("submit_report")) {
                        appViewModel.submitBugReport(bugReport)
                        dismiss()
                    }
                }
            }
            .navigationTitle(appViewModel.tr("support"))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(appViewModel.tr("close")) { dismiss() }
                }
            }
        }
    }
}

struct AboutUsView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Image("BrandLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 290, height: 150)

                CardContainer {
                    Text(appViewModel.tr("about_text_1"))
                        .font(HASETTheme.font(.medium, 14))
                    Text(appViewModel.tr("about_text_2"))
                        .font(HASETTheme.font(.medium, 14))
                        .padding(.top, 16)
                    Text(appViewModel.tr("about_text_3"))
                        .font(HASETTheme.font(.medium, 14))
                        .foregroundStyle(HASETTheme.greenPrimary)
                        .padding(.top, 16)
                }

                CardContainer {
                    Text(appViewModel.tr("medical_disclaimer"))
                        .font(HASETTheme.font(.medium, 14))
                        .foregroundStyle(HASETTheme.redPrimary)
                    Text(appViewModel.tr("about_disclaimer_body"))
                        .font(HASETTheme.font(.regular, 12))
                        .padding(.top, 8)
                }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("about_us"))
    }
}

private struct DoctorDirectoryCard: View {
    let doctor: DoctorSummary
    let isSaved: Bool
    let onOpen: () -> Void
    let onToggleSave: () -> Void
    @EnvironmentObject private var appViewModel: AppViewModel

    var body: some View {
        Button(action: onOpen) {
            CardContainer {
                VStack(alignment: .leading, spacing: 12) {
                    HStack(alignment: .top, spacing: 12) {
                        ZStack(alignment: .bottomTrailing) {
                            ProfileAvatarView(
                                imageSource: doctor.profileImage ?? "",
                                initials: doctor.name.split(separator: " ").prefix(2).compactMap(\.first).map(String.init).joined().uppercased(),
                                size: 60,
                                fontSize: 18
                            )

                            if doctor.verified {
                                Image(systemName: "checkmark.seal.fill")
                                    .font(.system(size: 16))
                                    .foregroundStyle(HASETTheme.greenPrimary)
                                    .padding(2)
                                    .background(Circle().fill(Color.white))
                            }
                        }

                        VStack(alignment: .leading, spacing: 5) {
                            Text(doctor.name)
                                .font(HASETTheme.font(.medium, 16))
                                .foregroundStyle(HASETTheme.textPrimary)
                                .lineLimit(1)
                                .multilineTextAlignment(.leading)

                            Text("\(doctor.specialty) | \(doctor.hospital)")
                                .font(HASETTheme.font(.regular, 13))
                                .foregroundStyle(HASETTheme.textSecondary)
                                .lineLimit(2)
                                .multilineTextAlignment(.leading)

                            if let experienceYears = doctor.experienceYears {
                                Text("\(experienceYears)+ \(appViewModel.tr("experience"))")
                                    .font(HASETTheme.font(.regular, 12))
                                    .foregroundStyle(HASETTheme.textSecondary)
                            }
                        }

                        Spacer()

                        Button(action: onToggleSave) {
                            Image(systemName: isSaved ? "bookmark.fill" : "bookmark")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundStyle(isSaved ? HASETTheme.greenPrimary : HASETTheme.textSecondary)
                                .frame(width: 34, height: 34)
                                .background(Circle().fill(HASETTheme.backgroundPrimary))
                        }
                        .buttonStyle(.plain)
                    }

                    HStack(spacing: 12) {
                        Label(String(format: "%.1f", doctor.rating), systemImage: "star.fill")
                            .foregroundStyle(.orange)

                        Text(formattedAvailableTime)
                            .foregroundStyle(HASETTheme.textSecondary)

                        Spacer()

                        Text(doctor.availableToday ? appViewModel.tr("available") : appViewModel.tr("booked"))
                            .font(HASETTheme.font(.medium, 11))
                            .foregroundStyle(doctor.availableToday ? HASETTheme.greenPrimary : HASETTheme.redPrimary)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(
                                Capsule()
                                    .fill((doctor.availableToday ? HASETTheme.greenPrimary : HASETTheme.redPrimary).opacity(0.10))
                            )
                    }
                    .font(HASETTheme.font(.regular, 12))

                    HStack {
                        Text(doctor.consultationFee)
                            .font(HASETTheme.font(.medium, 13))
                            .foregroundStyle(HASETTheme.greenPrimary)
                        Spacer()
                        Text(appViewModel.tr("view_doctor"))
                            .font(HASETTheme.font(.medium, 13))
                            .foregroundStyle(HASETTheme.textSecondary)
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(HASETTheme.textSecondary)
                    }
                }
            }
        }
        .buttonStyle(.plain)
    }

    private var formattedAvailableTime: String {
        guard let availableTimes = doctor.availableTimes, !availableTimes.isEmpty else {
            return "09:00-17:00"
        }
        if availableTimes.count == 1 {
            return availableTimes[0]
        }
        return "\(availableTimes.first ?? "")-\(availableTimes.last ?? "")"
    }
}

private struct SettingsRow<Trailing: View>: View {
    let icon: String
    let title: String
    let subtitle: String
    @ViewBuilder var trailing: Trailing

    init(icon: String, title: String, subtitle: String, @ViewBuilder trailing: () -> Trailing = { EmptyView() }) {
        self.icon = icon
        self.title = title
        self.subtitle = subtitle
        self.trailing = trailing()
    }

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(HASETTheme.greenPrimary)
                .frame(width: 22)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(HASETTheme.font(.medium, 14))
                    .foregroundStyle(HASETTheme.textPrimary)
                Text(subtitle)
                    .font(HASETTheme.font(.regular, 12))
                    .foregroundStyle(HASETTheme.textSecondary)
            }
            Spacer()
            trailing
        }
        .padding(.vertical, 10)
    }
}

private struct SettingsDivider: View {
    var body: some View {
        Divider()
            .overlay(HASETTheme.divider)
    }
}

struct MedicalRecordsView: View {
    @EnvironmentObject private var appViewModel: AppViewModel

    var body: some View {
        List {
            Label("Consultation Summary", systemImage: "doc.text")
            Label("Prescriptions", systemImage: "cross.case")
            Label("Lab Reports", systemImage: "waveform.path.ecg")
        }
        .navigationTitle(appViewModel.tr("medical_records"))
    }
}

struct EditProfileView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var profileImageData: Data?
    @State private var hasSelectedNewPhoto = false
    @State private var fullName = ""
    @State private var phone = ""
    @State private var age = ""
    @State private var gender = ""
    @State private var bio = ""
    @State private var specialization = ""
    @State private var consultationFee = ""
    @State private var selectedAvailableTimes: Set<String> = []
    private var uploadPhotoLabel: String { appViewModel.tr("upload_photo") }

    var body: some View {
        Form {
            Section {
                VStack(spacing: 14) {
                    Group {
                        if let image = profileUIImage {
                            Image(uiImage: image)
                                .resizable()
                                .scaledToFill()
                        } else {
                            Circle()
                                .fill(HASETTheme.greenPrimary.opacity(0.12))
                                .overlay(
                                    Text(profileInitials)
                                        .font(HASETTheme.font(.medium, 28))
                                        .foregroundStyle(HASETTheme.greenPrimary)
                                )
                        }
                    }
                    .frame(width: 110, height: 110)
                    .clipShape(Circle())

                    PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                        Text(uploadPhotoLabel)
                    }
                    .font(HASETTheme.font(.medium, 15))
                    .foregroundStyle(HASETTheme.greenPrimary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
            }

            Section(appViewModel.tr("basic")) {
                TextField(appViewModel.tr("full_name"), text: $fullName)
                TextField(appViewModel.tr("phone"), text: $phone)
                    .keyboardType(.phonePad)
                TextField(appViewModel.tr("age"), text: $age)
                    .keyboardType(.numberPad)
                TextField(appViewModel.tr("gender"), text: $gender)
            }

            Section(appViewModel.tr("additional")) {
                TextField(appViewModel.tr("bio"), text: $bio, axis: .vertical)
                if appViewModel.currentUser?.role == .doctor {
                    TextField(appViewModel.tr("specialization"), text: $specialization)
                    TextField(appViewModel.tr("consultation_fee"), text: $consultationFee)
                    VStack(alignment: .leading, spacing: 10) {
                        Text(appViewModel.tr("available_times"))
                            .font(HASETTheme.font(.medium, 14))
                            .foregroundStyle(HASETTheme.textPrimary)

                        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 3), spacing: 8) {
                            ForEach(StaticContentService.timeSlots, id: \.self) { slot in
                                Button {
                                    toggleAvailableTime(slot)
                                } label: {
                                    Text(slot)
                                        .font(HASETTheme.font(.regular, 12))
                                        .foregroundStyle(selectedAvailableTimes.contains(slot) ? .white : HASETTheme.textPrimary)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 10)
                                        .background(
                                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                                .fill(selectedAvailableTimes.contains(slot) ? HASETTheme.greenPrimary : Color.white)
                                        )
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                                .stroke(selectedAvailableTimes.contains(slot) ? HASETTheme.greenPrimary : HASETTheme.divider, lineWidth: 1)
                                        )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }

            Button(appViewModel.tr("save_changes")) {
                Task {
                    let saved = await appViewModel.saveProfile(
                        fullName: fullName,
                        phone: phone,
                        age: age,
                        gender: gender,
                        bio: bio,
                        specialization: specialization,
                        consultationFee: consultationFee,
                        availableTimes: Array(selectedAvailableTimes).sorted { lhs, rhs in lhs < rhs },
                        profileImageData: hasSelectedNewPhoto ? profileImageData : nil
                    )
                    if saved { dismiss() }
                }
            }
            .buttonStyle(PrimaryButtonStyle())
            .disabled(appViewModel.isLoading)
            .listRowInsets(EdgeInsets())
            .padding(.top, 8)
        }
        .navigationTitle(appViewModel.tr("edit_profile"))
        .onAppear {
            guard let user = appViewModel.currentUser else { return }
            fullName = user.fullName
            phone = user.phone
            age = user.age ?? ""
            gender = user.gender ?? ""
            bio = user.bio ?? ""
            specialization = user.specialization ?? ""
            consultationFee = user.consultationFee ?? ""
            selectedAvailableTimes = Set(user.availableTimes ?? [])
            profileImageData = decodeProfileImage(from: user.profileImage)
        }
        .onChange(of: selectedPhotoItem) { newItem in
            guard let newItem else { return }
            Task {
                guard let data = try? await newItem.loadTransferable(type: Data.self),
                      let prepared = prepareProfileImage(data) else { return }
                profileImageData = prepared
                hasSelectedNewPhoto = true
            }
        }
    }

    private func toggleAvailableTime(_ slot: String) {
        if selectedAvailableTimes.contains(slot) {
            selectedAvailableTimes.remove(slot)
        } else {
            selectedAvailableTimes.insert(slot)
        }
    }

    private var profileInitials: String {
        let parts = fullName.split(separator: " ").prefix(2).compactMap(\.first)
        return String(parts).uppercased()
    }

    private var profileUIImage: UIImage? {
        guard let profileImageData else { return nil }
        return UIImage(data: profileImageData)
    }

    private func decodeProfileImage(from value: String) -> Data? {
        guard !value.isEmpty else { return nil }
        if let data = Data(base64Encoded: value) {
            return data
        }
        if let url = URL(string: value), let data = try? Data(contentsOf: url) {
            return data
        }
        return nil
    }

    private func prepareProfileImage(_ data: Data) -> Data? {
        guard let image = UIImage(data: data) else { return nil }
        let longestSide = max(image.size.width, image.size.height)
        let scale = min(1, 1024 / longestSide)
        let targetSize = CGSize(width: image.size.width * scale, height: image.size.height * scale)
        let renderer = UIGraphicsImageRenderer(size: targetSize)
        let resized = renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: targetSize))
        }
        return resized.jpegData(compressionQuality: 0.82)
    }
}

struct UsersView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    var body: some View {
        List(["All Users", "Doctors", "Patients", "Administrators"], id: \.self) { item in
            Text(item)
                .font(HASETTheme.font(.regular, 15))
        }
        .navigationTitle(appViewModel.tr("all_users"))
    }
}

struct ActivityLogsView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    var body: some View {
        List(["Login", "Registration", "Notification", "Doctor Approval", "Profile Update"], id: \.self) { item in
            Text(item)
                .font(HASETTheme.font(.regular, 15))
        }
        .navigationTitle(appViewModel.tr("activity_logs"))
    }
}

private extension NavigationLink where Label == Text, Destination: View {
    func profileLink() -> some View {
        self
            .font(HASETTheme.font(.medium, 16))
            .foregroundStyle(HASETTheme.textPrimary)
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(Color.white))
    }
}
