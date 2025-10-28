import SwiftUI

@main
struct AINotificationAgentApp: App {
    var body: some Scene {
        WindowGroup {
            TabView {
                HomeView()
                    .tabItem { Label("Home", systemImage: "house") }
                SnoozedDashboardView()
                    .tabItem { Label("Snoozed", systemImage: "clock") }
            }
        }
    }
}
