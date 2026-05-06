# 03 — AWS SDK v2 Migration: DynamoDbEnhancedClient and AttributeConverter

## Why this exists

AWS SDK v1 for Java (`com.amazonaws:aws-java-sdk-*`) was placed in maintenance mode in 2023
and will reach end-of-life in 2025. The capstone was built entirely on v1. Any new AWS feature,
security patch, or performance improvement will only appear in v2.

SDK v2 (`software.amazon.awssdk:*`) is not a drop-in replacement. The package names changed,
the client construction model changed, and the DynamoDB mapper was replaced with a new
`DynamoDbEnhancedClient` that uses a different annotation set. You cannot mix v1 and v2 code
in the same module — they conflict at the class level.

The migration is worth understanding in depth because DynamoDB and its SDK are common interview
topics for any backend role working with AWS.

---

## The v1 vs v2 mental model

**v1 — `DynamoDBMapper`**

The v1 mapper worked like a basic ORM. You annotated your class, handed it to the mapper,
and it handled serialization. Simple, but limited — it had no async support, no built-in
request/response interceptors, and its type handling was brittle for complex nested objects.

```java
// v1 — how the capstone originally wrote DynamoDB access
@DynamoDBTable(tableName = "Events")
public class EventRecord {
    @DynamoDBHashKey
    private String id;
    // ...
}

DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDB);
EventRecord record = mapper.load(EventRecord.class, id);
```

**v2 — `DynamoDbEnhancedClient`**

The v2 Enhanced Client uses a `TableSchema` to describe the mapping, and you interact with
a typed `DynamoDbTable<T>`. The annotation set changed entirely.

```java
// v2 — how it works now
@DynamoDbBean
public class EventRecord {
    @DynamoDbPartitionKey
    public String getId() { return id; }
    // ...
}

DynamoDbTable<EventRecord> table = enhancedClient.table("Events", TableSchema.fromBean(EventRecord.class));
EventRecord record = table.getItem(Key.builder().partitionValue(id).build());
```

---

## The code

### `Application/src/main/java/.../config/DynamoDbConfig.java`

This file creates the two beans that the rest of the application uses:

```java
@Bean
@Primary
@ConditionalOnProperty(name = "dynamodb.override_endpoint", havingValue = "true")
public DynamoDbClient localDynamoDbClient(@Value("${dynamodb.endpoint}") String dynamoEndpoint) {
    return DynamoDbClient.builder()
            .endpointOverride(URI.create(dynamoEndpoint))   // (1)
            .region(Region.US_EAST_1)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
}

@Bean
public DynamoDbClient dynamoDbClient() {
    return DynamoDbClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())  // (2)
            .build();
}

@Bean
public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
    return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)  // (3)
            .build();
}
```

**(1)** `endpointOverride` — points the client at a local DynamoDB Docker container during
development instead of AWS. The `@ConditionalOnProperty` means this bean only activates when
`dynamodb.override_endpoint=true` is set in the active profile. The production bean below it
is the fallback.

**(2)** `DefaultCredentialsProvider` — looks for AWS credentials in the standard chain: environment
variables → `~/.aws/credentials` → IAM instance role. You never hardcode credentials.

**(3)** `DynamoDbEnhancedClient` wraps `DynamoDbClient`. Your DAOs only take the Enhanced Client
as a constructor argument — they never interact with the raw `DynamoDbClient` directly.

---

### `EventDao` — using the Enhanced Client

```java
@Repository
public class EventDao {

    private static final String TABLE_NAME = "Events";
    private final DynamoDbTable<EventRecord> eventTable;

    public EventDao(DynamoDbEnhancedClient enhancedClient) {
        this.eventTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(EventRecord.class));
    }

    public Optional<EventRecord> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Optional.ofNullable(eventTable.getItem(key));   // (1)
    }

    public List<EventRecord> findAll() {
        return eventTable.scan().items().stream().toList();    // (2)
    }
}
```

**(1)** `getItem` is a direct key lookup — O(1), uses the partition key. This is different from
`query`, which returns multiple items matching a key condition. For a table with only a partition
key (no sort key), `getItem` is always what you want for single-item retrieval.

**(2)** `scan` reads every item in the table. It is expensive at scale and should be replaced
with a `query` on a GSI (Global Secondary Index) once the data model matures. For now, it is
correct for `getAllEvents`.

---

### `UserTypeConverter` and `CustomerTypeConverter` — the `AttributeConverter<T>` pattern

DynamoDB stores data as primitive attribute types: `S` (string), `N` (number), `BOOL`, `L` (list),
`M` (map). When your record has a complex nested object — like a `User` or `List<Customer>` — you
need to tell DynamoDB how to convert it.

In v1, this was done with `DynamoDBTypeConverter`. In v2, it is `AttributeConverter<T>`.

```java
public class UserTypeConverter implements AttributeConverter<User> {

    private static final Gson GSON = new Gson();

    @Override
    public AttributeValue transformFrom(User user) {           // (1)
        return AttributeValue.builder()
                .s(GSON.toJson(user))
                .build();
    }

    @Override
    public AttributeValue transformTo(AttributeValue value) {  // (2)
        return GSON.fromJson(value.s(), User.class);
    }

    @Override
    public EnhancedType<User> type() {
        return EnhancedType.of(User.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;   // (3)
    }
}
```

**(1)** `transformFrom` — converts your Java object to a DynamoDB `AttributeValue`. We serialize
the `User` to JSON and store it as a string (`S` type). Simple, readable in the DynamoDB console.

**(2)** `transformTo` — the reverse: deserialize the JSON string back to a `User`.

**(3)** `AttributeValueType.S` — tells the Enhanced Client this converter produces a string
attribute. This must match what `transformFrom` actually produces.

The converter is wired to the record via `@DynamoDbConvertedBy`:

```java
@DynamoDbConvertedBy(UserTypeConverter.class)
public User getUser() { return user; }
```

---

## What to understand

1. What is the difference between `DynamoDbClient` and `DynamoDbEnhancedClient`, and which one
   does your DAO interact with directly?
2. Why does `getItem` use a `Key.builder()` instead of passing the ID string directly?
3. `scan()` reads the entire table. When would this become a problem in production, and what
   is the AWS-native alternative?
4. Why do we serialize `User` and `List<Customer>` to JSON strings rather than using DynamoDB's
   native `M` (map) or `L` (list) types?

---

## Next

[04 — Removing the Lambda Proxy](04-removing-lambda-proxy.md)
