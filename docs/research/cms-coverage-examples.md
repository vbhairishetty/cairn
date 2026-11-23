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
  to the nearest ten.
- **Caps:**
  - A rounded deductible is capped at the actual self-only deductible.
  - If rounding makes the cost sharing exceed the self-only out-of-pocket limit, the limit is
    used instead. That amount is then added to the limits or exclusions to give the total the
    patient pays.

Design section 6 now uses these rules (director decision, 2026-11-23). Reconciliation applies
the same rounding and caps to the engine's results and compares every printed field.

## Category mapping

CMS says plans "may classify an item or service under another category". So Cairn keeps its
own mapping in the YAML (`category_mapping`).
- **13 categories** map cleanly to an SBC service row, each with a reason.
- **Five need a decision from the director:**
  - **Professional Services: Inpatient (delivery anesthesia).** The SBC row is
    "Childbirth/delivery professional services", but the template's example lists it as
    "Specialist visit (anesthesia)".
  - **Professional Services: Obstetric Care (Bundled).** The global obstetric fee covers
    prenatal visits, delivery and postnatal care, so it fits both "Office visits" and
    "Childbirth/delivery professional services".
  - **Prescription Drugs: Insulin.** Whether it is generic, preferred brand or specialty
    depends on the plan's formulary.
  - **Medical Supplies** (syringes, lancets, test strips, glucose meter). The template's example
    calls the meter "Durable medical equipment", but plans often cover supplies under the
    pharmacy benefit.
  - **Over-the-counter Drugs.** There is no SBC row; they are usually not covered, so they
    likely count under "Limits or exclusions".

Until these are decided, reconciliation reports the affected scenarios as `unmappable` instead
of guessing.

## Caveats

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
