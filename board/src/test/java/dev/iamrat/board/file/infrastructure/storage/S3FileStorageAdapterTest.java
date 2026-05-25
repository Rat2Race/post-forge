package dev.iamrat.board.file.infrastructure.storage;

import java.net.MalformedURLException;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class S3FileStorageAdapterTest {

    @Mock
    private S3Properties s3Properties;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private S3FileStorageAdapter storageAdapter;

    @Test
    @DisplayName("S3 업로드 Presigned URL을 생성한다")
    void createUploadUrl_returnsPresignedPutUrl() throws MalformedURLException {
        given(s3Properties.bucket()).willReturn("postforge-uploads");
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        given(presignedRequest.url()).willReturn(URI.create("https://s3.example.com/upload").toURL());
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedRequest);

        String url = storageAdapter.createUploadUrl("images/file.png", "image/png");

        assertThat(url).isEqualTo("https://s3.example.com/upload");
    }

    @Test
    @DisplayName("S3 다운로드 Presigned URL을 생성한다")
    void createDownloadUrl_returnsPresignedGetUrl() throws MalformedURLException {
        given(s3Properties.bucket()).willReturn("postforge-uploads");
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        given(presignedRequest.url()).willReturn(URI.create("https://s3.example.com/download").toURL());
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(presignedRequest);

        String url = storageAdapter.createDownloadUrl("images/file.png");

        assertThat(url).isEqualTo("https://s3.example.com/download");
    }
}
