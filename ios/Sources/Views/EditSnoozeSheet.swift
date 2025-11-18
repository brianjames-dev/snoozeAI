import SwiftUI

struct EditSnoozeSheet: View {
    let item: SnoozedItem
    var onSave: (SnoozedItem) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var title: String
    @State private var messageBody: String
    @State private var snoozeDate: Date
    @State private var isSaving = false
    @State private var errorMessage: String?

    init(item: SnoozedItem, onSave: @escaping (SnoozedItem) -> Void) {
        self.item = item
        self.onSave = onSave
        _title = State(initialValue: item.title)
        _messageBody = State(initialValue: item.body)
        _snoozeDate = State(initialValue: item.snoozeUntil)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Details") {
                    TextField("Title", text: $title)
                    ZStack(alignment: .topLeading) {
                        if messageBody.isEmpty {
                            Text("Body")
                                .foregroundColor(Color(.placeholderText))
                                .padding(.horizontal, 6)
                                .padding(.vertical, 10)
                        }
                        TextEditor(text: $messageBody)
                            .frame(minHeight: 100)
                    }
                }

                Section("Snooze Until") {
                    DatePicker("Remind me", selection: $snoozeDate, displayedComponents: [.date, .hourAndMinute])
                }

                if let errorMessage {
                    Text(errorMessage)
                        .foregroundColor(.red)
                        .font(.footnote)
                }
            }
            .navigationTitle("Edit Snooze")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", role: .cancel) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task { await save() }
                    } label: {
                        if isSaving { ProgressView() }
                        Text("Save")
                    }
                    .disabled(title.isEmpty || messageBody.isEmpty || isSaving)
                }
            }
        }
    }

    @MainActor
    private func save() async {
        guard !title.isEmpty, !messageBody.isEmpty else { return }
        isSaving = true
        errorMessage = nil
        do {
            let updated = try await SnoozeService.shared.updateSnooze(
                itemID: item.id,
                title: title,
                body: messageBody,
                snoozeUntil: snoozeDate
            )
            onSave(updated)
            dismiss()
        } catch {
            errorMessage = "Failed to save changes."
            print("Edit error:", error)
        }
        isSaving = false
    }
}
