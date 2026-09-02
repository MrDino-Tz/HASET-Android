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
    var approved: Bool?

    var isAdminVerifiedDoctor: Bool {
        guard role == .doctor, approved == true else { return false }
        return verified != false
    }
}

struct RemoteAppConfig: Codable, Equatable {
    var maintenanceMode: Bool = false
    var minVersionCode: Int = 0
    var updateUrl: String?
    var maintenanceMessage: String?
    var doctorRegistrationFee: Double?

    enum CodingKeys: String, CodingKey {
        case maintenanceMode
        case minVersionCode
        case updateUrl
        case maintenanceMessage
        case doctorRegistrationFee
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        maintenanceMode = try container.decodeIfPresent(Bool.self, forKey: .maintenanceMode) ?? false
        minVersionCode = try container.decodeIfPresent(Int.self, forKey: .minVersionCode) ?? 0
        updateUrl = try container.decodeIfPresent(String.self, forKey: .updateUrl)
        maintenanceMessage = try container.decodeIfPresent(String.self, forKey: .maintenanceMessage)
        doctorRegistrationFee = Self.decodeDouble(container, forKey: .doctorRegistrationFee)
    }

    private static func decodeDouble(
        _ container: KeyedDecodingContainer<CodingKeys>,
        forKey key: CodingKeys
    ) -> Double? {
        if let value = try? container.decode(Double.self, forKey: key) {
            return value
        }
        if let value = try? container.decode(String.self, forKey: key) {
            return Double(value.trimmingCharacters(in: .whitespacesAndNewlines))
        }
        return nil
    }
}

enum AppRoute: Equatable {
    case splash
    case onboarding
    case login
    case mfaChallenge
    case mfaEnrollment
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

struct BlockingDialogState: Identifiable {
    let id = UUID()
    let title: String
    let message: String
    let updateURL: String?
}

struct PendingDoctorRegistration: Identifiable {
    let id = UUID()
    let doctor: DoctorSummary
    let amount: Double
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
    let isDemo: Bool
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

struct HealthTipSummary: Identifiable, Hashable {
    let id: String
    let text: String
    let author: String
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
        case declined = "Declined"

        var id: String { rawValue }
    }

    let id: String
    let patientId: String?
    let doctorId: String?
    let title: String
    let subtitle: String
    let date: String
    let time: String
    let dateText: String
    let status: Status
    let appointmentType: String?
    let profileImage: String?
    let createdAt: TimeInterval?
}

struct ConversationSummary: Identifiable, Hashable {
    let id: String
    let otherUserId: String
    let name: String
    let lastMessage: String
    let lastMessageTimestamp: TimeInterval
    let unreadCount: Int
    let isOnline: Bool
    let archived: Bool
    let profileImage: String?
}

struct ChatMessageSummary: Identifiable, Hashable {
    let id: String
    let senderId: String
    let receiverId: String
    let message: String
    let timestamp: TimeInterval
    let isRead: Bool
    let messageType: String
    let attachmentURL: String?
    let attachmentFileName: String?
    let replyToMessageID: String?
    let replyToText: String?

    var isOutgoing: Bool = false
}

struct ChatSendAccessSummary: Hashable {
    let canSend: Bool
    let message: String?

    static let allowed = ChatSendAccessSummary(canSend: true, message: nil)
}

struct NotificationSummary: Identifiable, Hashable {
    let id: String
    let title: String
    let message: String
    let type: String
    let isRead: Bool
    let timestamp: TimeInterval
}

struct DoctorWalletSummary: Codable, Hashable {
    let doctorId: String
    let balance: Double
    let totalEarnings: Double?
    let lastUpdated: TimeInterval?
    let mobileMoneyDestination: PayoutDestinationSummary?
    let bankDestination: PayoutDestinationSummary?
}

struct PayoutDestinationSummary: Codable, Hashable {
    let available: Bool
    let label: String
}

struct DoctorWithdrawalSummary: Identifiable, Hashable {
    let id: String
    let amount: Double
    let feeAmount: Double
    let status: String
    let createdAt: Date?
    let failureReason: String?
}

struct MobileMFASetupResponse: Codable {
    let secret: String
    let otpauthURI: String
    let recoveryCodes: [String]
    enum CodingKeys: String, CodingKey { case secret; case otpauthURI = "otpauth_uri"; case recoveryCodes = "recovery_codes" }
}

struct DoctorPresenceSummary: Codable, Hashable {
    let doctorId: String
    let online: Bool
    let lastUpdated: TimeInterval?
}

struct HospitalSummary: Identifiable, Hashable {
    let id: String
    let name: String
    let location: String
    let distance: String
    let address: String?
    let city: String?
    let phone: String?
    let latitude: Double?
    let longitude: Double?

    var mapsQuery: String {
        var parts = [name]
        if let address, !address.isEmpty { parts.append(address) }
        else if !location.isEmpty { parts.append(location) }
        if let city, !city.isEmpty { parts.append(city) }
        return parts.joined(separator: ", ")
    }
}

struct PrescriptionMedicine: Identifiable, Hashable, Codable {
    let id: String
    let name: String
    let dosage: String
    let frequency: String
    let duration: Int
}

struct PrescriptionSummary: Identifiable, Hashable {
    let id: String
    let appointmentId: String?
    let patientId: String
    let patientName: String
    let doctorId: String
    let doctorName: String
    let medicines: [PrescriptionMedicine]
    let instructions: String
    let imageUrl: String?
    let createdAt: TimeInterval?
}

struct PharmacyProductSummary: Identifiable, Hashable {
    let id: String
    let name: String
    let description: String
    let category: String
    let price: Double
    let imageUrl: String?
    let manufacturer: String?
    let inStock: Bool
}

struct PharmacyCartItem: Identifiable, Hashable {
    let id: String
    let productId: String
    let name: String
    let price: Double
    let quantity: Int
    let imageUrl: String?

