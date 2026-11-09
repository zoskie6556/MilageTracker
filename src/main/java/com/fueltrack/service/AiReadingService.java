package com.fueltrack.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiReadingService {

    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public AiReadingService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${ANTHROPIC_API_KEY:}") String apiKey,
            @Value("${app.ai.model:claude-sonnet-5-5}") String model) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * Requests an odometer reading from Anthropic and converts miles to kilometres.
     *
     * @param jpegBytes resized image bytes
     * @return parsed reading and model confidence
     * @throws AiReadingException when AI is unconfigured, unavailable or returns invalid data
     */
    public AiReadingResult readOdometer(byte[] jpegBytes) {
        if (apiKey.isBlank()) {
            throw new AiReadingException("AI reading is not configured. Enter the mileage manually; your photo is attached.");
        }

        Map<String, Object> image = Map.of(
                "type", "image",
                "source", Map.of(
                        "type", "base64",
                        "media_type", "image/jpeg",
                        "data", Base64.getEncoder().encodeToString(jpegBytes)));
        Map<String, Object> text = Map.of(
                "type", "text",
                "text", "Read the odometer value from this photo. Return ONLY JSON with exactly these fields: "
                        + "{\"odometer_km\": number or null, \"unit\": \"km\" or \"miles\", "
                        + "\"confidence\": \"high|medium|low\", \"notes\": string}. "
                        + "Use the unit shown by the odometer. Do not include markdown or extra text.");
        Map<String, Object> request = Map.of(
                "model", model,
                "max_tokens", 256,
                "messages", List.of(Map.of("role", "user", "content", List.of(image, text))));

        String response;
        try {
            response = restClient.post()
                    .uri(ENDPOINT)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException exception) {
            throw new AiReadingException(
                    "AI could not read this photo. Enter the mileage manually; your photo is attached.",
                    exception);
        }
        return parseResponse(response);
    }

    AiReadingResult parseResponse(String response) {
        try {
            if (response == null || response.isBlank()) {
                throw invalidResponse();
            }
            JsonNode envelope = objectMapper.readTree(response);
            JsonNode content = envelope == null ? null : envelope.path("content");
            if (content == null || !content.isArray() || content.isEmpty()) {
                throw invalidResponse();
            }
            String text = content.get(0).path("text").asText(null);
            if (text == null || text.isBlank()) {
                throw invalidResponse();
            }
            String json = text.trim().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
            JsonNode reading = objectMapper.readTree(json);
            if (reading == null || !reading.isObject()) {
                throw invalidResponse();
            }
            JsonNode value = reading.get("odometer_km");
            if (value == null || value.isNull()) {
                throw new AiReadingException(
                        "No odometer value was detected. Enter the mileage manually; your photo is attached.");
            }
            if (!value.isNumber()) {
                throw invalidResponse();
            }

            String unit = reading.path("unit").asText("");
            BigDecimal amount = value.decimalValue();
            if ("miles".equalsIgnoreCase(unit)) {
                amount = amount.multiply(new BigDecimal("1.609344"));
            } else if (!"km".equalsIgnoreCase(unit)) {
                throw invalidResponse();
            }
            if (amount.signum() <= 0 || amount.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
                throw invalidResponse();
            }
            int odometerKm = amount.setScale(0, RoundingMode.HALF_UP).intValueExact();
            if (odometerKm <= 0) {
                throw invalidResponse();
            }

            String confidence = reading.path("confidence").asText("low").toLowerCase(Locale.ROOT);
            if (!List.of("high", "medium", "low").contains(confidence)) {
                confidence = "low";
            }
            String notes = reading.path("notes").asText("");
            return new AiReadingResult(odometerKm, confidence, notes);
        } catch (JsonProcessingException | ArithmeticException exception) {
            throw new AiReadingException(
                    "AI returned an unreadable result. Enter the mileage manually; your photo is attached.",
                    exception);
        }
    }

    private static AiReadingException invalidResponse() {
        return new AiReadingException(
                "AI returned an unreadable result. Enter the mileage manually; your photo is attached.");
    }
}
