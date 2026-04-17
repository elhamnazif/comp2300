import UIKit
import SwiftUI
import clientKit

private let statusBarAppearanceDidChange = Notification.Name("com.group8.comp2300.statusBarAppearanceDidChange")
private let darkIconsStatusBarPayload = "dark-icons"
private let lightIconsStatusBarPayload = "light-icons"

final class StatusBarHostingController: UIViewController {
    private let composeViewController = MainViewControllerKt.MainViewController()
    private var currentStatusBarStyle: UIStatusBarStyle = .default

    override func viewDidLoad() {
        super.viewDidLoad()

        currentStatusBarStyle = defaultStatusBarStyle()
        view.backgroundColor = .clear

        addChild(composeViewController)
        composeViewController.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(composeViewController.view)
        NSLayoutConstraint.activate([
            composeViewController.view.topAnchor.constraint(equalTo: view.topAnchor),
            composeViewController.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            composeViewController.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            composeViewController.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        composeViewController.didMove(toParent: self)

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleStatusBarAppearanceChange(_:)),
            name: statusBarAppearanceDidChange,
            object: nil
        )
    }

    override var preferredStatusBarStyle: UIStatusBarStyle {
        currentStatusBarStyle
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    @objc
    private func handleStatusBarAppearanceChange(_ notification: Notification) {
        guard let payload = notification.object as? String else { return }

        switch payload {
        case darkIconsStatusBarPayload:
            currentStatusBarStyle = .darkContent
        case lightIconsStatusBarPayload:
            currentStatusBarStyle = .lightContent
        default:
            currentStatusBarStyle = defaultStatusBarStyle()
        }

        setNeedsStatusBarAppearanceUpdate()
    }

    private func defaultStatusBarStyle() -> UIStatusBarStyle {
        traitCollection.userInterfaceStyle == .dark ? .lightContent : .darkContent
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        StatusBarHostingController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
