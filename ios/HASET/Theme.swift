import SwiftUI
import UIKit

enum HASETTheme {
    static let greenPrimary = Color(red: 0.0, green: 0.533, blue: 0.0)
    static let greenLight = Color(red: 0.067, green: 0.667, blue: 0.067)
    static let redPrimary = Color(red: 0.867, green: 0.0, blue: 0.0)
    static let backgroundPrimary = Color(uiColor: UIColor { trait in
        trait.userInterfaceStyle == .dark
            ? UIColor(red: 0.06, green: 0.08, blue: 0.10, alpha: 1)
            : UIColor(red: 0.973, green: 0.976, blue: 0.980, alpha: 1)
    })
    static let backgroundCard = Color(uiColor: UIColor { trait in
        trait.userInterfaceStyle == .dark
            ? UIColor(red: 0.10, green: 0.12, blue: 0.16, alpha: 1)
            : .white
    })
    static let textPrimary = Color(uiColor: UIColor { trait in
        trait.userInterfaceStyle == .dark
            ? UIColor(red: 0.95, green: 0.96, blue: 0.98, alpha: 1)
            : UIColor(red: 0.122, green: 0.161, blue: 0.216, alpha: 1)
    })
    static let textSecondary = Color(uiColor: UIColor { trait in
        trait.userInterfaceStyle == .dark
            ? UIColor(red: 0.66, green: 0.70, blue: 0.77, alpha: 1)
            : UIColor(red: 0.420, green: 0.447, blue: 0.502, alpha: 1)
    })
    static let divider = Color(uiColor: UIColor { trait in
        trait.userInterfaceStyle == .dark
            ? UIColor(red: 0.20, green: 0.24, blue: 0.29, alpha: 1)
            : UIColor(red: 0.898, green: 0.906, blue: 0.922, alpha: 1)
    })
    static let error = Color(red: 0.824, green: 0.184, blue: 0.184)

    static func font(_ style: FontStyle, _ size: CGFloat) -> Font {
        switch style {
        case .regular:
            return .custom("Poppins-Regular", size: size)
        case .medium:
            return .custom("Poppins-Medium", size: size)
        case .black:
            return .custom("Poppins-Black", size: size)
        }
    }

    enum FontStyle {
        case regular
        case medium
        case black
    }
}

struct CardContainer<Content: View>: View {
    let content: Content
    var fill: Color = HASETTheme.backgroundCard
    var shadowColor: Color = HASETTheme.greenPrimary.opacity(0.08)
    var cornerRadius: CGFloat = 20
    var padding: CGFloat = 20

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    init(
        fill: Color,
        shadowColor: Color = HASETTheme.greenPrimary.opacity(0.08),
        cornerRadius: CGFloat = 20,
        padding: CGFloat = 20,
        @ViewBuilder content: () -> Content
    ) {
        self.content = content()
        self.fill = fill
        self.shadowColor = shadowColor
        self.cornerRadius = cornerRadius
        self.padding = padding
    }

    var body: some View {
        content
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(fill)
                    .shadow(color: shadowColor, radius: 14, x: 0, y: 8)
            )
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(HASETTheme.font(.medium, 16))
            .foregroundStyle(Color.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(HASETTheme.greenPrimary.opacity(configuration.isPressed ? 0.85 : 1))
            )
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
    }
}

struct CheckmarkShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.minX + rect.width * 0.18, y: rect.midY + rect.height * 0.02))
        path.addLine(to: CGPoint(x: rect.minX + rect.width * 0.42, y: rect.midY + rect.height * 0.30))
        path.addLine(to: CGPoint(x: rect.maxX - rect.width * 0.16, y: rect.midY - rect.height * 0.26))
        return path
    }
}

struct AnimatedSendButton: View {
    enum Phase: Equatable {
        case idle
        case sending
        case success
        case failed
    }

    let idleTitle: String
    let sendingTitle: String
    let successTitle: String
    @Binding var phase: Phase
    var disabled = false
    var action: () -> Void

