package io.github.neobliz1.validproto;

import io.github.neobliz1.validproto.annotation.ValidProto;
import io.github.neobliz1.validproto.annotation.ValidatedProto;
import io.github.neobliz1.validproto.test.ComplexTestPayload;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ValidatedProto
@RestController
@SpringBootApplication(scanBasePackages = "io.github.neobliz1.validproto")
public class TestApplication {

    @PostMapping(value = "/reactive/mono", consumes = MediaType.APPLICATION_PROTOBUF_VALUE)
    public Mono<String> handleReactiveMono(@ValidProto @RequestBody Mono<ComplexTestPayload> payloadMono) {
        return payloadMono.map(payload -> "SUCCESS: " + payload.getPriorityLevel());
    }

    @PostMapping(value = "/reactive/stream", consumes = MediaType.APPLICATION_PROTOBUF_VALUE)
    public Flux<String> handleStream(@ValidProto @RequestBody Flux<ComplexTestPayload> payloads) {
        return payloads.map(payload -> "Processed: " + payload.getPriorityLevel());
    }

    @PostMapping(value = "/sync/single", consumes = MediaType.APPLICATION_PROTOBUF_VALUE)
    public String handleSyncSingle(@ValidProto @RequestBody ComplexTestPayload payload) {
        return "SUCCESS: " + payload.getPriorityLevel();
    }

    @ExceptionHandler(MethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleSyncValidationException(MethodValidationException ex) {
        List<String> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(org.springframework.context.MessageSourceResolvable::getDefaultMessage)
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("violations", errors);

        return ResponseEntity.badRequest().body(body);
    }
}
