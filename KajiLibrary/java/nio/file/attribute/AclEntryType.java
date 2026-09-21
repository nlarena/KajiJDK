package java.nio.file.attribute;

// What an ACL entry does when it matches: it allows, denies, or only leaves a trace.
public enum AclEntryType {

    /** It gives access. */
    ALLOW,

    /** It denies access. */
    DENY,

    /** It does not change the access: it records the attempt in the audit log. */
    AUDIT,

    /** It does not change the access: it raises an alarm. */
    ALARM
}
