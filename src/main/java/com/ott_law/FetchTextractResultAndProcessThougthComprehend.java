package com.ott_law;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import com.ott_law.comprehent.lambda.constants.Constants;
import com.ott_law.service.TestractResultProcessingService;

@SpringBootApplication
public class FetchTextractResultAndProcessThougthComprehend implements RequestHandler<SQSEvent, String> {

	static TestractResultProcessingService testractResultProcessingService = new TestractResultProcessingService();
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public String handleRequest(SQSEvent event, Context context) {

		for (SQSMessage msg : event.getRecords()) {
			try {
				logger.info("message is {}", msg.getBody());
				logger.info("message to string is {}", msg.getBody());

				JSONObject json = new JSONObject(msg.getBody());
				logger.info("json message is {}", json.getString("Message"));
				JSONObject message = new JSONObject(json.getString("Message"));
				logger.info("Message is {}", json.getString("Message"));
				logger.info("doc location {} | message.get(\"DocumentLocation\") : {} ",
						message.getJSONObject("DocumentLocation"), message.get("DocumentLocation"));

				logger.info("job id is {}", message.getString("JobId"));

				if (!message.isEmpty() && message.get(Constants.API_KEY) != null) {

					switch (message.get(Constants.API_KEY).toString()) {
					case Constants.START_DOCUMENT_TEXT_DETECTION:
						testractResultProcessingService.processRawText(message, "Textract");
						break;

					case Constants.START_DOCUMENT_ANALYSIS:
						testractResultProcessingService.processFormAndTableText(message, "Textract");
						break;
					default:
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return null;
	}

}
