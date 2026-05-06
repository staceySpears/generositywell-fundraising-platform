# 15 — Agentforce and Salesforce Data Cloud

> **Phase 4 — Not yet implemented.**

---

## Why this exists

Organizers are volunteers. They run events, coordinate food, manage venues, communicate with
donors — and then at the end of a campaign they are supposed to sit down and write a compelling
impact report. Most do not. The transparency loop breaks at the last step, not because organizers
do not care, but because writing from scratch is hard after an exhausting campaign.

Agentforce changes the equation. Salesforce Data Cloud unifies all campaign data — donations,
attendee counts, event outcomes, Salesforce NPSP opportunity data — into a single unified
profile. An Agentforce agent reads that data and drafts an `ImpactUpdate` with structured metrics
already filled in. The organizer reviews, edits, and publishes with one click.

The AI removes the blank-page problem. The human stays accountable for what goes live.

---

## The pieces

**Salesforce Data Cloud** — unifies data from GenerosityWell (via REST API sync) and Salesforce
NPSP into a queryable unified profile per campaign. This is where the Agentforce agent reads from.

**Agentforce agent configuration** — a no-code/low-code agent built in Salesforce's Agent Builder:
- Trigger: organizer requests a draft (or scheduled after campaign closes)
- Data sources: Data Cloud campaign profile, donation totals, event attendance
- Action: calls the GenerosityWell REST API (`POST /impact-updates`) with a DRAFT update

**`POST /impact-updates` endpoint** — accepts the agent-generated draft. The Spring Boot API
stores it as `status: DRAFT`. Organizer reviews in the dashboard and calls
`PUT /impact-updates/:id/publish` to make it live.

---

## What to understand before you build this

1. What is Salesforce Data Cloud and how does it differ from a standard Salesforce org?
2. What is an Agentforce agent, and what is the difference between an autonomous agent and
   an agent that proposes actions for a human to approve?
3. The Agentforce agent calls your REST API. How do you authenticate that request — how does
   your Spring Boot app know the call is coming from your Salesforce org and not from a
   random client?
4. An organizer edits the agent's draft before publishing. What fields should the organizer
   be allowed to edit, and what fields should be locked?

---

## Next

[16 — CI/CD Pipeline](16-cicd-pipeline.md)
