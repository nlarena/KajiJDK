package java.awt.event;

import java.awt.Component;

/**
 * Alguien tocó el teclado.
 *
 * <p>Hay dos maneras de mirar una pulsación y esta clase lleva las dos, que es de donde viene toda
 * la confusión con el teclado en Java.
 *
 * <p>El <strong>código de tecla</strong> ({@code VK_A}, {@code VK_F1}, {@code VK_SHIFT}) identifica
 * una **tecla física** del teclado, y viene en {@code KEY_PRESSED} y {@code KEY_RELEASED}. Sirve
 * para atajos y para teclas que no escriben nada: no hay un carácter "F1".
 *
 * <p>El <strong>carácter</strong> es lo que se escribió, y viene en {@code KEY_TYPED}. No hay una
 * correspondencia uno a uno con las teclas: Mayús+a produce una sola pulsación con carácter, dos
 * teclas apretadas y ningún código de tecla propio; una tecla muerta seguida de una vocal son tres
 * pulsaciones y un solo carácter.
 *
 * <p>De ahí la regla práctica: <b>los atajos van en {@code keyPressed}, el texto en
 * {@code keyTyped}</b>. Usar el otro es la causa de casi todos los teclados que se portan raro con
 * un idioma que no es inglés.
 *
 * <p>La <strong>ubicación</strong> distingue teclas que comparten código: el Mayús de la izquierda
 * del de la derecha, el 1 del teclado numérico del de la fila de arriba.
 */
public class KeyEvent extends InputEvent {

    private static final long serialVersionUID = -2352130953028126954L;

    /** Ningún carácter: es lo que trae una tecla que no produce texto. */
    public static final char CHAR_UNDEFINED = '\uFFFF';

    /** Constante de {@code KeyEvent}. */
    public static final int KEY_FIRST = 400;

    /** Constante de {@code KeyEvent}. */
    public static final int KEY_LAST = 402;

    /** Constante de {@code KeyEvent}. */
    public static final int KEY_LOCATION_LEFT = 2;

    /** Constante de {@code KeyEvent}. */
    public static final int KEY_LOCATION_NUMPAD = 4;

    /** Constante de {@code KeyEvent}. */
    public static final int KEY_LOCATION_RIGHT = 3;

    /** Constante de {@code KeyEvent}. */
    public static final int KEY_LOCATION_STANDARD = 1;

    /** Constante de {@code KeyEvent}. */
    public static final int KEY_LOCATION_UNKNOWN = 0;

    /** Constante de {@code KeyEvent}. */
    public static final int KEY_PRESSED = 401;

    /** Constante de {@code KeyEvent}. */
    public static final int KEY_RELEASED = 402;

    /** Constante de {@code KeyEvent}. */
    public static final int KEY_TYPED = 400;

    /** La tecla <b>0</b>. */
    public static final int VK_0 = 48;

    /** La tecla <b>1</b>. */
    public static final int VK_1 = 49;

    /** La tecla <b>2</b>. */
    public static final int VK_2 = 50;

    /** La tecla <b>3</b>. */
    public static final int VK_3 = 51;

    /** La tecla <b>4</b>. */
    public static final int VK_4 = 52;

    /** La tecla <b>5</b>. */
    public static final int VK_5 = 53;

    /** La tecla <b>6</b>. */
    public static final int VK_6 = 54;

    /** La tecla <b>7</b>. */
    public static final int VK_7 = 55;

    /** La tecla <b>8</b>. */
    public static final int VK_8 = 56;

    /** La tecla <b>9</b>. */
    public static final int VK_9 = 57;

    /** La tecla <b>a</b>. */
    public static final int VK_A = 65;

    /** La tecla <b>accept</b>. */
    public static final int VK_ACCEPT = 30;

    /** La tecla <b>add</b>. */
    public static final int VK_ADD = 107;

    /** La tecla <b>again</b>. */
    public static final int VK_AGAIN = 65481;

    /** La tecla <b>all candidates</b>. */
    public static final int VK_ALL_CANDIDATES = 256;

    /** La tecla <b>alphanumeric</b>. */
    public static final int VK_ALPHANUMERIC = 240;

