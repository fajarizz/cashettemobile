# Cashette for Android

Native Compose client for Cashette, a personal-finance app for Indonesian users
(IDR, `Rp` formatting, `id-ID` grouping). It talks to the same Go backend as
`cashetteweb` and mirrors that app's feature set and visual world.

**Siblings:** `../cashetteweb` (TanStack Start + React, the reference
implementation) · `../cashettend` (Go + Gin + Postgres/Supabase, the API).

---

## The design contract

Read this before touching any UI file. It is the thing every later decision is
measured against.

**THESIS** — Cashette is a ledger you talk to. The app refuses the fintech
default of a chart-first dashboard fronting a data-entry chore: recording money
is the primary act, and it happens by conversation or in one tap, never by
filling a form you had to go find.

**OWN-WORLD** — A dark olive ground (`#141500`) under wheat-cream ink, with sage
and ochre as the working accents. Surfaces are rounded far past Material's
defaults — 16dp cards, 24dp heroes, fully-pilled buttons — and separated by
tone, not by borders or shadows. Recognizable with every label removed: no other
finance app is this colour.

**STORY** — The user opens the app to answer "can I afford this" or to record
something before they forget it. They should get the answer in one screen and
record in one tap.

**FIRST VIEWPORT** — Home: current month's net position at hero scale in tabular
figures, the budget rail beneath it, recent activity below the fold. The add-FAB
sits bottom-end above the bar, always reachable by thumb.

**FORM** — Inherited world. `cashetteweb` carries the visual identity; this app
translates it into Material 3 Expressive rather than inventing a second Cashette.

**MODE** — Operate. Expression lives in shape, motion, and colour-as-system.
It never costs the user scanability, a familiar affordance, or a visible state.

---

## Colour

`ui/theme/Color.kt` is **generated**, not hand-written. It comes from six CIE-LCh
tonal palettes whose dark tones gamut-clip onto cashetteweb's exact `oklch()`
tokens — ground lands at `#141500` against the web's `#111400`, card at `#1b1d00`
against `#1a1d00`. Editing a hex by hand desyncs the two apps. Change the
`PALETTES` inputs in `tools/palette.mjs` and regenerate:

```
node tools/palette.mjs app/src/main/java/com/basbasdev/cashette/ui/theme/Color.kt
```

It prints each web token's CIE LCh alongside the generated tones, so drift between
the two apps is visible in the diff.

| Palette | Hue | Role |
|---|---|---|
| primary | 94 (wheat/cream) | Filled buttons, FAB, active state — the web's cream-on-olive inversion |
| secondary | 118 (sage) | Tonal buttons, chips, secondary containers |
| tertiary | 72 (ochre) | The Expressive accent. Where the app gets its energy |
| neutral | 112 (olive) | Ground and every surface layer. Deliberately high-chroma — the olive cast *is* the brand |
| neutralVariant | 110 | Outlines, dividers, surface variants |
| error | 26 | UI failure states only |

**Dark only. There is no light scheme, and adding one is a regression.** The web
hard-codes `<html className="dark">` and the olive ground *is* the brand. In a
light scheme M3 inverts `primary` to a dark tone of the cream — a muddy khaki —
and pushes the ground to near-white, so the green disappears entirely and the app
stops looking like Cashette. `CashetteTheme` takes no `darkTheme` parameter;
`Theme.Cashette` in `themes.xml` uses a dark parent so there is no white flash
before the first Compose frame; and `MainActivity` pins the system bars to light
icons rather than letting them follow the device.

**Money colour is not `error`.** An expense is not a failure; painting a grocery
run red-as-error is a category mistake. Direction-of-value lives in
`CashetteTheme.finance` (`income`, `expense`, and their containers), carrying the
web's exact `#4ade80` and `#ff6467`.

**Rules**
- Never write `Color(0x…)` in a screen or component. Every colour comes from
  `MaterialTheme.colorScheme` or `CashetteTheme.finance`.
- Only signed amounts and their indicators may take income/expense colour.
  Never a card background, never body text.
- Charts use `finance.chartRamp`, ordered by magnitude, **never cycled**. Past
  seven categories, fold the tail into "Other" — a generated eighth hue is a bug.
