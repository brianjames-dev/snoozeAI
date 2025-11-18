import SwiftUI

struct NewSnoozeSheet: View {
    enum DurationOption: String, CaseIterable, Identifiable {
        case fifteenMin
        case oneHour
        case todayPM
        case custom

        var id: String { rawValue }

        var label: String {
            switch self {
            case .fifteenMin: return "15 minutes"
            case .oneHour: return "1 hour"
            case .todayPM: return "Today PM"
            case .custom: return "Custom"
            }
        }
    }

    @Environment(\.dismiss) private var dismiss
    @AppStorage("defaultSnoozeMinutes") private var defaultSnoozeMinutes: Int = 60

    var onCreate: (SnoozedItem) -> Void

    @State private var title: String = ""
    @State private var body: String = ""
    @State private var selectedOption: DurationOption
    @State private var customDate: Date
    @State private var isSaving = false
    @State private var errorMessage: String?

    init(onCreate: @escaping (SnoozedItem) -> Void) {
        self.onCreate = onCreate
        let storedMinutes = UserDefaults.standard.object(forKey: "defaultSnoozeMinutes") as? Int ?? 60
        let option = NewSnoozeSheet.defaultOption(for: storedMinutes)
        _selectedOption = State(initialValue: option)
        let initialDate = Date().addingTimeInterval(TimeInterval(max(storedMinutes, 60)) * 60)
        _customDate = State(initialValue: initialDate)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Details") {
                    TextField("Title", text: $title)
                    TextEditor(text: $body)
                        .frame(minHeight: 100)
                }

                Section("Duration") {
                    Picker("Snooze duration", selection: $selectedOption) {
                        ForEach(DurationOption.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                    .pickerStyle(.segmented)

                    if selectedOption == .custom {
                        DatePicker("Custom time", selection: $customDate, displayedComponents: [.date, .hourAndMinute])
                    }
                }

                if let errorMessage {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundColor(.red)
                }
            }
            .navigationTitle("New Snooze")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", role: .cancel) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(action: { Task { await save() } }) {
                        if isSaving { ProgressView() }
                        Text("Snooze")
                    }
                    .disabled(title.isEmpty || body.isEmpty || isSaving)
                }
            }
            .onChange(of: defaultSnoozeMinutes) { newValue in
                selectedOption = NewSnoozeSheet.defaultOption(for: newValue)
            }
        }
    }

    private func resolveSnoozeDate() -> Date {
        switch selectedOption {
        case .fifteenMin:
            return Date().addingTimeInterval(15 * 60)
        case .oneHour:
            return Date().addingTimeInterval(60 * 60)
        case .todayPM:
            var calendar = Calendar.current
            calendar.timeZone = .current
            var components = calendar.dateComponents([.year, .month, .day], from: Date())
            components.hour = 15
            components.minute = 0
            let afternoon = calendar.date(from: components) ?? Date().addingTimeInterval(6 * 3600)
            if afternoon < Date() {
                return calendar.date(byAdding: .day, value: 1, to: afternoon) ?? Date().addingTimeInterval(24 * 3600)
            }
            return afternoon
        case .custom:
            return customDate
        }
    }

    @MainActor
    private func save() async {
        guard !title.isEmpty, !body.isEmpty else { return }
        isSaving = true
        errorMessage = nil
        let snoozeDate = resolveSnoozeDate()
        do {
            let item = try await SnoozeService.shared.createSnooze(
                title: title,
                body: body,
                snoozeUntil: snoozeDate,
                urgency: nil
            )
            onCreate(item)
            dismiss()
        } catch {
            errorMessage = "Failed to snooze. Please try again."
            print("New snooze error:", error)
        }
        isSaving = false
    }

    private static func defaultOption(for minutes: Int) -> DurationOption {
        if minutes <= 20 { return .fifteenMin }
        if minutes <= 90 { return .oneHour }
        if minutes <= 12 * 60 { return .todayPM }
        return .custom
    }
}