    /** La tecla <b>alt</b>. */
    public static final int VK_ALT = 18;

    /** La tecla <b>alt graph</b>. */
    public static final int VK_ALT_GRAPH = 65406;

    /** La tecla <b>ampersand</b>. */
    public static final int VK_AMPERSAND = 150;

    /** La tecla <b>asterisk</b>. */
    public static final int VK_ASTERISK = 151;

    /** La tecla <b>at</b>. */
    public static final int VK_AT = 512;

    /** La tecla <b>b</b>. */
    public static final int VK_B = 66;

    /** La tecla <b>back quote</b>. */
    public static final int VK_BACK_QUOTE = 192;

    /** La tecla <b>back slash</b>. */
    public static final int VK_BACK_SLASH = 92;

    /** La tecla <b>back space</b>. */
    public static final int VK_BACK_SPACE = 8;

    /** La tecla <b>begin</b>. */
    public static final int VK_BEGIN = 65368;

    /** La tecla <b>braceleft</b>. */
    public static final int VK_BRACELEFT = 161;

    /** La tecla <b>braceright</b>. */
    public static final int VK_BRACERIGHT = 162;

    /** La tecla <b>c</b>. */
    public static final int VK_C = 67;

    /** La tecla <b>cancel</b>. */
    public static final int VK_CANCEL = 3;

    /** La tecla <b>caps lock</b>. */
    public static final int VK_CAPS_LOCK = 20;

    /** La tecla <b>circumflex</b>. */
    public static final int VK_CIRCUMFLEX = 514;

    /** La tecla <b>clear</b>. */
    public static final int VK_CLEAR = 12;

    /** La tecla <b>close bracket</b>. */
    public static final int VK_CLOSE_BRACKET = 93;

    /** La tecla <b>code input</b>. */
    public static final int VK_CODE_INPUT = 258;

    /** La tecla <b>colon</b>. */
    public static final int VK_COLON = 513;

    /** La tecla <b>comma</b>. */
    public static final int VK_COMMA = 44;

    /** La tecla <b>compose</b>. */
    public static final int VK_COMPOSE = 65312;

    /** La tecla <b>context menu</b>. */
    public static final int VK_CONTEXT_MENU = 525;

    /** La tecla <b>control</b>. */
    public static final int VK_CONTROL = 17;

    /** La tecla <b>convert</b>. */
    public static final int VK_CONVERT = 28;

    /** La tecla <b>copy</b>. */
    public static final int VK_COPY = 65485;

    /** La tecla <b>cut</b>. */
    public static final int VK_CUT = 65489;

    /** La tecla <b>d</b>. */
    public static final int VK_D = 68;

    /** La tecla <b>dead abovedot</b>. */
    public static final int VK_DEAD_ABOVEDOT = 134;

    /** La tecla <b>dead abovering</b>. */
    public static final int VK_DEAD_ABOVERING = 136;

    /** La tecla <b>dead acute</b>. */
    public static final int VK_DEAD_ACUTE = 129;

    /** La tecla <b>dead breve</b>. */
    public static final int VK_DEAD_BREVE = 133;

    /** La tecla <b>dead caron</b>. */
    public static final int VK_DEAD_CARON = 138;

    /** La tecla <b>dead cedilla</b>. */
    public static final int VK_DEAD_CEDILLA = 139;

    /** La tecla <b>dead circumflex</b>. */
    public static final int VK_DEAD_CIRCUMFLEX = 130;

    /** La tecla <b>dead diaeresis</b>. */
    public static final int VK_DEAD_DIAERESIS = 135;

    /** La tecla <b>dead doubleacute</b>. */
    public static final int VK_DEAD_DOUBLEACUTE = 137;

    /** La tecla <b>dead grave</b>. */
    public static final int VK_DEAD_GRAVE = 128;

    /** La tecla <b>dead iota</b>. */
    public static final int VK_DEAD_IOTA = 141;

    /** La tecla <b>dead macron</b>. */
    public static final int VK_DEAD_MACRON = 132;

