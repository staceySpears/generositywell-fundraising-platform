# 13 — Webhook Handling: Spring Boot vs Lambda

## Why this module exists

In Phase 1, we removed the Lambda because it was a synchronous DynamoDB proxy — the wrong tool
for the job. The original design for Phase 4 planned to bring Lambda back for Stripe webhook
processing. This module explains why the webhook landed in Spring Boot instead, and what
conditions would make Lambda the right choice.

---

## What was planned vs what was built

**Planned:** A dedicated `ServiceLambda` module would handle `payment_intent.succeeded`. Stripe
would invoke the Lambda function URL directly. The Lambda would write to DynamoDB and sync
Salesforce asynchronously.

**Built:** `StripeWebhookController` in the Spring Boot `Application` module handles the webhook
at `POST /webhooks/stripe`. The controller verifies the Stripe signature, reads the PaymentIntent
metadata, and calls `campaignService.addDonation()`. It returns 200 immediately; errors are
logged, not propagated.

---

## Why Spring Boot is correct for this project right now

**The volume argument:** Lambda shines at high webhook throughput — thousands of Stripe events
per second, each needing isolated, stateless processing. GenerosityWell is a neighborhood
fundraising platform. Webhook volume is low. The operational overhead of a separate Lambda
deployment (IAM roles, function URLs, separate CloudWatch log groups, cold start latency)
would outweigh the benefit.

**The DRY argument:** `addDonation` is already in `CampaignService`. A Lambda would need either
its own copy of that logic or an internal HTTP call back to the Spring Boot app. Both are worse
than calling the service method directly.

**The deployment argument:** The `Application` module deploys to ECS (or Elastic Beanstalk).
Adding a Lambda adds a second deployment artifact, a second CI step, and a second surface for
configuration drift. At current scale, this cost is not justified.

---

## When Lambda would be the right call

Lambda becomes correct when any of these conditions hold:

- **Burst traffic:** A viral campaign generates 10,000 webhook events in a minute. Spring Boot
  on a fixed ECS task count cannot scale to match; Lambda scales automatically per event.
- **Isolation:** If webhook processing starts requiring complex external calls (fraud screening,
  Salesforce bulk sync, email notifications) that can fail independently of the payment record,
  isolating them in Lambda protects the main API from cascading failures.
- **Cold tolerance:** Webhook endpoints have relaxed latency requirements compared to interactive
  API endpoints. Lambda's cold starts (100–500ms) are acceptable for a webhook that just needs
  to return 200 quickly and process asynchronously.

---

## The pattern regardless of host

The webhook handling pattern is the same whether implemented in Spring Boot or Lambda:

```
1.  Verify Stripe signature — reject unauthenticated requests immediately
2.  Return 200 as soon as verification passes
3.  Process the event — write to DynamoDB, sync Salesforce
4.  Catch application errors; log them; do not re-throw
5.  Never return non-2xx after signature verification succeeds
```

Step 5 is the most counterintuitive. If your processing code has a bug and you return 500,
Stripe will retry the event for 72 hours. You will fire the same buggy code thousands of times.
Returning 200 and logging the error is the correct posture — the AuditLog and Stripe dashboard
provide the reconciliation tools you need.

---

## The idempotency gap

The current implementation does not deduplicate webhook events. If Stripe delivers
`payment_intent.succeeded` twice for the same PaymentIntent (which it can — Stripe guarantees
at-least-once delivery), `addDonation` will be called twice and the campaign's
`raisedAmountInCents` will be incremented twice.

The fix: before calling `addDonation`, check whether the PaymentIntent ID has already been
processed. This requires either a DynamoDB write to a `ProcessedWebhooks` table or an idempotency
flag on the `CampaignRecord`. This is the next engineering task in the payment flow.

---

## What to understand

1. What is the difference between synchronous and asynchronous webhook processing? Why does
   returning 200 immediately matter?
2. Stripe guarantees at-least-once delivery. What does that mean, and how does it affect the
   design of your webhook handler?
3. What is a Lambda "dead letter queue" and when would you use one?
4. If you moved this webhook handler to a Lambda, which part of the current codebase would you
   have to duplicate or restructure? Is that duplication acceptable?
5. The controller catches all exceptions from `addDonation` and logs them. What monitoring
   would you add to ensure those logged errors are noticed quickly?

---

## Next

[14 — Impact Reporting](14-impact-reporting.md)
