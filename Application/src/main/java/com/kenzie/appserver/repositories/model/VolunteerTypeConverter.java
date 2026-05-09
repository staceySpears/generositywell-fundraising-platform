package com.kenzie.appserver.repositories.model;

import com.kenzie.appserver.service.model.Volunteer;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DynamoDB {@link AttributeConverter} for {@code List<Volunteer>}.
 *
 * <p>Each volunteer is serialised as a pipe-delimited string:
 * {@code "id|name|email|rsvpStatus"} and stored in a DynamoDB List ({@code L}).
 * Pipe ({@code |}) was chosen over {@code x} (used by {@link SupporterTypeConverter}) to
 * avoid ambiguity with names that contain the letter 'x'.
 */
public class VolunteerTypeConverter implements AttributeConverter<List<Volunteer>> {

    private static final String DELIMITER = "|";
    private static final String DELIMITER_REGEX = "\\|";

    /**
     * Converts a list of volunteers to a DynamoDB AttributeValue (List type).
     *
     * @param input the list of volunteers, or {@code null}
     * @return a DynamoDB AttributeValue containing the serialised volunteers
     */
    @Override
    public AttributeValue transformFrom(List<Volunteer> input) {
        if (input == null) {
            return AttributeValue.builder().nul(true).build();
        }
        List<AttributeValue> items = input.stream()
                .map(v -> AttributeValue.builder()
                        .s(String.join(DELIMITER,
                                v.getId(),
                                v.getName(),
                                v.getEmail(),
                                v.getRsvpStatus()))
                        .build())
                .collect(Collectors.toList());
        return AttributeValue.builder().l(items).build();
    }

    /**
     * Converts a DynamoDB AttributeValue (List type) back to a list of volunteers.
     *
     * @param input the DynamoDB AttributeValue
     * @return the deserialised list of volunteers
     */
    @Override
    public List<Volunteer> transformTo(AttributeValue input) {
        return input.l().stream().map(av -> {
            String[] parts = av.s().split(DELIMITER_REGEX, 4);
            Volunteer volunteer = new Volunteer();
            volunteer.setId(parts[0]);
            volunteer.setName(parts[1]);
            volunteer.setEmail(parts[2]);
            volunteer.setRsvpStatus(parts[3]);
            return volunteer;
        }).collect(Collectors.toList());
    }

    /** @return the enhanced type descriptor for {@code List<Volunteer>} */
    @Override
    public EnhancedType<List<Volunteer>> type() {
        return EnhancedType.listOf(Volunteer.class);
    }

    /** @return the DynamoDB attribute type (List) */
    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.L;
    }
}
