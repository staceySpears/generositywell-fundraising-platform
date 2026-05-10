# 12 — Stripe Integration: Payment Processing End to End

## Why this exists

Stripe is the industry standard for payment processing. It handles PCI compliance, fraud
detection, and payment method support so you do not have to. You never see or store raw card
numbers — Stripe's frontend SDK tokenizes the card on the client side, and your backend only
ever works with a `PaymentIntent` object.

The PaymentIntent pattern separates two concerns: your server creates the *intent* to charge
(knowing the amount and campaign), and the client completes the actual *payment* using Stripe's
React components. Your server learns the result asynchronously via webhook.

---

## The full flow

```
1.  User enters donation amount on CampaignDetailPage
2.  Frontend calls POST /campaigns/{id}/payment-intent (auth: JWT → donorId extracted)
3.  CampaignController reads donorId from Authentication, calls StripeService
4.  StripeService creates PaymentIntent with campaignId + donorId in metadata
5.  Backend returns { clientSecret, paymentIntentId, amountInCents }
6.  Frontend mounts <Elements> provider with the clientSecret
7.  User enters card via <PaymentElement> (Stripe-hosted, no card data touches your JS)
8.  stripe.confirmPayment() fires — payment completes in-place (redirect:'if_required')
9.  Frontend shows success card; webhook will record donation asynchronously
10. Stripe sends payment_intent.succeeded to POST /webhooks/stripe
11. StripeWebhookController verifies signature, reads metadata, calls addDonation()
12. addDonation increments raisedAmountInCents and upserts the Supporter entry
```

---

## The backend

### `StripeService` — creating the PaymentIntent

```java
public PaymentIntentResponse createPaymentIntent(
        String campaignId, Long amountInCents, String donorId) {
    try {
        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .putMetadata("campaignId", campaignId)    // (1)
                .addPaymentMethodType("card");

        if (donorId != null) {
            builder.putMetadata("donorId", donorId);      // (2)
        }

        PaymentIntent intent = PaymentIntent.create(builder.build());
        return new PaymentIntentResponse(
                intent.getClientSecret(), intent.getId(), amountInCents);

    } catch (StripeException e) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Payment provider error: " + e.getMessage());    // (3)
    }
}
```

**(1)** `campaignId` is stored in the PaymentIntent metadata. When the webhook fires, your
handler reads this field to know which campaign to credit. Without it, the webhook has no
way to route the payment.

**(2)** `donorId` is optional. Authenticated donors get their giving history recorded;
anonymous payments still increment the campaign total but cannot be attributed to a user.

**(3)** Stripe errors surface as `StripeException`. We re-throw as HTTP 502 (Bad Gateway)
because the failure originated in an upstream service, not in our code.

---

### `StripeWebhookController` — processing the result

```java
@PostMapping(value = "/stripe", consumes = "application/json")
public ResponseEntity<String> handleWebhook(
        @RequestBody byte[] payload,
        @RequestHeader("Stripe-Signature") String sigHeader) {

    Event event;
    try {
        event = Webhook.constructEvent(          // (1)
                new String(payload), sigHeader, webhookSecret);
    } catch (SignatureVerificationException e) {
        return ResponseEntity.badRequest().body("Invalid signature");
    }

    if ("payment_intent.succeeded".equals(event.getType())) {
        Optional<StripeObject> obj = event.getDataObjectDeserializer().getObject();
        if (obj.isPresent() && obj.get() instanceof PaymentIntent intent) {
            String campaignId = intent.getMetadata().get("campaignId");
            String donorId    = intent.getMetadata().get("donorId");   // null for anonymous
            long   amount     = intent.getAmount();

            if (campaignId != null) {
                try {
                    campaignService.addDonation(campaignId, amount, donorId);
                } catch (Exception e) {
                    log.error("Failed to record donation: {}", e.getMessage()); // (2)
                }
            }
        }
    }

    return ResponseEntity.ok("received");   // (3)
}
```

**(1)** `Webhook.constructEvent` uses the `Stripe-Signature` header and your webhook signing
secret to verify the payload has not been tampered with. This is the only authentication on
this endpoint — it is not JWT-protected, because Stripe does not have your users' tokens.

**(2)** Application errors are caught and logged, not re-thrown. The webhook always returns 200
after signature verification succeeds. If you return a non-2xx, Stripe will retry the event
for up to 72 hours — retrying a network error is useful; retrying a bug in your code will
just fire the bug repeatedly.

