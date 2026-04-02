// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorCommunityGenericOauth2",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapacitorCommunityGenericOauth2",
            targets: ["GenericOAuth2Plugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "GenericOAuth2Plugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/GenericOAuth2Plugin"),
        .testTarget(
            name: "GenericOAuth2PluginTests",
            dependencies: ["GenericOAuth2Plugin"],
            path: "ios/Tests/GenericOAuth2PluginTests")
    ]
)