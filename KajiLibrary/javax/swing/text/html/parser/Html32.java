package javax.swing.text.html.parser;

import java.util.StringTokenizer;
import java.util.Vector;

/**
 * The HTML 3.2 DTD, written as data.
 *
 * <h2>Why a table and not a file</h2>
 *
 * <p>The JDK keeps this same DTD in a binary file it loads from its image. Here it goes as text
 * inside the class: there is no resource to look up, nor to package, nor that can be lost when
 * building a trimmed image. The cost is that the class is long; in exchange, the library depends
 * on nothing external to parse HTML.
 *
 * <p>The data is the HTML 3.2 DTD, which is a public W3C specification. It is verified against
 * the JDK: {@code java/texto/Dtd1.java} reads the JDK's binary file with {@link DTD#read} and
 * {@code java/texto/Dtd2.java} compares this table against the result, element by element and
 * entity by entity.
 *
 * <h2>The order matters</h2>
 *
 * <p>{@link #NAMES} is walked first and creates every element empty, before defining any. It is
 * not for tidiness: {@link DTD#getElement(String)} numbers each new element in the order it
 * appears, and the exclusions and inclusions are stored as sets of those numbers. If an element
 * were born at another moment, every set would point at something else.
 */
final class Html32 {

    private Html32() {
    }

    /** The elements, in the exact order in which they have to be created; see the class note. */
    private static final String[] NAMES = {
        "#pcdata", "html", "meta", "base", "isindex", "head", "body", "applet", "param", "p",
        "title", "style", "link", "script", "unknown", "plaintext", "nextid", "noscript", "h1",
        "h2", "h3", "h4", "h5", "h6", "ul", "ol", "dir", "menu", "pre", "xmp", "listing", "dl",
        "div", "center", "blockquote", "form", "hr", "table", "object", "tt", "i", "b", "u",
        "strike", "s", "big", "small", "sub", "sup", "em", "strong", "dfn", "code", "samp", "kbd",
        "var", "cite", "a", "img", "font", "basefont", "br", "map", "nobr", "wbr", "blink",
        "span", "input", "select", "textarea", "address", "frameset", "noframes", "nohotjava",
        "animate", "tr", "td", "area", "option", "dt", "dd", "li", "caption", "frame", "th",
        "thead", "tfoot", "tbody"
    };

