package javax.imageio.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageTypeSpecifier;

/**
 * El esquema de {@code javax_imageio_1.0}, el formato de metadatos comun a todos los formatos de
 * imagen.
 *
 * <p>De acceso de paquete: se llega por {@code IIOMetadataFormatImpl.getStandardFormatInstance()}.
 *
 * <p>Las siete ramas de primer nivel --Chroma, Compression, Data, Dimension, Document, Text y
 * Transparency-- son las categorias en que el estandar divide todo lo que un formato de imagen puede
 * decir de si mismo. Un formato concreto llena las que sepa y deja las demas afuera.
 *
 * <p>El esquema se transcribio del propio JDK 25 y no a mano: son mas de treinta elementos con sus
 * atributos, enumeraciones y rangos, y un valor distinto produce un formato que acepta arboles que el
 * JDK rechaza.
 *
 * <p>{@link #canNodeAppear} devuelve <b>siempre</b> true, incluso para un nombre que el formato no
 * define. Es lo que hace el JDK 25 --se comprobo-- y tiene su logica: el formato estandar es comun a
 * todos los tipos de imagen, asi que ningun tipo puede descartar una rama, y el metodo no esta pensado
 * para validar nombres. Para eso estan {@code getChildPolicy} y companeros, que si fallan.
 */
final class StandardMetadataFormat extends IIOMetadataFormatImpl {

