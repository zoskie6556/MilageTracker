package com.fueltrack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fueltrack.service.AiReadingException;
import com.fueltrack.service.AiReadingService;
import com.fueltrack.service.ImageStorageService;
import com.fueltrack.service.PhotoAnalysisService;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoAnalysisServiceTest {

    @TempDir
    Path uploadDirectory;

    @Test
    void aiFailureRetainsStoredPhotoForManualEntry() throws Exception {
        ImageStorageService imageStorageService = new ImageStorageService(uploadDirectory.toString());
        AiReadingService unavailableAi = new AiReadingService(
                RestClient.builder(), new ObjectMapper(), "test-key", "test-model") {
            @Override
            public com.fueltrack.service.AiReadingResult readOdometer(byte[] jpegBytes) {
                throw new AiReadingException("AI timed out. Enter the mileage manually; your photo is attached.");
            }
        };
        PhotoAnalysisService photoAnalysisService = new PhotoAnalysisService(imageStorageService, unavailableAi);
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        MockMultipartFile upload = new MockMultipartFile("photo", "dash.png", "image/png", bytes.toByteArray());

        PhotoAnalysisService.PhotoAnalysis result = photoAnalysisService.analyze(upload);

        assertThat(result.succeeded()).isFalse();
        assertThat(imageStorageService.load(result.photoPath())).isNotEmpty();
        assertThat(result.failureMessage()).contains("manually");
    }
}
