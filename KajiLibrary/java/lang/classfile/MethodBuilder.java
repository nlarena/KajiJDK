package java.lang.classfile;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.AccessFlag.Location;
import jdk.internal.classfile.impl.AccessFlagsImpl;
import java.util.function.Consumer;

/** Where a method gets written. */
public interface MethodBuilder extends ClassFileBuilder<MethodElement, MethodBuilder> {

    /** The method's flags, as a bit mask. */
    default MethodBuilder withFlags(int flags) {
        return this.with(new AccessFlagsImpl(flags, Location.METHOD));
    }

    /** The method's flags. See the note on `FieldBuilder.withFlags`. */
    default MethodBuilder withFlags(AccessFlag... flags) {
        int m = 0;
        for (int i = 0; i < flags.length; i++) {
            m = m | flags[i].mask();
        }
        return this.with(new AccessFlagsImpl(m, Location.METHOD));
    }

    /**
     * The method's body: the `Consumer` gets a {@link CodeBuilder} and writes the instructions.
     *
     * <p>An abstract or native method does not call it; the others do, and exactly once.
     */
    MethodBuilder withCode(Consumer<CodeBuilder> code);

    /** The body, copied from that model through that transformation. */
    MethodBuilder transformCode(CodeModel code, CodeTransform transform);
}
