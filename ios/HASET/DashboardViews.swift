import PhotosUI
import AVFoundation
import StoreKit
import SwiftUI
import UIKit
import WebKit
import UniformTypeIdentifiers
import QuickLook

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
    @State private var dashboardSheet: DashboardSheetItem?
    @State private var navigationAlert: AlertState?

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
        .onChange(of: appViewModel.pendingNavigation) { destination in
            guard let destination else { return }
            handlePendingNavigation(destination)
        }
        .sheet(item: $dashboardSheet) { item in
            switch item {
            case .notifications:
                embeddedSheet {
                    NotificationsView()
                }
            case .prescriptions:
                embeddedSheet {
                    PrescriptionsListView(role: role)
                }
            case .chat(let conversation):
                ChatThreadView(conversation: conversation, role: role)
                    .environmentObject(appViewModel)
            }
        }
        .alert(item: $navigationAlert) { state in
            Alert(
                title: Text(state.title),
                message: Text(state.message),
                dismissButton: .default(Text(appViewModel.tr("ok")))
            )
        }
    }

    private func embeddedSheet<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        NavigationStack {
            content()
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(appViewModel.tr("done")) { dashboardSheet = nil }
                    }
                }
        }
        .environmentObject(appViewModel)
    }

    private func handlePendingNavigation(_ destination: PendingNavigation) {
        appViewModel.clearPendingNavigation()
        switch destination {
        case .tab(let tab):
            selectedTab = tab
        case .chat(let otherUserId):
            if let conversation = appViewModel.conversations.first(where: { $0.otherUserId == otherUserId }) {
                dashboardSheet = .chat(conversation)
            } else {
                selectedTab = .chat
            }
        case .prescriptions:
            dashboardSheet = .prescriptions
        case .notifications:
            dashboardSheet = .notifications
        case .alert(let title, let message):
            navigationAlert = AlertState(title: title, message: message)
        }
    }
}

private enum DashboardSheetItem: Identifiable {
    case notifications
    case prescriptions
    case chat(ConversationSummary)

    var id: String {
        switch self {
        case .notifications: return "notifications"
        case .prescriptions: return "prescriptions"
        case .chat(let conversation): return "chat-\(conversation.id)"
        }
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
                    if appViewModel.currentUser?.approved != true {
                        doctorPendingApprovalBanner
                    }
                } else {
                    headerCard
                }

                switch role {
                case .patient:
                    patientContent
                case .doctor:
                    doctorContent
                case .admin:
                    EmptyView()
                }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .refreshable {
            if role == .doctor {
                await appViewModel.refreshCurrentUser()
                await appViewModel.loadDoctorWallet(force: true)
            }
            await appViewModel.loadAppointments(force: true)
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            if role == .patient {
                await appViewModel.loadPatientHomeContent(force: false)
            } else if role == .doctor {
                await appViewModel.refreshCurrentUser()
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
                GlobalSearchView()
            } label: {
                headerIconButton("magnifyingglass")
            }
            .buttonStyle(.plain)
            .accessibilityLabel(appViewModel.tr("search"))

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

    private var doctorPendingApprovalBanner: some View {
        CardContainer(
            fill: HASETTheme.backgroundPrimary,
            shadowColor: .clear,
            cornerRadius: 12,
            padding: 14
        ) {
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Color.orange)
                Text(appViewModel.tr("doctor_awaiting_admin_approval"))
                    .font(HASETTheme.font(.medium, 14))
                    .foregroundStyle(HASETTheme.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(Color.orange.opacity(0.55), lineWidth: 1)
        )
    }

    private var doctorContent: some View {
        Group {
            SectionTitle(appViewModel.tr("today_s_overview"))
            doctorOverviewStats

            SectionTitle(appViewModel.tr("quick_actions"))
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 3), spacing: 12) {
                NavigationLink { AppointmentsOverviewView(role: .doctor) } label: {
                    doctorActionCard(title: appViewModel.tr("manage_schedule"), systemImage: "calendar")
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

                    Text(appViewModel.tr("no_pending_appointments"))
                        .font(HASETTheme.font(.medium, 16))
                        .foregroundStyle(HASETTheme.textPrimary)

                    Text(appViewModel.tr("all_caught_up"))
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
        .confirmationDialog(appViewModel.tr("filter_appointments"), isPresented: $showingDoctorFilterOptions, titleVisibility: .visible) {
            Button(appViewModel.tr("pending")) { selectedDoctorStatus = .pending }
            Button(appViewModel.tr("approved")) { selectedDoctorStatus = .approved }
            Button(appViewModel.tr("completed")) { selectedDoctorStatus = .completed }
            Button(appViewModel.tr("canceled")) { selectedDoctorStatus = .cancelled }
            Button(appViewModel.tr("clear_filter"), role: .destructive) { selectedDoctorStatus = nil }
        } message: {
            Text(appViewModel.tr("filter_appointments_by_status"))
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
        case .declined: return "hand.raised.fill"
        }
    }

    private func colorForAppointment(_ status: AppointmentSummary.Status) -> Color {
        switch status {
        case .pending: return HASETTheme.redPrimary
        case .approved: return HASETTheme.greenPrimary
        case .completed: return HASETTheme.textPrimary
        case .cancelled: return HASETTheme.redPrimary
        case .declined: return HASETTheme.redPrimary
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
    @State private var showBalance = false
    @State private var showWithdraw = false
    @State private var amount = ""
    @State private var mfaCode = ""
    @State private var message: String?
    @State private var showNoBalanceAlert = false
    @State private var payoutMethod = "mobile_money"
    @State private var withdrawalOtpSent = false
    @State private var sendCodePhase: AnimatedSendButton.Phase = .idle
    @State private var confirmPhase: AnimatedSendButton.Phase = .idle
    @State private var showWithdrawalConfirmation = false
    @State private var showPayoutAccounts = false
    @State private var selectedPayoutProvider = ""
    @State private var destinationPhone = ""
    @State private var destinationMFA = ""
    @State private var destinationMessage: String?
    @State private var savingDestination = false
    @State private var destinationSendPhase: AnimatedSendButton.Phase = .idle
    @State private var destinationOtpSent = false
    @State private var resendSeconds = 0
    @State private var destinationCooldownTask: Task<Void, Never>?

    private let payoutProviderOptions = [
        "Vodacom M-Pesa",
        "Airtel Money",
        "Tigo Pesa",
        "Halotel",
        "Mixx by Yas"
    ]

    private var balanceText: String {
        if !showBalance { return "•••••• TZS" }
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
        guard let updated = appViewModel.doctorWallet?.lastUpdated else {
            return appViewModel.tr("wallet_unknown_date")
        }
        let date = Date(timeIntervalSince1970: updated / 1000)
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: appViewModel.selectedLanguage == "sw" ? "sw_TZ" : "en_US")
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                SectionTitle(appViewModel.tr("my_wallet"))
                CardContainer {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Image("icons8_coins")
                                .resizable()
                                .renderingMode(.original)
                                .scaledToFit()
                                .frame(width: 22, height: 22)
                            Spacer()
                            Text("\(appViewModel.tr("wallet_updated")) \(updatedText)")
                                .font(HASETTheme.font(.regular, 12))
                                .foregroundStyle(HASETTheme.textSecondary)
                            Button { showBalance.toggle() } label: {
                                Image(systemName: showBalance ? "eye" : "eye.slash")
                                    .font(.system(size: 20, weight: .semibold))
                                    .foregroundStyle(HASETTheme.greenPrimary)
                                    .frame(width: 40, height: 40)
                                    .background(Circle().fill(HASETTheme.greenPrimary.opacity(0.08)))
                            }
                            .accessibilityLabel(showBalance ? appViewModel.tr("hide_balance") : appViewModel.tr("show_balance"))
                        }

                        Text(balanceText)
                            .font(HASETTheme.font(.black, 28))
                            .foregroundStyle(HASETTheme.textPrimary)

                        SectionTitle(appViewModel.tr("wallet_statistics"))
                        HStack(spacing: 12) {
                            CardContainer(fill: HASETTheme.greenPrimary.opacity(0.08), shadowColor: .clear, cornerRadius: 14, padding: 14) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(appViewModel.tr("total_earnings"))
                                        .font(HASETTheme.font(.regular, 12))
                                        .foregroundStyle(HASETTheme.textSecondary)
                                    Text(earningsText)
                                        .font(HASETTheme.font(.medium, 16))
                                        .foregroundStyle(HASETTheme.textPrimary)
                                }
                            }
                        }
                    }
                }
                Button(appViewModel.tr("btn_withdraw")) {
                    if (appViewModel.doctorWallet?.balance ?? 0) > 0 {
                        selectDefaultPayoutMethod()
                        showWithdraw = true
                    } else {
                        showNoBalanceAlert = true
                    }
                }
                    .buttonStyle(PrimaryButtonStyle())
                Button { configurePayoutDestination() } label: {
                    Label(appViewModel.tr("payout_accounts"), systemImage: "wallet.pass.fill")
                        .frame(maxWidth: .infinity)
                }
                    .buttonStyle(.borderedProminent)
                    .tint(HASETTheme.greenPrimary)
                SectionTitle(appViewModel.tr("withdrawal_history"))
                if appViewModel.doctorWalletLoading && appViewModel.doctorWithdrawals.isEmpty { ProgressView() }
                else if let error = appViewModel.doctorWalletError {
                    VStack {
                        Text(error)
                        Button(appViewModel.tr("retry")) { Task { await appViewModel.loadDoctorWithdrawals() } }
                    }
                }
                else if appViewModel.doctorWithdrawals.isEmpty {
                    Text(appViewModel.tr("no_withdrawals_yet"))
                        .foregroundStyle(HASETTheme.textSecondary)
                }
                else { ForEach(appViewModel.doctorWithdrawals) { item in
                    CardContainer {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.id).font(.caption)
                                Text(String(format: "%,.0f TZS", item.amount)).font(.headline)
                                if item.feeAmount > 0 {
                                    Text(String(format: appViewModel.tr("provider_fee_format"), String(format: "%,.0f", item.feeAmount)))
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
                Text(appViewModel.tr("wallet_consultation_fees_info"))
                    .font(HASETTheme.font(.regular, 12))
                    .foregroundStyle(HASETTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("my_wallet"))
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { showBalance = !isHidden }
        .task {
            await appViewModel.loadDoctorWallet(force: true)
            await appViewModel.loadDoctorWithdrawals()
        }
        .refreshable { await appViewModel.loadDoctorWithdrawals() }
        .alert(appViewModel.tr("withdrawal_unavailable_title"), isPresented: $showNoBalanceAlert) {
            Button(appViewModel.tr("ok"), role: .cancel) {}
        } message: {
            Text(appViewModel.tr("no_balance_withdrawal"))
        }
        .sheet(isPresented: $showWithdraw) {
            VStack(spacing: 16) {
                Text(appViewModel.tr("request_withdrawal")).font(.title3.bold())
                if availablePayoutMethods.isEmpty {
                    Text(appViewModel.tr("no_verified_payout_destination")).foregroundStyle(.red)
                } else {
                    Picker(appViewModel.tr("payout_account_label"), selection: $payoutMethod) {
                        ForEach(availablePayoutMethods, id: \.0) { method in Text(method.1).tag(method.0) }
                    }.pickerStyle(.segmented)
                    Text(selectedDestinationLabel).font(.caption).foregroundStyle(HASETTheme.textSecondary)
                }
                TextField(appViewModel.tr("withdrawal_amount"), text: $amount).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
                if withdrawalOtpSent {
                    Text(appViewModel.tr("withdrawal_email_code_notice"))
                        .font(.caption).foregroundStyle(HASETTheme.textSecondary)
                        .multilineTextAlignment(.center)
                    SixDigitMFAInput(code: $mfaCode, isInvalid: message != nil, isVerified: false) {}
                    AnimatedSendButton(
                        idleTitle: appViewModel.tr("verify_and_withdraw"),
                        sendingTitle: appViewModel.tr("submitting"),
                        successTitle: appViewModel.tr("withdrawal_requested"),
                        phase: $confirmPhase,
                        disabled: availablePayoutMethods.isEmpty
                    ) { submitWithdrawal() }
                } else {
                    AnimatedSendButton(
                        idleTitle: appViewModel.tr("continue_to_email_verification"),
                        sendingTitle: appViewModel.tr("sending_email_code"),
                        successTitle: appViewModel.tr("code_sent"),
                        phase: $sendCodePhase,
                        disabled: availablePayoutMethods.isEmpty
                    ) { sendWithdrawalCode() }
                }
                if let message { Text(message).foregroundStyle(.red).font(.caption) }
                Button(appViewModel.tr("cancel")) { clearPayoutState(); showWithdraw = false }
            }
            .padding(24)
            .background(HASETTheme.backgroundPrimary)
            .interactiveDismissDisabled(sendCodePhase == .sending || confirmPhase == .sending)
            .overlay {
                if showWithdrawalConfirmation {
                    ConfirmationOverlay(message: appViewModel.tr("withdrawal_request_submitted"))
                }
            }
        }
        .sheet(isPresented: $showPayoutAccounts) {
            ScrollView {
                VStack(spacing: 16) {
                    Text(appViewModel.tr("payout_accounts")).font(.title3.bold())
                    Text(appViewModel.tr("payout_accounts_desc"))
                        .font(.caption).foregroundStyle(HASETTheme.textSecondary)
                        .multilineTextAlignment(.center)
                    VStack(alignment: .leading, spacing: 8) {
                        Text(appViewModel.tr("payment_provider"))
                            .font(HASETTheme.font(.medium, 14))
                            .foregroundStyle(HASETTheme.textPrimary)
                        Picker(appViewModel.tr("payment_provider"), selection: $selectedPayoutProvider) {
                            ForEach(payoutProviderOptions, id: \.self) { provider in
                                Text(provider).tag(provider)
                            }
                        }
                        .pickerStyle(.menu)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .stroke(HASETTheme.divider, lineWidth: 1)
                        )
                    }
                    TextField(appViewModel.tr("mobile_number"), text: $destinationPhone)
                        .keyboardType(.phonePad)
                        .textFieldStyle(.roundedBorder)
                    Text(appViewModel.tr("email_verification_code_hint"))
                        .font(.caption).foregroundStyle(HASETTheme.textSecondary)
                        .multilineTextAlignment(.center)
                    SixDigitMFAInput(code: $destinationMFA, isInvalid: destinationMessage != nil, isVerified: false) {}
                    AnimatedSendButton(
                        idleTitle: destinationOtpSent ? appViewModel.tr("resend_email_code") : appViewModel.tr("send_email_code"),
                        sendingTitle: appViewModel.tr("sending_email_code"),
                        successTitle: appViewModel.tr("code_sent"),
                        phase: $destinationSendPhase,
                        disabled: resendSeconds > 0
                    ) { sendDestinationCode() }
                    if resendSeconds > 0 {
                        Text(String(format: appViewModel.tr("resend_email_code_countdown"), resendSeconds))
                            .font(.caption).foregroundStyle(HASETTheme.textSecondary)
                            .frame(maxWidth: .infinity)
                    }
                    if destinationOtpSent {
                        Text(appViewModel.tr("email_verification_code_sent"))
                            .font(.caption).foregroundStyle(HASETTheme.greenPrimary)
                    }
                    if let destinationMessage { Text(destinationMessage).foregroundStyle(.red).font(.caption) }
                    Button(savingDestination ? appViewModel.tr("submitting") : appViewModel.tr("save_payout_account")) { submitPayoutDestination() }
                        .buttonStyle(PrimaryButtonStyle()).disabled(savingDestination || selectedPayoutProvider.isEmpty)
                    Button(appViewModel.tr("cancel")) { clearDestinationState(); showPayoutAccounts = false }
                }.padding(24)
            }
            .onAppear {
                if selectedPayoutProvider.isEmpty {
                    selectedPayoutProvider = payoutProviderOptions.first ?? ""
                }
            }
            .onDisappear {
                destinationCooldownTask?.cancel()
            }
            .interactiveDismissDisabled(savingDestination)
        }
    }

    private var availablePayoutMethods: [(String, String)] {
        var methods: [(String, String)] = []
        if appViewModel.doctorWallet?.mobileMoneyDestination?.available == true {
            methods.append(("mobile_money", appViewModel.tr("mobile_money")))
        }
        if appViewModel.doctorWallet?.bankDestination?.available == true {
            methods.append(("bank", appViewModel.tr("bank")))
        }
        return methods
    }

    private var selectedDestinationLabel: String {
        payoutMethod == "bank"
            ? (appViewModel.doctorWallet?.bankDestination?.label ?? appViewModel.tr("bank"))
            : (appViewModel.doctorWallet?.mobileMoneyDestination?.label ?? appViewModel.tr("mobile_money"))
    }

    private func sendWithdrawalCode() {
        guard let value = Int(amount), value >= 5000, Double(value) <= (appViewModel.doctorWallet?.balance ?? 0) else {
            message = appViewModel.tr("enter_valid_withdrawal_amount")
            return
        }
        guard !payoutMethod.isEmpty, availablePayoutMethods.contains(where: { $0.0 == payoutMethod }) else {
            message = appViewModel.tr("no_verified_payout_destination")
            return
        }
        guard let session = appViewModel.activeSession ?? SessionStore().loadSession() else {
            message = appViewModel.tr("authentication_expired")
            return
        }
        sendCodePhase = .sending
        message = nil
        Task {
            do {
                let service = AuthService()
                let fresh = try await service.refreshSessionIfNeeded(session)
                SessionStore().saveSession(fresh)
                appViewModel.activeSession = fresh
                try await service.sendDoctorPayoutEmailOtp(idToken: fresh.idToken)
                await MainActor.run {
                    sendCodePhase = .success
                    withdrawalOtpSent = true
                    mfaCode = ""
                }
            } catch {
                await MainActor.run {
                    sendCodePhase = .failed
                    message = error.localizedDescription
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                        if !withdrawalOtpSent { sendCodePhase = .idle }
                    }
                }
            }
        }
    }

