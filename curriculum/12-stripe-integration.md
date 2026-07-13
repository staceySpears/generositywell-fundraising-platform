# 12 — Stripe Integration: Payment Processing

> **Status: Implemented foundation.** PaymentIntent creation, React PaymentElement confirmation,
> and a signed Spring webhook path are present. Live deployment and end-to-end production
> operation have not been validated.

---

## Why this exists

Stripe provides payment processing, fraud tooling, and support for multiple payment methods.
Using hosted Stripe Elements keeps raw card details out of the GenerosityWell application and
reduces, but does not eliminate, the project's security and PCI-compliance responsibilities.

---

## The payment flow

```
1. Authenticated client calls POST /campaigns/{campaignId}/payment-intent
2. Spring Boot creates a Stripe PaymentIntent
3. Spring Boot returns the PaymentIntent client secret
4. React PaymentElement confirms payment through Stripe.js
5. Stripe sends a signed event to POST /webhooks/stripe
6. The Spring webhook controller handles payment_intent.succeeded
7. The campaign total is updated and the asynchronous Salesforce donation sync is invoked
```

The Spring Boot API never processes the payment directly — it creates the intent and hands off
to Stripe's frontend SDK. This keeps card data off your servers entirely.

---

## The `Donation` entity

```
Donation
  ├── id: String (UUID)
  ├── campaignId: String
  ├── donorId: String
  ├── amountCents: Long
  ├── currency: String             ("usd")
  ├── stripePaymentIntentId: String
  ├── status: DonationStatus       (PENDING | COMPLETED | FAILED | REFUNDED)
  └── createdAt: Instant
```

This entity is a target design, not the current persistence model. The implemented webhook
updates the campaign total and supporter history; it does not persist the `Donation` record
described above. Webhook idempotency and a durable donation model remain hardening work.

---

## What to understand

1. What is a Stripe PaymentIntent, and why does the client need the `client_secret` to complete
   the payment?
2. Why must monetary amounts be stored as `Long` (cents) rather than `Double`?
3. Stripe webhooks can be delivered more than once. What does "idempotent webhook handling" mean
   and how do you implement it?
4. What is Stripe test mode, and how do you use test card numbers to simulate payments without
   real money?

---

## Next

[13 — Lambda Webhooks](13-lambda-webhooks.md)
