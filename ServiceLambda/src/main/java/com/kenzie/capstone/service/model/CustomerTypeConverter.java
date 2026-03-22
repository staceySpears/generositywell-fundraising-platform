package com.kenzie.capstone.service.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * AWS SDK v2 AttributeConverter for List<Customer>.
     * Serializes the customer list to/from a single DynamoDB String attribute
     * using JSON via Gson (already a dependency in ServiceLambda).
     *
     * Replaces the legacy SDK v1 DynamoDBTypeConverter implementation.
     */
public class CustomerTypeConverter implements AttributeConverter<List<Customer>> {

    private static final Gson GSON = new Gson();
        private static final Type CUSTOMER_LIST_TYPE = new TypeToken<List<Customer>>() {}.getType();

    @Override
        public AttributeValue transformFrom(List<Customer> customers) {
                    if (customers == null || customers.isEmpty()) {
                                    return AttributeValue.builder().s("[]").build();
                    }
                    return AttributeValue.builder().s(GSON.toJson(customers)).build();
        }

    @Override
        public List<Customer> transformTo(AttributeValue attributeValue) {
                    try {
                                    String json = attributeValue.s();
                                    if (json == null || json.isEmpty() || json.equals("[]")) {
                                                        return new ArrayList<>();
                                    }
                                    return GSON.fromJson(json, CUSTOMER_LIST_TYPE);
                    } catch (Exception e) {
                                    e.printStackTrace();
                                    return new ArrayList<>();
                    }
        }

    @Override
        public EnhancedType<List<Customer>> type() {
                    return EnhancedType.listOf(Customer.class);
        }

    @Override
        public AttributeValueType attributeValueType() {
                    return AttributeValueType.S;
        }
}
