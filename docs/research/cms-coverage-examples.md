# CMS coverage-example claims (T-007)

Research date: 2026-11-23. Researched by the orchestrator. Result: the claims are published
by CMS and can be cited, so reconciliation can work as designed (section 6 of the design).

## Sources

All are on the CMS CCIIO page "Forms, Reports, and Other Resources", in the SBC section for plan
or policy years beginning on or after January 1, 2021:
https://www.cms.gov/cciio/Resources/Forms-Reports-and-Other-Resources/index

| Document | URL | Used for |
| --- | --- | --- |
| Having a Baby calculation guide (XLSX, PDF) | https://www.cms.gov/files/document/having-baby-guide-060723-xlsx.xlsx, https://www.cms.gov/files/document/having-baby-guide-060723-pdf.pdf | claim lines |
| Managing Type 2 Diabetes calculation guide | https://www.cms.gov/files/document/managing-type-2-diabetes-guide-060723-xlsx.xlsx, https://www.cms.gov/files/document/managing-type-2-diabetes-guide-060723-pdf.pdf | claim lines |
| Simple Fracture calculation guide | https://www.cms.gov/files/document/simple-fracture-guide-060723-xlsx.xlsx, https://www.cms.gov/files/document/simple-fracture-guide-060723-pdf.pdf | claim lines |
| SBC group instructions (Edition Date January 2021) | https://www.cms.gov/files/document/group-instructions-060723.pdf | calculation and display rules |
| Coverage Examples Calculator instructions (Rev. 3.01, Dec 2019) | https://www.cms.gov/files/document/coverage-examples-calculator-instructions-060723.pdf | calculator order of operations |
| English SBC template (accessible PDF) | https://www.cms.gov/files/document/english-sbc-template-accessible-format-110424.pdf | printed totals |

- **Template:** the 2021 edition. Plans must use it for open enrollment periods for plan years
  beginning on or after January 1, 2021.
- **Guide revision:** the guides are dated 2023-06-07, from the `060723` in their file names.
- **Downloaded files** (2026-11-23, SHA-256):
  - `having-baby-guide-060723-xlsx.xlsx`
    `14861670f5e09d5690b2b94e1aee16577b76f7646559c231b359dffc01aa9d22`
  - `managing-type-2-diabetes-guide-060723-xlsx.xlsx`
    `f483386a60feb231eaa02f51db963a82d768ae938d39b9e9a7adec7660112976`
  - `simple-fracture-guide-060723-xlsx.xlsx`
    `81fedef1021ec8095052a3d0db6dd565b6b36b365091974408822e6c88cc421e`

The data file `src/main/resources/rules/sbc-coverage-examples.yaml` was generated from the XLSX
guides by a stdlib-only script that reads the "Scenario" sheet of each guide.

## What the guides contain

Each guide has a "Scenario" sheet. Each claim line has these columns: date of service, ICD-10
code, CPT/HCPCS/NDC code, provider type, category, description and allowed amount.

The guide defines the allowed amount as "an estimated national average allowed amount for each
item or service, which plans or issuers must use to calculate cost sharing". The dates show
the order of care; the year in them (2016) is notional.

## Hand check of the totals

The line sums were taken from the generated YAML. The CMS totals are the "Totals" cell in each
guide, and the printed totals are on the SBC template.

| Scenario | Lines | Sum of lines | CMS "Totals" cell | Printed "Total Example Cost" |
| --- | --- | --- | --- | --- |
| `having_baby` | 50 | 12,686.85 | 12,686.85 | $12,700 |
| `managing_diabetes` | 97 | 5,601.1016345616 | 5,601.1016345616 | $5,600 |
| `simple_fracture` | 18 | 2,800.17 | 2,800.17 | $2,800 |

Every sum equals the CMS total exactly. Each printed total is the CMS total rounded to the
nearest $100.

## Decisions taken in this task

- **No billing or diagnosis codes are stored (director, 2026-11-23).**
  - The guides state "CPT copyright 2010 American Medical Association. All rights reserved."
  - CPT lines get a plain label written for Cairn, such as "Laboratory test", in place of the
    AMA short descriptor.
  - Lines billed with an HCPCS Level II code, a drug or supply product number (NDC), an MS-DRG
    or "OTC" keep the CMS description, because those descriptors are not AMA text.
  - The engine needs only the category and the amount.
- **Amounts are stored exactly as published.**
  - The diabetes guide has sub-cent amounts, for example `20.619405634`. Excel values are read
    through their shortest exact decimal form, so `2609.9299999999998` becomes `2609.93`.
  - The future `rules` loader rounds each line to cents, half up, because `Money` (T-005) is
    cents-only. Across 97 lines this moves a total by under $0.50, far below CMS's display
    rounding to the nearest $100 or $10.
- **Bundled obstetric visits are stored as $0.00.** The guide shows "-" for the follow-up
  visits under the global obstetric fee, because their cost is in the single $2,609.93 line.

## CMS calculation and display rules that matter for Cairn

- **Order:** "These calculations should be made using the order in which the services were
  provided (Date of Service)." (group instructions)
- **Tier:** use the cost sharing for the self-only coverage tier.
- **Assumptions:**
  - all care is in-network and in the lowest-cost tier
  - no out-of-network charges
  - generic drugs where available
  - no wellness-program participation
  - no other medical expenses
  - all services fall in one policy period
- **Splitting the patient's share:**
  - copays that apply to the deductible are reported under "Deductible"
  - "Copayments" holds the copays that do not apply to the deductible
  - "Coinsurance" is the total less the deductible, copayments and limits or exclusions
