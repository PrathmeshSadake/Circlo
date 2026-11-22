package me.prathmeshsadake.linkedin.uploaderService.controller;

import lombok.RequiredArgsConstructor;
import me.prathmeshsadake.linkedin.uploaderService.dto.UploadResponse;
import me.prathmeshsadake.linkedin.uploaderService.service.ImageStorageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class UploadController {

    private final ImageStorageService imageStorageService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse uploadImage(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "post") String usage,
            @RequestPart("file") MultipartFile file) {
        return new UploadResponse(imageStorageService.upload(userId, usage, file));
    }
}
