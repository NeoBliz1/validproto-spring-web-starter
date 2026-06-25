package io.github.neobliz1.validproto.infra;

import static java.util.Objects.requireNonNull;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Violation;
import com.google.protobuf.Message;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A unified validation adapter that implements both {@link Validator} and {@link ExecutableValidator} contracts natively,
 * eliminating brittle anonymous class allocations exactly like Hibernate's internal engine.
 * <p>
 * This adapter bridges the gap between the protovalidate SDK (from buf.build) and the Jakarta Validation API,
 * allowing Protobuf messages to be validated using Spring's built-in validation mechanisms.
 * </p>
 * <p>
 * The adapter supports:
 * <ul>
 *   <li>Parameter validation for method arguments annotated with {@code @ValidProto}</li>
 *   <li>Return value validation for methods with return type annotated with {@code @ValidProto}</li>
 *   <li>Constructor parameter validation for Protobuf builders</li>
 *   <li>Full compatibility with Spring WebFlux controllers</li>
 * </ul>
 * </p>
 * <p>
 * Usage example:
 * </p>
 * <pre>{@code
 * @ValidatedProto
 * @RestController
 * public class MyController {
 *     
 *     @PostMapping("/api/validate")
 *     public ResponseEntity<String> validate(
 *         @ValidProto @RequestBody MyProtoMessage message) {
 *         // message will be automatically validated using this adapter
 *         return ResponseEntity.ok("Validated");
 *     }
 * }
 * }</pre>
 * 
 * @see Validator
 * @see ExecutableValidator
 * @see ProtobufConstraintViolation
 * @see ProtobufPath
 * @since 0.0.1
 */
public class ProtobufValidatorAdapter implements Validator, ExecutableValidator {

    private final build.buf.protovalidate.Validator protovalidateEngine;

    /**
     * Creates a new {@link ProtobufValidatorAdapter} instance with the specified protovalidate engine.
     * 
     * @param protovalidateEngine the protovalidate engine to use for validation; must not be null
     * @throws NullPointerException if protovalidateEngine is null
     */
    public ProtobufValidatorAdapter(build.buf.protovalidate.Validator protovalidateEngine) {
        this.protovalidateEngine = requireNonNull(protovalidateEngine);
    }
    
    /**
     * Validates the given object for constraint violations.
     * <p>
     * This method validates Protobuf messages using the protovalidate engine.
     * If the object is not a Protobuf message, an empty set of violations is returned.
     * </p>
     * 
     * @param object the object to validate; must be a Protobuf Message instance
     * @param groups the group or groups to validate
     * @return a set of constraint violations; empty if no violations or object is not a message
     * @throws NullPointerException if object is null
     * @see Message
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = new HashSet<>();
        if (object instanceof Message message) {
            try {
                ValidationResult result = protovalidateEngine.validate(message);
                if (!result.getViolations().isEmpty()) {
                    for (Violation violation : result.getViolations()) {
                        violations.add(new ProtobufConstraintViolation<>(object, violation));
                    }
                }
            } catch (Exception e) {
                violations.add(new ProtobufConstraintViolation<>(object, e.getMessage()));
            }
        }
        return violations;
    }

    /**
     * Returns the executable validator for validating methods and constructors.
     * <p>
     * Since this class implements both {@link Validator} and {@link ExecutableValidator},
     * it returns {@code this} instance directly, eliminating the need for anonymous class allocations.
     * </p>
     * 
     * @return this instance as an ExecutableValidator
     */
    @Override
    public ExecutableValidator forExecutables() {
        // Return this instance directly because we implement ExecutableValidator natively!
        return this;
    }
    
    /**
     * Validates the given object's method parameters for constraint violations.
     * <p>
     * This method iterates through the parameter values and validates any Protobuf
     * message instances using the protovalidate engine.
     * </p>
     * 
     * @param object the object whose method parameters are being validated
     * @param method the method being validated (used for context only)
     * @param parameterValues the values of the method parameters
     * @param groups the group or groups to validate
     * @return a set of constraint violations for invalid parameters
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validateParameters(
            T object, Method method, Object[] parameterValues, Class<?>... groups) {

        Set<ConstraintViolation<T>> violations = new HashSet<>();
        for (Object value : parameterValues) {
            if (value instanceof Message) {
                validateAndAddViolations(groups, value, violations);
            }
        }
        return violations;
    }

    /**
     * Validates a Protobuf message and adds any violations to the provided set.
     * <p>
     * This helper method delegates to the main {@link #validate} method and
     * adds the resulting violations to the specified set.
     * </p>
     * 
     * @param groups the group or groups to validate
     * @param value the Protobuf message to validate
     * @param violations the set to which violations will be added
     */
    private <T> void validateAndAddViolations(Class<?>[] groups, Object value, Set<ConstraintViolation<T>> violations) {
        Set<ConstraintViolation<Object>> messageViolations = validate(value, groups);
        for (ConstraintViolation<Object> violation : messageViolations) {
            @SuppressWarnings("unchecked")
            ConstraintViolation<T> adaptedViolation = (ConstraintViolation<T>) violation;
            violations.add(adaptedViolation);
        }
    }

