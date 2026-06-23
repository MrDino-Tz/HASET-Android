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

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(HASETTheme.backgroundCard)
                    .shadow(color: HASETTheme.greenPrimary.opacity(0.08), radius: 14, x: 0, y: 8)
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
