package io.github.neobliz1.validproto.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation placed on the {@link org.springframework.web.bind.annotation.RestController} class level to enable
 * Protobuf Method/Parameter validation scanning.
 * <p>
 * <strong>Important:</strong> This annotation <em>must</em> be used together with {@link ValidProto} on method parameters.
 * {@link ValidatedProto} enables the validation mechanism at the controller level, while {@link ValidProto}
 * marks specific parameters that should be validated. <strong>Both annotations are required</strong> for validation to work.
 * </p>
 * <p>
 * Without {@code @ValidatedProto} on the controller class, {@code @ValidProto} on parameters has <strong>no effect</strong>.
 * Without {@code @ValidProto} on parameters, {@code @ValidatedProto} alone does not trigger validation on any parameters.
 * </p>
 * <p>
 * <strong>Required usage pattern:</strong>
 * <ul>
 *   <li>{@code @ValidatedProto} on the controller class - enables the validation mechanism</li>
 *   <li>{@code @ValidProto} on method parameters with Protobuf messages - marks for validation</li>
 * </ul>
 * </p>
 * <p>
 * Example usage (both annotations required):
 * </p>
 * <pre>{@code
 * @ValidatedProto  // REQUIRED: Enable validation on this controller
 * @RestController
 * public class MyController {
 *     
 *     @PostMapping("/api/validate")
 *     public ResponseEntity<String> validate(
 *         @ValidProto @RequestBody MyProtoMessage message) {  // REQUIRED: Validate this parameter
 *         // message will be automatically validated
 *         return ResponseEntity.ok("Validated");
 *     }
 * }
 * }</pre>
 * 
 * @see ValidProto
 * @see io.github.neobliz1.validproto.infra.ProtobufValidatorAdapter
 * @since 0.0.1
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatedProto {
}
