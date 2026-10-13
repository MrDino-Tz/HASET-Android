import PhotosUI
import SwiftUI

struct PrescriptionsListView: View {
    let role: UserRole
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var prescriptions: [PrescriptionSummary] = []
    @State private var loading = true
    @State private var errorMessage: String?
    @State private var showAddPrescription = false
    @State private var selectedPrescription: PrescriptionSummary?

    var body: some View {
        Group {
            if loading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if prescriptions.isEmpty {
                CardContainer {
                    Text(appViewModel.tr("no_prescriptions_message"))
                        .font(HASETTheme.font(.medium, 15))
                        .foregroundStyle(HASETTheme.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.vertical, 12)
                }
                .padding(20)
            } else {
                List(prescriptions) { item in
                    Button {
                        selectedPrescription = item
                    } label: {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(role == .doctor ? item.patientName : item.doctorName)
                                .font(HASETTheme.font(.medium, 16))
                            Text(item.instructions.isEmpty ? "Prescription" : item.instructions)
                                .font(HASETTheme.font(.regular, 13))
                                .foregroundStyle(HASETTheme.textSecondary)
                                .lineLimit(2)
                            if !item.medicines.isEmpty {
                                Text(item.medicines.map(\.name).joined(separator: ", "))
                                    .font(HASETTheme.font(.regular, 12))
                                    .foregroundStyle(HASETTheme.greenPrimary)
                                    .lineLimit(1)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
                .listStyle(.plain)
            }
        }
        .background(HASETTheme.backgroundPrimary)
        .navigationTitle(appViewModel.tr("prescriptions"))
        .toolbar {
            if role == .doctor {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showAddPrescription = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
        }
        .task { await loadPrescriptions() }
        .refreshable { await loadPrescriptions() }
        .sheet(isPresented: $showAddPrescription) {
            AddPrescriptionView { await loadPrescriptions() }
                .environmentObject(appViewModel)
        }
        .sheet(item: $selectedPrescription) { item in
            PrescriptionDetailView(prescription: item, onDeleted: { await loadPrescriptions() })
                .environmentObject(appViewModel)
        }
        .alert("Error", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage ?? "")
        }
    }

    private func loadPrescriptions() async {
        guard let profile = appViewModel.currentUser,
              let session = appViewModel.activeSession ?? SessionStore().loadSession() else {
            loading = false
            return
        }
        loading = true
        do {
            prescriptions = try await AuthService().fetchPrescriptions(
                userId: profile.userId,
                role: role,
                idToken: session.idToken
            )
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
        loading = false
    }
}

struct PrescriptionDetailView: View {
    let prescription: PrescriptionSummary
    var onDeleted: () async -> Void = {}
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var showShareSheet = false
    @State private var showDeleteConfirmation = false
    @State private var isDeleting = false
    @State private var errorMessage: String?
    @State private var pdfData: Data?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    CardContainer {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(prescription.doctorName)
                                .font(HASETTheme.font(.medium, 18))
                            Text("Patient: \(prescription.patientName)")
                                .font(HASETTheme.font(.regular, 14))
                                .foregroundStyle(HASETTheme.textSecondary)
                            if let createdAt = prescription.createdAt {
                                Text(Formatters.mediumDateTime.string(from: Date(timeIntervalSince1970: createdAt / 1000)))
                                    .font(HASETTheme.font(.regular, 12))
                                    .foregroundStyle(HASETTheme.textSecondary)
                            }
                            if !prescription.instructions.isEmpty {
                                Text(prescription.instructions)
                                    .font(HASETTheme.font(.regular, 14))
                                    .padding(.top, 4)
                            }
                        }
                    }

                    if !prescription.medicines.isEmpty {
                        CardContainer {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Medicines")
                                    .font(HASETTheme.font(.medium, 16))
                                ForEach(prescription.medicines) { medicine in
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(medicine.name)
                                            .font(HASETTheme.font(.medium, 15))
                                        Text("\(medicine.dosage) • \(medicine.frequency) • \(medicine.duration) days")
                                            .font(HASETTheme.font(.regular, 13))
                                            .foregroundStyle(HASETTheme.textSecondary)
                                    }
                                    if medicine.id != prescription.medicines.last?.id {
                                        Divider()
                                    }
                                }
                            }
                        }
                    }

                    if let imageURL = prescription.imageUrl, let url = URL(string: imageURL) {
                        CardContainer {
                            AsyncImage(url: url) { phase in
                                switch phase {
                                case .success(let image):
                                    image.resizable().scaledToFit()
                                case .failure:
                                    Text("Unable to load prescription image")
                                        .foregroundStyle(HASETTheme.textSecondary)
                                default:
                                    ProgressView()
                                }
                            }
                        }
                    }
                }
                .padding(20)
            }
            .background(HASETTheme.backgroundPrimary)
            .navigationTitle(appViewModel.tr("prescriptions"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(appViewModel.tr("close")) { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button {
                            generatePDF()
                        } label: {
                            Label(appViewModel.tr("download_pdf"), systemImage: "doc.downarrow")
                        }
                        Button(role: .destructive) {
                            showDeleteConfirmation = true
                        } label: {
                            Label(appViewModel.tr("delete"), systemImage: "trash")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                    .accessibilityLabel(appViewModel.tr("more_options"))
                }
            }
            .confirmationDialog(
                appViewModel.tr("delete_prescription_confirm"),
                isPresented: $showDeleteConfirmation,
                titleVisibility: .visible
            ) {
                Button(appViewModel.tr("delete"), role: .destructive) {
                    Task { await deletePrescription() }
                }
                Button(appViewModel.tr("cancel"), role: .cancel) {}
            }
            .sheet(isPresented: $showShareSheet) {
                if let pdfData {
                    ActivityShareSheet(items: [pdfData])
                }
            }
            .alert(appViewModel.tr("error"), isPresented: Binding(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(errorMessage ?? "")
            }
            .screenshotProtected()
        }
    }

    private func generatePDF() {
        guard let data = PrescriptionPDFBuilder.make(prescription: prescription) else {
            errorMessage = appViewModel.tr("failed_to_generate_pdf")
            return
        }
        pdfData = data
        showShareSheet = true
    }

    private func deletePrescription() async {
        guard !isDeleting else { return }
        guard let session = appViewModel.activeSession ?? SessionStore().loadSession() else {
            errorMessage = appViewModel.tr("session_expired")
            return
        }
        isDeleting = true
        defer { isDeleting = false }
        do {
            try await AuthService().deletePrescription(
                prescriptionId: prescription.id,
                idToken: session.idToken
            )
            await onDeleted()
            dismiss()
        } catch {
            errorMessage = appViewModel.tr("failed_to_delete_prescription")
        }
    }
}