    /** La tecla <b>dead ogonek</b>. */
    public static final int VK_DEAD_OGONEK = 140;

    /** La tecla <b>dead semivoiced sound</b>. */
    public static final int VK_DEAD_SEMIVOICED_SOUND = 143;

    /** La tecla <b>dead tilde</b>. */
    public static final int VK_DEAD_TILDE = 131;

    /** La tecla <b>dead voiced sound</b>. */
    public static final int VK_DEAD_VOICED_SOUND = 142;

    /** La tecla <b>decimal</b>. */
    public static final int VK_DECIMAL = 110;

    /** La tecla <b>delete</b>. */
    public static final int VK_DELETE = 127;

    /** La tecla <b>divide</b>. */
    public static final int VK_DIVIDE = 111;

    /** La tecla <b>dollar</b>. */
    public static final int VK_DOLLAR = 515;

    /** La tecla <b>down</b>. */
    public static final int VK_DOWN = 40;

    /** La tecla <b>e</b>. */
    public static final int VK_E = 69;

    /** La tecla <b>end</b>. */
    public static final int VK_END = 35;

    /** La tecla <b>enter</b>. */
    public static final int VK_ENTER = 10;

    /** La tecla <b>equals</b>. */
    public static final int VK_EQUALS = 61;

    /** La tecla <b>escape</b>. */
    public static final int VK_ESCAPE = 27;

    /** La tecla <b>euro sign</b>. */
    public static final int VK_EURO_SIGN = 516;

    /** La tecla <b>exclamation mark</b>. */
    public static final int VK_EXCLAMATION_MARK = 517;

    /** La tecla <b>f</b>. */
    public static final int VK_F = 70;

    /** La tecla <b>f1</b>. */
    public static final int VK_F1 = 112;

    /** La tecla <b>f10</b>. */
    public static final int VK_F10 = 121;

    /** La tecla <b>f11</b>. */
    public static final int VK_F11 = 122;

    /** La tecla <b>f12</b>. */
    public static final int VK_F12 = 123;

    /** La tecla <b>f13</b>. */
    public static final int VK_F13 = 61440;

    /** La tecla <b>f14</b>. */
    public static final int VK_F14 = 61441;

    /** La tecla <b>f15</b>. */
    public static final int VK_F15 = 61442;

    /** La tecla <b>f16</b>. */
    public static final int VK_F16 = 61443;

    /** La tecla <b>f17</b>. */
    public static final int VK_F17 = 61444;

    /** La tecla <b>f18</b>. */
    public static final int VK_F18 = 61445;

    /** La tecla <b>f19</b>. */
    public static final int VK_F19 = 61446;

    /** La tecla <b>f2</b>. */
    public static final int VK_F2 = 113;

    /** La tecla <b>f20</b>. */
    public static final int VK_F20 = 61447;

    /** La tecla <b>f21</b>. */
    public static final int VK_F21 = 61448;

    /** La tecla <b>f22</b>. */
    public static final int VK_F22 = 61449;

    /** La tecla <b>f23</b>. */
    public static final int VK_F23 = 61450;

    /** La tecla <b>f24</b>. */
    public static final int VK_F24 = 61451;

    /** La tecla <b>f3</b>. */
    public static final int VK_F3 = 114;

    /** La tecla <b>f4</b>. */
    public static final int VK_F4 = 115;

    /** La tecla <b>f5</b>. */
    public static final int VK_F5 = 116;

    /** La tecla <b>f6</b>. */
    public static final int VK_F6 = 117;

    /** La tecla <b>f7</b>. */
    public static final int VK_F7 = 118;

    /** La tecla <b>f8</b>. */
    public static final int VK_F8 = 119;

    /** La tecla <b>f9</b>. */
    public static final int VK_F9 = 120;

    /** La tecla <b>final</b>. */
    public static final int VK_FINAL = 24;

    /** La tecla <b>find</b>. */
    public static final int VK_FIND = 65488;

    /** La tecla <b>full width</b>. */
    public static final int VK_FULL_WIDTH = 243;

    /** La tecla <b>g</b>. */
    public static final int VK_G = 71;