    private func submitWithdrawal() {
        guard let value = Int(amount), value >= 5000, Double(value) <= (appViewModel.doctorWallet?.balance ?? 0) else {
            message = appViewModel.tr("enter_valid_withdrawal_amount")
            return
        }
        guard mfaCode.count == 6 else {
            message = appViewModel.tr("email_code_required")
            return
        }
        guard let session = appViewModel.activeSession ?? SessionStore().loadSession() else {
            message = appViewModel.tr("authentication_expired")
            return
        }
        confirmPhase = .sending
        message = nil
        Task { do {
            let service = AuthService()
            let fresh = try await service.refreshSessionIfNeeded(session)
            SessionStore().saveSession(fresh)
            appViewModel.activeSession = fresh
            let token = try await service.verifyDoctorActionCode(code: mfaCode, idToken: fresh.idToken)
            try await service.requestDoctorWithdrawal(amount: value, reason: "Doctor payout request", payoutMethod: payoutMethod, idToken: fresh.idToken, mfaActionToken: token)
            await MainActor.run {
                confirmPhase = .success
                showWithdrawalConfirmation = true
            }
            await appViewModel.loadDoctorWithdrawals()
            try? await Task.sleep(nanoseconds: 1_600_000_000)
            await MainActor.run {
                clearPayoutState()
                showWithdrawalConfirmation = false
                showWithdraw = false
            }
        } catch {
            await MainActor.run {
                confirmPhase = .failed
                message = error.localizedDescription
                mfaCode = ""
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                    if !showWithdrawalConfirmation { confirmPhase = .idle }
                }
            }
        } }
    }

    private func configurePayoutDestination() {
        clearDestinationState()
        showPayoutAccounts = true
    }

    private func sendDestinationCode() {
        guard destinationSendPhase != .sending, resendSeconds <= 0 else { return }
        guard let session = appViewModel.activeSession ?? SessionStore().loadSession() else {
            destinationMessage = appViewModel.tr("authentication_expired")
            return
        }
        destinationSendPhase = .sending
        destinationMessage = nil
        Task {
            do {
                let service = AuthService()
                let fresh = try await service.refreshSessionIfNeeded(session)
                SessionStore().saveSession(fresh)
                appViewModel.activeSession = fresh
                try await service.sendDoctorPayoutEmailOtp(idToken: fresh.idToken)
                await MainActor.run {
                    destinationOtpSent = true
                    destinationSendPhase = .success
                    startDestinationCooldown()
                }
            } catch {
                await MainActor.run {
                    destinationSendPhase = .failed
                    destinationMessage = error.localizedDescription
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                        if !destinationOtpSent { destinationSendPhase = .idle }
                    }
                }
            }
        }
    }

    private func startDestinationCooldown(seconds: Int = 60) {
        resendSeconds = seconds
        destinationCooldownTask?.cancel()
        destinationCooldownTask = Task { @MainActor in
            while resendSeconds > 0 && !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard !Task.isCancelled else { return }
                resendSeconds -= 1
            }
            guard !Task.isCancelled else { return }
            destinationSendPhase = .idle
        }
    }

    private func selectDefaultPayoutMethod() {
        if !availablePayoutMethods.contains(where: { $0.0 == payoutMethod }) {
            payoutMethod = availablePayoutMethods.first?.0 ?? "mobile_money"
        }
    }

    private func submitPayoutDestination() {
        let provider = selectedPayoutProvider.trimmingCharacters(in: .whitespacesAndNewlines)
        let phone = destinationPhone.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !provider.isEmpty,
              phone.range(of: "^(?:0\\d{9}|\\+255\\d{9})$", options: .regularExpression) != nil else {
            destinationMessage = appViewModel.tr("invalid_phone_payout_format")
            return
        }
        guard destinationMFA.count == 6 else {
            destinationMessage = appViewModel.tr("email_code_required")
            return
        }
        guard let session = appViewModel.activeSession ?? SessionStore().loadSession() else {
            destinationMessage = appViewModel.tr("authentication_expired")
            return
        }
        savingDestination = true; destinationMessage = nil
        Task { do {
            let service = AuthService()
            let fresh = try await service.refreshSessionIfNeeded(session)
            SessionStore().saveSession(fresh); appViewModel.activeSession = fresh
            let token = try await service.verifyDoctorActionCode(code: destinationMFA, idToken: fresh.idToken)
            try await service.updateDoctorPayoutDestination(type: "mobile_money", provider: provider, phone: phone, bankCode: "", bankAccount: "", idToken: fresh.idToken, mfaActionToken: token)
            try await service.mirrorPayoutDestinationInstant(
                doctorId: fresh.userId,
                destinationType: "mobile_money",
                provider: provider,
                bankCode: "",
                phoneNumber: phone,
                bankAccount: "",
                idToken: fresh.idToken
            )
            await MainActor.run {
                clearDestinationState()
                showPayoutAccounts = false
                appViewModel.markPayoutDestinationAvailable(type: "mobile_money", provider: provider, phone: phone)
                appViewModel.alertState = AlertState(
                    title: appViewModel.tr("payout_account_saved_title"),
                    message: appViewModel.tr("payout_account_saved_message")
                )
            }
        } catch { await MainActor.run { savingDestination = false; destinationMessage = error.localizedDescription; destinationMFA = "" } } }
    }
    private func clearDestinationState() {
        destinationCooldownTask?.cancel()
        destinationCooldownTask = nil
        selectedPayoutProvider = payoutProviderOptions.first ?? ""
        destinationPhone = ""
        destinationMFA = ""
        destinationMessage = nil
        savingDestination = false
        destinationSendPhase = .idle
        destinationOtpSent = false
        resendSeconds = 0
    }
    private func clearPayoutState() {
        amount = ""
        mfaCode = ""
        message = nil
        withdrawalOtpSent = false
        sendCodePhase = .idle
        confirmPhase = .idle
        showWithdrawalConfirmation = false
    }
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
                .frame(maxWidth: .infinity)
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
                    instantOptionCard(
                        title: "Online Chat",
                        systemImage: "message.fill",
                        isSelected: appointmentType == "Online Chat"
                    ) {
                        selectInstantAppointment(type: "Online Chat")
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
                        submitAppointment(paymentConfirmed: true)
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
                onPaymentConfirmed: { _ in
                    submitAppointment(paymentConfirmed: true)
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
        !doctor.isDemo && consultationFeeAmount > 0
    }

    private var bookingDoctorCard: some View {
        CardContainer {
            HStack(spacing: 14) {
                ZStack(alignment: .bottomTrailing) {
                    ProfileAvatarWithVerifiedBadge(
                        imageSource: doctor.profileImage ?? "",
                        initials: doctor.name.split(separator: " ").prefix(2).compactMap(\.first).map(String.init).joined().uppercased(),
                        size: 60,
                        fontSize: 18,
                        showVerified: doctor.verified
                    )
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

    private func submitAppointment(paymentConfirmed: Bool = false) {
        appViewModel.bookAppointment(
            doctor: doctor,
            date: selectedDate,
            time: selectedTime,
            reason: reason,
            appointmentType: appointmentType,
            paymentConfirmed: paymentConfirmed
        )
    }
}

struct PaymentCheckoutView: View {
    enum PaymentMethod: String {
        case mobileMoney
        // Card / bank payments disabled for now — mobile money only (Android parity).
        // case cardPayment
    }

    let doctor: DoctorSummary
    let amount: Double
    let initialMethod: PaymentMethod
    let onPaymentConfirmed: (Int?) -> Void
    var onCancelRegistrationPayment: (() -> Void)? = nil

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
    /*
    private let cardProviders: [ProviderOption] = [
        ProviderOption(name: "CRDB", displayName: "CRDB", imageName: nil),
        ProviderOption(name: "NMB", displayName: "NMB", imageName: nil),
        ProviderOption(name: "TCB", displayName: "TCB", imageName: nil),
        ProviderOption(name: "AKIBA", displayName: "AKIBA", imageName: nil)
    ]
    */

    init(
        doctor: DoctorSummary,
        amount: Double,
        initialMethod: PaymentMethod,
        consultationIdOverride: String? = nil,
        onPaymentConfirmed: @escaping (Int?) -> Void,
        onCancelRegistrationPayment: (() -> Void)? = nil
    ) {
        self.doctor = doctor
        self.amount = amount
        self.initialMethod = initialMethod
        self.onPaymentConfirmed = onPaymentConfirmed
        self.onCancelRegistrationPayment = onCancelRegistrationPayment
        _selectedMethod = State(initialValue: initialMethod)
        let registrationUserId = SessionStore().loadSession()?.userId ?? ""
        if doctor.id == "doctor_registration", !registrationUserId.isEmpty {
            _consultationId = State(initialValue: "registration-\(registrationUserId)")
        } else if let consultationIdOverride, !consultationIdOverride.isEmpty {
            _consultationId = State(initialValue: consultationIdOverride)
        } else {
            _consultationId = State(initialValue: "consult-\(UUID().uuidString.lowercased())")
        }
        // Keep margin below Snippe's 30-character processor limit.
        _idempotencyKey = State(initialValue: String(UUID().uuidString.replacingOccurrences(of: "-", with: "").prefix(24)).lowercased())
    }

    private var isDoctorRegistrationPayment: Bool {
        doctor.id == "doctor_registration" || consultationId == "registration-\(appViewModel.currentUser?.userId ?? "")"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    CardContainer {
                        HStack(spacing: 14) {
                            ProfileAvatarWithVerifiedBadge(
                                imageSource: doctor.profileImage ?? "",
                                initials: doctor.name.split(separator: " ").prefix(2).compactMap(\.first).map(String.init).joined().uppercased(),
                                size: 56,
                                fontSize: 18,
                                showVerified: doctor.verified
                            )

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

                                paymentMethodCard(
                                    title: appViewModel.tr("mobile_money"),
                                    systemImage: "iphone.gen3.radiowaves.left.and.right",
                                    isSelected: true
                                ) {}
                                .disabled(true)
                            }

                            VStack(alignment: .leading, spacing: 10) {
                                Text(appViewModel.tr("payment_provider"))
                                    .font(HASETTheme.font(.medium, 13))
                                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                                    ForEach(mobileProviders) { provider in
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
                                Text(appViewModel.tr("payment_number"))
                                    .font(HASETTheme.font(.medium, 13))
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
                                .disabled(isProcessing || normalizedWalletNumber.isEmpty)
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
                        } else if doctor.id == "doctor_registration", let onCancelRegistrationPayment {
                            onCancelRegistrationPayment()
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
        .screenshotProtected()
    }

    private func preparePaymentSession() async {
        await MainActor.run {
            statusMessage = appViewModel.tr("preparing_payment")
            canRetryStatus = false
        }
        if paymentAuthSession != nil {
            await MainActor.run {
                paymentSessionReady = true
                statusMessage = ""
            }
            return
        }
        if appViewModel.activeSession != nil || sessionStore.loadSession() != nil {
            await MainActor.run {
                paymentSessionReady = true
                statusMessage = ""
            }
            return
        }
        if isDoctorRegistrationPayment {
            await MainActor.run {
                paymentSessionReady = false
                statusMessage = appViewModel.tr("verify_email_before_login")
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
        mobileProviders
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
            userId: paymentAuthSession?.userId ?? sessionStore.loadSession()?.userId ?? appViewModel.activeSession?.userId ?? "",
            email: doctor.email ?? "",
            fullName: doctor.name,
            phone: doctor.phoneNumber ?? "",
            role: isDoctorRegistrationPayment ? .doctor : .patient,
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
        guard !normalizedWalletNumber.isEmpty else { return }
        guard amount >= HASETConstants.minPaymentAmount else {
            await MainActor.run {
                statusMessage = "Minimum payment amount is \(Int(HASETConstants.minPaymentAmount)) TZS."
            }
            return
        }
        guard amount <= HASETConstants.maxPaymentAmount else {
            await MainActor.run {
                statusMessage = "Amount exceeds maximum limit."
            }
            return
        }

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
                paymentMethod: "mobile_money",
                provider: selectedProvider,
                paymentAccount: backendPaymentAccount,
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
                await MainActor.run {
                    statusMessage = appViewModel.tr("check_phone_complete_payment")
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
                    if isDoctorRegistrationPayment {
                        let userId = appViewModel.currentUser?.userId
                            ?? appViewModel.activeSession?.userId
                            ?? sessionStore.loadSession()?.userId
                            ?? ""
                        if !userId.isEmpty {
                            try? await paymentService.markDoctorRegistrationPaid(
                                userId: userId,
                                consultationId: consultationId,
                                transactionId: transactionId,
                                idToken: idToken
                            )
                        }
                    }
                    await MainActor.run {
                        isProcessing = false
                        canRetryStatus = false
                        processingPulse = false
                        statusMessage = appViewModel.tr("payment_confirmed")
                        hostedCheckout = nil
                        onPaymentConfirmed(transactionId)
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
        guard let session = paymentAuthSession ?? appViewModel.activeSession ?? sessionStore.loadSession() else { return nil }
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

    var profileImage: String? { appointments.first?.profileImage }
}

private struct DoctorPatientDetailView: View {
    let patient: DoctorPatientItem
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var showAllAppointments = false
    @State private var loadedProfileImage = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                CardContainer {
                    VStack(spacing: 10) {
                        ProfileAvatarView(
                            imageSource: loadedProfileImage.isEmpty ? (patient.profileImage ?? "") : loadedProfileImage,
                            initials: String(patient.name.prefix(1)).uppercased(),
                            size: 92,
                            fontSize: 32
                        )
                        Text(patient.name).font(HASETTheme.font(.medium, 22))
                        Text("Patient").foregroundStyle(HASETTheme.textSecondary)
                    }
                    .frame(maxWidth: .infinity)
                }

                if !patient.appointments.isEmpty {
                    CardContainer {
                        VStack(alignment: .leading, spacing: 12) {
                            Button {
                                withAnimation(.easeInOut(duration: 0.2)) { showAllAppointments.toggle() }
                            } label: {
                                HStack {
                                    Text("All appointments").font(HASETTheme.font(.medium, 18))
                                    Spacer()
                                    Image(systemName: showAllAppointments ? "chevron.up" : "chevron.down")
                                        .foregroundStyle(HASETTheme.greenPrimary)
                                }
                            }
                            .buttonStyle(.plain)

                            if showAllAppointments {
                                ForEach(patient.appointments.sorted { ($0.createdAt ?? 0) > ($1.createdAt ?? 0) }) { appointment in
                                VStack(alignment: .leading, spacing: 6) {
                                    detailLine("Reason", appointment.subtitle)
                                    detailLine("Type", appointment.appointmentType ?? "Visit")
                                    detailLine("Date", appointment.dateText)
                                    detailLine("Status", appointment.status.localizedLabel(languageCode: "en"))
                                }
                                if appointment.id != patient.appointments.last?.id { Divider() }
                                }
                            } else if let latest = patient.appointments.max(by: { ($0.createdAt ?? 0) < ($1.createdAt ?? 0) }) {
                                detailLine("Latest", latest.dateText.isEmpty ? latest.subtitle : latest.dateText)
                            }
                        }
                    }
                }

                CardContainer {
                    Text("Medical information and profile details will appear here when the patient has provided them.")
                        .font(HASETTheme.font(.regular, 14))
                        .foregroundStyle(HASETTheme.textSecondary)
                }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle("Patient details")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            if let image = await appViewModel.loadProfileImage(userId: patient.id), !image.isEmpty {
                loadedProfileImage = image
            }
        }
    }

    private func detailLine(_ title: String, _ value: String) -> some View {
        HStack(alignment: .top) {
            Text(title).font(HASETTheme.font(.medium, 14)).frame(width: 90, alignment: .leading)
            Text(value.isEmpty ? "Not provided" : value)
                .font(HASETTheme.font(.regular, 14))
                .foregroundStyle(HASETTheme.textSecondary)
            Spacer()
        }
    }
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
                        NavigationLink {
                            DoctorPatientDetailView(patient: patient)
                        } label: {
                            CardContainer {
                            HStack(spacing: 14) {
                                ProfileAvatarView(
                                    imageSource: patient.profileImage ?? "",
                                    initials: String(patient.name.prefix(1)).uppercased(),
                                    size: 48,
                                    fontSize: 18
                                )
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
                        .buttonStyle(.plain)
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
    @State private var exportPayload: [Any]?
    @State private var exportError: String?

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
                    get: { selectedStatus ?? .pending },
                    set: { selectedStatus = $0 }
                )) {
                    ForEach([AppointmentSummary.Status.pending, .approved, .completed, .cancelled], id: \.id) { status in
                        Text(status.localizedLabel(languageCode: appViewModel.selectedLanguage)).tag(status)
                    }
                }
                .pickerStyle(.segmented)
                .onAppear {
                    if selectedStatus == nil {
                        // New bookings are created as pending and become
                        // approved only after the doctor accepts them. Start
                        // patients on the state where their new booking is
                        // visible, matching Android's appointment flow.
                        selectedStatus = .pending
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
                                HStack(spacing: 12) {
                                    ProfileAvatarView(
                                        imageSource: appointment.profileImage ?? "",
                                        initials: String(appointment.title.prefix(1)).uppercased(),
                                        size: 44,
                                        fontSize: 17
                                    )
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
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button {
                        export(.csv)
                    } label: {
                        Label(appViewModel.tr("export_csv"), systemImage: "tablecells")
                    }
                    Button {
                        export(.pdf)
                    } label: {
                        Label(appViewModel.tr("export_pdf"), systemImage: "doc.fill")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
                .accessibilityLabel(appViewModel.tr("export_appointments"))
                .disabled(appointments.isEmpty)
            }
        }
        .sheet(isPresented: Binding(
            get: { exportPayload != nil },
            set: { if !$0 { exportPayload = nil } }
        )) {
            ShareSheet(items: exportPayload ?? [])
        }
        .alert(appViewModel.tr("error"), isPresented: Binding(
            get: { exportError != nil },
            set: { if !$0 { exportError = nil } }
        )) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(exportError ?? "")
        }
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
        } else if role == .patient && appointment.canPatientCancel {
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

    private enum ExportFormat {
        case csv
        case pdf
    }

    private func export(_ format: ExportFormat) {
        let rows = appointments
        guard !rows.isEmpty else { return }
        switch format {
        case .csv:
            exportPayload = [AppointmentExportBuilder.csv(rows)]
        case .pdf:
            guard let data = AppointmentExportBuilder.pdf(rows) else {
                exportError = appViewModel.tr("export_failed")
                return
            }
            exportPayload = [data]
        }
    }

    private func statusColor(_ status: AppointmentSummary.Status) -> Color {
        switch status {
        case .approved, .completed:
            return HASETTheme.greenPrimary
        case .pending:
            return .orange
        case .cancelled:
            return HASETTheme.redPrimary
        case .declined:
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
            otherUserId: otherUserId,
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
                                    ProfileAvatarView(
                                        imageSource: conversation.profileImage ?? "",
                                        initials: String(conversation.name.prefix(1)).uppercased(),
                                        size: 48,
                                        fontSize: 18
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
    @State private var selectedMessageIDs: Set<String> = []
    @State private var isSelectingMessages = false
    @State private var attachmentItem: PhotosPickerItem?
    @State private var videoPickerItem: PhotosPickerItem?
    @State private var showAttachmentSheet = false
    @State private var showCameraPicker = false
    @State private var showDocumentPicker = false
    @State private var chatErrorMessage: String?
    @State private var isPreparingAttachment = false
    @State private var previewFileURL: URL?
    @State private var showPrescriptionSheet = false
    @State private var selectedChatPrescription: PrescriptionSummary?
    @State private var showServiceRequestSheet = false
    @State private var serviceStatuses: [String: String] = [:]
    @State private var pendingServicePayment: PendingServicePayment?
    @State private var recorder: AVAudioRecorder?
    @State private var isRecording = false
    @State private var recordingSeconds = 0
    @State private var recordingTimer: Timer?
    @State private var audioPlayer: AVAudioPlayer?
    @State private var replyMessage: ChatMessageSummary?
    @State private var chatSendAccess = ChatSendAccessSummary(canSend: false, message: "Checking chat access...")

    private var currentUserId: String {
        appViewModel.activeSession?.userId ?? appViewModel.currentUser?.userId ?? ""
    }

    private var otherUserId: String {
        conversation.otherUserId
    }

    private var chatRoomId: String {
        conversation.id
    }

    private var otherUserName: String {
        conversation.name
    }

    var body: some View {
        VStack(spacing: 0) {
            if isSelectingMessages {
                HStack {
                    Button("Cancel") { selectedMessageIDs.removeAll(); isSelectingMessages = false }
                    Spacer()
                    Text("\(selectedMessageIDs.count) selected")
                    Button(role: .destructive) {
                        let ids = selectedMessageIDs
                        Task {
                            for id in ids { try? await appViewModel.deleteChatMessage(chatRoomId: chatRoomId, messageId: id) }
                            selectedMessageIDs.removeAll(); isSelectingMessages = false
                            await loadMessages()
                        }
                    } label: { Image(systemName: "trash") }
                    .disabled(selectedMessageIDs.isEmpty)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
                .background(Color.white)
            }
            messagesScroll()

            if let accessMessage = chatSendAccess.message, !chatSendAccess.canSend {
                chatAccessBanner(accessMessage)
            }

            chatInputBar()
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(conversation.name)
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await loadMessages()
            await refreshChatSendAccess()
            await appViewModel.markChatMessagesRead(chatRoomId: chatRoomId)
            await appViewModel.loadConversations(force: true)
        }
        .task(id: pendingServiceIds) {
            // Mirrors Android's persistent ValueEventListener per pending
            // service card: Firebase's REST streaming endpoint pushes status
            // the moment the backend flips the request to paid. When a card
            // becomes paid it leaves `pendingServiceIds` and the task restarts.
            guard !pendingServiceIds.isEmpty else { return }
            let ids = pendingServiceIds
            let session = appViewModel.activeSession ?? SessionStore().loadSession()
            let stream = AuthService().servicePaymentStatusUpdates(serviceIds: ids, idToken: session?.idToken)
            for await update in stream {
                if Task.isCancelled { return }
                serviceStatuses[update.serviceId] = update.status
            }
        }
        .onChange(of: attachmentItem) { item in
            guard let item else { return }
            showAttachmentSheet = false
            Task { await handlePickedImage(item) }
        }
        .onChange(of: videoPickerItem) { item in
            guard let item else { return }
            showAttachmentSheet = false
            Task { await handlePickedVideo(item) }
        }
        .sheet(isPresented: $showCameraPicker) {
            CameraChatPicker { image in
                showCameraPicker = false
                Task { await handleCameraImage(image) }
            } onCancel: {
                showCameraPicker = false
            }
        }
        .fileImporter(
            isPresented: $showDocumentPicker,
            allowedContentTypes: ChatAttachmentProcessor.documentTypes,
            allowsMultipleSelection: false
        ) { result in
            guard case .success(let urls) = result, let url = urls.first else { return }
            Task { await handlePickedDocument(url) }
        }
        .sheet(isPresented: Binding(
            get: { previewFileURL != nil },
            set: { if !$0 { previewFileURL = nil } }
        )) {
            if let previewFileURL {
                QuickLookChatPreview(fileURL: previewFileURL)
            }
        }
        .sheet(isPresented: $showPrescriptionSheet) {
            ChatPrescriptionSheet(
                chatRoomId: chatRoomId,
                patientId: otherUserId,
                patientName: otherUserName,
                onSent: { await loadMessages() }
            )
            .environmentObject(appViewModel)
        }
        .sheet(item: $selectedChatPrescription) { item in
            PrescriptionDetailView(prescription: item, onDeleted: { selectedChatPrescription = nil })
                .environmentObject(appViewModel)
        }
        .sheet(isPresented: $showServiceRequestSheet) {
            ChatServiceRequestSheet(
                chatRoomId: chatRoomId,
                doctorId: currentUserId,
                doctorName: appViewModel.currentUser?.fullName ?? "",
                patientId: otherUserId,
                patientName: otherUserName,
                onSent: { await loadMessages() }
            )
            .environmentObject(appViewModel)
        }
        .sheet(item: $pendingServicePayment) { pending in
            PaymentCheckoutView(
                doctor: pending.checkoutDoctor,
                amount: pending.payload.patientPayAmount ?? 0,
                initialMethod: .mobileMoney,
                consultationIdOverride: "service-\(pending.messageId)",
                onPaymentConfirmed: { transactionId in
                    Task {
                        await completeServicePayment(pending: pending, transactionId: transactionId)
                    }
                }
            )
            .environmentObject(appViewModel)
        }
        .alert(appViewModel.tr("error"), isPresented: Binding(
            get: { chatErrorMessage != nil },
            set: { if !$0 { chatErrorMessage = nil } }
        )) { Button("OK", role: .cancel) {} } message: {
            Text(chatErrorMessage ?? "")
        }
        .screenshotProtected()
    }

    private func startRecording() {
        guard chatSendAccess.canSend else { return }
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            guard granted else { return }
            DispatchQueue.main.async {
                let url = FileManager.default.temporaryDirectory.appendingPathComponent("voice-\(UUID().uuidString).m4a")
                let settings: [String: Any] = [AVFormatIDKey: Int(kAudioFormatMPEG4AAC), AVSampleRateKey: 12_000, AVNumberOfChannelsKey: 1, AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue]
                do {
                    let session = AVAudioSession.sharedInstance()
                    try session.setCategory(.record, mode: .spokenAudio)
                    try session.setActive(true)
                    let recorder = try AVAudioRecorder(url: url, settings: settings)
                    recorder.record(); self.recorder = recorder; isRecording = true; recordingSeconds = 0
                    recordingTimer?.invalidate()
                    recordingTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in recordingSeconds += 1 }
                } catch { chatErrorMessage = appViewModel.tr("attachment_failed") }
            }
        }
    }

    private func stopRecording() {
        guard let recorder else { return }
        recorder.stop(); self.recorder = nil; isRecording = false
        recordingTimer?.invalidate(); recordingTimer = nil
        let url = recorder.url
        Task {
            do {
                let data = try Data(contentsOf: url)
                let remoteURL = try await appViewModel.uploadChatAttachment(data, fileName: url.lastPathComponent, mimeType: "audio/mp4")
                try await appViewModel.sendChatMessage(chatRoomId: chatRoomId, receiverId: otherUserId, receiverName: otherUserName, message: "Voice message", messageType: "audio", attachmentURL: remoteURL, attachmentFileName: url.lastPathComponent)
                await loadMessages()
            } catch { chatErrorMessage = appViewModel.tr("attachment_failed") }
            try? FileManager.default.removeItem(at: url)
        }
    }

    private func loadMessages() async {
        guard !currentUserId.isEmpty else { return }
        messages = await appViewModel.loadChatMessages(chatRoomId: chatRoomId, currentUserId: currentUserId)
        await refreshServicePaymentStatuses()
    }

    private var pendingServiceIds: [String] {
        messages
            .filter { $0.messageType == "service" }
            .compactMap { message -> String? in
                guard let payload = ChatServicePayload(json: message.message),
                      let serviceId = payload.serviceId, !serviceId.isEmpty else { return nil }
                return (serviceStatuses[serviceId] ?? payload.paymentStatus ?? "").lowercased() == "paid" ? nil : serviceId
            }
            .sorted()
    }

    private func refreshServicePaymentStatuses() async {
        let session = appViewModel.activeSession ?? SessionStore().loadSession()
        let serviceMessages = messages.filter { $0.messageType == "service" }
        for message in serviceMessages {
            guard let payload = ChatServicePayload(json: message.message),
                  let serviceId = payload.serviceId, !serviceId.isEmpty else { continue }
            if let status = await AuthService().fetchServicePaymentStatus(
                serviceId: serviceId,
                idToken: session?.idToken
            ) {
                serviceStatuses[serviceId] = status
            }
        }
    }

    private func completeServicePayment(pending: PendingServicePayment, transactionId: Int?) async {
        guard let serviceId = pending.payload.serviceId else { return }
        let session = appViewModel.activeSession ?? SessionStore().loadSession()
        do {
            try await AuthService().updateServicePaymentStatus(
                serviceId: serviceId,
                status: "paid",
                transactionId: transactionId,
                idToken: session?.idToken
            )
            serviceStatuses[serviceId] = "paid"
            await loadMessages()
        } catch {
            chatErrorMessage = error.localizedDescription
        }
    }

    @ViewBuilder
    private func chatInputBar() -> some View {
        HStack(spacing: 10) {
            Button {
                showAttachmentSheet = true
            } label: {
                Image(systemName: "paperclip")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(chatSendAccess.canSend ? HASETTheme.greenPrimary : HASETTheme.textSecondary.opacity(0.45))
            }
            .disabled(!chatSendAccess.canSend || isPreparingAttachment)
            .accessibilityLabel(appViewModel.tr("attach_file"))
            .sheet(isPresented: $showAttachmentSheet) {
                ChatAttachmentSheet(
                    role: role,
                    onCamera: {
                        showAttachmentSheet = false
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                            showCameraPicker = true
                        }
                    },
                    onDocument: {
                        showAttachmentSheet = false
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                            showDocumentPicker = true
                        }
                    },
                    imageSelection: $attachmentItem,
                    videoSelection: $videoPickerItem,
                    onPrescription: {
                        showAttachmentSheet = false
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                            showPrescriptionSheet = true
                        }
                    },
                    onRequestService: {
                        showAttachmentSheet = false
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                            showServiceRequestSheet = true
                        }
                    }
                )
                .environmentObject(appViewModel)
            }
            Button {
                if isRecording { stopRecording() } else { startRecording() }
            } label: {
                if isRecording {
                    Text(String(format: "%02d:%02d", recordingSeconds / 60, recordingSeconds % 60))
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.red)
                } else {
                    Image(systemName: "mic.fill")
                        .font(.system(size: 19, weight: .semibold))
                        .foregroundStyle(chatSendAccess.canSend ? HASETTheme.greenPrimary : HASETTheme.textSecondary.opacity(0.45))
                }
            }
            .disabled(!chatSendAccess.canSend)
            TextField("Type a message", text: $draftMessage, axis: .vertical)
                .disabled(!chatSendAccess.canSend)
                .lineLimit(1...4)
                .font(HASETTheme.font(.regular, 15))
                .padding(.horizontal, 16)
                .padding(.vertical, 11)
                .background(
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .fill(HASETTheme.backgroundPrimary)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .stroke(HASETTheme.divider, lineWidth: 1)
                )
            Button {
                sendMessage()
            } label: {
                Image(systemName: "paperplane.fill")
                    .foregroundStyle(.white)
                    .font(.system(size: 17, weight: .semibold))
                    .frame(width: 48, height: 48)
                    .background(Circle().fill(HASETTheme.greenPrimary))
                    .shadow(color: HASETTheme.greenPrimary.opacity(0.22), radius: 5, y: 3)
            }
            .disabled(!chatSendAccess.canSend || draftMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSending)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.white)
    }

    @ViewBuilder
    private func messagesScroll() -> some View {
        ScrollView {
            LazyVStack(spacing: 10) {
                ForEach(messages) { message in
                    let bodyView = AnyView(messageBody(message))
                    let statusView = AnyView(messageStatus(message))
                    HStack {
                        if message.isOutgoing { Spacer(minLength: 32) }
                        HStack(alignment: .bottom, spacing: 8) {
                            bodyView

                            if message.isOutgoing { statusView }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .fill(message.isOutgoing ? HASETTheme.greenPrimary : Color.white)
                        )
                        .frame(maxWidth: 280, alignment: message.isOutgoing ? .trailing : .leading)
                        .contextMenu { messageContextMenu(message) }
                        if !message.isOutgoing { Spacer(minLength: 32) }
                    }
                }
            }
            .padding(20)
        }
    }

    @ViewBuilder
    private func chatAccessBanner(_ text: String) -> some View {
        Text(text)
            .font(HASETTheme.font(.medium, 12))
            .foregroundStyle(.orange)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(Color.orange.opacity(0.09))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(Color.orange.opacity(0.35), lineWidth: 1)
            )
            .padding(.horizontal, 16)
            .padding(.top, 8)
    }

    private func refreshChatSendAccess() async {
        guard !currentUserId.isEmpty else { return }
        chatSendAccess = await appViewModel.loadChatSendAccess(
            chatRoomId: chatRoomId,
            otherUserId: otherUserId,
            role: role
        )
    }

    private func toggleSelection(_ id: String) {
        if selectedMessageIDs.contains(id) { selectedMessageIDs.remove(id) }
        else { selectedMessageIDs.insert(id) }
    }

    private func handlePickedImage(_ item: PhotosPickerItem) async {
        attachmentItem = nil
        guard chatSendAccess.canSend else { return }
        guard let data = try? await item.loadTransferable(type: Data.self) else {
            chatErrorMessage = appViewModel.tr("attachment_failed")
            return
        }
        guard let prepared = ChatAttachmentProcessor.imageData(data) else {
            chatErrorMessage = appViewModel.tr("image_too_large")
            return
        }
        await uploadAndSend(
            data: prepared,
            fileName: "image-\(UUID().uuidString).jpg",
            mimeType: "image/jpeg",
            label: "Image",
            messageType: "image"
        )
    }

    private func handleCameraImage(_ image: UIImage) async {
        guard chatSendAccess.canSend else { return }
        guard let data = ChatAttachmentProcessor.jpegData(from: image) else {
            chatErrorMessage = appViewModel.tr("attachment_failed")
            return
        }
        await uploadAndSend(
            data: data,
            fileName: "image-\(UUID().uuidString).jpg",
            mimeType: "image/jpeg",
            label: "Image",
            messageType: "image"
        )
    }

    private func handlePickedVideo(_ item: PhotosPickerItem) async {
        videoPickerItem = nil
        guard chatSendAccess.canSend else { return }
        guard let picked = try? await item.loadTransferable(type: PickedChatVideo.self) else {
            chatErrorMessage = appViewModel.tr("attachment_failed")
            return
        }
        await sendVideoFile(at: picked.url)
    }

    private func handlePickedDocument(_ url: URL) async {
        guard chatSendAccess.canSend else { return }
        let accessing = url.startAccessingSecurityScopedResource()
        defer { if accessing { url.stopAccessingSecurityScopedResource() } }
        guard let data = try? Data(contentsOf: url) else {
            chatErrorMessage = appViewModel.tr("attachment_failed")
            return
        }
        guard data.count <= ChatAttachmentProcessor.maxDocumentBytes else {
            chatErrorMessage = appViewModel.tr("document_too_large")
            return
        }
        let name = url.lastPathComponent
        await uploadAndSend(
            data: data,
            fileName: name,
            mimeType: ChatAttachmentProcessor.mimeType(for: name),
            label: "Document",
            messageType: "document"
        )
    }

    private func sendVideoFile(at url: URL) async {
        do {
            let data = try await ChatAttachmentProcessor.videoData(url: url)
            await uploadAndSend(
                data: data,
                fileName: "video-\(UUID().uuidString).mp4",
                mimeType: "video/mp4",
                label: "Video",
                messageType: "video"
            )
        } catch ChatAttachmentProcessor.AttachmentError.tooLarge {
            chatErrorMessage = appViewModel.tr("video_too_large")
        } catch {
            chatErrorMessage = appViewModel.tr("attachment_failed")
        }
    }

    private func uploadAndSend(
        data: Data,
        fileName: String,
        mimeType: String,
        label: String,
        messageType: String
    ) async {
        guard chatSendAccess.canSend else { return }
        isPreparingAttachment = true
        defer { isPreparingAttachment = false }
        do {
            let remote = try await appViewModel.uploadChatAttachment(
                data,
                fileName: fileName,
                mimeType: mimeType
            )
            try await appViewModel.sendChatMessage(
                chatRoomId: chatRoomId,
                receiverId: otherUserId,
                receiverName: otherUserName,
                message: label,
                messageType: messageType,
                attachmentURL: remote,
                attachmentFileName: fileName,
                attachmentSize: ChatAttachmentProcessor.formattedSize(data.count)
            )
            await loadMessages()
        } catch {
            chatErrorMessage = appViewModel.tr("attachment_failed")
        }
    }

    @ViewBuilder
    private func serviceMessageCard(message: ChatMessageSummary, payload: ChatServicePayload) -> some View {
        let status = payload.serviceId.flatMap { serviceStatuses[$0] } ?? payload.paymentStatus
        let paid = status == "paid" || payload.isPaid == true
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "creditcard.fill")
                    .font(.system(size: 16))
                    .foregroundStyle(message.isOutgoing ? .white : HASETTheme.greenPrimary)
                Text(payload.serviceName ?? appViewModel.tr("service"))
                    .font(HASETTheme.font(.medium, 15))
                    .lineLimit(2)
            }
            VStack(alignment: .leading, spacing: 3) {
                HStack {
                    Text(appViewModel.tr("total_fee"))
                        .font(HASETTheme.font(.regular, 13))
                    Spacer()
                    Text(String(format: "%,.0f TZS", payload.appointmentFee ?? 0))
                        .font(HASETTheme.font(.medium, 13))
                }
                HStack {
                    Text("\(appViewModel.tr("you_pay")) (\(payload.patientPercentage ?? 0)%):")
                        .font(HASETTheme.font(.regular, 13))
                    Spacer()
                    Text(String(format: "%,.0f TZS", payload.patientPayAmount ?? 0))
                        .font(HASETTheme.font(.medium, 13))
                        .foregroundStyle(HASETTheme.greenPrimary)
                }
            }
            HStack {
                Text(paid ? appViewModel.tr("payment_complete") : appViewModel.tr("payment_pending"))
                    .font(HASETTheme.font(.medium, 12))
                    .foregroundStyle(paid ? HASETTheme.greenPrimary : .orange)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(
                        Capsule().fill((paid ? HASETTheme.greenPrimary : Color.orange).opacity(0.12))
                    )
                Spacer()
                if paid {
                    Text(appViewModel.tr("paid"))
                        .font(HASETTheme.font(.medium, 12))
                        .foregroundStyle(HASETTheme.textSecondary)
                } else if !message.isOutgoing {
                    Button {
                        startServicePayment(message: message, payload: payload)
                    } label: {
                        Text(appViewModel.tr("pay_now"))
                            .font(HASETTheme.font(.medium, 13))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 7)
                            .background(Capsule().fill(HASETTheme.greenPrimary))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .frame(maxWidth: 250, alignment: .leading)
    }

    private func startServicePayment(message: ChatMessageSummary, payload: ChatServicePayload) {
        guard (payload.patientPayAmount ?? 0) > 0 else {
            chatErrorMessage = appViewModel.tr("error")
            return
        }
        let doctor = DoctorSummary(
            id: message.senderId,
            name: otherUserName,
            specialty: "",
            hospital: "",
            phoneNumber: nil,
            email: nil,
            address: nil,
            bio: nil,
            rating: 0,
            experienceYears: nil,
            verified: false,
            isDemo: false,
            consultationFee: "",
            availableToday: false,
            profileImage: nil,
            availableTimes: nil
        )
        pendingServicePayment = PendingServicePayment(
            messageId: message.id,
            payload: payload,
            checkoutDoctor: doctor
        )
    }

    private func openChatPrescription(_ message: ChatMessageSummary) {
        guard let prescriptionId = message.prescriptionId, !prescriptionId.isEmpty else {
            chatErrorMessage = appViewModel.tr("prescription_not_found")
            return
        }
        Task {
            let session = appViewModel.activeSession ?? SessionStore().loadSession()
            do {
                selectedChatPrescription = try await AuthService().fetchPrescription(
                    prescriptionId: prescriptionId,
                    idToken: session?.idToken
                )
            } catch {
                chatErrorMessage = error.localizedDescription
            }
        }
    }

    private func previewAttachment(_ message: ChatMessageSummary) async {
        guard let value = message.attachmentURL, let remote = URL(string: value) else { return }
        do {
            let (data, _) = try await URLSession.shared.data(from: remote)
            let fallbackName = message.messageType == "video" ? "video.mp4" : "document"
            let name = message.attachmentFileName ?? fallbackName
            let local = FileManager.default.temporaryDirectory
                .appendingPathComponent("\(UUID().uuidString)-\(name)")
            try data.write(to: local)
            previewFileURL = local
        } catch {
            chatErrorMessage = appViewModel.tr("attachment_failed")
        }
    }

    @ViewBuilder private func messageContextMenu(_ message: ChatMessageSummary) -> some View {
        Button { isSelectingMessages = true; selectedMessageIDs.insert(message.id) } label: { Text("Select") }
        Button { UIPasteboard.general.string = message.message } label: { Text("Copy") }
        Button { replyMessage = message; draftMessage = "" } label: { Text("Reply") }
        if message.isOutgoing {
            Button(role: .destructive) {
                Task { try? await appViewModel.deleteChatMessage(chatRoomId: chatRoomId, messageId: message.id); await loadMessages() }
            } label: { Text("Delete") }
        }
    }

    @ViewBuilder private func messageBody(_ message: ChatMessageSummary) -> some View {
        if message.messageType == "image", let value = message.attachmentURL, let url = URL(string: value) {
            Button {
                Task { await previewAttachment(message) }
            } label: {
                AsyncImage(url: url) { image in image.resizable().scaledToFit() } placeholder: { ProgressView() }
                    .frame(maxWidth: 180, maxHeight: 180)
            }
            .buttonStyle(.plain)
        } else if message.messageType == "prescription" {
            Button {
                openChatPrescription(message)
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: "cross.case.fill")
                        .font(.system(size: 22))
                        .foregroundStyle(message.isOutgoing ? .white : HASETTheme.greenPrimary)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(message.message)
                            .font(HASETTheme.font(.medium, 14))
                        Text(appViewModel.tr("view_prescription"))
                            .font(HASETTheme.font(.regular, 12))
                            .opacity(0.8)
                    }
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .opacity(0.7)
                }
                .foregroundStyle(message.isOutgoing ? .white : HASETTheme.textPrimary)
                .frame(maxWidth: 240, alignment: .leading)
            }
            .buttonStyle(.plain)
        } else if message.messageType == "audio", let value = message.attachmentURL, let url = URL(string: value) {
            Button { playVoice(url) } label: { Label("Play voice message", systemImage: "play.circle.fill") }
                .foregroundStyle(message.isOutgoing ? .white : HASETTheme.greenPrimary)
        } else if message.messageType == "service", let payload = ChatServicePayload(json: message.message) {
            serviceMessageCard(message: message, payload: payload)
        } else if message.messageType == "document" || message.messageType == "video" {
            Button {
                Task { await previewAttachment(message) }
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: message.messageType == "video" ? "film.fill" : "doc.fill")
                        .font(.system(size: 22))
                    VStack(alignment: .leading, spacing: 3) {
                        Text(message.attachmentFileName ?? message.message)
                            .font(HASETTheme.font(.medium, 14))
                            .lineLimit(1)
                        if let size = message.attachmentSize {
                            Text(size)
                                .font(HASETTheme.font(.regular, 12))
                                .opacity(0.8)
                        }
                    }
                }
                .foregroundStyle(message.isOutgoing ? .white : HASETTheme.textPrimary)
                .frame(maxWidth: 220, alignment: .leading)
            }
            .buttonStyle(.plain)
        } else {
            Text(message.message)
                .font(HASETTheme.font(.regular, 15))
                .foregroundStyle(message.isOutgoing ? .white : HASETTheme.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private func messageStatus(_ message: ChatMessageSummary) -> some View {
        let color: Color = message.isRead ? .blue : Color.gray.opacity(0.75)
        return ZStack(alignment: .trailing) {
            Image(systemName: "checkmark").font(.system(size: 10, weight: .semibold)).foregroundStyle(color).offset(x: -3)
            Image(systemName: "checkmark").font(.system(size: 10, weight: .semibold)).foregroundStyle(color).offset(x: 2)
        }
        .frame(width: 12, height: 12)
        .padding(.leading, 4)
    }

    private func playVoice(_ url: URL) {
        Task {
            do {
                let (data, _) = try await URLSession.shared.data(from: url)
                let player = try AVAudioPlayer(data: data)
                await MainActor.run { audioPlayer = player; player.play() }
            } catch { chatErrorMessage = appViewModel.tr("attachment_failed") }
        }
    }

    private func sendMessage() {
        guard chatSendAccess.canSend else { return }
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
                    message: text,
                    messageType: "text",
                    replyToMessageID: replyMessage?.id,
                    replyToText: replyMessage?.message
                )
                await loadMessages()
                await appViewModel.markChatMessagesRead(chatRoomId: chatRoomId)
                await appViewModel.loadConversations(force: true)
                replyMessage = nil
            } catch {
                draftMessage = text
            }
        }
    }
}

struct ProfileScreen: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var showDeleteConfirmation = false
    @State private var profileSheet: ProfileSheetItem?

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
                        ProfileAvatarWithVerifiedBadge(
                            imageSource: user.profileImage,
                            initials: profileInitials(from: user.fullName),
                            size: 100,
                            fontSize: 30,
                            showVerified: user.isAdminVerifiedDoctor
                        )

                        Text(displayName(for: user, fallback: user.fullName))
                            .font(HASETTheme.font(.medium, 20))
                            .foregroundStyle(HASETTheme.textPrimary)

                        if user.role == .doctor, !user.isAdminVerifiedDoctor {
                            Text(appViewModel.tr("doctor_pending_admin_badge"))
                                .font(HASETTheme.font(.medium, 12))
                                .foregroundStyle(Color.orange)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(
                                    Capsule()
                                        .fill(Color.orange.opacity(0.12))
                                )
                        }

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

                sectionHeader(appViewModel.tr("general"))
                CardContainer {
                    VStack(spacing: 0) {
                        if let user = appViewModel.currentUser, user.role == .patient || user.role == .doctor {
                            NavigationLink {
                                PrescriptionsListView(role: user.role)
                                    .environmentObject(appViewModel)
                            } label: {
                                ProfileOptionRow(
                                    icon: "cross.case",
                                    title: appViewModel.tr("my_prescriptions"),
                                    subtitle: appViewModel.tr("my_prescriptions_desc")
                                )
                            }
                            .buttonStyle(.plain)
                            rowDivider()
                        }
                            NavigationLink { InAppWebContentView(title: appViewModel.tr("terms_of_use"), url: URL(string: HASETConstants.termsURL)!) } label: {
                                ProfileOptionRow(icon: "doc.text", title: appViewModel.tr("terms_of_use"), subtitle: appViewModel.tr("terms_of_use_desc"))
                            }
                        .buttonStyle(.plain)
                        rowDivider()
                            NavigationLink { InAppWebContentView(title: appViewModel.tr("about"), url: URL(string: HASETConstants.supportURL)!) } label: {
                                ProfileOptionRow(icon: "info.circle", title: appViewModel.tr("about"), subtitle: appViewModel.tr("about_desc"))
                            }
                        .buttonStyle(.plain)
                        rowDivider()
                        Button { profileSheet = .rating } label: {
                            ProfileOptionRow(icon: "hand.thumbsup", title: appViewModel.tr("rate_app"), subtitle: appViewModel.tr("rate_app_desc"))
                        }
                        .buttonStyle(.plain)
                        rowDivider()
                        Button { profileSheet = .support } label: {
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
                                ProfileValueRow(icon: "dollarsign.circle", title: appViewModel.tr("consultation_fee"), value: user.consultationFee ?? appViewModel.tr("not_set"))
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
                                    value: appViewModel.locationEnabled ? appViewModel.tr("location_enabled") : appViewModel.tr("location_disabled")
                                )
                            }
                            .buttonStyle(.plain)
                            rowDivider()
                            NavigationLink { EditProfileView() } label: {
                                ProfileOptionRow(
                                    icon: "pencil",
                                    title: appViewModel.tr("edit_professional_info"),
                                    subtitle: appViewModel.tr("edit_professional_info_desc")
                                )
                            }
                            .buttonStyle(.plain)
                            rowDivider()
                            NavigationLink { DoctorPolicyView() } label: {
                                ProfileOptionRow(
                                    icon: "checkmark.shield",
                                    title: appViewModel.tr("doctor_policy"),
                                    subtitle: appViewModel.tr("doctor_policy_desc")
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }

                }

                sectionHeader(appViewModel.tr("preferences"))
                CardContainer {
                    VStack(spacing: 0) {
                        Button { showDeleteConfirmation = true } label: {
                            ProfileOptionRow(
                                icon: "trash.fill",
                                title: appViewModel.tr("delete_account"),
                                subtitle: appViewModel.tr("permanently_delete_your_account"),
                                tint: .red
                            )
                        }
                        .buttonStyle(.plain)
                        rowDivider()
                        Button { appViewModel.logout() } label: {
                            ProfileOptionRow(
                                icon: "rectangle.portrait.and.arrow.right.fill",
                                title: appViewModel.tr("logout"),
                                subtitle: appViewModel.tr("securely_log_out_of_account"),
                                tint: .red
                            )
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
        .confirmationDialog(appViewModel.tr("delete_account_confirm_title"), isPresented: $showDeleteConfirmation, titleVisibility: .visible) {
            Button(appViewModel.tr("delete_permanently"), role: .destructive) { appViewModel.deleteAccount() }
            Button(appViewModel.tr("cancel"), role: .cancel) {}
        } message: {
            Text(appViewModel.tr("delete_account_confirm_message"))
        }
        .sheet(item: $profileSheet) { item in
            switch item {
            case .rating:
                RateAppView()
                    .environmentObject(appViewModel)
            case .support:
                SupportSheet()
                    .environmentObject(appViewModel)
            }
        }
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

private enum ProfileSheetItem: Identifiable {
    case rating
    case support

    var id: String {
        switch self {
        case .rating: return "rating"
        case .support: return "support"
        }
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
    var tint: Color = HASETTheme.greenPrimary

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(tint)
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

private struct DoctorVerifiedBadge: View {
    var body: some View {
        ZStack {
            Circle()
                .fill(HASETTheme.greenPrimary)
            Circle()
                .stroke(Color.white, lineWidth: 2)
            Image(systemName: "checkmark")
                .font(.system(size: 9, weight: .bold))
                .foregroundStyle(.white)
        }
        .frame(width: 22, height: 22)
        .shadow(color: Color.black.opacity(0.12), radius: 2, x: 0, y: 1)
    }
}

private struct ProfileAvatarWithVerifiedBadge: View {
    let imageSource: String
    let initials: String
    let size: CGFloat
    let fontSize: CGFloat
    let showVerified: Bool

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            ProfileAvatarView(
                imageSource: imageSource,
                initials: initials,
                size: size,
                fontSize: fontSize
            )
            if showVerified {
                DoctorVerifiedBadge()
                    .offset(x: 3, y: 3)
            }
        }
    }
}

private struct ProfileAvatarView: View {
    let imageSource: String
    let initials: String
    let size: CGFloat
    let fontSize: CGFloat

    private var normalizedImageSource: String {
        imageSource.trimmingCharacters(in: .whitespacesAndNewlines)
    }

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
        guard !normalizedImageSource.isEmpty else { return nil }
        if let data = Data(base64Encoded: normalizedImageSource), let image = UIImage(data: data) {
            return image
        }
        return nil
    }

    private var isRemoteURL: Bool {
        normalizedImageSource.lowercased().hasPrefix("http://") || normalizedImageSource.lowercased().hasPrefix("https://")
    }

    private var remoteURL: URL? {
        URL(string: normalizedImageSource)
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

struct GlobalSearchView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var query = ""
    @State private var recentSearches: [String] = SearchHistoryStore.load()
    @State private var articles: [ArticleSummary] = []
    @State private var loading = false
    @State private var loadedOnce = false
    @State private var loadFailed = false

    private var trimmedQuery: String {
        query.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var matchedDoctors: [DoctorSummary] {
        let needle = trimmedQuery.lowercased()
        guard !needle.isEmpty else { return [] }
        return appViewModel.doctors.filter {
            $0.name.lowercased().contains(needle)
                || $0.specialty.lowercased().contains(needle)
                || $0.hospital.lowercased().contains(needle)
        }
    }

    private var matchedArticles: [ArticleSummary] {
        let needle = trimmedQuery.lowercased()
        guard !needle.isEmpty else { return [] }
        return articles.filter {
            $0.title.lowercased().contains(needle)
                || $0.excerpt.lowercased().contains(needle)
                || $0.category.lowercased().contains(needle)
        }
    }

    var body: some View {
        Group {
            if trimmedQuery.isEmpty {
                idleContent
            } else {
                resultsContent
            }
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("search"))
        .navigationBarTitleDisplayMode(.inline)
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: appViewModel.tr("search_doctors_articles"))
        .onSubmit(of: .search) { commitSearch() }
        .task { await loadIfNeeded() }
    }

    private var idleContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                SectionTitle(appViewModel.tr("browse_by_category"))
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 4), spacing: 14) {
                    PatientServiceShortcut(title: appViewModel.tr("popular_doctors"), systemImage: "stethoscope", destination: AnyView(DoctorsCatalogView()))
                    PatientServiceShortcut(title: appViewModel.tr("articles"), systemImage: "newspaper", destination: AnyView(ArticlesView(role: .patient)))
                    PatientServiceShortcut(title: appViewModel.tr("find_hospital"), systemImage: "cross.case", destination: AnyView(HospitalListView()))
                    PatientServiceShortcut(title: appViewModel.tr("prescriptions"), systemImage: "cross.case", destination: AnyView(PrescriptionsListView(role: .patient)))
                }

                if !recentSearches.isEmpty {
                    HStack {
                        SectionTitle(appViewModel.tr("recent_searches"))
                        Spacer()
                        Button(appViewModel.tr("clear")) { clearRecentSearches() }
                            .font(HASETTheme.font(.medium, 12))
                            .foregroundStyle(HASETTheme.redPrimary)
                            .accessibilityLabel(appViewModel.tr("clear_recent_searches"))
                    }
                    VStack(spacing: 0) {
                        ForEach(Array(recentSearches.enumerated()), id: \.offset) { index, term in
                            Button {
                                query = term
                                commitSearch()
                            } label: {
                                HStack(spacing: 10) {
                                    Image(systemName: "clock.arrow.circlepath")
                                        .foregroundStyle(HASETTheme.textSecondary)
                                    Text(term)
                                        .font(HASETTheme.font(.regular, 14))
                                        .foregroundStyle(HASETTheme.textPrimary)
                                    Spacer()
                                    Image(systemName: "arrow.up.left")
                                        .font(.system(size: 11))
                                        .foregroundStyle(HASETTheme.textSecondary)
                                }
                                .padding(.vertical, 12)
                                .padding(.horizontal, 4)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("\(appViewModel.tr("recent_searches")): \(term)")
                            if index < recentSearches.count - 1 {
                                Divider().overlay(HASETTheme.divider)
                            }
                        }
                    }
                }
            }
            .padding(20)
        }
    }

    private var resultsContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                if loading {
                    HStack {
                        Spacer()
                        ProgressView().tint(HASETTheme.greenPrimary)
                        Spacer()
                    }
                    .padding(.top, 30)
                }

                if !matchedDoctors.isEmpty {
                    SectionTitle("\(appViewModel.tr("doctors")) (\(matchedDoctors.count))")
                    VStack(spacing: 0) {
                        ForEach(Array(matchedDoctors.enumerated()), id: \.element.id) { index, doctor in
                            NavigationLink {
                                DoctorDetailView(doctor: doctor)
                            } label: {
                                SearchResultRow(
                                    title: doctor.name,
                                    subtitle: doctor.specialty,
                                    imageSource: doctor.profileImage
                                )
                            }
                            .buttonStyle(.plain)
                            if index < matchedDoctors.count - 1 {
                                Divider().overlay(HASETTheme.divider).padding(.leading, 56)
                            }
                        }
                    }
                }

                if !matchedArticles.isEmpty {
                    SectionTitle("\(appViewModel.tr("articles")) (\(matchedArticles.count))")
                    VStack(spacing: 0) {
                        ForEach(Array(matchedArticles.enumerated()), id: \.element.id) { index, article in
                            NavigationLink {
                                ArticleDetailView(article: article)
                            } label: {
                                SearchResultRow(
                                    title: article.title,
                                    subtitle: article.category,
                                    imageSource: article.imageURL
                                )
                            }
                            .buttonStyle(.plain)
                            if index < matchedArticles.count - 1 {
                                Divider().overlay(HASETTheme.divider).padding(.leading, 56)
                            }
                        }
                    }
                }

                if !loading && matchedDoctors.isEmpty && matchedArticles.isEmpty {
                    CardContainer {
                        VStack(spacing: 8) {
                            Image(systemName: "magnifyingglass")
                                .font(.system(size: 26))
                                .foregroundStyle(HASETTheme.textSecondary)
                            Text(appViewModel.tr("no_results_for"))
                                .font(HASETTheme.font(.medium, 15))
                                .foregroundStyle(HASETTheme.textPrimary)
                                .multilineTextAlignment(.center)
                            Text(trimmedQuery)
                                .font(HASETTheme.font(.regular, 13))
                                .foregroundStyle(HASETTheme.textSecondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                    }
                }

                if loadFailed {
                    Button(appViewModel.tr("retry")) {
                        Task { await loadIfNeeded(force: true) }
                    }
                    .font(HASETTheme.font(.medium, 14))
                    .foregroundStyle(HASETTheme.greenPrimary)
                }
            }
            .padding(20)
        }
    }

    private func loadIfNeeded(force: Bool = false) async {
        guard !loadedOnce || force else { return }
        loading = true
        loadFailed = false
        if appViewModel.doctors.isEmpty {
            await appViewModel.loadDoctors(force: force)
        }
        do {
            articles = try await AuthService().fetchPopularArticles(idToken: appViewModel.currentIdToken())
            loadedOnce = true
        } catch {
            loadFailed = true
        }
        loading = false
    }

    private func commitSearch() {
        let term = trimmedQuery
        guard !term.isEmpty else { return }
        SearchHistoryStore.add(term)
        recentSearches = SearchHistoryStore.load()
    }

    private func clearRecentSearches() {
        SearchHistoryStore.clear()
        recentSearches = []
    }
}

private struct SearchResultRow: View {
    let title: String
    let subtitle: String
    let imageSource: String?

    var body: some View {
        HStack(spacing: 12) {
            ProfileAvatarView(
                imageSource: imageSource ?? "",
                initials: String(title.prefix(1)).uppercased(),
                size: 40,
                fontSize: 15
            )
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(HASETTheme.font(.medium, 15))
                    .foregroundStyle(HASETTheme.textPrimary)
                    .lineLimit(1)
                Text(subtitle)
                    .font(HASETTheme.font(.regular, 13))
                    .foregroundStyle(HASETTheme.textSecondary)
                    .lineLimit(1)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(HASETTheme.textSecondary)
        }
        .padding(.vertical, 10)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
    }
}

enum SearchHistoryStore {
    private static let key = "recent_searches"
    private static let limit = 8

    static func load() -> [String] {
        UserDefaults.standard.stringArray(forKey: key) ?? []
    }

    static func add(_ term: String) {
        let trimmed = term.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        var items = load().filter { $0.caseInsensitiveCompare(trimmed) != .orderedSame }
        items.insert(trimmed, at: 0)
        if items.count > limit { items = Array(items.prefix(limit)) }
        UserDefaults.standard.set(items, forKey: key)
    }

    static func clear() {
        UserDefaults.standard.removeObject(forKey: key)
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
    @State private var hospitals: [HospitalSummary] = []
    @State private var loading = true

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                if loading {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 24)
                } else if hospitals.isEmpty {
                    CardContainer {
                        Text(appViewModel.tr("no_hospitals_found"))
                            .font(HASETTheme.font(.medium, 15))
                            .foregroundStyle(HASETTheme.textSecondary)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, 12)
                    }
                } else {
                    ForEach(hospitals) { hospital in
                        Button {
                            openInMaps(hospital)
                        } label: {
                            CardContainer {
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(hospital.name)
                                        .font(HASETTheme.font(.medium, 16))
                                        .foregroundStyle(HASETTheme.textPrimary)
                                    if !hospital.location.isEmpty {
                                        Text(hospital.location)
                                            .font(HASETTheme.font(.regular, 13))
                                            .foregroundStyle(HASETTheme.textSecondary)
                                    }
                                    if let phone = hospital.phone, !phone.isEmpty {
                                        Text(phone)
                                            .font(HASETTheme.font(.regular, 12))
                                            .foregroundStyle(HASETTheme.greenPrimary)
                                    }
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("hospitals"))
        .task { await loadHospitals() }
        .refreshable { await loadHospitals() }
    }

    private func loadHospitals() async {
        loading = true
        defer { loading = false }
        let token = appViewModel.activeSession?.idToken ?? SessionStore().loadSession()?.idToken
        hospitals = (try? await AuthService().fetchHospitals(idToken: token)) ?? []
    }

    private func openInMaps(_ hospital: HospitalSummary) {
        let query = hospital.mapsQuery.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? hospital.name
        if let latitude = hospital.latitude, let longitude = hospital.longitude,
           let url = URL(string: "http://maps.apple.com/?ll=\(latitude),\(longitude)&q=\(query)") {
            UIApplication.shared.open(url)
            return
        }
        if let url = URL(string: "http://maps.apple.com/?q=\(query)") {
            UIApplication.shared.open(url)
        }
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

struct RateAppView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var selectedRating = 0

    private var ratingText: String {
        switch selectedRating {
        case 1: return appViewModel.tr("rating_poor")
        case 2: return appViewModel.tr("rating_fair")
        case 3: return appViewModel.tr("rating_good")
        case 4: return appViewModel.tr("rating_very_good")
        case 5: return appViewModel.tr("rating_excellent")
        default: return appViewModel.tr("tap_to_rate")
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Image(systemName: "star.circle.fill")
                    .font(.system(size: 54))
                    .foregroundStyle(HASETTheme.greenPrimary)
                    .accessibilityHidden(true)

                Text(appViewModel.tr("rate_app_title"))
                    .font(HASETTheme.font(.medium, 20))
                    .foregroundStyle(HASETTheme.textPrimary)
                    .multilineTextAlignment(.center)

                Text(ratingText)
                    .font(HASETTheme.font(.regular, 15))
                    .foregroundStyle(HASETTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .accessibilityAddTraits(.updatesFrequently)

                HStack(spacing: 14) {
                    ForEach(1...5, id: \.self) { star in
                        Button {
                            selectedRating = star
                        } label: {
                            Image(systemName: star <= selectedRating ? "star.fill" : "star")
                                .font(.system(size: 34))
                                .foregroundStyle(star <= selectedRating ? Color.yellow : HASETTheme.textSecondary)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(appViewModel.tr("star_content_desc") + " \(star)")
                        .accessibilityAddTraits(star <= selectedRating ? .isSelected : [])
                    }
                }

                Button {
                    submitRating()
                } label: {
                    Text(appViewModel.tr("rate_now"))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(selectedRating == 0)
                .accessibilityHint(appViewModel.tr("rate_now_hint"))

                Button(appViewModel.tr("not_now")) { dismiss() }
                    .font(HASETTheme.font(.medium, 15))
                    .foregroundStyle(HASETTheme.textSecondary)

                Spacer()
            }
            .padding(24)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(HASETTheme.backgroundPrimary)
            .navigationTitle(appViewModel.tr("rate_app"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(appViewModel.tr("cancel")) { dismiss() }
                }
            }
        }
    }

    private func submitRating() {
        let defaults = UserDefaults.standard
        defaults.set(true, forKey: "rating_requested")
        defaults.set(selectedRating, forKey: "last_submitted_rating")
        requestSystemReview()
        dismiss()
    }

    private func requestSystemReview() {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first
        if let scene {
            SKStoreReviewController.requestReview(in: scene)
        }
    }
}

struct NotificationsView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var selectedTab = 0

    private var appointmentNotifications: [AppointmentSummary] {
        let hidden = AppViewModel.notificationHiddenAppointmentIds()
        return appViewModel.appointments.filter {
            !hidden.contains($0.id) && $0.rawStatus.lowercased() != "read"
        }
    }

    private var appointmentBadgeCount: Int { appointmentNotifications.count }
    private var paymentBadgeCount: Int { appViewModel.notifications.filter { !$0.isRead }.count }

    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $selectedTab) {
                Text(appointmentTabTitle).tag(0)
                Text(paymentTabTitle).tag(1)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            ScrollView {
                LazyVStack(spacing: 0) {
                    if selectedTab == 0 {
                        appointmentTabContent
                    } else {
                        paymentTabContent
                    }
                }
                .padding(.bottom, 16)
            }
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("notifications"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button {
                        clearAppointments()
                    } label: {
                        Label(appViewModel.tr("clear_appointments"), systemImage: "calendar")
                    }
                    Button(role: .destructive) {
                        Task { await clearAll() }
                    } label: {
                        Label(appViewModel.tr("clear_all"), systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.system(size: 17, weight: .regular))
                        .foregroundStyle(HASETTheme.greenPrimary)
                }
            }
        }
        .task {
            await appViewModel.loadAppointments(force: true)
            await appViewModel.loadNotifications(force: true)
        }
    }

    private var appointmentTabTitle: String {
        badgeLabel(base: appViewModel.tr("appointment"), count: appointmentBadgeCount)
    }

    private var paymentTabTitle: String {
        badgeLabel(base: appViewModel.tr("info"), count: paymentBadgeCount)
    }

    private func badgeLabel(base: String, count: Int) -> String {
        count > 0 ? "\(base) (\(count))" : base
    }

    @ViewBuilder
    private var appointmentTabContent: some View {
        if appointmentNotifications.isEmpty {
            emptyState(appViewModel.tr("no_appointments"))
        } else {
            ForEach(Array(appointmentNotifications.enumerated()), id: \.element.id) { index, appointment in
                Button {
                    dismiss()
                    appViewModel.pendingNavigation = .tab(.appointments)
                } label: {
                    appointmentRow(appointment)
                }
                .buttonStyle(.plain)
                if index < appointmentNotifications.count - 1 {
                    Divider().padding(.leading, 76)
                }
            }
        }
    }

    @ViewBuilder
    private var paymentTabContent: some View {
        if appViewModel.notifications.isEmpty {
            emptyState(appViewModel.tr("no_notifications"))
        } else {
            ForEach(appViewModel.notifications) { item in
                paymentRow(item)
                if item.id != appViewModel.notifications.last?.id {
                    Divider().padding(.leading, 76)
                }
            }
        }
    }

    private func emptyState(_ text: String) -> some View {
        VStack(spacing: 10) {
            Image(systemName: "bell.badge")
                .font(.system(size: 30, weight: .regular))
                .foregroundStyle(HASETTheme.textSecondary.opacity(0.6))
                .padding(.top, 40)
            Text(text)
                .font(HASETTheme.font(.regular, 14))
                .foregroundStyle(HASETTheme.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 24)
    }

    private func appointmentRow(_ appointment: AppointmentSummary) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "calendar")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(HASETTheme.greenPrimary)
                .frame(width: 48, height: 48)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(HASETTheme.greenPrimary.opacity(0.12))
                )

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(String(format: appViewModel.tr("appointment_status_title"), appointment.status.localizedLabel(languageCode: appViewModel.selectedLanguage)))
                        .font(HASETTheme.font(.medium, 15))
                        .foregroundStyle(HASETTheme.textPrimary)
                        .lineLimit(1)
                    Spacer(minLength: 8)
                    Text(relativeTime(appointment.createdAt))
                        .font(HASETTheme.font(.regular, 11))
                        .foregroundStyle(HASETTheme.textSecondary)
                }
                Text(String(format: appViewModel.tr("appointment_notification_message"), doctorName(for: appointment), appointment.date, appointment.time))
                    .font(HASETTheme.font(.regular, 13))
                    .foregroundStyle(HASETTheme.textSecondary)
                    .lineLimit(2)
                if let (text, color) = statusBadge(appointment) {
                    Text(text)
                        .font(HASETTheme.font(.medium, 11))
                        .foregroundStyle(color)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(
                            Capsule().fill(color.opacity(0.12))
                        )
                        .padding(.top, 2)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private func paymentRow(_ item: NotificationSummary) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: infoIcon(for: item.type))
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(HASETTheme.greenPrimary)
                .frame(width: 48, height: 48)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(HASETTheme.greenPrimary.opacity(0.12))
                )

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(item.title)
                        .font(HASETTheme.font(.medium, 15))
                        .foregroundStyle(HASETTheme.textPrimary)
                        .lineLimit(1)
                    Spacer(minLength: 8)
                    Text(relativeTime(item.timestamp))
                        .font(HASETTheme.font(.regular, 11))
                        .foregroundStyle(HASETTheme.textSecondary)
                }
                if !item.message.isEmpty {
                    Text(item.message)
                        .font(HASETTheme.font(.regular, 13))
                        .foregroundStyle(HASETTheme.textSecondary)
                        .lineLimit(2)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private func doctorName(for appointment: AppointmentSummary) -> String {
        let role = appViewModel.currentUser?.role
        if role == .doctor {
            return appViewModel.currentUser?.fullName ?? "Doctor"
        }
        return appointment.title
    }

    private func statusBadge(_ appointment: AppointmentSummary) -> (String, Color)? {
        switch appointment.status {
        case .pending:
            return (appViewModel.tr("pending"), .orange)
        case .approved:
            return (appViewModel.tr("approved"), HASETTheme.greenPrimary)
        case .completed:
            return (appViewModel.tr("completed"), HASETTheme.greenPrimary)
        case .cancelled:
            return (appViewModel.tr("canceled"), HASETTheme.redPrimary)
        case .declined:
            return (appViewModel.tr("declined"), HASETTheme.redPrimary)
        }
    }

    private func infoIcon(for type: String) -> String {
        let normalized = type.lowercased()
        if normalized == "withdrawal" { return "wallet.pass.fill" }
        if normalized == "payment" { return "creditcard.fill" }
        return "bell.fill"
    }

    /// Replicates Android's `GenericNotificationAdapter.getRelativeTime()`
    /// exactly (Android hardcodes these English strings).
    private func relativeTime(_ timestampMilliseconds: TimeInterval?) -> String {
        guard let timestampMilliseconds, timestampMilliseconds > 0 else { return "" }
        let now = Date().timeIntervalSince1970 * 1000
        let diff = now - timestampMilliseconds
        if diff < 60_000 { return "Just now" }
        if diff < 3_600_000 { return "\(Int(diff / 60_000)) min ago" }
        if diff < 86_400_000 { return "\(Int(diff / 3_600_000))h ago" }
        if diff < 604_800_000 { return "\(Int(diff / 86_400_000))d ago" }
        let formatter = DateFormatter()
        formatter.dateFormat = "dd MMM"
        return formatter.string(from: Date(timeIntervalSince1970: timestampMilliseconds / 1000))
    }

    private func clearAppointments() {
        var hidden = AppViewModel.notificationHiddenAppointmentIds()
        hidden.formUnion(appointmentNotifications.map { $0.id })
        AppViewModel.saveNotificationHiddenAppointmentIds(hidden)
        appViewModel.refreshNotificationBadge()
    }

    private func clearAll() async {
        clearAppointments()
        await appViewModel.clearAllNotifications()
    }
}