- Accent colour marks primary action, current selection, and state. Not
  decoration.

## Type

One family, fixed scale, ~1.2 steps (`ui/theme/Type.kt`). Product UI has more type
elements than a brand surface; exaggerated contrast just makes noise.

**Money uses `CashetteText.Money*`**, which sets `tnum`. Figures are compared down
a column — they must not reflow between states. This mirrors the web's
`tabular-nums` on every balance and total.

> **Outstanding asset:** the web runs **Figtree Variable**; Android currently
> falls back to system sans. Drop `figtree_{regular,medium,semibold,bold}.ttf`
> into `res/font/` and change the single `CashetteFontFamily` declaration —
> every style inherits it. Until then, type is the one place the mirror is
> imperfect.

## Shape

`ui/theme/Shape.kt`. Cashette is rounder than stock Material, and that is
load-bearing identity, not a preference.

`CashetteShape.Field` 12dp · `.Card` 16dp · `.Hero` 24dp · `.Sheet` 28dp top ·
`.Pill` for **every** button and FAB. The web has no square buttons; neither does
this app. Grouped lists use `groupTop/Middle/Bottom` so a run of rows reads as one
object.

## Motion

`ui/theme/Motion.kt`, with `MotionScheme.expressive()` driving components.
Expressive moves on springs; a hand-rolled `tween` beside a spring-driven Button
reads as a bug. Use `CashetteMotion.*` for anything you animate yourself.

Operate mode keeps motion short and meaningful: state change, feedback, reveal.
**No orchestrated load sequences** — the user came to do a task, not watch the
screen assemble. Skeletons for loading, never a spinner parked in content.

---

## Material 3 Expressive vocabulary

> **Dependency note.** Compose BOM `2026.02.01` pins material3 **1.4.0**, where
> `MaterialExpressiveTheme` and `MotionScheme` are `internal` and every Expressive
> component is absent — verified by compiling against it. The catalog therefore
> pins `material3 = "1.5.0-alpha26"` explicitly, overriding the BOM. This is a
> deliberate trade: an alpha dependency is the price of the Expressive brief.
> Reverting is one line in `libs.versions.toml`, but it also reverts `Theme.kt` to
> plain `MaterialTheme` and removes everything in the table below.
>
> Most of these carry `@ExperimentalMaterial3ExpressiveApi`; opt in at the
> composable, not project-wide, so the blast radius of an API change stays visible.

Reach for these deliberately; each one earns its place on a specific screen.
All are verified present in `1.5.0-alpha26`.

| Component | Where | Why |
|---|---|---|
| `FloatingActionButtonMenu` | Global add | Expands to Income / Expense / Transfer. The app's primary act, one thumb-reach away |
| `ButtonGroup` | Analytics period, budget month | Connected segments that squash on press — replaces the web's `Select` |
| `SplitButton` | Account and debt rows | Primary action plus its overflow without a second control |
| `LoadingIndicator` | All loading | The shape-morphing indicator, not `CircularProgressIndicator` |
| `ToggleButton` | History filters | Shape morphs between unselected and selected |
| `MaterialShapes` | Category avatars, empty states | Cookie/clover/pill forms give categories identity without an icon set |
| `HorizontalFloatingToolbar` | Chart and list screens | Contextual actions that float over content instead of eating a bar |
| `Carousel` | Pockets, accounts | Balance cards read as objects to flick through |
| `WideNavigationRail` | Tablet / expanded width | Bottom bar collapses to a rail past 600dp |

**Never** an emoji or a unicode glyph standing in for an icon.

**Icons are generated, like `Color.kt`.** `res/drawable/ic_*.xml` comes from the same
Hugeicons set cashetteweb draws with, at the same stroke weight of 2:

```
node tools/icons.mjs app/src/main/res/drawable
```

The map from resource name to Hugeicons module lives at the top of that script; the
names on the right are the exact ones `cashetteweb/src/components/app-sidebar.tsx`
imports, so a change there is a one-line change here. Do not reach for
`material-icons-extended` — it is frozen upstream and is a different drawing style,
which breaks both the mirror and the one-stroke-weight rule. Strokes are emitted black
and recoloured at the use site by `Icon(tint = …)`.

