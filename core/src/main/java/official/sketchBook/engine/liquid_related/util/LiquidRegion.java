package official.sketchBook.engine.liquid_related.util;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderableObjectII;
import official.sketchBook.engine.components_related.objects.TransformComponent;

public class LiquidRegion implements RenderableObjectII {

    private boolean
        inScreen = true;

    /// Condensação de dados de coordenadas de dimensões
    private final TransformComponent transformC;
    public LiquidRegion(float x, float y, float width, float height) {
        transformC = new TransformComponent(
            x,
            y,
            0,
            0,
            width,
            height,
            1,
            1,
            false,
            false
        );
    }

    public float getX() {
        return transformC.x;
    }

    public float getY() {
        return transformC.y;
    }

    public float getWidth() {
        return transformC.width;
    }

    public float getHeight() {
        return transformC.height;
    }

    @Override
    public int getRenderIndex() {
        return (int) transformC.z;
    }

    @Override
    public void updateVisuals(float delta) {

    }

    @Override
    public void render(SpriteBatch batch) {

    }

    @Override
    public boolean canRender() {
        return true;
    }

    @Override
    public boolean isInScreen() {
        return inScreen;
    }

    @Override
    public void setInScreen(boolean inScreen) {
        this.inScreen = inScreen;
    }

    @Override
    public TransformComponent getTransformC() {
        return transformC;
    }

    @Override
    public void disposeGraphics() {

    }
}
