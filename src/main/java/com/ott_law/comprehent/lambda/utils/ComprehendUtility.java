package com.ott_law.comprehent.lambda.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.ComprehendException;
import software.amazon.awssdk.services.comprehend.model.DetectEntitiesRequest;
import software.amazon.awssdk.services.comprehend.model.DetectEntitiesResponse;
import software.amazon.awssdk.services.comprehend.model.DetectKeyPhrasesRequest;
import software.amazon.awssdk.services.comprehend.model.DetectKeyPhrasesResponse;
import software.amazon.awssdk.services.comprehend.model.DetectSentimentRequest;
import software.amazon.awssdk.services.comprehend.model.DetectSentimentResponse;
import software.amazon.awssdk.services.comprehend.model.Entity;
import software.amazon.awssdk.services.comprehend.model.KeyPhrase;
import software.amazon.awssdk.services.comprehendmedical.ComprehendMedicalClient;
import software.amazon.awssdk.services.comprehendmedical.model.ComprehendMedicalException;

@Component
public class ComprehendUtility {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	
	public final Supplier<ComprehendClient> comprehendClient = () -> ComprehendClient.builder().region(Region.US_EAST_1).build();


	public final Supplier<ComprehendMedicalClient> comprehendMedicalClient = () -> ComprehendMedicalClient.builder().region(Region.US_EAST_1).build();


	public final Function<String, List<Map<String,String>>> fetchComprehendEntities = text -> {
		List<Map<String,String>> entities = new ArrayList<>();
		try {
			DetectEntitiesRequest detectEntitiesRequest = DetectEntitiesRequest.builder().text(text).languageCode("en")
					.build();

			DetectEntitiesResponse detectEntitiesResult = comprehendClient.get().detectEntities(detectEntitiesRequest);
			List<Entity> entList = detectEntitiesResult.entities();
			Iterator<Entity> lanIterator = entList.iterator();

			while (lanIterator.hasNext()) {
				Map<String,String> entityMap=new HashMap<>();
				Entity entity = lanIterator.next();
				entityMap.put("Entity", entity.text());
				entityMap.put("Type", entity.type().toString());
				entities.add(entityMap);

			}
		} catch (ComprehendException e) {
			e.printStackTrace();
		}
		return entities;

	};

	public final Function<String, List<String>> fetchComprehendKeyPhrases = text -> {
		List<String> keyPhrases = new ArrayList<>();
		try {
			DetectKeyPhrasesRequest detectKeyPhrasesRequest = DetectKeyPhrasesRequest.builder().text(text)
					.languageCode("en").build();

			DetectKeyPhrasesResponse detectKeyPhrasesResult = comprehendClient.get()
					.detectKeyPhrases(detectKeyPhrasesRequest);

			List<KeyPhrase> phraseList = detectKeyPhrasesResult.keyPhrases();
			Iterator<KeyPhrase> keyIterator = phraseList.iterator();

			while (keyIterator.hasNext()) {
				KeyPhrase keyPhrase = keyIterator.next();
				logger.info("Key phrase text is {}" , keyPhrase);
				keyPhrases.add(keyPhrase.text());
			}

		} catch (ComprehendException e) {
			logger.info(e.awsErrorDetails().errorMessage());
			System.exit(1);
		}

		return keyPhrases;
	};

	public final Function<String, List<Map<String, String>>> fetchComprehendSentiment = text -> {
		List<Map<String, String>> sentimentList=new ArrayList<>();
		Map<String, String> sentiment = new HashMap<>();
		try {
			DetectSentimentRequest detectSentimentRequest = DetectSentimentRequest.builder().text(text)
					.languageCode("en").build();

			DetectSentimentResponse detectSentimentResult = comprehendClient.get()
					.detectSentiment(detectSentimentRequest);
			logger.info("The Neutral value is {}" , detectSentimentResult.sentimentScore().neutral());
			sentiment.put("neutral", detectSentimentResult.sentimentScore().neutral().toString());
			sentiment.put("positive", detectSentimentResult.sentimentScore().positive().toString());
			sentiment.put("negative", detectSentimentResult.sentimentScore().negative().toString());
			sentiment.put("mixed", detectSentimentResult.sentimentScore().mixed().toString());
			sentimentList.add(sentiment);
			
		} catch (ComprehendException e) {
			logger.info(e.awsErrorDetails().errorMessage());
			System.exit(1);
		}

		return sentimentList;
	};

	public final Function<String, List<Map<String, String>>> detectAllMedicalEntities = text -> {
		List<Map<String, String>> medicalEntities = new ArrayList<>();
		try {
			software.amazon.awssdk.services.comprehendmedical.model.DetectEntitiesRequest detectEntitiesRequest = software.amazon.awssdk.services.comprehendmedical.model.DetectEntitiesRequest
					.builder().text(text).build();

			software.amazon.awssdk.services.comprehendmedical.model.DetectEntitiesResponse detectEntitiesResult = comprehendMedicalClient
					.get().detectEntities(detectEntitiesRequest);

			List<software.amazon.awssdk.services.comprehendmedical.model.Entity> entList = detectEntitiesResult
					.entities();
			Iterator<software.amazon.awssdk.services.comprehendmedical.model.Entity> lanIterator = entList.iterator();

			while (lanIterator.hasNext()) {
				software.amazon.awssdk.services.comprehendmedical.model.Entity entity = lanIterator.next();
				logger.info("Entity text is {}" , entity);
				logger.info("{} | ",entity.attributes());
				logger.info("{}",entity.type());
				Map<String, String> entityData = new HashMap<>();
				entityData.put("Entity", entity.text());
				entityData.put("Type", entity.typeAsString());
				entityData.put("Category", entity.categoryAsString());
				entityData.put("Traits", entity.traits().toString());
				medicalEntities.add(entityData);
			}

		} catch (ComprehendMedicalException e) {
			e.printStackTrace();
		}
		return medicalEntities;
	};

}
