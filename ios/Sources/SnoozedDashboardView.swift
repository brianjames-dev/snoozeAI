import SwiftUI

struct SnoozedDashboardView: View {
    @State private var items: [SnoozedItem] = []
    @State private var isStoring = false
    private let service = SnoozeService.shared

    var body: some View {
        NavigationView {
            VStack {
                if items.isEmpty {
                    VStack(spacing: 8) {
                        Image(systemName: "clock.badge.questionmark").font(.largeTitle)
                        Text("No Snoozed Items").font(.headline)
                        Text("Tap the button to create a demo item.")
                            .font(.subheadline).foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List(items) { item in
                        SnoozedCardView(
                            title: item.title,
                            summary: item.summary,
                            urgency: item.urgency,
                            snoozeUntil: item.snoozeUntil
                        )
                        // iOS 15+; if Xcode complains, keep the #available wrapper
                        .modifier(HideSeparatorIfAvailable())
                    }
                    .listStyle(.plain)
                }

                Button {
                    Task { await snoozeDemo() }
                } label: {
                    HStack {
                        if isStoring { ProgressView().padding(.trailing, 6) }
                        Text("Snooze (Demo) → Backend")
                    }
                }
                .buttonStyle(.borderedProminent)
                .padding()
            }
            .navigationTitle("Snoozed")
        }
        .onAppear {
            // PRELOAD CACHE HERE
            items = LocalSnoozedCache.shared.load()
        }
    }

    private func snoozeDemo() async {
        guard !isStoring else { return }
        isStoring = true
        defer { isStoring = false }

        let body = "Standup in 15 minutes"
        do {
            let summary = try await service.fetchSummary(for: body)
            let urgency = try await service.fetchUrgency(for: body)

            let iso = ISO8601DateFormatter().string(from: Date().addingTimeInterval(3600))
            try await service.storeSnoozed(.init(
                userId: "user_123", title: "Meeting", body: body,
                summary: summary, urgency: urgency, snoozeUntil: iso
            ))

            let item = SnoozedItem(
                id: UUID().uuidString,
                title: "Meeting",
                summary: summary,
                urgency: urgency,
                snoozeUntil: Date().addingTimeInterval(3600)
            )
            items.append(item)
            try? LocalSnoozedCache.shared.save(items: items)
        } catch {
            print("Snooze/store demo error:", error)
        }
    }
}

/// Small helper so `.listRowSeparator(.hidden)` compiles everywhere.
private struct HideSeparatorIfAvailable: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 15.0, *) {
            content.listRowSeparator(.hidden)
        } else {
            content
        }
    }
}
