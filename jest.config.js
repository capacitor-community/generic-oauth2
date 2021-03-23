module.exports = {
    preset: 'ts-jest',
    verbose: true,
    // transformIgnorePatterns: [
    //     "node_modules/(?!(@capacitor))"
    // ],
    // moduleNameMapper: {
    //     // '^@capacitor/core$': '<rootDir>/node_modules/@capacitor/core/dist/esm/index.js',
    //     // '^@capacitor/core$': '<rootDir>/node_modules/@capacitor/core/dist/capacitor.js',
    //
    // },
    // transform: {
    //     "^.+\\.(ts|tsx)$": "ts-jest"
    // },
    testEnvironment: 'node',
    globals: {
        window: {}
    }
};
