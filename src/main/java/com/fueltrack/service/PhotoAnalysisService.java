package com.fueltrack.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoAnalysisService {

    private final ImageStorageService imageStorageService;
    private final AiReadingService aiReadingService;
    private final AiRateLimiter rateLimiter;

    public PhotoAnalysisService(
            ImageStorageService imageStorageService,
            AiReadingService aiReadingService,
            AiRateLimiter rateLimiter) {
        this.imageStorageService = imageStorageService;
        this.aiReadingService = aiReadingService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Stores a resized upload before attempting AI, retaining the photo for manual fallback.
     *
     * @param photo user-uploaded odometer photo
     * @param userId authenticated account identifier
     * @return analysis result, or a clear fallback message and stored photo filename
     */
    public PhotoAnalysis analyze(MultipartFile photo, Long userId) {
        String photoPath = imageStorageService.store(photo);
        if (!rateLimiter.allowCall(userId)) {
            return new PhotoAnalysis(photoPath, null,
                    "AI reading limit reached. Enter the mileage manually; your photo is attached.");
        }
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
