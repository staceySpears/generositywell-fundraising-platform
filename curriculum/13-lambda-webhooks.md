# 13 — Lambda Webhooks: Bringing Lambda Back Intentionally

> **Phase 4 — Not yet implemented.**

---

## Why this exists

In Phase 1, we removed the Lambda because it was a synchronous DynamoDB proxy — the wrong
tool for the job. In Phase 4, Lambda comes back for Stripe webhook processing — the right
tool for the job. Understanding the difference is the point of this module.

The capstone Lambda: synchronous, blocking, added latency, no business logic.
The Stripe webhook Lambda: asynchronous, event-driven, isolated, stateless.

---

## Why Lambda is correct for webhooks

Stripe sends a `POST` to your webhook endpoint when a payment event occurs. You must respond
with `200 OK` within a few seconds or Stripe retries. The actual processing — updating the
donation status, syncing to Salesforce, updating the campaign's raised total — takes longer
than Stripe will wait.

The pattern:

```
Stripe → POST /webhooks/stripe (Lambda) → 200 OK immediately
                                        → write to DynamoDB (async)
                                        → call Salesforce API (async)
                                        → update Campaign.raisedAmountCents
```

Lambda handles each webhook event as a separate invocation. If processing fails, Lambda retries.
Each invocation is stateless — no shared memory, no risk of one payment affecting another.

The `ServiceLambda` module, which was dormant after Phase 1, becomes this webhook handler.
It stays as a separate module in the Gradle build for the same reason it was originally
separate: it deploys independently, scales independently, and fails independently of the
Spring Boot app.

---

## What to understand before you build this

1. What is the difference between synchronous and asynchronous processing? Why does it matter
   for webhook handling specifically?
2. Stripe may deliver the same webhook event more than once. How does the Lambda ensure that
   a duplicate event does not create a duplicate donation record?
3. What is a Lambda "dead letter queue" and when would you use one?
4. The webhook handler calls both DynamoDB and Salesforce. If the Salesforce call fails, should
   the Lambda fail and retry? What are the consequences of retrying?

---

## Next

[14 — Impact Reporting](14-impact-reporting.md)
