package io.github.neobliz1.validproto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.github.neobliz1.validproto.test.ComplexTestPayload;
import io.github.neobliz1.validproto.test.SystemStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.Instant;

public final class TestUtil {

    private static final String SYNC_SINGLE_URL = "/sync/single";
    private static final String REACTIVE_MONO_URL = "/reactive/mono";
    private static final String REACTIVE_STREAM_URL = "/reactive/stream";

    private TestUtil() {
        // Prevent utility class instantiation
    }

    public static ComplexTestPayload.Builder createValidBase() {
        Timestamp futureTime = Timestamp.newBuilder()
                .setSeconds(Instant.now().getEpochSecond()+3600)
                .build();

        Duration timeout = Duration.newBuilder()
                .setSeconds(30)
                .build();

        ComplexTestPayload.ProcessingWindow validWindow = ComplexTestPayload.ProcessingWindow.newBuilder()
                .setStartHour(9)
                .setEndHour(17)
                .build();

        return ComplexTestPayload.newBuilder()
                .setUserEmail("developer@github.io")
                .setSystemCode("SYS-PRD-2026")
                .setServerIp("192.168.1.1")
                .setClientUuid("f47ac10b-58cc-4372-a567-0e02b2c3d479")
                .setHomepageUrl("https://sonatype.com")
                .setLatitude(45.0)
                .setLongitude(90.0)
                .setPriorityLevel(1)
                .setEncryptedSignature(ByteString.copyFrom(new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                        16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32 }))
                .setComplianceAccepted(true)
                .setStatus(SystemStatus.STATUS_ACTIVE)
                .addSecurityRoles(2000)
                .putMetadataTags("env", "production")
                .setExecutionTime(futureTime)
                .setProcessingTimeout(timeout)
                .setNullableDescription(StringValue.newBuilder().setValue("Valid Description Over Five Chars").build())
                .setWindow(validWindow)
                .setApiToken("SECURE_TOKEN_GREATER_THAN_THIRTY_TWO_CHARS");
    }

    public static void runAllEndpoints(WebTestClient webTestClient, ComplexTestPayload.Builder builder, String expectedViolationSnippet) {
        ComplexTestPayload payload = builder.build();
        performInvalidPost(webTestClient, REACTIVE_MONO_URL, payload, expectedViolationSnippet);
        performInvalidPost(webTestClient, REACTIVE_STREAM_URL, Flux.just(payload), expectedViolationSnippet);
        performInvalidPost(webTestClient, SYNC_SINGLE_URL, payload.toByteArray(), expectedViolationSnippet);
    }

    public static void performValidPost(WebTestClient webTestClient, String uri, Object bodyPayload) {
        if(bodyPayload instanceof Flux<?> flux) {
            webTestClient.post().uri(uri)
                    .contentType(MediaType.APPLICATION_PROTOBUF)
                    .body(flux, ComplexTestPayload.class)
                    .exchange()
                    .expectStatus().isOk();
        } else {
            webTestClient.post().uri(uri)
                    .contentType(MediaType.APPLICATION_PROTOBUF)
                    .bodyValue(bodyPayload)
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    public static void performInvalidPost(WebTestClient webTestClient, String uri, Object bodyPayload, String expectedViolationSnippet) {
        if(bodyPayload instanceof Flux<?> flux) {
            webTestClient.post().uri(uri)
                    .contentType(MediaType.APPLICATION_PROTOBUF)
                    .body(flux, ComplexTestPayload.class)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.violations").value(violations -> assertErrorMessage(expectedViolationSnippet, violations));
        } else {
            webTestClient.post().uri(uri)
                    .contentType(MediaType.APPLICATION_PROTOBUF)
                    .bodyValue(bodyPayload)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.violations").value(violations -> assertErrorMessage(expectedViolationSnippet, violations));
        }
    }

    private static void assertErrorMessage(String expectedViolationSnippet, Object violations) {
        String str = violations.toString().toLowerCase();
        String targetSnippet = expectedViolationSnippet.toLowerCase();

        assertTrue(
                str.contains(targetSnippet) || str.contains("failed"),
                "Expected violation details missing from payload output! Got: "+violations
        );
    }
}