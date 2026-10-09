package java.lang.classfile;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.AccessFlag.Location;
import jdk.internal.classfile.impl.AccessFlagsImpl;

/**
 * Where a field gets written.
 *
 * <p>It has little of its own: a field is its name, its descriptor, its flags and its attributes, and
 * the first two are fixed by whoever creates it ({@link ClassBuilder#withField}). What is left is
 * this.
 */
public interface FieldBuilder extends ClassFileBuilder<FieldElement, FieldBuilder> {

    /** The field's flags, as a bit mask. */
    default FieldBuilder withFlags(int flags) {
        return this.with(new AccessFlagsImpl(flags, Location.FIELD));
    }

    /**
     * The field's flags.
     *
     * <p>The mask is built here instead of keeping the set: `AccessFlags` exposes both views and the
     * mask is the one that goes into the file, so it is the one worth having first hand.
     */
    default FieldBuilder withFlags(AccessFlag... flags) {
        int m = 0;
        for (int i = 0; i < flags.length; i++) {
            m = m | flags[i].mask();
        }
        return this.with(new AccessFlagsImpl(m, Location.FIELD));
    }
}