    StandardMetadataFormat() {
        super("javax_imageio_1.0", CHILD_POLICY_SOME);
        // Sin paquete de recursos: las descripciones del formato estandar viven en un archivo de
        // textos del JDK que esta biblioteca no trae, y devolver null es mejor que inventarlas.
        setResourceBaseName(null);
        addElement("Chroma", "javax_imageio_1.0", CHILD_POLICY_SOME);
        addElement("ColorSpaceType", "Chroma", CHILD_POLICY_EMPTY);
        addAttribute("ColorSpaceType", "name", DATATYPE_STRING, true, null,
                     Arrays.asList(new String[] { "XYZ", "Lab", "Luv", "YCbCr", "Yxy", "YCCK", "PhotoYCC", "RGB", "GRAY", "HSV", "HLS", "CMYK", "CMY", "2CLR", "3CLR", "4CLR", "5CLR", "6CLR", "7CLR", "8CLR", "9CLR", "ACLR", "BCLR", "CCLR", "DCLR", "ECLR", "FCLR" }));
        addElement("NumChannels", "Chroma", CHILD_POLICY_EMPTY);
        addAttribute("NumChannels", "value", DATATYPE_INTEGER, true, 0, 2147483647);
        addElement("Gamma", "Chroma", CHILD_POLICY_EMPTY);
        addAttribute("Gamma", "value", DATATYPE_FLOAT, true, null);
        addElement("BlackIsZero", "Chroma", CHILD_POLICY_EMPTY);
        addAttribute("BlackIsZero", "value", DATATYPE_BOOLEAN, true, "TRUE",
                     Arrays.asList(new String[] { "TRUE", "FALSE" }));
        addElement("Palette", "Chroma", 0, 2147483647);
        addElement("PaletteEntry", "Palette", CHILD_POLICY_EMPTY);
        addAttribute("PaletteEntry", "index", DATATYPE_INTEGER, true, null);
        addAttribute("PaletteEntry", "red", DATATYPE_INTEGER, true, null);
        addAttribute("PaletteEntry", "green", DATATYPE_INTEGER, true, null);
        addAttribute("PaletteEntry", "blue", DATATYPE_INTEGER, true, null);
        addAttribute("PaletteEntry", "alpha", DATATYPE_INTEGER, false, "255");
        addElement("BackgroundIndex", "Chroma", CHILD_POLICY_EMPTY);
        addAttribute("BackgroundIndex", "value", DATATYPE_INTEGER, true, null);
        addElement("BackgroundColor", "Chroma", CHILD_POLICY_EMPTY);
        addAttribute("BackgroundColor", "red", DATATYPE_INTEGER, true, null);
        addAttribute("BackgroundColor", "green", DATATYPE_INTEGER, true, null);
        addAttribute("BackgroundColor", "blue", DATATYPE_INTEGER, true, null);
        addElement("Compression", "javax_imageio_1.0", CHILD_POLICY_SOME);
        addElement("CompressionTypeName", "Compression", CHILD_POLICY_EMPTY);
        addAttribute("CompressionTypeName", "value", DATATYPE_STRING, true, null);
        addElement("Lossless", "Compression", CHILD_POLICY_EMPTY);
        addAttribute("Lossless", "value", DATATYPE_BOOLEAN, true, "TRUE",
                     Arrays.asList(new String[] { "TRUE", "FALSE" }));
        addElement("NumProgressiveScans", "Compression", CHILD_POLICY_EMPTY);
        addAttribute("NumProgressiveScans", "value", DATATYPE_INTEGER, true, null);
        addElement("BitRate", "Compression", CHILD_POLICY_EMPTY);
        addAttribute("BitRate", "value", DATATYPE_FLOAT, true, null);
        addElement("Data", "javax_imageio_1.0", CHILD_POLICY_SOME);
        addElement("PlanarConfiguration", "Data", CHILD_POLICY_EMPTY);
        addAttribute("PlanarConfiguration", "value", DATATYPE_STRING, true, null,
                     Arrays.asList(new String[] { "PixelInterleaved", "PlaneInterleaved", "LineInterleaved", "TileInterleaved" }));
        addElement("SampleFormat", "Data", CHILD_POLICY_EMPTY);
        addAttribute("SampleFormat", "value", DATATYPE_STRING, true, null,
                     Arrays.asList(new String[] { "SignedIntegral", "UnsignedIntegral", "Real", "Index" }));
        addElement("BitsPerSample", "Data", CHILD_POLICY_EMPTY);
        addAttribute("BitsPerSample", "value", DATATYPE_INTEGER, true, 1, 2147483647);
        addElement("SignificantBitsPerSample", "Data", CHILD_POLICY_EMPTY);
        addAttribute("SignificantBitsPerSample", "value", DATATYPE_INTEGER, true, 1, 2147483647);
        addElement("SampleMSB", "Data", CHILD_POLICY_EMPTY);
        addAttribute("SampleMSB", "value", DATATYPE_INTEGER, true, 1, 2147483647);
        addElement("Dimension", "javax_imageio_1.0", CHILD_POLICY_SOME);
        addElement("PixelAspectRatio", "Dimension", CHILD_POLICY_EMPTY);
        addAttribute("PixelAspectRatio", "value", DATATYPE_FLOAT, true, null);
        addElement("ImageOrientation", "Dimension", CHILD_POLICY_EMPTY);
        addAttribute("ImageOrientation", "value", DATATYPE_STRING, true, null,
                     Arrays.asList(new String[] { "Normal", "Rotate90", "Rotate180", "Rotate270", "FlipH", "FlipV", "FlipHRotate90", "FlipVRotate90" }));
        addElement("HorizontalPixelSize", "Dimension", CHILD_POLICY_EMPTY);
        addAttribute("HorizontalPixelSize", "value", DATATYPE_FLOAT, true, null);
        addElement("VerticalPixelSize", "Dimension", CHILD_POLICY_EMPTY);
        addAttribute("VerticalPixelSize", "value", DATATYPE_FLOAT, true, null);
        addElement("HorizontalPhysicalPixelSpacing", "Dimension", CHILD_POLICY_EMPTY);
        addAttribute("HorizontalPhysicalPixelSpacing", "value", DATATYPE_FLOAT, true, null);
        addElement("VerticalPhysicalPixelSpacing", "Dimension", CHILD_POLICY_EMPTY);
        addAttribute("VerticalPhysicalPixelSpacing", "value", DATATYPE_FLOAT, true, null);
        addElement("HorizontalPosition", "Dimension", CHILD_POLICY_EMPTY);
        addAttribute("HorizontalPosition", "value", DATATYPE_FLOAT, true, null);
        addElement("VerticalPosition", "Dimension", CHILD_POLICY_EMPTY);
        addAttribute("VerticalPosition", "value", DATATYPE_FLOAT, true, null);
        addElement("HorizontalPixelOffset", "Dimension", CHILD_POLICY_EMPTY);
        addAttribute("HorizontalPixelOffset", "value", DATATYPE_INTEGER, true, null);
        addElement("VerticalPixelOffset", "Dimension", CHILD_POLICY_EMPTY);
        addAttribute("VerticalPixelOffset", "value", DATATYPE_INTEGER, true, null);
        addElement("HorizontalScreenSize", "Dimension", CHILD_POLICY_EMPTY);
        addAttribute("HorizontalScreenSize", "value", DATATYPE_INTEGER, true, null);
        addElement("VerticalScreenSize", "Dimension", CHILD_POLICY_EMPTY);
        addAttribute("VerticalScreenSize", "value", DATATYPE_INTEGER, true, null);
        addElement("Document", "javax_imageio_1.0", CHILD_POLICY_SOME);
        addElement("FormatVersion", "Document", CHILD_POLICY_EMPTY);
        addAttribute("FormatVersion", "value", DATATYPE_STRING, true, null);
        addElement("SubimageInterpretation", "Document", CHILD_POLICY_EMPTY);
        addAttribute("SubimageInterpretation", "value", DATATYPE_STRING, true, null,
                     Arrays.asList(new String[] { "Standalone", "SinglePage", "FullResolution", "ReducedResolution", "PyramidLayer", "Preview", "VolumeSlice", "ObjectView", "Panorama", "AnimationFrame", "TransparencyMask", "CompositingLayer", "SpectralSlice", "Unknown" }));
        addElement("ImageCreationTime", "Document", CHILD_POLICY_EMPTY);
        addAttribute("ImageCreationTime", "year", DATATYPE_INTEGER, true, null);
        addAttribute("ImageCreationTime", "month", DATATYPE_INTEGER, true, null, "1", "12", true, true);
        addAttribute("ImageCreationTime", "day", DATATYPE_INTEGER, true, null, "1", "31", true, true);
        addAttribute("ImageCreationTime", "hour", DATATYPE_INTEGER, false, "0", "0", "23", true, true);
        addAttribute("ImageCreationTime", "minute", DATATYPE_INTEGER, false, "0", "0", "59", true, true);
        addAttribute("ImageCreationTime", "second", DATATYPE_INTEGER, false, "0", "0", "60", true, true);
        addElement("ImageModificationTime", "Document", CHILD_POLICY_EMPTY);
        addAttribute("ImageModificationTime", "year", DATATYPE_INTEGER, true, null);
        addAttribute("ImageModificationTime", "month", DATATYPE_INTEGER, true, null, "1", "12", true, true);
        addAttribute("ImageModificationTime", "day", DATATYPE_INTEGER, true, null, "1", "31", true, true);
        addAttribute("ImageModificationTime", "hour", DATATYPE_INTEGER, false, "0", "0", "23", true, true);
        addAttribute("ImageModificationTime", "minute", DATATYPE_INTEGER, false, "0", "0", "59", true, true);
        addAttribute("ImageModificationTime", "second", DATATYPE_INTEGER, false, "0", "0", "60", true, true);
        addElement("Text", "javax_imageio_1.0", 0, 2147483647);
        addElement("TextEntry", "Text", CHILD_POLICY_EMPTY);
        addAttribute("TextEntry", "keyword", DATATYPE_STRING, false, null);
        addAttribute("TextEntry", "value", DATATYPE_STRING, true, null);
        addAttribute("TextEntry", "language", DATATYPE_STRING, false, null);
        addAttribute("TextEntry", "encoding", DATATYPE_STRING, false, null);
        addAttribute("TextEntry", "compression", DATATYPE_STRING, false, "none",
                     Arrays.asList(new String[] { "none", "lzw", "zip", "bzip", "other" }));
        addElement("Transparency", "javax_imageio_1.0", CHILD_POLICY_SOME);
        addElement("Alpha", "Transparency", CHILD_POLICY_EMPTY);
        addAttribute("Alpha", "value", DATATYPE_STRING, false, "none",
                     Arrays.asList(new String[] { "none", "premultiplied", "nonpremultiplied" }));
        addElement("TransparentIndex", "Transparency", CHILD_POLICY_EMPTY);
        addAttribute("TransparentIndex", "value", DATATYPE_INTEGER, true, null);
        addElement("TransparentColor", "Transparency", CHILD_POLICY_EMPTY);
        addAttribute("TransparentColor", "value", DATATYPE_INTEGER, true, 0, 2147483647);
        addElement("TileTransparencies", "Transparency", 0, 2147483647);
        addElement("TransparentTile", "TileTransparencies", CHILD_POLICY_EMPTY);
        addAttribute("TransparentTile", "x", DATATYPE_INTEGER, true, null);
        addAttribute("TransparentTile", "y", DATATYPE_INTEGER, true, null);
        addElement("TileOpacities", "Transparency", 0, 2147483647);
        addElement("OpaqueTile", "TileOpacities", CHILD_POLICY_EMPTY);
        addAttribute("OpaqueTile", "x", DATATYPE_INTEGER, true, null);
        addAttribute("OpaqueTile", "y", DATATYPE_INTEGER, true, null);
    }

    /** Siempre true. Ver la nota de la clase: no valida el nombre. */
    @Override
    public boolean canNodeAppear(String elementName, ImageTypeSpecifier imageType) {
        return true;
    }
}