    var lineTotal: Double { price * Double(quantity) }
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
    let transactionId: Int?
    let orderReference: String?
    let paymentStatus: String?
    let paymentChannel: String?
    let paymentUrl: String?
    let reference: String?

    enum CodingKeys: String, CodingKey {
        case status
        case message
        case transactionId = "transaction_id"
        case orderReference = "order_reference"
        case paymentStatus = "payment_status"
        case paymentChannel = "payment_channel"
        case paymentUrl = "payment_url"
        case checkoutUrl = "checkout_url"
        case url
        case reference
        case data
    }

    private struct NestedData: Decodable {
        let transactionId: Int?
        let paymentUrl: String?

        enum CodingKeys: String, CodingKey {
            case transactionId = "transaction_id"
            case id
            case paymentUrl = "payment_url"
            case checkoutUrl = "checkout_url"
            case url
        }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            transactionId = Self.decodeInt(container, key: .transactionId)
                ?? Self.decodeInt(container, key: .id)
            paymentUrl = try? container.decodeIfPresent(String.self, forKey: .paymentUrl)
                ?? container.decodeIfPresent(String.self, forKey: .checkoutUrl)
                ?? container.decodeIfPresent(String.self, forKey: .url)
        }

        private static func decodeInt(
            _ container: KeyedDecodingContainer<CodingKeys>,
            key: CodingKeys
        ) -> Int? {
            if let value = try? container.decode(Int.self, forKey: key) {
                return value
            }
            if let value = try? container.decode(String.self, forKey: key) {
                return Int(value)
            }
            return nil
        }
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let nestedData = try? container.decodeIfPresent(NestedData.self, forKey: .data)
        status = try container.decodeIfPresent(String.self, forKey: .status) ?? "error"
        message = try container.decodeIfPresent(String.self, forKey: .message)
        transactionId = Self.decodeInt(container, key: .transactionId)
            ?? nestedData?.transactionId
        orderReference = try container.decodeIfPresent(String.self, forKey: .orderReference)
        paymentStatus = try container.decodeIfPresent(String.self, forKey: .paymentStatus)
        paymentChannel = try container.decodeIfPresent(String.self, forKey: .paymentChannel)
        paymentUrl = try container.decodeIfPresent(String.self, forKey: .paymentUrl)
            ?? container.decodeIfPresent(String.self, forKey: .checkoutUrl)
            ?? container.decodeIfPresent(String.self, forKey: .url)
            ?? nestedData?.paymentUrl
        reference = try container.decodeIfPresent(String.self, forKey: .reference)
    }

    private static func decodeInt(
        _ container: KeyedDecodingContainer<CodingKeys>,
        key: CodingKeys
    ) -> Int? {
        if let value = try? container.decode(Int.self, forKey: key) {
            return value
        }
        if let value = try? container.decode(String.self, forKey: key) {
            return Int(value)
        }
        return nil
    }

    var isSuccess: Bool {
        status.caseInsensitiveCompare("success") == .orderedSame
    }
}

struct PaymentStatusEnvelope: Decodable {
    struct Transaction: Decodable {
        let id: Int?
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

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            id = (try? container.decode(Int.self, forKey: .id))
                ?? (try? Int(container.decode(String.self, forKey: .id)))
            status = try container.decodeIfPresent(String.self, forKey: .status) ?? "pending"
            paymentStatus = try container.decodeIfPresent(String.self, forKey: .paymentStatus)
            amount = try container.decodeIfPresent(Double.self, forKey: .amount)
            currency = try container.decodeIfPresent(String.self, forKey: .currency)
            provider = try container.decodeIfPresent(String.self, forKey: .provider)
            createdAt = try container.decodeIfPresent(String.self, forKey: .createdAt)
            updatedAt = try container.decodeIfPresent(String.self, forKey: .updatedAt)
            externalReference = try container.decodeIfPresent(String.self, forKey: .externalReference)
        }

        var isSuccess: Bool {
            let payment = paymentStatus?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? ""
            if !payment.isEmpty {
                if Self.pendingValues.contains(payment) || Self.failedValues.contains(payment) {
                    return false
                }
                return Self.settledValues.contains(payment)
            }
            return Self.settledValues.contains(status.lowercased())
        }

        var isFailed: Bool {
            Self.failedValues.contains(status.lowercased())
                || Self.failedValues.contains((paymentStatus ?? "").lowercased())
        }

        var isProcessing: Bool {
            if isSuccess || isFailed { return false }
            let payment = paymentStatus?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? ""
            if !payment.isEmpty {
                return Self.pendingValues.contains(payment) || !Self.settledValues.contains(payment)
            }
            return Self.pendingValues.contains(status.lowercased())
        }

        private static let settledValues: Set<String> = ["success", "completed", "paid"]
        private static let pendingValues: Set<String> = ["processing", "pending", "initiated", "submitted"]
        private static let failedValues: Set<String> = [
            "failed", "cancelled", "canceled", "expired", "declined", "rejected", "voided"
        ]
    }

    let status: String
    let message: String?
    let transaction: Transaction?

    enum CodingKeys: String, CodingKey {
        case status
        case message
        case transaction
        case data
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        status = try container.decodeIfPresent(String.self, forKey: .status) ?? "pending"
        message = try container.decodeIfPresent(String.self, forKey: .message)
        transaction = try container.decodeIfPresent(Transaction.self, forKey: .transaction)
            ?? (try? container.decodeIfPresent(Transaction.self, forKey: .data)) ?? nil
    }
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
