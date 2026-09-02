# Pharmacy Feature Elimination — Analysis & Implementation Plan

This document describes the **effect** of removing the pharmacy feature from the HASET
Android app and provides a precise, ordered **implementation plan** to do so safely.

> Status: **ANALYSIS ONLY.** The removal has **not** been executed. This doc exists so the
> blast radius is understood before any files are deleted. When the removal is desired,
> follow Section 5.

---

## 1. What the pharmacy feature is

The pharmacy feature is a **fully-coded but dormant (V1-disabled) module**. It has real
UI, a ViewModel, a Repository, Firebase Realtime Database persistence, adapters and
resources — but **nothing in the app launches it**. The patient home and global search show
a "Pharmacy coming soon" dialog instead.

### Module inventory

**Java (`app/src/main/java/com/haset/hasetapp/`)**

| File | Role |
|---|---|
| `activities/PharmacyActivity.java` | Screen host (search bar + bottom nav home/cart) |
| `fragments/PharmacyHomeFragment.java` | Categories + bestseller carousel |
| `fragments/PharmacyCartFragment.java` | Cart list + totals + (stubbed, commented-out) checkout |
| `fragments/PharmacyTabFragment.java` | Category product grid |
| `viewmodels/PharmacyViewModel.java` | State + LiveData for products/cart/search |
| `repositories/PharmacyRepository.java` | Firebase RTDB reads/writes (`pharmacy_products`, `carts/{uid}`) |
| `models/PharmacyProduct.java` | Product data |
| `models/CartItem.java` | Cart line item |
| `adapters/PharmacyProductAdapter.java` | Product rows |
| `adapters/PharmacyBestsellerAdapter.java` | Bestseller cards |
| `adapters/PharmacyCategoryAdapter.java` | Category chips |
| `adapters/PharmacyBannerAdapter.java` | Banners — **unused** by any screen |
| `adapters/CartItemAdapter.java` | Cart rows |

**Resources (`app/src/main/res/`)**

| Resource | Kind |
|---|---|
| `layout/activity_pharmacy.xml`, `fragment_pharmacy_home.xml`, `fragment_pharmacy_cart.xml`, `fragment_pharmacy_tab.xml` | Layouts |
| `layout/item_pharmacy_product.xml`, `item_pharmacy_product_bestseller.xml`, `item_pharmacy_category.xml`, `item_pharmacy_banner.xml`, `item_cart_product.xml` | Item layouts |
| `menu/bottom_nav_pharmacy.xml` | Bottom nav |
| `color/bottom_nav_pharmacy_color.xml` | Nav color state list |
| `drawable/pharmacy_sign.webp` | Icon |
| `drawable/ic_cart.xml` | Cart icon (also used by non-pharmacy layout — see §4) |

**Manifest:** `AndroidManifest.xml` line 46–48 registers `.activities.PharmacyActivity`
(`exported="false"`).

---

## 2. Data flow (what it touches in Firebase)

`PharmacyRepository` reads/writes **two RTDB nodes** only:

| Method | Node |
|---|---|
| `getAllProducts()`, `getProductsByCategory(cat)` | `pharmacy_products` |
| `addToCart`, `removeFromCart`, `updateCartItemQuantity`, `clearCart` | `carts/{userId}/{productId}` |

There is **no order, checkout, stock-update or payment method** anywhere in the module.

---

## 3. External references (the blast radius)

The pharmacy module is **not isolated** — several non-pharmacy files reference its types,
repository and data. These are shown below and MUST be handled during removal.

### 3.1 `repositories/HomeRepository.java`
- imports `com.haset.hasetapp.models.PharmacyProduct` (line 15)
- field `private final PharmacyRepository pharmacyRepository = new PharmacyRepository();` (line 27)
- method `getFeaturedMedicines()` (line 208) → returns `pharmacyRepository.getAllProducts()`
  - supplies the **Featured Medicines** carousel on the patient home

### 3.2 `viewmodels/HomeViewModel.java`
- imports `PharmacyProduct` (line 14)
- field `featuredMedicines` (line 27)
- method `getFeaturedMedicines()` (line 96) delegates to `HomeRepository.getFeaturedMedicines()`
- exposes the medicine list to `PatientHomeFragment`

### 3.3 `fragments/PatientHomeFragment.java`
The heaviest consumer:
- views `llBuyMedicine`, `rvMedicineNew`, `tvViewAllMedicine` (lines 96–98, wired at 753–757,
  released at 1289–1299)
