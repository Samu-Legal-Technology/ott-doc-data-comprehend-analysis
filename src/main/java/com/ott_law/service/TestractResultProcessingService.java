package com.ott_law.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.json.JSONObject;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
//import com.amazonaws.services.dynamodbv2.document.Item;
//import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
//import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.textract.model.Block;
import com.amazonaws.services.textract.model.GetDocumentAnalysisRequest;
import com.amazonaws.services.textract.model.GetDocumentAnalysisResult;
import com.amazonaws.services.textract.model.GetDocumentTextDetectionRequest;
import com.amazonaws.services.textract.model.GetDocumentTextDetectionResult;
import com.amazonaws.services.textract.model.Relationship;
import com.ott_law.comprehent.lambda.constants.Constants;
import com.ott_law.comprehent.lambda.utils.AmazonElasticsearchUtility;
import com.ott_law.comprehent.lambda.utils.BeanFactory;
import com.ott_law.comprehent.lambda.utils.ComprehendUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestractResultProcessingService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	static final ComprehendUtility comprehendUtility = BeanFactory.getComprehendUtilityObject.get();

	static final AmazonElasticsearchUtility esUtility=BeanFactory.getESUtilityObject.get();
	
	public final SimpleDateFormat dateFormate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	
	
	public BiFunction<JSONObject, String, Map<String, Object>> prepareESDocument=(message,text)->{
		Map<String, Object> document = new HashMap<>();
		String bucketName = message.getJSONObject("DocumentLocation").getString("S3Bucket");
		String documentName = message.getJSONObject("DocumentLocation").getString("S3ObjectName");

		
		document.put("Id", message.getString("JobId")); // Textract Job ID
		document.put("BucketName", bucketName);
		document.put("DocumentName", documentName);
		document.put("DocumentType", message.getString("JobTag"));
		document.put("S3URI", "s3://"+bucketName+"/"+documentName);
		document.put("ObjectURL", "https://"+bucketName+".s3.amazonaws.com/"+documentName);
		document.put("Text",text);
//		document.put("ComprehendKeyPhrases", comprehendUtility.fetchComprehendKeyPhrases.apply(text));
		
		
		if (!message.getString("JobTag").equalsIgnoreCase("medical")) {
			document.put("EntitiesAnalysis",comprehendUtility.fetchComprehendEntities.apply(text));
			document.put("SentimentsAnalysis", comprehendUtility.fetchComprehendSentiment.apply(text));
		} else {
			document.put("MedicalEntitiesAnalysis",
					comprehendUtility.detectAllMedicalEntities.apply(text));
		}
		
		document.put("CreatedAt",dateFormate.format(new Date()));
		
		return document;
	};
	
	

	public void processRawText(JSONObject message, String tableName) {
		try {
			logger.info("Inside processRawText ........");
			DynamoDB db = new DynamoDB(BeanFactory.getDynamoDbUtilityObject.get().dynamoDBClient.get());
			GetDocumentTextDetectionRequest request = new GetDocumentTextDetectionRequest();
			request.setJobId(message.getString("JobId"));

			logger.info("GetDocumentTextDetectionRequest request os created");

			GetDocumentTextDetectionResult documentTextDetection = BeanFactory.getTextractUtilityObject
					.get().textractClient.get().getDocumentTextDetection(request);
			logger.info("job status is .... {}", documentTextDetection.getJobStatus());
			logger.info("blocks are....... {}", documentTextDetection.getBlocks());
			Iterator<Block> iterator = documentTextDetection.getBlocks().iterator();
			String bucketName = message.getJSONObject("DocumentLocation").getString("S3Bucket");
			String documentName = message.getJSONObject("DocumentLocation").getString("S3ObjectName");
			String text = "";
			while (iterator.hasNext()) {
				if (iterator.next().getBlockType().equals("LINE")) {
					Block record = iterator.next();
					logger.info("{} | {}  ", text, record.getText());
					text=text+" "+iterator.next().getText();
				}

			}

			if (!text.trim().isEmpty()) {
				
				Map<String, Object> esDocument=prepareESDocument.apply(message, text);
				esUtility.uploadESDocument.accept(esDocument);
//				Table table = db.getTable(tableName);
//				Item item = new Item().withPrimaryKey("Id", message.getString("JobId"))
//						.withString("BucketName", bucketName)
//						.withString("DocumentName", documentName)
//						.with("S3URL", "s3://"+bucketName+"/"+documentName)
//						.withString("Text", text)
//						.withList("EntitiesAnalysis", comprehendUtility.fetchComprehendEntities.apply(text))
////						.withList("ComprehendKeyPhrases", comprehendUtility.fetchComprehendKeyPhrases.apply(text))
//						.withMap("SentimentsAnalysis", comprehendUtility.fetchComprehendSentiment.apply(text))
//						.withString("CreatedAt",dateFormate.format(new Date()));
//
//				PutItemOutcome putItem = table.putItem(item);
//				if (putItem.getPutItemResult().getSdkHttpMetadata().getHttpStatusCode() == 200)
//					logger.info("Data saved in db");
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}

	}

	public void processFormAndTableText(JSONObject message, String tableName) {
		try {
			logger.info("Inside processFormAndTableText ........");
			List<Block> keyList = new ArrayList<>();
			List<Block> valueList = new ArrayList<>();
			Map<String, Block> blockMap = new HashMap<>();
			DynamoDB db = new DynamoDB(BeanFactory.getDynamoDbUtilityObject.get().dynamoDBClient.get());
			GetDocumentAnalysisRequest request = new GetDocumentAnalysisRequest();
			request.setJobId(message.getString("JobId"));

			logger.info("GetDocumentTextDetectionRequest request os created");

			GetDocumentAnalysisResult documentTextDetection = BeanFactory.getTextractUtilityObject.get().textractClient
					.get().getDocumentAnalysis(request);
//			logger.info("job status is .... {}", documentTextDetection.getJobStatus());
			logger.info("blocks are....... {}", documentTextDetection.getBlocks());
//			logger.info("documentTextDetection : {} ", documentTextDetection.getDocumentMetadata().toString());
			Iterator<Block> iterator = documentTextDetection.getBlocks().iterator();
//			String bucketName = message.getJSONObject("DocumentLocation").getString("S3Bucket");
//			String documentName = message.getJSONObject("DocumentLocation").getString("S3ObjectName");
			StringBuilder text = new StringBuilder("");
			List<Block> tableBlocks = new ArrayList<>();
			while (iterator.hasNext()) {
				Block record = iterator.next();
				blockMap.put(record.getId(), record);

//				logger.info("{} | {}  | {} ", record.getText(), record.getColumnIndex(), record.getBlockType());
//				logger.info("{}", record.getRelationships());
				if (record.getBlockType().equals("LINE")) {
					text.append(" " + record.getText());
				}

				if (record.getBlockType().equals("KEY_VALUE_SET")) {
					logger.info("KEY-Value : {}", record);
					if (record.getEntityTypes().contains("KEY")) {
//						logger.info("KEY : {}", record.getText());
						keyList.add(record);
					} else {
						valueList.add(record);
					}
				}

				if (record.getBlockType().equals("TABLE")) {
					tableBlocks.add(record);
				}

			}
			logger.info("{}", text);
			StringBuilder keys = new StringBuilder("");
			try {
				keys = getTextFromFormData(keyList, blockMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
			logger.info("{}", keys);

			List<String> keysList = new ArrayList<>();
			try {
				keysList = getTextListFromFormData(keyList, blockMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Keys List : " + keysList);

			StringBuilder values = new StringBuilder("");
			try {
				values = getTextFromFormData(valueList, blockMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
			logger.info("{}", values);

			List<String> valuesList = new ArrayList<>();
			try {
				valuesList = getTextListFromFormData(valueList, blockMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Values List : " + valuesList);
			
			List<Map<String, String>> kvMap=new ArrayList<>();
			try {
				kvMap=getKeyValueRelationship(keyList, valueList, blockMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Key-Value Pairs :" + kvMap);

//			Map<String, HashMap<String, String>> tableDataMap = getTextMapFromTable(tableBlocks, blockMap);
//			System.out.println(tableDataMap);

			List<Map<String, List>> tableDataMap = new ArrayList<>();
			try {
				tableDataMap = getTextMapFromTable(tableBlocks, blockMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println(tableDataMap);
			StringBuilder tableText = getTextFromTable(tableBlocks, blockMap);
			logger.info("tableText : {}" , tableText);

			String finalText = text.toString() + " " + keys.toString() + " " + values.toString() + " "
					+ tableText.toString();


			if (finalText != null && !finalText.trim().isEmpty()) {
				Map<String, Object> esDocument=prepareESDocument.apply(message, finalText);
				esDocument.put("FormKeyValueMapping", kvMap);
				esDocument.put("TableData", tableDataMap);
				esUtility.uploadESDocument.accept(esDocument);
//				Table table = db.getTable(tableName);
//				Item item = new Item().withPrimaryKey("Id", message.getString("JobId"))
//
//						.withString("BucketName", bucketName)
//						.withString("DocumentName", documentName)
//						.with("S3URL", "s3://"+bucketName+"/"+documentName)
//						.withString("Text", text.toString())
////						.withList("FormKeys", keysList)
////						.withList("FormValues", valuesList)
//						.withList("FormKeyValueMapping", kvMap)
//						.withMap("TableData", tableDataMap)
//						.withString("CreatedAt",dateFormate.format(new Date()));
//
//				if (!message.getString("JobTag").equalsIgnoreCase("medical")) {
//					item.withList("EntitiesAnalysis", comprehendUtility.fetchComprehendEntities.apply(finalText))
////							.withList("KeyPhrasesAnalysis",
////									comprehendUtility.fetchComprehendKeyPhrases.apply(finalText))
//							.withMap("SentimentsAnalysis", comprehendUtility.fetchComprehendSentiment.apply(finalText));
//				} else {
//					item.withMap("MedicalEntitiesAnalysis",
//							comprehendUtility.detectAllMedicalEntities.apply(finalText));
//				}
//				PutItemOutcome putItem = table.putItem(item);
//				if (putItem.getPutItemResult().getSdkHttpMetadata().getHttpStatusCode() == 200)
//					logger.info("Data saved in db");
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}

	}

//	Key-value mapping
//	public void getKeyValueMapping(List<Block> keyList,List<Block> valueList, Map<String, Block> blockMap) {
//		Map<String, Map<Block,Block>> keysValuesMap=new HashMap<>();
//		for(Block keyBlock:keyList) {
//			Map<Block,Block> keyValueMap=new HashMap<>();
//			String keyTest=getText(relationship, blockMap, text)
//		}
//	}

	public StringBuilder getTextFromFormData(List<Block> childDataList, Map<String, Block> blockMap) {
		StringBuilder text = new StringBuilder("");
		for (Block childDataBlock : childDataList) {
			if (childDataBlock.getRelationships() != null && !childDataBlock.getRelationships().isEmpty()) {
				for (Relationship relationship : childDataBlock.getRelationships()) {
					text = getTextFromFormElement(relationship, blockMap, text);
				}
			}
		}
		return text;
	}

	public StringBuilder getTextFromFormElement(Relationship relationship, Map<String, Block> blockMap,
			StringBuilder text) {
		if (relationship.getType().equals(Constants.CHILD)) {

			for (String id : relationship.getIds()) {
				Block word = blockMap.get(id);
				if (word.getBlockType().equals(Constants.WORD)) {
					text.append(word.getText() + " ");
				}
				if (word.getBlockType().equals(Constants.SELECTION_ELEMENT)
						&& word.getBlockType().equals(Constants.SELECTED)) {
					text.append("X ");
				}
			}
		}
		return text;
	}

	public List<String> getTextListFromFormData(List<Block> childDataList, Map<String, Block> blockMap) {
		List<String> textList = new ArrayList<>();
		StringBuilder text = new StringBuilder("");
		for (Block childDataBlock : childDataList) {
			if (childDataBlock.getRelationships() != null && !childDataBlock.getRelationships().isEmpty()) {
				for (Relationship relationship : childDataBlock.getRelationships()) {
					text = getTextFromFormElement(relationship, blockMap, text);
				}
				textList.add(text.toString());
				text = new StringBuilder("");
			}
		}
		return textList;
	}

//	public Map<String, HashMap<String, String>> getTextMapFromTable(List<Block> tableDataList,
//			Map<String, Block> blockMap) {
//		Map<String, HashMap<String, String>> tableRows = new HashMap<>();
//		for (Block tableDataBlock : tableDataList) {
//			logger.info("{}", tableDataBlock);
//			if (tableDataBlock.getRelationships() != null && !tableDataBlock.getRelationships().isEmpty()) {
//				for (Relationship relationship : tableDataBlock.getRelationships()) {
//					if (relationship.getType().equals(Constants.CHILD)) {
//
//						for (String id : relationship.getIds()) {
//							Block cell = blockMap.get(id);
//							if (cell.getBlockType().equals(Constants.CELL)) {
//								Integer rowIndex = cell.getRowIndex();
//								Integer columnIndex = cell.getColumnIndex();
//								HashMap<String, String> cellMap = new HashMap<>();
//								if (!tableRows.containsKey(rowIndex.toString())) {
//									cellMap.put(columnIndex.toString(), null);
//
//								} else {
//									cellMap = tableRows.get(rowIndex.toString());
//								}
//
//								if (cell.getRelationships() != null && !cell.getRelationships().isEmpty()) {
//									for (Relationship cellRelationship : cell.getRelationships()) {
//										StringBuilder text = getTextFromFormElement(cellRelationship, blockMap,
//												new StringBuilder(""));
//										cellMap.put(columnIndex.toString(), text.toString());
//									}
//								}
//								tableRows.put(rowIndex.toString(), cellMap);
//							}
//						}
//					}
//				}
//			}
//		}
//		logger.info("{}", tableRows);
//		return tableRows;
//	}
	
	
	public List<Map<String, List>> getTextMapFromTable(List<Block> tableDataList,
			Map<String, Block> blockMap) {
		Map<String, HashMap<String, String>> tableRows = new HashMap<>();
		List<Map<String, List>> rowsList=new ArrayList<>();
		for (Block tableDataBlock : tableDataList) {
			logger.info("{}", tableDataBlock);
			if (tableDataBlock.getRelationships() != null && !tableDataBlock.getRelationships().isEmpty()) {
				for (Relationship relationship : tableDataBlock.getRelationships()) {
					if (relationship.getType().equals(Constants.CHILD)) {
//						List<Map<String, String>> columnsList=new ArrayList<>();

						for (String id : relationship.getIds()) {
							Block cell = blockMap.get(id);
							if (cell.getBlockType().equals(Constants.CELL)) {
								Integer rowIndex = cell.getRowIndex();
								Integer columnIndex = cell.getColumnIndex();
								HashMap<String, String> cellMap = new HashMap<>();
								if (!tableRows.containsKey(rowIndex.toString())) {
									cellMap.put(columnIndex.toString(), null);

								} else {
									cellMap = tableRows.get(rowIndex.toString());
								}

								if (cell.getRelationships() != null && !cell.getRelationships().isEmpty()) {
									for (Relationship cellRelationship : cell.getRelationships()) {
										StringBuilder text = getTextFromFormElement(cellRelationship, blockMap,
												new StringBuilder(""));
										cellMap.put(columnIndex.toString(), text.toString());
//										Map<String , String> cellData=new HashMap<>();
//										cellData.put("column", text.toString());
//										columnsList.add(cellData);
									}
								}
								tableRows.put(rowIndex.toString(), cellMap);
//								Map<String , List> rowData=new HashMap<>();
//								rowData.put("row", columnsList);
//								rowsList.add(rowData);
							}
						}
//						Map<String , List> rowData=new HashMap<>();
//						rowData.put("row", columnsList);
//						rowsList.add(rowData);
					}
				}
			}
		}
		
//		Iterator<Map<String, HashMap<String, String>>> tableRowsItegratorIterator;
		for(Map.Entry<String,HashMap<String,String>> tableRow:tableRows.entrySet()) {
			List<Map<String, String>> columnsList=new ArrayList<>();
			Map<String,List> colMap=new HashMap<>();
			for(HashMap.Entry<String, String> column:tableRow.getValue().entrySet()) {
				Map<String , String> cellData=new HashMap<>();
				cellData.put("column", column.getValue().toString());
				columnsList.add(cellData);
			}
			colMap.put("columns",columnsList);
			Map<String , List> rowData=new HashMap<>();
			rowData.put("row", columnsList);
			rowsList.add(rowData);
		}
		System.out.println(rowsList);
		logger.info("{}", tableRows);
		return rowsList;
	}

	public StringBuilder getTextFromTable(List<Block> tableDataList, Map<String, Block> blockMap) {
		StringBuilder text = new StringBuilder("");
		for (Block tableDataBlock : tableDataList) {
			logger.info("{}", tableDataBlock);
			if (tableDataBlock.getRelationships() != null && !tableDataBlock.getRelationships().isEmpty()) {
				for (Relationship relationship : tableDataBlock.getRelationships()) {
					if (relationship.getType().equals(Constants.CHILD)) {

						for (String id : relationship.getIds()) {
							Block cell = blockMap.get(id);
							if (cell.getBlockType().equals(Constants.CELL) && cell.getRelationships() != null
									&& !cell.getRelationships().isEmpty()) {
								for (Relationship cellRelationship : cell.getRelationships()) {
									text = getTextFromFormElement(cellRelationship, blockMap, text);
								}
							}
						}
					}
				}
			}
		}
		logger.info("{}", text);
		return text;
	}
	
	public List<Map<String,String>> getKeyValueRelationship(List<Block> keyBlocks,List<Block> valueBlocks,Map<String, Block> blockMap) {
		List<Map<String,String>> kvPairList=new ArrayList<>();
		
		for(Block keyBlock: keyBlocks) {
			Map<String,String> kvMap=new HashMap<>();
			Block valueBlock=findValueBlock(keyBlock, valueBlocks);
			String key=getFormText(keyBlock, blockMap);
			String value=getFormText(valueBlock, blockMap);
			kvMap.put("key", key);
			kvMap.put("value",value);
//			kvMap.put(key, value);
			kvPairList.add(kvMap);
		}
		return kvPairList;
	}
	
	public Block findValueBlock(Block keyBlock,List<Block> valueBlocks){
		Block finalValueBlock=null;
		for(Relationship relationship:keyBlock.getRelationships()) {
			if(relationship.getType().equalsIgnoreCase(Constants.VALUE)) {
				for(String id:relationship.getIds()) {
					Block value=null;
					for(Block valueBlock:valueBlocks) {
						if(valueBlock.getId().equals(id)) {
							value=valueBlock;
						}
					}
//					values.add(value);
					finalValueBlock=value;
					System.out.println(value);
				}
			}
		}
		return finalValueBlock;
		
	}
	
	public String getFormText(Block result,Map<String, Block> blockMap) {
		String text="";
			if(result.getRelationships()!=null && !result.getRelationships().isEmpty()) {
				for(Relationship relationship:result.getRelationships()) {
					if(relationship.getType().equalsIgnoreCase(Constants.CHILD)) {
						for(String childId:relationship.getIds()) {
							Block word=blockMap.get(childId);
							if(word.getBlockType().equalsIgnoreCase(Constants.WORD)) {
								text=text+" "+word.getText();
							}
							if(word.getBlockType().equalsIgnoreCase(Constants.SELECTION_ELEMENT)) {
								if(word.getSelectionStatus().equalsIgnoreCase(Constants.SELECTED)) {
									text=text+" X";
								}
							}
						}
					}
				}
			}
		return text;
	}
}

