package com.kenzie.capstone.service.model;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * AWS SDK v2 AttributeConverter for the User domain object.
     * Serializes User to/from a single DynamoDB String attribute
     * using the format: "id x name x email".
     *
     * Replaces the legacy SDK v1 DynamoDBTypeConverter implementation.
     */
public class UserTypeConverter implements AttributeConverter<User> {

    @Override
        public AttributeValue transformFrom(User user) {
                    if (user == null) {
                                    return AttributeValue.builder().nul(true).build();
                    }
                    String value = String.format("%s x %s x %s",
                                                                 user.getId(), user.getName(), user.getEmail());
                    return AttributeValue.builder().s(value).build();
        }

    @Override
        public User transformTo(AttributeValue attributeValue) {
                    User user = new User();
                    try {
                                    String s = attributeValue.s();
                                    if (s != null && !s.isEmpty()) {
                                                        String[] data = s.split(" x ");
                                                        user.setId(data[0].trim());
                                                        user.setName(data[1].trim());
                                                        user.setEmail(data[2].trim());
                                    }
                    } catch (Exception e) {
                                    e.printStackTrace();
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
