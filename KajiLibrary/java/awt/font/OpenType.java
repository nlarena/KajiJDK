package java.awt.font;

/**
 * Acceso crudo a las tablas de una fuente TrueType u OpenType.
 *
 * <p>Un archivo de fuente es una colección de tablas con nombre, y el nombre es un entero de cuatro
 * bytes que se lee como cuatro letras: `cmap` es `0x636D6170`. Esta interfaz devuelve esas tablas
 * **sin interpretar**, para que quien sepa leerlas lo haga.
 *
 * <p>Es la puerta de escape del modelo: todo lo demás en este paquete describe el texto en términos
 * de fuentes, glifos y medidas; esto entrega los bytes.
 *
 * <p>Los nombres se pueden pasar como entero o como cadena de cuatro caracteres, que es la misma
 * cosa escrita de dos maneras.
 */
public interface OpenType {

    /** La tabla con el mapa de carácter a glifo. */
    int TAG_CMAP = 0x636D6170;

    /** La tabla con la cabecera de la fuente. */
    int TAG_HEAD = 0x68656164;

    /** La tabla con los nombres de la fuente. */
    int TAG_NAME = 0x6E616D65;

    /** La tabla con los contornos de los glifos. */
    int TAG_GLYF = 0x676C7966;

    /** La tabla con los máximos del perfil. */
    int TAG_MAXP = 0x6D617870;

    /** La tabla con el programa de preparación de tamaño. */
    int TAG_PREP = 0x70726570;

    /** La tabla con las métricas horizontales. */
    int TAG_HMTX = 0x686D7478;

    /** La tabla con el ajuste entre pares. */
    int TAG_KERN = 0x6B65726E;

    /** La tabla con las métricas horizontales de dispositivo. */
    int TAG_HDMX = 0x68646D78;

    /** La tabla con el índice de posiciones de glifo. */
    int TAG_LOCA = 0x6C6F6361;

    /** La tabla con la información PostScript. */
    int TAG_POST = 0x706F7374;

    /** La tabla con las métricas de OS/2 y Windows. */
    int TAG_OS2 = 0x4F532F32;

    /** La tabla con los valores de control. */
    int TAG_CVT = 0x63767420;

    /** La tabla con los umbrales de suavizado por tamaño. */
    int TAG_GASP = 0x67617370;

    /** La tabla con las métricas verticales de dispositivo. */
    int TAG_VDMX = 0x56444D58;

    /** La tabla con las métricas verticales. */
    int TAG_VMTX = 0x766D7478;

    /** La tabla con la cabecera vertical. */
    int TAG_VHEA = 0x76686561;

    /** La tabla con la cabecera horizontal. */
    int TAG_HHEA = 0x68686561;

    /** La tabla con los datos Type 1. */
    int TAG_TYP1 = 0x74797031;

    /** La tabla con las líneas de base. */
    int TAG_BSLN = 0x62736C6E;

    /** La tabla con las sustituciones de glifo. */
    int TAG_GSUB = 0x47535542;

    /** La tabla con la firma digital. */
    int TAG_DSIG = 0x44534947;

    /** La tabla con el programa de la fuente. */
    int TAG_FPGM = 0x6670676D;

    /** La tabla con los ejes de variación. */
    int TAG_FVAR = 0x66766172;

    /** La tabla con las variaciones de glifo. */
    int TAG_GVAR = 0x67766172;

    /** La tabla con los contornos en formato compacto. */
    int TAG_CFF = 0x43464620;

    /** La tabla con los datos Multiple Master. */
    int TAG_MMSD = 0x4D4D5344;

    /** La tabla con las métricas Multiple Master. */
    int TAG_MMFX = 0x4D4D4658;

    /** La tabla con las líneas de base tipográficas. */
    int TAG_BASE = 0x42415345;

    /** La tabla con las definiciones de glifo. */
    int TAG_GDEF = 0x47444546;

    /** La tabla con las posiciones de glifo. */
    int TAG_GPOS = 0x47504F53;

    /** La tabla con la justificación. */
    int TAG_JSTF = 0x4A535446;

    /** La tabla con los datos de mapa de bits. */
    int TAG_EBDT = 0x45424454;

    /** La tabla con la ubicación de los mapas de bits. */
    int TAG_EBLC = 0x45424C43;

    /** La tabla con el escalado de mapas de bits. */
    int TAG_EBSC = 0x45425343;

    /** La tabla con el umbral de escalado lineal. */
    int TAG_LTSH = 0x4C545348;

    /** La tabla con los datos PCL 5. */
    int TAG_PCLT = 0x50434C54;

    /** La tabla con los glifos acentuados. */
    int TAG_ACNT = 0x61636E74;

    /** La tabla con la variación de ejes. */
    int TAG_AVAR = 0x61766172;

    /** La tabla con los datos de mapa de bits. */
    int TAG_BDAT = 0x62646174;

    /** La tabla con la ubicación de los mapas de bits. */
    int TAG_BLOC = 0x626C6F63;

    /** La tabla con la variación de los valores de control. */
    int TAG_CVAR = 0x63766172;

    /** La tabla con los nombres de característica. */
    int TAG_FEAT = 0x66656174;

    /** La tabla con la descripción de la fuente. */
    int TAG_FDSC = 0x66647363;

    /** La tabla con las métricas de la fuente. */
    int TAG_FMTX = 0x666D7478;

    /** La tabla con la justificación. */
    int TAG_JUST = 0x6A757374;

    /** La tabla con los cursores de ligadura. */
    int TAG_LCAR = 0x6C636172;

    /** La tabla con la reordenación de glifos. */
    int TAG_MORT = 0x6D6F7274;

    /** La tabla con los bordes ópticos. */
    int TAG_OPBD = 0x6F706264;

    /** La tabla con las propiedades de glifo. */
    int TAG_PROP = 0x70726F70;

    /** La tabla con el espaciado por tamaño. */
    int TAG_TRAK = 0x7472616B;

    /** La versión de la fuente, como un entero de punto fijo de 16.16. */
    int getVersion();

    /**
     * Una tabla entera.
     *
     * @return sus bytes, o `null` si la fuente no la trae
     */
    byte[] getFontTable(int sfntTag);

    /**
     * Una tabla entera, nombrada por sus cuatro letras.
     *
     * @return sus bytes, o `null` si la fuente no la trae
     */
    byte[] getFontTable(String strSfntTag);

    /**
     * Un tramo de una tabla.
     *
     * @return sus bytes, o `null` si la fuente no trae la tabla
     */
    byte[] getFontTable(int sfntTag, int offset, int count);

    /**
     * Un tramo de una tabla, nombrada por sus cuatro letras.
     *
     * @return sus bytes, o `null` si la fuente no trae la tabla
     */
    byte[] getFontTable(String strSfntTag, int offset, int count);

    /** Cuánto mide una tabla, o 0 si la fuente no la trae. */
    int getFontTableSize(int sfntTag);

    /** Cuánto mide una tabla, o 0 si la fuente no la trae. */
    int getFontTableSize(String strSfntTag);
}
