package javax.smartcardio;

import java.security.Permission;

/**
 * KajiLibrary's javax.smartcardio.CardPermission -- permission to talk to a reader.
 *
 * <p>The name is the reader's, or {@code "*"} for all of them. There are six actions:
 *
 * <ul>
 *   <li>{@code connect}: connect to the card;
 *   <li>{@code reset}: reset it, which wipes the state of anybody else using it;
 *   <li>{@code exclusive}: keep the reader for one only;
 *   <li>{@code transmitControl}: send commands to the reader, not to the card;
 *   <li>{@code getBasicChannel}: use the basic channel;
 *   <li>{@code openLogicalChannel}: open a new logical channel.
 * </ul>
 *
 * <p>{@code "*"} is also valid as an action and means all six. Naming them all gives the same: the
 * canonical form {@link #getActions} returns collapses them into an asterisk.
 *
 * <p>That canonical form orders the actions <b>alphabetically</b>, not by importance; two
 * permissions with the same actions in a different order are equal.
 */
public class CardPermission extends Permission {

    private static final long serialVersionUID = 7146787880530705613L;

    /** Connect to the card. */
    private static final int CONNECT = 0x1;

    /** Keep the reader. */
    private static final int EXCLUSIVE = 0x2;

    /** Use the basic channel. */
    private static final int GET_BASIC_CHANNEL = 0x4;

    /** Open a logical channel. */
    private static final int OPEN_LOGICAL_CHANNEL = 0x8;

    /** Reset the card. */
    private static final int RESET = 0x10;

    /** Send commands to the reader. */
    private static final int TRANSMIT_CONTROL = 0x20;

    /** All six together. */
    private static final int ALL = CONNECT | EXCLUSIVE | GET_BASIC_CHANNEL | OPEN_LOGICAL_CHANNEL
        | RESET | TRANSMIT_CONTROL;

    /** The names, in bit order, which is the alphabetical one. */
    private static final String[] ACTION_NAMES = {
        "connect", "exclusive", "getBasicChannel", "openLogicalChannel", "reset", "transmitControl",
    };

    /** Which actions are permitted. */
    private final int mask;

    /** The canonical form, or null if it was built without actions. */
    private final String actions;

    /**
     * That permission on that reader.
     *
     * @param name the reader's name, or {@code "*"}
     * @param actions the actions separated by commas, {@code "*"}, or null
     * @throws NullPointerException if the name is null
     * @throws IllegalArgumentException if the actions are the empty string or one does not exist
     */
    public CardPermission(String name, String actions) {
        super(name);
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        this.mask = getMask(actions);
        this.actions = actions == null ? null : canonicalize(this.mask);
    }

    /** The actions in canonical form, or null if it was built without them. */
    @Override
    public String getActions() {
        return this.actions;
    }

    /**
     * Whether this permission covers what the other asks for.
     *
     * <p>Both things are needed: that the name be the same or ours be {@code "*"}, and that our
     * actions include all of theirs.
     */
    @Override
    public boolean implies(Permission permission) {
        if (!(permission instanceof CardPermission)) {
            return false;
        }
        CardPermission other = (CardPermission) permission;
        if ((this.mask & other.mask) != other.mask) {
            return false;
        }
        return "*".equals(getName()) || getName().equals(other.getName());
    }

    /** Two permissions are equal if they name the same reader and permit the same actions. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CardPermission)) {
            return false;
        }
        CardPermission other = (CardPermission) obj;
        return this.mask == other.mask && getName().equals(other.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode() + 31 * this.mask;
    }

    /**
     * The bits of that list of actions.
     *
     * <p>It does not trim spaces on purpose: {@code " connect "} is not an action, and accepting it
     * quietly would let badly written lists through that later give permissions nobody meant to
     * give.
     */
    private static int getMask(String actions) {
        if (actions == null) {
            return 0;
        }
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("actions must not be empty");
        }
        if ("*".equals(actions)) {
            return ALL;
        }
        int mask = 0;
        String[] pieces = actions.split(",", -1);
        int i = 0;
        while (i < pieces.length) {
            int bit = bitFor(pieces[i]);
            if (bit == 0) {
                throw new IllegalArgumentException("Invalid action: '" + pieces[i] + "'");
            }
            mask = mask | bit;
            i = i + 1;
        }
        return mask;
    }

    /** The bit of that action, case-insensitively, or zero if it does not exist. */
    private static int bitFor(String action) {
        int i = 0;
        while (i < ACTION_NAMES.length) {
            if (ACTION_NAMES[i].equalsIgnoreCase(action)) {
                return 1 << i;
            }
            i = i + 1;
        }
        return 0;
    }

    /** The canonical form of those bits. See the class note. */
    private static String canonicalize(int mask) {
        if (mask == ALL) {
            return "*";
        }
        StringBuilder text = new StringBuilder();
        int i = 0;
        while (i < ACTION_NAMES.length) {
            if ((mask & (1 << i)) != 0) {
                if (text.length() > 0) {
                    text.append(',');
                }
                text.append(ACTION_NAMES[i]);
            }
            i = i + 1;
        }
        return text.toString();
    }
}