    /** La tecla <b>greater</b>. */
    public static final int VK_GREATER = 160;

    /** La tecla <b>h</b>. */
    public static final int VK_H = 72;

    /** La tecla <b>half width</b>. */
    public static final int VK_HALF_WIDTH = 244;

    /** La tecla <b>help</b>. */
    public static final int VK_HELP = 156;

    /** La tecla <b>hiragana</b>. */
    public static final int VK_HIRAGANA = 242;

    /** La tecla <b>home</b>. */
    public static final int VK_HOME = 36;

    /** La tecla <b>i</b>. */
    public static final int VK_I = 73;

    /** La tecla <b>input method on off</b>. */
    public static final int VK_INPUT_METHOD_ON_OFF = 263;

    /** La tecla <b>insert</b>. */
    public static final int VK_INSERT = 155;

    /** La tecla <b>inverted exclamation mark</b>. */
    public static final int VK_INVERTED_EXCLAMATION_MARK = 518;

    /** La tecla <b>j</b>. */
    public static final int VK_J = 74;

    /** La tecla <b>japanese hiragana</b>. */
    public static final int VK_JAPANESE_HIRAGANA = 260;

    /** La tecla <b>japanese katakana</b>. */
    public static final int VK_JAPANESE_KATAKANA = 259;

    /** La tecla <b>japanese roman</b>. */
    public static final int VK_JAPANESE_ROMAN = 261;

    /** La tecla <b>k</b>. */
    public static final int VK_K = 75;

    /** La tecla <b>kana</b>. */
    public static final int VK_KANA = 21;

    /** La tecla <b>kana lock</b>. */
    public static final int VK_KANA_LOCK = 262;

    /** La tecla <b>kanji</b>. */
    public static final int VK_KANJI = 25;

    /** La tecla <b>katakana</b>. */
    public static final int VK_KATAKANA = 241;

    /** La tecla <b>kp down</b>. */
    public static final int VK_KP_DOWN = 225;

    /** La tecla <b>kp left</b>. */
    public static final int VK_KP_LEFT = 226;

    /** La tecla <b>kp right</b>. */
    public static final int VK_KP_RIGHT = 227;

    /** La tecla <b>kp up</b>. */
    public static final int VK_KP_UP = 224;

    /** La tecla <b>l</b>. */
    public static final int VK_L = 76;

    /** La tecla <b>left</b>. */
    public static final int VK_LEFT = 37;

    /** La tecla <b>left parenthesis</b>. */
    public static final int VK_LEFT_PARENTHESIS = 519;

    /** La tecla <b>less</b>. */
    public static final int VK_LESS = 153;

    /** La tecla <b>m</b>. */
    public static final int VK_M = 77;

    /** La tecla <b>meta</b>. */
    public static final int VK_META = 157;

    /** La tecla <b>minus</b>. */
    public static final int VK_MINUS = 45;

    /** La tecla <b>modechange</b>. */
    public static final int VK_MODECHANGE = 31;

    /** La tecla <b>multiply</b>. */
    public static final int VK_MULTIPLY = 106;

    /** La tecla <b>n</b>. */
    public static final int VK_N = 78;

    /** La tecla <b>nonconvert</b>. */
    public static final int VK_NONCONVERT = 29;

    /** La tecla <b>number sign</b>. */
    public static final int VK_NUMBER_SIGN = 520;

    /** La tecla <b>numpad0</b>. */
    public static final int VK_NUMPAD0 = 96;

    /** La tecla <b>numpad1</b>. */
    public static final int VK_NUMPAD1 = 97;

    /** La tecla <b>numpad2</b>. */
    public static final int VK_NUMPAD2 = 98;

    /** La tecla <b>numpad3</b>. */
    public static final int VK_NUMPAD3 = 99;

    /** La tecla <b>numpad4</b>. */
    public static final int VK_NUMPAD4 = 100;

    /** La tecla <b>numpad5</b>. */
    public static final int VK_NUMPAD5 = 101;

    /** La tecla <b>numpad6</b>. */
    public static final int VK_NUMPAD6 = 102;

    /** La tecla <b>numpad7</b>. */
    public static final int VK_NUMPAD7 = 103;

