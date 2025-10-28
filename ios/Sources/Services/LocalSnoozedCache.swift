import Foundation

struct SnoozedItemDTO: Codable, Identifiable {
    let id: String
    let title: String
    let summary: String
    let urgency: Double
    let snoozeUntil: Date

    init(_ item: SnoozedItem) {
        id = item.id
        title = item.title
        summary = item.summary
        urgency = item.urgency
        snoozeUntil = item.snoozeUntil
    }

    var toItem: SnoozedItem {
        .init(id: id, title: title, summary: summary, urgency: urgency, snoozeUntil: snoozeUntil)
    }
}

@MainActor                 // ← add this line
final class LocalSnoozedCache {
    static let shared = LocalSnoozedCache()
    private init() {}

    private var url: URL {
        let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        return dir.appendingPathComponent("snoozed_cache.json")
    }

    func load() -> [SnoozedItem] {
        guard let data = try? Data(contentsOf: url) else { return [] }
        guard let decoded = try? JSONDecoder().decode([SnoozedItemDTO].self, from: data) else { return [] }
        return decoded.map { $0.toItem }
    }

    func save(items: [SnoozedItem]) throws {
        let dtos = items.map(SnoozedItemDTO.init)
        let data = try JSONEncoder().encode(dtos)
        try data.write(to: url, options: .atomic)
    }
}
