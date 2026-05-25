package dev.iamrat.board.file.infrastructure.storage;

import dev.iamrat.board.file.application.FileStorage;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class S3FileStorageAdapter implements FileStorage {

    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(5);

    private final S3Properties s3Properties;
    private final S3Presigner s3Presigner;

    @Override
    public String createUploadUrl(String objectKey, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
            .bucket(s3Properties.bucket())
            .key(objectKey)
            .contentType(contentType)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(PRESIGNED_URL_TTL)
            .putObjectRequest(objectRequest)
            .build();

        return s3Presigner.presignPutObject(presignRequest)
            .url()
            .toString();
    }

    @Override
    public String createDownloadUrl(String objectKey) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
            .bucket(s3Properties.bucket())
            .key(objectKey)
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(PRESIGNED_URL_TTL)
            .getObjectRequest(objectRequest)
            .build();

        return s3Presigner.presignGetObject(presignRequest)
            .url()
            .toString();
    }
}