struct SettingsView: View {
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var supportPresented = false
    @State private var languagePickerPresented = false
    @State private var themePickerPresented = false
    @State private var mfaEnabled = false
    @State private var mfaRequireOnLogin = false
    @State private var mfaBusy = true
    @State private var mfaEnrollmentPresented = false
    @State private var mfaDisablePresented = false
    @State private var mfaDisableLoginPresented = false

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
                        SettingsRow(icon: "bell", title: appViewModel.tr("notification"), subtitle: appViewModel.tr("notification_subtitle")) {
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
                        if mfaEnabled {
                            SettingsDivider()
                            SettingsRow(
                                icon: "lock",
                                title: appViewModel.tr("mfa_require_login_title"),
                                subtitle: appViewModel.tr("mfa_require_login_desc")
                            ) {
                                Toggle("", isOn: Binding(
                                    get: { mfaRequireOnLogin },
                                    set: { newValue in
                                        if newValue {
                                            mfaRequireOnLogin = true
                                            if let userId = appViewModel.activeSession?.userId
                                                ?? SessionStore().loadSession()?.userId {
                                                SessionStore().setMfaRequiredOnLogin(userId: userId, required: true)
                                            }
                                        } else {
                                            mfaDisableLoginPresented = true
                                        }
                                    }
                                ))
                                .labelsHidden()
                                .disabled(mfaBusy)
                            }
                        }
                        SettingsDivider()
                        NavigationLink {
                            ForgotPasswordView(useAuthBackNavigation: false)
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
                    mfaRequireOnLogin = false
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
                    mfaRequireOnLogin = false
                    if let userId = appViewModel.activeSession?.userId
                        ?? SessionStore().loadSession()?.userId {
                        SessionStore().setMfaRequiredOnLogin(userId: userId, required: false)
                    }
                    mfaDisablePresented = false
                },
                onCancel: { mfaDisablePresented = false }
            )
            .environmentObject(appViewModel)
        }
        .sheet(isPresented: $mfaDisableLoginPresented) {
            MFADisableLoginRequireView(
                onDisabled: {
                    mfaRequireOnLogin = false
                    if let userId = appViewModel.activeSession?.userId
                        ?? SessionStore().loadSession()?.userId {
                        SessionStore().setMfaRequiredOnLogin(userId: userId, required: false)
                    }
                    mfaDisableLoginPresented = false
                    appViewModel.alertState = AlertState(
                        title: appViewModel.tr("settings"),
                        message: appViewModel.tr("mfa_login_disabled_success")
                    )
                },
                onCancel: { mfaDisableLoginPresented = false }
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
            mfaRequireOnLogin = mfaEnabled && SessionStore().isMfaRequiredOnLogin(userId: freshSession.userId)
            if !mfaEnabled {
                SessionStore().setMfaRequiredOnLogin(userId: freshSession.userId, required: false)
                mfaRequireOnLogin = false
            }
        } catch {
            appViewModel.alertState = AlertState(title: appViewModel.tr("error"), message: error.localizedDescription)
        }
    }
}

