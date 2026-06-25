package io.github.neobliz1.validproto;

import io.github.neobliz1.validproto.annotation.ValidProto;
import io.github.neobliz1.validproto.annotation.ValidatedProto;
import io.github.neobliz1.validproto.test.ComplexTestPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Validated
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

    @PostMapping(value = "/json/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String handleStandardJson(@Valid @RequestBody StandardJsonPayload payload) {
        return "JSON SUCCESS: " + payload.getName();
    }

    @ExceptionHandler({MethodValidationException.class, WebExchangeBindException.class})
    public ResponseEntity<Map<String, Object>> handleAllValidationExceptions(Exception ex) {
        List<String> errors;

        if (ex instanceof MethodValidationException mve) {
            // Unpacks errors coming from your Buf engine
            errors = mve.getParameterValidationResults().stream()
                    .flatMap(result -> result.getResolvableErrors().stream())
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .toList();
        } else if (ex instanceof WebExchangeBindException wbe) {
            // Unpacks errors coming from the default JSON engine
            errors = wbe.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .toList();
        } else {
            errors = List.of(ex.getMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("violations", errors);

        return ResponseEntity.badRequest().body(body);
    }

    public static class StandardJsonPayload {
        @NotBlank(message = "JSON name must not be blank")
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
