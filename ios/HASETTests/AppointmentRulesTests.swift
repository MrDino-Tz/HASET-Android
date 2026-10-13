import XCTest
@testable import HASET

final class AppointmentRulesTests: XCTestCase {
    private func makeAppointment(
        status: AppointmentSummary.Status,
        rawStatus: String,
        paymentStatus: String? = nil,
        appointmentType: String? = "Visit",
        date: String = "2030-01-01",
        time: String = "10:00",
        createdAt: TimeInterval? = Date().timeIntervalSince1970 * 1000
    ) -> AppointmentSummary {
        var appointment = AppointmentSummary(
            id: "appt-1",
            patientId: "patient-1",
            doctorId: "doctor-1",
            title: "Patient One",
            subtitle: "Checkup",
            date: date,
            time: time,
            dateText: "\(date) \(time)",
            status: status,
            appointmentType: appointmentType,
            profileImage: nil,
            createdAt: createdAt
        )
        appointment.rawStatus = rawStatus
        appointment.paymentStatus = paymentStatus
        return appointment
    }

    func testAwaitingPaymentIsHiddenFromDoctor() {
        let appointment = makeAppointment(status: .pending, rawStatus: "awaiting_payment", paymentStatus: "unpaid")
        XCTAssertTrue(appointment.isHiddenFromDoctorUntilPaid)
    }

    func testUnpaidPendingIsHiddenFromDoctor() {
        let appointment = makeAppointment(status: .pending, rawStatus: "pending", paymentStatus: "unpaid")
        XCTAssertTrue(appointment.isHiddenFromDoctorUntilPaid)
    }

    func testMissingPaymentStatusPendingIsHiddenFromDoctor() {
        let appointment = makeAppointment(status: .pending, rawStatus: "pending", paymentStatus: nil)
        XCTAssertTrue(appointment.isHiddenFromDoctorUntilPaid)
    }

    func testPaidPendingIsVisibleToDoctor() {
        let appointment = makeAppointment(status: .pending, rawStatus: "pending", paymentStatus: "paid")
        XCTAssertFalse(appointment.isHiddenFromDoctorUntilPaid)
    }

    func testApprovedIsVisibleRegardlessOfPaymentStatus() {
        let appointment = makeAppointment(status: .approved, rawStatus: "approved", paymentStatus: nil)
        XCTAssertFalse(appointment.isHiddenFromDoctorUntilPaid)
    }

    func testScheduledMillisParsesSupportedFormats() {
        XCTAssertNotNil(AppointmentSummary.scheduledMillis(date: "2030-01-02", time: "09:30"))
        XCTAssertNotNil(AppointmentSummary.scheduledMillis(date: "02 Jan 2030", time: "09:30"))
        XCTAssertNotNil(AppointmentSummary.scheduledMillis(date: "02/01/2030", time: "09:30"))
        XCTAssertNil(AppointmentSummary.scheduledMillis(date: "", time: "09:30"))
    }

    func testApprovedVisitOlderThan24HoursAutoCompletes() {
        let past = Date().addingTimeInterval(-25 * 60 * 60)
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        let dateString = formatter.string(from: past)
        formatter.dateFormat = "HH:mm"
        let timeString = formatter.string(from: past)

        let appointment = makeAppointment(
            status: .approved,
            rawStatus: "approved",
            date: dateString,
            time: timeString
        )
        XCTAssertTrue(appointment.shouldAutoComplete)
    }

    func testApprovedVisitWithin24HoursDoesNotAutoComplete() {
        let recent = Date().addingTimeInterval(-60 * 60)
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        let dateString = formatter.string(from: recent)
        formatter.dateFormat = "HH:mm"
        let timeString = formatter.string(from: recent)

        let appointment = makeAppointment(
            status: .approved,
            rawStatus: "approved",
            date: dateString,
            time: timeString
        )
        XCTAssertFalse(appointment.shouldAutoComplete)
    }

    func testOnlineChatNeverAutoCompletes() {
        let past = Date().addingTimeInterval(-72 * 60 * 60)
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        let dateString = formatter.string(from: past)
        formatter.dateFormat = "HH:mm"
        let timeString = formatter.string(from: past)

        let appointment = makeAppointment(
            status: .approved,
            rawStatus: "approved",
            appointmentType: "Online Chat",
            date: dateString,
            time: timeString
        )
        XCTAssertFalse(appointment.shouldAutoComplete)
    }

    func testPatientCancelRules() {
        XCTAssertTrue(makeAppointment(status: .pending, rawStatus: "pending").canPatientCancel)
        XCTAssertTrue(makeAppointment(status: .approved, rawStatus: "approved").canPatientCancel)
        XCTAssertFalse(
            makeAppointment(status: .approved, rawStatus: "approved", appointmentType: "Online Chat").canPatientCancel
        )
        XCTAssertFalse(makeAppointment(status: .completed, rawStatus: "completed").canPatientCancel)
    }

    func testHistoryRetentionWindow() {
        let recent = makeAppointment(status: .completed, rawStatus: "completed")
        XCTAssertTrue(recent.isWithinHistoryRetention)

        let oldCreatedAt = Date().addingTimeInterval(-100 * 24 * 60 * 60).timeIntervalSince1970 * 1000
        let stale = makeAppointment(status: .completed, rawStatus: "completed", createdAt: oldCreatedAt)
        XCTAssertFalse(stale.isWithinHistoryRetention)
    }
}
