package java.awt.event;

import java.awt.Component;

/**
 * Someone touched the keyboard.
 *
 * <p>There are two ways of looking at a keystroke and this class carries both, which is where all
 * the confusion with the keyboard in Java comes from.
 *
 * <p>The <strong>key code</strong> ({@code VK_A}, {@code VK_F1}, {@code VK_SHIFT}) identifies a
 * **physical key** of the keyboard, and comes in {@code KEY_PRESSED} and {@code KEY_RELEASED}. It
 * serves for shortcuts and for keys that write nothing: there is no "F1" character.
 *
 * <p>The <strong>character</strong> is what was written, and comes in {@code KEY_TYPED}. There is no
 * one-to-one correspondence with the keys: Shift+a produces a single keystroke with a character, two
 * keys down and no key code of its own; a dead key followed by a vowel is three keystrokes and a
 * single character.
 *
 * <p>Hence the practical rule: <b>shortcuts go in {@code keyPressed}, text in {@code keyTyped}</b>.
 * Using the other one is the cause of nearly every keyboard that behaves oddly with a language that
 * is not English.
 *
 * <p>The <strong>location</strong> tells apart keys that share a code: the left Shift from the right
 * one, the numeric keypad's 1 from the one on the top row.
 */
public class KeyEvent extends InputEvent {

    private static final long serialVersionUID = -2352130953028126954L;

    /** No character: it is what a key that produces no text carries. */
    public static final char CHAR_UNDEFINED = '\uFFFF';

    /** The family's first identifier. */
    public static final int KEY_FIRST = 400;

    /** The family's last identifier. */
    public static final int KEY_LAST = 402;

    /** The key on the left of a pair that shares a code. */
    public static final int KEY_LOCATION_LEFT = 2;

    /** The key on the numeric keypad. */
    public static final int KEY_LOCATION_NUMPAD = 4;

    /** The key on the right of a pair that shares a code. */
    public static final int KEY_LOCATION_RIGHT = 3;

    /** The key in its usual place, with no twin. */
    public static final int KEY_LOCATION_STANDARD = 1;

    /** The place is not known. */
    public static final int KEY_LOCATION_UNKNOWN = 0;

    /** A key was pressed. */
    public static final int KEY_PRESSED = 401;

    /** A key was released. */
    public static final int KEY_RELEASED = 402;

    /** A character was produced. */
    public static final int KEY_TYPED = 400;

    /** The <b>0</b> key. */
    public static final int VK_0 = 48;

    /** The <b>1</b> key. */
    public static final int VK_1 = 49;

    /** The <b>2</b> key. */
    public static final int VK_2 = 50;

    /** The <b>3</b> key. */
    public static final int VK_3 = 51;

    /** The <b>4</b> key. */
    public static final int VK_4 = 52;

    /** The <b>5</b> key. */
    public static final int VK_5 = 53;

    /** The <b>6</b> key. */
    public static final int VK_6 = 54;

    /** The <b>7</b> key. */
    public static final int VK_7 = 55;

    /** The <b>8</b> key. */
    public static final int VK_8 = 56;

    /** The <b>9</b> key. */
    public static final int VK_9 = 57;

    /** The <b>a</b> key. */
    public static final int VK_A = 65;

    /** The <b>accept</b> key. */
    public static final int VK_ACCEPT = 30;

    /** The <b>add</b> key. */
    public static final int VK_ADD = 107;

    /** The <b>again</b> key. */
    public static final int VK_AGAIN = 65481;

    /** The <b>all candidates</b> key. */
    public static final int VK_ALL_CANDIDATES = 256;

    /** The <b>alphanumeric</b> key. */
    public static final int VK_ALPHANUMERIC = 240;

    /** The <b>alt</b> key. */
    public static final int VK_ALT = 18;

    /** The <b>alt graph</b> key. */
    public static final int VK_ALT_GRAPH = 65406;

    /** The <b>ampersand</b> key. */
    public static final int VK_AMPERSAND = 150;

    /** The <b>asterisk</b> key. */
    public static final int VK_ASTERISK = 151;

    /** The <b>at</b> key. */
    public static final int VK_AT = 512;

    /** The <b>b</b> key. */
    public static final int VK_B = 66;

    /** The <b>back quote</b> key. */
    public static final int VK_BACK_QUOTE = 192;

    /** The <b>back slash</b> key. */
    public static final int VK_BACK_SLASH = 92;

    /** The <b>back space</b> key. */
    public static final int VK_BACK_SPACE = 8;

