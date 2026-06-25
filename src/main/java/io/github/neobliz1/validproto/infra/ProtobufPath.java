package io.github.neobliz1.validproto.infra;

import static io.github.neobliz1.validproto.infra.ProtobufConstraintViolation.PROTOBUF_ENGINE_ERROR;

import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of {@link Path} for Protobuf validation violations.
 * <p>
 * This class represents the property path to a field that failed validation in a Protobuf message.
 * The path is constructed from dot-separated field names extracted from protovalidate violations.
 * </p>
 * <p>
 * Example paths:
 * <ul>
 *   <li>{@code "user_email"} - for top-level field validation</li>
 *   <li>{@code "window.start_hour"} - for nested field validation</li>
 *   <li>{@code "metadata_tags[key_name]"} - for map field validation</li>
 * </ul>
 * </p>
 * <p>
 * The path includes an implicit "argument" node at the beginning to indicate parameter-level validation,
 * followed by property nodes for each field in the path.
 * </p>
 * 
 * @see Path
 * @see ProtobufConstraintViolation
 * @since 0.0.1
 */
public class ProtobufPath implements Path {
    private final List<Node> nodes = new ArrayList<>();
    private final String pathStr;

    /**
     * Creates a new {@link ProtobufPath} instance from a field path string.
     * <p>
     * The path string is expected to be in snake_case format (e.g., "user_email", "window.start_hour").
     * The constructor parses this string and creates a list of {@link Node} instances representing
     * the path to the validated field.
     * </p>
     * <p>
     * The first node is always an implicit "argument" node of kind {@link ElementKind#PARAMETER},
     * followed by property nodes for each field in the path.
     * </p>
     * 
     * @param fieldPath the dot-separated field path string from the protovalidate violation
     * @see Node
     * @see ElementKind
     */
    public ProtobufPath(String fieldPath) {
        this.pathStr = fieldPath;

        nodes.add(new ProtobufNode("argument", ElementKind.PARAMETER));

        if (!fieldPath.isEmpty() && !PROTOBUF_ENGINE_ERROR.equals(fieldPath)) {
            for (String part : fieldPath.split("\\.")) {
                nodes.add(new ProtobufNode(part, ElementKind.PROPERTY));
            }
        }
    }

    /**
     * Returns an iterator over the nodes in this path.
     * <p>
     * The first node is always the implicit "argument" parameter node, followed by
     * property nodes for each field in the path.
     * </p>
     * 
     * @return an iterator over the path nodes
     */
    @Override
    public @NonNull Iterator<Node> iterator() {
        return nodes.iterator();
    }

    /**
     * Returns the string representation of this path.
     * <p>
     * This is the original field path string that was passed to the constructor.
     * </p>
     * 
     * @return the path string
     */
    @Override
    public String toString() {
        return pathStr;
    }

    /**
     * A node in the Protobuf validation path.
     * <p>
     * This record represents either a parameter or property node in the validation path.
     * For Protobuf validation, we primarily use property nodes for each field in the message.
     * </p>
     * 
     * @param name the name of the node
     * @param kind the kind of node (PARAMETER or PROPERTY)
     * @see Node
     * @see ElementKind
     */
    private record ProtobufNode(String name, ElementKind kind) implements Node {

        /**
         * Returns the name of this node.
         * 
         * @return the node name
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * Returns whether this node is in an iterable (array/collection).
         * <p>
         * For Protobuf validation, this always returns false.
         * </p>
         * 
         * @return false
         */
        @Override
        public boolean isInIterable() {
            return false;
        }

        /**
         * Returns the index of this node if it's in an iterable.
         * <p>
         * Since Protobuf validation doesn't use iterables in the path,
         * this always returns null.
         * </p>
         * 
         * @return null
         */
        @Override
        public Integer getIndex() {
            return null;
        }

        /**
         * Returns the key of this node if it's in a map.
         * <p>
         * Since Protobuf validation doesn't use maps in the path,
         * this always returns null.
         * </p>
         * 
         * @return null
         */
        @Override
        public Object getKey() {
            return null;
        }

        /**
         * Returns the kind of this node.
         * 
         * @return the node kind (PARAMETER or PROPERTY)
         */
        @Override
        public ElementKind getKind() {
            return kind;
        }

        /**
         * Unwraps this node to the specified type.
         *
         * @param aClass the type to unwrap to
         * @return this instance cast to the specified type, or null if types are incompatible
         */
        @Override
        public <T extends Node> T as(Class<T> aClass) {
            if (aClass.isAssignableFrom(ParameterNode.class) && kind == ElementKind.PARAMETER) {
                return aClass.cast(new ParameterNode() {
                    @Override
                    public int getParameterIndex() {
                        return 0;
                    }

                    @Override
                    public String getName() {
                        return name;
                    }

                    @Override
                    public boolean isInIterable() {
                        return false;
                    }

                    @Override
                    public Integer getIndex() {
                        return null;
                    }

                    @Override
                    public Object getKey() {
                        return null;
                    }

                    @Override
                    public ElementKind getKind() {
                        return kind;
                    }

                    @Override
                    public <U extends Node> U as(Class<U> aClass) {
                        return null;
                    }
                });
            }
            return null;
        }
    }
}
