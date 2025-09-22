package com.example.webhook.service;

import com.example.webhook.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WebhookService implements CommandLineRunner {

    @Autowired
    private RestTemplate restTemplate;

    private final String GENERATE_WEBHOOK_URL = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
    private final String SUBMIT_WEBHOOK_URL = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting Bajaj Webhook Application...");
        System.out.println("Processing Question 2: Count younger employees in same department");

        try {
            // Step 1: Generate webhook
            WebhookResponse response = generateWebhook();

            if (response != null) {
                System.out.println("Webhook generated successfully!");
                System.out.println("Webhook URL: " + response.getWebhook());

                // Step 2: Determine SQL question based on regNo
                String regNo = "REG12347"; // Replace with YOUR actual registration number
                String sqlQuery = determineSqlQuery(regNo);

                System.out.println("Generated SQL Query: " + sqlQuery);

                // Step 3: Submit solution
                submitSolution(sqlQuery, response.getAccessToken());
            } else {
                System.err.println("❌ Failed to generate webhook. Application stopping.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error in webhook application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private WebhookResponse generateWebhook() {
        try {
            WebhookRequest request = new WebhookRequest();
            request.setName("Your Name"); // Replace with YOUR actual name
            request.setRegNo("REG12347"); // Replace with YOUR actual registration number
            request.setEmail("your.email@example.com"); // Replace with YOUR actual email

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<WebhookRequest> entity = new HttpEntity<>(request, headers);

            System.out.println("Sending POST request to generate webhook...");
            ResponseEntity<WebhookResponse> response = restTemplate.postForEntity(
                    GENERATE_WEBHOOK_URL, entity, WebhookResponse.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                System.err.println("Unexpected response status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error generating webhook: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private String determineSqlQuery(String regNo) {
        try {
            // Extract last two digits of registration number and trim any whitespace
            String lastTwoDigits = regNo.substring(regNo.length() - 2).trim();

            // Additional safety check
            if (lastTwoDigits.isEmpty()) {
                System.err.println("❌ Invalid registration number: " + regNo);
                return getDefaultQuery();
            }

            System.out.println("Last two digits of regNo: '" + lastTwoDigits + "'");
            int digits = Integer.parseInt(lastTwoDigits);
            System.out.println("Parsed digits: " + digits);

            if (digits % 2 == 1) {
                // Odd - Question 1
                System.out.println("Registration number ends in ODD digits (" + digits + ") - Question 1");
                return "SELECT * FROM employees WHERE salary > (SELECT AVG(salary) FROM employees);";
            } else {
                // Even - Question 2 (Count younger employees in same department)
                System.out.println("Registration number ends in EVEN digits (" + digits + ") - Question 2");
                return "SELECT e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME, " +
                        "COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT " +
                        "FROM EMPLOYEE e1 " +
                        "JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID " +
                        "LEFT JOIN EMPLOYEE e2 ON e1.DEPARTMENT = e2.DEPARTMENT AND e2.DOB > e1.DOB " +
                        "GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME " +
                        "ORDER BY e1.EMP_ID DESC;";
            }
        } catch (NumberFormatException e) {
            System.err.println("❌ Error parsing registration number: " + regNo);
            System.err.println("Error details: " + e.getMessage());
            return getDefaultQuery();
        } catch (Exception e) {
            System.err.println("❌ Unexpected error in determineSqlQuery: " + e.getMessage());
            return getDefaultQuery();
        }
    }

    private String getDefaultQuery() {
        System.out.println("Using default query for Question 2 (Even registration numbers)");
        return "SELECT e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME, " +
                "COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT " +
                "FROM EMPLOYEE e1 " +
                "JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID " +
                "LEFT JOIN EMPLOYEE e2 ON e1.DEPARTMENT = e2.DEPARTMENT AND e2.DOB > e1.DOB " +
                "GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME " +
                "ORDER BY e1.EMP_ID DESC;";
    }

    private void submitSolution(String sqlQuery, String accessToken) {
        try {
            SubmissionRequest request = new SubmissionRequest();
            request.setFinalQuery(sqlQuery);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken);

            HttpEntity<SubmissionRequest> entity = new HttpEntity<>(request, headers);

            System.out.println("Submitting SQL solution to webhook...");
            ResponseEntity<String> response = restTemplate.postForEntity(
                    SUBMIT_WEBHOOK_URL, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("✅ Solution submitted successfully!");
                System.out.println("Response: " + response.getBody());
                System.out.println("🎉 Bajaj Webhook Application completed successfully!");
            } else {
                System.err.println("❌ Unexpected response status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ Error submitting solution: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
