# OTT Document Data Comprehend Analysis

## Overview

This AWS Lambda function provides intelligent document analysis by combining AWS Textract for text extraction with AWS Comprehend for natural language processing. It processes legal documents, forms, and medical records to extract structured data and perform sentiment analysis, entity recognition, and key phrase extraction, then indexes the results in Elasticsearch for searchability.

## Architecture

```
┌─────────────────┐     ┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   S3 Document   │────▶│  Textract   │────▶│   SQS Queue     │────▶│     Lambda      │
└─────────────────┘     │   Service   │     │ (Job Complete)  │     │   (Process)     │
                        └─────────────┘     └─────────────────┘     └────────┬────────┘
                                                                              │
                        ┌─────────────────────────────────────────────────────┴────────┐
                        │                                                              │
                        ▼                                                              ▼
                ┌─────────────────┐                                    ┌──────────────────┐
                │   Comprehend    │                                    │  Elasticsearch   │
                │  (NLP Analysis) │                                    │ (Index & Search) │
                └─────────────────┘                                    └──────────────────┘
```

## Features

### Document Processing
- **Text Extraction**: Extract text from PDFs, images, and scanned documents
- **Form Analysis**: Extract key-value pairs from structured forms
- **Table Processing**: Preserve table structure with row/column relationships
- **Multi-page Support**: Handle documents with multiple pages

### Natural Language Processing
- **Entity Recognition**: Identify people, places, organizations, dates, and quantities
- **Sentiment Analysis**: Determine document sentiment (positive, negative, neutral, mixed)
- **Key Phrase Extraction**: Extract important phrases and concepts
- **Medical Entity Detection**: Specialized detection for medical documents
  - Medical conditions
  - Medications
  - Anatomy references
  - Protected Health Information (PHI)

### Data Management
- **Elasticsearch Integration**: Full-text search on processed documents
- **Structured Output**: JSON format preserving document structure
- **Metadata Preservation**: S3 location, timestamps, job IDs

## Technical Stack

- **Java 8**: Core programming language
- **Spring Boot 2.5.3**: Application framework
- **AWS Lambda**: Serverless compute
- **AWS Textract**: OCR and document analysis
- **AWS Comprehend**: Natural language processing
- **AWS Comprehend Medical**: Healthcare-specific NLP
- **Amazon Elasticsearch**: Document indexing and search
- **Maven**: Build and dependency management

## Prerequisites

- Java 8 or higher
- Maven 3.6+
- AWS Account with appropriate permissions
- Elasticsearch domain configured

## Installation & Build

1. Clone the repository:
```bash
git clone https://github.com/Samu-Legal-Technology/ott-doc-data-comprehend-analysis.git
cd ott-doc-data-comprehend-analysis
```

2. Build the project:
```bash
mvn clean package
```

3. The Lambda deployment package will be created at:
```
target/aws-comprehend-lambda-0.0.1-SNAPSHOT.jar
```

## Configuration

### Application Properties

Configure in `src/main/resources/application.properties`:

```properties
# AWS Configuration
aws.region=us-east-1

# Elasticsearch Configuration
aws.es.endpoint=https://your-es-domain.us-east-1.es.amazonaws.com
aws.es.index=document-analysis-index

# Logging Levels
logging.level.com.ott_law=INFO
logging.level.org.springframework=INFO
```

### Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `AWS_REGION` | AWS region | `us-east-1` |
| `ES_ENDPOINT` | Elasticsearch endpoint | `https://domain.es.amazonaws.com` |
| `ES_INDEX` | Index name | `document-analysis-index` |

## Deployment

### Lambda Function Setup

1. Create Lambda function in AWS Console
2. Runtime: Java 8 (Corretto)
3. Handler: `com.ott_law.FetchTextractResultAndProcessThougthComprehend::handleRequest`
4. Memory: 1024 MB (recommended)
5. Timeout: 5 minutes
6. Upload JAR file from target directory

### IAM Permissions

Required permissions for Lambda execution role:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "textract:GetDocumentTextDetection",
        "textract:GetDocumentAnalysis"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "comprehend:DetectEntities",
        "comprehend:DetectSentiment",
        "comprehend:DetectKeyPhrases",
        "comprehend:DetectSyntax",
        "comprehendmedical:DetectEntitiesV2",
        "comprehendmedical:DetectPHI"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::*/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:*:*:*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "es:*"
      ],
      "Resource": "arn:aws:es:*:*:*/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

### SQS Configuration

