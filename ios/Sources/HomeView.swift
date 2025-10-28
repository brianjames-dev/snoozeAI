import SwiftUI
import UserNotifications

struct HomeView: View {
    var body: some View {
        VStack(spacing: 16) {
            Text("Home works ✅").font(.title).padding()
            Button("Request Notification Permission") {
                UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _,_ in }
            }
            Button("Send Local Notification (Extension Test)") {
                let content = UNMutableNotificationContent()
                content.title = "Meeting"
                content.body = "Standup in 15 minutes with the iOS team. Join via Zoom link in invite."
                // set your extension category if needed later
                let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 2, repeats: false)
                let req = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger)
                UNUserNotificationCenter.current().add(req, withCompletionHandler: nil)
            }
        }
    }
}
