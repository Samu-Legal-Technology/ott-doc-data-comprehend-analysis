package com.ott_law.comprehent.lambda.utils;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.ott_law.comprehent.lambda.constants.Constants;
import com.ott_law.interceptors.AWSRequestSigningApacheInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.ott_law.comprehent.lambda.constants.Constants.AWS_ES_SERVICE_NAME;

@Component
public class AmazonElasticsearchUtility {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());

 
	static final AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();

	// Adds the intercepter to the ES REST client
	public static final Supplier<RestHighLevelClient>  elasticSearchClient =()->{
		AWS4Signer signer = new AWS4Signer();
		signer.setServiceName(AWS_ES_SERVICE_NAME);
		signer.setRegionName(Constants.AWS_REGION_NAME);
		HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(AWS_ES_SERVICE_NAME, signer,
				credentialsProvider);
		return new RestHighLevelClient(RestClient.builder(HttpHost.create("https://search-ott-stage-document-analysis-6goxqpago76rsqj73niwy22pay.us-east-1.es.amazonaws.com"))
				.setHttpClientConfigCallback(hacb -> hacb.addInterceptorLast(interceptor)));
	};


	public final Consumer<Map<String, Object>> uploadESDocument= document ->{
		RestHighLevelClient esClient = elasticSearchClient.get();
		IndexRequest request = new IndexRequest("document-analysis-index").source(document);
		IndexResponse response;
		try {
			response = esClient.index(request, RequestOptions.DEFAULT);
			logger.info("{}",response);
		} catch (IOException e) {
			e.printStackTrace();
		}
	};
	

}
