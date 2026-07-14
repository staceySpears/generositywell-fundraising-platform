# 09 — Campaign Entity and Status Lifecycle

> **Status: Implemented.** The description below reflects the current repository model.

---

## Why this exists

The capstone modeled gatherings as events. GenerosityWell adds a fundraising `Campaign` with a
goal, dates, supporter history, and lifecycle. A `FundraisingEvent` can reference a campaign by
ID, so event logistics and fundraising state remain separate.

Impact reporting is planned. Its appearance in product rationale does not mean an `ImpactUpdate`
model or publishing workflow currently exists.

## Implemented persistence model

```text
CampaignRecord
  ├── id: String (DynamoDB partition key)
  ├── name: String
  ├── date: LocalDate
  ├── deadline: LocalDate
  ├── category: String
  ├── user: User (campaign owner)
  ├── supporters: List<Supporter>
  ├── address: String
  ├── description: String
  ├── goalAmount: Long (cents)
  ├── currentAmount: Long (cents)
  ├── status: String (CampaignStatus name)
  └── salesforceCampaignId: String (nullable)
```

Money is always stored in cents. The Salesforce campaign ID is written back asynchronously after
a successful Salesforce create call and may remain null when the integration is disabled or the
sync fails.

## Implemented lifecycle

`CampaignStatus` defines:

```text
ACTIVE → FUNDED → CLOSED
```

- New campaigns are active and accept donations.
- Reaching the goal changes the campaign to funded; overfunding remains allowed.
- The owner can close a campaign, after which donations are rejected.
- The path ID is authoritative on update, and the campaign owner cannot be replaced by the client.

The current model does not use the earlier conceptual `DRAFT` state.

## What to understand

1. Why are `goalAmount` and `currentAmount` stored as `Long` cents?
2. Why is `FundraisingEvent` stored separately with a campaign ID instead of embedded in the
   campaign record?
3. Why must donation totals be changed by the donation/payment flow rather than a general campaign
   update request?
4. What consistency risk is introduced by writing `salesforceCampaignId` asynchronously?

---

## Next

[10 — Spring Security and JWT Auth](10-spring-security-jwt.md)
