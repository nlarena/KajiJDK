package javax.imageio;

import javax.imageio.metadata.IIOMetadata;

/**
 * KajiLibrary's javax.imageio.ImageTranscoder -- translates metadata from one format to another.
 *
 * <p>Converting the <b>pixels</b> from PNG to JPEG is easy: decode and encode. What gets lost on
 * the way is the metadata, because each format stores it its own way. This interface is how it is
 * kept.
 *
 * <p>Both methods take the metadata of the source format and return the equivalent in the target
 * one. The translation goes through the <b>standard format</b> of {@code javax.imageio.metadata}:
 * the source is expressed in the common tree, and from there the target takes what it
 * understands.
 *
 * <p>What the target cannot express is lost, and there is no way around that. Returning null is
 * valid and means "I cannot translate anything of this".
 *
 * <p>There are two methods because there is <b>stream</b> metadata --valid for all the images of a
 * file with several-- and metadata of <b>each image</b>.
 */
public interface ImageTranscoder {

    /**
     * Translates the stream metadata.
     *
     * @param inData the source format's
     * @param param the write parameters, or null
     * @return the target format's, or null if nothing can be translated
     */
    IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param);

    /**
     * Translates an image's metadata.
     *
     * @param imageType what type the written image will be
     * @return the target format's, or null
     */
    IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType,
                                     ImageWriteParam param);
}
