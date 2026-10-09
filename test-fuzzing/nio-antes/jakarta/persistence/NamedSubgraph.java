package jakarta.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The {@code @NamedSubgraph} annotation of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary rather than from the specification
 * sources: the members come from the class file's method descriptors and the
 * {@code default} clauses from its {@code AnnotationDefault} attributes. What the
 * annotation <em>means</em> is defined by the Jakarta Persistence specification,
 * not here.
 */
@Retention(RUNTIME)
public @interface NamedSubgraph {

    /**
     * The {@code name} member.
     *
     * @return the configured value
     */
    String name();

    /**
     * The {@code type} member.
     *
     * @return the configured value
     */
    Class<?> type();

    /**
     * The {@code attributeNodes} member.
     *
     * @return the configured value
     */
    NamedAttributeNode[] attributeNodes();
}
