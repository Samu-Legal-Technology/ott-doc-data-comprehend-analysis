package com.ott_law.service;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.comprehendmedical.ComprehendMedicalClient;
import software.amazon.awssdk.services.comprehendmedical.model.DetectEntitiesRequest;
import software.amazon.awssdk.services.comprehendmedical.model.DetectEntitiesResponse;
import software.amazon.awssdk.services.comprehendmedical.model.Entity;
import software.amazon.awssdk.services.comprehendmedical.model.ComprehendMedicalException;
import java.util.Iterator;
import java.util.List;

public class DetectMedicalEntities {

    public static void main(String[] args) {
    	
    	

        String text = "Pt is 87 yo woman, highschool teacher with past medical history that includes\n" +
                "   - status post cardiac catheterization in April 2019.\n" +
                "She presents today with palpitations and chest pressure.\n" +
                "HPI : Sleeping trouble on present dosage of Clonidine. Severe Rash  on face and leg, slightly itchy  \n" +
                "Meds : Vyvanse 50 mgs po at breakfast daily, \n" +
                "            Clonidine 0.2 mgs -- 1 and 1 / 2 tabs po qhs \n" +
                "HEENT : Boggy inferior turbinates, No oropharyngeal lesion \n" +
                "Lungs : clear \n" +
                "Heart : Regular rhythm \n" +
                "Skin :  Mild erythematous eruption to hairline \n" +
                "\n" +
                "Follow-up as scheduled";
        Region region = Region.US_EAST_1;
        ComprehendMedicalClient medClient = ComprehendMedicalClient.builder()
                .region(region)
                .build();

        System.out.println("Calling Detect Medical Entities");
        detectAllEntities(medClient, text) ;
        medClient.close();

    }
    public static void detectAllEntities(ComprehendMedicalClient medClient, String text ) {

        try {
            DetectEntitiesRequest detectEntitiesRequest = DetectEntitiesRequest.builder()
                    .text(text)
                    .build();

            DetectEntitiesResponse detectEntitiesResult = medClient.detectEntities(detectEntitiesRequest);
            List<Entity> entList = detectEntitiesResult.entities();
            Iterator<Entity> lanIterator = entList.iterator();

            while (lanIterator.hasNext()) {
                Entity entity = lanIterator.next();
                System.out.println("Entity text is " + entity.text());
            }

        } catch (ComprehendMedicalException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
}