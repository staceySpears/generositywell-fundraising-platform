package com.kenzie.appserver.repositories.model;

import com.kenzie.appserver.service.model.Customer;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.stream.Collectors;

public class CustomerTypeConverter implements AttributeConverter<List<Customer>> {

    @Override
    public AttributeValue transformFrom(List<Customer> input) {
        if (input == null) {
            return AttributeValue.builder().nul(true).build();
        }
        List<AttributeValue> items = input.stream()
                .map(n -> AttributeValue.builder()
                        .s(String.format("%s x %s x %s", n.getId(), n.getName(), n.getEmail()))
                        .build())
                .collect(Collectors.toList());
        return AttributeValue.builder().l(items).build();
    }

    @Override
    public List<Customer> transformTo(AttributeValue input) {
        return input.l().stream().map(av -> {
            Customer customer = new Customer();
            String[] data = av.s().split("x");
            customer.setId(data[0].trim());
            customer.setName(data[1].trim());
            customer.setEmail(data[2].trim());
            return customer;
        }).collect(Collectors.toList());
    }

    @Override
    public EnhancedType<List<Customer>> type() {
        return EnhancedType.listOf(Customer.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.L;
    }
}
