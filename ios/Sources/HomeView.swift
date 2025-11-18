import SwiftUI
import UserNotifications

struct HomeView: View {
    @AppStorage("useRealAI") private var aiEnabled = true
    @AppStorage("quietHoursEnabled") private var quietHoursToggle = true
    @AppStorage("quietHoursStart") private var quietHoursStartValue: Double = SettingsView.defaultQuietHoursStart.timeIntervalSinceReferenceDate
    @AppStorage("quietHoursEnd") private var quietHoursEndValue: Double = SettingsView.defaultQuietHoursEnd.timeIntervalSinceReferenceDate
    @AppStorage("defaultSnoozeMinutes") private var defaultSnoozeMinutes: Int = 60
    @AppStorage("useResurfaceTime") private var useResurfaceTime = false
    @AppStorage("defaultResurfaceTime") private var defaultResurfaceTimeValue: Double = SettingsView.defaultResurfaceTime.timeIntervalSinceReferenceDate
    @AppStorage("prioritySources") private var prioritySourcesRaw: String = ""
    @AppStorage("quietBreakSamples") private var quietBreakSamples: String = "" // CSV of timestamps

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    statusGrid
                    statusSummary
                    actionsCard
                }
                .padding()
            }
            .navigationTitle("Home")
        }
    }

    private var statusSummary: some View {
        HomeCard(title: "Status") {
            VStack(alignment: .leading, spacing: 12) {
                Label(aiEnabled ? "AI summaries enabled" : "Fallback summaries", systemImage: aiEnabled ? "sparkles" : "slash.circle")
                    .foregroundStyle(.secondary)
                Label(quietHoursToggle ? "Quiet hours \(quietHoursRangeLabel)" : "Quiet hours off", systemImage: "moon.stars")
                    .foregroundStyle(.secondary)
                Label(useResurfaceTime ? "Default resurface \(resurfaceLabel)" : "Default snooze \(defaultSnoozeLabel)", systemImage: "clock.arrow.circlepath")
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var statusGrid: some View {
        VStack(spacing: 12) {
            HStack(spacing: 12) {
                statTile(icon: "bell.badge", value: "2", label: "Urgent now")
                statTile(icon: "clock.arrow.circlepath", value: "6", label: "Snoozed today")
            }
            HStack(spacing: 12) {
                statTile(icon: "bolt.badge.clock", value: useResurfaceTime ? resurfaceLabel : defaultSnoozeLabel, label: useResurfaceTime ? "Resurface time" : "Snooze length")
                statTile(icon: "moon.zzz.fill", value: quietBreakSummary.value, label: quietBreakSummary.label)
            }
        }
    }

    private func statTile(icon: String, value: String, label: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Label(label, systemImage: icon)
                .font(.footnote)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.title.bold())
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(RoundedRectangle(cornerRadius: 16).fill(Color(.secondarySystemBackground)))
    }

    private var actionsCard: some View {
        HomeCard(title: "Quick actions") {
            VStack(alignment: .leading, spacing: 12) {
                Button {
                    UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
                } label: {
                    Label("Request Notification Permission", systemImage: "bell.badge.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)

                Button {
                    sendLocalTestNotification()
                } label: {
                    Label("Send Local Test Notification", systemImage: "paperplane.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
        }
    }

    private func sendLocalTestNotification() {
        let content = UNMutableNotificationContent()
        content.title = "Meeting"
        content.subtitle = "AI Summary"
        content.body = "Standup in 15 minutes with the iOS team."
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 2, repeats: false)
        let req = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(req, withCompletionHandler: nil)
    }

    private var quietHoursRangeLabel: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        let start = Date(timeIntervalSinceReferenceDate: quietHoursStartValue)
        let end = Date(timeIntervalSinceReferenceDate: quietHoursEndValue)
        return "\(formatter.string(from: start)) – \(formatter.string(from: end))"
    }

    private var defaultSnoozeLabel: String {
        if defaultSnoozeMinutes >= 60 {
            let hours = Double(defaultSnoozeMinutes) / 60.0
            if hours.truncatingRemainder(dividingBy: 1) == 0 {
                return "\(Int(hours))h"
            }
            return String(format: "%.1fh", hours)
        }
        return "\(defaultSnoozeMinutes)m"
    }

    private var resurfaceLabel: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        let date = Date(timeIntervalSinceReferenceDate: defaultResurfaceTimeValue)
        return formatter.string(from: date)
    }

    private var quietBreakSummary: (value: String, label: String) {
        let calendar = Calendar.current
        let samples = quietBreakDates()
        let weekAgo = calendar.date(byAdding: .day, value: -7, to: Date()) ?? Date()
        let recent = samples.filter { $0 >= weekAgo }
        guard !recent.isEmpty else { return ("0", "Quiet breaks") }

        let buckets = Dictionary(grouping: recent) { date -> Int in
            let comps = calendar.dateComponents([.hour, .minute], from: date)
            return (comps.hour ?? 0) * 60 + (comps.minute ?? 0)
        }
        let topBucket = buckets.max { $0.value.count < $1.value.count }
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        let displayTime: String
        if let bucket = topBucket?.key {
            var comps = DateComponents()
            comps.hour = bucket / 60
            comps.minute = bucket % 60
            let representative = calendar.date(from: comps) ?? Date()
            displayTime = formatter.string(from: representative)
        } else {
            displayTime = formatter.string(from: Date())
        }
        return ("\(recent.count)", "Breaks • \(displayTime)")
    }

    private func quietBreakDates() -> [Date] {
        let iso = ISO8601DateFormatter()
        return quietBreakSamples
            .split(separator: ",")
            .compactMap { token -> Date? in
                let trimmed = token.trimmingCharacters(in: .whitespacesAndNewlines)
                if trimmed.isEmpty { return nil }
                if let seconds = Double(trimmed) {
                    return Date(timeIntervalSince1970: seconds)
                }
                return iso.date(from: trimmed)
            }
    }

    private var prioritySourcesCount: Int {
        prioritySourcesRaw
            .components(separatedBy: CharacterSet(charactersIn: ",\n"))
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .count
    }
}

private struct HomeCard<Content: View>: View {
    let title: String
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.headline)
            content
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 20).fill(Color(.systemBackground)))
        .shadow(color: Color.black.opacity(0.06), radius: 12, x: 0, y: 6)
    }
}
