const { getDefaultConfig } = require('expo/metro-config');

const config = getDefaultConfig(__dirname);

// Force zustand to use CJS instead of ESM (import.meta not supported in Metro)
config.resolver.unstable_enablePackageExports = false;

module.exports = config;