**(3)** Always return 200. If recording the donation fails, you have a bug to fix, not a payment
to retry. The `AuditLog` and Stripe dashboard are your reconciliation tools.

---

## The frontend

### `src/lib/stripe.js` — singleton loader

```js
import { loadStripe } from '@stripe/stripe-js';

// Called once at module load, not inside a render.
// loadStripe is async and returns a Promise<Stripe>; calling it inside a
// component would re-initialize Stripe on every render.
export const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY);
```

### Three-step flow in `CampaignDetailPage`

The page manages a simple state machine: `amount` → `card` → `success`.

```jsx
// Step 1: user enters amount, frontend calls createPaymentIntent
// Step 2: mount Elements provider + StripePaymentForm
// Step 3: onSuccess callback advances to success card

{step === 'card' && clientSecret && (
    <Elements stripe={stripePromise} options={{ clientSecret }}>
        <StripePaymentForm
            amountInCents={amountInCents}
            onSuccess={() => setStep('success')}
            onBack={() => setStep('amount')}
        />
    </Elements>
)}
```

`<Elements>` is the Stripe context provider — it must wrap any component that uses
`useStripe()` or `useElements()`. The `clientSecret` prop links the provider to the specific
PaymentIntent created by your backend.

### `StripePaymentForm` — card entry and confirmation

```jsx
export default function StripePaymentForm({ amountInCents, onSuccess, onBack }) {
    const stripe   = useStripe();    // (1)
    const elements = useElements();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsProcessing(true);

        const { error } = await stripe.confirmPayment({
            elements,
            confirmParams: { return_url: window.location.href },
            redirect: 'if_required',   // (2)
        });

        if (error) {
            setErrorMessage(error.message);
            setIsProcessing(false);
        } else {
            onSuccess();  // webhook records the donation asynchronously
        }
    };

    return (
        <form onSubmit={handleSubmit}>
            <PaymentElement />   {/* (3) Stripe-hosted card input */}
            <button disabled={isProcessing || !stripe}>
                Donate {formatDollars(amountInCents)}
            </button>
        </form>
    );
}
```

**(1)** `useStripe()` and `useElements()` are null until the Stripe.js script loads. The submit
button is disabled when `!stripe` to prevent submission before the SDK is ready.

**(2)** `redirect: 'if_required'` means standard card payments complete in-place. Only payment
methods that require a bank redirect (iDEAL, Sofort) will redirect. For a domestic card
donation flow this almost never redirects.

**(3)** `<PaymentElement>` renders Stripe's hosted card form inside an iframe. Your JavaScript
never touches the card number. PCI compliance is Stripe's responsibility for this component.

---

## Configuration

Two environment variables are required:

| Variable | Where | Purpose |
|---|---|---|
| `STRIPE_SECRET_KEY` | Spring Boot (`application.properties`) | Creates PaymentIntents |
| `STRIPE_WEBHOOK_SECRET` | Spring Boot | Verifies incoming webhook signatures |
| `VITE_STRIPE_PUBLISHABLE_KEY` | Frontend (`.env`) | Initializes Stripe.js |

The secret key and webhook secret are never exposed to the frontend. The publishable key is
safe to ship in client-side code — it can only create PaymentMethods, not charge anything.

---

## What to understand

1. What is a Stripe PaymentIntent, and why does the client need the `clientSecret` to complete
   the payment? What does the server's `secretKey` do instead?
2. Why does `StripeWebhookController` return 200 even when `addDonation` throws? What are
   the consequences of returning 500 instead?
3. Stripe may deliver the same webhook event more than once ("at-least-once delivery"). How
   would you make `addDonation` idempotent so a duplicate event does not create a duplicate
   donation? (Hint: look at what the PaymentIntent ID could be used for.)
4. The `donorId` is extracted from the JWT in the controller and passed through to
   `StripeService` as PaymentIntent metadata. Why not read the `Authentication` object inside
   `StripeService` directly?
5. What is the difference between `redirect: 'if_required'` and `redirect: 'always'`? When
   would a payment using `redirect: 'if_required'` still redirect?

---

## Next

[13 — Webhook Handling: Spring Boot vs Lambda](13-lambda-webhooks.md)
