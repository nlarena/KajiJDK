package java.nio.file.attribute;

// A view of a **file's** attributes, as opposed to `FileStoreAttributeView`, which is the volume's.
// It adds nothing to `AttributeView`: the division exists only so the type system tells the two
// families apart, which is why `Files.getFileAttributeView` can ask for `Class<V extends
// FileAttributeView>` and reject a volume view at compile time.
public interface FileAttributeView extends AttributeView {
}
