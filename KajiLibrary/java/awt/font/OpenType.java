package java.awt.font;

/**
 * Raw access to a TrueType or OpenType font's tables.
 *
 * <p>A font file is a collection of named tables, and the name is a four-byte integer read as four
 * letters: `cmap` is `0x636D6170`. This interface returns those tables **uninterpreted**, so that
 * whoever knows how to read them may do so.
 *
 * <p>It is the model's escape hatch: everything else in this package describes text in terms of
 * fonts, glyphs and measurements; this hands over the bytes.
 *
 * <p>The names may be passed as an integer or as a four-character string, which is the same thing
 * written two ways.
 */
public interface OpenType {

    /** The character-to-glyph map table. */
    int TAG_CMAP = 0x636D6170;

    /** The font header table. */
    int TAG_HEAD = 0x68656164;

    /** The font name table. */
    int TAG_NAME = 0x6E616D65;

    /** The glyph outline table. */
    int TAG_GLYF = 0x676C7966;

    /** The profile maxima table. */
    int TAG_MAXP = 0x6D617870;

    /** The size preparation program table. */
    int TAG_PREP = 0x70726570;

    /** The horizontal metrics table. */
    int TAG_HMTX = 0x686D7478;

    /** The kerning table. */
    int TAG_KERN = 0x6B65726E;

    /** The horizontal device metrics table. */
    int TAG_HDMX = 0x68646D78;

    /** The glyph location index table. */
    int TAG_LOCA = 0x6C6F6361;

    /** The PostScript information table. */
    int TAG_POST = 0x706F7374;

    /** The OS/2 and Windows metrics table. */
    int TAG_OS2 = 0x4F532F32;

    /** The control value table. */
    int TAG_CVT = 0x63767420;

    /** The per-size smoothing threshold table. */
    int TAG_GASP = 0x67617370;

    /** The vertical device metrics table. */
    int TAG_VDMX = 0x56444D58;

    /** The vertical metrics table. */
    int TAG_VMTX = 0x766D7478;

    /** The vertical header table. */
    int TAG_VHEA = 0x76686561;

    /** The horizontal header table. */
    int TAG_HHEA = 0x68686561;

    /** The Type 1 data table. */
    int TAG_TYP1 = 0x74797031;

    /** The baseline table. */
    int TAG_BSLN = 0x62736C6E;

    /** The glyph substitution table. */
    int TAG_GSUB = 0x47535542;

    /** The digital signature table. */
    int TAG_DSIG = 0x44534947;

    /** The font program table. */
    int TAG_FPGM = 0x6670676D;

    /** The variation axis table. */
    int TAG_FVAR = 0x66766172;

    /** The glyph variation table. */
    int TAG_GVAR = 0x67766172;

    /** The compact-format outline table. */
    int TAG_CFF = 0x43464620;

    /** The Multiple Master data table. */
    int TAG_MMSD = 0x4D4D5344;

    /** The Multiple Master metrics table. */
    int TAG_MMFX = 0x4D4D4658;

    /** The typographic baseline table. */
    int TAG_BASE = 0x42415345;

    /** The glyph definition table. */
    int TAG_GDEF = 0x47444546;

    /** The glyph positioning table. */
    int TAG_GPOS = 0x47504F53;

    /** The justification table. */
    int TAG_JSTF = 0x4A535446;

    /** The bitmap data table. */
    int TAG_EBDT = 0x45424454;

    /** The bitmap location table. */
    int TAG_EBLC = 0x45424C43;

    /** The bitmap scaling table. */
    int TAG_EBSC = 0x45425343;

    /** The linear threshold table. */
    int TAG_LTSH = 0x4C545348;

    /** The PCL 5 data table. */
    int TAG_PCLT = 0x50434C54;

    /** The accented glyph table. */
    int TAG_ACNT = 0x61636E74;

    /** The axis variation table. */
    int TAG_AVAR = 0x61766172;

    /** The bitmap data table. */
    int TAG_BDAT = 0x62646174;

    /** The bitmap location table. */
    int TAG_BLOC = 0x626C6F63;

    /** The control value variation table. */
    int TAG_CVAR = 0x63766172;

    /** The feature name table. */
    int TAG_FEAT = 0x66656174;

    /** The font description table. */
    int TAG_FDSC = 0x66647363;

    /** The font metrics table. */
    int TAG_FMTX = 0x666D7478;

    /** The justification table. */
    int TAG_JUST = 0x6A757374;

    /** The ligature caret table. */
    int TAG_LCAR = 0x6C636172;

    /** The glyph reordering table. */
    int TAG_MORT = 0x6D6F7274;

    /** The optical bounds table. */
    int TAG_OPBD = 0x6F706264;

    /** The glyph properties table. */
    int TAG_PROP = 0x70726F70;

    /** The tracking table. */
    int TAG_TRAK = 0x7472616B;

    /** The font's version, as a 16.16 fixed-point integer. */
    int getVersion();

    /**
     * A whole table.
     *
     * @return its bytes, or `null` if the font does not carry it
     */
    byte[] getFontTable(int sfntTag);

    /**
     * A whole table, named by its four letters.
     *
     * @return its bytes, or `null` if the font does not carry it
     */
    byte[] getFontTable(String strSfntTag);

    /**
     * A stretch of a table.
     *
     * @return its bytes, or `null` if the font does not carry the table
     */
    byte[] getFontTable(int sfntTag, int offset, int count);

    /**
     * A stretch of a table, named by its four letters.
     *
     * @return its bytes, or `null` if the font does not carry the table
     */
    byte[] getFontTable(String strSfntTag, int offset, int count);

    /** How much a table measures, or 0 if the font does not carry it. */
    int getFontTableSize(int sfntTag);

    /** How much a table measures, or 0 if the font does not carry it. */
    int getFontTableSize(String strSfntTag);
}
