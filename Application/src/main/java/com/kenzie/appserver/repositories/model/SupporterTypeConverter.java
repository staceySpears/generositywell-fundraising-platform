package com.kenzie.appserver.repositories.model;

import com.kenzie.appserver.service.model.Supporter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DynamoDB AttributeConverter for {@code List<Supporter>}.
 *
 * <p><b>Wire format (v2):</b> each supporter is serialised as five pipe-delimited,
 * URL-encoded fields:
 * <pre>{@code URLEncode(id)|URLEncode(name)|URLEncode(email)|amountInCents|donationDate}</pre>
 * {@code amountInCents} and {@code donationDate} are the literal string {@code "null"}
 * when not present, so the deserialiser can distinguish them from empty strings.
 *
 * <p><b>Backward compatibility (v1):</b> records written before donation tracking was added
 * use the legacy format {@code "id x name x email"} (space–x–space delimiter, no URL
 * encoding).  The deserialiser detects v1 records by the absence of a pipe character and
 * reads them without URL-decoding, leaving the new fields as {@code null}.
 */
public class SupporterTypeConverter implements AttributeConverter<List<Supporter>> {

    private static final String DELIMITER = "|";
    private static final String NULL_SENTINEL = "null";

    /**
     * Converts a list of supporters to a DynamoDB AttributeValue (List type).
     *
     * @param input the list of supporters, or {@code null}
     * @return a DynamoDB AttributeValue containing the serialised supporters
     */
    @Override
    public AttributeValue transformFrom(List<Supporter> input) {
        if (input == null) {
            return AttributeValue.builder().nul(true).build();
        }
        List<AttributeValue> items = input.stream()
                .map(s -> {
                    String serialized = encode(s.getId()) + DELIMITER
                            + encode(s.getName()) + DELIMITER
                            + encode(s.getEmail()) + DELIMITER
                            + (s.getAmountInCents() != null ? s.getAmountInCents() : NULL_SENTINEL) + DELIMITER
                            + (s.getDonationDate() != null ? s.getDonationDate() : NULL_SENTINEL);
                    return AttributeValue.builder().s(serialized).build();
                })
                .collect(Collectors.toList());
        return AttributeValue.builder().l(items).build();
    }

    /**
     * Converts a DynamoDB AttributeValue (List type) back to a list of supporters.
     * Handles both v1 ({@code "id x name x email"}) and v2 (pipe-delimited) records.
     *
     * @param input the DynamoDB AttributeValue
     * @return the deserialised list of supporters
     */
    @Override
    public List<Supporter> transformTo(AttributeValue input) {
        if (input == null || Boolean.TRUE.equals(input.nul()) || input.l() == null) {
            return new java.util.ArrayList<>();
        }
        return input.l().stream().map(av -> {
            String raw = av.s();
            Supporter supporter = new Supporter();

            if (raw.contains(DELIMITER)) {
                // v2 format: id|name|email|amountInCents|donationDate
                String[] parts = raw.split("\\|", -1);
                supporter.setId(decode(parts[0]));
                supporter.setName(parts.length > 1 ? decode(parts[1]) : "");
                supporter.setEmail(parts.length > 2 ? decode(parts[2]) : "");
                if (parts.length > 3 && !NULL_SENTINEL.equals(parts[3])) {
                    try {
                        supporter.setAmountInCents(Long.parseLong(parts[3]));
                    } catch (NumberFormatException ignored) { /* leave null */ }
                }
                if (parts.length > 4 && !NULL_SENTINEL.equals(parts[4])) {
                    supporter.setDonationDate(parts[4]);
                }
            } else {
                // v1 legacy format: "id x name x email" (no URL encoding)
                String[] parts = raw.split(" x ");
                supporter.setId(parts.length > 0 ? parts[0].trim() : "");
                supporter.setName(parts.length > 1 ? parts[1].trim() : "");
                supporter.setEmail(parts.length > 2 ? parts[2].trim() : "");
            }

            return supporter;
        }).collect(Collectors.toList());
    }

    /** @return the enhanced type descriptor for {@code List<Supporter>} */
    @Override
    public EnhancedType<List<Supporter>> type() {
        return EnhancedType.listOf(Supporter.class);
    }

    /** @return the DynamoDB attribute type (List) */
    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.L;
    }

    private static String encode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return value == null ? "" : URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