private struct MFADisableLoginRequireView: View {
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
                Text(appViewModel.tr("mfa_disable_login_title"))
                    .font(HASETTheme.font(.medium, 20))
                    .multilineTextAlignment(.center)
                Text(useRecoveryCode
                     ? "Enter one unused 10-character recovery code. This code will be consumed."
                     : appViewModel.tr("mfa_disable_login_message"))
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
                Button(loading ? "…" : appViewModel.tr("mfa_disable_login_action")) { verifyAndDisable() }
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

    private func verifyAndDisable() {
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
                try await service.verifyLoginMobileMFA(code: code, idToken: freshSession.idToken)
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
    @State private var submitting = false
    @State private var resultMessage: String?
    @State private var resultIsError = false

    private var trimmedReport: String {
        bugReport.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(appViewModel.tr("contact")) {
                    Link(appViewModel.tr("support_whatsapp"), destination: URL(string: StaticContentService.supportWhatsAppURL)!)
                        .accessibilityLabel(appViewModel.tr("support_whatsapp"))
                    Link(appViewModel.tr("call_support"), destination: URL(string: "tel://\(StaticContentService.supportPhone)")!)
                        .accessibilityLabel(appViewModel.tr("call_support"))
                }

                Section(appViewModel.tr("bug_report")) {
                    TextEditor(text: $bugReport)
                        .frame(minHeight: 120)
                        .accessibilityLabel(appViewModel.tr("bug_report"))
                    if let resultMessage {
                        Text(resultMessage)
                            .font(HASETTheme.font(.regular, 13))
                            .foregroundStyle(resultIsError ? HASETTheme.redPrimary : HASETTheme.greenPrimary)
                    }
                    Button {
                        Task { await submit() }
                    } label: {
                        HStack {
                            if submitting {
                                ProgressView().tint(HASETTheme.greenPrimary)
                            }
                            Text(appViewModel.tr("submit_report"))
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .disabled(submitting || trimmedReport.isEmpty)
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

    private func submit() async {
        guard !submitting else { return }
        submitting = true
        resultMessage = nil
        do {
            try await appViewModel.submitBugReport(bugReport)
            resultIsError = false
            resultMessage = appViewModel.tr("report_recorded")
            bugReport = ""
        } catch {
            resultIsError = true
            resultMessage = appViewModel.tr("failed_to_submit_report")
        }
        submitting = false
    }
}

struct InAppWebContentView: View {
    let title: String
    let url: URL
    var body: some View {
        HostedCheckoutWebView(url: url, onCallback: {})
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
    }
}

struct DoctorPolicyView: View {
    @EnvironmentObject private var appViewModel: AppViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                Text(appViewModel.tr("per_service_fee_policy"))
                    .font(HASETTheme.font(.medium, 20))
                    .foregroundStyle(HASETTheme.greenPrimary)
                Text(appViewModel.tr("per_service_fee_policy_body"))
                    .font(HASETTheme.font(.regular, 14))
                    .foregroundStyle(HASETTheme.textSecondary)
                Text(appViewModel.tr("location_tracking_access_policy"))
                    .font(HASETTheme.font(.medium, 20))
                    .foregroundStyle(HASETTheme.greenPrimary)
                Text(appViewModel.tr("location_tracking_policy_body"))
                    .font(HASETTheme.font(.regular, 14))
                    .foregroundStyle(HASETTheme.textSecondary)
            }
            .padding(20)
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("doctor_policy"))
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
                        ProfileAvatarWithVerifiedBadge(
                            imageSource: doctor.profileImage ?? "",
                            initials: doctor.name.split(separator: " ").prefix(2).compactMap(\.first).map(String.init).joined().uppercased(),
                            size: 60,
                            fontSize: 18,
                            showVerified: doctor.verified
                        )

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
                Picker(appViewModel.tr("gender"), selection: $gender) {
                    Text(appViewModel.tr("not_set")).tag("")
                    Text("Female").tag("Female")
                    Text("Male").tag("Male")
                    Text("Other").tag("Other")
                }
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

enum AppointmentExportBuilder {
    private static let pageWidth: CGFloat = 595
    private static let pageHeight: CGFloat = 842
    private static let margin: CGFloat = 40

    static func csv(_ rows: [AppointmentSummary]) -> String {
        func escape(_ value: String) -> String {
            if value.contains(",") || value.contains("\"") || value.contains("\n") {
                return "\"\(value.replacingOccurrences(of: "\"", with: "\"\""))\""
            }
            return value
        }
        var lines = ["Appointment ID,Title,Participant,Status,Date,Time,Display,Type"]
        for row in rows {
            let fields = [
                row.id,
                "Appointment",
                row.title,
                row.status.rawValue,
                row.date,
                row.time,
                row.dateText,
                row.appointmentType ?? ""
            ]
            lines.append(fields.map(escape).joined(separator: ","))
        }
        return lines.joined(separator: "\r\n")
    }

    static func pdf(_ rows: [AppointmentSummary]) -> Data? {
        let renderer = UIGraphicsPDFRenderer(
            bounds: CGRect(x: 0, y: 0, width: pageWidth, height: pageHeight)
        )
        var y: CGFloat = 0
        let data = renderer.pdfData { context in
            context.beginPage()
            y = draw(
                "AfyaHASET Appointments",
                font: .systemFont(ofSize: 18, weight: .bold),
                color: .black,
                at: y + margin,
                width: pageWidth - margin * 2
            )
            y += 4
            y = draw(
                "Exported \(Formatters.mediumDate.string(from: Date())) • \(rows.count) appointments",
                font: .systemFont(ofSize: 11),
                color: .darkGray,
                at: y,
                width: pageWidth - margin * 2
            )
            y += 16

            for row in rows {
                if y > pageHeight - margin - 70 {
                    context.beginPage()
                    y = margin
                }
                y = draw(
                    "\(row.title) — \(row.status.rawValue)",
                    font: .systemFont(ofSize: 13, weight: .semibold),
                    color: .black,
                    at: y,
                    width: pageWidth - margin * 2
                )
                y += 2
                y = draw(
                    "\(row.dateText)\(row.appointmentType.map { " • \($0)" } ?? "")",
                    font: .systemFont(ofSize: 11),
                    color: .darkGray,
                    at: y,
                    width: pageWidth - margin * 2
                )
                y += 14
            }
        }
        return data.isEmpty ? nil : data
    }

    private static func draw(
        _ text: String,
        font: UIFont,
        color: UIColor,
        at y: CGFloat,
        width: CGFloat
    ) -> CGFloat {
        let attributes: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: color]
        let bounding = (text as NSString).boundingRect(
            with: CGSize(width: width, height: .greatestFiniteMagnitude),
            options: [.usesLineFragmentOrigin, .usesFontLeading],
            attributes: attributes,
            context: nil
        )
        (text as NSString).draw(
            in: CGRect(x: margin, y: y, width: width, height: ceil(bounding.height)),
            withAttributes: attributes
        )
        return y + ceil(bounding.height)
    }
}

enum ChatAttachmentProcessor {
    enum AttachmentError: Error {
        case tooLarge
        case failed
    }

