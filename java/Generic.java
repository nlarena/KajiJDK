import java.util.List;
public class Generic<T> {
    List<String> field;
    <U> U pick(U a, U b) { return a; }
}