1. Create SQS queue for Textract job notifications
2. Configure Textract to send completion notifications to SQS
3. Add SQS as Lambda trigger with appropriate batch size

## Usage

### Document Processing Flow

1. **Upload Document**: Place document in S3 bucket
2. **Start Textract Job**:
   ```python
   # For simple text extraction
   textract.start_document_text_detection(
       DocumentLocation={'S3Object': {'Bucket': bucket, 'Name': key}}
   )
   
   # For forms and tables
   textract.start_document_analysis(
       DocumentLocation={'S3Object': {'Bucket': bucket, 'Name': key}},
       FeatureTypes=['FORMS', 'TABLES']
   )
   ```
3. **Lambda Triggered**: Automatically processes when job completes
4. **View Results**: Query Elasticsearch for processed documents

### Supported Document Types

- **Legal Documents**: Contracts, agreements, court filings
- **Medical Records**: Patient records, prescriptions, lab results
- **Forms**: Tax forms, applications, questionnaires
- **Structured Documents**: Invoices, receipts, statements

## API Reference

### Main Handler

```java
public class FetchTextractResultAndProcessThougthComprehend 
    implements RequestHandler<SQSEvent, Void> {
    
    public Void handleRequest(SQSEvent input, Context context)
}
```

### Utility Classes

#### ComprehendUtility
- `detectEntities(String text)`: Extract entities
- `detectSentiment(String text)`: Analyze sentiment
- `extractKeyPhrases(String text)`: Extract key phrases
- `detectMedicalEntities(String text)`: Medical entity detection

#### TextractUtility
- `getTextractResults(String jobId)`: Retrieve job results
- `processTextDetection(GetDocumentTextDetectionResult result)`: Process text
- `processDocumentAnalysis(GetDocumentAnalysisResult result)`: Process forms/tables

## Data Models

### Processed Document Structure

```json
{
  "documentId": "unique-id",
  "s3Location": "s3://bucket/key",
  "timestamp": "2024-01-01T00:00:00Z",
  "textractJobId": "job-id",
  "extractedText": "full document text",
  "entities": [
    {
      "text": "John Doe",
      "type": "PERSON",
      "score": 0.99
    }
  ],
  "sentiment": {
    "sentiment": "POSITIVE",
    "scores": {
      "positive": 0.95,
      "negative": 0.02,
      "neutral": 0.02,
      "mixed": 0.01
    }
  },
  "keyPhrases": ["legal agreement", "contract terms"],
  "forms": {
    "keyValuePairs": [
      {
        "key": "Name",
        "value": "John Doe"
      }
    ]
  },
  "tables": [
    {
      "rows": 3,
      "columns": 4,
      "cells": []
    }
  ]
}
```

## Monitoring & Debugging

### CloudWatch Logs
- Lambda execution logs
- Textract job status
- Comprehend API calls
- Elasticsearch indexing results

### Metrics to Monitor
- Lambda invocation count
- Error rate
- Duration
- SQS message age
- Elasticsearch indexing success rate

### Common Issues
1. **Textract Job Not Found**: Ensure job ID is valid
2. **Comprehend Throttling**: Implement retry logic
3. **Elasticsearch Connection**: Verify endpoint and permissions
4. **Large Documents**: Increase Lambda memory/timeout

## Performance Optimization

1. **Batch Processing**: Process multiple documents per Lambda invocation
2. **Text Chunking**: Split large texts for Comprehend (5000 byte limit)
3. **Caching**: Cache frequently accessed Elasticsearch queries
4. **Async Processing**: Use step functions for complex workflows

## Security Considerations

- **Data Encryption**: Enable S3 encryption for documents
- **VPC Configuration**: Place Lambda in VPC for Elasticsearch access
- **IAM Policies**: Use least-privilege access
- **PHI Handling**: Enable HIPAA compliance for medical documents
- **Audit Logging**: Track all document processing activities

## Future Enhancements

- [ ] Support for additional languages
- [ ] Custom entity recognition models
- [ ] Document classification
- [ ] Automated redaction of sensitive information
- [ ] Integration with document management systems
- [ ] Real-time processing dashboard
- [ ] Machine learning model training on extracted data

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/Enhancement`)
3. Commit changes (`git commit -m 'Add Enhancement'`)
4. Push to branch (`git push origin feature/Enhancement`)
5. Open Pull Request

## License

Copyright © 2024 Samu Legal Technology. All rights reserved.

---

*Maintained by Samu Legal Technology Development Team*