import SwiftUI

struct SettingsView: View {
    @AppStorage("useRealAI") private var useRealAI: Bool = true
    @AppStorage("quietHoursEnabled") private var quietHoursEnabled: Bool = true
    @AppStorage("quietHoursStart") private var quietHoursStartValue: Double = SettingsView.defaultQuietHoursStart.timeIntervalSinceReferenceDate
    @AppStorage("quietHoursEnd") private var quietHoursEndValue: Double = SettingsView.defaultQuietHoursEnd.timeIntervalSinceReferenceDate
    @AppStorage("useResurfaceTime") private var useResurfaceTime: Bool = false
    @AppStorage("defaultSnoozeMinutes") private var defaultSnoozeMinutes: Int = 60
    @AppStorage("defaultResurfaceTime") private var defaultResurfaceTimeValue: Double = SettingsView.defaultResurfaceTime.timeIntervalSinceReferenceDate
    @AppStorage("prioritySources") private var prioritySourcesRaw: String = ""

    static var defaultQuietHoursStart: Date {
        Calendar.current.date(bySettingHour: 22, minute: 0, second: 0, of: Date()) ?? Date()
    }

    static var defaultQuietHoursEnd: Date {
        Calendar.current.date(bySettingHour: 7, minute: 30, second: 0, of: Date()) ?? Date()
    }

    static var defaultResurfaceTime: Date {
        Calendar.current.date(bySettingHour: 17, minute: 0, second: 0, of: Date()) ?? Date()
    }

    var body: some View {
        NavigationView {
            Form {
                Section("Intelligence") {
                    Toggle("Real AI summaries", isOn: $useRealAI)
                    Text(useRealAI ? "LLM summaries are enabled." : "Using fallback summaries.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }

                Section("Quiet Hours") {
                    Toggle("Enable Quiet Hours", isOn: $quietHoursEnabled)
                    DatePicker("Start", selection: quietHoursBinding(for: $quietHoursStartValue), displayedComponents: .hourAndMinute)
                        .disabled(!quietHoursEnabled)
                        .opacity(quietHoursEnabled ? 1 : 0.5)
                    DatePicker("End", selection: quietHoursBinding(for: $quietHoursEndValue), displayedComponents: .hourAndMinute)
                        .disabled(!quietHoursEnabled)
                        .opacity(quietHoursEnabled ? 1 : 0.5)
                }

                Section("Defaults") {
                    Toggle("Use resurface time", isOn: $useResurfaceTime)
                    Picker("Default Snooze Length", selection: $defaultSnoozeMinutes) {
                        Text("15 minutes").tag(15)
                        Text("1 hour").tag(60)
                        Text("Today PM").tag(180)
                        Text("Tomorrow morning").tag(24 * 60)
                    }
                    .disabled(useResurfaceTime)
                    .opacity(useResurfaceTime ? 0.4 : 1)

                    DatePicker(
                        "Default resurface time",
                        selection: resurfaceBinding(for: $defaultResurfaceTimeValue),
                        displayedComponents: .hourAndMinute
                    )
                    .disabled(!useResurfaceTime)
                    .opacity(useResurfaceTime ? 1 : 0.4)
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

    private func resurfaceBinding(for value: Binding<Double>) -> Binding<Date> {
        Binding<Date>(
            get: { Date(timeIntervalSinceReferenceDate: value.wrappedValue) },
            set: { value.wrappedValue = $0.timeIntervalSinceReferenceDate }
        )
    }
}
