package java.lang.foreign;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * KajiLibrary's java.lang.foreign.SymbolLookup -- looking a native symbol up by name.
 *
 * <p>It is the top half of a native call: first the function's address is found, then {@link Linker}
 * turns it into something invocable. Without a linker the second half does not exist, and neither
 * can the two forms that **load** a library: loading a dll or an so is an operating system operation
 * this VM does not do.
 *
 * <p>The interface is here in full all the same, and the two `default` methods --{@link #findOrThrow}
 * and {@link #or}-- are real: they lean only on {@link #find}, so a lookup of one's own written by
 * somebody else inherits them working. That is what makes declaring it worthwhile instead of leaving
 * it out.
 */
public interface SymbolLookup {

    /** The address of the symbol with that name, or empty if it is not there. */
    Optional<MemorySegment> find(String name);

    /**
     * The one above, demanding that it be there.
     *
     * @throws NoSuchElementException if it is not there
     */
    default MemorySegment findOrThrow(String name) {
        Optional<MemorySegment> found = this.find(name);
        if (!found.isPresent()) {
            throw new NoSuchElementException("symbol not found: " + name);
        }
        return found.get();
    }

    /**
     * This lookup, and if it fails the other one.
     *
     * <p>The order matters and is the one that reads: **this** one wins. It is what allows putting a
     * table of one's own in front of the platform's.
     */
    default SymbolLookup or(SymbolLookup other) {
        if (other == null) {
            throw new NullPointerException("other");
        }
        return new ChainedLookup(this, other);
    }

    /**
     * The symbols the class loader has published.
     *
     * <p>It returns a lookup that **finds nothing**, and that is the truth and not a stub: this VM
     * loads no native libraries, so there is no published symbol. It is the same answer the JDK gives
     * when none has been loaded.
     */
    static SymbolLookup loaderLookup() {
        return new EmptyLookup();
    }

    /**
     * That library's symbols.
     *
     * @throws UnsupportedOperationException always, in this library: loading a dll or an so is an
     *     operating system operation this VM does not do. Returning an empty lookup would be worse
     *     -- it would say "I loaded it and it has no symbols" instead of "I cannot load it".
     */
    static SymbolLookup libraryLookup(String name, Arena arena) {
        throw new UnsupportedOperationException("KajiJDK loads no native libraries: " + name);
    }

    /** See {@link #libraryLookup(String, Arena)}. */
    static SymbolLookup libraryLookup(java.nio.file.Path path, Arena arena) {
        throw new UnsupportedOperationException("KajiJDK loads no native libraries: " + path);
    }
}

// The two implementations the statics return. They are named classes and not anonymous ones because
// this compiler's anonymous classes drag along captures that are not needed here.
final class EmptyLookup implements SymbolLookup {

    public Optional<MemorySegment> find(String name) {
        return Optional.empty();
    }
}

final class ChainedLookup implements SymbolLookup {

    private final SymbolLookup first;
    private final SymbolLookup second;

    ChainedLookup(SymbolLookup first, SymbolLookup second) {
        this.first = first;
        this.second = second;
    }

    public Optional<MemorySegment> find(String name) {
        Optional<MemorySegment> found = this.first.find(name);
        if (found.isPresent()) {
            return found;
        }
        return this.second.find(name);
    }
}
