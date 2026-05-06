# 11 — Salesforce Integration: The Two-Layer Architecture

> **Phase 3 — Not yet implemented.**

---

## Why this exists

Nonprofits already live in Salesforce. Their donor lists, grant tracking, email campaigns, and
reporting are all there. Building a parallel CRM inside GenerosityWell would mean double data
entry, two sources of truth, and a worse product.

The architecture decision: GenerosityWell owns the public-facing transaction layer (the community
experience). Salesforce owns the back-office management layer (the organizer experience). Data
flows one direction — from GenerosityWell into Salesforce — via Salesforce's REST API.

Salesforce's Nonprofit Success Pack (NPSP) is the standard data model for nonprofits in
Salesforce. When a donation is created in GenerosityWell, we sync it as an NPSP `Opportunity`.
When a user registers, we sync them as a `Contact`. Organizers then manage relationships,
run reports, and track campaign health entirely in Salesforce — the tool they already know.

---

## The pieces you will build

**OAuth 2.0 Connected App** — Salesforce uses OAuth 2.0 (client credentials flow for server-to-server)
to authenticate API calls from GenerosityWell.

**`SalesforceClient`** — a Spring `@Service` that wraps the Salesforce REST API:
- `createContact(User user)` — called when a user registers
- `createOpportunity(Donation donation)` — called when a donation is confirmed
- `updateCampaign(Campaign campaign)` — syncs campaign goal and raised amount

**Event hooks in services** — `UserService.createUser` calls `salesforceClient.createContact` after
saving to DynamoDB. Order matters: DynamoDB first (your system of record), Salesforce second
(downstream sync). A Salesforce failure should not roll back the user registration.

---

## What to understand before you build this

1. What is the difference between OAuth 2.0 authorization code flow and client credentials flow?
   Which is appropriate for server-to-server communication?
2. Salesforce has rate limits (API call limits per day). How would you handle a Salesforce API
   failure without losing the data that was supposed to sync?
3. What is Salesforce NPSP, and what standard objects does it add (`Opportunity`, `Contact`,
   `Account`)?
4. If GenerosityWell is the system of record for transactions, what does "Salesforce as the
   system of record" mean? What is Salesforce the authoritative source for?

---

## Next

[12 — Stripe Integration](12-stripe-integration.md)
