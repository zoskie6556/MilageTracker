package com.fueltrack;

import com.fueltrack.service.ImageStorageService;
import com.fueltrack.service.ImageUploadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageStorageServiceTest {

    @TempDir
    Path uploadDirectory;

    @Test
    void sniffsAndResizesImageBeforeStoringWithGeneratedName() throws Exception {
        BufferedImage source = new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(source, "png", bytes);
        ImageStorageService service = new ImageStorageService(uploadDirectory.toString());

        String filename = service.store(new MockMultipartFile("photo", "wrong-extension.txt",
                "text/plain", bytes.toByteArray()));

        BufferedImage stored = ImageIO.read(uploadDirectory.resolve(filename).toFile());
        assertThat(filename).matches("[0-9a-f-]{36}\\.jpg");
        assertThat(stored.getWidth()).isEqualTo(1600);
        assertThat(stored.getHeight()).isEqualTo(800);
    }

    @Test
    void rejectsUnsupportedContentEvenWhenFileClaimsToBeAnImage() {
        ImageStorageService service = new ImageStorageService(uploadDirectory.toString());
        MockMultipartFile fake = new MockMultipartFile("photo", "photo.jpg", "image/jpeg",
                "not really a photo".getBytes());

        assertThatThrownBy(() -> service.store(fake))
                .isInstanceOf(ImageUploadException.class)
                .hasMessageContaining("Only JPEG, PNG and WebP");
    }
}
