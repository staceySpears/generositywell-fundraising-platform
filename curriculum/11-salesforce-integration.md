# 11 — Salesforce Integration: The Two-Layer Architecture

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

## The two-layer principle in practice

The key architectural rule: **DynamoDB first, Salesforce second.**

When a user registers, the sequence is:
1. `UserService.createUser` saves the new user to DynamoDB
2. `UserService.createUser` calls `salesforceService.syncContact(user)` — async

A Salesforce API failure (rate limit, network blip, credentials rotation) must not roll back
the user registration. DynamoDB is the system of record. Salesforce is a downstream subscriber.
If the sync fails, the data can be replayed; if the registration fails, the user is lost.

This is why the Salesforce calls are `@Async` — they run on a separate thread and cannot block
or throw into the calling transaction.

---

## The implementation

`SalesforceClient` wraps the Salesforce REST API using a `RestTemplate`. It handles the OAuth
2.0 client credentials flow (server-to-server: no user interaction, no redirect) to obtain an
access token, then makes API calls to create and update Salesforce records.

`SalesforceService` is the Spring `@Service` that orchestrates the sync logic. It is injected
into `UserService` and `CampaignService`. Both services call it after their own writes succeed.
The `@Async` annotation means each sync call runs in a thread pool managed by `AsyncConfig` —
the calling service method returns immediately.

The sync targets:
- **User registration** → Salesforce `Contact` (NPSP standard object)
- **Donation confirmed** → Salesforce `Opportunity` (NPSP standard object for donations)
- **Campaign updated** → Salesforce Campaign record

---

## What to understand

1. What is the difference between OAuth 2.0 authorization code flow and client credentials flow?
   Which is appropriate for server-to-server communication, and why?
2. Salesforce has API call limits per day (per org). How would you handle a Salesforce API
   failure without losing the data that was supposed to sync?
3. What is Salesforce NPSP, and what standard objects does it add (`Opportunity`, `Contact`,
   `Account`)?
4. Why are the Salesforce sync calls `@Async`? What would happen if they were synchronous?
5. If the Salesforce sync fails for a donation, the donation is recorded in DynamoDB but not
   in Salesforce. How would you detect and replay missed syncs? (Hint: the AuditLog table
   records every PAYMENT_SUCCEEDED event with campaignId and donorId.)

---

## Next

[12 — Stripe Integration](12-stripe-integration.md)