- **Rounding:** amounts of $100 or more round to the nearest hundred; amounts under $100 round
  to the nearest ten (group instructions). The guides' assumption sheets say "greater than $100"
  and "less than $100", leaving exactly $100 unstated; Cairn follows the instructions. CMS states
  no tie rule. Its example of a $1,750 deductible shown as $1,800 before the cap implies half
  up, which Cairn uses.
- **Caps:**
  - A rounded deductible is capped at the actual self-only deductible.
  - If rounding makes the cost sharing exceed the self-only out-of-pocket limit, the limit is
    used instead. That amount is then added to the limits or exclusions to give the total the
    patient pays.
  - A service-specific deductible that makes the shown deductible larger than the overall
    deductible is marked with an asterisk, not capped. v1 does not model separate deductibles,
    so this cannot arise in Cairn's own results; an SBC that shows one is a `mismatch` with that
    cause.

Design section 6 now uses these rules (director decision, 2026-11-23). Reconciliation applies
the same rounding and caps to the engine's results and compares every printed field.

## Rounding edges that allow a `near_match`

A `near_match` exists only for a printed field whose printed value is under $100 and which
differs from Cairn's by exactly $10, one rounding step. Fields of $100 or more round to the
nearest $100, so any difference there is a `mismatch`, escalated to the director with its cause
and never absorbed (director rule: reconciliation stays strict). The derived rows
(`cost_sharing_sum`, `total`) inherit a `near_match` only when their signed difference is
exactly the sum of their `near_match` fields' signed differences (design section 6;
confirmed by the director, 2026-11-23).

The $10 difference passes only for one of these causes, and the cause is shown with the result:

1. **Sub-cent amounts:** Cairn rounds each claim line to cents before use. For the diabetes
   scenario this moves the total from 5,601.1016 to 5,601.13. A plan's own calculator may keep
   full precision, so a field just across a $5 boundary can land one $10 step away.
2. **Tie rule:** CMS states no tie rule, and Cairn rounds half up. A plan that rounds a tie
   down shows, for example, $20 where Cairn shows $30 for $25.

Exactly $100 is not an edge: it rounds to $100 under both CMS wordings. Any other cause,
including a plan classifying a service differently from Cairn's mapping, is a `mismatch`.

## Category mapping

CMS says plans "may classify an item or service under another category". So Cairn keeps its
own mapping in the YAML (`category_mapping`).
- **17 categories** map to an SBC service row, each with a reason. Five were decided by the
  director on 2026-11-23:
  - **Professional Services: Inpatient** (delivery anesthesia) -> `childbirth_professional`.
    The template's example labels it "Specialist visit (anesthesia)".
  - **Professional Services: Obstetric Care (Bundled)** -> `prenatal_postnatal_office` (the SBC
    row "If you are pregnant: Office visits"). The global fee is dated at the first prenatal
    visit (2016-04-01), and the template lists this care as "Specialist office visits (prenatal
    care)". Correction: the first write-up said the fee is billed at delivery (2016-12-09). The
    director first chose `childbirth_professional` on that wrong premise, then re-decided with
    the correct date. Because claims run in date order, this $2,609.93 line meets most
    deductibles before any lab test.
  - **Prescription Drugs: Insulin** -> `generic_rx`, following CMS's assumption on the guide's
    Label and Assumptions sheet: "All medications are covered as generic equivalents if
    available". Correction: the first write-up also said "generic insulin glargine exists".
    That was unsourced; unbranded insulin glargine is a biosimilar, not an FDA generic.
  - **Medical Supplies** -> `durable_medical_equipment`. The template's diabetes example lists
    "Durable medical equipment (glucose meter)".
  - **Other Facility Services** (fiberglass cast supplies, billed at the specialist cast visit)
    -> `specialist`, as part of that visit. The first draft mapped it to the outpatient surgery
    facility fee, but no surgery took place.
- **One is excluded:** Over-the-counter Drugs (not covered; the full amount counts under
  "Limits or exclusions"; director decision 2026-11-23).
- **None remain undecided.**

Plans may classify these differently. A real SBC that does is reported as a `near_match` or
`mismatch` with its cause, never hidden.

## Caveats

- **Effective date:** `effective_from: 2021-01-01` is the template edition's start date. The
  guide amounts were revised in 2023: the XLSX files were last modified 2023-07-30, and the
  2019 calculator packet shows different maternity amounts. So nothing shows these exact amounts
  applied from 2021-01-01. Treat them as the current CMS values as of the 2023 revision.

- **Expired OMB control number:** every CMS document shows OMB control number 0938-1146 with
  expiration 05/31/2026. No renewed version was on the CMS page on 2026-11-23. The 2021
  template remains the one in use, but the rules data must be re-verified on every rules
  review (`verified_on`).
- **Amounts change between versions:** the 2019 calculator packet shows different maternity
  amounts from the 2023 guides. The data carries `effective_from` and the guide revision, and
  a new guide version becomes a new entry, never an edit.
- **Contradictory calculator text:** the calculator instructions' Phase 5 text is
  contradictory about deductible order. Cairn follows the group instructions' rules above. If a
  golden SBC differs because of this, it is reported as a `near_match` or `mismatch`, never
  hidden.
- **Public domain:** CMS states its website is a public domain website. The guides carry no
  separate terms. The AMA CPT notice is handled by not storing CPT codes or descriptors.
