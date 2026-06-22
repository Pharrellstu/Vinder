# Vinder UML Diagrams

PlantUML sources. Edit the `.puml` files and re-render.

| File | Diagram |
|------|---------|
| `usecase.puml` | Use case (`Vinder-UseCase.png/.svg`) |
| `component.puml` | Component (`Vinder-Component.png/.svg`) |

Scope is the target product, not only what's coded today (checkout/payments not built yet).

## Render

```bash
brew install plantuml graphviz
plantuml -tpng -tsvg *.puml
```

No install: paste a file into https://www.plantuml.com/plantuml.
