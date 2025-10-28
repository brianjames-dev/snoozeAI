import Foundation

struct SummaryResponse: Decodable { let summary: String }
struct ClassifyResponse: Decodable { let urgency: Double; let label: String }

struct StorePayload: Encodable {
    let userId: String
    let title: String
    let body: String
    let summary: String
    let urgency: Double
    let snoozeUntil: String // ISO8601
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

        _ = try await withRetry {
            try await URLSession.shared.data(for: req)
        }
    }
}