    /** The <b>begin</b> key. */
    public static final int VK_BEGIN = 65368;

    /** The <b>braceleft</b> key. */
    public static final int VK_BRACELEFT = 161;

    /** The <b>braceright</b> key. */
    public static final int VK_BRACERIGHT = 162;

    /** The <b>c</b> key. */
    public static final int VK_C = 67;

    /** The <b>cancel</b> key. */
    public static final int VK_CANCEL = 3;

    /** The <b>caps lock</b> key. */
    public static final int VK_CAPS_LOCK = 20;

    /** The <b>circumflex</b> key. */
    public static final int VK_CIRCUMFLEX = 514;

    /** The <b>clear</b> key. */
    public static final int VK_CLEAR = 12;

    /** The <b>close bracket</b> key. */
    public static final int VK_CLOSE_BRACKET = 93;

    /** The <b>code input</b> key. */
    public static final int VK_CODE_INPUT = 258;

    /** The <b>colon</b> key. */
    public static final int VK_COLON = 513;

    /** The <b>comma</b> key. */
    public static final int VK_COMMA = 44;

    /** The <b>compose</b> key. */
    public static final int VK_COMPOSE = 65312;

    /** The <b>context menu</b> key. */
    public static final int VK_CONTEXT_MENU = 525;

    /** The <b>control</b> key. */
    public static final int VK_CONTROL = 17;

    /** The <b>convert</b> key. */
    public static final int VK_CONVERT = 28;

    /** The <b>copy</b> key. */
    public static final int VK_COPY = 65485;

    /** The <b>cut</b> key. */
    public static final int VK_CUT = 65489;

    /** The <b>d</b> key. */
    public static final int VK_D = 68;

    /** The <b>dead abovedot</b> key. */
    public static final int VK_DEAD_ABOVEDOT = 134;

    /** The <b>dead abovering</b> key. */
    public static final int VK_DEAD_ABOVERING = 136;

    /** The <b>dead acute</b> key. */
    public static final int VK_DEAD_ACUTE = 129;

    /** The <b>dead breve</b> key. */
    public static final int VK_DEAD_BREVE = 133;

    /** The <b>dead caron</b> key. */
    public static final int VK_DEAD_CARON = 138;

    /** The <b>dead cedilla</b> key. */
    public static final int VK_DEAD_CEDILLA = 139;

    /** The <b>dead circumflex</b> key. */
    public static final int VK_DEAD_CIRCUMFLEX = 130;

    /** The <b>dead diaeresis</b> key. */
    public static final int VK_DEAD_DIAERESIS = 135;

    /** The <b>dead doubleacute</b> key. */
    public static final int VK_DEAD_DOUBLEACUTE = 137;

    /** The <b>dead grave</b> key. */
    public static final int VK_DEAD_GRAVE = 128;

    /** The <b>dead iota</b> key. */
    public static final int VK_DEAD_IOTA = 141;

    /** The <b>dead macron</b> key. */
    public static final int VK_DEAD_MACRON = 132;

    /** The <b>dead ogonek</b> key. */
    public static final int VK_DEAD_OGONEK = 140;

    /** The <b>dead semivoiced sound</b> key. */
    public static final int VK_DEAD_SEMIVOICED_SOUND = 143;

    /** The <b>dead tilde</b> key. */
    public static final int VK_DEAD_TILDE = 131;

    /** The <b>dead voiced sound</b> key. */
    public static final int VK_DEAD_VOICED_SOUND = 142;

    /** The <b>decimal</b> key. */
    public static final int VK_DECIMAL = 110;

    /** The <b>delete</b> key. */
    public static final int VK_DELETE = 127;

    /** The <b>divide</b> key. */
    public static final int VK_DIVIDE = 111;

    /** The <b>dollar</b> key. */
    public static final int VK_DOLLAR = 515;

    /** The <b>down</b> key. */
    public static final int VK_DOWN = 40;

    /** The <b>e</b> key. */
    public static final int VK_E = 69;

    /** The <b>end</b> key. */
    public static final int VK_END = 35;

    /** The <b>enter</b> key. */
    public static final int VK_ENTER = 10;

    /** The <b>equals</b> key. */
    public static final int VK_EQUALS = 61;

    /** The <b>escape</b> key. */
    public static final int VK_ESCAPE = 27;

    /** The <b>euro sign</b> key. */
    public static final int VK_EURO_SIGN = 516;

    /** The <b>exclamation mark</b> key. */
    public static final int VK_EXCLAMATION_MARK = 517;

    /** The <b>f</b> key. */
    public static final int VK_F = 70;

