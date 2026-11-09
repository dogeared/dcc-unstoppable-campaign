# Campaign sheet field map

Source material: `DCCUnstoppableCampaignSheet.pdf` (2 pages) and the official
*Dungeon Crawler Carl: Unstoppable* rulebook, pp. 2–3 (sheet anatomy) and p. 19
(campaign progress).

This is the proposed transcription of the paper sheet into an application
schema. It is the review gate before domain classes, migrations, and UI are
written. Nothing here is final until signed off.

---

## 1. Side A — campaign state

### 1.1 Floor grid

Nine floor columns. The first three ship in the core box; later releases add
floors, and the paper sheet reserves nine.

| Field | Type | Cardinality | Derived? | Notes |
| --- | --- | --- | --- | --- |
| `floorNumber` | int | 1–9 | no | Fixed set. |
| `floorScore` | int ≥ 0 | 1 per floor | yes (proposed) | XP earned **on that floor**. Never reduced by spending. |
| `defeats` | int ≥ 0 | 1 per floor | yes (proposed) | Failed attempts. Each is **−3 final score only**; it does **not** reduce XP. |

Rulebook semantics (p. 19):

- Achievements are recorded and their XP is added to the current floor's score
  **even when the floor is lost**.
- Boss and quest XP is added to the floor score only when the floor is
  **successfully completed**.
- If a floor is retried, the same floor-score cell keeps accumulating. The cell
  becomes final once the party descends to the next floor.

This means a floor score is not strictly immutable from the start; it is
immutable once the floor is completed. The app can enforce that by making the
floor score read-only after completion while still accepting achievement XP
from failed attempts.

### 1.2 Party row

| Field | Type | Derived? | Notes |
| --- | --- | --- | --- |
| `partyName` | text | no | |
| `difficulty` | enum | no | `NORMAL`, `HARD`, `EXPERT`, `UNBEATABLE` |
| `availableXp` | int | **yes** | The paper's "XP" box. The rulebook calls it *Campaign Score (Spendable XP)*. |

`availableXp = Σ floorScore − Σ purchase.cost`

The paper label is just "XP"; the rulebook label is more precise. The app should
call it **Campaign XP (spendable)** so it is never confused with a floor score.

### 1.3 Player blocks (two)

| Field | Type | Notes |
| --- | --- | --- |
| `seat` | 1 or 2 | Solo play uses seat 1 only. |
| `crawler` | text | Character name. |
| `race` | text | |
| `class` | text | |
| `startingHealth` | int | Printed value on the starting character card. |
| `startingMana` | int | |
| `startingGold` | int | |

### 1.4 Deck list (one per player)

The sheet prints 14 fixed rows plus two signature cards:

```
Strike ×6
New Achievement! ×1
Rest ×1
Character Card A
Character Card B
Character Card C
??? ×3
Signature Card (level 1)
Signature Card (level 2)
```

Each row records a card and has three independent annotations from the sheet
instructions:

| Field | Type | Notes |
| --- | --- | --- |
| `slot` | enum/int | The printed position, so the sheet order is stable. |
| `baseCard` | text | Printed label (`Strike`, `Rest`, `Character Card A`, …). |
| `cardName` | text | Current name, including an added card typed into a `???` slot. |
| `eliminated` | bool | "cross it out" — card removed from the starting deck. |
| `upgradeName` | text | "write the name of the upgrade next to it." |
| `extraStartingHand` | bool | Checkmark purchased on Side B. |
| `mastered` | bool | Card retained through a Master a Card purchase. |
| `notes` | text | Free text. |

A structured slot list is recommended over a free-text block: it makes the
"reset your starting deck" step unambiguous and supports the checks and
upgrades the physical sheet uses.

---

## 2. Side B — progression and purchases

### 2.1 XP purchases

| # | Cost | Limit per player | Effect | Target required |
| --- | --- | --- | --- | --- |
| 1 | 5 | 1 | Permanently eliminate one Starting (level 0) card. | deck slot / card |
| 2 | 4 | 1 | Master a Level 1 card; keep it in the deck and record it. No upgrades on it. | card name |
| 3 | 8 | 1 | Master a Level 2 card; keep it in the deck and record it. No upgrades on it. | card name |
| 4 | 5 | 2 | Checkmark: add the marked card to your starting hand, then shuffle and draw the rest. | deck slot |
| 5 | X + 1 | unlimited | Permanently keep an upgrade on a `Strike` / `New Achievement!` / `Rest` card. | deck slot, upgrade name, X |
| 6 | X + 3 | unlimited | Permanently keep an upgrade on a character card or mastered card. | deck slot, upgrade name, X |

