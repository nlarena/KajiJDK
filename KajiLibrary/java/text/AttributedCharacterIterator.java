package java.text;

import java.io.InvalidObjectException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Iteration over text that also carries <em>attributes</em> attached to each run.
 *
 * <p>A {@link CharacterIterator} hands out characters; this one hands out characters and, at each
 * position, the set of attribute/value pairs that rule there. The question it adds is not "what
 * character is here?" but "how far does the run sharing these attributes reach?" -- hence
 * {@code getRunStart}/{@code getRunLimit}, which exist so a renderer can paint run by run instead of
 * asking about attributes character by character.
 *
 * <p>That is also why it lives in {@code java.text}: it is
 * {@code Format.formatToCharacterIterator}'s return type, the way a formatter tells <em>where</em>
 * each field of the result ended up without the caller having to reparse the text.
 *
 * <p>{@code getRunStart}'s three overloads are not sugar: with no argument the run is the one
 * sharing <em>every</em> attribute, with one attribute it is that one's alone, and with a set it is
 * those. A "bold" run can cross several "language" runs, so collapsing them would give the wrong
 * limit.
 */
public interface AttributedCharacterIterator extends CharacterIterator {

    /**
     * An attribute's key. It is a class and not an enum nor a {@code String} on purpose: the keys
     * are extensible (AWT defines its own, {@link Format} defines {@code Format.Field}) and at the
     * same time they have to be comparable by identity, not by name, so that two different packages
     * do not collide using the same word.
     *
     * <p>That is why {@code equals} and {@code hashCode} are {@code final} and are {@code Object}'s:
     * a subclass cannot weaken the identity. The name exists only for {@code toString} and for
     * resolving deserialisation.
     */
    public static class Attribute implements Serializable {

        // A register of the constants defined by THIS class, for readResolve. It is only populated
        // when the object built is an exact Attribute: a subclass that keeps no register of its own
        // must not contaminate this one, because two different subclasses may legitimately use the
        // same name.
        private static final Map<String, AttributedCharacterIterator.Attribute> INSTANCES =
                new HashMap<String, AttributedCharacterIterator.Attribute>();

        private final String name;

        protected Attribute(String name) {
            this.name = name;
            if (this.getClass() == AttributedCharacterIterator.Attribute.class) {
                INSTANCES.put(name, this);
            }
        }

        /** The run's language; the value is a {@link java.util.Locale}. */
        public static final AttributedCharacterIterator.Attribute LANGUAGE =
                new AttributedCharacterIterator.Attribute("language");

        /**
         * The run's reading -- the pronunciation of a text whose spelling does not determine it.
         * It exists because of Japanese: a proper name's kanji do not say how they are read, and the
         * furigana travels here.
         */
        public static final AttributedCharacterIterator.Attribute READING =
                new AttributedCharacterIterator.Attribute("reading");

        /** A segment handed over by an input method; the value is an {@link Annotation}. */
        public static final AttributedCharacterIterator.Attribute INPUT_METHOD_SEGMENT =
                new AttributedCharacterIterator.Attribute("input_method_segment");

        public final boolean equals(Object obj) {
            return this == obj;
        }

        public final int hashCode() {
            return super.hashCode();
        }

        public String toString() {
            return this.getClass().getName() + "(" + this.name + ")";
        }

        protected String getName() {
            return this.name;
        }

        /**
         * It returns the constant equivalent to the deserialised object, so that identity survives
         * a trip through a stream.
         *
         * <p>It is implemented even though KajiLibrary does not deserialise yet: the method's
         * contract is defined and the body can honour it today. The exact-type guard is part of that
         * contract -- a subclass that does not reimplement it <em>has</em> to fail, because otherwise
         * it would return a constant of the superclass instead of its own.
         */
        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != AttributedCharacterIterator.Attribute.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }
            AttributedCharacterIterator.Attribute instance = INSTANCES.get(this.getName());
            if (instance != null) {
                return instance;
            }
            throw new InvalidObjectException("unknown attribute name");
        }
    }

    int getRunStart();

    int getRunStart(AttributedCharacterIterator.Attribute attribute);

    int getRunStart(Set<? extends AttributedCharacterIterator.Attribute> attributes);

    int getRunLimit();

    int getRunLimit(AttributedCharacterIterator.Attribute attribute);

    int getRunLimit(Set<? extends AttributedCharacterIterator.Attribute> attributes);

    Map<AttributedCharacterIterator.Attribute, Object> getAttributes();

    Object getAttribute(AttributedCharacterIterator.Attribute attribute);

    Set<AttributedCharacterIterator.Attribute> getAllAttributeKeys();
}