    /** The <b>f1</b> key. */
    public static final int VK_F1 = 112;

    /** The <b>f10</b> key. */
    public static final int VK_F10 = 121;

    /** The <b>f11</b> key. */
    public static final int VK_F11 = 122;

    /** The <b>f12</b> key. */
    public static final int VK_F12 = 123;

    /** The <b>f13</b> key. */
    public static final int VK_F13 = 61440;

    /** The <b>f14</b> key. */
    public static final int VK_F14 = 61441;

    /** The <b>f15</b> key. */
    public static final int VK_F15 = 61442;

    /** The <b>f16</b> key. */
    public static final int VK_F16 = 61443;

    /** The <b>f17</b> key. */
    public static final int VK_F17 = 61444;

    /** The <b>f18</b> key. */
    public static final int VK_F18 = 61445;

    /** The <b>f19</b> key. */
    public static final int VK_F19 = 61446;

    /** The <b>f2</b> key. */
    public static final int VK_F2 = 113;

    /** The <b>f20</b> key. */
    public static final int VK_F20 = 61447;

    /** The <b>f21</b> key. */
    public static final int VK_F21 = 61448;

    /** The <b>f22</b> key. */
    public static final int VK_F22 = 61449;

    /** The <b>f23</b> key. */
    public static final int VK_F23 = 61450;

    /** The <b>f24</b> key. */
    public static final int VK_F24 = 61451;

    /** The <b>f3</b> key. */
    public static final int VK_F3 = 114;

    /** The <b>f4</b> key. */
    public static final int VK_F4 = 115;

    /** The <b>f5</b> key. */
    public static final int VK_F5 = 116;

    /** The <b>f6</b> key. */
    public static final int VK_F6 = 117;

    /** The <b>f7</b> key. */
    public static final int VK_F7 = 118;

    /** The <b>f8</b> key. */
    public static final int VK_F8 = 119;

    /** The <b>f9</b> key. */
    public static final int VK_F9 = 120;

    /** The <b>final</b> key. */
    public static final int VK_FINAL = 24;

    /** The <b>find</b> key. */
    public static final int VK_FIND = 65488;

    /** The <b>full width</b> key. */
    public static final int VK_FULL_WIDTH = 243;

    /** The <b>g</b> key. */
    public static final int VK_G = 71;

    /** The <b>greater</b> key. */
    public static final int VK_GREATER = 160;

    /** The <b>h</b> key. */
    public static final int VK_H = 72;

    /** The <b>half width</b> key. */
    public static final int VK_HALF_WIDTH = 244;

    /** The <b>help</b> key. */
    public static final int VK_HELP = 156;

    /** The <b>hiragana</b> key. */
    public static final int VK_HIRAGANA = 242;

    /** The <b>home</b> key. */
    public static final int VK_HOME = 36;

    /** The <b>i</b> key. */
    public static final int VK_I = 73;

    /** The <b>input method on off</b> key. */
    public static final int VK_INPUT_METHOD_ON_OFF = 263;

    /** The <b>insert</b> key. */
    public static final int VK_INSERT = 155;

    /** The <b>inverted exclamation mark</b> key. */
    public static final int VK_INVERTED_EXCLAMATION_MARK = 518;

    /** The <b>j</b> key. */
    public static final int VK_J = 74;

    /** The <b>japanese hiragana</b> key. */
    public static final int VK_JAPANESE_HIRAGANA = 260;

    /** The <b>japanese katakana</b> key. */
    public static final int VK_JAPANESE_KATAKANA = 259;

    /** The <b>japanese roman</b> key. */
    public static final int VK_JAPANESE_ROMAN = 261;

    /** The <b>k</b> key. */
    public static final int VK_K = 75;

    /** The <b>kana</b> key. */
    public static final int VK_KANA = 21;

    /** The <b>kana lock</b> key. */
    public static final int VK_KANA_LOCK = 262;

    /** The <b>kanji</b> key. */
    public static final int VK_KANJI = 25;

    /** The <b>katakana</b> key. */
    public static final int VK_KATAKANA = 241;

    /** The <b>kp down</b> key. */
    public static final int VK_KP_DOWN = 225;

    /** The <b>kp left</b> key. */
    public static final int VK_KP_LEFT = 226;

    /** The <b>kp right</b> key. */
    public static final int VK_KP_RIGHT = 227;

    /** The <b>kp up</b> key. */
    public static final int VK_KP_UP = 224;

    /** The <b>l</b> key. */
    public static final int VK_L = 76;

