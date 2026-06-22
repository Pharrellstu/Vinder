# DESIGN.md — Vinder (Android)

Source of truth for Vinder's design system, reflecting the actual Jetpack Compose implementation (not a web mockup). CLAUDE.md's brand-book summary is derived from this file.

## 1. Project Overview
Vinder is a native Android marketplace app (Kotlin + Jetpack Compose) for buying and selling second-hand items, in the spirit of Vinted/Marktplaats. Navigation is a single-activity, state-driven screen stack (no Compose Navigation graph) rooted in `MainActivity.kt`.

## 2. Design Tokens

### 2.1 Color (`ui/theme/Color.kt`)
| Token | Hex | Usage |
|---|---|---|
| `VinderAzure` | `#4E8098` | Primary brand color — CTAs, active nav state, selected chips, links, icon tints |
| `VinderAzureLight` | `#D6E8F5` | Hero banner gradient |
| `VinderGreen` | `#4CAF50` | Positive/success accents |
| `VinderAmber` | `#FFC107` | Ratings/stars, secondary Material color |
| `VinderError` | `#D32F2F` | Destructive actions (delete, log out), error text — always reference this token |
| `Grey97` | `#F5F6F8` | App/screen background |
| `Grey95` | `#F0F0F5` | Search bar / search-results background, unselected chip fill |
| `Grey94` | `#EFEFF2` | Subtle surface fill |
| `Grey91` | `#E5E5EA` | Borders, dividers, image placeholder background |
| `Grey57` | `#8E8E93` | Secondary text (timestamps, sizes, brand labels) |
| `Grey36` | `#5A5A60` | Tertiary text (stat labels, seller meta) |
| `Grey11` | `#1C1C1E` | Primary text |
| `Grey82` | `#D1D1D6` | Filter pill unselected border, bottom-sheet drag handle |
| `inputColor` | `#E5EEF3` | Text input fill on auth screens |
| `grayColor` | `#D9D9D9` | Misc borders (auth screens, filter chips) |
| `boxDivColor` | `#FDF8F8` | Auth card background |
| `StatusPendingBg` | `#FFF8E1` | Offer status badge background — pending |
| `StatusPendingText` | `#F57F17` | Offer status badge text — pending |
| `StatusAcceptedBg` | `#E8F5E9` | Offer status badge background — accepted |
| `StatusAcceptedText` | `#2E7D32` | Offer status badge text — accepted |
| `StatusRejectedBg` | `#FFEBEE` | Offer status badge background — rejected |
| `StatusRejectedText` | `#C62828` | Offer status badge text — rejected |
| `StatusCancelledBg` | `#F5F5F5` | Offer status badge background — cancelled (text uses `Grey57`) |

Surfaces use plain white (`Color.White`); there's no dedicated "Surface" token yet.

### 2.2 Typography (`ui/theme/Type.kt`, `Font.kt`)
Single font family app-wide: **Inter** (`inter`, one static weight file — Compose synthesizes Bold/SemiBold). Mapped into Material3 `Typography`:

| Style | Size | Weight | Notes |
|---|---|---|---|
| `headlineLarge` | 28sp | Normal | |
| `titleLarge` | 24sp | SemiBold | |
| `titleMedium` | 16sp | SemiBold | |
| `bodyLarge` | 13sp | Normal | |
| `bodySmall` | 11sp | Normal | |
| `labelLarge` | 13sp | Bold | |
| `labelSmall` | 10sp | Normal | letter-spacing 0.8sp |

**Instrument Serif** (`instrumentSerifNormal`, italic variant available) is reserved exclusively for the "Vinder" wordmark logo:
- Auth screens (Login/Register/ForgotPassword): 56sp, italic, `VinderAzure`, default weight.
- HomeScreen top bar: 26sp, italic, `VinderAzure`, **SemiBold** (the one place the logo is bolder, to hold up at small size in the app bar).

Many screens set `fontFamily`/`fontSize`/`fontWeight` inline on `Text` rather than pulling a named `MaterialTheme.typography` style — the type scale above is the intent, but per-screen sizes drift somewhat in practice.

### 2.3 Shape (`ui/theme/Shape.kt`)
- `roundedInputShape` = `RoundedCornerShape(20.dp)` — text inputs.
- Pills (category chips, search "Spring Drop" badge, chat bubbles): `RoundedCornerShape(999.dp)`.
- Cards/images: `10–16.dp` corner radius depending on component (see §4).
- Buttons: `8–12.dp` corner radius.

### 2.4 Spacing
- Screen horizontal padding: **16dp** (most screens).
- Section/group gaps: **20dp**.
- Card/grid internal padding: **10dp**.
- Tight/chip padding: **4–6dp**.
- Bottom nav bottom inset: **22dp** (clears the system nav bar).
- Standard tappable/input height: **46–50dp**.

