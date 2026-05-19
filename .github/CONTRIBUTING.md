# Contributing to class-service

## Branch Strategy

```
main (production)
  └── develop (integration)
        ├── feature/track-1-db-foundation
        ├── feature/track-2-api-core
        ├── feature/track-3-billing
        ├── feature/track-4-frontend-foundation
        ├── feature/track-5-frontend-core
        └── feature/track-6-frontend-billing
```

### Branch naming

| Type | Pattern | Example |
|---|---|---|
| Feature | `feature/track-N-short-description` | `feature/track-2-student-import` |
| Bug fix | `fix/short-description` | `fix/null-tenant-on-login` |
| Hotfix | `hotfix/short-description` | `hotfix/jwt-expiry-crash` |
| Chore | `chore/short-description` | `chore/update-dependencies` |

### Branch protection

**`main`** (production-ready only):
- All commits must come via a PR from `develop`
- PR requires at least 1 approving review
- All CI checks must pass before merging
- No force push
- No direct commits

**`develop`** (integration branch):
- All commits must come via a PR from a feature/fix/hotfix branch
- PR requires at least 1 approving review
- All CI checks must pass before merging
- No force push

### Pull request checklist

Before opening a PR, ensure:

- [ ] Branch is up to date with its target (`develop` or `main`)
- [ ] All CI checks pass locally (`mvn test` and `npm test`)
- [ ] No TypeScript errors (`npx tsc --noEmit` in `frontend/`)
- [ ] New code has appropriate test coverage
- [ ] PR description clearly explains WHAT changed and WHY
- [ ] Breaking changes are noted in the PR description

### Merge order (track dependencies)

```
Track 1 → develop
Track 4 → develop (can merge parallel with Track 1)
Track 2 → develop (requires Track 1)
Track 5 → develop (requires Track 4 + Track 2)
Track 3 → develop (requires Track 2)
Track 6 → develop (requires Track 5 + Track 3)
```

### Commit message convention

```
<type>(<scope>): <short summary>

Examples:
feat(auth): add JWT token refresh endpoint
fix(billing): correct cancelled-session exclusion in bill calculation
chore(deps): update JJWT to 0.12.6
test(students): add Excel import edge case tests
```

Types: `feat`, `fix`, `chore`, `test`, `docs`, `refactor`, `perf`, `build`
