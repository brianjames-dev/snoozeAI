import SwiftUI
import BackgroundTasks

@main
struct AINotificationAgentApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        BackgroundRefreshScheduler.shared.register()
    }

    var body: some Scene {
        WindowGroup {
            TabView {
                HomeView()
                    .tabItem { Label("Home", systemImage: "house") }
                SnoozedDashboardView()
                    .tabItem { Label("Snoozed", systemImage: "clock") }
                SettingsView()
                    .tabItem { Label("Settings", systemImage: "gearshape") }
            }
        }
        .onChange(of: scenePhase) { phase in
            guard phase == .background else { return }
            BackgroundRefreshScheduler.shared.schedule()
        }
    }
}

final class BackgroundRefreshScheduler {
    static let shared = BackgroundRefreshScheduler()
    private let identifier = "dev.brianjames.AINotificationAgent.refresh"

    func register() {
        guard #available(iOS 13.0, *) else { return }
        BGTaskScheduler.shared.register(forTaskWithIdentifier: identifier, using: nil) { task in
            self.complete(task: task)
        }
    }

    func schedule() {
        guard #available(iOS 13.0, *) else { return }
        let request = BGAppRefreshTaskRequest(identifier: identifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 60 * 60)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("BGTask schedule failed:", error)
        }
    }

    private func complete(task: BGTask) {
        task.setTaskCompleted(success: true)
        schedule()
    }
}