    static let maxImageBytes = 1_048_576
    static let maxDocumentBytes = 1_048_576
    static let maxVideoBytes = 5_242_880

    static let documentTypes: [UTType] = [
        .pdf,
        .plainText,
        .commaSeparatedText,
        .rtf,
        UTType("org.openxmlformats.wordprocessingml.document") ?? .data,
        UTType("org.openxmlformats.spreadsheetml.sheet") ?? .data,
        UTType("com.microsoft.word.doc") ?? .data,
        UTType("com.microsoft.excel.xls") ?? .data
    ]

    static func mimeType(for fileName: String) -> String {
        switch (fileName as NSString).pathExtension.lowercased() {
        case "pdf": return "application/pdf"
        case "txt": return "text/plain"
        case "csv": return "text/csv"
        case "rtf": return "application/rtf"
        case "doc": return "application/msword"
        case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        case "xls": return "application/vnd.ms-excel"
        case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        case "png": return "image/png"
        case "jpg", "jpeg": return "image/jpeg"
        case "mp4": return "video/mp4"
        case "mov": return "video/quicktime"
        case "m4a": return "audio/mp4"
        default: return "application/octet-stream"
        }
    }

    static func formattedSize(_ bytes: Int) -> String {
        if bytes >= 1_048_576 {
            return String(format: "%.1f MB", Double(bytes) / 1_048_576)
        }
        if bytes >= 1024 {
            return String(format: "%.1f KB", Double(bytes) / 1024)
        }
        return "\(bytes) B"
    }