    /** La tecla <b>numpad8</b>. */
    public static final int VK_NUMPAD8 = 104;

    /** La tecla <b>numpad9</b>. */
    public static final int VK_NUMPAD9 = 105;

    /** La tecla <b>num lock</b>. */
    public static final int VK_NUM_LOCK = 144;

    /** La tecla <b>o</b>. */
    public static final int VK_O = 79;

    /** La tecla <b>open bracket</b>. */
    public static final int VK_OPEN_BRACKET = 91;

    /** La tecla <b>p</b>. */
    public static final int VK_P = 80;

    /** La tecla <b>page down</b>. */
    public static final int VK_PAGE_DOWN = 34;

    /** La tecla <b>page up</b>. */
    public static final int VK_PAGE_UP = 33;

    /** La tecla <b>paste</b>. */
    public static final int VK_PASTE = 65487;

    /** La tecla <b>pause</b>. */
    public static final int VK_PAUSE = 19;

    /** La tecla <b>period</b>. */
    public static final int VK_PERIOD = 46;

    /** La tecla <b>plus</b>. */
    public static final int VK_PLUS = 521;

    /** La tecla <b>previous candidate</b>. */
    public static final int VK_PREVIOUS_CANDIDATE = 257;

    /** La tecla <b>printscreen</b>. */
    public static final int VK_PRINTSCREEN = 154;

    /** La tecla <b>props</b>. */
    public static final int VK_PROPS = 65482;

    /** La tecla <b>q</b>. */
    public static final int VK_Q = 81;

    /** La tecla <b>quote</b>. */
    public static final int VK_QUOTE = 222;

    /** La tecla <b>quotedbl</b>. */
    public static final int VK_QUOTEDBL = 152;

    /** La tecla <b>r</b>. */
    public static final int VK_R = 82;

    /** La tecla <b>right</b>. */
    public static final int VK_RIGHT = 39;

    /** La tecla <b>right parenthesis</b>. */
    public static final int VK_RIGHT_PARENTHESIS = 522;

    /** La tecla <b>roman characters</b>. */
    public static final int VK_ROMAN_CHARACTERS = 245;

    /** La tecla <b>s</b>. */
    public static final int VK_S = 83;

    /** La tecla <b>scroll lock</b>. */
    public static final int VK_SCROLL_LOCK = 145;

    /** La tecla <b>semicolon</b>. */
    public static final int VK_SEMICOLON = 59;

    /** La tecla <b>separater</b>. */
    public static final int VK_SEPARATER = 108;

    /** La tecla <b>separator</b>. */
    public static final int VK_SEPARATOR = 108;

    /** La tecla <b>shift</b>. */
    public static final int VK_SHIFT = 16;

    /** La tecla <b>slash</b>. */
    public static final int VK_SLASH = 47;

    /** La tecla <b>space</b>. */
    public static final int VK_SPACE = 32;

    /** La tecla <b>stop</b>. */
    public static final int VK_STOP = 65480;

    /** La tecla <b>subtract</b>. */
    public static final int VK_SUBTRACT = 109;

    /** La tecla <b>t</b>. */
    public static final int VK_T = 84;

    /** La tecla <b>tab</b>. */
    public static final int VK_TAB = 9;

    /** La tecla <b>u</b>. */
    public static final int VK_U = 85;

    /** Ninguna tecla conocida. */
    public static final int VK_UNDEFINED = 0;

    /** La tecla <b>underscore</b>. */
    public static final int VK_UNDERSCORE = 523;

    /** La tecla <b>undo</b>. */
    public static final int VK_UNDO = 65483;

    /** La tecla <b>up</b>. */
    public static final int VK_UP = 38;

    /** La tecla <b>v</b>. */
    public static final int VK_V = 86;

    /** La tecla <b>w</b>. */
    public static final int VK_W = 87;

    /** La tecla <b>windows</b>. */
    public static final int VK_WINDOWS = 524;

    /** La tecla <b>x</b>. */
    public static final int VK_X = 88;

    /** La tecla <b>y</b>. */
    public static final int VK_Y = 89;