    /** One entity per line: name, type and the codes of its characters. */
    private static final String[] ENTITIES = {
        "#RE\u000965536\u000913", "#RS\u000965536\u000910", "#SPACE\u000965536\u000932",
        "AElig\u000965537\u0009198", "Aacute\u000965537\u0009193", "Acirc\u000965537\u0009194",
        "Agrave\u000965537\u0009192", "Alpha\u000965537\u0009913", "Aring\u000965537\u0009197",
        "Atilde\u000965537\u0009195", "Auml\u000965537\u0009196", "Beta\u000965537\u0009914",
        "Ccedil\u000965537\u0009199", "Chi\u000965537\u0009935", "Dagger\u000965537\u00098225",
        "Delta\u000965537\u0009916", "ETH\u000965537\u0009208", "Eacute\u000965537\u0009201",
        "Ecirc\u000965537\u0009202", "Egrave\u000965537\u0009200", "Epsilon\u000965537\u0009917",
        "Eta\u000965537\u0009919", "Euml\u000965537\u0009203", "Gamma\u000965537\u0009915",
        "Iacute\u000965537\u0009205", "Icirc\u000965537\u0009206", "Igrave\u000965537\u0009204",
        "Iota\u000965537\u0009921", "Iuml\u000965537\u0009207", "Kappa\u000965537\u0009922",
        "Lambda\u000965537\u0009923", "Mu\u000965537\u0009924", "Ntilde\u000965537\u0009209",
        "Nu\u000965537\u0009925", "OElig\u000965537\u0009338", "Oacute\u000965537\u0009211",
        "Ocirc\u000965537\u0009212", "Ograve\u000965537\u0009210", "Omega\u000965537\u0009937",
        "Omicron\u000965537\u0009927", "Oslash\u000965537\u0009216", "Otilde\u000965537\u0009213",
        "Ouml\u000965537\u0009214", "Phi\u000965537\u0009934", "Pi\u000965537\u0009928",
        "Prime\u000965537\u00098243", "Psi\u000965537\u0009936", "Rho\u000965537\u0009929",
        "Scaron\u000965537\u0009352", "Sigma\u000965537\u0009931", "THORN\u000965537\u0009222",
        "Tau\u000965537\u0009932", "Theta\u000965537\u0009920", "Uacute\u000965537\u0009218",
        "Ucirc\u000965537\u0009219", "Ugrave\u000965537\u0009217", "Upsilon\u000965537\u0009933",
        "Uuml\u000965537\u0009220", "Xi\u000965537\u0009926", "Yacute\u000965537\u0009221",
        "Yuml\u000965537\u0009376", "Zeta\u000965537\u0009918", "aacute\u000965537\u0009225",
        "acirc\u000965537\u0009226", "acute\u000965537\u0009180", "aelig\u000965537\u0009230",
        "agrave\u000965537\u0009224", "alefsym\u000965537\u00098501", "alpha\u000965537\u0009945",
        "amp\u000965537\u000938", "and\u000965537\u00098743", "ang\u000965537\u00098736",
        "aring\u000965537\u0009229", "asymp\u000965537\u00098776", "atilde\u000965537\u0009227",
        "auml\u000965537\u0009228", "bdquo\u000965537\u00098222", "beta\u000965537\u0009946",
        "brvbar\u000965537\u0009166", "bull\u000965537\u00098226", "cap\u000965537\u00098745",
        "ccedil\u000965537\u0009231", "cedil\u000965537\u0009184", "cent\u000965537\u0009162",
        "chi\u000965537\u0009967", "circ\u000965537\u0009710", "clubs\u000965537\u00099827",
        "cong\u000965537\u00098773", "copy\u000965537\u0009169", "crarr\u000965537\u00098629",
        "cup\u000965537\u00098746", "curren\u000965537\u0009164", "dArr\u000965537\u00098659",
        "dagger\u000965537\u00098224", "darr\u000965537\u00098595", "deg\u000965537\u0009176",
        "delta\u000965537\u0009948", "diams\u000965537\u00099830", "divide\u000965537\u0009247",
        "eacute\u000965537\u0009233", "ecirc\u000965537\u0009234", "egrave\u000965537\u0009232",
        "empty\u000965537\u00098709", "emsp\u000965537\u00098195", "ensp\u000965537\u00098194",
        "epsilon\u000965537\u0009949", "equiv\u000965537\u00098801", "eta\u000965537\u0009951",
        "eth\u000965537\u0009240", "euml\u000965537\u0009235", "euro\u000965537\u00098364",
        "exist\u000965537\u00098707", "fnof\u000965537\u0009402", "forall\u000965537\u00098704",
        "frac12\u000965537\u0009189", "frac14\u000965537\u0009188", "frac34\u000965537\u0009190",
        "frasl\u000965537\u00098260", "gamma\u000965537\u0009947", "ge\u000965537\u00098805",
        "gt\u000965537\u000962", "hArr\u000965537\u00098660", "harr\u000965537\u00098596",
        "hearts\u000965537\u00099829", "hellip\u000965537\u00098230",
        "iacute\u000965537\u0009237", "icirc\u000965537\u0009238", "iexcl\u000965537\u0009161",
        "igrave\u000965537\u0009236", "image\u000965537\u00098465", "infin\u000965537\u00098734",
        "int\u000965537\u00098747", "iota\u000965537\u0009953", "iquest\u000965537\u0009191",
        "isin\u000965537\u00098712", "iuml\u000965537\u0009239", "kappa\u000965537\u0009954",
        "lArr\u000965537\u00098656", "lambda\u000965537\u0009955", "lang\u000965537\u00099001",
        "laquo\u000965537\u0009171", "larr\u000965537\u00098592", "lceil\u000965537\u00098968",
        "ldquo\u000965537\u00098220", "le\u000965537\u00098804", "lfloor\u000965537\u00098970",
        "lowast\u000965537\u00098727", "loz\u000965537\u00099674", "lrm\u000965537\u00098206",
        "lsaquo\u000965537\u00098249", "lsquo\u000965537\u00098216", "lt\u000965537\u000960",
        "macr\u000965537\u0009175", "mdash\u000965537\u00098212", "micro\u000965537\u0009181",
        "middot\u000965537\u0009183", "minus\u000965537\u00098722", "mu\u000965537\u0009956",
        "nabla\u000965537\u00098711", "nbsp\u000965537\u0009160", "ndash\u000965537\u00098211",
        "ne\u000965537\u00098800", "ni\u000965537\u00098715", "not\u000965537\u0009172",
        "notin\u000965537\u00098713", "nsub\u000965537\u00098836", "ntilde\u000965537\u0009241",
        "nu\u000965537\u0009957", "oacute\u000965537\u0009243", "ocirc\u000965537\u0009244",
        "oelig\u000965537\u0009339", "ograve\u000965537\u0009242", "oline\u000965537\u00098254",
        "omega\u000965537\u0009969", "omicron\u000965537\u0009959", "oplus\u000965537\u00098853",
        "or\u000965537\u00098744", "ordf\u000965537\u0009170", "ordm\u000965537\u0009186",
        "oslash\u000965537\u0009248", "otilde\u000965537\u0009245", "otimes\u000965537\u00098855",
        "ouml\u000965537\u0009246", "para\u000965537\u0009182", "part\u000965537\u00098706",
        "permil\u000965537\u00098240", "perp\u000965537\u00098869", "phi\u000965537\u0009966",
        "pi\u000965537\u0009960", "piv\u000965537\u0009982", "plusmn\u000965537\u0009177",
        "pound\u000965537\u0009163", "prime\u000965537\u00098242", "prod\u000965537\u00098719",
        "prop\u000965537\u00098733", "psi\u000965537\u0009968", "quot\u000965537\u000934",
        "rArr\u000965537\u00098658", "radic\u000965537\u00098730", "rang\u000965537\u00099002",
        "raquo\u000965537\u0009187", "rarr\u000965537\u00098594", "rceil\u000965537\u00098969",
        "rdquo\u000965537\u00098221", "real\u000965537\u00098476", "reg\u000965537\u0009174",
        "rfloor\u000965537\u00098971", "rho\u000965537\u0009961", "rlm\u000965537\u00098207",
        "rsaquo\u000965537\u00098250", "rsquo\u000965537\u00098217", "sbquo\u000965537\u00098218",
        "scaron\u000965537\u0009353", "sdot\u000965537\u00098901", "sect\u000965537\u0009167",
        "shy\u000965537\u0009173", "sigma\u000965537\u0009963", "sigmaf\u000965537\u0009962",
        "sim\u000965537\u00098764", "spades\u000965537\u00099824", "sub\u000965537\u00098834",
        "sube\u000965537\u00098838", "sum\u000965537\u00098721", "sup\u000965537\u00098835",
        "sup1\u000965537\u0009185", "sup2\u000965537\u0009178", "sup3\u000965537\u0009179",
        "supe\u000965537\u00098839", "szlig\u000965537\u0009223", "tau\u000965537\u0009964",
        "there4\u000965537\u00098756", "theta\u000965537\u0009952",
        "thetasym\u000965537\u0009977", "thinsp\u000965537\u00098201",
        "thorn\u000965537\u0009254", "tilde\u000965537\u0009732", "times\u000965537\u0009215",
        "trade\u000965537\u00098482", "uArr\u000965537\u00098657", "uacute\u000965537\u0009250",
        "uarr\u000965537\u00098593", "ucirc\u000965537\u0009251", "ugrave\u000965537\u0009249",
        "uml\u000965537\u0009168", "upsih\u000965537\u0009978", "upsilon\u000965537\u0009965",
        "uuml\u000965537\u0009252", "weierp\u000965537\u00098472", "xi\u000965537\u0009958",
        "yacute\u000965537\u0009253", "yen\u000965537\u0009165", "yuml\u000965537\u0009255",
        "zeta\u000965537\u0009950", "zwj\u000965537\u00098205", "zwnj\u000965537\u00098204"
    };

