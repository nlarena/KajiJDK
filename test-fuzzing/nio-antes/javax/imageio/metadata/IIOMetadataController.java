package javax.imageio.metadata;

/**
 * KajiLibrary's javax.imageio.metadata.IIOMetadataController -- asks the user to fill in the
 * metadata.
 *
 * <p>The equivalent of {@code javax.imageio.IIOParamController} for metadata, with the same
 * contract: it modifies the object it receives, and if it returns false it should leave it <b>as it
 * was</b> (the JDK only asks for false on cancel; see {@code IIOParamController}).
 *
 * <p>It serves to ask the user for an image's title, author or description before saving it,
 * without the code that writes the image knowing anything about user interfaces.
 */
public interface IIOMetadataController {

    /**
     * Fills in that metadata.
     *
     * @return whether the user accepted; false should leave it untouched
     */
    boolean activate(IIOMetadata metadata);
}
