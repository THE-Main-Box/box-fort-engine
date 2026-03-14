package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.physics.box2d.Fixture;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.ray_cast.RayCastData;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyTagHelper;
import official.sketchBook.engine.util_related.pools.RayCastPool;

import static official.sketchBook.game.util_related.constants.DebugConstants.show_ray_cast;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;

public class RayCastGroundDetectionComponent implements Component {

    private PhysicalObjectII object;
    private RayCastPool pool;
    private ObjectType[] validGroundType;

    public float footOffsetY = 1f;
    public float rayLength = 4f;
    public float footMargin = 1f;

    private boolean
        disposed = false,
        onGround = false;

    public RayCastGroundDetectionComponent(
        PhysicalObjectII object,
        RayCastPool pool,
        ObjectType... validGroundTypes
    ) {
        this.object = object;
        this.validGroundType = validGroundTypes;
        this.pool = pool;
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void postUpdate() {
        detectGround();
    }

    private void detectGround() {
        TransformComponent transformC = object.getTransformC();

        float halfWidth = transformC.getHalfWidth() / PPM;
        float halfHeight = transformC.getHalfHeight() / PPM;
        float centerX = transformC.getCenterX() / PPM;
        float centerY = transformC.getCenterY() / PPM;

        float footY = centerY - halfHeight + footOffsetY / PPM;
        float rayLengthM = rayLength / PPM;
        float margin = footMargin / PPM;

        float[] xOffsets = {
            -halfWidth + margin,
            0f,
            halfWidth - margin
        };

        onGround = false;

        for (float dx : xOffsets) {
            float startX = centerX + dx;

            RayCastData result = pool.cast(
                startX, footY,
                startX, footY - rayLengthM,
                false
            );

            boolean hit = result.hasHit() && isValidGround(result.fixture);


            if (!show_ray_cast)
                pool.free(result);

            if (hit) {
                onGround = true;
                return;
            }
        }
    }

    private boolean isValidGround(Fixture fixture) {
        GameObjectTag tag = BodyTagHelper.getFromBodyTag(fixture);
        if (tag == null) return false;

        for (ObjectType type : validGroundType) {
            if (tag.type == type) return true;
        }

        return false;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        nullifyReferences();
        disposed = true;
    }

    @Override
    public void nullifyReferences() {
        validGroundType = null;
        object = null;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    public boolean isOnGround() {
        return onGround;
    }
}
