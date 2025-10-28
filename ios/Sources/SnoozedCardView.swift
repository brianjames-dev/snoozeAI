import SwiftUI

struct SnoozedCardView: View {
    let title: String
    let summary: String
    let urgency: Double
    let snoozeUntil: Date

    @State private var remaining: TimeInterval = 0
    @State private var timer: Timer?

    var urgencyLabel: String {
        urgency > 0.6 ? "Urgent" : urgency > 0.3 ? "Normal" : "Low"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title).font(.headline)
                Spacer()
                Text(urgencyLabel)
                    .font(.caption).bold()
                    .padding(.horizontal, 8).padding(.vertical, 4)
                    .background(urgency > 0.6 ? Color.red.opacity(0.15)
                               : urgency > 0.3 ? Color.orange.opacity(0.15)
                               : Color.green.opacity(0.15))
                    .clipShape(Capsule())
            }
            Text(summary).font(.subheadline).foregroundStyle(.secondary)
            HStack(spacing: 6) {
                Image(systemName: "clock")
                Text(timeString(from: remaining))
            }
            .font(.caption).foregroundStyle(.secondary)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .strokeBorder(.quaternary)
                .background(RoundedRectangle(cornerRadius: 12).fill(Color(.systemBackground)))
        )
        .onAppear { startTimer() }
        .onDisappear { timer?.invalidate() }
    }

    private func startTimer() {
        remaining = max(0, snoozeUntil.timeIntervalSinceNow)
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            remaining = max(0, snoozeUntil.timeIntervalSinceNow)
        }
    }

    private func timeString(from seconds: TimeInterval) -> String {
        let s = Int(seconds)
        let h = s / 3600, m = (s % 3600) / 60, sec = s % 60
        if h > 0 { return String(format: "%dh %dm %ds", h, m, sec) }
        if m > 0 { return String(format: "%dm %ds", m, sec) }
        return String(format: "%ds", sec)
    }
}
