package io.github.neobliz1.validproto.config;

import build.buf.protovalidate.ValidatorFactory;
import io.github.neobliz1.validproto.infra.ProtobufValidatorAdapter;
import io.github.neobliz1.validproto.annotation.ValidatedProto;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Auto-configuration class for enabling Protobuf validation in Spring Boot applications.
 * <p>
 * This configuration class is automatically loaded by Spring Boot when the
 * {@code validproto-spring-web-starter} dependency is present on the classpath.
 * It registers the {@link ProtobufValidatorAdapter} and {@link MethodValidationPostProcessor}
 * beans required for validating Protobuf messages in Spring WebFlux controllers.
 * </p>
 * <p>
 * The configuration performs the following:
 * <ul>
 *   <li>Creates a {@link ProtobufValidatorAdapter} bean using the {@link ValidatorFactory}
 *       from the protovalidate library</li>
 *   <li>Registers a {@link MethodValidationPostProcessor} that scans for {@link ValidatedProto}
 *       annotations on controller classes</li>
 *   <li>Configures the post-processor to adapt constraint violations to the protovalidate format</li>
 * </ul>
 * </p>
 * <p>
 * The {@link MethodValidationPostProcessor} bean is registered with:
 * <ul>
 *   <li>{@link Role#INFRASTRUCTURE} to ensure proper lifecycle management</li>
 *   <li>{@link Order#HIGHEST_PRECEDENCE + 100} to ensure it processes before other method validators</li>
 * </ul>
 * </p>
 * 
 * @see ProtobufValidatorAdapter
 * @see ValidatedProto
 * @see MethodValidationPostProcessor
 * @see ValidatorFactory
 * @since 0.0.1
 */
@AutoConfiguration
public class HttpValidateProtoAutoConfiguration {

    /**
     * Creates a {@link ProtobufValidatorAdapter} bean for validating Protobuf messages.
     * <p>
     * This method initializes the adapter with a {@link ValidatorFactory} that uses
     * the default configuration from the protovalidate library.
     * </p>
     * 
     * @return a configured {@link ProtobufValidatorAdapter} instance
     * @see ProtobufValidatorAdapter#ProtobufValidatorAdapter(build.buf.protovalidate.Validator)
     * @see ValidatorFactory#newBuilder()
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public ProtobufValidatorAdapter protobufValidatorAdapter() {
        return new ProtobufValidatorAdapter(ValidatorFactory.newBuilder().build());
    }

    /**
     * Creates a {@link MethodValidationPostProcessor} bean that scans for {@link ValidatedProto} annotations.
     * <p>
     * The post-processor is configured to:
     * <ul>
     *   <li>Only validate methods in classes annotated with {@link ValidatedProto}</li>
     *   <li>Adapt constraint violations to work with the protovalidate validation format</li>
     * </ul>
     * </p>
     * 
     * @param protobufValidatorAdapter the validator adapter to use for validation
     * @return a configured {@link MethodValidationPostProcessor} instance
     * @see MethodValidationPostProcessor#setValidator
     * @see MethodValidationPostProcessor#setValidatedAnnotationType
     * @see MethodValidationPostProcessor#setAdaptConstraintViolations
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    public MethodValidationPostProcessor protobufMethodValidationPostProcessor(
            ProtobufValidatorAdapter protobufValidatorAdapter) {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(protobufValidatorAdapter);
        processor.setValidatedAnnotationType(ValidatedProto.class);
        processor.setAdaptConstraintViolations(true);
        return processor;
    }
}
