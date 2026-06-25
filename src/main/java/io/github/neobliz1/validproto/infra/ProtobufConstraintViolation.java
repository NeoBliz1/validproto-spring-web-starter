package io.github.neobliz1.validproto.infra;

import static java.lang.reflect.Proxy.newProxyInstance;

import build.buf.validate.Violation;
import io.github.neobliz1.validproto.annotation.ValidatedProto;
import jakarta.validation.ConstraintTarget;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.metadata.ValidateUnwrappedValue;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link ConstraintViolation} that adapts protovalidate violations
 * to the Jakarta Validation API format.
 * <p>
 * This class wraps violations from the protovalidate library and converts them into
 * standard Jakarta Validation API constraint violations, making them compatible with
 * Spring's validation framework.
 * </p>
 * <p>
 * The adapter extracts field names from protovalidate's FieldPath format and formats
 * error messages in a consistent format: {@code "field_name: error_message"}.
 * </p>
 * <p>
 * Example violation message:
 * </p>
 * <pre>{@code
 * "user_email: must be a well-formed email address"
 * "priority_level: must be one of [1, 3, 5]
 * }</pre>
 * 
 * @param <T> the type of the root bean being validated
 * @see ConstraintViolation
 * @see ProtobufPath
 * @since 0.0.1
 */
public class ProtobufConstraintViolation<T> implements ConstraintViolation<T> {

    public static final String PROTOBUF_ENGINE_ERROR = "protobufEngineError";
    private final T rootBean;
    private final String message;
    private final Path propertyPath;

    /**
     * Creates a new {@link ProtobufConstraintViolation} instance from a protovalidate violation.
     * <p>
     * This constructor extracts the field path and error message from the protovalidate violation,
     * formats them into a standardized message, and creates a {@link ProtobufPath} for the violation.
     * </p>
     * 
     * @param rootBean the root bean being validated
     * @param violation the protovalidate violation to wrap
     * @see Violation
     * @see ProtobufPath
     */
    public ProtobufConstraintViolation(T rootBean, build.buf.protovalidate.Violation violation) {
        this.rootBean = rootBean;

        // 1. Unwrap the low-level proto model from the SDK wrapper interface
        Violation protoViolation = violation.toProto();

        // 2. Hydrate the clean error message text description
        String rawMessage = protoViolation.getMessage();

        // 3. Extract the clean snake_case field path name out of FieldPath's text format dump
        String fieldPathString = "";
        if (protoViolation.hasField()) {
            String rawFieldStr = protoViolation.getField().toString();

            // Try to extract field_name from FieldPath message
            if (rawFieldStr.contains("field_name:")) {
                // Find the pattern "field_name: \"value\"" and extract the value
                int fieldStart = rawFieldStr.indexOf("field_name: \"");
                if (fieldStart >= 0) {
                    int start = fieldStart + "field_name: \"".length();
                    int end = rawFieldStr.indexOf("\"", start);
                    if (end > start) {
                        fieldPathString = rawFieldStr.substring(start, end);
                    }
                }
            } else {
                // Fallback: try to extract any quoted string after field
                int quoteIndex = rawFieldStr.indexOf('"');
                if (quoteIndex >= 0) {
                    int endQuote = rawFieldStr.indexOf('"', quoteIndex + 1);
                    if (endQuote > 0) {
                        fieldPathString = rawFieldStr.substring(quoteIndex + 1, endQuote);
                    }
                }
            }
        }

        if (fieldPathString.isEmpty()) {
            fieldPathString = "payload";
        }

        // 4. Combine them into the exact format your WebTestClient assertion snippets look for
        this.message = fieldPathString + ": " + rawMessage;
        this.propertyPath = new ProtobufPath(fieldPathString);
    }

