# SimpleWebSpider-Android - CI/CD Setup

## 📋 Tasks Status

### Phase 1: Modernization (In Progress)
- [ ] Update Android Gradle Plugin to 8.x
- [ ] Update targetSdkVersion to 34
- [ ] Update buildToolsVersion to 34.0.0
- [ ] Update Support Libraries to AndroidX

### Phase 2: CI/CD Workflows
- [ ] Build & Release Workflow (auto APK + semver)
- [ ] Dependency Update Workflow (auto-merge)
- [ ] Quality Checks (build verification)

### Phase 3: Testing & Validation
- [ ] Verify build succeeds
- [ ] Test APK generation
- [ ] Validate semver tagging

---

## 🔧 Key Configuration

**Current State:**
- Build Tools: 3.1.4 (3 years old)
- Target SDK: 21 (API level)
- Min SDK: 14

**Target State:**
- Build Tools: 8.x (latest)
- Target SDK: 34 (current Android)
- Min SDK: 21 (minimum recommended)

---

## 📝 Important Instructions

1. **Semver Releases**: Uses GitHub Releases with tags like `v1.0.0`
2. **APK Location**: Built to `app/build/outputs/apk/release/`
3. **Dependency Updates**: Uses Dependabot via workflow
4. **Branch Protection**: Main branch, auto-merge only on passing tests

---

## 💾 Commits Made

- None yet (starting setup)

---

## 🔗 Related Files

- `.github/workflows/build-release.yml` - Build & Release
- `.github/workflows/deps-update.yml` - Dependency updates
- `build.gradle` - Top-level Gradle config
- `app/build.gradle` - App-level Gradle config

---

*Last Updated: 2026-05-03*
