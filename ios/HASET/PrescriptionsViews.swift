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
            PrescriptionDetailView(prescription: item)
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
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

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
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .screenshotProtected()
        }
    }
}

private struct AddPrescriptionView: View {
    let onSaved: () async -> Void
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var patientId = ""
    @State private var patientName = ""
    @State private var instructions = ""
    @State private var medicineName = ""
    @State private var dosage = ""
    @State private var frequency = ""
    @State private var duration = ""
    @State private var medicines: [PrescriptionMedicine] = []
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var imageData: Data?
    @State private var saving = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    TextField("Patient ID", text: $patientId)
                        .textFieldStyle(.roundedBorder)
                    TextField("Patient name", text: $patientName)
                        .textFieldStyle(.roundedBorder)
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
        let trimmedPatientId = patientId.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPatientName = patientName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPatientId.isEmpty, !trimmedPatientName.isEmpty else {
            errorMessage = "Patient ID and name are required."
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
                patientId: trimmedPatientId,
                patientName: trimmedPatientName,
                doctorId: profile.userId,
                doctorName: profile.fullName,
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
}