    /**
     * One element per entry.
     *
     * <p>The first line is the element -- name, type, whether the opening may be omitted, whether
     * the closing may be omitted, exclusions, inclusions and content model -- and those that follow
     * are its attributes.
     *
     * <p>The model goes in the form {@code type:content:next}, where the content is {@code E} and
     * an element name, or {@code M(...)} with another model inside. It is the same shape as
     * {@link ContentModel}'s tree, written on one line.
     */
    private static final String[] ELEMENTS = {
        "#pcdata\u000919\u0009false\u0009false\u0009\u0009\u0009-",
        "html\u000918\u0009true\u0009true\u0009\u0009\u000944:M(0:Ehead:0:Ebody:63:M(0:Eplaintext:-):-):-\nversion\u00091\u00091\u0009-//HotJava//DTD HotJava 1.0 HTML 3.2 Draft 19960821//EN\u0009",
        "meta\u000917\u0009false\u0009true\u0009\u0009\u0009-\ncontent\u00091\u00092\u0009 \u0009\nname\u00097\u00095\u0009 \u0009\nhttp-equiv\u00097\u00095\u0009 \u0009",
        "base\u000917\u0009false\u0009true\u0009\u0009\u0009-\ntarget\u00091\u00095\u0009 \u0009\nhref\u00091\u00095\u0009 \u0009",
        "isindex\u000917\u0009false\u0009true\u0009\u0009\u0009-\nprompt\u00091\u00095\u0009 \u0009",
        "head\u000918\u0009true\u0009true\u0009\u0009meta,style,link,script,noscript,\u000938:M(63:M(0:Etitle:-):63:M(0:Eisindex:-):63:M(0:Ebase:-):63:M(0:Enextid:-):-):-",
        "body\u000918\u0009true\u0009true\u0009\u0009\u000942:M(124:M(0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eaddress:0:Eframeset:0:Enoframes:0:Enohotjava:0:Eanimate:-):-):-\nheight\u000914\u00095\u0009 \u0009\nwidth\u000914\u00095\u0009 \u0009\nalink\u00091\u00095\u0009 \u0009\nvlink\u00091\u00095\u0009 \u0009\nlink\u00091\u00095\u0009 \u0009\ntext\u00091\u00095\u0009 \u0009\nbgcolor\u00091\u00095\u0009 \u0009\nbackground\u00091\u00095\u0009 \u0009",
        "applet\u000918\u0009false\u0009false\u0009\u0009param,\u000942:M(124:M(42:M(124:M(0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:0:Einput:0:Eselect:0:Etextarea:0:Etr:0:Etd:-):-):-\nvspace\u00091\u00095\u0009 \u0009\nhspace\u00091\u00095\u0009 \u0009\nalign\u00099\u00090\u0009baseline\u0009top|middle|bottom|left|right|texttop|absmiddle|baseline|absbottom|\nheight\u00091\u00092\u0009 \u0009\nwidth\u00091\u00092\u0009 \u0009\narchive\u00091\u00095\u0009 \u0009\nname\u00091\u00095\u0009 \u0009\nalt\u00091\u00095\u0009 \u0009\ncode\u00091\u00095\u0009 \u0009\ncodebase\u00091\u00095\u0009 \u0009",
        "param\u000917\u0009false\u0009true\u0009\u0009\u0009-\ntype\u00091\u00095\u0009 \u0009\nvaluetype\u00099\u00090\u0009DATA\u0009data|ref|object|\nvalue\u00091\u00095\u0009 \u0009\nname\u00097\u00092\u0009 \u0009",
        "p\u000918\u0009false\u0009true\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):-\nalign\u00099\u00095\u0009 \u0009left|center|right|",
        "title\u000918\u0009false\u0009false\u0009meta,style,link,script,noscript,\u0009\u000942:M(44:M(42:M(0:E#pcdata:-):-):-):-",
        "style\u000918\u0009false\u0009false\u0009meta,style,link,script,noscript,\u0009\u000942:M(44:M(42:M(0:E#pcdata:-):-):-):-",
        "link\u000917\u0009false\u0009true\u0009\u0009\u0009-\ntitle\u00091\u00095\u0009 \u0009\nrev\u00091\u00095\u0009 \u0009\nrel\u00091\u00095\u0009 \u0009\nhref\u00091\u00095\u0009 \u0009\nid\u00094\u00095\u0009 \u0009",
        "script\u000918\u0009false\u0009false\u0009meta,style,link,script,noscript,\u0009\u000942:M(124:M(0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eaddress:0:Eframeset:0:Enoframes:0:Enohotjava:0:Eanimate:-):-):-\nlanguage\u00091\u00095\u0009 \u0009",
        "unknown\u000917\u0009false\u0009true\u0009\u0009\u0009-",
        "plaintext\u00091\u0009false\u0009true\u0009\u0009\u0009-",
        "nextid\u000917\u0009false\u0009true\u0009\u0009\u0009-\nn\u00091\u00092\u0009 \u0009",
        "noscript\u000918\u0009false\u0009false\u0009\u0009\u000943:M(124:M(0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:-):-):-",
        "h1\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Eimg:0:Ebr:0:Ehr:0:Ecenter:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):-\nalign\u00099\u00095\u0009 \u0009left|center|right|",
        "h2\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Eimg:0:Ebr:0:Ehr:0:Ecenter:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):-\nalign\u00099\u00095\u0009 \u0009left|center|right|",
        "h3\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Eimg:0:Ebr:0:Ehr:0:Ecenter:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):-\nalign\u00099\u00095\u0009 \u0009left|center|right|",
        "h4\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Eimg:0:Ebr:0:Ehr:0:Ecenter:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):-\nalign\u00099\u00095\u0009 \u0009left|center|right|",
        "h5\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Eimg:0:Ebr:0:Ehr:0:Ecenter:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):-\nalign\u00099\u00095\u0009 \u0009left|center|right|",
        "h6\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Eimg:0:Ebr:0:Ehr:0:Ecenter:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):-\nalign\u00099\u00095\u0009 \u0009left|center|right|",
        "ul\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Ep:0:Ebr:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eol:0:Eul:0:Eli:-):-):-\ncompact\u00099\u00095\u0009compact\u0009compact|\ntype\u00099\u00095\u0009 \u0009disc|square|circle|",
        "ol\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Ep:0:Ebr:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eol:0:Eul:0:Eli:-):-):-\ncompact\u00099\u00095\u0009compact\u0009compact|\nstart\u000912\u00095\u0009 \u0009\ntype\u00091\u00090\u00091\u0009",
        "dir\u000918\u0009false\u0009false\u0009isindex,plaintext,pre,xmp,listing,dl,div,center,blockquote,form,hr,table,object,\u0009p,\u000942:M(124:M(0:Eul:0:Eol:0:Edir:0:Emenu:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eli:-):-):-\ncompact\u00099\u00095\u0009compact\u0009compact|",
        "menu\u000918\u0009false\u0009false\u0009isindex,plaintext,pre,xmp,listing,dl,div,center,blockquote,form,hr,table,object,\u0009p,\u000942:M(124:M(0:Eul:0:Eol:0:Edir:0:Emenu:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eli:-):-):-\ncompact\u00099\u00095\u0009compact\u0009compact|",
        "pre\u000918\u0009false\u0009false\u0009\u0009p,hr,\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):-\nwidth\u000912\u00095\u0009 \u0009",
        "xmp\u00091\u0009false\u0009false\u0009\u0009\u0009-",
        "listing\u00091\u0009false\u0009false\u0009\u0009\u0009-",
        "dl\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Ep:0:Edl:0:Edt:0:Edd:-):-):-\ncompact\u00099\u00095\u0009compact\u0009compact|",
        "div\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eaddress:0:Eframeset:0:Enoframes:0:Enohotjava:0:Eanimate:-):-):-\nalign\u00099\u00090\u0009left\u0009left|center|right|",
        "center\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eaddress:0:Eframeset:0:Enoframes:0:Enohotjava:0:Eanimate:-):-):-",
        "blockquote\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eaddress:0:Eframeset:0:Enoframes:0:Enohotjava:0:Eanimate:-):-):-",
        "form\u000918\u0009false\u0009false\u0009form,\u0009\u000942:M(124:M(0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eaddress:0:Eframeset:0:Enoframes:0:Enohotjava:0:Eanimate:-):-):-\nenctype\u00091\u00090\u0009application/x-www-form-urlencoded\u0009\nmethod\u00099\u00090\u0009GET\u0009get|post|\naction\u00091\u00095\u0009 \u0009",
        "hr\u000917\u0009false\u0009true\u0009\u0009\u0009-\nwidth\u00091\u00095\u0009 \u0009\nsize\u00091\u00095\u0009 \u0009\nnoshade\u00099\u00095\u0009noshade\u0009noshade|\nalign\u00099\u00095\u0009 \u0009left|right|center|",
        "table\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Etr:0:Ecaption:-):-):-\nbgcolor\u00091\u00095\u0009 \u0009\ncellpadding\u00091\u00095\u0009 \u0009\ncellspacing\u00091\u00095\u0009 \u0009\nborder\u00091\u00095\u0009 \u0009\nwidth\u00091\u00095\u0009 \u0009\nalign\u00099\u00095\u0009 \u0009left|center|right|",
        "object\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Eparam:42:M(124:M(0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:42:M(124:M(0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):0:Eaddress:-):-):0:Einput:0:Eselect:0:Etextarea:-):-):-\nname\u00091\u00095\u0009 \u0009\nshapes\u00099\u00095\u0009shapes\u0009shapes|\nusemap\u00091\u00095\u0009 \u0009\nvspace\u00091\u00095\u0009 \u0009\nhspace\u00091\u00095\u0009 \u0009\nborder\u00091\u00095\u0009 \u0009\nwidth\u00091\u00095\u0009 \u0009\nheight\u00091\u00095\u0009 \u0009\nalign\u00099\u00095\u0009 \u0009top|middle|bottom|left|right|texttop|absmiddle|baseline|absbottom|\nstandby\u00091\u00095\u0009 \u0009\ncodetype\u00091\u00095\u0009 \u0009\ntype\u00091\u00095\u0009 \u0009\ndata\u00091\u00095\u0009 \u0009\ncodebase\u00091\u00095\u0009 \u0009\nclassid\u00091\u00095\u0009 \u0009\ndeclare\u00099\u00095\u0009declare\u0009declare|\ndir\u00099\u00095\u0009 \u0009ltr|rtl|\nlang\u00097\u00095\u0009 \u0009\nstyle\u00091\u00095\u0009 \u0009\nclass\u00091\u00095\u0009 \u0009\nid\u00094\u00095\u0009 \u0009",
        "tt\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "i\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "b\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "u\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "strike\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "s\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "big\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "small\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "sub\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "sup\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "em\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "strong\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "dfn\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "code\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "samp\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "kbd\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "var\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "cite\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-",
        "a\u000918\u0009false\u0009false\u0009a,\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:0:Epre:-):-):-\nismap\u00099\u00095\u0009ismap\u0009ismap|\ncoords\u00091\u00095\u0009 \u0009\nshape\u00099\u00095\u0009 \u0009rect|circle|poly|default|\ntarget\u00091\u00095\u0009 \u0009\ntitle\u00091\u00095\u0009 \u0009\nrev\u00091\u00095\u0009 \u0009\nrel\u00091\u00095\u0009 \u0009\nhref\u00091\u00095\u0009 \u0009\nname\u00091\u00095\u0009 \u0009",
        "img\u000917\u0009false\u0009true\u0009\u0009\u0009-\nlowsrc\u00091\u00095\u0009 \u0009\nismap\u00099\u00095\u0009ismap\u0009ismap|\nusemap\u00091\u00095\u0009 \u0009\nvspace\u00091\u00095\u0009 \u0009\nhspace\u00091\u00095\u0009 \u0009\nborder\u00091\u00095\u0009 \u0009\nwidth\u00091\u00095\u0009 \u0009\nheight\u00091\u00095\u0009 \u0009\nalign\u00099\u00090\u0009baseline\u0009top|middle|bottom|left|right|texttop|absmiddle|baseline|absbottom|center|\nalt\u00091\u00095\u0009 \u0009\nsrc\u00091\u00092\u0009 \u0009",
        "font\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Ecenter:0:Ep:0:Edl:0:Eul:0:Eol:-):-):-\ncolor\u00091\u00095\u0009 \u0009\nsize\u00091\u00095\u0009 \u0009",
        "basefont\u000917\u0009false\u0009true\u0009\u0009\u0009-\nsize\u00091\u00095\u0009 \u0009",
        "br\u000917\u0009false\u0009true\u0009\u0009\u0009-\nclear\u00099\u00090\u0009none\u0009left|all|right|none|",
        "map\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Ebase:0:Earea:-):-):-\nname\u00091\u00095\u0009 \u0009",
        "nobr\u000918\u0009false\u0009false\u0009\u0009\u000943:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):-",
        "wbr\u000917\u0009false\u0009true\u0009\u0009\u0009-",
        "blink\u000919\u0009false\u0009false\u0009\u0009\u0009-",
        "span\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):-",
        "input\u000917\u0009false\u0009true\u0009\u0009\u0009-\nalign\u00099\u00090\u0009top\u0009top|middle|bottom|left|right|\nsrc\u00091\u00095\u0009 \u0009\nmaxlength\u000912\u00095\u0009 \u0009\nsize\u00091\u00095\u0009 \u0009\nchecked\u00099\u00095\u0009checked\u0009checked|\nborder\u00091\u00095\u0009 \u0009\nvalue\u00091\u00095\u0009 \u0009\nname\u00091\u00095\u0009 \u0009\ntype\u00099\u00090\u0009TEXT\u0009text|password|checkbox|radio|submit|reset|file|hidden|image|",
        "select\u000918\u0009false\u0009false\u0009\u0009\u000944:M(43:M(0:Eoption:-):-):-\nmultiple\u00099\u00095\u0009multiple\u0009multiple|\nsize\u000912\u00095\u0009 \u0009\nname\u00091\u00092\u0009 \u0009",
        "textarea\u000918\u0009false\u0009false\u0009\u0009\u000942:M(44:M(42:M(0:E#pcdata:-):-):-):-\ncols\u000912\u00092\u0009 \u0009\nrows\u000912\u00092\u0009 \u0009\nname\u00091\u00092\u0009 \u0009",
        "address\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(124:M(42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):0:Ep:-):-):-",
        "frameset\u000918\u0009false\u0009true\u0009\u0009\u000942:M(124:M(0:Eframeset:0:Eframe:0:Enoframes:-):-):-\ncols\u00091\u00095\u0009 \u0009\nrows\u00091\u00095\u0009 \u0009",
        "noframes\u00091\u0009false\u0009false\u0009\u0009\u0009-",
        "nohotjava\u00091\u0009false\u0009false\u0009\u0009\u0009-",
        "animate\u00091\u0009false\u0009false\u0009\u0009\u0009-",
        "tr\u000918\u0009false\u0009true\u0009\u0009\u000942:M(124:M(0:Etd:0:Eth:0:Escript:0:Emap:-):-):-\nbgcolor\u00091\u00095\u0009 \u0009\nvalign\u00099\u00095\u0009 \u0009top|middle|bottom|baseline|\nalign\u00099\u00095\u0009 \u0009left|center|right|",
        "td\u000918\u0009false\u0009true\u0009\u0009\u000942:M(124:M(0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eaddress:0:Eframeset:0:Enoframes:0:Enohotjava:0:Eanimate:-):-):-\nheight\u00091\u00095\u0009 \u0009\nwidth\u00091\u00095\u0009 \u0009\nvalign\u00099\u00095\u0009 \u0009top|middle|bottom|baseline|\nalign\u00099\u00095\u0009 \u0009left|center|right|\ncolspan\u000912\u00090\u00091\u0009\nrowspan\u000912\u00090\u00091\u0009\nbgcolor\u00091\u00095\u0009 \u0009\nnowrap\u00099\u00095\u0009nowrap\u0009nowrap|",
        "area\u000917\u0009false\u0009true\u0009\u0009\u0009-\ntarget\u00091\u00095\u0009 \u0009\nalt\u00091\u00095\u0009 \u0009\nnohref\u00099\u00095\u0009nohref\u0009nohref|\nhref\u00091\u00095\u0009 \u0009\ncoords\u00091\u00095\u0009 \u0009\nshape\u00099\u00090\u0009rect\u0009rect|circle|poly|default|",
        "option\u000918\u0009false\u0009true\u0009\u0009\u000942:M(44:M(42:M(0:E#pcdata:-):-):-):-\nvalue\u00091\u00095\u0009 \u0009\nselected\u00099\u00095\u0009selected\u0009selected|",
        "dt\u000918\u0009false\u0009true\u0009\u0009\u000942:M(124:M(0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):-",
        "dd\u000918\u0009false\u0009true\u0009\u0009\u000942:M(124:M(0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):-",
        "li\u000918\u0009false\u0009true\u0009\u0009\u000942:M(124:M(42:M(124:M(0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:-):-):0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:-):-):-\nvalue\u000912\u00095\u0009 \u0009\ntype\u00091\u00095\u0009 \u0009",
        "caption\u000918\u0009false\u0009false\u0009\u0009\u000942:M(124:M(0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eaddress:0:Eframeset:0:Enoframes:0:Enohotjava:0:Eanimate:-):-):-\nalign\u00099\u00090\u0009top\u0009top|bottom|",
        "frame\u000917\u0009false\u0009true\u0009\u0009\u0009-\nnoresize\u00099\u00095\u0009noresize\u0009noresize|\nscrolling\u00099\u00090\u0009AUTO\u0009yes|no|auto|\nmarginheight\u000912\u00095\u0009 \u0009\nmarginwidth\u000912\u00095\u0009 \u0009\nframeborder\u000912\u00090\u00091\u0009\nname\u00091\u00095\u0009 \u0009\nsrc\u00091\u00095\u0009 \u0009",
        "th\u000918\u0009false\u0009true\u0009\u0009\u000942:M(124:M(0:Eh1:0:Eh2:0:Eh3:0:Eh4:0:Eh5:0:Eh6:0:Ep:0:Eul:0:Eol:0:Edir:0:Emenu:0:Epre:0:Exmp:0:Elisting:0:Eplaintext:0:Edl:0:Ediv:0:Ecenter:0:Enoscript:0:Eblockquote:0:Eform:0:Eisindex:0:Ehr:0:Etable:0:Eobject:42:M(0:E#pcdata:-):0:Ett:0:Ei:0:Eb:0:Eu:0:Estrike:0:Es:0:Ebig:0:Esmall:0:Esub:0:Esup:0:Eem:0:Estrong:0:Edfn:0:Ecode:0:Esamp:0:Ekbd:0:Evar:0:Ecite:0:Ea:0:Eimg:0:Eapplet:0:Eobject:0:Efont:0:Ebasefont:0:Ebr:0:Escript:0:Emap:0:Enobr:0:Ewbr:0:Eblink:0:Espan:0:Einput:0:Eselect:0:Etextarea:0:Eaddress:0:Eframeset:0:Enoframes:0:Enohotjava:0:Eanimate:-):-):-\nheight\u00091\u00095\u0009 \u0009\nwidth\u00091\u00095\u0009 \u0009\nvalign\u00099\u00095\u0009 \u0009top|middle|bottom|baseline|\nalign\u00099\u00095\u0009 \u0009left|center|right|\ncolspan\u000912\u00090\u00091\u0009\nrowspan\u000912\u00090\u00091\u0009\nbgcolor\u00091\u00095\u0009 \u0009\nnowrap\u00099\u00095\u0009nowrap\u0009nowrap|",
        "thead\u000918\u0009false\u0009true\u0009\u0009\u000943:M(44:M(0:Etr:-):-):-",
        "tfoot\u000918\u0009false\u0009true\u0009\u0009\u000943:M(44:M(0:Etr:-):-):-",
        "tbody\u000918\u0009true\u0009true\u0009\u0009\u000943:M(44:M(0:Etr:-):-):-"
    };

