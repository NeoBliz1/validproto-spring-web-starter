package io.github.neobliz1.validproto.config;

import build.buf.protovalidate.ValidatorFactory;
import io.github.neobliz1.validproto.annotation.ValidatedProto;
import io.github.neobliz1.validproto.infra.ProtobufValidatorAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Auto-configuration class for ProtoBuf validation in Spring Web applications.
 * <p>
 * Provides automatic setup of validation infrastructure including:
 * <ul>
 *   <li>{@link LocalValidatorFactoryBean} for general validation
 *   <li>{@link ProtobufValidatorAdapter} for ProtoBuf message validation
 *   <li>{@link MethodValidationPostProcessor} for method-level validation with {@link ValidatedProto} annotation
 * </ul>
 * <p>
 * This configuration is automatically applied when the required dependencies are present
 * on the classpath.
 *
 * @author NeoBliz1
 * @since 0.0.1
 */
@AutoConfiguration
public class HttpValidateProtoAutoConfiguration {

    /**
     * Creates the default {@link LocalValidatorFactoryBean} instance for the application.
     * <p>
     * This bean is registered as infrastructure role and is conditional on not already existing.
     * Customizes the validator configuration with any available {@link ValidationConfigurationCustomizer} beans.
     *
     * @param applicationContext the Spring application context
     * @param customizers object provider for validation configuration customizers
     * @return configured {@link LocalValidatorFactoryBean} instance
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(LocalValidatorFactoryBean.class)
    public static LocalValidatorFactoryBean defaultValidator(
            ApplicationContext applicationContext,
            ObjectProvider<ValidationConfigurationCustomizer> customizers) {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.setConfigurationInitializer((configuration) -> customizers.orderedStream()
                .forEach((customizer) -> customizer.customize(configuration)));
        MessageInterpolatorFactory interpolatorFactory = new MessageInterpolatorFactory(applicationContext);
        factoryBean.setMessageInterpolator(interpolatorFactory.getObject());
        return factoryBean;
    }

    /**
     * Creates the {@link ProtobufValidatorAdapter} instance for ProtoBuf message validation.
     * <p>
     * This bean is registered as infrastructure role and is conditional on not already existing.
     * Uses the default {@link ValidatorFactory} configuration.
     *
     * @return {@link ProtobufValidatorAdapter} instance
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(ProtobufValidatorAdapter.class)
    public ProtobufValidatorAdapter protobufValidatorAdapter() {
        return new ProtobufValidatorAdapter(ValidatorFactory.newBuilder().build());
    }

    /**
     * Creates the {@link MethodValidationPostProcessor} for method-level ProtoBuf validation.
     * <p>
     * This bean is registered as infrastructure role with high precedence and is conditional
     * on not already existing. Configures the processor to use {@link ProtobufValidatorAdapter}
     * and {@link ValidatedProto} annotation for validation.
     *
     * @param environment the Spring environment for property access
     * @param protobufValidatorAdapter the ProtoBuf validator adapter
     * @return configured {@link MethodValidationPostProcessor} instance
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Order(Ordered.HIGHEST_PRECEDENCE+100)
    @ConditionalOnMissingBean(name = "protobufMethodValidationPostProcessor")
    public MethodValidationPostProcessor protobufMethodValidationPostProcessor(
            Environment environment,
            ProtobufValidatorAdapter protobufValidatorAdapter) {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        boolean proxyTargetClass = environment.getProperty("spring.aop.proxy-target-class", Boolean.class, true);
        processor.setProxyTargetClass(proxyTargetClass);
        processor.setAdaptConstraintViolations(true);
        processor.setValidator(protobufValidatorAdapter);
        processor.setValidatedAnnotationType(ValidatedProto.class);
        return processor;
    }
}