    /**
     * Creates a fallback {@link ProtobufConstraintViolation} for unexpected validation engine errors.
     * <p>
     * This constructor is used when the validation engine encounters unexpected compilation
     * or runtime errors that cannot be represented as standard protovalidate violations.
     * </p>
     * 
     * @param rootBean the root bean being validated
     * @param engineErrorMessage the error message from the validation engine
     */
    public ProtobufConstraintViolation(T rootBean, String engineErrorMessage) {
        this.rootBean = rootBean;
        this.message = "Engine error: " + engineErrorMessage;
        this.propertyPath = new ProtobufPath(PROTOBUF_ENGINE_ERROR);
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getMessageTemplate() {
        return message;
    }

    /**
     * Returns the root bean that was being validated.
     * 
     * @return the root bean instance
     */
    @Override
    public T getRootBean() {
        return rootBean;
    }

    /**
     * Returns the class of the root bean.
     * 
     * @return the root bean class
     */
    @Override
    public Class<T> getRootBeanClass() {
        @SuppressWarnings("unchecked")
        Class<T> beanClass = (Class<T>) rootBean.getClass();
        return beanClass;
    }

    /**
     * Returns the leaf bean that was being validated.
     * <p>
     * For Protobuf validation, this is the same as the root bean.
     * </p>
     * 
     * @return the root bean instance (same as {@link #getRootBean()})
     */
    @Override
    public Object getLeafBean() {
        return rootBean;
    }

    @Override
    public Object[] getExecutableParameters() {
        return new Object[0];
    }

    /**
     * Returns the executable return value that was being validated.
     * <p>
     * For parameter validation, this is always null.
     * </p>
     * 
     * @return null (parameter validation has no return value)
     */
    @Override
    public Object getExecutableReturnValue() {
        return null;
    }

    /**
     * Returns the path to the property that failed validation.
     * <p>
     * The path is formatted as a dot-separated string of field names
     * extracted from the protovalidate violation.
     * </p>
     * 
     * @return the property path as a {@link Path} instance
     */
    @Override
    public Path getPropertyPath() {
        return propertyPath;
    }

    /**
     * Returns the invalid value that caused the constraint violation.
     * <p>
     * For Protobuf validation, this is typically the root bean itself
     * (the Protobuf message instance) since we validate at the message level.
     * </p>
     * 
     * @return the invalid value (root bean instance)
     */
    @Override
    public Object getInvalidValue() {
        // Fallback placeholder instead of hardcoded null to allow basic mapping evaluation
        return rootBean;
    }

    /**
     * Returns the constraint descriptor for this violation.
     * <p>
     * This method creates a dynamic proxy implementation of {@link ConstraintDescriptor}
     * that represents the {@link ValidatedProto} annotation. Since protovalidate
     * constraints are not Java annotations, we create a synthetic descriptor.
     * </p>
     * 
     * @return the constraint descriptor for the ValidatedProto annotation
     * @see ConstraintDescriptor
     * @see ValidatedProto
     */
    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return new ConstraintDescriptor<ValidatedProto>() {
            @Override
            public ValidatedProto getAnnotation() {
                return (ValidatedProto) newProxyInstance(
                        ValidatedProto.class.getClassLoader(),
                        new Class<?>[]{ ValidatedProto.class },
                        (proxy, method, args) -> {
                            String methodName = method.getName();

                            switch(methodName) {
                                case "annotationType" -> {
                                    return ValidatedProto.class;
                                }
                                case "toString" -> {
                                    return "@io.github.neobliz1.validproto.annotation.ValidatedProto()";
                                }
                                case "hashCode" -> {
                                    return ValidatedProto.class.hashCode();
                                }
                            }
                            if("equals".equals(methodName) && args!=null && args.length==1) {
                                return proxy==args[0];
                            }
                            return null;
                        }
                );
            }

            @Override
            public String getMessageTemplate() {
                return message;
            }

            // FIX: Replaced obsolete Collections references with modern, immutable Set/List/Map factories
            @Override
            public Set<Class<?>> getGroups() {
                return Set.of();
            }

            /**
             * Returns the groups to validate.
             * <p>
             * For Protobuf validation, no specific groups are defined.
             * </p>
             * 
             * @return an empty set of groups
             */
            @Override
            public Set<Class<? extends jakarta.validation.Payload>> getPayload() {
                return Set.of();
            }

            /**
             * Returns the target of the validation.
             * <p>
             * For Protobuf validation, validation applies implicitly to parameters.
             * </p>
             * 
             * @return {@link ConstraintTarget#IMPLICIT}
             */
            @Override
            public ConstraintTarget getValidationAppliesTo() {
                return ConstraintTarget.IMPLICIT;
            }

            /**
             * Returns the constraint validator classes for this constraint.
             * <p>
             * For Protobuf validation, no standard constraint validators are used.
             * </p>
             * 
             * @return an empty list
             */
            @Override
            public List<Class<? extends ConstraintValidator<ValidatedProto, ?>>> getConstraintValidatorClasses() {
                return List.of();
            }

            /**
             * Returns the attributes of the constraint annotation.
             * <p>
             * Since we use a dynamic proxy, no additional attributes are defined.
             * </p>
             * 
             * @return an empty map of attributes
             */
            @Override
            public Map<String, Object> getAttributes() {
                return Map.of();
            }

            /**
             * Returns the composing constraints for this constraint.
             * <p>
             * For Protobuf validation, there are no composing constraints.
             * </p>
             * 
             * @return an empty set of constraint descriptors
             */
            @Override
            public Set<ConstraintDescriptor<?>> getComposingConstraints() {
                return Set.of();
            }

            /**
             * Returns whether this constraint should be reported as a single violation.
             * <p>
             * For Protobuf validation, each field violation is reported separately.
             * </p>
             * 
             * @return false
             */
            @Override
            public boolean isReportAsSingleViolation() {
                return false;
            }

            /**
             * Returns the value unwrapping strategy for this constraint.
             * <p>
             * Uses the default unwrapping strategy.
             * </p>
             * 
             * @return {@link ValidateUnwrappedValue#DEFAULT}
             */
            @Override
            public ValidateUnwrappedValue getValueUnwrapping() {
                return ValidateUnwrappedValue.DEFAULT;
            }

            /**
             * Unwraps this constraint descriptor to the specified type.
             * 
             * @param type the type to unwrap to
             * @return null (constraint descriptor cannot be unwrapped)
             */
            @Override
            public <U> U unwrap(Class<U> type) {
                return null;
            }
        };
    }

    /**
     * Unwraps this constraint violation to the specified type.
     * 
     * @param type the type to unwrap to
     * @return this instance cast to the specified type, or null if types are incompatible
     */
    @Override
    public <U> U unwrap(Class<U> type) {
        if(type.isInstance(this)) {
            return type.cast(this);
        }
        return null;
    }
}