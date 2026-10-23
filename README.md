# Smart Pantry Manager

**A Java Android application that suggests recipes based strictly on leftover ingredients, to cut food waste.**

Module: Mobile App Development 700 (Practical Assignment) — Richfield Graduate Institute of Technology, Faculty of Information Technology.

## What the app does

Food is wasted at home because people forget what they already have. Smart Pantry Manager lets a user
record what is actually in their kitchen, then suggests **only** the recipes that can be cooked right
now from those items — no shopping trip, and no recipe unless the user genuinely already has
everything it needs.

The **strict-matching rule** is the core of the app: a recipe is suggested only when **every**
ingredient it requires is present in the pantry, **in at least the required quantity**. A recipe needing
five ingredients when the user has four is never shown as a suggestion.

## Features

- **Pantry management** — add, edit and delete ingredients (name, quantity, unit, optional expiry date).
- **Pantry list** — a `RecyclerView` with a custom adapter bound directly to the SQLite database.
- **20 seeded recipes** — inserted on first run, each with its required ingredients and method.
- **Suggested Recipes** — runs the strict matcher and lists only what can be cooked now.
- **Almost there** (optional bonus) — recipes missing exactly one ingredient, in a clearly separated section.
- **Recipe detail** — full ingredient list (each item re-checked against the pantry) and numbered method.
- **Settings** — expiring-soon alerts, exclude-expired matching, "almost there" toggle, metric/imperial display.
- **Validation** on every field, **empty-state messaging** instead of blank screens, and persistence across restarts.

No maps, no GPS/location services and no payment features are used — the scope is strictly the user's own pantry.

## Screens

| # | Screen | Activity |
|---|--------|----------|
| 1 | Pantry List | `ui/MainActivity` |
| 2 | Add / Edit Ingredient | `ui/AddEditIngredientActivity` |
| 3 | Suggested Recipes | `ui/SuggestedRecipesActivity` |
| 4 | Recipe Detail | `ui/RecipeDetailActivity` |
| 5 | Settings | `ui/SettingsActivity` |

## Database option: SQLite (SQLiteOpenHelper)

I chose **SQLite**, implemented locally on-device with `SQLiteOpenHelper`, for three reasons:

1. **It fits the app.** The pantry is private, single-user data that must work in a kitchen with no signal. An on-device file needs no account, no API key and no network permission.
2. **It matches the module.** SQLite through `SQLiteOpenHelper` and `ContentValues` is the persistence approach covered in the module's persistent data chapter, so the code is easy to defend in the video.
3. **It demonstrates CRUD cleanly.** Three tables (`ingredients`, `recipes`, `recipe_ingredients`) with real `INSERT`, `SELECT`, `UPDATE` and `DELETE` statements, and data that survives closing and reopening the app.

Firebase would have added internet dependency for data that is inherently local, and PostgreSQL would have required a backend and network code for a single-user pantry.

## Tech stack

Java 17 · Android Gradle Plugin 8.1.4 · compileSdk 34 · minSdk 23 · AndroidX AppCompat · AndroidX RecyclerView · Material Components 1.11.0 · SQLite

## Setup and run instructions

**Option A — Android Studio (recommended)**

1. Download or clone this repository.
2. In Android Studio: **File → Open**, select the `smart-pantry-manager` folder, and wait for the Gradle sync to finish (Android Studio downloads the required Gradle version and Android SDK components automatically).
3. Create a device if needed: **Tools → Device Manager → Create device** (e.g. Pixel 6, API 34).
4. Press **Run ▶** (or `Shift+F10`).
5. On first launch the database is created and the 20 recipes are seeded. Open **Settings → Load demo pantry items**, then the **Recipes** tab, to see the matcher working immediately.

**Option B — command line**

```bash
# Generate the Gradle wrapper scripts once (the .jar is not committed)
gradle wrapper
./gradlew assembleDebug          # build the APK
./gradlew installDebug           # install on a connected device / running emulator
```

## Project structure

```
app/src/main/
├── AndroidManifest.xml                 5 activities declared
├── java/com/example/smartpantrymanager/
│   ├── data/DatabaseHelper.java        schema, CRUD, 20 seeded recipes
│   ├── model/                          Ingredient, Recipe, RecipeIngredient
│   ├── util/
│   │   ├── RecipeMatcher.java          THE STRICT-MATCHING RULE
│   │   ├── IngredientNormalizer.java   plurals, case, synonyms
│   │   ├── UnitConverter.java          unit families (g/kg, ml/tbsp, piece/slice…)
│   │   ├── Prefs.java                  SharedPreferences access
│   │   └── FormatUtils.java            number and date wording
│   ├── adapter/                        IngredientAdapter, RecipeAdapter
│   └── ui/                             MainActivity + 4 more screens
└── res/                                layouts, menus, strings, drawables
```

## How the strict matcher works

1. **Index the pantry** — names are normalised (lowercase, punctuation stripped, plurals removed, synonyms mapped) and quantities are converted into a base unit per family (grams / millilitres / count). Expired items are skipped when the setting is on.
2. **Test every requirement** — a requirement passes only if the pantry holds at least that amount in the same unit family, so `0.5 kg` covers `300 g`, but `500 ml` never covers `500 g`.
3. **Split the results** — recipes missing nothing go to *Ready to cook now*; recipes missing exactly one requirement go to the separate *Almost there* section; anything missing two or more is not shown.

## Author

Tarun Bridgemohan — [student number] — Mobile App Development 700
