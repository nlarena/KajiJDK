package java.nio.file;

// What a `FileVisitor` answers the walk with to decide how it carries on.
//
// There are four and not a boolean because the cut has three different reaches: stop everything
// (`TERMINATE`), do not descend into this directory (`SKIP_SUBTREE`), or look at no more siblings
// at this level (`SKIP_SIBLINGS`).
public enum FileVisitResult {

    /** To go on normally. */
    CONTINUE,

    /** End the whole walk. */
    TERMINATE,

    /** Do not go into this directory; carry on with the siblings. */
    SKIP_SUBTREE,

    /** Do not look at the remaining siblings; go up a level. */
    SKIP_SIBLINGS
}
