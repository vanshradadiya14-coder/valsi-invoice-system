# Valsi Invoice System

An offline-first Android invoicing app for a food distribution business, built with
**Kotlin + Jetpack Compose + Material 3**, MVVM, Room, Hilt, and Navigation Compose.

## Opening & building

This project is designed to be opened in **Android Studio** (Koala / Ladybug or newer).

1. **Open the folder** `Valsi Invoice System` in Android Studio (`File → Open`).
2. On first open, Android Studio will:
   - Generate the Gradle wrapper JAR (`gradle/wrapper/gradle-wrapper.jar`) if missing.
   - Create `local.properties` pointing at your Android SDK.
   - Sync and download dependencies.
3. Pick a device/emulator (**min SDK 26**, Android 8.0+) and press **Run**.

> The repo intentionally omits `local.properties` (machine-specific SDK path) and the
> binary `gradle-wrapper.jar`; Android Studio regenerates both automatically. From the
> command line you can also run `gradle wrapper` once (with a local Gradle 8.9) to create
> the wrapper JAR, then use `./gradlew assembleDebug`.

## Tech stack

| Concern            | Choice                                             |
|--------------------|----------------------------------------------------|
| Language / UI      | Kotlin 2.0, Jetpack Compose, Material 3            |
| Architecture       | MVVM + Repository layer                             |
| Local database     | Room (KSP)                                          |
| DI                 | Hilt                                                |
| Navigation         | Navigation Compose + bottom navigation bar         |
| Images             | Coil (company logo / product placeholder)          |
| PDF                | `android.graphics.pdf.PdfDocument` + FileProvider  |

## Project structure

```
com.valsi.invoicesystem
├── data
│   ├── entity        Room entities + enums + relation POJOs
│   ├── dao           DAOs (Customer, Product, Invoice, InvoiceItem, AppSettings)
│   ├── model         NewInvoiceRequest / NewInvoiceLine (domain input)
│   ├── repository    CustomerRepository, ProductRepository, InvoiceRepository, SettingsRepository
│   ├── Converters, SeedData, ValsiDatabase
├── di                Hilt DatabaseModule (DB + DAO providers, first-launch seeding)
├── navigation        Routes, TopLevelDestination, ValsiApp (NavHost), ValsiBottomBar
├── pdf               PdfInvoiceGenerator, PdfActions (share/print/save/view), print adapter
├── ui
│   ├── components     Reusable UI (search bar, invoice row, quantity stepper, empty state, chips)
│   ├── screens        home / customer / product / invoice / settings
│   └── theme          Color, Type, Theme (dark-green Material 3, light + dark)
├── util              Money & date formatters
└── viewmodel         One ViewModel per screen + InvoiceCreationViewModel for the cart flow
```

## Key design decisions

- **Price snapshots.** `InvoiceItem` stores `unitPriceSnapshot` and `productNameSnapshot`.
  Editing a product's price never changes historical invoices.
- **Atomic invoice numbering.** `InvoiceRepository.createInvoice` reserves and increments
  `AppSettings.nextInvoiceNumber` and inserts the invoice + items inside a single
  `withTransaction` block, so concurrent generation can't produce duplicate numbers.
- **Financial integrity.** Finalized invoices are never edited or deleted — only their
  payment status changes, or they are **voided** (kept for numbering integrity). Only
  DRAFT invoices can be deleted.
- **Outstanding balance** is recomputed from a customer's non-void invoices
  (`Σ grandTotal − amountPaid`) after every create / payment-update / void / delete.
- **Offline by design.** Everything is local Room; no network calls are required for any
  core feature (invoice creation, PDF generation, sharing).

## First-launch seed data

On first run the database seeds the 19 fixed products (price £0.00, edit later on the
Products screen) and a default `AppSettings` row (currency `£`, VAT 20%, prefix `VLS-`).