private struct ActivityShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

enum Formatters {
    static let mediumDateTime: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM dd, yyyy, hh:mm a"
        return formatter
    }()

    static let mediumDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd MMM yyyy"
        return formatter
    }()
}

enum PrescriptionPDFBuilder {
    static let pageWidth: CGFloat = 595
    static let pageHeight: CGFloat = 842
    private static let margin: CGFloat = 48

    static func make(prescription: PrescriptionSummary) -> Data? {
        let renderer = UIGraphicsPDFRenderer(
            bounds: CGRect(x: 0, y: 0, width: pageWidth, height: pageHeight)
        )
        let data = renderer.pdfData { context in
            context.beginPage()
            var y = margin

            y = draw(
                "AfyaHASET Prescription",
                style: .title,
                at: y,
                width: pageWidth - margin * 2
            )
            y += 6
            y = draw(
                prescription.createdAt.map {
                    Formatters.mediumDate.string(from: Date(timeIntervalSince1970: $0 / 1000))
                } ?? Formatters.mediumDate.string(from: Date()),
                style: .caption,
                at: y,
                width: pageWidth - margin * 2
            )
            y += 18

            y = draw("Doctor: \(prescription.doctorName)", style: .body, at: y, width: pageWidth - margin * 2)
            y += 4
            y = draw("Patient: \(prescription.patientName)", style: .body, at: y, width: pageWidth - margin * 2)

            if !prescription.instructions.isEmpty {
                y += 18
                y = draw("Instructions", style: .heading, at: y, width: pageWidth - margin * 2)
                y += 4
                y = draw(prescription.instructions, style: .body, at: y, width: pageWidth - margin * 2)
            }

            if !prescription.medicines.isEmpty {
                y += 18
                y = draw("Medicines", style: .heading, at: y, width: pageWidth - margin * 2)
                y += 8
                for (index, medicine) in prescription.medicines.enumerated() {
                    y = draw("\(index + 1). \(medicine.name)", style: .body, at: y, width: pageWidth - margin * 2)
                    y += 2
                    y = draw(
                        "   \(medicine.dosage) • \(medicine.frequency) • \(medicine.duration) days",
                        style: .caption,
                        at: y,
                        width: pageWidth - margin * 2
                    )
                    y += 10
                }
            }

            y += 24
            _ = draw(
                "Generated by AfyaHASET — hasethospital.or.tz",
                style: .caption,
                at: y,
                width: pageWidth - margin * 2
            )
        }
        return data.isEmpty ? nil : data
    }

    private enum Style {
        case title
        case heading
        case body
        case caption

        var font: UIFont {
            switch self {
            case .title: return .systemFont(ofSize: 20, weight: .bold)
            case .heading: return .systemFont(ofSize: 15, weight: .semibold)
            case .body: return .systemFont(ofSize: 13, weight: .regular)
            case .caption: return .systemFont(ofSize: 11, weight: .regular)
            }
        }

        var color: UIColor {
            switch self {
            case .title, .heading: return .black
            case .body: return .black
            case .caption: return .darkGray
            }
        }
    }

