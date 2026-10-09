package java.awt;

/**
 * How a shape's outline is turned into the filled shape that is really painted.
 *
 * <p>It is one method, and of {@code java.awt}'s five painting interfaces it is the only one that
 * mentions nothing from {@code java.awt.image}.
 */
public interface Stroke {

    /**
     * Returns the shape that has to be filled for {@code p}'s stroke to show. It does not draw:
     * thickness, caps and joins are resolved here, in geometry, and the rasterizer then just fills.
     */
    Shape createStrokedShape(Shape p);
}
