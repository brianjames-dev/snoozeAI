import Foundation

@MainActor
final class LocalSnoozedCache {
    static let shared = LocalSnoozedCache()
    private init() {}

    private var url: URL {
        let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        return dir.appendingPathComponent("snoozed_cache.json")
    }

    private let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }()

    private let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        encoder.outputFormatting = [.prettyPrinted]
        return encoder
    }()

    func load() -> [SnoozedItem] {
        guard let data = try? Data(contentsOf: url) else { return [] }
        guard let decoded = try? decoder.decode([SnoozedItem].self, from: data) else { return [] }
        return decoded
    }

    func save(items: [SnoozedItem]) throws {
        let data = try encoder.encode(items)
        try data.write(to: url, options: .atomic)
    }
}