    /** La tecla <b>z</b>. */
    public static final int VK_Z = 90;

    private int keyCode;
    private char keyChar;
    private int keyLocation;

    /**
     * Con todo dado.
     *
     * @throws IllegalArgumentException si la fuente es `null`, si un {@code KEY_TYPED} trae un
     *     código de tecla o una ubicación conocida, o si la ubicación no es una de las cuatro
     */
    public KeyEvent(Component source, int id, long when, int modifiers, int keyCode, char keyChar,
            int keyLocation) {
        super(source, id, when, modifiers);
        if (id == KEY_TYPED) {
            if (keyCode != VK_UNDEFINED) {
                throw new IllegalArgumentException("invalid keyCode for KEY_TYPED event");
            }
            if (keyChar == CHAR_UNDEFINED) {
                throw new IllegalArgumentException("invalid keyChar for KEY_TYPED event");
            }
        }
        if (keyLocation < KEY_LOCATION_UNKNOWN || keyLocation > KEY_LOCATION_NUMPAD) {
            throw new IllegalArgumentException("invalid keyLocation");
        }
        this.keyCode = keyCode;
        this.keyChar = keyChar;
        this.keyLocation = keyLocation;
    }

    /**
     * Sin decir la ubicación.
     *
     * @throws IllegalArgumentException si la fuente es `null` o los datos no cuadran con el tipo
     */
    public KeyEvent(Component source, int id, long when, int modifiers, int keyCode,
            char keyChar) {
        this(source, id, when, modifiers, keyCode, keyChar, KEY_LOCATION_UNKNOWN);
    }

    /**
     * Sin carácter.
     *
     * @deprecated no permite decir qué carácter se escribió, y sin eso un {@code KEY_TYPED} no
     *     significa nada. Usar alguno de los otros dos.
     * @throws IllegalArgumentException si la fuente es `null`
     */
    @Deprecated
    public KeyEvent(Component source, int id, long when, int modifiers, int keyCode) {
        this(source, id, when, modifiers, keyCode, (char) keyCode, KEY_LOCATION_UNKNOWN);
    }

    /** Qué tecla física fue. */
    public int getKeyCode() {
        return this.keyCode;
    }

    /** Cambia qué tecla física fue. */
    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    /** Qué carácter se escribió, o {@link #CHAR_UNDEFINED} si ninguno. */
    public char getKeyChar() {
        return this.keyChar;
    }

    /** Cambia qué carácter se escribió. */
    public void setKeyChar(char keyChar) {
        this.keyChar = keyChar;
    }

    /**
     * Cambia los modificadores.
     *
     * @deprecated cambiar los modificadores no cambia el carácter que ya se calculó, así que el
     *     evento queda diciendo dos cosas que no se corresponden.
     */
    @Deprecated
    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    /** Cuál de las teclas que comparten ese código fue. */
    public int getKeyLocation() {
        return this.keyLocation;
    }

    /**
     * El nombre de una tecla, para mostrárselo a una persona.
     *
     * <p>Devuelve los nombres en inglés, que son los que el JDK usa cuando no encuentra la
     * traducción: acá no hay traducciones que buscar, así que es la respuesta correcta y no un
     * relleno.
     */
    public static String getKeyText(int keyCode) {
        if (keyCode >= VK_0 && keyCode <= VK_9 || keyCode >= VK_A && keyCode <= VK_Z) {
            return String.valueOf((char) keyCode);
        }
        if (keyCode == VK_ENTER) {
            return "Enter";
        }
        if (keyCode == VK_ESCAPE) {
            return "Escape";
        }
        if (keyCode == VK_SPACE) {
            return "Space";
        }
        if (keyCode == VK_TAB) {
            return "Tab";
        }
        if (keyCode == VK_BACK_SPACE) {
            return "Backspace";
        }
        if (keyCode == VK_DELETE) {
            return "Delete";
        }
        if (keyCode == VK_SHIFT) {
            return "Shift";
        }
        if (keyCode == VK_CONTROL) {
            return "Ctrl";
        }
        if (keyCode == VK_ALT) {
            return "Alt";
        }
        if (keyCode == VK_META) {
            return "Meta";
        }
        if (keyCode == VK_LEFT) {
            return "Left";
        }
        if (keyCode == VK_RIGHT) {
            return "Right";
        }
        if (keyCode == VK_UP) {
            return "Up";
        }
        if (keyCode == VK_DOWN) {
            return "Down";
        }
        if (keyCode >= VK_F1 && keyCode <= VK_F12) {
            return "F" + (keyCode - VK_F1 + 1);
        }
        return "Unknown keyCode: 0x" + Integer.toString(keyCode, 16);
    }

