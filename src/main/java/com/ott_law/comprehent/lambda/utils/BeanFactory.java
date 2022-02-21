package com.ott_law.comprehent.lambda.utils;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BeanFactory {

	private BeanFactory() {

	}

	/**
	 * 
	 * @author pooja.bhagat The private inner static class that contains the
	 *         instance of the singleton class BeanFactory
	 */
	private static class SingletonHelper {
		private static final Logger logger = LoggerFactory.getLogger(SingletonHelper.class);

		static {
			logger.info("********* Inside BeanFactory Singltone Class ************");
		}
		private static final BeanFactory INSTANCE = new BeanFactory();
		private static final ComprehendUtility COMPREHEND_UTILITY = new ComprehendUtility();
		private static final DynamoDbUtility DYNAMO_DB_UTILITY = new DynamoDbUtility();
		public static final TextractUtility TEXTRACT_UTILITY = new TextractUtility();
		public static final AmazonElasticsearchUtility AMAZON_ELASTICSEARCH_UTILITY=new AmazonElasticsearchUtility();

	}

	public static final Supplier<BeanFactory> getInstance = () -> SingletonHelper.INSTANCE;

	public static final Supplier<ComprehendUtility> getComprehendUtilityObject = () -> SingletonHelper.COMPREHEND_UTILITY;

	public static final Supplier<DynamoDbUtility> getDynamoDbUtilityObject = () -> SingletonHelper.DYNAMO_DB_UTILITY;

	public static final Supplier<TextractUtility> getTextractUtilityObject = () -> SingletonHelper.TEXTRACT_UTILITY;

	public static final Supplier<AmazonElasticsearchUtility> getESUtilityObject = () -> SingletonHelper.AMAZON_ELASTICSEARCH_UTILITY;

}
