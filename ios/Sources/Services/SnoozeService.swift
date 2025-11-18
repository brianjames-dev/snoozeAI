import Foundation
import UserNotifications

struct SummaryResponse: Decodable { let summary: String }
struct ClassifyResponse: Decodable { let urgency: Double; let label: String }
struct StorePayload: Encodable {
    let id: String
    let title: String
    let body: String
    let summary: String
    let urgency: Double
    let snoozeUntil: String
}
struct StoreResponse: Decodable { let ok: Bool; let id: String }

struct ItemsResponse: Decodable {
    let items: [RemoteSnoozedItem]
}

struct RemoteSnoozedItem: Decodable {
    let id: String
    let title: String
    let summary: String
    let urgency: Double
    let snoozeUntil: Date
}

// ---- Retry helper ----
func withRetry<T>(
    times: Int = 2,
    initialDelay: TimeInterval = 0.5,
    _ op: @escaping () async throws -> T
) async throws -> T {
    var attempt = 0
    var delay = initialDelay
    while true {
        do { return try await op() }
        catch {
            attempt += 1
            if attempt > times { throw error }
            try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
            delay *= 2
        }
    }
}

// ---- Service ----
@MainActor
final class SnoozeService {
    static let shared = SnoozeService()
    private init() {}

    private let isoFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    func createSnooze(
        title: String,
        body: String,
        snoozeUntil: Date,
        urgency providedUrgency: Double? = nil
    ) async throws -> SnoozedItem {
        async let summaryTask = fetchSummary(for: body)
        async let urgencyTask = fetchUrgency(for: body)

        let summary = try await summaryTask
        let urgency = providedUrgency ?? (try await urgencyTask)
        let identifier = UUID().uuidString
        let payload = StorePayload(
            id: identifier,
            title: title,
            body: body,
            summary: summary,
            urgency: urgency,
            snoozeUntil: isoFormatter.string(from: snoozeUntil)
        )
        try await storeSnoozed(payload)

        let item = SnoozedItem(
            id: identifier,
            title: title,
            body: body,
            summary: summary,
            urgency: urgency,
            snoozeUntil: snoozeUntil
        )
        scheduleLocalNotification(for: item)
        return item
    }

    func fetchItems(limit: Int = 50) async throws -> [SnoozedItem] {
        let url = API.base.appendingPathComponent("/items")
        var components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        components?.queryItems = [URLQueryItem(name: "limit", value: "\(limit)")]
        guard let finalURL = components?.url else { return [] }
        let (data, _) = try await withRetry {
            var request = URLRequest(url: finalURL)
            request.httpMethod = "GET"
            return try await URLSession.shared.data(for: request)
        }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let response = try decoder.decode(ItemsResponse.self, from: data)
        return response.items.map {
            SnoozedItem(
                id: $0.id,
                title: $0.title,
                body: $0.summary,
                summary: $0.summary,
                urgency: $0.urgency,
                snoozeUntil: $0.snoozeUntil
            )
        }
    }

    func fetchSummary(for text: String, maxTokens: Int = 80) async throws -> String {
        var req = URLRequest(url: API.base.appendingPathComponent("/summarize"))
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        struct SummarizeIn: Encodable { let text: String; let max_tokens: Int? }
        req.httpBody = try JSONEncoder().encode(SummarizeIn(text: text, max_tokens: maxTokens))

        let (data, _) = try await withRetry {
            try await URLSession.shared.data(for: req)
        }
        return try JSONDecoder().decode(SummaryResponse.self, from: data).summary
    }

    func fetchUrgency(for text: String) async throws -> Double {
        var req = URLRequest(url: API.base.appendingPathComponent("/classify"))
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        struct ClassifyIn: Encodable { let text: String }
        req.httpBody = try JSONEncoder().encode(ClassifyIn(text: text))

        let (data, _) = try await withRetry {
            try await URLSession.shared.data(for: req)
        }
        return try JSONDecoder().decode(ClassifyResponse.self, from: data).urgency
    }

    func storeSnoozed(_ payload: StorePayload) async throws {
        var req = URLRequest(url: API.base.appendingPathComponent("/store"))
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try JSONEncoder().encode(payload)

        let (data, _) = try await withRetry {
            try await URLSession.shared.data(for: req)
        }
        _ = try JSONDecoder().decode(StoreResponse.self, from: data)
    }

    private func scheduleLocalNotification(for item: SnoozedItem) {
        let content = UNMutableNotificationContent()
        content.title = "Snoozed: \(item.title)"
        content.body = item.summary
        content.sound = .default
        let triggerInterval = max(5, item.snoozeUntil.timeIntervalSinceNow)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: triggerInterval, repeats: false)
        let request = UNNotificationRequest(identifier: item.id, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }
}