    private static func draw(_ text: String, style: Style, at y: CGFloat, width: CGFloat) -> CGFloat {
        let attributes: [NSAttributedString.Key: Any] = [
            .font: style.font,
            .foregroundColor: style.color
        ]
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

private struct PatientOption: Identifiable, Hashable {
    let id: String
    let name: String
}

private struct AddPrescriptionView: View {
    let onSaved: () async -> Void
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var patients: [PatientOption] = []
    @State private var selectedPatientId = ""
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
    @State private var saving = false
    @State private var errorMessage: String?

    private var appointmentsForPatient: [AppointmentSummary] {
        appointments.filter { $0.patientId == selectedPatientId }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if appointments.isEmpty {
                        Text(appViewModel.tr("no_appointments_for_prescription"))
                            .font(HASETTheme.font(.regular, 13))
                            .foregroundStyle(.orange)
                    } else {
                        Picker(appViewModel.tr("patient"), selection: $selectedPatientId) {
                            ForEach(patients) { patient in
                                Text(patient.name).tag(patient.id)
                            }
                        }
                        .pickerStyle(.menu)
                        .onChange(of: selectedPatientId) { _ in
                            selectedAppointmentId = appointmentsForPatient.first?.id ?? ""
                        }

                        if appointmentsForPatient.isEmpty {
                            Text(appViewModel.tr("no_appointments_for_patient"))
                                .font(HASETTheme.font(.regular, 13))
                                .foregroundStyle(.orange)
                        } else {
                            Picker(appViewModel.tr("select_appointment"), selection: $selectedAppointmentId) {
                                ForEach(appointmentsForPatient) { appointment in
                                    Text(appointmentLabel(appointment)).tag(appointment.id)
                                }
                            }
                        }
                    }
                    TextField("Instructions", text: $instructions, axis: .vertical)
                        .lineLimit(3...6)
                        .textFieldStyle(.roundedBorder)
                    TextField("Medicine name", text: $medicineName)
                        .textFieldStyle(.roundedBorder)
                    TextField("Dosage", text: $dosage)
                        .textFieldStyle(.roundedBorder)
                    TextField("Frequency", text: $frequency)
                        .textFieldStyle(.roundedBorder)
                    TextField("Duration (days)", text: $duration)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)
                    Button("Add medicine") { addMedicine() }
                        .disabled(medicineName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    ForEach(medicines) { medicine in
                        Text("\(medicine.name) - \(medicine.dosage)")
                            .font(HASETTheme.font(.regular, 14))
                    }
                    PhotosPicker(selection: $selectedPhoto, matching: .images) {
                        Text(imageData == nil ? "Upload image" : "Image selected")
                    }
                }
                .padding(20)
            }
            .navigationTitle("New Prescription")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(saving ? "Saving..." : "Save") {
                        Task { await save() }
                    }
                    .disabled(saving)
                }
            }
            .onChange(of: selectedPhoto) { item in
                Task {
                    imageData = try? await item?.loadTransferable(type: Data.self)
                }
            }
            .task { await loadDoctorAppointments() }
            .alert("Error", isPresented: .init(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )) {
                Button("OK", role: .cancel) {}
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

    private func save() async {
        guard let profile = appViewModel.currentUser,
              let session = appViewModel.activeSession ?? SessionStore().loadSession() else { return }
        guard let patient = patients.first(where: { $0.id == selectedPatientId }) else {
            errorMessage = appViewModel.tr("select_patient_required")
            return
        }
        guard !selectedAppointmentId.isEmpty else {
            errorMessage = appViewModel.tr("select_appointment_required")
            return
        }

        saving = true
        defer { saving = false }
        do {
            var imageURL: String?
            if let imageData {
                imageURL = try await AuthService().uploadPrescriptionImage(imageData)
            }
            try await AuthService().createPrescription(
                patientId: patient.id,
                patientName: patient.name,
                doctorId: profile.userId,
                doctorName: profile.fullName,
                appointmentId: selectedAppointmentId,
                medicines: medicines,
                instructions: instructions.trimmingCharacters(in: .whitespacesAndNewlines),
                imageUrl: imageURL,
                idToken: session.idToken
            )
            await onSaved()
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    @MainActor
    private func loadDoctorAppointments() async {
        guard appointments.isEmpty,
              let profile = appViewModel.currentUser,
              let session = appViewModel.activeSession ?? SessionStore().loadSession() else { return }
        do {
            appointments = try await AuthService().fetchAppointments(
                userId: profile.userId,
                role: .doctor,
                idToken: session.idToken
            )
            .sorted { ($0.createdAt ?? 0) > ($1.createdAt ?? 0) }

            var seenPatientIds = Set<String>()
            patients = appointments.compactMap { appointment in
                guard let patientId = appointment.patientId, !patientId.isEmpty else { return nil }
                return PatientOption(id: patientId, name: appointment.title)
            }
            .filter { seenPatientIds.insert($0.id).inserted }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }

            if !patients.contains(where: { $0.id == selectedPatientId }) {
                selectedPatientId = patients.first?.id ?? ""
            }
            if !appointmentsForPatient.contains(where: { $0.id == selectedAppointmentId }) {
                selectedAppointmentId = appointmentsForPatient.first?.id ?? ""
            }
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
