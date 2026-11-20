---
name: Obsidian Flow
colors:
  surface: '#121316'
  surface-dim: '#121316'
  surface-bright: '#38393c'
  surface-container-lowest: '#0d0e11'
  surface-container-low: '#1b1b1f'
  surface-container: '#1f1f23'
  surface-container-high: '#292a2d'
  surface-container-highest: '#343538'
  on-surface: '#e3e2e6'
  on-surface-variant: '#c6c5d5'
  inverse-surface: '#e3e2e6'
  inverse-on-surface: '#2f3034'
  outline: '#908f9e'
  outline-variant: '#454653'
  surface-tint: '#bdc2ff'
  primary: '#bdc2ff'
  on-primary: '#131e8c'
  primary-container: '#818cf8'
  on-primary-container: '#101b8a'
  inverse-primary: '#4953bc'
  secondary: '#44e2cd'
  on-secondary: '#003731'
  secondary-container: '#03c6b2'
  on-secondary-container: '#004d44'
  tertiary: '#4de082'
  on-tertiary: '#003919'
  tertiary-container: '#00ac58'
  on-tertiary-container: '#003617'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e0e0ff'
  primary-fixed-dim: '#bdc2ff'
  on-primary-fixed: '#000767'
  on-primary-fixed-variant: '#2f3aa3'
  secondary-fixed: '#62fae3'
  secondary-fixed-dim: '#3cddc7'
  on-secondary-fixed: '#00201c'
  on-secondary-fixed-variant: '#005047'
  tertiary-fixed: '#6dfe9c'
  tertiary-fixed-dim: '#4de082'
  on-tertiary-fixed: '#00210c'
  on-tertiary-fixed-variant: '#005227'
  background: '#121316'
  on-background: '#e3e2e6'
  surface-variant: '#343538'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 56px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.03em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.03em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-mobile: 0.75rem
  margin: 1rem
  margin-mobile: 1rem
  margin-tablet: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

The design system is engineered for modern personal wealth intelligence, delivering immediate glanceability, analytical clarity, and financial serenity. It couples deep, battery-conserving AMOLED charcoals with high-vibrancy optical signals, reducing visual fatigue while delivering instant semantic comprehension of liquidity, cash flows, and investments.

The aesthetic fuses **Material 3 Expressive** with **High-Contrast Precision Dark Mode**:
- **Tactile Contrast & Ambient Depth:** Surfaces leverage structural tonal layering rather than aggressive drop shadows, letting key transactional numbers float effortlessly above near-black backdrops.
- **Glanceable Telemetry:** Financial metrics follow a rigorous semantic color logic—instantly distinguishing positive inflows, active commitments, and cash drains without cognitive strain.
- **Micro-Delight & Fluidity:** Pill-shaped interactions, continuous rounded corners, and soft touch targets instill a feeling of effortless balance and control.

## Colors

The palette establishes an OLED-first chromatic architecture anchored by deep, neutral charcoal tones that allow vivid semantic accents to guide user attention.

### Surface Tones (Dark AMOLED Base)
- **Background / Canvas (`#0b0c0e`):** The foundational absolute black substrate, maximizing power efficiency and dynamic range.
- **Surface (`#121316`):** Base canvas for application views and bottom navigation bars.
- **Surface Container (`#1a1c1e`):** Primary level for broad content sections and screen segments.
- **Surface Container High (`#222428`):** Elevated cards, grouped transaction modules, and floating modals.
- **Surface Container Highest (`#2d3036`):** Interactive inputs, pill containers, chip backgrounds, and pressed states.

### Core & Semantic Accents
- **Primary / Actions (`#818cf8` - Energetic Indigo):** Primary buttons, interactive FABs, bottom nav active indicators, and high-priority navigations.
- **Secondary (`#2dd4bf` - Electric Teal):** Secondary interactive triggers, neutral net balance indicators, and growth milestones.
- **Positive Inflow / Net Credit (`#4ade80` - Emerald Mint):** Income, positive yields, savings deposits, and cleared items.
- **Expense / Debit (`#f87171` - Soft Rose):** Subtractions, recurring liabilities, subscription expenses, and overdue notices.
- **Caution / Synchronizing (`#fbbf24` - Amber Glow):** Pending settlements, sync errors, budget threshold alerts, and recurring warnings.

Use absolute pure white (`#ffffff`) sparingly—reserve it exclusively for primary monetary figures and key ledger amounts. Secondary metadata and labels must use muted alpha variations (`rgba(255, 255, 255, 0.7)` and `rgba(255, 255, 255, 0.45)`).

## Typography

Plus Jakarta Sans provides geometric legibility alongside friendly, modern letterforms. Tabular figures (`tnum`) should be enabled across all financial tables, net balance widgets, and transaction logs to maintain vertical alignment of decimals and currencies.

### Usage Hierarchy
- **Display Large & Headline Large:** Reserved for hero portfolio balances and main dashboard summary figures.
- **Headline Medium / Small:** Section headings, bottom sheet modal titles, and monthly performance overviews.
- **Title Medium / Large:** Card headers, merchant titles in activity feeds, and account names.
- **Body Regular:** Supporting transaction descriptions, bank connection states, and descriptive helper text.
- **Label Bold / Medium:** Metric badges, category tags, timestamp metadata, and bottom navigation labels.

