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
        System.out.println("Starting Webhook Application...");

        // Step 1: Generate webhook
        WebhookResponse response = generateWebhook();

        if (response != null) {
            System.out.println("Webhook generated successfully!");
            System.out.println("Webhook URL: " + response.getWebhook());

            // Step 2: Determine SQL question based on regNo
            String regNo = "REG12347"; // Replace with your actual registration number
            String sqlQuery = determineSqlQuery(regNo);

            // Step 3: Submit solution
            submitSolution(sqlQuery, response.getAccessToken());
        }
    }

    private WebhookResponse generateWebhook() {
        try {
            WebhookRequest request = new WebhookRequest();
            request.setName("Your Name"); // Replace with your actual name
            request.setRegNo("REG12347"); // Replace with your actual registration number
            request.setEmail("your.email@example.com"); // Replace with your actual email

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<WebhookRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<WebhookResponse> response = restTemplate.postForEntity(
                    GENERATE_WEBHOOK_URL, entity, WebhookResponse.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
        } catch (Exception e) {
            System.err.println("Error generating webhook: " + e.getMessage());
        }
        return null;
    }

    private String determineSqlQuery(String regNo) {
        // Extract last two digits of registration number
        String lastTwoDigits = regNo.substring(regNo.length() - 2);
        int digits = Integer.parseInt(lastTwoDigits);

        if (digits % 2 == 1) {
            // Odd - Question 1
            return "SELECT * FROM employees WHERE salary > (SELECT AVG(salary) FROM employees);";
        } else {
            // Even - Question 2
            return "SELECT department, COUNT(*) as employee_count FROM employees GROUP BY department HAVING COUNT(*) > 5;";
        }
    }

    private void submitSolution(String sqlQuery, String accessToken) {
        try {
            SubmissionRequest request = new SubmissionRequest();
            request.setFinalQuery(sqlQuery);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken);

            HttpEntity<SubmissionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    SUBMIT_WEBHOOK_URL, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Solution submitted successfully!");
                System.out.println("Response: " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("Error submitting solution: " + e.getMessage());
        }
    }
}
