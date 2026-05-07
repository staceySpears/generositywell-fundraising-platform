package com.kenzie.appserver.repositories.model;

import com.kenzie.appserver.service.model.User;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * DynamoDB AttributeConverter for {@link User}.
 * Serialises a User as the String {@code "id x name x email"} stored in a DynamoDB S attribute.
 */
public class UserTypeConverter implements AttributeConverter<User> {

    /**
     * Converts a User to a DynamoDB String AttributeValue.
     *
     * @param input the User to serialise, or {@code null}
     * @return a DynamoDB AttributeValue containing the serialised user
     */
    @Override
    public AttributeValue transformFrom(User input) {
        if (input == null) {
            return AttributeValue.builder().nul(true).build();
        }
        String value = String.format("%s x %s x %s",
                input.getId(), input.getName(), input.getEmail());
        return AttributeValue.builder().s(value).build();
    }

    /**
     * Converts a DynamoDB String AttributeValue back to a User.
     *
     * @param input the DynamoDB AttributeValue
     * @return the deserialised User
     */
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

    /** @return the enhanced type descriptor for {@link User} */
    @Override
    public EnhancedType<User> type() {
        return EnhancedType.of(User.class);
    }

    /** @return the DynamoDB attribute type (String) */
    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
