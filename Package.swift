// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorCommunityGenericOauth2",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "CapacitorCommunityGenericOauth2",
            targets: ["GenericOAuth2Plugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "6.0.0"),
        .package(url: "https://github.com/OAuthSwift/OAuthSwift.git", from: "2.2.0")
    ],
    targets: [
        .target(
            name: "GenericOAuth2Plugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm"),
                .product(name: "OAuthSwift", package: "OAuthSwift")
            ],
            path: "ios/Sources"
        ),
        .testTarget(
            name: "GenericOAuth2PluginTests",
            dependencies: ["GenericOAuth2Plugin"],
            path: "ios/Tests"
        )
    ]
)
