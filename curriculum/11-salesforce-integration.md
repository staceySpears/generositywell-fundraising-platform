# 11 — Salesforce Integration: Implemented REST Seam

> **Status: Implemented; actual-org validation and delivery hardening are planned.**

---

## Why this exists

GenerosityWell includes a narrow integration seam for sending selected application data to
Salesforce. This demonstrates server-to-server OAuth and object mapping without treating the
current Salesforce org as a proven system of record or claiming production operation.

The application remains authoritative for the user-facing transaction. After the local write,
the service invokes a disabled-by-default, one-way asynchronous Salesforce sync. A Salesforce
failure does not roll back the local request.

## What is implemented

The implementation has three layers:

- `SalesforceTokenService` obtains a client-credentials OAuth token and caches it locally.
- `SalesforceClient` issues authenticated REST `POST` and `PATCH` requests.
- `SalesforceService` maps local records and runs public sync methods with `@Async`.

Service hooks currently invoke these operations:

| Local event       | Salesforce operation | Concrete mapping                                                                   |
| ----------------- | -------------------- | ---------------------------------------------------------------------------------- |
| Campaign created  | Create `Campaign`    | Name, description, dates, type, status, expected revenue, actual cost, active flag |
| Donation recorded | Create `Opportunity` | Name, amount, close date, stage, type, and CampaignId when available               |
| User registered   | Create `Contact`     | First name, last name, and email                                                   |

Money is stored locally in cents and converted to Salesforce currency-unit values at the REST
boundary. The local campaign stores the returned Salesforce Campaign ID. Contact and Opportunity
IDs are logged but are not currently persisted as local idempotency/external-ID references.

The code maps to standard `Campaign`, `Opportunity`, and `Contact` objects. That mapping alone
does not establish compatibility with NPSP or current Nonprofit Cloud. The configured default is
Salesforce API `v59.0`; the API version, field availability, picklist values, permissions, and
object semantics must be confirmed in the actual portfolio org before the integration is labeled
org-validated.

## Current failure and delivery behavior

`SalesforceService` catches exceptions and logs an error so the asynchronous failure is not
propagated to the originating request. That is failure isolation, not reliable delivery.

The current implementation does **not** provide:

- an observable per-record sync status;
- a durable queue or transactional outbox;
- bounded retry with backoff;
- an established external-ID/idempotency policy;
- reconciliation tooling;
- demonstrated end-to-end validation against the portfolio org.

There is also a race condition boundary: a donation can sync before the campaign's Salesforce ID
has been written back. In that case the Opportunity is created without `CampaignId`, and the
service logs a warning. There is no automated repair pass yet.

## Security and data rules

The integration is disabled unless `salesforce.enabled=true`. Client ID, client secret, token URL,
and instance URL are injected through configuration. Real credentials, access tokens, local auth
files, and org-specific secrets must never be committed.

Portfolio development must use synthetic data only. Do not copy real donor, payment, volunteer,
beneficiary, or organizer records into a development or portfolio org. Any future sample-data
tooling must make synthetic records identifiable and support repeatable removal.

## Hardening backlog

Before expanding the Salesforce surface:

1. Add unit tests for token caching/expiration, request construction, mapping, error paths, and
   create-versus-update behavior.
2. Define external IDs and idempotency keys for Campaign, Contact, Opportunity, and future records.
3. Add observable sync state and either bounded retry/backoff or a durable outbox.
4. Validate the configured API version, object fields, picklists, and least-privilege permissions
   in the portfolio org using synthetic records.
5. Put supported Salesforce metadata under source control in a Salesforce DX project and document
   setup, deploy, retrieve, test, reset, and secret-handling procedures.

Do not describe the seam as guaranteed delivery or production-ready until the corresponding
implementation and evidence exist.

## What to understand

1. Why is client-credentials OAuth appropriate for this server-to-server seam?
2. What consistency gap is created by making the local write before an asynchronous Salesforce
   write, and how would an outbox address it?
3. Why do standard object API names not prove NPSP or Nonprofit Cloud compatibility?
4. Which stable local identifiers should become Salesforce external IDs to make retries idempotent?

---

## Next

[12 — Stripe Integration](12-stripe-integration.md)
