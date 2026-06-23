import Foundation

enum UserRole: String, Codable, CaseIterable, Identifiable {
    case patient
    case doctor
    case admin

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .patient: return "Patient"
        case .doctor: return "Doctor"
        case .admin: return "Administrator"
        }
    }
}

struct UserProfile: Codable, Equatable {
    var userId: String
    var email: String
    var fullName: String
    var phone: String
    var role: UserRole
    var profileImage: String
    var createdAt: TimeInterval
    var regNo: String?
    var gender: String?
    var age: String?
    var location: String?
    var bio: String?
    var specialization: String?
    var consultationFee: String?
    var availableTimes: [String]?
    var verified: Bool?
}

struct RemoteAppConfig: Codable, Equatable {
    var maintenanceMode: Bool = false
    var minVersionCode: Int = 0
    var updateUrl: String?
    var maintenanceMessage: String?
}

enum AppRoute: Equatable {
    case splash
    case onboarding
    case login
    case forgotPassword
    case roleSelection
    case register(UserRole)
    case dashboard(UserRole)
}

enum DashboardTab: Hashable {
    case home
    case appointments
    case chat
    case profile
}

struct StoredSession: Codable {
    var userId: String
    var idToken: String
    var refreshToken: String?
    var role: UserRole
    var userName: String
    var email: String
    var phone: String
}

struct AlertState: Identifiable {
    let id = UUID()
    let title: String
    let message: String
}

struct DoctorSummary: Identifiable, Hashable {
    let id: String
    let name: String
    let specialty: String
    let hospital: String
    let phoneNumber: String?
    let email: String?
    let address: String?
    let bio: String?
    let rating: Double
    let experienceYears: Int?
    let verified: Bool
    let consultationFee: String
    let availableToday: Bool
    let profileImage: String?
    let availableTimes: [String]?
}

struct ArticleSummary: Identifiable, Hashable {
    let id: String
    let title: String
    let author: String
    let authorImage: String?
    let category: String
    let excerpt: String
    let imageName: String?
    let imageURL: String?
    let timestamp: TimeInterval
    let readTime: String
    let content: [String]
    let viewCount: Int
    let likeCount: Int
    let commentCount: Int
    let shareCount: Int
    let type: String

    var hasVisual: Bool {
        (imageURL?.isEmpty == false) || (imageName?.isEmpty == false)
    }
}

struct ArticleComment: Identifiable, Hashable {
    let id: String
    let userId: String
    let userName: String
    let userImage: String?
    let text: String
    let timestamp: TimeInterval
}

struct HomeHighlight: Identifiable, Hashable {
    let id: String
    let titleLine1: String
    let titleLine2: String
    let badge: String
    let buttonText: String
    let imageName: String?
    let imageURL: String?
    let bannerType: String
    let targetAction: String?

    var isImageOnly: Bool {
        titleLine1.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        titleLine2.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}

struct AppointmentSummary: Identifiable, Hashable {
    enum Status: String, CaseIterable, Identifiable {
        case pending = "Pending"
        case approved = "Approved"
        case completed = "Completed"
        case cancelled = "Canceled"

        var id: String { rawValue }
    }

    let id: String
    let title: String
    let subtitle: String
    let dateText: String
    let status: Status
}

struct ConversationSummary: Identifiable, Hashable {
    let id: String
    let name: String
    let lastMessage: String
    let lastMessageTimestamp: TimeInterval
    let unreadCount: Int
    let isOnline: Bool
    let archived: Bool
    let profileImage: String?
}

struct NotificationSummary: Identifiable, Hashable {
    let id: String
    let title: String
    let message: String
    let type: String
    let isRead: Bool
    let timestamp: TimeInterval
}

struct HospitalSummary: Identifiable, Hashable {
    let id: String
    let name: String
    let location: String
    let distance: String
}

struct PharmacyCategory: Identifiable, Hashable {
    let id: String
    let title: String
    let subtitle: String
}

struct AdminMetric: Identifiable, Hashable {
    let id: String
    let title: String
    let value: String
}

struct PaymentInitiationResponse: Decodable {
    let status: String
    let message: String?
    let transactionId: Int
    let orderReference: String?
    let paymentStatus: String?
    let paymentChannel: String?
    let reference: String?

    enum CodingKeys: String, CodingKey {
        case status
        case message
        case transactionId = "transaction_id"
        case orderReference = "order_reference"
        case paymentStatus = "payment_status"
        case paymentChannel = "payment_channel"
        case reference
    }

    var isSuccess: Bool {
        status.caseInsensitiveCompare("success") == .orderedSame
    }
}

struct PaymentStatusEnvelope: Decodable {
    struct Transaction: Decodable {
        let id: Int
        let status: String
        let paymentStatus: String?
        let amount: Double?
        let currency: String?
        let provider: String?
        let createdAt: String?
        let updatedAt: String?
        let externalReference: String?

        enum CodingKeys: String, CodingKey {
            case id
            case status
            case paymentStatus = "payment_status"
            case amount
            case currency
            case provider
            case createdAt = "created_at"
            case updatedAt = "updated_at"
            case externalReference = "external_reference"
        }

        var isSuccess: Bool {
            status.caseInsensitiveCompare("success") == .orderedSame
        }

        var isFailed: Bool {
            ["failed", "cancelled", "expired", "declined"].contains(status.lowercased())
        }

        var isProcessing: Bool {
            ["processing", "pending"].contains(status.lowercased())
        }
    }

    let status: String
    let message: String?
    let transaction: Transaction?
}

enum ThemeMode: String, Codable, CaseIterable, Identifiable {
    case light
    case dark
    case system

    var id: String { rawValue }

    var label: String {
        switch self {
        case .light:
            return "Light"
        case .dark:
            return "Dark"
        case .system:
            return "System"
        }
    }
}
