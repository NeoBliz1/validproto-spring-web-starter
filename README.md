# ValidProto Spring Web Starter

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-green.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/)
[![Tests](https://img.shields.io/badge/Tests-31%20passed-green.svg)](https://maven.apache.org/surefire/)

Automated declarative parameter validation for Protobuf payloads over HTTP in Spring Web controllers.

## Overview

ValidProto Spring Web Starter is a Spring Boot starter that enables automatic validation of Protobuf messages using [protovalidate](https://buf.build/docs/validate/introduction) constraints. It bridges the gap between the protovalidate library and Spring's validation framework, allowing you to validate Protobuf messages using simple, familiar annotations.

### Key Features

- ✅ **Declarative validation** - Use familiar annotations like `@ValidProto`
- ✅ **Full protovalidate support** - Validates all protovalidate constraints (strings, numerics, enums, collections, maps, WKTs, CEL rules)
- ✅ **Spring Boot integration** - Auto-configuration with minimal setup
- ✅ **WebFlux support** - Works with both synchronous and reactive controllers
- ✅ **Type-safe** - Full type safety with compile-time validation rules

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.neobliz1</groupId>
    <artifactId>validproto-spring-web-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

### Required Dependencies

To use ValidProto Spring Web Starter, you need to include the following dependencies in your `pom.xml`:

```xml
<properties>
    <protobuf.version>4.34.1</protobuf.version>
    <protovalidate.version>1.2.2</protovalidate.version>
</properties>

<dependencies>
    <!-- Protocol Buffers runtime -->
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>${protobuf.version}</version>
    </dependency>
    
    <!-- Protovalidate runtime -->
    <dependency>
        <groupId>build.buf</groupId>
        <artifactId>protovalidate</artifactId>
        <version>${protovalidate.version}</version>
    </dependency>
</dependencies>
```

### Gradle

Add the following dependency to your `build.gradle`:

```groovy
implementation 'io.github.neobliz1:validproto-spring-web-starter:0.0.1'
```

## Quick Start

### 1. Define your Protobuf schema with validation rules

Create a `.proto` file and import the buf.validate constraints:

```proto
syntax = "proto3";

package com.example;

// Import protovalidate constraints
import "buf/validate/validate.proto";

message CreateUserRequest {
  string email = 1 [(buf.validate.field).string.email = true];
  string name = 2 [(buf.validate.field).string.min_len = 1, (buf.validate.field).string.max_len = 100];
  int32 age = 3 [(buf.validate.field).int32.gte = 0, (buf.validate.field).int32.lte = 150];
}
```

**Note:** To use `buf/validate/validate.proto`, you need to either:
- Download the proto files from the [protovalidate repository](https://github.com/bufbuild/protovalidate)
- Use BSR (Buf Schema Registry) to reference the official buf.validate bundle
- Add the protovalidate dependency which includes the necessary proto files

For Maven, add the following to your `pom.xml` to download the proto sources:

```xml
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>0.6.1</version>
    <configuration>
        <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
        <pluginId>grpc-java</pluginId>
        <pluginArtifact>io.grpc:protoc-gen-grpc-java:${grpc-java.version}:exe:${os.detected.classifier}</pluginArtifact>
        <additionalProtoPathElements>
            <additionalProtoPathElement>${project.build.directory}/dependencies/proto</additionalProtoPathElement>
        </additionalProtoPathElements>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 2. Enable validation on your controller

```java
@ValidatedProto
@RestController
public class UserController {

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(
            @ValidProto @RequestBody CreateUserRequest request) {
        // request is automatically validated before this method is called
        return ResponseEntity.ok(new UserResponse(request.getName()));
    }
}
```

### 3. Configure Maven build for Protobuf compilation

Add the `protobuf-maven-plugin` and `os-maven-plugin` extensions to your `pom.xml`:

```xml
<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.1</version>
        </extension>
    </extensions>
    
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
                <outputDirectory>${project.build.directory}/generated-sources</outputDirectory>
                <clearOutputDirectory>false</clearOutputDirectory>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>test-compile</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 4. Generate Protobuf classes and validators

Run the following Maven command to generate Java classes from your `.proto` files:

```bash
mvn compile
```

The `protobuf-maven-plugin` will automatically compile your Protobuf definitions and generate the corresponding Java classes in `target/generated-sources/protobuf/java`. The generated classes include:

- **Message classes** for each `message` definition
- **Enum classes** for each `enum` definition
- **Builder classes** for constructing messages immutably
- **Validation constraints** from `buf.validate` annotations (compiled into the generated code)

The generated classes contain embedded validation rules that are executed at runtime by the protovalidate library. No additional validator classes need to be generated separately - validation is embedded in the generated Protobuf code.

After generation, you can use the generated classes in your Spring controllers with `@ValidProto` validation.

## Supported Validation Rules

ValidProto supports all protovalidate constraints:

### String Constraints
- `email` - Validates email format
- `pattern` - Regex pattern matching
- `ip` - IPv4/IPv6 address validation
- `uuid` - RFC 4122 UUID validation
- `uri` - URI format validation
- `min_len` / `max_len` - String length constraints

### Numeric Constraints
- `gt` / `gte` - Greater than / greater than or equal
- `lt` / `lte` - Less than / less than or equal
- `in` / `not_in` - Value membership in a list
- Range validation for `int32`, `int64`, `float`, `double`

### Collection Constraints
- `min_items` / `max_items` - Collection size constraints
- `unique` - Ensures uniqueness of items
- Item-level validation

### Map Constraints
- `min_pairs` / `max_pairs` - Map size constraints
- Key and value validation

### Enum Constraints
- `defined_only` - Ensures only defined enum values
- `not_in` - Excludes specific enum values

### Well-Known Types (WKT)
- `google.protobuf.Timestamp`
  - `gt_now` - Must be in the future
  - `lt_now` - Must be in the past
- `google.protobuf.Duration`
  - `lte.seconds` - Maximum duration
- `google.protobuf.StringValue`
  - Custom CEL rules

### Complex Validation
- **Oneof validation** - Enforces oneof field requirements
- **CEL rules** - Custom expressions for cross-field validation

## Protovalidate Dependencies and Setup

### Required Dependencies

To use ValidProto Spring Web Starter with Protobuf validation, you need to include the following dependencies in your project's `pom.xml`:

```xml
<properties>
    <protobuf.version>4.34.1</protobuf.version>
    <protovalidate.version>1.2.2</protovalidate.version>
</properties>

<dependencies>
    <!-- Protocol Buffers runtime and utilities -->
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>${protobuf.version}</version>
    </dependency>
    
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java-util</artifactId>
        <version>${protobuf.version}</version>
    </dependency>
    
    <!-- Protovalidate runtime for validation constraints -->
    <dependency>
        <groupId>build.buf</groupId>
        <artifactId>protovalidate</artifactId>
        <version>${protovalidate.version}</version>
    </dependency>
    
    <!-- OS plugin for platform-specific protoc binaries -->
    <dependency>
        <groupId>kr.motd.maven</groupId>
        <artifactId>os-maven-plugin</artifactId>
        <version>1.7.1</version>
    </dependency>
</dependencies>
```

### Build Configuration

Add the Maven plugins to your `pom.xml` build section:

```xml
<build>
    <extensions>
        <!-- Detects OS to download appropriate protoc binary -->
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.1</version>
        </extension>
    </extensions>
    
    <plugins>
        <!-- Compiles .proto files to Java classes -->
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
                <outputDirectory>${project.build.directory}/generated-sources</outputDirectory>
                <clearOutputDirectory>false</clearOutputDirectory>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>test-compile</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## API Reference

### Annotations

> [!IMPORTANT]
> Both `@ValidatedProto` and `@ValidProto` **must be used together** for validation to work.
> - `@ValidatedProto` on the controller class enables the validation mechanism
> - `@ValidProto` on method parameters marks specific Protobuf payloads for validation
> - Neither annotation has any effect when used alone

#### `@ValidatedProto`

Class-level annotation that enables validation scanning on a controller.

**Required together with:** `@ValidProto` on method parameters

```java
@ValidatedProto
@RestController
public class MyController {
    // Methods in this controller will have validation enabled
}
```

#### `@ValidProto`

Method parameter annotation that triggers validation for a Protobuf message.

**Required together with:** `@ValidatedProto` on the controller class

```java
@PostMapping("/api/data")
public ResponseEntity<String> processData(
        @ValidProto @RequestBody MyProtoMessage message) {
    // message will be validated before this method executes
}
```

> [!NOTE]
> Using only `@ValidProto` on parameters without `@ValidatedProto` on the controller class will **not** enable validation.

### Exception Handling

Validation failures result in `MethodValidationException`. You can handle this exception in your controllers:

```java
@ExceptionHandler(MethodValidationException.class)
public ResponseEntity<Map<String, Object>> handleValidationException(MethodValidationException ex) {
    List<String> errors = ex.getParameterValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream())
            .map(org.springframework.context.MessageSourceResolvable::getDefaultMessage)
            .toList();

    Map<String, Object> body = new HashMap<>();
    body.put("status", 400);
    body.put("error", "Validation Failed");
    body.put("violations", errors);

    return ResponseEntity.badRequest().body(body);
}
```

### Configuration

No explicit configuration is required. The starter auto-configures validation automatically.

#### Custom Configuration (Optional)

If you need to customize the validator, you can create your own `ProtobufValidatorAdapter` bean:

```java
@Configuration
public class CustomValidatorConfig {

    @Bean
    public ProtobufValidatorAdapter customValidator() {
        ValidatorFactory factory = ValidatorFactory.newBuilder()
                // Configure factory if needed
                .build();
        return new ProtobufValidatorAdapter(factory);
    }
}
```

## Examples

### Basic Validation

```java
@ValidatedProto
@RestController
public class DataController {

    @PostMapping("/data")
    public ResponseEntity<String> processData(@ValidProto @RequestBody DataRequest request) {
        return ResponseEntity.ok("Data processed successfully");
    }
}
```

### Reactive (Mono) Validation

```java
@PostMapping("/reactive/mono")
public Mono<String> handleMono(@ValidProto @RequestBody Mono<DataRequest> requestMono) {
    return requestMono.map(req -> "Processed: " + req.getId());
}
```

### Stream Validation

```java
@PostMapping("/reactive/stream")
public Flux<String> handleStream(@ValidProto @RequestBody Flux<DataRequest> requests) {
    return requests.map(req -> "Processed: " + req.getId());
}
```

### Complex Validation with CEL Rules

```proto
message ProcessingRequest {
  int32 start_hour = 1 [(buf.validate.field).int32.gte = 0, (buf.validate.field).int32.lte = 23];
  int32 end_hour = 2 [(buf.validate.field).int32.gte = 0, (buf.validate.field).int32.lte = 23];
  
  ProcessingWindow window = 3 [
    (buf.validate.field).cel = {
      id: "window.duration",
      message: "End hour must be strictly greater than start hour",
      expression: "this.end_hour > this.start_hour"
    }
  ];
}
```

## Testing

The starter includes comprehensive integration tests covering all validation rules. Tests are located in `src/test/java/io/github/neobliz1/validproto/`.

### Running Tests

```bash
mvn test
```

### Test Coverage

- **Instruction Coverage**: 66%
- **Class Coverage**: 100%
- **Test Count**: 31 tests

## Troubleshooting

### Common Issues

1. **Validation not working**
   - Ensure `@ValidatedProto` is on the controller class
   - Ensure `@ValidProto` is on the method parameter
   - Check that the dependency is on the classpath

2. **Unexpected validation errors**
   - Verify your `.proto` files have the correct validation rules
   - Check that you're using the correct field types

3. **Build errors with protovalidate imports**
   - Ensure you have the `buf/validate/validate.proto` file available in your proto path
   - Add the protovalidate dependency to download the proto sources
   - Configure the protobuf-maven-plugin with the correct proto path

4. **Missing generated classes**
   - Run `mvn compile` to generate Protobuf classes
   - Check that `target/generated-sources/protobuf/java` contains your generated classes
   - Ensure your IDE recognizes the generated sources directory

5. **Protoc compiler not found**
   - The `os-maven-plugin` should automatically detect your OS
   - Check that `${os.detected.classifier}` is properly set in your build
   - Manually specify the protoc executable if needed

### Dependency Resolution Issues

If you encounter dependency conflicts with protobuf versions:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
            <version>4.34.1</version>
        </dependency>
        <dependency>
            <groupId>build.buf</groupId>
            <artifactId>protovalidate</artifactId>
            <version>1.2.2</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 🤖 LLM & AI Assistant Context Reference

If you are generating code or writing tests for this project using an AI assistant (e.g., Cursor, GitHub Copilot, ChatGPT), provide this section as context to ensure accurate code generation.

### Core Intent & Architecture
* **Purpose**: Provides declarative parameter validation for Protobuf messages over HTTP using the modern `buf.validate` (Protovalidate) engine.
* **Ecosystem**: Engineered for Spring Boot 3.x/4.x and Spring WebFlux (Project Reactor).
* **Mechanism**: Integrates natively into Spring's built-in validation architecture by overriding `MethodValidationPostProcessor` with a custom `ProtobufValidatorAdapter` implementing `jakarta.validation.Validator`.
* **Reactive Unwrapping**: Relies on Spring's native `ReactorValidationHelper` to inject `.doOnNext()` validation blocks into `Mono` and `Flux` pipelines. This is triggered because `@ValidProto` is meta-annotated with `@Valid`.

### Rules for Code Generation
1. **Never use standard Spring annotations**: Do not generate `@Validated` or `@Valid` on controller classes or parameters. Replace them entirely with `@ValidatedProto` and `@ValidProto`.
2. **Annotation Placement Rules**:
    * Place `@ValidatedProto` strictly at the **`@RestController` Class level** to activate validation scanning.
    * Place `@ValidProto` directly on the **method arguments / parameters**.
3. **Reactive Stream Processing**: `@ValidProto` natively triggers Spring to wrap synchronous messages (`Message`), monolithic reactive wrappers (`Mono<Message>`), and streaming pipelines (`Flux<Message>`). Do not write manual `.flatMap()` or validation blocks in the controller body.
4. **Exception Mapping**: Validation failures trigger a native Spring `MethodValidationException` (when `adaptConstraintViolations` is active) or `ConstraintViolationException`. Use an `@ExceptionHandler` to unpack and return violations in a JSON response containing `$.violations`.
5. **Case-Sensitivity Casing**: Underlying `buf.validate` assertions return capitalized tokens (e.g., `URI`, `UUID`, `IP`) inside violation messages. Ensure generated test suites evaluate string tokens case-insensitively.

### Correct Target Code Pattern
```java
import io.github.neobliz1.validproto.annotation.ValidatedProto;
import io.github.neobliz1.validproto.annotation.ValidProto;
import io.github.neobliz1.validproto.test.ComplexTestPayload;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ValidatedProto // 1. Activates class-level scanning via native MethodValidationPostProcessor
@RestController
public class ProtobufController {

    @PostMapping(value = "/sync", consumes = MediaType.APPLICATION_PROTOBUF_VALUE)
    public String sync(@ValidProto @RequestBody ComplexTestPayload payload) {
        return "SUCCESS: " + payload.getPriorityLevel();
    }

    @PostMapping(value = "/reactive/mono", consumes = MediaType.APPLICATION_PROTOBUF_VALUE)
    public Mono<String> mono(@ValidProto @RequestBody Mono<ComplexTestPayload> payloadMono) {
        return payloadMono.map(payload -> "SUCCESS: " + payload.getPriorityLevel());
    }

    @PostMapping(value = "/reactive/stream", consumes = MediaType.APPLICATION_PROTOBUF_VALUE)
    public Flux<String> stream(@ValidProto @RequestBody Flux<ComplexTestPayload> payloads) {
        return payloads.map(payload -> "Processed: " + payload.getPriorityLevel());
    }
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Protovalidate](https://buf.build/docs/validate/introduction) for the validation library
- [Spring Boot](https://spring.io/projects/spring-boot) for the framework
- [Protocol Buffers](https://developers.google.com/protocol-buffers) for the serialization format
