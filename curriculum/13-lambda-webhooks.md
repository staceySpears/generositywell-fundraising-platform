# 13 — Lambda Webhooks: Bringing Lambda Back Intentionally

> **Status: Planned.** The current webhook is handled by Spring Boot; Lambda is not the active
> Stripe webhook architecture.

---

## Why this exists

In Phase 1, the modernization removed a Lambda that acted as a synchronous DynamoDB proxy.
The current Stripe webhook is implemented in Spring Boot. Moving it to Lambda is only a target
architecture and should happen only if its operational tradeoffs are justified.

The capstone Lambda: synchronous, blocking, added latency, no business logic.
The Stripe webhook Lambda: asynchronous, event-driven, isolated, stateless.

---

## Target Lambda pattern to evaluate

Stripe sends a `POST` to your webhook endpoint when a payment event occurs. You must respond
with `200 OK` within a few seconds or Stripe retries. The actual processing — updating the
donation status, syncing to Salesforce, updating the campaign's raised total — takes longer
than Stripe will wait.

The pattern:

```
Stripe → POST /webhooks/stripe (Lambda) → 200 OK immediately
                                        → write to DynamoDB (async)
                                        → call Salesforce API (async)
                                        → update campaign total
```

Lambda could handle each webhook event as a separate stateless invocation, but retry behavior
depends on the chosen ingress and invocation model. A public synchronous Lambda endpoint does not
by itself provide durable asynchronous delivery, idempotency, or a dead-letter queue.

The legacy `ServiceLambda` module is not currently this webhook handler. Reusing or replacing it
would require a separate implementation PR, deployment configuration, idempotency tests, failure
handling, and evidence that it deploys independently.

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
