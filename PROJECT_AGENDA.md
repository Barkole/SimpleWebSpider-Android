# SimpleWebSpider-Android - Project Modernization Agenda

## Status: IN PROGRESS

### Phase 1: Modernization ✅
- [ ] Upgrade Gradle plugin to 8.x
- [ ] Upgrade Android SDK versions (minSdk, targetSdk)
- [ ] Update dependencies (jsoup, support libs)
- [ ] Fix deprecated APIs

### Phase 2: CI/CD Setup
- [ ] Create release workflow (build APK + semver)
- [ ] Create dependency update workflow (Dependabot alternative)
- [ ] Setup versioning system

### Key Versions to Update
- **Gradle Plugin**: 3.1.4 → 8.2.0
- **CompileSdk**: 24 → 34
- **TargetSdk**: 21 → 34
- **MinSdk**: 14 → 21
- **BuildTools**: 27.0.3 → 34.0.0

### Important Links & Configs
- Repo: https://github.com/Barkole/SimpleWebSpider-Android
- Language: Java
- License: MIT
- Default Branch: master

### CI/CD Workflows to Create
1. `build-and-release.yml` - Builds APK, creates GitHub releases with semver
2. `update-dependencies.yml` - Auto-updates deps, tests, auto-merges if successful

### Next Actions
- Commit each completed phase separately
- Test workflows before final merge
