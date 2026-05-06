# Exercise 01 — Rename Customer to Attendee

**Covers:** Module 06 — Domain Modeling and Naming
**Difficulty:** Intermediate
**Estimated time:** 45–90 minutes

---

## What you are doing

Renaming `Customer` to `Attendee` across the Application module. This is a layered rename —
the type appears in the domain model, the persistence layer, the type converter, the request
DTOs, and the response DTO. You will trace it through all of them.

This exercise is deliberately not trivial. A rename that touches six files and requires updating
a serialization converter is the kind of task that shows up in real code reviews. The goal is
that you can do it without missing anything.

---

## Files you will touch

| File | What changes |
|---|---|
| `service/model/Customer.java` | Rename class to `Attendee`, rename file |
| `repositories/model/CustomerTypeConverter.java` | Rename class to `AttendeeTypeConverter`, update type parameter |
| `repositories/model/EventRecord.java` | Update field type and `@DynamoDbConvertedBy` reference |
| `controller/model/CreateEventRequest.java` | Update `List<Customer>` to `List<Attendee>` |
| `controller/model/EventUpdateRequest.java` | Same |
| `controller/model/EventResponse.java` | Same |
| `service/EventService.java` | Update all references to `Customer` |

---

## Steps

**1.** Rename `Customer.java` to `Attendee.java`. Update the class name inside the file.
Update the package declaration if your IDE does not do this automatically.

**2.** Open `CustomerTypeConverter.java`. Rename it to `AttendeeTypeConverter.java`. Update
the class name and the type parameter: `AttributeConverter<Customer>` → `AttributeConverter<Attendee>`.
Update all internal references to `Customer`.

**3.** Open `EventRecord.java`. The field `List<Customer> listOfAttending` becomes
`List<Attendee> listOfAttending`. The `@DynamoDbConvertedBy(CustomerTypeConverter.class)`
annotation becomes `@DynamoDbConvertedBy(AttendeeTypeConverter.class)`.

**4.** Update the three DTO files: `CreateEventRequest`, `EventUpdateRequest`, `EventResponse`.
Change `List<Customer>` to `List<Attendee>` and update imports.

**5.** Update `EventService`. Remove the `Customer` import, add `Attendee`. Find every place
`Customer` is instantiated or referenced.

**6.** Run `./gradlew :Application:compileJava` with Java 21 set. Fix any remaining errors.

**7.** Commit with a clear message:
```
refactor: rename Customer to Attendee across Application module
```

---

## Verify your work

- `./gradlew :Application:compileJava` passes with no errors
- `grep -r "Customer" Application/src/main/java` returns no results (excluding the converter's
  old filename in git history)
- The `EventRecord` DynamoDB annotation still points to `AttendeeTypeConverter.class`

---

## What you should be able to explain after this

- Why did the rename touch both the domain model *and* the type converter?
- What would have broken at runtime if you updated the class name but forgot to update
  `@DynamoDbConvertedBy`?
- Why does `CustomerTypeConverter` serialize to JSON? What would happen if DynamoDB received
  a `List<Attendee>` without a converter?