- `llBuyMedicine` click → `showComingSoonDialog(R.string.pharmacy)` (lines 498–503, DISABLED)
- `tvViewAllMedicine` click → coming-soon (lines 518–522, DISABLED)
- `case "medicine":` service → "Pharmacy module coming soon" toast (lines 229–231)
- `ServiceItem(..., "medicine")` entry (line 276)
- observes `viewModel.getFeaturedMedicines()` (lines 911) → `updateMedicineUI(products)` (1203)
  which renders the **Featured Medicines** carousel via `item_patient_home_medicine.xml`
  and `PharmacyProduct`
- banner sections `BannerType.PHARMACY` (lines 1090–1100, 1120–1128)
- `case PHARMACY:` → coming-soon (lines 993–995)
- `"Pharmacy"` name branch → coming-soon (lines 941–944)

### 3.4 `activities/SearchActivity.java`
- imports `PharmacyProduct` (line 22), `PharmacyRepository` (line 25)
- fields `allDrugs`, `pharmacyRepository` (lines 43, 46), init (line 70)
- observes `pharmacyRepository.getAllProducts()` (line 115) → drug search
- filters `for (PharmacyProduct drug : allDrugs)` (line 199), labels "Haset pharmacy" (line 208)
- clicking a drug → coming-soon toast (lines 270–272, DISABLED)
- service case `"Pharmacy"` → coming-soon (lines 304–307, DISABLED)

### 3.5 Shared resources referenced OUTSIDE the module
These resources must **NOT** be deleted even though the module uses them, because
non-pharmacy code also references them:

- `drawable/ic_cart.xml` — used by `item_patient_home_medicine.xml`
- `string/medicines` — used by `bottom_sheet_upload_prescription.xml`
- `string/buy_medicine`, `string/feature_coming_soon`, `string/pharmacy`,
  `string/shopping_cart`, `string/cart`, `string/start_shopping`, etc. — used by
  `PatientHomeFragment` / `SearchActivity` / providers outside the module
- `layout/item_patient_home_medicine.xml` — part of the patient home (not the pharmacy
  module); it displays `PharmacyProduct` and stays

---

## 4. Effects of removal (what changes for users & code)

### 4.1 User-facing effect
- **App size** decreases (fewer Java classes, layouts, drawables).
- The patient home **Featured Medicines carousel disappears** unless its data source is
  re-pointed or removed.
- The "Buy Medicine" / pharmacy coming-soon entries on the patient home and search **no
  longer show** (they are already disabled/V1-stale, so effectively no visible regression).
- No user can currently reach the pharmacy screens anyway, so **no live user flow breaks**.

### 4.2 Code / build effect (compilation risk)
Removing the module's Java files **without** also editing `HomeRepository`,
`HomeViewModel`, `PatientHomeFragment` and `SearchActivity` would cause **compile errors**
(those files reference `PharmacyProduct` / `PharmacyRepository`). Therefore removal is
**all-or-nothing at the source level** — external references must be stripped in the same
change.

### 4.3 Data effect (Firebase)
- The RTDB nodes `pharmacy_products` and `carts/{uid}` are **not** deleted by removing app
  code. They remain in the console (data is untouched). Deleting data is a separate,
  optional backend operation and is **not** recommended while any other client (e.g. the
  web admin / future V2) might still read them.

### 4.4 Reports / monitoring
- `CrashMonitor`/`ErrorLogger` are **not** used anywhere in the pharmacy module, so removal
  has no effect on Crashlytics instrumentation. (The module was intentionally not
  instrumented while dormant.)

---

## 5. Implementation plan (ordered, when removal is approved)

> Follow exactly this order so the project compiles at each step after the final change.

### 5.1 Delete pharmacy-module resources
```
app/src/main/res/layout/activity_pharmacy.xml
app/src/main/res/layout/fragment_pharmacy_home.xml
app/src/main/res/layout/fragment_pharmacy_cart.xml
app/src/main/res/layout/fragment_pharmacy_tab.xml
app/src/main/res/layout/item_pharmacy_product.xml
app/src/main/res/layout/item_pharmacy_product_bestseller.xml
app/src/main/res/layout/item_pharmacy_category.xml
app/src/main/res/layout/item_pharmacy_banner.xml
app/src/main/res/layout/item_cart_product.xml
app/src/main/res/menu/bottom_nav_pharmacy.xml
app/src/main/res/color/bottom_nav_pharmacy_color.xml
app/src/main/res/drawable/pharmacy_sign.webp
```

Do **NOT** delete (shared):
```
app/src/main/res/drawable/ic_cart.xml          # used by item_patient_home_medicine.xml
app/src/main/res/layout/item_patient_home_medicine.xml
```

