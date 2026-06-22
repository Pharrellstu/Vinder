# Vinder — UML Diagrams

Diagram-as-code (PlantUML) for the Vinder marketplace app. Edit the `.puml`
text files and re-render — the images stay in sync, no manual redraw.

| File | What it is | Jira |
|------|------------|------|
| `usecase.puml` → `Vinder-UseCase.png/.svg` | Use Case diagram — who can do what | ADV-157 |
| `component.puml` → `Vinder-Component.png/.svg` | Component diagram — software building blocks & dependencies | ADV-158 |

**Scope:** the full *target* product (how we want Vinder to look — Marktplaats-style),
not only what's coded today.

## Rendering

PlantUML needs Java; Graphviz gives nicer layout.

```bash
# one-time install (macOS)
brew install plantuml graphviz

# render every .puml in this folder to PNG + SVG
plantuml -tpng *.puml
plantuml -tsvg *.puml
```

No local install? Two easy options:
- **VS Code**: install the "PlantUML" extension, open a `.puml`, `Alt+D` to preview.
- **Browser**: paste a file's contents into <https://www.plantuml.com/plantuml> or <https://www.planttext.com>.

If `dot` (Graphviz) isn't available, add `-Playout=smetana` to use the pure-Java
layout engine (slightly messier, no Graphviz needed).

---

## What these two diagrams are (quick primer)

**Use Case diagram** — a *behavioural* view. Stick-figure **actors** (roles, not
people) sit outside a **system boundary**; ovals inside are **use cases** (goals
the system fulfils). A line = "this actor can do this". Dashed arrows are
`<<include>>` (a use case always pulls in another — *Create listing* includes
*Upload photos*) or `<<extend>>` (optional add-on — *Filter unread* extends
*View inbox*). The hollow-triangle arrow between actors is generalisation:
**User → Visitor** means a logged-in User can do everything a Visitor can, plus
more — which is why we don't redraw the public use cases for both. Answers
**"what can the system do, and for whom?"**

**Component diagram** — a *structural* view. Boxes are **components**:
replaceable chunks of software (a screen module, a backend service, a library,
the database). Arrows are **dependencies** ("uses / talks to"). The little
circle-on-a-stick ("lollipop") is a **provided interface** and the matching
dependency is a **required interface** — here the UI state plugs into the
`Repository API`. Answers **"what are the moving parts and how do they fit?"**
It reads top-to-bottom in layers: UI → state → repository → data sources →
backend services → database.

> **Coil** (in "Image Loader (Coil)") is the Kotlin image-loading library the
> app already uses (`io.coil-kt.coil3`) to load product photos via `AsyncImage`.

---

## Astah cheat-sheet — Use Case diagram

Recreate `usecase.puml` in Astah with these elements.

**Actors:**
- Visitor — not logged in
- User — registered; draw a generalisation arrow **User ▷ Visitor** (hollow triangle pointing at Visitor)
- Payment Gateway, Push Notification Service, Device Camera & Gallery (system actors, on the right)

**System boundary:** rectangle labelled *Vinder App* containing all use cases.

**Use cases by group:**
- *Authentication:* Register account, Log in, Verify account (2FA), Reset password
- *Discovery & Search:* Browse home feed, Filter by category, Search items, Filter & sort results, View item detail, Save to favourites
- *Buying & Selling:* Buy now, Make an offer, Complete checkout, Track order, Create listing, Upload photos, Manage my listings
- *Messaging:* View inbox, Filter unread, Chat with user
- *Profile:* View profile & stats, Edit profile, Manage settings

**Associations:**
- Visitor → Register account, Log in, Browse home feed, Search items, View item detail
- User → Reset password, Save to favourites, Buy now, Make an offer, Track order, Create listing, Manage my listings, View inbox, Chat with user, View profile & stats, Edit profile, Manage settings
  *(User also inherits all of Visitor's use cases via the generalisation — no need to redraw them.)*

**`<<include>>`** (dashed, arrow → included case):
- Log in → Verify account (2FA) · Register account → Verify account (2FA)
- Buy now → Complete checkout · Create listing → Upload photos

**`<<extend>>`** (dashed, arrow → base case):
- Filter by category → Browse home feed · Filter & sort results → Search items · Filter unread → View inbox

**System-actor links:** Complete checkout → Payment Gateway · Upload photos →
Device Camera & Gallery · Chat with user → Push Notification Service.
