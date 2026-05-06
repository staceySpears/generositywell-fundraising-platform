\# CLAUDE.md — GenerosityWell Fundraising Platform  
\#\# About This Document  
This file serves two purposes:  
1\. \*\*Instructions for AI coding assistants\*\* (Claude Code, Cursor, Copilot, etc.) — everything  
   needed to understand the project, make consistent decisions, and execute tasks correctly.  
2\. \*\*A learning guide for the developer\*\* — each phase explains not just \*what\* to build but  
   \*why\* it is built that way, what concept is being practiced, and what to understand before  
   writing the code.  
Read the "Before You Code" section of each phase before asking an AI assistant to implement it.  
The goal is that you could explain every decision in a technical interview.  
\---  
\#\# Project Overview  
\*\*GenerosityWell\*\* is a full-stack fundraising platform inspired by GiveButter. It allows  
nonprofit organizers to create fundraising campaigns and accept donations. It is built on top of  
a Kenzie Academy Java bootcamp capstone (2022) that implemented an event management system.  
The core insight driving this project: the original capstone's architecture — Spring Boot \+  
AWS Lambda \+ DynamoDB \+ Caffeine cache \+ CI/CD pipeline \+ Swagger docs \+ CloudWatch metrics —  
is production-quality and maps cleanly to a fundraising domain. Rather than rebuilding from  
scratch, we rename the domain and add the missing financial layer (Stripe).  
\*\*This is a portfolio project.\*\* The goal is not to ship to production but to demonstrate  
real-world backend engineering judgment to employers.  
\---  
\#\# Repository Structure (Existing)  
\`\`\`  
ata-capstone-project-andrisr/  
├── Application/               ← Spring Boot app (EC2, REST API layer)  
│   └── src/main/java/com/kenzie/appserver/  
│       ├── config/            ← CacheStore (Caffeine), Spring config  
│       ├── controller/        ← REST controllers \+ request/response models  
│       ├── repositories/      ← Spring Data DynamoDB repositories  
│       └── service/           ← Business logic \+ domain models  
├── ServiceLambda/             ← AWS Lambda functions (separate deploy unit)  
│   └── src/main/java/com/kenzie/capstone/service/  
│       ├── dao/               ← DynamoDB access objects  
│       ├── lambda/            ← Lambda handler classes  
│       ├── model/             ← Shared data models (records, requests, responses)  
│       └── converter/         ← Mapping between record/response types  
├── ServiceLambdaJavaClient/   ← HTTP client the Application uses to call Lambdas  
├── ServiceLambdaModel/        ← Shared model classes between Application and Lambda  
├── Frontend/                  ← Vanilla JS \+ Webpack frontend  
│   └── src/  
│       ├── api/               ← JS API client files  
│       ├── pages/             ← JS page logic  
│       └── \*.html             ← HTML pages  
├── IntegrationTests/          ← Integration test suite  
├── EventTable.yml             ← CloudFormation: DynamoDB EventTable definition  
├── UsersTable.yml             ← CloudFormation: DynamoDB UsersTable definition  
├── Application-template.yml   ← CloudFormation: Application (EC2/EB) stack  
└── LambdaService-template.yml ← CloudFormation: Lambda stack  
\`\`\`  
\---  
\#\# Technology Stack  
| Layer | Technology | Why It's Here |  
|---|---|---|  
| REST API | Spring Boot 2.6.3 | Industry-standard Java web framework |  
| Business Logic | Spring \`@Service\` classes | Separation of concerns pattern |  
| Persistence (Lambda) | AWS DynamoDB via custom DAO | NoSQL, serverless-friendly, scales automatically |  
| Persistence (App) | Spring Data DynamoDB repository | ORM-style interface over DynamoDB |  
| Caching | Caffeine (in-memory) via Spring Cache | Reduces Lambda invocation latency for reads |  
| Microservice calls | AWS Lambda (Java) | Decouples data access from app logic |  
| Metrics/Observability | Micrometer \+ CloudWatch | Production monitoring, already wired |  
| API Docs | SpringDoc OpenAPI (Swagger UI) | Self-documenting API, already wired |  
| Build | Gradle (multi-module) | Manages dependency graph across modules |  
| CI/CD | AWS CodePipeline (via createPipeline.sh) | Automated build/test/deploy |  
| Frontend | Vanilla JS \+ Webpack | Basic but functional; upgrade to React later |  
| Payments | Stripe Java SDK (TO ADD) | Industry standard for payment processing |  
\---  
\#\# Naming Conventions (Domain Rename Map)  
Every rename follows this exact mapping. When an AI assistant sees an "Event" class,  
it should rename it according to this table:  
| Original Name | New Name | Notes |  
|---|---|---|  
| \`Event\` | \`Campaign\` | A fundraising campaign |  
| \`CreateEventRequest\` | \`CreateCampaignRequest\` | |  
| \`EventResponse\` | \`CampaignResponse\` | |  
| \`EventUpdateRequest\` | \`UpdateCampaignRequest\` | |  
| \`EventRecord\` | \`CampaignRecord\` | DynamoDB record |  
| \`EventRepository\` | \`CampaignRepository\` | Spring Data repo |  
| \`EventController\` | \`CampaignController\` | REST controller |  
| \`EventService\` | \`CampaignService\` | Service layer |  
| \`EventDao\` | \`CampaignDao\` | Lambda DAO |  
| \`LambdaService\` | \`CampaignLambdaService\` | Lambda service class |  
| \`User\` (organizer) | \`Organizer\` | The nonprofit running the campaign |  
| \`Customer\` (attendee) | \`Donor\` | Someone who gives to a campaign |  
| \`UserController\` | \`OrganizerController\` | |  
| \`UserService\` | \`OrganizerService\` | |  
| \`EventTable\` | \`CampaignTable\` | DynamoDB table |  
| \`UsersTable\` | \`OrganizerTable\` | DynamoDB table |  
| \`listOfAttending\` | \`donors\` | Field renamed |  
| \`address\` | removed | Not relevant to campaigns |  
| \`date\` | \`startDate\` \+ \`endDate\` | Campaigns have a range |  
\---  
\#\# Data Models  
\#\#\# CampaignRecord (DynamoDB)  
\`\`\`java  
@DynamoDBTable(tableName \= "campaigns")  
public class CampaignRecord {  
    private String campaignId;       // PK, UUID  
    private String title;  
    private String description;  
    private Long goalAmountCents;    // Always store money in cents (Long, never double)  
    private Long currentAmountCents; // Running total, updated on each donation  
    private String startDate;        // ISO-8601 string  
    private String endDate;          // ISO-8601 string  
    private String organizerId;      // FK to organizer  
    private String status;           // ACTIVE | PAUSED | COMPLETED | CANCELLED  
    private String category;         // EDUCATION | HEALTH | COMMUNITY | ARTS | EMERGENCY  
    private String createdAt;        // ISO-8601 string  
}  
\`\`\`  
\#\#\# OrganizerRecord (DynamoDB)  
\`\`\`java  
@DynamoDBTable(tableName \= "organizers")  
public class OrganizerRecord {  
    private String organizerId;      // PK, UUID  
    private String name;  
    private String email;  
    private String organizationName;  
    private String createdAt;  
}  
\`\`\`  
\#\#\# DonationRecord (DynamoDB) — NEW TABLE  
\`\`\`java  
@DynamoDBTable(tableName \= "donations")  
public class DonationRecord {  
    private String donationId;       // PK, UUID  
    private String campaignId;       // used for GSI query  
    private String donorName;  
    private String donorEmail;  
    private Long amountCents;        // Always Long, never double or float  
    private String message;          // Optional  
    private Boolean anonymous;  
    private String donatedAt;        // ISO-8601 string  
    private String status;           // PENDING | COMPLETED | REFUNDED  
    private String stripePaymentIntentId; // for reconciliation  
}  
\`\`\`  
\*\*Why cents?\*\* Never store currency as a floating-point number. \`0.1 \+ 0.2 \= 0.30000000000000004\`  
in floating-point math. All monetary values are stored as \`Long\` (cents). $12.50 \= 1250L.  
Division and display formatting happen only in the presentation layer.  
\---  
\#\# Phase 0: Modernize the Stack (DO THIS FIRST)
\#\#\# What You're Learning
\- How to migrate a Spring Boot 2 project to Spring Boot 3
\- Why \`javax.\*\` imports became \`jakarta.\*\` (the Jakarta EE namespace move)
\- How to read a migration guide and apply it systematically
\#\#\# Why This Comes First
The capstone was built on Spring Boot 2.x / Java 11 (circa 2022). Before renaming any
domain classes, get the project compiling on modern versions. Doing it after the rename
creates more churn. This is also a genuine portfolio talking point: you can explain
what changed between Spring Boot 2 and 3 and why.
\#\#\# Key Breaking Change: javax → jakarta
Spring Boot 3 moved from Java EE (\`javax.\*\`) to Jakarta EE (\`jakarta.\*\`). Every import
that says \`javax.persistence\`, \`javax.validation\`, or \`javax.servlet\` must change to
\`jakarta.persistence\`, \`jakarta.validation\`, \`jakarta.servlet\`. This is a mechanical
find-and-replace but it will cause compile errors if missed.
\#\#\# Tasks for AI Assistant
1\. In \`Application/build.gradle\` (and any submodule \`build.gradle\` files):
   \- Change \`id 'org.springframework.boot' version '2.x.x'\` → \`'3.2.x'\` (latest 3.x)
   \- Change \`sourceCompatibility = '11'\` → \`sourceCompatibility = '21'\`
   \- Update \`io.spring.dependency-management\` plugin to \`1.1.x\`
   \- Remove explicit \`javax.\*\` dependency versions (Spring Boot BOM manages them now)
2\. Update the Gradle wrapper to 8.x: \`./gradlew wrapper --gradle-version 8.7\`
3\. Find and replace all \`javax.\` imports with \`jakarta.\` across all Java source files.
4\. Update AWS SDK dependencies if on v1 patterns — confirm the DynamoDB mapper annotations
   (\`@DynamoDBTable\`, \`@DynamoDBHashKey\`) still work with the updated Spring Data DynamoDB
   dependency, or migrate to AWS SDK v2 + \`software.amazon.awssdk:dynamodb-enhanced\`.
5\. Update \`springfox-swagger\` → \`springdoc-openapi\` if not already done (Springfox is
   incompatible with Spring Boot 3).
6\. Run \`./gradlew build\` and fix all compilation errors before proceeding to Phase 1.
\#\#\# After This Phase
The project compiles on Java 21 / Spring Boot 3. All existing tests still pass. No
domain renaming has happened yet.
\---
\#\# Phase 1: Domain Rename
\#\#\# What You're Learning  
\- How to safely refactor a working codebase without breaking it  
\- The difference between a domain model, a persistence model, and a transport model  
\- Why Java has separate "record", "model", and "response" classes for the same concept  
\#\#\# Why Three Model Types Exist  
The original code has three representations of an Event:  
1\. \`EventRecord\` — what gets stored in DynamoDB (persistence model)  
2\. \`Event\` — the domain object used in service layer logic (domain model)  
3\. \`EventResponse\` — what gets sent back to the API caller (transport/DTO model)  
This separation exists so that: database schema changes don't break your API contract,  
internal business logic isn't tied to what the database looks like, and you control  
exactly what data is exposed to external callers. This pattern is called \*\*DTO  
(Data Transfer Object)\*\* pattern and is standard in enterprise Java.  
\#\#\# Tasks for AI Assistant  
1\. In \`Application/src/main/java/com/kenzie/appserver/\`:  
   \- Rename \`controller/EventController.java\` → \`CampaignController.java\`  
   \- Update all mappings to use \`/campaigns\` instead of \`/events\`  
   \- Rename \`controller/model/CreateEventRequest.java\` → \`CreateCampaignRequest.java\`  
   \- Rename \`controller/model/EventResponse.java\` → \`CampaignResponse.java\`  
   \- Rename \`controller/model/EventUpdateRequest.java\` → \`UpdateCampaignRequest.java\`  
   \- Rename \`service/EventService.java\` → \`CampaignService.java\`  
   \- Rename \`service/model/Event.java\` → \`Campaign.java\`  
   \- Rename \`repositories/model/EventRecord.java\` → \`CampaignRecord.java\`  
   \- Rename \`repositories/EventRepository.java\` → \`CampaignRepository.java\`  
   \- Update all field names per the naming convention table above  
   \- Fix the bug in \`CampaignService.addNewCampaign()\`: replace \`lambdaResponse.toString() \== ""\`  
     with \`lambdaResponse.toString().isEmpty()\` (using \`==\` to compare Strings is a Java bug)  
2\. In \`ServiceLambda/src/main/java/com/kenzie/capstone/service/\`:  
   \- Rename \`LambdaService.java\` → \`CampaignLambdaService.java\`  
   \- Rename \`dao/EventDao.java\` → \`CampaignDao.java\`  
   \- Rename all model classes per the naming convention table  
3\. In \`ServiceLambdaModel/\`:  
   \- Rename all shared model classes per the naming convention table  
   \- Replace \`address\` field with nothing; replace \`date\` with \`startDate\` and \`endDate\`  
   \- Replace \`listOfAttending: List\<Customer\>\` with \`donors: List\<Donor\>\`  
   \- \*\*Add \`goalAmountCents: Long\` and \`currentAmountCents: Long\`\*\*  
4\. Update \`EventTable.yml\` → \`CampaignTable.yml\`:  
   \- Rename the CloudFormation stack and DynamoDB table to \`campaigns\`  
   \- Add attributes: \`goalAmountCents\` (N), \`currentAmountCents\` (N), \`status\` (S),  
     \`category\` (S), \`startDate\` (S), \`endDate\` (S), \`organizerId\` (S)  
5\. Update all import statements, \`@DynamoDBTable\` annotations, and \`@RequestMapping\` paths.  
6\. Verify the project compiles: \`./gradlew build\`  
\#\#\# After This Phase  
The application should compile and all existing tests should still pass. The API now  
responds at \`/campaigns\` instead of \`/events\`. No new functionality yet.  
\---  
\#\# Phase 2: Add the Donation Domain  
\#\#\# What You're Learning  
\- How to add a new domain entity end-to-end (from DynamoDB table to REST endpoint)  
\- DynamoDB Global Secondary Indexes (GSI) — how to query by a non-primary-key attribute  
\- The service layer ownership check pattern (only authorized users can mutate data)  
\#\#\# Key Concept: DynamoDB GSIs  
DynamoDB's primary key uniquely identifies one item. But what if you need to query all  
donations \*for a given campaign\*? DonationRecord's PK is \`donationId\`, so you can't  
query by \`campaignId\` directly. A \*\*Global Secondary Index (GSI)\*\* creates an alternate  
index on a different attribute, enabling efficient queries on that attribute. Think of it  
like adding a database index to a non-primary-key column in SQL.  
\#\#\# Tasks for AI Assistant  
1\. Create \`DonationTable.yml\` in the project root:  
\`\`\`yaml  
AWSTemplateFormatVersion: '2010-09-09'  
Resources:  
  DonationsTable:  
    Type: AWS::DynamoDB::Table  
    Properties:  
      TableName: donations  
      AttributeDefinitions:  
        \- AttributeName: donationId  
          AttributeType: S  
        \- AttributeName: campaignId  
          AttributeType: S  
      KeySchema:  
        \- AttributeName: donationId  
          KeyType: HASH  
      GlobalSecondaryIndexes:  
        \- IndexName: campaignId-index  
          KeySchema:  
            \- AttributeName: campaignId  
              KeyType: HASH  
          Projection:  
            ProjectionType: ALL  
          ProvisionedThroughput:  
            ReadCapacityUnits: 5  
            WriteCapacityUnits: 5  
      ProvisionedThroughput:  
        ReadCapacityUnits: 5  
        WriteCapacityUnits: 5  
\`\`\`  
2\. Create \`DonationRecord.java\` in \`Application/src/main/java/com/kenzie/appserver/repositories/model/\`  
   with all fields listed in the Data Models section above.  
3\. Create \`DonationRepository.java\` in \`Application/src/main/java/com/kenzie/appserver/repositories/\`:  
\`\`\`java  
@EnableScan  
public interface DonationRepository extends CrudRepository\<DonationRecord, String\> {  
    List\<DonationRecord\> findByCampaignId(String campaignId);  
}  
\`\`\`  
4\. Create request/response models in \`Application/.../controller/model/\`:  
   \- \`CreateDonationRequest.java\`: fields \`campaignId\`, \`donorName\`, \`donorEmail\`,  
     \`amountCents\` (Long), \`message\` (String, nullable), \`anonymous\` (boolean)  
   \- \`DonationResponse.java\`: all DonationRecord fields except \`stripePaymentIntentId\`  
     (never expose internal payment IDs to API callers)  
5\. Create \`DonationService.java\` in \`Application/.../service/\`:  
   \- \`createDonation(CreateDonationRequest)\` — validates campaign is ACTIVE and not past  
     end date, creates DonationRecord with status PENDING, returns DonationResponse  
   \- \`getDonationsByCampaign(String campaignId)\` — returns list of DonationResponse  
   \- \`getDonationTotal(String campaignId)\` — sums \`amountCents\` for all COMPLETED donations  
   \- \`confirmDonation(String donationId)\` — sets status to COMPLETED, increments  
     \`currentAmountCents\` on the CampaignRecord (call CampaignRepository.save)  
6\. Create \`DonationController.java\` in \`Application/.../controller/\`:  
   \- \`POST /donations\` → \`createDonation()\`  
   \- \`GET /donations/{donationId}\` → \`getDonationById()\`  
   \- \`GET /campaigns/{campaignId}/donations\` → \`getDonationsByCampaign()\`  
   \- \`GET /campaigns/{campaignId}/total\` → \`getDonationTotal()\`  
\#\#\# After This Phase  
You can create donations, query them by campaign, and see running totals. No real payment  
processing yet — donations are created directly. Write a unit test for  
\`DonationService.createDonation()\` that verifies a donation against a PAUSED campaign  
throws a \`ResponseStatusException\`.  
\---  
\#\# Phase 3: Stripe Integration  
\#\#\# What You're Learning  
\- How payment processing works at a high level (the PaymentIntent flow)  
\- Webhook security (verifying Stripe's signature so attackers can't fake payment confirmations)  
\- The principle of never trusting client-side payment confirmation  
\- Idempotency in financial systems  
\#\#\# Key Concept: The PaymentIntent Flow  
Never let the frontend decide whether a payment succeeded. The correct flow is:  
\`\`\`  
1\. Frontend calls your server: POST /payments/create-intent {campaignId, amountCents}  
2\. Your server calls Stripe API → gets back a PaymentIntent with a clientSecret  
3\. Your server creates a DonationRecord with status=PENDING, stores the paymentIntentId  
4\. Server returns clientSecret to the frontend (NOT the full PaymentIntent)  
5\. Frontend uses Stripe.js \+ clientSecret to render card input and submit payment  
6\. Stripe processes payment → calls your webhook: POST /webhooks/stripe  
7\. Your webhook handler verifies Stripe's signature, then calls confirmDonation()  
8\. confirmDonation() sets status=COMPLETED and updates campaign total  
\`\`\`  
The frontend \*\*never\*\* tells you a payment succeeded. Only Stripe's signed webhook does.  
This prevents a malicious user from faking a payment confirmation by calling your API directly.  
\#\#\# Key Concept: Webhook Signature Verification  
Stripe signs every webhook request with your webhook secret. Before processing any webhook  
event, you must verify this signature using \`Webhook.constructEvent()\`. If verification  
fails, return HTTP 400 and do nothing. If you skip this step, anyone can POST to your  
webhook endpoint and fake payment confirmations.  
\#\#\# Key Concept: Idempotency  
What if Stripe sends the same webhook twice (it does this for reliability)? Your  
\`confirmDonation()\` must be idempotent: if a donation is already COMPLETED, calling  
\`confirmDonation()\` again should do nothing, not create a duplicate. Always check  
the current state before mutating it.  
\#\#\# Tasks for AI Assistant  
1\. Add the Stripe dependency to \`Application/build.gradle\`:  
\`\`\`gradle  
implementation 'com.stripe:stripe-java:24.3.0'  
\`\`\`  
2\. Add to \`Application/src/main/resources/application.properties\`:  
\`\`\`properties  
stripe.api.key=${STRIPE\_SECRET\_KEY}  
stripe.webhook.secret=${STRIPE\_WEBHOOK\_SECRET}  
\`\`\`  
These values come from environment variables — never hardcode them.  
3\. Create \`StripeConfig.java\` in \`Application/.../config/\`:  
\`\`\`java  
@Configuration  
public class StripeConfig {  
    @Value("${stripe.api.key}")  
    private String apiKey;  
    @PostConstruct  
    public void init() {  
        Stripe.apiKey \= apiKey;  
    }  
}  
\`\`\`  
4\. Create \`PaymentService.java\` in \`Application/.../service/\`:  
   \- \`createPaymentIntent(String campaignId, Long amountCents, String currency)\`:  
     \- Validates the campaign exists and is ACTIVE  
     \- Calls \`PaymentIntent.create()\` with amount, currency, and metadata  
       \`{campaignId: campaignId}\`  
     \- Creates a DonationRecord with status=PENDING and the paymentIntentId  
     \- Returns a \`PaymentIntentResponse\` containing only \`clientSecret\` and \`donationId\`  
   \- Note: Store \`campaignId\` in the PaymentIntent metadata so the webhook can  
     find the donation record without a database query  
5\. Create \`WebhookController.java\` in \`Application/.../controller/\`:  
\`\`\`java  
@RestController  
public class WebhookController {  
    @Value("${stripe.webhook.secret}")  
    private String webhookSecret;  
    @PostMapping("/webhooks/stripe")  
    public ResponseEntity\<String\> handleStripeWebhook(  
            @RequestBody String payload,  
            @RequestHeader("Stripe-Signature") String sigHeader) {  
        Event event;  
        try {  
            event \= Webhook.constructEvent(payload, sigHeader, webhookSecret);  
        } catch (SignatureVerificationException e) {  
            return ResponseEntity.badRequest().body("Invalid signature");  
        }  
        if ("payment\_intent.succeeded".equals(event.getType())) {  
            PaymentIntent intent \= (PaymentIntent) event.getDataObjectDeserializer()  
                    .getObject().orElseThrow();  
            String paymentIntentId \= intent.getId();  
            donationService.confirmDonationByPaymentIntentId(paymentIntentId);  
        }  
        return ResponseEntity.ok("Received");  
    }  
}  
\`\`\`  
6\. Add \`confirmDonationByPaymentIntentId(String paymentIntentId)\` to \`DonationService\`:  
   \- Finds the DonationRecord by \`stripePaymentIntentId\`  
   \- If status is already COMPLETED, return immediately (idempotency)  
   \- Otherwise: set status=COMPLETED, increment \`currentAmountCents\` on the campaign  
7\. Create \`PaymentController.java\`:  
   \- \`POST /payments/create-intent\` → calls \`PaymentService.createPaymentIntent()\`  
   \- Returns only \`{clientSecret, donationId}\` — never return the full PaymentIntent  
\#\#\# After This Phase  
End-to-end payment flow works. Test it using Stripe's test mode card number  
\`4242 4242 4242 4242\`. Use the Stripe CLI (\`stripe listen \--forward-to localhost:8080/webhooks/stripe\`)  
to forward webhook events to your local server during development.  
\---  
\#\# Phase 4: Frontend Updates  
\#\#\# What You're Learning  
\- How to integrate Stripe.js (Stripe's browser-side library) safely  
\- The separation between server-side payment creation and client-side card handling  
\- Why Stripe's hosted card element exists (PCI compliance — card data never touches your server)  
\#\#\# Key Concept: PCI Compliance  
PCI DSS (Payment Card Industry Data Security Standard) governs how card data can be handled.  
If card numbers ever pass through your server, you must undergo extensive security audits.  
Stripe's solution: Stripe.js renders an iframe for card input directly on your page, and  
card data goes from the browser directly to Stripe's servers — your server only ever sees  
a \`clientSecret\` and a \`paymentIntentId\`, never a card number.  
\#\#\# Tasks for AI Assistant  
1\. Update \`campaign\_detail.html\` (create this page):  
   \- Shows campaign title, description, goal amount, current amount, progress bar  
   \- Progress bar: \`(currentAmountCents / goalAmountCents) \* 100\`%  
   \- Donor list (hide name if \`anonymous: true\`, show "Anonymous" instead)  
   \- Donation form: amount input, name, email, message fields  
   \- Stripe card element container: \`\<div id="card-element"\>\</div\>\`  
   \- Submit button  
2\. Create \`Frontend/src/pages/campaignDetailPage.js\`:  
\`\`\`javascript  
import { loadStripe } from '@stripe/stripe-js';  
import { createPaymentIntent } from '../api/paymentClient';  
const stripePromise \= loadStripe('pk\_test\_YOUR\_PUBLISHABLE\_KEY');  
async function submitDonation(campaignId, formData) {  
    // 1\. Call your server to create a PaymentIntent  
    const { clientSecret, donationId } \= await createPaymentIntent({  
        campaignId,  
        amountCents: formData.amountCents,  
    });  
    // 2\. Use Stripe.js to confirm the payment  
    const stripe \= await stripePromise;  
    const { error } \= await stripe.confirmCardPayment(clientSecret, {  
        payment\_method: {  
            card: cardElement, // the Stripe card element  
            billing\_details: { name: formData.donorName, email: formData.donorEmail },  
        },  
    });  
    if (error) {  
        showError(error.message);  
    } else {  
        showSuccess('Donation complete\! Thank you.');  
        refreshCampaignTotal(campaignId);  
    }  
}  
\`\`\`  
3\. Create \`Frontend/src/api/paymentClient.js\`:  
   \- \`createPaymentIntent(data)\` → POST \`/payments/create-intent\`  
   \- Returns \`{ clientSecret, donationId }\`  
4\. Create \`Frontend/src/api/campaignClient.js\` (replacing \`eventClient.js\`):  
   \- \`getCampaignById(id)\` → GET \`/campaigns/{id}\`  
   \- \`getAllCampaigns()\` → GET \`/campaigns/all\`  
   \- \`createCampaign(data)\` → POST \`/campaigns\`  
   \- \`getDonationTotal(campaignId)\` → GET \`/campaigns/{campaignId}/total\`  
   \- \`getDonationsByCampaign(campaignId)\` → GET \`/campaigns/{campaignId}/donations\`  
5\. Update \`home\_page.html\` to show a grid of active campaigns with progress bars.  
6\. Update \`create\_account\_page.html\` to be the organizer registration form  
   (maps to \`POST /organizers\`).  
\---  
\#\# Phase 5: Code Quality & Tests  
\#\#\# What You're Learning  
\- How to write meaningful unit tests with Mockito  
\- The difference between unit tests, integration tests, and end-to-end tests  
\- What test coverage actually tells you (and what it doesn't)  
\#\#\# Key Concept: What Makes a Good Unit Test  
A unit test verifies one behavior of one class in isolation. It should:  
\- Be fast (no network calls, no database)  
\- Test behavior, not implementation (test what the method returns, not how it does it)  
\- Follow the Arrange-Act-Assert pattern  
\- Cover both the happy path and at least one failure path  
Bad test: \`assertTrue(true)\` (this is what the original capstone had — it always passes)  
Good test: Verify that \`CampaignService.createCampaign()\` throws an exception when  
the organizer ID doesn't exist in the repository.  
\#\#\# Priority Tests to Write  
1\. \`CampaignServiceTest.java\`:  
   \- \`createCampaign\_validRequest\_returnsCampaignResponse()\`  
   \- \`getCampaignById\_campaignDoesNotExist\_returnsNull()\`  
   \- \`updateCampaign\_wrongOrganizer\_throwsResponseStatusException()\`  
   \- \`deleteCampaign\_emptyId\_throwsResponseStatusException()\`  
2\. \`DonationServiceTest.java\`:  
   \- \`createDonation\_campaignIsActive\_returnsDonationResponse()\`  
   \- \`createDonation\_campaignIsPaused\_throwsResponseStatusException()\`  
   \- \`createDonation\_campaignPastEndDate\_throwsResponseStatusException()\`  
   \- \`confirmDonation\_alreadyCompleted\_doesNotIncrementTotal()\` ← idempotency test  
   \- \`getDonationTotal\_noCompletedDonations\_returnsZero()\`  
3\. \`PaymentServiceTest.java\`:  
   \- \`createPaymentIntent\_campaignNotActive\_throwsException()\`  
   \- \`createPaymentIntent\_validCampaign\_returnsClientSecret()\`  
\#\#\# Mockito Pattern (use this template)  
\`\`\`java  
@ExtendWith(MockitoExtension.class)  
class DonationServiceTest {  
    @Mock  
    private DonationRepository donationRepository;  
    @Mock  
    private CampaignRepository campaignRepository;  
    @InjectMocks  
    private DonationService donationService;  
    @Test  
    void createDonation\_campaignIsPaused\_throwsException() {  
        // Arrange  
        CampaignRecord pausedCampaign \= new CampaignRecord();  
        pausedCampaign.setStatus("PAUSED");  
        when(campaignRepository.findById("campaign-1"))  
            .thenReturn(Optional.of(pausedCampaign));  
        CreateDonationRequest request \= new CreateDonationRequest();  
        request.setCampaignId("campaign-1");  
        request.setAmountCents(1000L);  
        // Act \+ Assert  
        assertThrows(ResponseStatusException.class,  
            () \-\> donationService.createDonation(request));  
    }  
}  
\`\`\`  
\---  
\#\# Phase 6: Final Polish  
\#\#\# Tasks for AI Assistant  
1\. \*\*Fix the Gradle module names\*\* — rename all \`com.kenzie\` package references to  
   \`com.generositywell\` throughout the project to match the new product name.  
2\. \*\*Add \`@Valid\` annotations\*\* to all controller request body parameters and add  
   \`@NotNull\`, \`@NotBlank\`, \`@Positive\` constraints to request model fields.  
   Add \`spring-boot-starter-validation\` to \`Application/build.gradle\`.  
3\. \*\*Consolidate converter methods\*\* — create a single \`CampaignConverter.java\` utility  
   class in the Application service package with static methods:  
   \`fromRecordToResponse()\`, \`fromRequestToRecord()\`, \`fromDomainToResponse()\`.  
   Remove the duplicate mapping code that currently appears in \`CampaignService\`.  
4\. \*\*Fix \`getAllCampaigns()\`\*\* — the original \`getAllEvents()\` calls the Lambda AND  
   the local repository but only uses the local result. Remove the Lambda call from  
   this method; it serves no purpose here. The authoritative source for \`getAll\`  
   queries is the Spring Data DynamoDB repository scan.  
5\. \*\*Update the README.md\*\* with:  
   \- Architecture diagram (ASCII is fine)  
   \- Local setup instructions (AWS credentials, DynamoDB local, Stripe test keys)  
   \- How to run the app locally  
   \- How to run tests  
   \- A note that this is a portfolio project and uses Stripe test mode  
\---  
\#\# Known Issues to Fix (from Original Codebase)  
| File | Issue | Fix |  
|---|---|---|  
| \`EventService.java:108\` | \`lambdaResponse.toString() \== ""\` — String \== comparison bug | Use \`.isEmpty()\` |  
| \`EventService.java:117\` | \`addNewEventLocally()\` has commented-out user validation | Restore the organizer existence check in \`addNewCampaignLocally()\` |  
| \`EventService.java:151\` | \`getAllEvents()\` calls Lambda then ignores the result | Remove the Lambda call |  
| \`DonationController\` (original) | Not annotated as \`@RestController\` | Add annotation |  
| All test files | Tests contain only \`assertTrue(true)\` | Replace with real assertions |  
\---  
\#\# Environment Variables Required  
\`\`\`bash  
\# AWS  
AWS\_ACCESS\_KEY\_ID=...  
AWS\_SECRET\_ACCESS\_KEY=...  
AWS\_REGION=us-east-1  
\# Stripe (use test keys for development)  
STRIPE\_SECRET\_KEY=sk\_test\_...  
STRIPE\_WEBHOOK\_SECRET=whsec\_...  
STRIPE\_PUBLISHABLE\_KEY=pk\_test\_...  
\# DynamoDB (local dev only)  
DYNAMODB\_ENDPOINT=http://localhost:8000  
\`\`\`  
For local development, store these in a \`.env\` file (add \`.env\` to \`.gitignore\` — never commit secrets).  
\---  
\#\# Build & Run Commands  
\`\`\`bash  
\# Build everything  
./gradlew build  
\# Run locally (uses 'local' Spring profile)  
./gradlew bootRunDev  
\# Run tests  
./gradlew test  
\# Start local DynamoDB  
./local-dynamodb.sh  
\# Deploy CI/CD pipeline (AWS credentials must be set)  
./createPipeline.sh  
\# Tear down  
./cleanupPipeline.sh  
\`\`\`  
\---  
\#\# Coding Standards  
\- \*\*Never store monetary values as \`double\` or \`float\`\*\* — always use \`Long\` (cents)  
\- \*\*Never compare Strings with \`==\`\*\* — always use \`.equals()\` or \`.isEmpty()\`  
\- \*\*Never hardcode secrets\*\* — use \`@Value("${property.name}")\` \+ environment variables  
\- \*\*Always check for null\*\* before accessing fields on objects returned from repositories  
\- \*\*Service methods that mutate data\*\* must validate preconditions and throw  
  \`ResponseStatusException\` with a meaningful HTTP status and message  
\- \*\*Controllers\*\* should contain no business logic — only call the service and return  
  a \`ResponseEntity\`  
\- \*\*Converters\*\* belong in a dedicated converter/mapper class, not scattered in the service  
\- Monetary display formatting (e.g., \`1250L\` → \`"$12.50"\`) belongs in the frontend or  
  a dedicated formatter, not in the domain model  
\---  
\#\# Suggested Implementation Order  
Follow this order to keep the app in a runnable state at every step:
1\. Phase 0: Spring Boot 3 / Java 21 upgrade (compiles on modern stack before any renaming)
2\. Phase 1: Domain rename (app compiles and existing tests pass throughout)
3\. Phase 2: Donation domain (new table, service, controller — no Stripe yet)
4\. Phase 5 (partial): Write tests for Phases 1 and 2 before continuing
5\. Phase 3: Stripe integration
6\. Phase 4: Frontend updates
7\. Phase 5 (remainder): Tests for Stripe
8\. Phase 6: Polish and cleanup  
\---  
\#\# Interview Talking Points  
When you have built this project, you should be able to explain all of the following:  
\- Why the project uses three model types (Record, Domain, Response/DTO) for the same entity  
\- What a DynamoDB GSI is and why the donations table needs one  
\- Why monetary values are stored as Long (cents) instead of Double  
\- What the Stripe PaymentIntent flow is and why the webhook — not the frontend — confirms payment  
\- What webhook signature verification is and why skipping it is a security vulnerability  
\- What idempotency means and how \`confirmDonation()\` implements it  
\- Why there's a separate Lambda module instead of querying DynamoDB directly from the service  
\- What the Caffeine cache is doing and what happens when a cached campaign gets a new donation  
  (the cache must be evicted on write — see \`cache.evict(id)\` in the service)  
\- What \`@Valid\` and \`@NotNull\` annotations do at the HTTP boundary  
\- Why \`CompareStringsWithEquals\` is a bug in the original code  