    /** The <b>left</b> key. */
    public static final int VK_LEFT = 37;

    /** The <b>left parenthesis</b> key. */
    public static final int VK_LEFT_PARENTHESIS = 519;

    /** The <b>less</b> key. */
    public static final int VK_LESS = 153;

    /** The <b>m</b> key. */
    public static final int VK_M = 77;

    /** The <b>meta</b> key. */
    public static final int VK_META = 157;

    /** The <b>minus</b> key. */
    public static final int VK_MINUS = 45;

    /** The <b>modechange</b> key. */
    public static final int VK_MODECHANGE = 31;

    /** The <b>multiply</b> key. */
    public static final int VK_MULTIPLY = 106;

    /** The <b>n</b> key. */
    public static final int VK_N = 78;

    /** The <b>nonconvert</b> key. */
    public static final int VK_NONCONVERT = 29;

    /** The <b>number sign</b> key. */
    public static final int VK_NUMBER_SIGN = 520;

    /** The <b>numpad0</b> key. */
    public static final int VK_NUMPAD0 = 96;

    /** The <b>numpad1</b> key. */
    public static final int VK_NUMPAD1 = 97;

    /** The <b>numpad2</b> key. */
    public static final int VK_NUMPAD2 = 98;

    /** The <b>numpad3</b> key. */
    public static final int VK_NUMPAD3 = 99;

    /** The <b>numpad4</b> key. */
    public static final int VK_NUMPAD4 = 100;

    /** The <b>numpad5</b> key. */
    public static final int VK_NUMPAD5 = 101;

    /** The <b>numpad6</b> key. */
    public static final int VK_NUMPAD6 = 102;

    /** The <b>numpad7</b> key. */
    public static final int VK_NUMPAD7 = 103;

    /** The <b>numpad8</b> key. */
    public static final int VK_NUMPAD8 = 104;

    /** The <b>numpad9</b> key. */
    public static final int VK_NUMPAD9 = 105;

    /** The <b>num lock</b> key. */
    public static final int VK_NUM_LOCK = 144;

    /** The <b>o</b> key. */
    public static final int VK_O = 79;

    /** The <b>open bracket</b> key. */
    public static final int VK_OPEN_BRACKET = 91;

    /** The <b>p</b> key. */
    public static final int VK_P = 80;

    /** The <b>page down</b> key. */
    public static final int VK_PAGE_DOWN = 34;

    /** The <b>page up</b> key. */
    public static final int VK_PAGE_UP = 33;

    /** The <b>paste</b> key. */
    public static final int VK_PASTE = 65487;

    /** The <b>pause</b> key. */
    public static final int VK_PAUSE = 19;

    /** The <b>period</b> key. */
    public static final int VK_PERIOD = 46;

    /** The <b>plus</b> key. */
    public static final int VK_PLUS = 521;

    /** The <b>previous candidate</b> key. */
    public static final int VK_PREVIOUS_CANDIDATE = 257;

    /** The <b>printscreen</b> key. */
    public static final int VK_PRINTSCREEN = 154;

    /** The <b>props</b> key. */
    public static final int VK_PROPS = 65482;

    /** The <b>q</b> key. */
    public static final int VK_Q = 81;

    /** The <b>quote</b> key. */
    public static final int VK_QUOTE = 222;

    /** The <b>quotedbl</b> key. */
    public static final int VK_QUOTEDBL = 152;

    /** The <b>r</b> key. */
    public static final int VK_R = 82;

    /** The <b>right</b> key. */
    public static final int VK_RIGHT = 39;

    /** The <b>right parenthesis</b> key. */
    public static final int VK_RIGHT_PARENTHESIS = 522;

    /** The <b>roman characters</b> key. */
    public static final int VK_ROMAN_CHARACTERS = 245;

    /** The <b>s</b> key. */
    public static final int VK_S = 83;

    /** The <b>scroll lock</b> key. */
    public static final int VK_SCROLL_LOCK = 145;

    /** The <b>semicolon</b> key. */
    public static final int VK_SEMICOLON = 59;

    /** The <b>separater</b> key. */
    public static final int VK_SEPARATER = 108;

    /** The <b>separator</b> key. */
    public static final int VK_SEPARATOR = 108;

    /** The <b>shift</b> key. */
    public static final int VK_SHIFT = 16;

    /** The <b>slash</b> key. */
    public static final int VK_SLASH = 47;

    /** The <b>space</b> key. */
    public static final int VK_SPACE = 32;

    /** The <b>stop</b> key. */
    public static final int VK_STOP = 65480;

