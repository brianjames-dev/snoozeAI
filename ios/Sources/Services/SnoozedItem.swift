import Foundation

struct SnoozedItem: Identifiable, Codable, Equatable {
    let id: String
    let title: String
    let body: String
    let summary: String
    let urgency: Double
    let snoozeUntil: Date

    init(
        id: String = UUID().uuidString,
        title: String,
        body: String,
        summary: String,
        urgency: Double,
        snoozeUntil: Date
    ) {
        self.id = id
        self.title = title
        self.body = body
        self.summary = summary
        self.urgency = urgency
        self.snoozeUntil = snoozeUntil
    }

    func updating(urgency newUrgency: Double) -> SnoozedItem {
        SnoozedItem(
            id: id,
            title: title,
            body: body,
            summary: summary,
            urgency: newUrgency,
            snoozeUntil: snoozeUntil
        )
    }
}
