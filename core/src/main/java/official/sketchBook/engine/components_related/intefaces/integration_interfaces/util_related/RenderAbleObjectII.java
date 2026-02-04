package official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.components_related.objects.TransformComponent;

public interface RenderAbleObjectII {
    int getRenderIndex();
    void updateVisuals(float delta);
    void render(SpriteBatch batch);
    boolean canRender();

    TransformComponent getTransformC();

    boolean isGraphicsDisposed();
    void disposeGraphics();
}
