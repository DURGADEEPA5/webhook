package com.pes.bajaj;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class BajajFinservHealthApplication implements CommandLineRunner {

    // API endpoint from PDF
    private static final String GENERATE_WEBHOOK_API =
            "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

    @Value("${user.name}")
    private String name;

    @Value("${user.regno}")
    private String regNo;

    @Value("${user.email}")
    private String email;

    @Value("${user.finalQuery}")
    private String finalSqlQuery;

    public static void main(String[] args) {
        SpringApplication.run(BajajFinservHealthApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        // Step 1: Send POST request to generate webhook
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("regNo", regNo);
        body.put("email", email);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        System.out.println("➡️ Sending POST request to generate webhook...");

        ResponseEntity<String> response = restTemplate.exchange(
                GENERATE_WEBHOOK_API, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            System.err.println("❌ Failed to generate webhook! Status: " + response.getStatusCode());
            System.err.println("Response: " + response.getBody());
            return;
        }

        JsonNode json = mapper.readTree(response.getBody());
        String webhookUrl = json.path("webhook").asText();
        String accessToken = json.path("accessToken").asText();

        System.out.println("✅ Webhook URL received: " + webhookUrl);
        System.out.println("✅ Access Token received: " + accessToken);

        // Step 2: Submit SQL query to webhook using JWT token
        Map<String, String> sqlBody = new HashMap<>();
        sqlBody.put("finalQuery", finalSqlQuery);

        HttpHeaders sqlHeaders = new HttpHeaders();
        sqlHeaders.setContentType(MediaType.APPLICATION_JSON);
        sqlHeaders.set("Authorization", accessToken);

        HttpEntity<Map<String, String>> sqlRequest = new HttpEntity<>(sqlBody, sqlHeaders);

        System.out.println("📤 Submitting SQL query to webhook...");

        ResponseEntity<String> sqlResponse = restTemplate.exchange(
                webhookUrl, HttpMethod.POST, sqlRequest, String.class);

        System.out.println("✅ Submission Response: " + sqlResponse.getBody());
    }
}