    static func imageData(_ data: Data) -> Data? {
        guard let image = UIImage(data: data) else {
            return data.count <= maxImageBytes ? data : nil
        }
        return jpegData(from: image)
    }

    static func jpegData(from image: UIImage) -> Data? {
        let longestSide = max(image.size.width, image.size.height)
        guard longestSide > 0 else { return nil }
        let scale = min(1, 1800 / longestSide)
        let targetSize = CGSize(width: image.size.width * scale, height: image.size.height * scale)
        let renderer = UIGraphicsImageRenderer(size: targetSize)
        let resized = renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: targetSize))
        }
        var quality: CGFloat = 0.88
        var encoded = resized.jpegData(compressionQuality: quality)
        while let current = encoded, current.count > maxImageBytes, quality > 0.4 {
            quality -= 0.12
            encoded = resized.jpegData(compressionQuality: quality)
        }
        guard let data = encoded, data.count <= maxImageBytes else { return nil }
        return data
    }

    static func videoData(url: URL) async throws -> Data {
        let size = (try? url.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0
        if size > 0, size <= maxVideoBytes {
            return try Data(contentsOf: url)
        }
        return try await compressedVideoData(at: url)
    }

    private static func compressedVideoData(at url: URL) async throws -> Data {
        let asset = AVURLAsset(url: url)
        guard let session = AVAssetExportSession(
            asset: asset,
            presetName: AVAssetExportPresetMediumQuality
        ) else {
            throw AttachmentError.failed
        }
        let output = FileManager.default.temporaryDirectory
            .appendingPathComponent("chat-video-\(UUID().uuidString).mp4")
        try? FileManager.default.removeItem(at: output)
        session.outputURL = output
        session.outputFileType = .mp4
        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            session.exportAsynchronously {
                continuation.resume()
            }
        }
        guard session.status == .completed else {
            throw AttachmentError.failed
        }
        let exportedSize = (try? output.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0
        defer { try? FileManager.default.removeItem(at: output) }
        guard exportedSize > 0, exportedSize <= maxVideoBytes else {
            throw AttachmentError.tooLarge
        }
        return try Data(contentsOf: output)
    }
}

