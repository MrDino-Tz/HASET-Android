import XCTest
@testable import HASET

final class AppointmentExportBuilderTests: XCTestCase {
    private func sampleAppointments() -> [AppointmentSummary] {
        [
            AppointmentSummary(
                id: "appt-1",
                patientId: "patient-1",
                doctorId: "doctor-1",
                title: "Dr. Asha, Mwinyi",
                subtitle: "Cardiology",
                date: "2030-01-02",
                time: "09:30",
                dateText: "2030-01-02, 09:30",
                status: .approved,
                appointmentType: "Visit",
                profileImage: nil,
                createdAt: Date().timeIntervalSince1970 * 1000
            ),
            AppointmentSummary(
                id: "appt-2",
                patientId: "patient-2",
                doctorId: "doctor-1",
                title: "John Doe",
                subtitle: "Online",
                date: "2030-01-03",
                time: "14:00",
                dateText: "2030-01-03, 14:00",
                status: .pending,
                appointmentType: "Online Chat",
                profileImage: nil,
                createdAt: Date().timeIntervalSince1970 * 1000
            )
        ]
    }

    func testCSVIncludesHeaderAndOneRowPerAppointment() {
        let csv = AppointmentExportBuilder.csv(sampleAppointments())
        let lines = csv.components(separatedBy: "\r\n")
        XCTAssertEqual(lines.count, 3)
        XCTAssertEqual(lines.first, "Appointment ID,Title,Participant,Status,Date,Time,Display,Type")
        XCTAssertTrue(csv.contains("appt-1"))
        XCTAssertTrue(csv.contains("appt-2"))
    }

    func testCSVEscapesCommasAndQuotes() {
        let csv = AppointmentExportBuilder.csv(sampleAppointments())
        XCTAssertTrue(csv.contains("\"Dr. Asha, Mwinyi\""))
    }

    func testPDFIsGeneratedForRows() {
        let data = AppointmentExportBuilder.pdf(sampleAppointments())
        let unwrapped = try? XCTUnwrap(data)
        XCTAssertNotNil(unwrapped)
        XCTAssertGreaterThan(unwrapped?.count ?? 0, 0)
    }
}
