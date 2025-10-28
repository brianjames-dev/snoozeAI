import Foundation
import FirebaseFirestore

struct SnoozedItem: Identifiable {
    let id: String
    let title: String
    let summary: String
    let urgency: Double
    let snoozeUntil: Date
}

final class FirestoreClient {
    static let shared = FirestoreClient()
    private let db = Firestore.firestore()

    func fetchSnoozed(completion: @escaping ([SnoozedItem]) -> Void) {
        db.collection("snoozed_notifications")
            .order(by: "snoozeUntil", descending: false)
            .limit(to: 50)
            .getDocuments { snap, err in
                guard let docs = snap?.documents, err == nil else { completion([]); return }
                let items: [SnoozedItem] = docs.compactMap { d in
                    let data = d.data()
                    guard let title = data["title"] as? String,
                          let summary = data["summary"] as? String,
                          let urgency = data["urgency"] as? Double,
                          let ts = data["snoozeUntil"] as? Timestamp else { return nil }
                    return SnoozedItem(id: d.documentID, title: title, summary: summary,
                                       urgency: urgency, snoozeUntil: ts.dateValue())
                }
                completion(items)
            }
    }
}
