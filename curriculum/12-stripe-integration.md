# 12 — Stripe Integration: Payment Processing

> **Phase 4 — Not yet implemented.**

---

## Why this exists

Stripe is the industry standard for payment processing in modern web applications. It handles
PCI compliance, fraud detection, and payment method support (cards, bank transfers, wallets)
so you do not have to. You never see or store raw card numbers — Stripe's frontend SDK
tokenizes the card on the client side, and you only ever work with a `PaymentIntent` object.

---

## The payment flow

```
1. Client calls POST /donations (amount, campaignId)
2. Spring Boot creates a Stripe PaymentIntent
3. Spring Boot returns the PaymentIntent client_secret to the client
4. Client completes payment in the browser using Stripe.js + the client_secret
5. Stripe sends a webhook event to POST /webhooks/stripe
6. Lambda webhook handler processes payment.intent.succeeded
7. Donation record written to DynamoDB; Salesforce synced
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

A `Donation` is created with status `PENDING` when the PaymentIntent is created. The Stripe
webhook handler updates it to `COMPLETED` or `FAILED` when Stripe confirms the outcome.

---

## What to understand before you build this

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