struct PickedChatVideo: Transferable {
    let url: URL

    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(contentType: .movie) { received in
            SentTransferredFile(received.url)
        } importing: { received in
            let extensionName = received.file.pathExtension.isEmpty ? "mp4" : received.file.pathExtension
            let copy = FileManager.default.temporaryDirectory
                .appendingPathComponent("chat-video-\(UUID().uuidString).\(extensionName)")
            try? FileManager.default.removeItem(at: copy)
            try FileManager.default.copyItem(at: received.file, to: copy)
            return PickedChatVideo(url: copy)
        }
    }
}

struct CameraChatPicker: UIViewControllerRepresentable {
    let onImage: (UIImage) -> Void
    let onCancel: () -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.mediaTypes = ["public.image"]
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(onImage: onImage, onCancel: onCancel)
    }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        private let onImage: (UIImage) -> Void
        private let onCancel: () -> Void

        init(onImage: @escaping (UIImage) -> Void, onCancel: @escaping () -> Void) {
            self.onImage = onImage
            self.onCancel = onCancel
        }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            if let image = info[.originalImage] as? UIImage {
                onImage(image)
            } else {
                onCancel()
            }
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            onCancel()
        }
    }
}

struct QuickLookChatPreview: UIViewControllerRepresentable {
    let fileURL: URL

