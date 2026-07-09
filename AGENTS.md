# Agent Instructions

Before making code, UI, database, architecture, or requirement changes in this repository, read:

- [Project Constitution](docs/project-constitution.md)
- [Project Change Log](docs/change-log.md)
- [Feature Map](docs/feature-map.md)

The project constitution is the highest project-level guidance for this repository. If a user request, requirement document, implementation shortcut, or design idea conflicts with the constitution, pause and ask for confirmation before proceeding.

In particular, preserve these default assumptions unless the constitution is formally amended:

- The front-stage user workspace is Picset-style and focused on AI e-commerce visual generation.
- The back-stage admin console keeps the existing Egg admin style.
- Phase one requires intelligent chat with RAG.
- Phase one requires real image generation, not prompt-only output.
- Phase one does not include points, billing, payments, tenants, team spaces, or real video generation.
- A2A is a first-phase terminal feature and must be developed after the visual-generation MVP, intelligent chat/RAG loop, asset workflow, and export flow are usable.

When making code changes:

- Add simple comments for non-obvious business logic, AI workflow logic, storage logic, and integration logic.
- Add a short, plain-language note to `docs/change-log.md`.
- Update `docs/feature-map.md` when adding or moving feature code, pages, APIs, tables, or major directories.