    @State private var showCheckmark = false

    private var planeOffset: CGFloat { phase == .sending || phase == .failed ? 160 : 0 }
    private var planeRise: CGFloat { phase == .sending ? -70 : 0 }
    private var planeRotation: Double { phase == .sending ? -38 : 0 }
    private var planeScale: CGFloat { phase == .sending ? 0.6 : 1 }

    var body: some View {
        Button(action: action) {
            ZStack {
                HStack(spacing: 10) {
                    Image(systemName: "paperplane.fill")
                        .font(.system(size: 16, weight: .semibold))
                        .offset(x: planeOffset, y: planeRise)
                        .rotationEffect(.degrees(planeRotation))
                        .scaleEffect(planeScale)
                        .opacity(phase == .idle ? 1 : 0)
                    Text(idleTitle)
                        .opacity(phase == .idle ? 1 : 0)
                }
                .animation(.easeOut(duration: 0.34), value: phase)

                HStack(spacing: 10) {
                    if phase == .sending {
                        ProgressView().tint(.white).controlSize(.small)
                    }
                    Text(phase == .failed ? idleTitle : sendingTitle)
                        .opacity(phase == .sending ? 1 : (phase == .failed ? 1 : 0))
                }

                if phase == .success {
                    HStack(spacing: 10) {
                        CheckmarkShape()
                            .trim(from: 0, to: showCheckmark ? 1 : 0)
                            .stroke(Color.white, style: StrokeStyle(lineWidth: 3.2, lineCap: .round, lineJoin: .round))
                            .frame(width: 20, height: 20)
                        Text(successTitle)
                    }
                    .transition(.scale(scale: 0.6).combined(with: .opacity))
                }
            }
            .font(HASETTheme.font(.medium, 16))
            .foregroundStyle(Color.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(backgroundColor)
            )
        }
        .buttonStyle(.plain)
        .disabled(disabled || phase == .sending)
        .onChange(of: phase) { newPhase in
            showCheckmark = false
            if newPhase == .success {
                withAnimation(.spring(response: 0.45, dampingFraction: 0.6)) {
                    showCheckmark = true
                }
            }
        }
    }

    private var backgroundColor: Color {
        switch phase {
        case .failed:
            return HASETTheme.redPrimary
        case .success:
            return HASETTheme.greenPrimary
        case .idle, .sending:
            return HASETTheme.greenPrimary.opacity(disabled ? 0.55 : 1)
        }
    }
}

struct ConfirmationOverlay: View {
    let message: String
    @State private var animate = false
    @State private var drawCheck = false

    var body: some View {
        ZStack {
            Color.black.opacity(0.45).ignoresSafeArea()
            VStack(spacing: 18) {
                ZStack {
                    Circle()
                        .fill(HASETTheme.greenPrimary)
                        .frame(width: 92, height: 92)
                        .scaleEffect(animate ? 1 : 0.4)
                        .opacity(animate ? 1 : 0)
                    CheckmarkShape()
                        .trim(from: 0, to: drawCheck ? 1 : 0)
                        .stroke(Color.white, style: StrokeStyle(lineWidth: 4, lineCap: .round, lineJoin: .round))
                        .frame(width: 46, height: 46)
                }
                Text(message)
                    .font(HASETTheme.font(.medium, 17))
                    .foregroundStyle(HASETTheme.textPrimary)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 32)
            .padding(.vertical, 36)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(HASETTheme.backgroundCard)
                    .shadow(color: .black.opacity(0.15), radius: 24, x: 0, y: 12)
            )
            .padding(32)
        }
        .onAppear {
            withAnimation(.spring(response: 0.45, dampingFraction: 0.6)) {
                animate = true
            }
            withAnimation(.easeOut(duration: 0.35).delay(0.18)) {
                drawCheck = true
            }
        }
        .transition(.opacity)
    }
}
