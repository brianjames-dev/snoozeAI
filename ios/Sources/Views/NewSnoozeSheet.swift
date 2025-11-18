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
    @State private var messageBody: String = ""
    @State private var selectedOption: DurationOption
    @State private var customDate: Date
    @State private var isSaving = false
    @State private var errorMessage: String?
    @AppStorage("useResurfaceTime") private var useResurfaceTime = false
    @AppStorage("defaultResurfaceTime") private var defaultResurfaceTimeValue: Double = SettingsView.defaultResurfaceTime.timeIntervalSinceReferenceDate

    init(onCreate: @escaping (SnoozedItem) -> Void) {
        self.onCreate = onCreate
        let defaults = UserDefaults.standard
        let storedMinutes = defaults.object(forKey: "defaultSnoozeMinutes") as? Int ?? 60
        let useResurfaceDefaults = defaults.bool(forKey: "useResurfaceTime")
        if useResurfaceDefaults {
            _selectedOption = State(initialValue: .custom)
            let storedTime = defaults.double(forKey: "defaultResurfaceTime")
            let defaultDate = NewSnoozeSheet.defaultResurfaceDate(from: storedTime)
            _customDate = State(initialValue: defaultDate)
        } else {
            let option = NewSnoozeSheet.defaultOption(for: storedMinutes)
            _selectedOption = State(initialValue: option)
            let initialDate = Date().addingTimeInterval(TimeInterval(max(storedMinutes, 60)) * 60)
            _customDate = State(initialValue: initialDate)
        }
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
                    .disabled(title.isEmpty || messageBody.isEmpty || isSaving)
                }
            }
            .onChangeCompat(of: defaultSnoozeMinutes) { newValue in
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
        guard !title.isEmpty, !messageBody.isEmpty else { return }
        isSaving = true
        errorMessage = nil
        let snoozeDate = resolveSnoozeDate()
        do {
            let item = try await SnoozeService.shared.createSnooze(
                title: title,
                body: messageBody,
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

    private static func defaultResurfaceDate(from stored: Double) -> Date {
        let calendar = Calendar.current
        let baseRef = stored == 0 ? SettingsView.defaultResurfaceTime : Date(timeIntervalSinceReferenceDate: stored)
        let base = baseRef
        var todayComponents = calendar.dateComponents([.year, .month, .day], from: Date())
        let timeComponents = calendar.dateComponents([.hour, .minute], from: base)
        todayComponents.hour = timeComponents.hour
        todayComponents.minute = timeComponents.minute
        var candidate = calendar.date(from: todayComponents) ?? Date().addingTimeInterval(3600)
        if candidate < Date() {
            candidate = calendar.date(byAdding: .day, value: 1, to: candidate) ?? candidate
        }
        return candidate
    }
}