## Navigation

Ten web routes do not fit a bottom bar. Four hold a tab; the rest hang off the card
that already previews them.

**Home · Chat · ( FAB ) · History · Money**

| Tab | Answers | Holds |
|---|---|---|
| Home | "where do I stand, and where's everything else" | the summary, and every outbound link |
| Chat | "just record this / what's my BCA balance" | the assistant |
| History | "what did I spend on X" | the ledger: filter, search, insights |
| Money | "what do I hold, what do I owe" | accounts · pockets · debt, under one net position |

**Every tab is a place you go to do something Home cannot; none is a menu.** An
earlier draft grouped budget/subscriptions/analytics behind a `Plan` tab — a third
summary screen competing with Home and Money, whose whole content was links — and
buried History, one of the two daily screens, three taps down. Do not reintroduce it.
`Money` survives that rule because it is a real statement, and because Pockets and
Debt have no Home card and would otherwise be orphans.

One level down, each reached from the thing that previews it: **Budget** and
**Subscriptions** from their Home cards, **Analytics** from Home's top-bar action,
**Settings** from the Home avatar, **Accounts · Pockets · Debt** from their Money
sections. Everything is within two taps.

**Shell rules** (`navigation/AppScaffold.kt`, `navigation/AppNavHost.kt`):

- **Bar and FAB belong to the four top-level destinations only.** Anything deeper is
  full-bleed with a back arrow, so depth is legible without reading the title.
- **No FAB on Chat.** Its composer owns the bottom edge and is already the fastest
  way to record something.
- **One back stack, per-tab state saved** — `popUpTo(startDestination) { saveState }`
  + `launchSingleTop` + `restoreState`. Back from any tab lands on Home and the next
  back leaves the app. Not per-tab stacks; this is Android, not iOS.
- **Reselecting the current tab scrolls it to top.** List states are hoisted in
  `AppNavHost` because the bar sits outside the screen that owns the list.
- **Two NavHosts.** `CashetteNavHost` switches on `SessionState`; `AppNavHost` owns
  the signed-in graph with its own controller, so signing out destroys that back
  stack rather than leaving it reachable.
- **The add sheet is hoisted into the scaffold, not the graph** — it composes over
  whichever tab is showing and is never something the back stack has to model.
- **Screen transitions**: fade-through between tabs (siblings — a slide would imply a
  hierarchy that is not there), shared-axis slide into a detail, mirrored on the way
  back. Both on `CashetteMotion` springs.
- **Each screen owns its top app bar** (`ui/components/ScreenChrome.kt`). The web's
  global `SiteHeader` exists because it is a sidebar layout; a shared bar here would
  just be a title lookup table between a screen and its own actions.

---

## Build constraints

Four of these were found the hard way. Changing any one breaks the build.

- **`compileSdk` is 37.** The template shipped `core-ktx 1.19.0` and `lifecycle 2.11.0`,
  which both require API 37, against `compileSdk 36.1` — so the project could never
  assemble as generated. AGP downloads the platform automatically.
- **Kotlin is 2.2.10, which reads class metadata up to 2.3.0.** `supabase-kt 3.7.0` and
  `ktor 3.5.x` are built with Kotlin 2.4 and fail to load with "incompatible version of
  Kotlin". Hence `supabase 3.2.6` / `ktor 3.3.1`, the newest built for 2.2. Move Kotlin
  before bumping either.
- **`android.disallowKotlinSourceSets=false`** in `gradle.properties`. AGP 9's built-in
  Kotlin rejects plugins that register Kotlin source sets, which is how KSP wires in
  Hilt's generated code.
- **Gradle needs a full JDK, not a JRE.** `assembleDebug` runs `jlink` to build the
  Android JDK image; the IDE's bundled JRE has no `jlink`. Android Studio's JBR works:
  `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug`.

## Auth

Supabase owns identity; the Go backend only validates the JWT. `SessionState`
(`auth/SessionViewModel.kt`) is the single source of truth for what the user sees:
`Loading → SignedOut | NeedsDisplayName | Ready`. `CashetteNavHost` switches graphs on
it rather than navigating imperatively, so no back stack survives a sign-in or sign-out.

