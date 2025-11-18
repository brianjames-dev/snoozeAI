import SwiftUI

struct SnoozedDashboardView: View {
    enum LoadState: Equatable {
        case idle
        case loading
        case loaded
        case error(String)
    }

    @State private var items: [SnoozedItem] = []
    @State private var loadState: LoadState = .idle
    @State private var isPresentingSheet = false
    @State private var bannerMessage: String?
    @State private var lastRefresh: Date?
    @Environment(\.scenePhase) private var scenePhase

    private let service = SnoozeService.shared

    var body: some View {
        NavigationView {
            ZStack(alignment: .top) {
                content
                if let bannerMessage {
                    banner(text: bannerMessage)
                        .transition(.move(edge: .top))
                        .padding()
                }
            }
            .navigationTitle("Snoozed")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button {
                        Task { await refreshFromServer(showLoader: false) }
                    } label: {
                        Label("Refresh", systemImage: "arrow.clockwise")
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        isPresentingSheet = true
                    } label: {
                        Label("New Snooze", systemImage: "plus.circle.fill")
                    }
                }
            }
        }
        .sheet(isPresented: $isPresentingSheet) {
            NewSnoozeSheet { newItem in
                insertItem(newItem)
                Task { await refreshFromServer(showLoader: false) }
            }
        }
        .onAppear {
            if items.isEmpty {
                items = LocalSnoozedCache.shared.load()
            }

            if case .idle = loadState {
                Task { await refreshFromServer(showLoader: items.isEmpty) }
            }
        }
        .onChange(of: scenePhase) { phase in
            guard phase == .active else { return }
            attemptPassiveRefresh()
        }
    }

    @ViewBuilder
    private var content: some View {
        switch (loadState, items.isEmpty) {
        case (.loading, true):
            ProgressView("Loading snoozes…")
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        case (.error(let message), true):
            VStack(spacing: 12) {
                Image(systemName: "wifi.exclamationmark")
                    .font(.largeTitle)
                Text("Couldn’t Reach Backend")
                    .font(.headline)
                Text(message)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Button("Retry") {
                    Task { await refreshFromServer(showLoader: true) }
                }
                .buttonStyle(.borderedProminent)
            }
            .multilineTextAlignment(.center)
            .padding()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        default:
            listView
        }
    }

    private var listView: some View {
        Group {
            if items.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "clock.badge.questionmark")
                        .font(.largeTitle)
                    Text("No Snoozes Yet")
                        .font(.headline)
                    Text("Tap the + button to snooze your first notification.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
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
                    .modifier(HideSeparatorIfAvailable())
                }
                .listStyle(.plain)
                .refreshable {
                    await refreshFromServer(showLoader: false)
                }
            }
        }
    }

    private func insertItem(_ item: SnoozedItem) {
        items.insert(item, at: 0)
        persistCache()
    }

    private func attemptPassiveRefresh() {
        let threshold = Date().addingTimeInterval(-120)
        if let lastRefresh, lastRefresh > threshold { return }
        Task { await refreshFromServer(showLoader: false) }
    }

    @MainActor
    private func refreshFromServer(showLoader: Bool) async {
        if showLoader {
            loadState = .loading
        }
        do {
            let remote = try await service.fetchItems()
            items = merge(local: items, remote: remote)
            persistCache()
            loadState = .loaded
            bannerMessage = nil
        } catch {
            if items.isEmpty {
                loadState = .error("Please ensure the backend and Firestore are configured.")
            } else {
                bannerMessage = "Refresh failed. Tap retry."
            }
            print("Fetch items error:", error)
        }
        lastRefresh = Date()
    }

    private func merge(local: [SnoozedItem], remote: [SnoozedItem]) -> [SnoozedItem] {
        var merged = Dictionary(uniqueKeysWithValues: local.map { ($0.id, $0) })
        for item in remote {
            merged[item.id] = item
        }
        return merged.values.sorted { $0.snoozeUntil > $1.snoozeUntil }
    }

    private func persistCache() {
        try? LocalSnoozedCache.shared.save(items: items)
    }

    private func banner(text: String) -> some View {
        Text(text)
            .font(.footnote)
            .foregroundColor(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color.orange.opacity(0.9))
            .clipShape(Capsule())
    }
}

private struct HideSeparatorIfAvailable: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 15.0, *) {
            content.listRowSeparator(.hidden)
        } else {
            content
        }
    }
}
