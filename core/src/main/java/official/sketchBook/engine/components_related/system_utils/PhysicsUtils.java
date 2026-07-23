package official.sketchBook.engine.components_related.system_utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public final class PhysicsUtils {

    private static final Vector2 TMP1 = new Vector2();
    private static final Vector2 TMP2 = new Vector2();

    private PhysicsUtils() {}

    public static float calculateMass(Body body) {
        float mass = 0f;

        for (Fixture fixture : body.getFixtureList()) {
            mass += calculateArea(fixture.getShape()) *
                fixture.getDensity();
        }

        return mass;
    }

    public static float calculateVolume(Body body) {
        float volume = 0f;

        for (Fixture fixture : body.getFixtureList()) {
            volume += calculateArea(fixture.getShape());
        }

        return volume;
    }

    public static float calculateDensity(Body body) {
        float volume = calculateVolume(body);

        if (volume <= 0f) {
            return 0f;
        }

        return calculateMass(body) / volume;
    }

    private static float calculateArea(Shape shape) {
        switch (shape.getType()) {

            case Circle:
                CircleShape circle = (CircleShape) shape;

                float r = circle.getRadius();
                return MathUtils.PI * r * r;

            case Polygon:
                return calculatePolygonArea(
                    (PolygonShape) shape
                );

            default:
                return 0f;
        }
    }

    private static float calculatePolygonArea(
        PolygonShape shape) {

        int count = shape.getVertexCount();
        float area = 0f;

        for (int i = 0; i < count; i++) {
            shape.getVertex(i, TMP1);
            shape.getVertex((i + 1) % count, TMP2);

            area +=
                TMP1.x * TMP2.y -
                    TMP2.x * TMP1.y;
        }

        return Math.abs(area * 0.5f);
    }
}