    /**
     * Los modificadores escritos para una persona.
     *
     * @deprecated trabaja con la codificación vieja. Usar
     *     {@link InputEvent#getModifiersExText(int)}.
     */
    @Deprecated
    public static String getKeyModifiersText(int modifiers) {
        StringBuilder sb = new StringBuilder();
        if ((modifiers & InputEvent.META_MASK) != 0) {
            sb.append("Meta+");
        }
        if ((modifiers & InputEvent.CTRL_MASK) != 0) {
            sb.append("Ctrl+");
        }
        if ((modifiers & InputEvent.ALT_MASK) != 0) {
            sb.append("Alt+");
        }
        if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
            sb.append("Shift+");
        }
        if ((modifiers & InputEvent.ALT_GRAPH_MASK) != 0) {
            sb.append("Alt Graph+");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Si la tecla no produce texto: una de función, una flecha, un modificador.
     *
     * <p>Es la pregunta que separa las dos mitades del teclado, y la que decide si esperar un
     * {@code KEY_TYPED} después de esta pulsación tiene sentido.
     */
    public boolean isActionKey() {
        if (this.keyCode >= VK_F1 && this.keyCode <= VK_F24) {
            return true;
        }
        if (this.keyCode >= VK_LEFT && this.keyCode <= VK_DOWN) {
            return true;
        }
        return this.keyCode == VK_HOME || this.keyCode == VK_END || this.keyCode == VK_PAGE_UP
                || this.keyCode == VK_PAGE_DOWN || this.keyCode == VK_INSERT
                || this.keyCode == VK_PRINTSCREEN || this.keyCode == VK_SCROLL_LOCK
                || this.keyCode == VK_CAPS_LOCK || this.keyCode == VK_NUM_LOCK
                || this.keyCode == VK_PAUSE;
    }

    public String paramString() {
        String tipo;
        if (this.id == KEY_PRESSED) {
            tipo = "KEY_PRESSED";
        } else if (this.id == KEY_RELEASED) {
            tipo = "KEY_RELEASED";
        } else if (this.id == KEY_TYPED) {
            tipo = "KEY_TYPED";
        } else {
            tipo = "unknown type";
        }
        return tipo + ",keyCode=" + this.keyCode + ",keyText=" + getKeyText(this.keyCode)
                + ",keyChar=" + (this.keyChar == CHAR_UNDEFINED ? "Undefined keyChar"
                        : String.valueOf(this.keyChar))
                + ",keyLocation=" + this.keyLocation;
    }

    /**
     * El código extendido, que distingue teclas que el código común confunde.
     *
     * <p>Acá coincide siempre con {@link #getKeyCode}: el código extendido sólo se separa del común
     * cuando el sistema informa la disposición física del teclado, y esta biblioteca no tiene de
     * dónde sacarla.
     */
    public int getExtendedKeyCode() {
        return this.keyCode;
    }

    /**
     * El código extendido que corresponde a ese carácter.
     *
     * @return el código, o {@link #VK_UNDEFINED} si el carácter no tiene tecla propia
     */
    public static int getExtendedKeyCodeForChar(int c) {
        if (c >= 'a' && c <= 'z') {
            return VK_A + (c - 'a');
        }
        if (c >= 'A' && c <= 'Z') {
            return VK_A + (c - 'A');
        }
        if (c >= '0' && c <= '9') {
            return VK_0 + (c - '0');
        }
        if (c == ' ') {
            return VK_SPACE;
        }
        return VK_UNDEFINED;
    }
}
