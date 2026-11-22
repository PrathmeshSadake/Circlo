package me.prathmeshsadake.linkedin.uploaderService.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.prathmeshsadake.linkedin.uploaderService.client.UserServiceClient;
import me.prathmeshsadake.linkedin.uploaderService.dto.ProfileImageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final int PART_SIZE = 5 * 1024 * 1024;

    private final S3Client s3Client;
    private final UserServiceClient userServiceClient;

    @Value("${storage.s3.bucket}")
    private String bucket;

    @Value("${storage.s3.public-url}")
    private String publicUrl;

    @Value("${internal.token}")
    private String internalToken;

    @PostConstruct
    void ensureBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception ex) {
            if (ex.statusCode() != 404) {
                throw ex;
            }
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created bucket {}", bucket);
        }
    }

    public String upload(Long userId, String usage, MultipartFile file) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing authenticated user");
        }
        String normalizedUsage = usage == null ? "post" : usage.toLowerCase(Locale.ROOT);
        if (!normalizedUsage.equals("profile") && !normalizedUsage.equals("post")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "usage must be profile or post");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload");
        }
        if (bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        String key = normalizedUsage + "/" + userId + "/" + UUID.randomUUID() + extension(contentType);
        String url = multipartUpload(key, contentType, bytes);
        if ("profile".equals(normalizedUsage)) {
            userServiceClient.updateProfileImage(userId, new ProfileImageRequest(url), internalToken);
        }
        return url;
    }

    private String multipartUpload(String key, String contentType, byte[] bytes) {
        String uploadId = s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build()).uploadId();
        try {
            List<CompletedPart> parts = new ArrayList<>();
            int partNumber = 1;
            for (int offset = 0; offset < bytes.length; offset += PART_SIZE) {
                int length = Math.min(PART_SIZE, bytes.length - offset);
                byte[] slice = new byte[length];
                System.arraycopy(bytes, offset, slice, 0, length);
                String eTag = s3Client.uploadPart(UploadPartRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .uploadId(uploadId)
                                .partNumber(partNumber)
                                .contentLength((long) length)
                                .build(),
                        RequestBody.fromBytes(slice)).eTag();
                parts.add(CompletedPart.builder().partNumber(partNumber).eTag(eTag).build());
                partNumber++;
            }
            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                    .build());
            return publicUrl + "/" + bucket + "/" + key;
        } catch (RuntimeException ex) {
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .build());
            throw ex;
        }
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
