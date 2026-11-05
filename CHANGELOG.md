# Changelog

All notable changes of this package. Versions follow [Semantic Versioning](https://semver.org/).

## [2.3.0](https://github.com/unione-repo/unione-spring/releases/tag/v2.3.0) - 2026-11-05

### Added

- Tests of the package (offline: the API is faked, no request leaves the machine).
- A CI workflow (`.github/workflows/test.yml`) runs the tests on every push and pull request over the supported runtimes; a release is published only when they pass.
- The tests and the release of the package wait until the SDK version it requires is in the registry, so the SDK and the package can be released together.
- README: how send errors surface (the SDK error with its kind, retryable and hint).
- Configuration metadata of the properties (IDE completion). With Spring Security the webhook path gets its own `SecurityFilterChain` (no CSRF, permitAll, only this path).

### Changed

- Requires the new minor version of the SDK: its errors tell what failed, whether a repeat may succeed and how to fix an API error.

### Fixed

- The README names the SDK package.

Earlier releases: the [tags](https://github.com/unione-repo/unione-spring/tags) of the repository.
