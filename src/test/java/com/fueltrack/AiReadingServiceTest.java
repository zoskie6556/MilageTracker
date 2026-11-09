package com.fueltrack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fueltrack.service.AiReadingException;
import com.fueltrack.service.AiReadingResult;
import com.fueltrack.service.AiReadingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class AiReadingServiceTest {

    private MockRestServiceServer server;
    private AiReadingService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new AiReadingService(builder, new ObjectMapper(), "test-key", "test-model");
    }

    @Test
    void parsesValidKilometreResponseAndSendsAnthropicHeaders() {
        expectText("{\"odometer_km\":12345,\"unit\":\"km\",\"confidence\":\"high\",\"notes\":\"clear\"}");

        AiReadingResult result = service.readOdometer(new byte[]{1, 2, 3});

        assertThat(result).isEqualTo(new AiReadingResult(12345, "high", "clear"));
        server.verify();
    }

    @Test
    void parsesFencedJsonAndConvertsMilesToKilometres() {
        expectText("```json\n{\"odometer_km\":100,\"unit\":\"miles\",\"confidence\":\"medium\",\"notes\":\"\"}\n```");

        assertThat(service.readOdometer(new byte[]{1}).odometerKm()).isEqualTo(161);
        server.verify();
    }

    @Test
    void malformedJsonFailsClearly() {
        expectText("not json");

        assertThatThrownBy(() -> service.readOdometer(new byte[]{1}))
                .isInstanceOf(AiReadingException.class)
                .hasMessageContaining("Enter the mileage manually");
        server.verify();
    }

    @Test
    void nullOdometerValueRequestsManualEntry() {
        expectText("{\"odometer_km\":null,\"unit\":\"km\",\"confidence\":\"low\",\"notes\":\"not visible\"}");

        assertThatThrownBy(() -> service.readOdometer(new byte[]{1}))
                .isInstanceOf(AiReadingException.class)
                .hasMessageContaining("No odometer value was detected");
        server.verify();
    }

    @Test
    void nullJsonPayloadIsReportedAsManualFallback() {
        expectText("null");

        assertThatThrownBy(() -> service.readOdometer(new byte[]{1}))
                .isInstanceOf(AiReadingException.class)
                .hasMessageContaining("Enter the mileage manually");
        server.verify();
    }

    @Test
    void rateLimitIsReportedAsManualFallback() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(POST))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> service.readOdometer(new byte[]{1}))
                .isInstanceOf(AiReadingException.class)
                .hasMessageContaining("photo is attached");
        server.verify();
    }

    private void expectText(String json) {
        String envelope = """
                {"content":[{"type":"text","text":%s}]}
                """.formatted(new ObjectMapper().valueToTree(json).toString());
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(POST))
                .andExpect(header("x-api-key", "test-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andRespond(withSuccess(envelope, MediaType.APPLICATION_JSON));
    }
}