## 3. Navigation Structure
`MainActivity.kt` drives a 5-tab bottom nav (Home, Search, Sell, Inbox, Profile) plus a stack of boolean-flagged full-screen overlays layered on top of the current tab (each with its own `BackHandler`):

```
Tabs: Home(0) · Search(1) · Sell FAB(2, modal) · Inbox(3) · Profile(4)
Overlays (stack, most specific on top):
  ItemDetail → SellerPublicProfile → Chat
  EditProfile / Settings → NotificationSettings
  MyListings / Offers / OrderHistory / Wishlist
```

The center FAB (tab 2) opens `AddProductScreen` as a modal rather than selecting a tab.

## 4. Reusable Components (`ui/components/`)

- **`BottomNavBar`** — Home/Search/Inbox/Profile as Material outlined icons + 10sp label; selected = `VinderAzure` + Bold, unselected = `Grey57` + Normal. Center 50dp circular FAB (`VinderAzure`, white plus icon, 8dp shadow). Unread-message badge: 16dp `VinderAzure` circle, caps at "9+". Bar background: white at 96% alpha with a `Grey91` top divider.
- **`GridProductCard`** (2-col home/wishlist grid) — 10dp corner image (215dp tall, `Grey91` placeholder fill), 28dp circular favorite toggle (white 92% alpha), 4dp-radius `VinderAzure` discount badge, price 14sp Bold, name 12sp/2 lines, size+brand 11sp `Grey57`, seller row with 16dp `InitialAvatar` + 10sp name + 9sp rating.
- **`SaleProductCard`** — fixed 130dp-wide horizontal-carousel variant of the grid card (160dp image).
- **`SearchResultCard`** — search-grid variant, 128dp image, slightly tighter padding/type.
- **`HeroBanner`** — gradient row (`VinderAzureLight` → white → `VinderAzureLight`), 16dp corners, `Grey91` border; "SPRING DROP" pill badge; heading + subtitle; `VinderAzure` "Sell something →" CTA (8dp corners); two rotated 72×96dp placeholder images for visual interest.
- **`SellerProfileCard`** / **`InitialAvatar`** — circular avatar with the initial letter (white text on `VinderAzure`, sized at ~40% of the avatar box; default 44dp, 16dp in compact contexts). Card: white, 12dp corners, `Grey91` border. Header row: avatar + name (15sp SemiBold) + verified badge + chevron. Rating row uses `VinderAmber` star. Stats row uses grey-tinted icons. "Message seller" is an outlined `VinderAzure` button.
- **`SettingsComponents`** — grouped white sections (14dp corners) with uppercase grey section titles; each row has a 34dp `Grey97` icon badge (`VinderAzure` tint), title, optional subtitle, and a trailing chevron or switch.
- **`FilterBottomSheet` / `FilterPillChip`** — modal sort/filter sheet and pill-shaped filter chips.

## 5. Key Screens

- **HomeScreen** — `Grey97` background; top bar holds the SemiBold Instrument-Serif logo, search toggle, and notification bell; below it: category pill row → hero banner → "On sale today" horizontal carousel → "Latest finds" product grid, in a single `LazyColumn`.
- **ItemDetailScreen** — full-width `HorizontalPager` image gallery, info column (price/title/description/stats), `SellerProfileCard`, and a sticky two-button bar: outlined "Make Offer" + filled "Buy Now".
- **SearchResultsScreen** — `Grey95` background; custom top bar with inline query field, sort/filter bar, `FilterBottomSheet` modal, results grid of `SearchResultCard`.
- **MyListingsScreen** — top app bar with overflow menu, grid of the seller's own listings, delete confirmation dialog, snackbar errors.
- **ChatScreen** — top bar with seller name/avatar; message list with pill-shaped bubbles (`VinderAzure` for own messages, `Grey95` for the other party); input row with photo-attach + circular send button.
- **SettingsScreen** — grouped `SettingsComponents` sections: Account, Preferences, Support, About.
- **ProfileScreen** — header (avatar, name, rating, stats) + `ScrollableTabRow` (Listings/Sold/Reviews) over a `LazyVerticalGrid`.
- **OffersScreen** — list of incoming offers with seller info, item thumbnail, offered price, and a status badge (pending/accepted/rejected/cancelled).
- **AddProductScreen** — multi-step listing form (title → category → condition → description → photos → price), each step gated behind a `VinderAzure` Continue/Submit button.
- **Auth screens** (Login/Register/ForgotPassword) — shared pattern: centered ~380dp card (`boxDivColor` fill, 1dp 10%-black border, 15–16dp corners) on a white background; 56sp italic Instrument-Serif logo; inputs use `inputColor` fill + `roundedInputShape` (20dp); primary action is a full-width filled `VinderAzure` button, secondary actions are `VinderAzure`-outlined.