    func makeUIViewController(context: Context) -> QLPreviewController {
        let controller = QLPreviewController()
        controller.dataSource = context.coordinator
        return controller
    }

    func updateUIViewController(_ uiViewController: QLPreviewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(items: [fileURL])
    }

    final class Coordinator: NSObject, QLPreviewControllerDataSource {
        private let items: [URL]

        init(items: [URL]) {
            self.items = items
        }

        func numberOfPreviewItems(in controller: QLPreviewController) -> Int {
            items.count
        }

        func previewController(
            _ controller: QLPreviewController,
            previewItemAt index: Int
        ) -> QLPreviewItem {
            items[index] as NSURL
        }
    }
}

struct ChatAttachmentSheet: View {
    let role: UserRole
    let onCamera: () -> Void
    let onDocument: () -> Void
    @Binding var imageSelection: PhotosPickerItem?
    @Binding var videoSelection: PhotosPickerItem?
    var onPrescription: (() -> Void)? = nil
    var onRequestService: (() -> Void)? = nil

    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                if UIImagePickerController.isSourceTypeAvailable(.camera) {
                    Button {
                        onCamera()
                    } label: {
                        Label(appViewModel.tr("take_photo"), systemImage: "camera")
                    }
                }
                PhotosPicker(selection: $imageSelection, matching: .images) {
                    Label(appViewModel.tr("photo_library"), systemImage: "photo.on.rectangle")
                }
                PhotosPicker(selection: $videoSelection, matching: .videos) {
                    Label(appViewModel.tr("video_library"), systemImage: "film")
                }
                Button {
                    onDocument()
                } label: {
                    Label(appViewModel.tr("browse_document"), systemImage: "doc")
                }
                if role == .doctor {
                    if let onPrescription {
                        Button {
                            onPrescription()
                        } label: {
                            Label(appViewModel.tr("send_prescription"), systemImage: "cross.case")
                        }
                    }
                    if let onRequestService {
                        Button {
                            onRequestService()
                        } label: {
                            Label(appViewModel.tr("request_service_payment"), systemImage: "creditcard")
                        }
                    }
                }
            }
            .navigationTitle(appViewModel.tr("attach_file"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(appViewModel.tr("cancel")) { dismiss() }
                }
            }
        }
    }
}

struct ChatPrescriptionSheet: View {
    let chatRoomId: String
    let patientId: String
    let patientName: String
    let onSent: () async -> Void

    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var instructions = ""
    @State private var medicineName = ""
    @State private var dosage = ""
    @State private var frequency = ""
    @State private var duration = ""
    @State private var medicines: [PrescriptionMedicine] = []
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var imageData: Data?
    @State private var appointments: [AppointmentSummary] = []
    @State private var selectedAppointmentId = ""
    @State private var loadingAppointments = true
    @State private var saving = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(appViewModel.tr("patient"))
                            .font(HASETTheme.font(.medium, 13))
                            .foregroundStyle(HASETTheme.textSecondary)
                        Text(patientName)
                            .font(HASETTheme.font(.medium, 16))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(HASETTheme.backgroundPrimary)
                    )

                    if loadingAppointments {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                    } else if appointments.isEmpty {
                        Text(appViewModel.tr("no_appointments_for_patient"))
                            .font(HASETTheme.font(.regular, 13))
                            .foregroundStyle(.orange)
                    } else {
                        Picker(appViewModel.tr("select_appointment"), selection: $selectedAppointmentId) {
                            Text(appViewModel.tr("select_appointment")).tag("")
                            ForEach(appointments) { appointment in
                                Text(appointmentLabel(appointment)).tag(appointment.id)
                            }
                        }
                    }

                    TextField(appViewModel.tr("instructions"), text: $instructions, axis: .vertical)
                        .lineLimit(3...6)
                        .textFieldStyle(.roundedBorder)

                    TextField(appViewModel.tr("medicine_name"), text: $medicineName)
                        .textFieldStyle(.roundedBorder)
                    TextField(appViewModel.tr("dosage"), text: $dosage)
                        .textFieldStyle(.roundedBorder)
                    TextField(appViewModel.tr("frequency"), text: $frequency)
                        .textFieldStyle(.roundedBorder)
                    TextField(appViewModel.tr("duration_days"), text: $duration)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)

                    Button(appViewModel.tr("add_medicine")) { addMedicine() }
                        .disabled(medicineName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                    ForEach(medicines) { medicine in
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(medicine.name)
                                    .font(HASETTheme.font(.medium, 14))
                                Text("\(medicine.dosage) • \(medicine.frequency) • \(medicine.duration) \(appViewModel.tr("days"))")
                                    .font(HASETTheme.font(.regular, 12))
                                    .foregroundStyle(HASETTheme.textSecondary)
                            }
                            Spacer()
                            Button {
                                medicines.removeAll { $0.id == medicine.id }
                            } label: {
                                Image(systemName: "trash")
                                    .foregroundStyle(.red)
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(10)
                        .background(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .fill(Color.white)
                        )
                    }

                    PhotosPicker(selection: $selectedPhoto, matching: .images) {
                        Label(
                            imageData == nil ? appViewModel.tr("upload_image") : appViewModel.tr("image_selected"),
                            systemImage: imageData == nil ? "photo.badge.plus" : "photo.badge.checkmark"
                        )
                    }
                }
                .padding(20)
            }
            .navigationTitle(appViewModel.tr("send_prescription"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(appViewModel.tr("cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(saving ? appViewModel.tr("saving") : appViewModel.tr("save")) {
                        Task { await save() }
                    }
                    .disabled(saving || appointments.isEmpty)
                }
            }
            .task { await loadAppointments() }
            .onChange(of: selectedPhoto) { item in
                Task { imageData = try? await item?.loadTransferable(type: Data.self) }
            }
            .alert(appViewModel.tr("error"), isPresented: Binding(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )) {
                Button(appViewModel.tr("ok"), role: .cancel) {}
            } message: {
                Text(errorMessage ?? "")
            }
            .screenshotProtected()
        }
    }

    private func addMedicine() {
        let trimmedName = medicineName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else { return }
        medicines.append(
            PrescriptionMedicine(
                id: UUID().uuidString,
                name: trimmedName,
                dosage: dosage.trimmingCharacters(in: .whitespacesAndNewlines),
                frequency: frequency.trimmingCharacters(in: .whitespacesAndNewlines),
                duration: Int(duration.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0
            )
        )
        medicineName = ""
        dosage = ""
        frequency = ""
        duration = ""
    }

    @MainActor
    private func loadAppointments() async {
        guard let session = appViewModel.activeSession ?? SessionStore().loadSession() else {
            loadingAppointments = false
            return
        }
        do {
            let all = try await AuthService().fetchAppointments(
                userId: session.userId,
                role: .doctor,
                idToken: session.idToken
            )
            appointments = all
                .filter { ($0.patientId ?? "") == patientId }
                .sorted { ($0.createdAt ?? 0) > ($1.createdAt ?? 0) }
            selectedAppointmentId = appointments.first?.id ?? ""
        } catch {
            errorMessage = error.localizedDescription
        }
        loadingAppointments = false
    }

    private func save() async {
        guard let profile = appViewModel.currentUser,
              let session = appViewModel.activeSession ?? SessionStore().loadSession() else { return }
        guard !selectedAppointmentId.isEmpty else {
            errorMessage = appViewModel.tr("select_appointment_required")
            return
        }
        guard !medicines.isEmpty else {
            errorMessage = appViewModel.tr("at_least_one_medicine")
            return
        }

        saving = true
        defer { saving = false }
        do {
            var imageURL: String?
            if let imageData {
                imageURL = try await AuthService().uploadPrescriptionImage(imageData)
            }
            let prescriptionId = try await AuthService().createPrescription(
                patientId: patientId,
                patientName: patientName,
                doctorId: session.userId,
                doctorName: profile.fullName,
                appointmentId: selectedAppointmentId,
                medicines: medicines,
                instructions: instructions.trimmingCharacters(in: .whitespacesAndNewlines),
                imageUrl: imageURL,
                idToken: session.idToken
            )
            try await appViewModel.sendChatMessage(
                chatRoomId: chatRoomId,
                receiverId: patientId,
                receiverName: patientName,
                message: appViewModel.tr("new_prescription_issued"),
                messageType: "prescription",
                prescriptionId: prescriptionId
            )
            await onSent()
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func appointmentLabel(_ appointment: AppointmentSummary) -> String {
        [appointment.dateText, appointment.time, appointment.title]
            .filter { !$0.isEmpty }
            .joined(separator: " • ")
    }
}

struct ChatServicePayload: Codable, Hashable {
    let serviceId: String?
    let serviceName: String?
    let appointmentFee: Double?
    let patientPercentage: Int?
    let patientPayAmount: Double?
    let doctorId: String?
    let patientId: String?
    let isPaid: Bool?
    let paymentStatus: String?
    let createdAt: TimeInterval?

    init?(json: String) {
        guard let data = json.data(using: .utf8),
              let value = try? JSONDecoder().decode(ChatServicePayload.self, from: data) else {
            return nil
        }
        self = value
    }
}

struct PendingServicePayment: Identifiable {
    let messageId: String
    let payload: ChatServicePayload
    let checkoutDoctor: DoctorSummary

    var id: String { messageId }
}

struct ChatServiceRequestSheet: View {
    let chatRoomId: String
    let doctorId: String
    let doctorName: String
    let patientId: String
    let patientName: String
    let onSent: () async -> Void

    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var serviceName = ""
    @State private var feeText = ""
    @State private var patientPercentage = 50
    @State private var saving = false
    @State private var errorMessage: String?

    private var appointmentFee: Double {
        Double(feeText.replacingOccurrences(of: ",", with: "").trimmingCharacters(in: .whitespaces)) ?? 0
    }

    private var patientPayAmount: Double {
        appointmentFee * Double(patientPercentage) / 100.0
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(appViewModel.tr("patient"))
                            .font(HASETTheme.font(.medium, 13))
                            .foregroundStyle(HASETTheme.textSecondary)
                        Text(patientName)
                            .font(HASETTheme.font(.medium, 16))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(HASETTheme.backgroundPrimary)
                    )

                    TextField(appViewModel.tr("service_name"), text: $serviceName)
                        .textFieldStyle(.roundedBorder)

                    TextField(appViewModel.tr("service_fee"), text: $feeText)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)

                    Stepper(
                        "\(appViewModel.tr("you_pay")) \(patientPercentage)%",
                        value: $patientPercentage,
                        in: 0...100,
                        step: 5
                    )

                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(appViewModel.tr("total_fee"))
                                .font(HASETTheme.font(.regular, 14))
                            Spacer()
                            Text(String(format: "%,.0f TZS", appointmentFee))
                                .font(HASETTheme.font(.medium, 14))
                        }
                        HStack {
                            Text("\(appViewModel.tr("you_pay")) (\(patientPercentage)%):")
                                .font(HASETTheme.font(.regular, 14))
                            Spacer()
                            Text(String(format: "%,.0f TZS", patientPayAmount))
                                .font(HASETTheme.font(.medium, 14))
                                .foregroundStyle(HASETTheme.greenPrimary)
                        }
                    }
                    .padding(12)
                    .background(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(Color.white)
                    )

                    if appointmentFee > 0, patientPayAmount < HASETConstants.minPaymentAmount {
                        Text(appViewModel.tr("service_min_amount"))
                            .font(HASETTheme.font(.regular, 13))
                            .foregroundStyle(.orange)
                    }
                }
                .padding(20)
            }
            .navigationTitle(appViewModel.tr("request_service_payment"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(appViewModel.tr("cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(saving ? appViewModel.tr("saving") : appViewModel.tr("send")) {
                        Task { await save() }
                    }
                    .disabled(saving)
                }
            }
            .alert(appViewModel.tr("error"), isPresented: Binding(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )) {
                Button(appViewModel.tr("ok"), role: .cancel) {}
            } message: {
                Text(errorMessage ?? "")
            }
            .screenshotProtected()
        }
    }

    private func save() async {
        let trimmedName = serviceName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else {
            errorMessage = appViewModel.tr("service_name_required")
            return
        }
        guard appointmentFee > 0 else {
            errorMessage = appViewModel.tr("service_fee_required")
            return
        }
        guard patientPayAmount >= HASETConstants.minPaymentAmount else {
            errorMessage = appViewModel.tr("service_min_amount")
            return
        }
        guard let profile = appViewModel.currentUser,
              let session = appViewModel.activeSession ?? SessionStore().loadSession() else { return }

        saving = true
        defer { saving = false }

        let serviceId = UUID().uuidString
        let payload: [String: Any] = [
            "serviceId": serviceId,
            "serviceName": trimmedName,
            "appointmentFee": appointmentFee,
            "patientPercentage": patientPercentage,
            "patientPayAmount": patientPayAmount,
            "doctorId": session.userId,
            "patientId": patientId,
            "isPaid": false,
            "createdAt": Int(Date().timeIntervalSince1970 * 1000),
            "paymentStatus": "pending"
        ]
        do {
            let data = try JSONSerialization.data(withJSONObject: payload, options: [.sortedKeys])
            let json = String(data: data, encoding: .utf8) ?? "{}"
            let messageId = try await appViewModel.sendChatMessage(
                chatRoomId: chatRoomId,
                receiverId: patientId,
                receiverName: patientName,
                message: json,
                messageType: "service"
            )
            try await AuthService().createServicePaymentRequest(
                serviceId: serviceId,
                messageId: messageId,
                chatRoomId: chatRoomId,
                doctorId: session.userId,
                patientId: patientId,
                serviceName: trimmedName,
                appointmentFee: appointmentFee,
                patientPercentage: patientPercentage,
                patientPayAmount: patientPayAmount,
                idToken: session.idToken
            )
            await onSent()
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