### 5.2 Delete pharmacy-module Java files
```
app/src/main/java/com/haset/hasetapp/activities/PharmacyActivity.java
app/src/main/java/com/haset/hasetapp/fragments/PharmacyHomeFragment.java
app/src/main/java/com/haset/hasetapp/fragments/PharmacyCartFragment.java
app/src/main/java/com/haset/hasetapp/fragments/PharmacyTabFragment.java
app/src/main/java/com/haset/hasetapp/viewmodels/PharmacyViewModel.java
app/src/main/java/com/haset/hasetapp/repositories/PharmacyRepository.java
app/src/main/java/com/haset/hasetapp/models/PharmacyProduct.java
app/src/main/java/com/haset/hasetapp/models/CartItem.java
app/src/main/java/com/haset/hasetapp/adapters/PharmacyProductAdapter.java
app/src/main/java/com/haset/hasetapp/adapters/PharmacyBestsellerAdapter.java
app/src/main/java/com/haset/hasetapp/adapters/PharmacyCategoryAdapter.java
app/src/main/java/com/haset/hasetapp/adapters/PharmacyBannerAdapter.java
app/src/main/java/com/haset/hasetapp/adapters/CartItemAdapter.java
```

### 5.3 Remove manifest entry
In `app/src/main/AndroidManifest.xml` remove lines 46–48 (the `PharmacyActivity`
`<activity>` block).

### 5.4 Strip external references
These edits are **required** or the build breaks (see §4.2). Delete the corresponding
code and imports in each file:

**(a) `repositories/HomeRepository.java`**
- remove import `PharmacyProduct` (line 15)
- remove field `PharmacyRepository pharmacyRepository` (line 27)
- remove method `getFeaturedMedicines()` (lines 208–210)

**(b) `viewmodels/HomeViewModel.java`**
- remove import `PharmacyProduct` (line 14)
- remove field `featuredMedicines` (line 27)
- remove method `getFeaturedMedicines()` (lines 96–99)

**(c) `fragments/PatientHomeFragment.java`**
- remove views `llBuyMedicine` (498–503), `rvMedicineNew` (756, 1182–…, 1261, 1293),
  `tvViewAllMedicine` (518–522, 1299) and their declarations/wiring/release
- remove observer at 911 → `updateMedicineUI` (1203–…)
- remove `case "medicine"` (229–231), service entry (276), `"Pharmacy"` branch (941–944),
  `case PHARMACY` (993–995)
- remove `BannerType.PHARMACY` banner sections (1090–1100, 1120–1128)
- (optional) remove `string/medicines` usage there if it becomes unused

**(d) `activities/SearchActivity.java`**
- remove imports `PharmacyProduct` (22), `PharmacyRepository` (25)
- remove fields `allDrugs` (43), `pharmacyRepository` (46), init (70)
- remove observer (115), filter (199), label (208)
- remove drug-click coming-soon (270–272) and `"Pharmacy"` case (304–307)

### 5.5 Recommended: delete now-unused strings
Only after verifying with a search that no remaining code references them, remove any
pharmacy-only strings (e.g. `shopping_cart`, `your_cart_is_empty`, `proceed_to_checkout`,
`start_shopping`, `cart`, `add_items_to_your_cart_to_get_started`, `in_stock`,
`out_of_stock`, `personal_care`, `supplements`, `buy_medicine` if unused). **Double-check**
`feature_coming_soon` and `pharmacy` first — they are referenced by non-pharmacy code.

### 5.6 Verify
- `grep -rn "Pharmacy\|PharmacyProduct\|PharmacyRepository\|CartItem" app/src` → must return
  **no source matches** (only the doc).
- Do a **dry-run** first: comment out external references and run `./gradlew :app:compileDebugJavaWithJavac`
  (or the user's usual build command) to confirm compilation before deleting files.
- Optionally re-run the release build when satisfied.

### 5.7 Optional backend cleanup (do NOT do unless explicitly requested)
If/when V2 is dropped for good, the data nodes `pharmacy_products` and `carts` can be
deleted from the Firebase RTDB console or via rules. This is out of scope for the app
change and is deliberately left untouched here.

---

## 6. Decision notes / risks

1. **Compilation coupling is the main risk** — deleting module files without §5.4 breaks
   `HomeRepository`, `HomeViewModel`, `PatientHomeFragment`, `SearchActivity`.
2. **Removal is reversible** — because the code is dormant, removing it only shrinks the
   binary and de-clutters; restoring V2 is a re-add of these files.
3. **Data is preserved** — Firestore/RTDB nodes stay unless explicitly deleted.
4. **Shared resources must survive** — `ic_cart`, `medicines`, `pharmacy`,
   `feature_coming_soon`, and the patient-home medicine layout are used outside the module
   and are kept.
