import SwiftUI

struct SettingsView: View {
    @AppStorage("quietHoursStart") private var quietHoursStartValue: Double = SettingsView.defaultQuietHoursStart.timeIntervalSinceReferenceDate
    @AppStorage("quietHoursEnd") private var quietHoursEndValue: Double = SettingsView.defaultQuietHoursEnd.timeIntervalSinceReferenceDate
    @AppStorage("defaultSnoozeMinutes") private var defaultSnoozeMinutes: Int = 60
    @AppStorage("prioritySources") private var prioritySourcesRaw: String = ""

    private static var defaultQuietHoursStart: Date {
        Calendar.current.date(bySettingHour: 22, minute: 0, second: 0, of: Date()) ?? Date()
    }

    private static var defaultQuietHoursEnd: Date {
        Calendar.current.date(bySettingHour: 7, minute: 30, second: 0, of: Date()) ?? Date()
    }

    var body: some View {
        NavigationView {
            Form {
                Section("Quiet Hours") {
                    DatePicker("Start", selection: quietHoursBinding(for: $quietHoursStartValue), displayedComponents: .hourAndMinute)
                    DatePicker("End", selection: quietHoursBinding(for: $quietHoursEndValue), displayedComponents: .hourAndMinute)
                }

                Section("Defaults") {
                    Picker("Default Snooze Length", selection: $defaultSnoozeMinutes) {
                        Text("15 minutes").tag(15)
                        Text("1 hour").tag(60)
                        Text("Today PM").tag(180)
                        Text("Tomorrow morning").tag(24 * 60)
                    }
                }

                Section("High-Priority Sources") {
                    TextEditor(text: Binding(
                        get: { prioritySourcesRaw },
                        set: { prioritySourcesRaw = $0 }
                    ))
                    .frame(minHeight: 120)
                    Text("Separate entries by comma or newline. We'll use these hints when classifying notifications.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Settings")
        }
    }

    private func quietHoursBinding(for value: Binding<Double>) -> Binding<Date> {
        Binding<Date>(
            get: { Date(timeIntervalSinceReferenceDate: value.wrappedValue) },
            set: { value.wrappedValue = $0.timeIntervalSinceReferenceDate }
        )
    }
}
