package official.sketchBook.engine.gameObject_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderAbleObject;
import official.sketchBook.engine.components_related.objects.AnimationRenderingComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.dataManager_related.BaseGameObjectDataManager;

public abstract class AnimatedRenderableGameObject extends BaseGameObject implements RenderAbleObject {

    protected TransformComponent transformC;
    protected AnimationRenderingComponent animationRenderC;

    public AnimatedRenderableGameObject(
        float x,
        float y,
        float z,
        float rotation,
        float width,
        float height,
        float scaleX,
        float scaleY,
        boolean xAxisInverted,
        boolean yAxisInverted,
        BaseGameObjectDataManager worldDataManager
    ) {
        super(worldDataManager);
        this.transformC = new TransformComponent(
            x,
            y,
            z,
            rotation,
            width,
            height,
            scaleX,
            scaleY,
            xAxisInverted,
            yAxisInverted
        );

        animationRenderC = new AnimationRenderingComponent(transformC);

        initObject();
    }

    @Override
    public void update(float delta) {
        this.updateComponents(delta);
    }

    @Override
    public void postUpdate() {
        this.postUpdateComponents();
    }

    @Override
    public void updateVisuals(float delta) {
        this.animationRenderC.updateVisuals(delta);

    }

    @Override
    public void render(SpriteBatch batch) {
        this.animationRenderC.render(batch);
    }

    @Override
    public TransformComponent getTransformC() {
        return transformC;
    }

    public AnimationRenderingComponent getAnimationRenderC() {
        return animationRenderC;
    }

    @Override
    public int getRenderIndex() {
        return (int) transformC.z;
    }

    @Override
    protected void disposeData() {
        animationRenderC.dispose();
    }
}
