# Vinder UML Diagrams

PlantUML sources. Edit the `.puml` files and re-render.

| File | Diagram |
|------|---------|
| `usecase.puml` | Use case (`Vinder-UseCase.png/.svg`) |
| `component.puml` | Component (`Vinder-Component.png/.svg`) |
| `architecture.puml` | System architecture (`Vinder-Architecture.png/.svg`) |

Two different scopes, on purpose:
- `usecase.puml` and `component.puml` describe the **target product**, not only what's coded today (checkout/payments not built yet).
- `architecture.puml` describes the **as-built system today**: a Jetpack Compose client talking directly to Supabase Cloud (Auth · Postgrest · Realtime · Storage · Functions) via the supabase-kt SDK. No custom backend/microservices; notifications are Realtime-driven (foreground service + local notifications), not FCM.

## Render

```bash
brew install plantuml graphviz
plantuml -tpng -tsvg *.puml
```

No install: paste a file into https://www.plantuml.com/plantuml.
