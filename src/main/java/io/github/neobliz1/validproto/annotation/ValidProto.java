package io.github.neobliz1.validproto.annotation;

import jakarta.validation.Valid;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation placed directly on method arguments to trigger the validation
 * engine on that specific Protobuf payload.
 * <p>
 * <strong>Important:</strong> This annotation <em>must</em> be used together with {@link ValidatedProto}
 * on the controller class. {@code @ValidProto} alone on method parameters has <strong>no effect</strong>
 * without {@code @ValidatedProto} enabled at the class level.
 * </p>
 * <p>
 * When both annotations are used together, validation is enabled for the marked parameter:
 * <ul>
 *   <li>{@code @ValidatedProto} on the controller class - enables the validation mechanism</li>
 *   <li>{@code @ValidProto} on method parameters - marks specific Protobuf payloads for validation</li>
 * </ul>
 * </p>
 * <p>
 * Example usage (both annotations required):
 * </p>
 * <pre>{@code
 * @ValidatedProto  // REQUIRED: Enables validation on this controller
 * @RestController
 * public class MyController {
 *     
 *     @PostMapping("/api/validate")
 *     public ResponseEntity<String> validate(
 *         @ValidProto @RequestBody MyProtoMessage message) {
 *         // message will be automatically validated
 *         return ResponseEntity.ok("Validated");
 *     }
 * }
 * }</pre>
 * 
 * @see ValidatedProto
 * @see io.github.neobliz1.validproto.infra.ProtobufValidatorAdapter
 * @since 0.0.1
 */
@Valid
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidProto {
}
