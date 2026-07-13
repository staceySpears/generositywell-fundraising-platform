# 14 — Impact Reporting: Closing the Transparency Loop

> **Status: Planned.** No application model, approval workflow, or Salesforce metadata for
> impact reporting is committed yet.

---

## Why this exists

This planned feature would connect a campaign's contributions to reviewed outcome reporting,
rather than stopping at a fundraising progress total.

Radical transparency means: every donor can see exactly what their contribution enabled.
An organizer posts a structured `ImpactUpdate` — "With the funds raised, we served 450 meals
and distributed 120 backpacks." That update is linked to the campaign, timestamped, and
visible on the public campaign page. Donors who contributed during the campaign period see it
in their donor dashboard.

A planned Agentforce assistant may draft these updates from approved context, but it cannot
publish them. A person must review and edit the draft before approval, and only an approved update
may be published.

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
  ├── status: UpdateStatus          (DRAFT | IN_REVIEW | APPROVED | PUBLISHED)
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

## What to understand

1. Why are `ImpactMetric` values stored as `String` rather than `Long`, even for numbers?
2. Why must every create action, including an AI-assisted action, produce `DRAFT`, and what
   permissions and validation should govern review, approval, and publication?
3. The Agentforce agent will read `ImpactMetric` data to draft the update. What format should
   the metrics be in for the agent to reliably parse them?
4. A donor contributed to Campaign X in January. A new ImpactUpdate is published for Campaign X
   in March. How does the donor dashboard surface that update to them?

---

## Next

[15 — Agentforce and Salesforce Data Cloud](15-agentforce.md)
