package com.kenzie.appserver.repositories.model;

import com.kenzie.appserver.service.model.Attendee;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.stream.Collectors;

public class AttendeeTypeConverter implements AttributeConverter<List<Attendee>> {

    @Override
    public AttributeValue transformFrom(List<Attendee> input) {
        if (input == null) {
            return AttributeValue.builder().nul(true).build();
        }
        List<AttributeValue> items = input.stream()
                .map(a -> AttributeValue.builder()
                        .s(String.format("%s x %s x %s", a.getId(), a.getName(), a.getEmail()))
                        .build())
                .collect(Collectors.toList());
        return AttributeValue.builder().l(items).build();
    }

    @Override
    public List<Attendee> transformTo(AttributeValue input) {
        return input.l().stream().map(av -> {
            Attendee attendee = new Attendee();
            String[] data = av.s().split("x");
            attendee.setId(data[0].trim());
            attendee.setName(data[1].trim());
            attendee.setEmail(data[2].trim());
            return attendee;
        }).collect(Collectors.toList());
    }

    @Override
    public EnhancedType<List<Attendee>> type() {
        return EnhancedType.listOf(Attendee.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.L;
    }
}