    /** The <b>subtract</b> key. */
    public static final int VK_SUBTRACT = 109;

    /** The <b>t</b> key. */
    public static final int VK_T = 84;

    /** The <b>tab</b> key. */
    public static final int VK_TAB = 9;

    /** The <b>u</b> key. */
    public static final int VK_U = 85;

    /** No known key. */
    public static final int VK_UNDEFINED = 0;

    /** The <b>underscore</b> key. */
    public static final int VK_UNDERSCORE = 523;

    /** The <b>undo</b> key. */
    public static final int VK_UNDO = 65483;

    /** The <b>up</b> key. */
    public static final int VK_UP = 38;

    /** The <b>v</b> key. */
    public static final int VK_V = 86;

    /** The <b>w</b> key. */
    public static final int VK_W = 87;

    /** The <b>windows</b> key. */
    public static final int VK_WINDOWS = 524;

    /** The <b>x</b> key. */
    public static final int VK_X = 88;

    /** The <b>y</b> key. */
    public static final int VK_Y = 89;

    /** The <b>z</b> key. */
    public static final int VK_Z = 90;

    private int keyCode;
    private char keyChar;
    private int keyLocation;

    /**
     * With everything given.
     *
     * @throws IllegalArgumentException if the source is `null`, if a {@code KEY_TYPED} carries a key
     *     code or a known location, or if the location is none of the four
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
     * Without saying the location.
     *
     * @throws IllegalArgumentException if the source is `null` or the data does not match the type
     */
    public KeyEvent(Component source, int id, long when, int modifiers, int keyCode,
            char keyChar) {
        this(source, id, when, modifiers, keyCode, keyChar, KEY_LOCATION_UNKNOWN);
    }

    /**
     * Without a character.
     *
     * @deprecated it does not allow saying which character was written, and without that a
     *     {@code KEY_TYPED} means nothing. Use either of the other two.
     * @throws IllegalArgumentException if the source is `null`
     */
    @Deprecated
    public KeyEvent(Component source, int id, long when, int modifiers, int keyCode) {
        this(source, id, when, modifiers, keyCode, (char) keyCode, KEY_LOCATION_UNKNOWN);
    }

    /** Which physical key it was. */
    public int getKeyCode() {
        return this.keyCode;
    }

    /** Changes which physical key it was. */
    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    /** Which character was written, or {@link #CHAR_UNDEFINED} if none. */
    public char getKeyChar() {
        return this.keyChar;
    }

    /** Changes which character was written. */
    public void setKeyChar(char keyChar) {
        this.keyChar = keyChar;
    }

    /**
     * Changes the modifiers.
     *
     * @deprecated changing the modifiers does not change the character already worked out, so the
     *     event is left saying two things that do not match.
     */
    @Deprecated
    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    /** Which of the keys sharing that code it was. */
    public int getKeyLocation() {
        return this.keyLocation;
    }

    /**
     * A key's name, to be shown to a person.
     *
     * <p>It returns the English names, which are the ones the JDK uses when it finds no translation:
     * there are no translations to look up here, so it is the right answer and not a filler.
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
     * The modifiers written out for a person.
     *
     * @deprecated it works with the old encoding. Use
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
     * Whether the key produces no text: a function key, an arrow, a modifier.
     *
     * <p>It is the question that splits the keyboard in two halves, and the one that decides whether
     * expecting a {@code KEY_TYPED} after this keystroke makes sense.
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
        String type;
        if (this.id == KEY_PRESSED) {
            type = "KEY_PRESSED";
        } else if (this.id == KEY_RELEASED) {
            type = "KEY_RELEASED";
        } else if (this.id == KEY_TYPED) {
            type = "KEY_TYPED";
        } else {
            type = "unknown type";
        }
        return type + ",keyCode=" + this.keyCode + ",keyText=" + getKeyText(this.keyCode)
                + ",keyChar=" + (this.keyChar == CHAR_UNDEFINED ? "Undefined keyChar"
                        : String.valueOf(this.keyChar))
                + ",keyLocation=" + this.keyLocation;
    }

    /**
     * The extended code, which tells apart keys the common code confuses.
     *
     * <p>Here it always agrees with {@link #getKeyCode}: the extended code only parts from the
     * common one when the system reports the keyboard's physical layout, and this library has
     * nowhere to get that from.
     */
    public int getExtendedKeyCode() {
        return this.keyCode;
    }

    /**
     * The extended code corresponding to that character.
     *
     * @return the code, or {@link #VK_UNDEFINED} if the character has no key of its own
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
