# DESIGN.md - Marketplace Application

## 1. Project Overview
A high-fidelity, responsive marketplace web application focused on buying and selling second-hand items. The layout mimics the proven UX/UI paradigms of established platforms like Vinted and Marktplaats, structured with a custom color scheme.

## 2. Color Palette
- **Primary Brand Color**: `#4E8098` (Deep Teal Blue) - Applied to primary call-to-action buttons, active navigation states, and crucial UI highlights.
- **Secondary/Accent Color**: `#90C2E7` (Light Sky Blue) - Applied to hover states, secondary buttons, promotional badges, and subtle background accents.
- **Background Color**: `#F5F6F8` - A standard off-white/gray application background designed to create depth between the canvas and interactive cards.
- **Surface Color**: `#FFFFFF` - White for item cards, dropdown menus, and modal dialogs.
- **Text Colors**: 
  - Primary Text: `#1C1C1E` (Nearly black for optimal contrast)
  - Secondary Text: `#8E8E93` (Muted gray for timestamps, brands, and sizes)
- **Borders/Dividers**: `#E5E5EA`

## 3. Typography
- **Font Family**: Primary system sans-serif stack (Inter, Roboto, or San Francisco) to maintain a modern, native feel.
- **Headings**: Bold, tight tracking.
- **Body/UI**: Base size 14px-16px. 

## 4. Layout & Architecture

### 4.1. Global Header
- **Top Bar (Sticky)**:
  - **Logo**: Left-aligned, utilizing `#4E8098`.
  - **Search Bar**: Centered, expansive, with a pill-shaped or slightly rounded rectangle design. Light gray background `#F0F0F5` with a subtle search icon.
  - **User Actions**: Right-aligned.
    - Login/Register buttons.
    - **Primary CTA**: "Sell now" button (Solid `#4E8098` background, white text, slightly rounded corners).
    - System icons: Messages, Notifications, Profile avatar.
- **Category Navigation**:
  - Horizontal scrollable bar below the top header containing categories (Women, Men, Kids, Home, Electronics).
  - Active states underlined or highlighted in `#4E8098`.

### 4.2. Hero Section
- A full-width banner container (accounting for side margins) featuring lifestyle imagery or illustrations.
- Typography overlay with a clear value proposition (e.g., "Ready to declutter?").
- The background gradient or graphic elements should heavily feature `#90C2E7` to establish the brand identity immediately.

### 4.3. Main Content Feed
- **Feed Layout**: CSS Grid setup (`grid-template-columns: repeat(auto-fill, minmax(220px, 1fr))`) ensuring responsive scaling from mobile to ultra-wide displays.
- **Item Cards**:
  - **Media**: Image thumbnail maintaining a consistent aspect ratio (e.g., 4:5). Full width of the card.
  - **Overlays**: A heart icon floating in the top right corner for "Favorite" actions.
  - **Metadata Row 1**: Bold pricing (e.g., "€25.00").
  - **Metadata Row 2**: Size and Brand (Secondary Text color).
  - **Seller Info**: Small avatar and username underneath or overlaid on the bottom edge of the image.

### 4.4. Global Footer
- Multi-column layout (Grid or Flexbox).
- Sections: App links (About, Careers), Help & Support, Privacy & Terms, Social Media links.
- Background: Plain white or subtly tinted `#EAF3FA`.

## 5. State Management & Interactions
- **Hover Effects**: Item cards elevate with a slight drop shadow (`box-shadow: 0 4px 12px rgba(0,0,0,0.08)`) and a subtle `-2px` Y-axis translation.
- **Button States**: Primary buttons shift to `#90C2E7` or a darker shade of `#4E8098` upon hover/focus.
- **Loading Mechanisms**: Implement skeleton loaders mirroring the card dimensions prior to DOM hydration.

## 6. Development & Implementation Notes
- **Design Tokens**: Map all colors to CSS variables (e.g., `--color-primary: #4E8098`) to streamline theming and potential dark mode implementation.
- **Mobile Paradigm**: Shift the "Sell now" button to a fixed bottom navigation bar or a prominent Floating Action Button (FAB) on viewports `< 768px`. The category header should collapse into a horizontal swiping pill list or a hamburger menu.

