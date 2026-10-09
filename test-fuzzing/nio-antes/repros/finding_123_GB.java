// #123: GB es subtipo de GA, pero la relacion pasa por un extends PARAMETRIZADO.
import java.util.Set;
public interface finding_123_GB<X, E> extends finding_123_GA<X, Set<E>, E> {}