    /** Builds the HTML 3.2 DTD over the one passed in. */
    static void fill(DTD d) {
        for (int i = 0; i < NAMES.length; i++) {
            d.getElement(NAMES[i]);
        }
        for (int i = 0; i < ENTITIES.length; i++) {
            String[] p = partir(ENTITIES[i], '\t');
            char[] data = codes(p[2]);
            d.defineEntity(p[0], Integer.parseInt(p[1]), data);
        }
        for (int i = 0; i < ELEMENTS.length; i++) {
            String[] rows = partir(ELEMENTS[i], '\n');
            String[] p = partir(rows[0], '\t');
            AttributeList atts = null;
            // Backwards: the list is chained forwards, as when reading the binary.
            for (int j = rows.length - 1; j >= 1; j--) {
                String[] a = partir(rows[j], '\t');
                Vector<String> vals = null;
                if (a[4].length() > 0) {
                    vals = new Vector<String>();
                    StringTokenizer st = new StringTokenizer(a[4], "|");
                    while (st.hasMoreTokens()) {
                        vals.addElement(st.nextToken());
                    }
                }
                String value = " ".equals(a[3]) ? null : a[3];
                atts = new AttributeList(a[0], Integer.parseInt(a[1]),
                        Integer.parseInt(a[2]), value, vals, atts);
            }
            d.defineElement(p[0], Integer.parseInt(p[1]), "true".equals(p[2]),
                    "true".equals(p[3]), model(d, p[6], new int[1]),
                    set(d, p[4]), set(d, p[5]), atts);
        }
    }

