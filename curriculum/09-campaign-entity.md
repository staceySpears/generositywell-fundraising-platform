# 09 — Campaign Entity, Supporter Model, and Status Lifecycle

## Why this exists

The capstone had one entity: `Event`. A fundraising platform needs two distinct concepts that
operate at different levels:

- **`Campaign`** — a fundraising initiative with a goal, a timeline, and a roster of supporters.
  Donations accumulate here. Salesforce syncs at this level. The Agentforce agent reads from here.
- **`FundraisingEvent`** — a discrete gathering (a bake sale, a 5K run) that can be associated
  with a campaign. Volunteers RSVP here.

Collapsing these into one entity means you cannot track cumulative donations separately from event
attendance, cannot close a campaign without closing every event, and cannot report on campaign-level
impact without combing through event records.

---

## The data model

The Campaign entity as built:

```
Campaign
  ├── id: String (UUID)
  ├── user: User                      (creator; ownership never changes after creation)
  ├── name: String
  ├── description: String
  ├── goalAmountInCents: Long         (monetary amounts are always stored as cents)
  ├── raisedAmountInCents: Long       (incremented by addDonation, never by PUT /campaigns)
  ├── startDate: LocalDate
  ├── endDate: LocalDate
  ├── status: CampaignStatus          (DRAFT | ACTIVE | CLOSED | FUNDED)
  └── supporters: List<Supporter>    (embedded in DynamoDB, serialized as a custom type)
```

Note: `FundraisingEvent` is stored in a separate DynamoDB table with its own `campaignId` field.
Events are not embedded in the Campaign record — embedding would create unbounded item growth and
make GSI queries impossible.

---

## The Supporter model and serialization

`Supporter` is a donor entry embedded in the campaign record. It is not a standalone DynamoDB
table — it lives as a list attribute inside `CampaignRecord`.

```java
// service/model/Supporter.java
public class Supporter {
    private String id;           // user ID of the donor (null for anonymous Stripe donors)
    private String name;
    private String email;
    private Long amountInCents;  // cumulative total across all donations to this campaign
    private String donationDate; // ISO date of most recent donation
}
```

Supporters are stored in DynamoDB as a `StringSet`. Each entry is serialized by
`SupporterTypeConverter`, which handles two wire formats for backward compatibility:

**v2 (current):** `id|name|email|amountInCents|donationDate` — pipe-delimited; name and email
are URL-encoded to safely handle commas, pipes, and other special characters.

**v1 (legacy):** `id x name x email` — the original capstone format, no amount tracking.

The converter reads both formats and always writes v2. This is the backward-compatibility
pattern for embedded serialization: you own both sides of the wire, so you can migrate the
format gradually without a database migration.

---

## The status lifecycle

```
DRAFT  → ACTIVE    (organizer publishes via POST /campaigns/{id}/publish — not yet wired)
ACTIVE → CLOSED    (organizer closes via POST /campaigns/{id}/close)
ACTIVE → FUNDED    (automatic: triggered when raisedAmountInCents >= goalAmountInCents)
DRAFT  → CLOSED    (organizer cancels before publishing)
CLOSED → *         ✗ terminal — closed campaigns cannot change state
FUNDED → *         ✗ terminal — funded campaigns are also closed
```

`FUNDED` is the state that distinguishes "we hit the goal" from "we just decided to stop." Both
`CLOSED` and `FUNDED` are terminal, but the Agentforce agent and Salesforce sync treat them
differently — `FUNDED` triggers the impact reporting draft; `CLOSED` does not.

---

## The `addDonation` method

This is the core mutation. It runs on two paths: the direct-donate endpoint
(`POST /campaigns/{id}/donate`) and the Stripe webhook (`payment_intent.succeeded`).

```java
// CampaignService.addDonation
public void addDonation(String campaignId, long amountInCents, String donorId) {
    CampaignRecord record = campaignDao.findById(campaignId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    record.setRaisedAmountInCents(record.getRaisedAmountInCents() + amountInCents);

    // (1) Upsert the Supporter entry when donorId is present
    if (donorId != null) {
        List<Supporter> supporters = new ArrayList<>(record.getSupporters());
        Supporter existing = supporters.stream()
                .filter(s -> donorId.equals(s.getId()))
                .findFirst().orElse(null);

        if (existing != null) {
            existing.setAmountInCents(existing.getAmountInCents() + amountInCents);
            existing.setDonationDate(LocalDate.now().toString());
        } else {
            // fetch user profile for name/email, fall back to ID-only entry
            User donor = userService.getUserById(donorId);
            supporters.add(new Supporter(donorId,
                    donor != null ? donor.getName() : donorId,
                    donor != null ? donor.getEmail() : "",
                    amountInCents, LocalDate.now().toString()));
        }
        record.setSupporters(supporters);
    }

    // (2) Auto-transition to FUNDED when goal is met
    if (record.getRaisedAmountInCents() >= record.getGoalAmountInCents()
            && CampaignStatus.ACTIVE.equals(record.getStatus())) {
        record.setStatus(CampaignStatus.FUNDED);
    }

    campaignDao.save(record);
    cache.evict(campaignId);
    // (3) Synchronous audit log for all payment events
    auditLogService.log("PAYMENT_SUCCEEDED", campaignId, donorId, ...);
}
```

**(1)** `donorId` is nullable. Anonymous Stripe payments (a supporter who pays without logging
in) still increment `raisedAmountInCents` but cannot be attributed to a user record.

**(2)** Status transition happens inside the same write as the donation increment. This is
intentional — the state must be consistent. There is no separate endpoint to trigger `FUNDED`.

**(3)** Financial audit logs are synchronous, not async. If the audit write fails, the donation
write should also fail. Non-financial events (profile edits, RSVP changes) use the async audit
path.

---

## The QR code card (frontend)

`CampaignDetailPage` renders a QR code for every campaign using `qrcode.react`. The code
encodes the campaign's public URL so organizers can print flyers — a donor scans the code and
lands directly on the donation page.

```jsx
// No backend changes. The QR code is purely a frontend concern.
import { QRCodeSVG } from 'qrcode.react';

const campaignUrl = `${window.location.origin}/campaigns/${campaign.id}`;
<QRCodeSVG value={campaignUrl} size={160} />
```

A download button serializes the rendered SVG to a file. This is the right place to note the
principle: when a feature requires no server state — no persistence, no auth, no computation —
it belongs entirely in the frontend. Adding a backend endpoint to "generate" a QR code would be
over-engineering.

---

## What to understand

1. Why is `raisedAmountInCents` not updatable via `PUT /campaigns/{id}`? What problem would
   that create?
2. `Supporter` is embedded in the campaign record rather than stored in its own table. What
   is the trade-off? When would you move it to its own table?
3. The `SupporterTypeConverter` reads both v1 and v2 formats but always writes v2. What happens
   to a v1 record the first time a new donation arrives?
4. `addDonation` is called from both `CampaignController.donate` and `StripeWebhookController`.
   One passes a real `donorId`; the other passes `null`. Trace both paths through the method
   and describe the difference in what gets written to DynamoDB.
5. Why is the `FUNDED` transition checked inside `addDonation` rather than in a scheduled job?
   What are the trade-offs of each approach?

---

## Next

[10 — Spring Security and JWT Auth](10-spring-security-jwt.md)
