package java.nio.file.attribute;

// `FileAttributeView`'s counterpart on the volume's side (`FileStore`). Just as empty and for the
// same reason: it separates the two families in the type system.
public interface FileStoreAttributeView extends AttributeView {
}
