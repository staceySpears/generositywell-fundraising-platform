package com.kenzie.appserver.repositories.model;

import com.kenzie.appserver.service.model.Supporter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DynamoDB AttributeConverter for {@code List<Supporter>}.
 * Each supporter is serialised as {@code "id x name x email"} and stored in a DynamoDB List (L).
 */
public class SupporterTypeConverter implements AttributeConverter<List<Supporter>> {

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
                .map(s -> AttributeValue.builder()
                        .s(String.format("%s x %s x %s", s.getId(), s.getName(), s.getEmail()))
                        .build())
                .collect(Collectors.toList());
        return AttributeValue.builder().l(items).build();
    }

    /**
     * Converts a DynamoDB AttributeValue (List type) back to a list of supporters.
     *
     * @param input the DynamoDB AttributeValue
     * @return the deserialised list of supporters
     */
    @Override
    public List<Supporter> transformTo(AttributeValue input) {
        return input.l().stream().map(av -> {
            Supporter supporter = new Supporter();
            String[] data = av.s().split("x");
            supporter.setId(data[0].trim());
            supporter.setName(data[1].trim());
            supporter.setEmail(data[2].trim());
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
}