- Session persists through `EncryptedSessionManager` (EncryptedSharedPreferences), not
  the default plaintext prefs — the refresh token mints access tokens indefinitely.
- The API `HttpClient` reads the bearer per request and signs out on any 401, which is
  the mobile equivalent of the web's patched global `fetch`.
- `signUp` sends `full_name` as user metadata; migration 000014's trigger copies it into
  `public.profiles`. Email confirmation is off, so signup returns a live session.
- Password recovery deep-links to `cashette://auth`. While recovering, the UI is pinned
  to the reset screen — that session may only set a password, not browse the app.

**Two manual steps** this code cannot do for itself: add `cashette://auth` to the
Supabase dashboard's Redirect URLs, and restart the Go backend so migration 000014
applies (`schema_migrations` must read 14).

## API

Base `{API}/api`, bearer token from Supabase auth (`session.access_token`).
The Go handlers read the user from the token; most list endpoints also want an
explicit `user_id` query param.

`/transactions` (`?user_id&type&from_date&to_date`, dates are **`yyyy-MM-dd`** —
the handler silently drops anything else and returns unfiltered) ·
`/transactions/summary` · `/accounts` · `/categories` · `/budgets`,
`/budgets/summary` (`?month&year`) · `/subscriptions`, `/subscriptions/due`,
`/subscriptions/:id/record` · `/debts`, `/debts/:id/pay` · `/transfers` ·
`/profiles/:userId` · `/chat`, `/chat/parse`, `/chat/confirm` ·
`/report/export` (`?from&to`, xlsx).

Amounts arrive as decimal **strings** and become `BigDecimal` at the network boundary —
never `Double`. Format through `core/money/Money.kt`; nothing else formats IDR, and every
money figure on screen carries `toSpokenIdr()` as its content description, because
TalkBack reads "Rp 2.610.000" as "R P two point six one zero point zero zero zero".

**Empty list endpoints return `null`, not `[]`** — Go marshals a nil slice as JSON null.
`LedgerApi.getList` coalesces it. Decoding a list strictly turns "nothing recorded this
month" into an error state on Home.

## Home

`feature/home/`. A descending answer, not a dashboard: exactly one thing at hero scale,
and every block below it smaller, more specific, and **structurally distinct** — a hero
surface, a ranked list with bars, a ledger, one actionable row, a rack of cards. Five
identical cards stacked is the web's grid problem in a taller shape.

The hero is a ladder, and **no rung may render empty**:

| State | Hero |
|---|---|
| Budget set for the month | **Left to spend** + burn rail; flips to expense-coloured "over" past the limit |
| No budget, transactions exist | **Net this month**, plus a prompt to set one |
| No budget, no transactions, accounts exist | **Available to spend** — the state every user wakes to on the 1st |
| No accounts either | Home collapses to the first-run card |

`Section<T>` per block, not one screen-level state: five independent calls, so a failed
`/accounts` must still leave the hero standing with one inline retry on the strip. A
failed **bill** section shows its error rather than hiding — silence there is
indistinguishable from having no bills, and one of them may be due today.

Two bar meanings, so two appearances: cream with a stated limit when the category is
budgeted, muted `outlineVariant` when it is only ranking against the top spender.

The FAB hides on scroll down and returns on scroll up. It floats exactly over the
right-aligned amounts column, and an obscured figure in a ledger is worse than a missing
affordance.

Auth is Supabase; a Postgres trigger mirrors `auth.users` into `public.users` and
`public.profiles`. New users have no `full_name` until they set one — the web
blocks first entry on a display-name prompt, and this app must too, or the
account renders nameless.

## Rules

- Every interactive component ships default, hover/press, focus, disabled,
  loading, error. Half a set is not a component.
- Empty states teach the screen. "No data" is not an empty state.
- Same affordance everywhere: if the save button differs on two screens, one is
  wrong.
- Modal is the last resort, not the first thought. Exhaust inline and
  progressive disclosure first.
- IDR only. Format through one shared helper; the web hardcodes IDR throughout
  and a currency picker that changes nothing is worse than none.
- Body text ≥4.5:1, large text ≥3:1, verified against the olive ground.
