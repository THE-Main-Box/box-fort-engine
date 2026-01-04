package official.sketchBook.engine.components_related.intefaces.integration_interfaces;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.components_related.objects.TransformComponent;

public interface RenderAbleObject {

    boolean isxAxisInverted();
    boolean isyAxisInverted();

    int getRenderIndex();
    void updateVisuals(float delta);
    void render(SpriteBatch batch);
    boolean isPendingRemoval();

    TransformComponent getTransformC();

}
