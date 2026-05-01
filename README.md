# attendance

考勤管理系統

## Quick Start

1. **Read CLAUDE.md first** - Contains essential rules for Claude Code
2. Follow the pre-task compliance checklist before starting any work
3. Use proper module structure under `apps/`
4. Commit after every completed task

## Monorepo Project Structure

```
attendance/
├── apps/
│   ├── frontend/          # Frontend app (React + Vite + Tailwind CSS)
│   └── backend/           # Backend app (Spring Boot / Java 17+)
├── packages/              # Shared packages/libraries
├── infrastructure/        # Deployment and infrastructure config
├── docs/                  # Global project documentation
├── openspec/              # SDD specs and plans (Single Source of Truth)
└── CLAUDE.md              # Essential rules for Claude Code
```

## Development Guidelines

- **Always search first** before creating new files
- **Extend existing** functionality rather than duplicating
- **Use Task agents** for operations >30 seconds
- **Single source of truth** for all functionality
- **TDD workflow** - write tests before implementation
- **Commit after every completed task**
