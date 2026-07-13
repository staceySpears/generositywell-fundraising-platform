# 15 — Agentforce and Salesforce Data Cloud

> **Status: Planned. Data Cloud is optional/blocked by org capability.** No Agentforce or Data
> Cloud metadata is committed. Any future AI output is a draft and requires human review before
> publication.

---

## Why this exists

Organizers are volunteers. They run events, coordinate food, manage venues, communicate with
donors — and then at the end of a campaign they are supposed to sit down and write a compelling
impact report. Most do not. The transparency loop breaks at the last step, not because organizers
do not care, but because writing from scratch is hard after an exhausting campaign.

The target design uses approved campaign, donation, event, volunteer, and impact-metric context
to help an Agentforce assistant draft an `ImpactUpdate`. A direct Salesforce-data version is
acceptable; Data Cloud is optional unless a capable org and implemented data streams are
verified. The organizer reviews and edits the draft before a separate approval/publish step.

The AI removes the blank-page problem. The human stays accountable for what goes live.

---

## The pieces

**Salesforce Data Cloud (optional target)** — may unify GenerosityWell and Salesforce data if
the required org capabilities and implemented data streams are verified. Its presence in this
roadmap is not implementation evidence.

**Agentforce agent configuration** — a no-code/low-code agent built in Salesforce's Agent Builder:

- Trigger: organizer requests a draft
- Data sources: approved Salesforce campaign, donation, event, volunteer, and metric records;
  optionally Data Cloud after its implementation is verified
- Action: may call a governed GenerosityWell endpoint with a DRAFT update after authentication,
  authorization, validation, and audit logging are demonstrated

**`POST /impact-updates` endpoint** — accepts the agent-generated draft. The Spring Boot API
would store it as `status: DRAFT`. The endpoint and publishing workflow are planned. The assistant
must not publish, modify donation records, take financial action, or make compliance claims.

---

## What to understand

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
