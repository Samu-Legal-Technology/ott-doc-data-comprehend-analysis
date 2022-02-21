package com.ott_law.service;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.model.S3Object;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.StartDocumentAnalysisRequest;
import software.amazon.awssdk.services.textract.model.DocumentLocation;
import software.amazon.awssdk.services.textract.model.TextractException;
import software.amazon.awssdk.services.textract.model.StartDocumentAnalysisResponse;
import software.amazon.awssdk.services.textract.model.GetDocumentAnalysisRequest;
import software.amazon.awssdk.services.textract.model.GetDocumentAnalysisResponse;
import software.amazon.awssdk.services.textract.model.FeatureType;
import java.util.ArrayList;
import java.util.List;

public class StartDocumentAnalysis {

    public static void main(String[] args) {

//        final String USAGE = "\n" +
//                "Usage:\n" +
//                "    StartDocumentAnalysis <bucketName> <docName> \n\n" +
//                "Where:\n" +
//                "    bucketName - the name of the Amazon S3 bucket that contains the document. \n\n" +
//                "    docName - the document name (must be an image, for example, book.png). \n";

//        if (args.length != 2) {
//            System.out.println(USAGE);
//            System.exit(1);
//        }

        Region region = Region.US_EAST_1;
        TextractClient textractClient = TextractClient.builder()
                .region(region)
                .build();

//        String bucketName = args[0];
//        String docName = args[1];

        startDocAnalysisS3 (textractClient, "ottsalesforcepoc", "PdfFormExample.pdf");
        textractClient.close();
    }

    public static void startDocAnalysisS3 (TextractClient textractClient, String bucketName, String docName) {

        try {

            List<FeatureType> myList = new ArrayList<FeatureType>();
            myList.add(FeatureType.TABLES);
            myList.add(FeatureType.FORMS);

            S3Object s3Object = S3Object.builder()
                    .bucket(bucketName)
                    .name(docName)
                    .build();

            DocumentLocation location = DocumentLocation.builder()
                    .s3Object(s3Object)
                    .build();

            StartDocumentAnalysisRequest documentAnalysisRequest = StartDocumentAnalysisRequest.builder()
                    .documentLocation(location)
                    .featureTypes(myList)
                    .build();

            StartDocumentAnalysisResponse response = textractClient.startDocumentAnalysis(documentAnalysisRequest);

            // Get the job ID
            String jobId = response.jobId();

            String result = getJobResults(textractClient,jobId);

            System.out.println("The status of the job is: "+result);

        } catch (TextractException e) {

            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static String getJobResults(TextractClient textractClient, String jobId) {

        GetDocumentAnalysisRequest analysisRequest = GetDocumentAnalysisRequest.builder()
                .jobId(jobId)
                .maxResults(1000)
                .build();

        GetDocumentAnalysisResponse response = textractClient.getDocumentAnalysis(analysisRequest);
        String status = response.jobStatus().toString();
        return status;
    }
}