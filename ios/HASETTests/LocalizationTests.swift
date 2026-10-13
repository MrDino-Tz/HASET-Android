import XCTest
@testable import HASET

final class LocalizationTests: XCTestCase {
    func testEnglishAndSwahiliHaveIdenticalKeys() {
        let english = Set((L10n.strings["en"] ?? [:]).keys)
        let swahili = Set((L10n.strings["sw"] ?? [:]).keys)
        XCTAssertFalse(english.isEmpty)
        XCTAssertFalse(swahili.isEmpty)
        XCTAssertEqual(english.symmetricDifference(swahili), [], "en/sw key sets diverged")
    }

    func testNoDictionaryContainsBlankValues() {
        for (language, entries) in L10n.strings {
            for (key, value) in entries {
                XCTAssertFalse(
                    value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                    "Blank value for \(language).\(key)"
                )
            }
        }
    }

    func testTranslationFallsBackToEnglishThenKey() {
        XCTAssertEqual(L10n.tr("notifications", languageCode: "en"), "Notifications")
        XCTAssertEqual(L10n.tr("__missing_key__", languageCode: "en"), "__missing_key__")
    }
}