    /**
     * Validates the given constructor's parameters for constraint violations.
     * <p>
     * This method validates Protobuf message parameters that may be used during
     * constructor invocation. Since Protobuf messages are typically built using
     * the Builder pattern, this method delegates to {@link #validateParameters}.
     * </p>
     * 
     * @param constructor the constructor being validated
     * @param parameterValues the values of the constructor parameters
     * @param groups the group or groups to validate
     * @return a set of constraint violations for invalid parameters
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validateConstructorParameters(
            Constructor<? extends T> constructor, Object[] parameterValues, Class<?>... groups) {

        // Constructors can initialize nested fields carrying inner sub-messages
        return validateParameters(null, null, parameterValues, groups);
    }

    /**
     * Validates the given object's method return value for constraint violations.
     * <p>
     * This method validates the return value if it is a Protobuf message.
     * Null return values are not validated and an empty set is returned.
     * </p>
     * 
     * @param object the object whose method is being validated
     * @param method the method whose return value is being validated
     * @param returnValue the return value to validate
     * @param groups the group or groups to validate
     * @return a set of constraint violations; empty if return value is null or not a message
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validateReturnValue(
            T object, Method method, Object returnValue, Class<?>... groups) {

        if (returnValue == null) {
            return Collections.emptySet();
        }

        Set<ConstraintViolation<T>> violations = new HashSet<>();
        validateAndAddViolations(groups, returnValue, violations);

        return violations;
    }

    /**
     * Validates the given constructor's created object for constraint violations.
     * <p>
     * This method validates the state of an object after it has been constructed.
     * For Protobuf messages, this validates the final built message.
     * </p>
     * 
     * @param constructor the constructor that created the object
     * @param createdObject the object created by the constructor
     * @param groups the group or groups to validate
     * @return a set of constraint violations for the created object
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validateConstructorReturnValue(
            Constructor<? extends T> constructor, T createdObject, Class<?>... groups) {

        // Handles post-construction state validations seamlessly
        return validate(createdObject, groups);
    }

    /**
     * Validates a specific property of the given object.
     * <p>
     * This method is not fully implemented for Protobuf validation, as property-level
     * validation is typically handled by the protovalidate engine during message-level validation.
     * Returns an empty set of violations.
     * </p>
     * 
     * @param object the object whose property is being validated
     * @param propertyName the name of the property to validate
     * @param groups the group or groups to validate
     * @return an empty set of violations (not implemented for Protobuf)
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
        return Collections.emptySet();
    }

    /**
     * Validates a value for the specified property of the given bean type.
     * <p>
     * This method is not fully implemented for Protobuf validation, as property-level
     * validation is typically handled by the protovalidate engine during message-level validation.
     * Returns an empty set of violations.
     * </p>
     * 
     * @param beanType the type of the bean containing the property
     * @param propertyName the name of the property to validate
     * @param value the value to validate for the property
     * @param groups the group or groups to validate
     * @return an empty set of violations (not implemented for Protobuf)
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
        return Collections.emptySet();
    }

    /**
     * Returns a descriptor for the constraints of the specified class.
     * <p>
     * This method returns null as Protobuf message constraints are defined
     * in the .proto files rather than as Java bean constraints.
     * </p>
     * 
     * @param clazz the class to get constraints for
     * @return null (Protobuf constraints are in .proto files)
     */
    @Override
    public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
        return null;
    }

    /**
     * Unwraps this validator to the specified type.
     * <p>
     * This method allows retrieval of the underlying validator instance
     * if the requested type matches this class.
     * </p>
     * 
     * @param type the type to unwrap to
     * @return this instance cast to the specified type, or null if types are incompatible
     */
    @Override
    public <T> T unwrap(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        return null;
    }
}
