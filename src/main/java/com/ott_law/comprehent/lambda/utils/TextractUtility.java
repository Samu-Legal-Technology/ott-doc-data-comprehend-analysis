package com.ott_law.comprehent.lambda.utils;

import java.util.function.Supplier;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.AmazonTextractClientBuilder;

public class TextractUtility {

	public final Supplier<AmazonTextract> textractClient=()-> AmazonTextractClientBuilder.standard().withRegion(Regions.US_EAST_1).build();

}
