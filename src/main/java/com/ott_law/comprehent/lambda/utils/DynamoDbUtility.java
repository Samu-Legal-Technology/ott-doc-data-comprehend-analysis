package com.ott_law.comprehent.lambda.utils;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

@Component
public class DynamoDbUtility {

	public final Supplier<AmazonDynamoDB> dynamoDBClient=()-> AmazonDynamoDBClientBuilder.standard().build();
}