For rows 5 and 6, `X` is the **gold cost of the upgrade** and must be captured
per purchase. The cost is computed at purchase time and stored, so a later rules
change cannot rewrite history.

### 2.2 Achievements

Two columns of 15 ruled lines (30 total). Each achievement can be completed
**once per campaign**, even in two-player mode.

| Field | Type | Notes |
| --- | --- | --- |
| `name` | text | The sheet only has a line, no XP column. |
| `xp` | int | Printed on the floor mat, not on the sheet. Required for calculation. |
| `floorNumber` | int | Needed to roll XP into the right floor score. |
| `completedAt` | date/time | |

Storing `xp` is what lets the app compute the floor score instead of asking the
player to add it up by hand. A seeded per-floor catalog is possible, but the
catalog must be maintained as expansions ship; free entry with a default list is
more flexible.

---

## 3. Derived totals

```
spentXp             = Σ purchase.cost
floorScoreTotal     = Σ floorScore
totalDefeats        = Σ defeats
availableXp         = floorScoreTotal − spentXp
difficultyBonus     = 1 (NORMAL) | 5 (HARD) | 10 (EXPERT) | 20 (UNBEATABLE)
finalCampaignScore  = floorScoreTotal − (3 × totalDefeats) + difficultyBonus
```

Two properties the UI must make obvious:

- **Spending XP never lowers the final campaign score.** Purchases draw from
  `availableXp` only.
- **A defeat never lowers the XP total.** It lowers only the final score.

Optional display: `floorScore = Σ achievement.xp on floor + bossQuestXp`, where
`bossQuestXp` is the manual XP from defeated bosses and completed quests on a
successful attempt.

---

## 4. Proposed persistence model

| Entity | Key fields |
| --- | --- |
| `Campaign` | `id`, `ownerSubject`, `partyName`, `difficulty`, `status`, `version`, timestamps |
| `Participant` | `id`, `campaignId`, `seat`, `crawler`, `race`, `class`, `startingHealth/Mana/Gold`, `signature1`, `signature2` |
| `DeckEntry` | `id`, `participantId`, `position`, `baseCard`, `cardName`, `eliminated`, `upgradeName`, `extraStartingHand`, `mastered`, `notes` |
| `FloorProgress` | `id`, `campaignId`, `floorNumber`, `bossQuestXp`, `completedAt`, `notes` |
| `FloorAttempt` | `id`, `campaignId`, `floorNumber`, `playedAt`, `successful`, `notes` |
| `Achievement` | `id`, `campaignId`, `floorNumber`, `name`, `xp`, `completedAt` — unique `(campaignId, name)` |
| `Purchase` | `id`, `campaignId`, `participantId`, `benefit`, `cost`, `xValue`, `targetDeckEntryId`, `targetCard`, `notes`, `purchasedAt` |
| `CampaignInvite` | `id`, `campaignId`, `tokenHash`, `role`, `createdBySubject`, `expiresAt`, `revokedAt` |

`FloorProgress.defeats` and `FloorProgress.floorScore` are proposed as **derived
read-model values** from `FloorAttempt` and `Achievement`, not stored editable
columns. This preserves the "one attempt, one defeat" invariant and prevents the
final score from drifting out of sync with the log.

---

## 5. Open questions

1. **Floor score source.** Compute it from achievement entries + a manual
   boss/quest XP field (recommended), or keep the floor cell as one manually
   typed number?
2. **Achievement catalog.** Seed the per-floor achievements and their XP, or
   allow free entry with autocomplete against a user-editable list?
3. **Retries.** Model each play attempt separately so defeats and achievement XP
   accumulate correctly, or keep one editable row per floor?
4. **Deck list.** Structured slots with eliminated/upgraded/checkmark state
   (recommended), or a free-text list mirroring the paper lines?
5. **Mastered cards.** Dedicated "mastered cards" section, or annotate the deck
   slot and filter for it?
6. **Campaign scope.** Can a user own multiple campaigns? Should a campaign be
   cloneable for a new party or a new run?
7. **Session log.** Do you want dated session notes separate from floor attempts,
   including who played?
8. **Arcade mode.** Out of scope for this app, or should the sheet also record
   standalone arcade runs?
9. **Paper fidelity.** Should the read view reproduce both sides of the sheet
   exactly (30 achievement lines, 28 deck rows), or is a condensed summary
   acceptable?
10. **Export.** Do you need a print/PDF view of the filled sheet for the table?