## Layout & Spacing

The layout is built upon an 8pt modular grid with an auxiliary 4pt subgrid for compact data visualization. 

### Grid Configuration
- **Mobile Handsets (<600dp):** Single-column presentation with a consistent 16px (`1rem`) canvas margin. Cards span 100% of the interior grid width.
- **Foldables & Tablets (600dp–1024dp):** Two-column split-view, maintaining 24px canvas margins and a 16px gutter between primary analytical charts and transaction activity feeds.

### Spacing Tokens
- `space-xs` (4px): Micro-spacing between category icons and currency codes.
- `space-sm` (8px): Padding between list items and tag clusters.
- `space-md` (16px): Internal padding for ledger rows, list items, and standard input blocks.
- `space-lg` (24px): Padding within elevated summary cards and structural separation between screen modules.
- `space-xl` (32px): Separation between unrelated dashboard sections and sticky bottom actions.

## Elevation & Depth

Visual hierarchy does not rely on heavy drop shadows, which look muddied on OLED screens. Instead, depth is conveyed through **tonal progression** and **micro-surface highlights**.

1. **Surface Tiers:**
   - **Canvas Level 0 (`#0b0c0e`):** The default screen background.
   - **Surface Level 1 (`#121316`):** The base layer for views, persistent shelves, and navigation wrappers.
   - **Container Level 2 (`#222428`):** Elevated parent cards, graph containers, and action panels.
   - **Dialogs & Overlays Level 3 (`#2d3036`):** Bottom sheets, popovers, and quick-transfer dialogs.

2. **Ghost Borders & Ambient Illumination:**
   - Cards and structural containers feature a 1px border colored at `rgba(255, 255, 255, 0.06)` to provide distinct separation without creating visual clutter.
   - High-priority elements (e.g., net positive card) use a subtle ambient glow: `0 8px 32px rgba(45, 212, 191, 0.12)`.

## Shapes

The design adopts an expressive shape cadence that balances friendly mobile ergonomics with sharp geometric rhythm:

- **Full Pill (`rounded-full` / 9999px):** Filter tags, action chips, bottom navigation pill indicators, quick-transfer avatars, and status badges.
- **Large Cards (`24px` / `rounded-3xl`):** Primary account overview containers, spending breakdown cards, and hero analytics panels.
- **Items & Inputs (`16px` / `rounded-2xl`):** Individual transaction rows, text input fields, and modular list items.
- **Icon Enclosures (`12px` / `rounded-xl`):** Squircle backgrounds behind transactional category icons (e.g., groceries, transport, utilities).

## Components

### Buttons & Interactive Controls
- **Primary Action Buttons:** Height of 48px to 56px, filled with Indigo (`#818cf8`), label in pure black (`#0b0c0e`) or high-contrast deep slate, set in `label-lg` with `rounded-full` or `rounded-2xl` styling.
- **Tonal / Ghost Buttons:** Background in `surface-container-highest` (`#2d3036`) with text in Indigo (`#818cf8`) or pure white (`#ffffff`).

### Transaction Cards & Ledger Items
- **Card Containers:** Backed by `surface-container-high` (`#222428`) with `24px` corner radii, framed by a 1px subtle white outline (`rgba(255, 255, 255, 0.06)`).
- **Transaction Rows:** Set within `rounded-2xl` sub-containers or separated by soft divider lines (`rgba(255, 255, 255, 0.05)`). Category icons sit within a `12px` squircle container tinted with the respective category hue at 15% opacity.
- **Financial Value Indicators:** Positive inflows appear in Emerald Mint (`#4ade80`) prepended with `+`, while debits appear in pure white (`#ffffff`) or Soft Rose (`#f87171`) with a `-` symbol.

### Chips & Filters
- **Pill Badges:** Completely rounded (`rounded-full`), using `surface-container-highest` (`#2d3036`) in neutral states, transitioning to Indigo (`#818cf8`) with inverted text when active.
- **Semantic Badges:** Small, compact indicators with low-opacity backgrounds (e.g., amber warning with `rgba(251, 191, 36, 0.15)` fill and `#fbbf24` text).

### Form Fields & Inputs
- **Container Architecture:** 56px minimum height, filled with `surface-container-highest` (`#2d3036`), without default underline borders. Corner radius of `16px`.
- **Focused State:** 1.5px border glow in Indigo (`#818cf8`), with floating labels transitioning to `label-sm`.

### Bottom Navigation Bar (Material 3 Expressive)
- **Container:** Grounded in `surface` (`#121316`) with an integrated top divider (`rgba(255, 255, 255, 0.04)`).
- **Active Indicator Pill:** A horizontal pill (`64px × 32px`, `rounded-full`) in primary Indigo (`#818cf8`) or Electric Teal (`#2dd4bf`), housing rounded Material Symbols icons rendered in the dark background tone (`#121316`).
- **Inactive Destinations:** Muted white icon fills (`rgba(255, 255, 255, 0.6)`) with concise `label-sm` labels directly below.