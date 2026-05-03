# SimpleWebSpider-Android - Project Modernization Agenda

## Status: COMPLETED

### Phase 1: Modernization ✅
- [x] Upgrade Gradle wrapper to 8.5
- [x] Upgrade Android Gradle Plugin to 8.2.0
- [x] Upgrade Android SDK versions (minSdk 21, targetSdk 34, compileSdk 34)
- [x] Migrate to AndroidX (support-v4 → appcompat)
- [x] Update dependencies (jsoup to 1.17.1)
- [x] Remove deprecated uses-sdk from manifest
- [x] Add namespace to build.gradle

### Phase 2: CI/CD Setup ✅
- [x] Create GitHub Actions workflow for building APK on every commit
- [x] Create release workflow that uploads APK as downloadable release
- [x] Setup automatic semver using gradle-semantic-build-versioning plugin
- [x] Configure Dependabot for dependency updates
- [x] Add auto-merge workflow for Dependabot PRs if CI passes

### Key Versions Updated
- **Gradle**: 4.4 → 8.5
- **Gradle Plugin**: 3.1.4 → 8.2.0
- **CompileSdk**: 24 → 34
- **TargetSdk**: 21 → 34
- **MinSdk**: 14 → 21
- **BuildTools**: Removed (managed by AGP)
- **Support Libs**: Migrated to AndroidX

### CI/CD Workflows Created
1. `.github/workflows/android.yml` - Builds APK on push/PR, creates release on main with APK
2. `.github/workflows/auto-merge.yml` - Auto-merges Dependabot PRs if CI passes
3. `.github/dependabot.yml` - Weekly dependency updates for Gradle and GitHub Actions

### Versioning
- Uses `net.vivin.gradle-semantic-build-versioning` plugin
- Version based on git tags (e.g., v1.0.0)
- SNAPSHOT for dirty builds
- Releases created with clean version tags

### Important Links & Configs
- Repo: https://github.com/Barkole/SimpleWebSpider-Android
- Language: Java
- License: MIT
- Default Branch: main (assuming renamed from master)

### Maintenance
- Every commit to main builds APK and creates release
- Dependencies auto-updated weekly via Dependabot
- Successful updates auto-merged
- Semver updated automatically based on conventional commits/tags
