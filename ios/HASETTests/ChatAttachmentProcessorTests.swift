import XCTest
@testable import HASET

final class ChatAttachmentProcessorTests: XCTestCase {
    func testMimeTypeMapping() {
        XCTAssertEqual(ChatAttachmentProcessor.mimeType(for: "report.pdf"), "application/pdf")
        XCTAssertEqual(ChatAttachmentProcessor.mimeType(for: "notes.txt"), "text/plain")
        XCTAssertEqual(ChatAttachmentProcessor.mimeType(for: "data.csv"), "text/csv")
        XCTAssertEqual(ChatAttachmentProcessor.mimeType(for: "photo.JPG"), "image/jpeg")
        XCTAssertEqual(ChatAttachmentProcessor.mimeType(for: "clip.MOV"), "video/quicktime")
        XCTAssertEqual(ChatAttachmentProcessor.mimeType(for: "unknown.bin"), "application/octet-stream")
    }

    func testFormattedSizeBoundaries() {
        XCTAssertEqual(ChatAttachmentProcessor.formattedSize(512), "512 B")
        XCTAssertEqual(ChatAttachmentProcessor.formattedSize(1024), "1.0 KB")
        XCTAssertEqual(ChatAttachmentProcessor.formattedSize(1_048_576), "1.0 MB")
    }

    func testAttachmentLimitsMatchAndroidClient() {
        XCTAssertEqual(ChatAttachmentProcessor.maxImageBytes, 1_048_576)
        XCTAssertEqual(ChatAttachmentProcessor.maxDocumentBytes, 1_048_576)
        XCTAssertEqual(ChatAttachmentProcessor.maxVideoBytes, 5_242_880)
    }
}
