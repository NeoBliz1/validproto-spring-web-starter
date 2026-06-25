package io.github.neobliz1.validproto;

import static io.github.neobliz1.validproto.TestUtil.createValidBase;
import static io.github.neobliz1.validproto.TestUtil.performValidPost;
import static io.github.neobliz1.validproto.TestUtil.runAllEndpoints;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.github.neobliz1.validproto.config.HttpValidateProtoAutoConfiguration;
import io.github.neobliz1.validproto.test.ComplexTestPayload;
import io.github.neobliz1.validproto.test.SystemStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;

@AutoConfigureWebTestClient
@Import(HttpValidateProtoAutoConfiguration.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.web-application-type=reactive")
class HttpValidateProtoIntegrationTest {

    private static final String SYNC_SINGLE_URL = "/sync/single";
    private static final String REACTIVE_MONO_URL = "/reactive/mono";
    private static final String REACTIVE_STREAM_URL = "/reactive/stream";

    @Autowired
    private WebTestClient webTestClient;

    // ==========================================
    //          SUCCESS GATE SCENARIOS
    // ==========================================

    @Test
    void shouldReturn200_whenPayloadIsValid() {
        performValidPost(webTestClient, SYNC_SINGLE_URL, createValidBase().build().toByteArray());
    }

    @Test
    void shouldReturn200_whenMonoPayloadIsValid() {
        performValidPost(webTestClient, REACTIVE_MONO_URL, createValidBase().build());
    }

    @Test
    void shouldReturn200_whenStreamPayloadIsValid() {
        performValidPost(webTestClient, REACTIVE_STREAM_URL, Flux.just(createValidBase().build()));
    }

    @Test
    void shouldReturn200_whenStandardJsonPayloadIsValid() {
        TestApplication.StandardJsonPayload validJson = new TestApplication.StandardJsonPayload();
        validJson.setName("John Doe");

        webTestClient.post().uri("/json/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validJson)
                .exchange()
                .expectStatus().isOk();
    }

    // ==========================================
    //          VALIDATION RULE MATRICES
    // ==========================================

    // 1. Strings Validations
    @Test
    void shouldReturn400_whenEmailIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().setUserEmail("bad-email"), "email");
    }

    @Test
    void shouldReturn400_whenSystemCodeIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().setSystemCode("SHORT"), "pattern");
    }

    @Test
    void shouldReturn400_whenServerIpIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().setServerIp("999.999.999.999"), "ip");
    }

    @Test
    void shouldReturn400_whenClientUuidIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().setClientUuid("not-a-uuid"), "uuid");
    }

    @Test
    void shouldReturn400_whenHomepageUrlIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().setHomepageUrl("not_a_uri"), "uri");
    }

    // 2. Numeric Validations
    @Test
    void shouldReturn400_whenLatitudeIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().setLatitude(120.0), "latitude");
    }

    @Test
    void shouldReturn400_whenLongitudeIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().setLongitude(-200.0), "longitude");
    }

    @Test
    void shouldReturn400_whenPriorityLevelIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().setPriorityLevel(4), "priority_level");
    }

    // 3. Binary Validation
    @Test
    void shouldReturn400_whenSignatureLengthIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().setEncryptedSignature(ByteString.copyFrom(new byte[]{ 1, 2, 3 })), "signature");
    }

    // 4. Boolean Validation
    @Test
    void shouldReturn400_whenComplianceConstIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().setComplianceAccepted(false), "compliance_accepted");
    }

    // 5. Enum Validation
    @Test
    void shouldReturn400_whenEnumFallbackIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().setStatus(SystemStatus.STATUS_UNSPECIFIED), "status");
    }

    // 6. Collection Validations
    @Test
    void shouldReturn400_whenRepeatedMinItemsIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().clearSecurityRoles(), "security_roles");
    }

    @Test
    void shouldReturn400_whenRepeatedMaxItemsIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().addAllSecurityRoles(List.of(2001, 2002, 2003, 2004, 2005, 2006)), "security_roles");
    }

    @Test
    void shouldReturn400_whenRepeatedUniquenessIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().addAllSecurityRoles(List.of(2001, 2001)), "security_roles");
    }

    @Test
    void shouldReturn400_whenRepeatedItemsValueIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().addSecurityRoles(5), "security_roles");
    }

    // 7. Map Validations
    @Test
    void shouldReturn400_whenMapMinPairsIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().clearMetadataTags(), "metadata_tags");
    }

    @Test
    void shouldReturn400_whenMapKeysPatternIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().putMetadataTags("INVALID_KEY_UPPERCASE", "val"), "metadata_tags");
    }

    @Test
    void shouldReturn400_whenMapValuesLengthIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().putMetadataTags("tag", ""), "metadata_tags");
    }

    // 8. Well-Known Types (WKT) Validations
    @Test
    void shouldReturn400_whenExecutionTimeIsMissing() {
        runAllEndpoints(webTestClient, createValidBase().clearExecutionTime(), "execution_time");
    }

    @Test
    void shouldReturn400_whenExecutionTimeIsPast() {
        Timestamp past = Timestamp.newBuilder().setSeconds(100).build();
        runAllEndpoints(webTestClient, createValidBase().setExecutionTime(past), "execution_time");
    }

    @Test
    void shouldReturn400_whenProcessingTimeoutIsMissing() {
        runAllEndpoints(webTestClient, createValidBase().clearProcessingTimeout(), "processing_timeout");
    }

    @Test
    void shouldReturn400_whenProcessingTimeoutIsTooLong() {
        Duration longTimeout = Duration.newBuilder().setSeconds(500).build();
        runAllEndpoints(webTestClient, createValidBase().setProcessingTimeout(longTimeout), "processing_timeout");
    }

    // 9. Wrapper Object Validation
    @Test
    void shouldReturn400_whenWrapperValueCelIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().setNullableDescription(StringValue.newBuilder().setValue("tiny").build()), "nullable_description");
    }

    // 10. Oneof Validations
    @Test
    void shouldReturn400_whenOneofAuthenticationIsMissing() {
        runAllEndpoints(webTestClient, createValidBase().clearAuthenticationPayload(), "authentication_payload");
    }

    @Test
    void shouldReturn400_whenOneofApiTokenIsTooShort() {
        runAllEndpoints(webTestClient, createValidBase().setApiToken("short-token"), "api_token");
    }

    @Test
    void shouldReturn400_whenOneofOauthIdIsInvalid() {
        runAllEndpoints(webTestClient, createValidBase().setOauthId(-5), "oauth_id");
    }

    // 11. Cross-Field CEL Rule Validations
    @Test
    void shouldReturn400_whenCelWindowIsEqual() {
        ComplexTestPayload.ProcessingWindow win = ComplexTestPayload.ProcessingWindow.newBuilder().setStartHour(12).setEndHour(12).build();
        runAllEndpoints(webTestClient, createValidBase().setWindow(win), "window");
    }

    @Test
    void shouldReturn400_whenCelWindowIsInverted() {
        ComplexTestPayload.ProcessingWindow win = ComplexTestPayload.ProcessingWindow.newBuilder().setStartHour(15).setEndHour(10).build();
        runAllEndpoints(webTestClient, createValidBase().setWindow(win), "window");
    }

    @Test
    void shouldReturn400_whenStandardJsonPayloadIsInvalid() {
        TestApplication.StandardJsonPayload invalidJson = new TestApplication.StandardJsonPayload();
        invalidJson.setName(""); // Invalid: breaks @NotBlank

        webTestClient.post().uri("/json/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.violations").value(violations -> {
                    String str = violations.toString();
                    assertTrue(
                            str.contains("blank"),
                            "Expected standard JSON validation to trigger! Got: " + str
                    );
                });
    }
}
