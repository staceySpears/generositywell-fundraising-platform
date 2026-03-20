package com.kenzie.appserver.repositories.model;

import com.kenzie.appserver.service.model.User;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class UserTypeConverter implements AttributeConverter<User> {

    @Override
    public AttributeValue transformFrom(User input) {
        if (input == null) {
            return AttributeValue.builder().nul(true).build();
        }
        String value = String.format("%s x %s x %s",
                input.getId(), input.getName(), input.getEmail());
        return AttributeValue.builder().s(value).build();
    }

    @Override
    public User transformTo(AttributeValue input) {
        User user = new User();
        String s = input.s();
        if (s != null && s.length() != 0) {
            String[] data = s.split("x");
            user.setId(data[0].trim());
            user.setName(data[1].trim());
            user.setEmail(data[2].trim());
        }
        return user;
    }

    @Override
    public EnhancedType<User> type() {
        return EnhancedType.of(User.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
