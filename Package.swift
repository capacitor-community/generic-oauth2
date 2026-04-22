// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorCommunityGenericOauth2",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorCommunityGenericOauth2",
            targets: ["CapacitorCommunityGenericOauth2"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0"),
        .package(url: "https://github.com/OAuthSwift/OAuthSwift.git", from: "2.2.0")
    ],
    targets: [
        .target(
            name: "CapacitorCommunityGenericOauth2",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm"),
                .product(name: "OAuthSwift", package: "OAuthSwift")
            ],
            path: "ios/Sources/GenericOAuth2Plugin"),

        .testTarget(
            name: "GenericOAuth2PluginTests",
            dependencies: ["CapacitorCommunityGenericOauth2"],
            path: "ios/Tests/GenericOAuth2PluginTests")
    ]
)
