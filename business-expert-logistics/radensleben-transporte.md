# Radensleben Transporte — SAXE Use Case Analysis

**Date:** March 2026
**Context:** CEO meeting preparation
**Website:** radensleben-transporte.de

## Company Profile

- **Location:** Dresden, Germany (phone +49 351)
- **Founded:** ~1991 (35 years in business)
- **Employees:** 29 full-time
- **Annual mileage:** ~1,000,000 km
- **Availability:** 24/7
- **Type:** Owner-operated (inhabergeführt)

## Services

| Service | Description |
|---------|-------------|
| Kunsttransporte | White-glove art transport — trained staff, custom frames, climate storage, condition reports |
| Umzüge | Private, corporate, senior moves — including piano transport, packing, parking zone permits |
| Neumöbel-Logistik | Contractor for furniture distributors — warehouse, picking, delivery, assembly |
| Messe-Logistik | End-to-end trade show logistics — transport, booth setup/teardown, personnel, material storage |
| Stückgut | LTL / partial load shipping |
| Lagerung | Climate-controlled, high-security storage with regular inspections (especially art) |
| Möbelmontagen | Furniture assembly and installation |
| Rollende Bühne | Mobile event staging |
| Werbefläche | Advertising space on fleet vehicles |

## Fleet

| Vehicle | Payload | Capacity | Special |
|---------|---------|----------|---------|
| PKW | 599 kg | — | — |
| Transporter 3.5t | 800–940 kg | 3–6 Euro pallets | Air suspension, optional tail lift |
| Transporter 3.5t (box) | — | 6 EP | 750 kg tail lift, air suspension |
| LKW 7.5t | 2,350 kg | 15 EP | 1,000 kg tail lift |
| LKW 13.5t | 6,300 kg | 18 EP | 1,500 kg tail lift, felt-lined, air suspension |
| LKW 15t | 7,950 kg | 18 EP | 1,500 kg tail lift, ADR equipment, air suspension |
| LKW 18t | 9,000 kg | 18 EP | 1,500 kg tail lift, ADR equipment, air suspension |

## Identified Use Cases (12)

### Quick Wins — high AI suitability, high value

1. **Angebotskalkulation** — Customer inquiry → structured intake → auto-select vehicle/crew/material → branded quote PDF. Highest frequency (multiple daily), directly revenue-relevant. Today done from gut feel + Excel by dispatchers. Target: 5 min instead of 30–60 min.

2. **Kunsttransport-Protokoll** — Photos + artwork metadata → AI-guided condition documentation → standardized Zustandsprotokoll PDF for insurer. Key differentiator for their premium service. Currently paper/manual.

3. **Umzugs-Projektplan** — Confirmed order → task checklist with dependencies (parking zone, packing materials, crew, elevator, key handover) → customer-facing timeline. Especially complex for corporate relocations.

4. **Rechnungsstellung & Nachkalkulation** — Completed job + time sheets → compare actual vs. quoted → invoice draft → flag deviations → approval → send. Directly affects cash flow.

### Strategic — harder for AI, high value

5. **Tourenplanung** — Day's orders + fleet availability → optimized route/load plan per vehicle. Combines LTL, multi-stop moves, vehicle matching by payload/equipment. Optimization problem.

6. **Personalplanung** — Upcoming jobs + employee skills (art handling, ADR, forklift) + availability → weekly crew schedule. Must respect working time regulations (ArbZG), fairness.

7. **Messe-Logistik Planung** — Exhibition booking → complete logistics plan: transport schedule, crew roster, material checklist, setup/teardown timeline. Multi-party coordination.

### Simple Automation — easy for AI, lower individual value

8. **Halteverbotszone beantragen** — Move address + date → determine responsible Ordnungsamt → fill application form → signage plan. Pure form-filling, city-specific.

9. **Schadensmeldung** — Incident + photos → structured damage report → route to insurer + internal file. Infrequent but important when it happens.

10. **Kundenkommunikation** — Accepted order → order confirmation → prep checklist → day-before reminder → day-of status updates. Template-driven, currently ad-hoc phone/email.

11. **Fahrzeug- & Wartungsplanung** — Mileage + TÜV/HU/AU dates + ADR certificates + driver license renewals → maintenance calendar + expiry alerts. Calendar logic, regulatory compliance.

### Long-term — needs system integration

12. **Lagerverwaltung** — Intake → storage location assignment → searchable inventory → inspection reminders → monthly storage invoices. Requires real-time state management; moderate value at 29-person scale.

## 2x2 Matrix Summary

```
                     Hoher Wertbeitrag
                           │
   Strategisch             │  Quick Wins
   · Tourenplanung         │  · Angebotskalkulation
   · Personalplanung       │  · Kunsttransport-Protokoll
   · Messe-Logistik        │  · Umzugs-Projektplan
                           │  · Rechnungsstellung
  ─────────────────────────┼─────────────────────────
   Langfristig             │  Einfache Automatisierung
   · Lagerverwaltung       │  · Halteverbotszone
                           │  · Schadensmeldung
                           │  · Kundenkommunikation
                           │  · Fahrzeugwartung
                           │
  Schwieriger für KI ──────┼────── Leichter für KI
                     Niedriger Wertbeitrag
```

## Recommended Meeting Strategy

1. **Lead with Angebotskalkulation** — highest frequency, easiest to relate to, proves the SAXE skill concept
2. **Show Kunsttransport-Protokoll** — speaks to their premium differentiator, AI vision makes it tangible
3. **Mention Rechnungsstellung** — universally painful, everyone understands invoice headaches
4. **Frame the vision:** 12 processes, one platform, their expert knowledge stays in the company and becomes scalable + consistent
5. **Key message for owner-operated Mittelstand:** SAXE doesn't replace dispatchers — it makes their expertise available 24/7, consistent, and documented
