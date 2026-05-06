# 09 — Campaign Entity and Status Lifecycle

> **Phase 3 — Not yet implemented.**
> This module will be completed when Campaign is built. The conceptual section is here now
> so you understand the design before you write the code.

---

## Why this exists

The capstone modeled everything as `Event`. A fundraising platform needs a richer structure:
a **Campaign** is a fundraising initiative with a goal, a timeline, and impact reporting.
A **FundraisingEvent** is a discrete gathering that belongs to a Campaign. One campaign can
span multiple events over weeks or months.

Without this distinction, there is nowhere to attach donations (do you donate to an event or
to the campaign?), nowhere to track aggregate progress toward a goal, and nowhere for the
Agentforce agent to pull campaign-level data when drafting impact updates.

---

## The data model

```
Campaign
  ├── id: String (UUID)
  ├── organizerId: String (FK → User.id)
  ├── title: String
  ├── description: String
  ├── goalAmountCents: Long          (monetary amounts are always stored as cents)
  ├── raisedAmountCents: Long        (updated by Stripe webhook in Phase 4)
  ├── status: CampaignStatus         (DRAFT | ACTIVE | CLOSED)
  ├── startDate: LocalDate
  ├── endDate: LocalDate
  └── events: List<FundraisingEvent> (or stored separately, FK → Campaign.id)
```

---

## Status lifecycle

A `Campaign` moves through states. Not every transition is valid:

```
DRAFT → ACTIVE    (organizer publishes)
ACTIVE → CLOSED   (campaign ends or organizer closes manually)
DRAFT → CLOSED    (organizer cancels before publishing)
CLOSED → DRAFT    ✗ not allowed — a closed campaign cannot be reopened
ACTIVE → DRAFT    ✗ not allowed — a published campaign cannot be unpublished
```

Enforcing these transitions in the service layer (not just in the database) means the business
rule lives in one place. You will implement this as an enum with an `isTransitionAllowed` method.

---

## What to understand before you build this

1. Why store monetary amounts as `Long` (cents) rather than `Double` or `BigDecimal`?
2. What is the relationship between `Campaign` and `FundraisingEvent` in DynamoDB terms — do
   they live in the same table or different tables?
3. Where does status transition logic belong — in the controller, the service, or the entity?
4. `raisedAmountCents` will be updated by the Stripe webhook handler. Why should the HTTP
   `PUT /campaigns/:id` endpoint NOT update this field directly?

---

## Next

[10 — Spring Security and JWT Auth](10-spring-security-jwt.md)
