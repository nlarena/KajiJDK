package jakarta.validation.valueextraction;
// KajiLibrary's jakarta.validation.valueextraction.ValueExtractor — extracts the value(s) held by a
// container so they can be validated.
public interface ValueExtractor<T> {
    void extractValues(T originalValue, ValueReceiver receiver);
    public interface ValueReceiver {
        void value(String nodeName, Object object);
        void iterableValue(String nodeName, Object object);
        void indexedValue(String nodeName, int i, Object object);
        void keyedValue(String nodeName, Object key, Object object);
    }
}
