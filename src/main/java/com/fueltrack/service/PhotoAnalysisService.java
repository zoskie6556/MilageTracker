package com.fueltrack.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoAnalysisService {

    private final ImageStorageService imageStorageService;
    private final AiReadingService aiReadingService;

    public PhotoAnalysisService(
            ImageStorageService imageStorageService,
            AiReadingService aiReadingService) {
        this.imageStorageService = imageStorageService;
        this.aiReadingService = aiReadingService;
    }

    /**
     * Stores a resized upload before attempting AI, retaining the photo for manual fallback.
     *
     * @param photo user-uploaded odometer photo
     * @return analysis result, or a clear fallback message and stored photo filename
     */
    public PhotoAnalysis analyze(MultipartFile photo) {
        String photoPath = imageStorageService.store(photo);
        try {
            AiReadingResult result = aiReadingService.readOdometer(imageStorageService.read(photoPath));
            return new PhotoAnalysis(photoPath, result, null);
        } catch (AiReadingException exception) {
            return new PhotoAnalysis(photoPath, null, exception.getMessage());
        }
    }

    public record PhotoAnalysis(String photoPath, AiReadingResult result, String failureMessage) {

        public boolean succeeded() {
            return result != null;
        }
    }
}
