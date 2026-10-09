package java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

// A ResourceBundle written as a `.properties` file instead of as a class.
//
// It is ListResourceBundle's other half, and the one used when the translations are maintained by
// somebody who does not compile: a `.properties` is edited with any editor and does not have to go
// through javac. The counterpart is that the values can only be strings — a `.properties` cannot say
// "array of String" nor "integer".
//
// Loading is a single act: the file is read whole in the constructor and afterwards the bundle is
// immutable. That is why the constructor declares `throws IOException` and no other method does.
public class PropertyResourceBundle extends ResourceBundle {

    // The pairs read from the file.
    private final Properties lookup;

    // It reads the bundle from `stream`, in ISO-8859-1 as the format demands.
    //
    // That it be Latin-1 and not UTF-8 always surprises, and it is for compatibility: the format
    // predates UTF-8 being normal, and that is why it carries `\\uXXXX` — it is the only way of
    // writing a character that does not fit in a byte.
    public PropertyResourceBundle(InputStream stream) throws IOException {
        this.lookup = new Properties();
        this.lookup.load(stream);
    }

    // It reads the bundle from `reader`, which arrives already decoded.
    //
    // This is the overload to use for a file in UTF-8: the caller decides the encoding when building
    // the Reader, instead of being tied to the stream's Latin-1.
    public PropertyResourceBundle(Reader reader) throws IOException {
        this.lookup = new Properties();
        this.lookup.load(reader);
    }

    // `key`'s value in THIS bundle, or null. It does not consult the parent: getObject sees to
    // that.
    public Object handleGetObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return this.lookup.get(key);
    }

    // Every visible key: its own and whatever the parent adds.
    public Enumeration<String> getKeys() {
        Enumeration<String> fromParent = null;
        if (this.parent != null) {
            fromParent = this.parent.getKeys();
        }
        return new BundleKeyEnumeration(this.handleKeySet().iterator(), fromParent);
    }

    // The keys this bundle defines, without the parent's.
    protected Set<String> handleKeySet() {
        HashSet<String> out = new HashSet<String>();
        Iterator<String> it = this.lookup.stringPropertyNames().iterator();
        while (it.hasNext()) {
            out.add(it.next());
        }
        return out;
    }
}