    /** Splits on a character, keeping the empty fields; {@code split} would discard them. */
    private static String[] partir(String s, char sep) {
        int n = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == sep) {
                n++;
            }
        }
        String[] out = new String[n];
        int k = 0;
        int from = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == sep) {
                out[k] = s.substring(from, i);
                k++;
                from = i + 1;
            }
        }
        return out;
    }

    /** The characters of a comma-separated list of codes. */
    private static char[] codes(String s) {
        if (s.length() == 0) {
            return new char[0];
        }
        String[] p = partir(s, ',');
        char[] out = new char[p.length];
        for (int i = 0; i < p.length; i++) {
            out[i] = (char) Integer.parseInt(p[i]);
        }
        return out;
    }

    /** The set of elements named in that list, or null if it is empty. */
    private static java.util.BitSet set(DTD d, String s) {
        if (s.length() == 0) {
            return null;
        }
        java.util.BitSet b = new java.util.BitSet();
        StringTokenizer st = new StringTokenizer(s, ",");
        while (st.hasMoreTokens()) {
            b.set(d.getElement(st.nextToken()).getIndex());
        }
        return b;
    }

    /**
     * Rebuilds a content model from its flat form.
     *
     * <p>The {@code pos} integer goes in and comes out with the position: it is a cursor shared by
     * the recursive calls, and it is what allows reading a tree in a single pass over the string.
     */
    private static ContentModel model(DTD d, String s, int[] pos) {
        if (pos[0] >= s.length()) {
            return null;
        }
        if (s.charAt(pos[0]) == '-') {
            pos[0] = pos[0] + 1;
            return null;
        }
        int colon = s.indexOf(':', pos[0]);
        int type = Integer.parseInt(s.substring(pos[0], colon));
        pos[0] = colon + 1;

        Object content;
        if (s.charAt(pos[0]) == 'E') {
            int end = s.indexOf(':', pos[0]);
            content = d.getElement(s.substring(pos[0] + 1, end));
            pos[0] = end + 1;
        } else {
            // "M(" ... ")" -- the closing parenthesis is found by counting those that open.
            int level = 0;
            int i = pos[0] + 1;
            int from = i + 1;
            for (; i < s.length(); i++) {
                if (s.charAt(i) == '(') {
                    level++;
                } else if (s.charAt(i) == ')') {
                    level--;
                    if (level == 0) {
                        break;
                    }
                }
            }
            content = model(d, s.substring(from, i), new int[1]);
            pos[0] = i + 2;
        }
        return new ContentModel(type, content, model(d, s, pos));
    }
}
