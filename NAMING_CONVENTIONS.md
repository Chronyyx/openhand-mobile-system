# Pull Request Naming Convention

All pull requests **must** follow this naming format:


**Rules**
- **Pull Requests**: Must follow the format `type(OP-123): Description`.
  - Allowed types: `feat`, `fix`, `bug`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.
  - Example: `feat(OP-71): Implement Enhanced Auth`
- **Branches**: Must follow the format `type/OP-123_Description`.
  - Allowed types: `feat`, `bug`, `fix`, `hotfix`, `chore`.
  - Example: `feat/OP-71_Enhanced_Auth`
- PRs that do not follow this format will fail the validation check.
