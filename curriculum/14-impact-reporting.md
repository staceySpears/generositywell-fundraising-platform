# 14 — Impact Reporting: Closing the Transparency Loop

> **Phase 4 — Not yet implemented.**

---

## Why this exists

This is the feature that separates GenerosityWell from every generic fundraising tool. Donors
give money and then have no idea what happened to it. GiveButter shows a progress bar. That is
not transparency — that is just tracking whether the goal was met.

Radical transparency means: every donor can see exactly what their contribution enabled.
An organizer posts a structured `ImpactUpdate` — "With the funds raised, we served 450 meals
and distributed 120 backpacks." That update is linked to the campaign, timestamped, and
visible on the public campaign page. Donors who contributed during the campaign period see it
in their donor dashboard.

The Agentforce agent in Phase 4 drafts these updates automatically from campaign data — but
organizers approve and publish. The human stays in the loop; the AI removes the blank-page
friction of writing the update.

---

## The `ImpactUpdate` entity

```
ImpactUpdate
  ├── id: String (UUID)
  ├── campaignId: String
  ├── authorId: String (organizerId)
  ├── headline: String
  ├── body: String
  ├── metrics: List<ImpactMetric>   (structured key-value: "Meals served: 450")
  ├── status: UpdateStatus          (DRAFT | PUBLISHED)
  └── publishedAt: Instant
```

```
ImpactMetric
  ├── label: String   ("Meals served")
  └── value: String   ("450")
```

Storing metrics as a structured list (not free text) means the Agentforce agent can read them
programmatically and generate accurate summaries.

---

## What to understand before you build this

1. Why are `ImpactMetric` values stored as `String` rather than `Long`, even for numbers?
2. `ImpactUpdate.status` has `DRAFT` and `PUBLISHED`. Why does the `POST /impact-updates`
   endpoint always create a `DRAFT`, and what endpoint publishes it?
3. The Agentforce agent will read `ImpactMetric` data to draft the update. What format should
   the metrics be in for the agent to reliably parse them?
4. A donor contributed to Campaign X in January. A new ImpactUpdate is published for Campaign X
   in March. How does the donor dashboard surface that update to them?

---

## Next

[15 — Agentforce and Salesforce Data Cloud](15-agentforce.md)
