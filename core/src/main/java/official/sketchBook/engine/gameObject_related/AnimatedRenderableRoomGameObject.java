package official.sketchBook.engine.gameObject_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderAbleObjectII;
import official.sketchBook.engine.components_related.objects.AnimationRenderingComponent;
import official.sketchBook.engine.dataManager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

public abstract class AnimatedRenderableRoomGameObject extends BaseRoomGameObject implements RenderAbleObjectII {

    protected AnimationRenderingComponent animationRenderC;

    private boolean graphicsDisposed = false;

    public AnimatedRenderableRoomGameObject(
        BaseGameObjectDataManager worldDataManager,
        PlayableRoom ownerRoom,
        RoomObjectScope roomScope,
        float x,
        float y,
        float z,
        float rotation,
        float width,
        float height,
        float scaleX,
        float scaleY,
        boolean xAxisInverted,
        boolean yAxisInverted
    ) {
        super(
            worldDataManager,
            ownerRoom,
            roomScope,
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
    }

    @Override
    protected void initObject() {
        this.animationRenderC = new AnimationRenderingComponent(this.transformC);
    }

    @Override
    public void updateVisuals(float delta) {
        animationRenderC.updateVisuals(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        animationRenderC.render(batch);
    }

    public final void disposeGraphics(){
        if(graphicsDisposed) return;
        executeDisposeGraphics();
        graphicsDisposed = true;
    }

    protected abstract void executeDisposeGraphics();

    @Override
    protected void disposeCriticalData() {
        super.disposeCriticalData();
        this.animationRenderC.dispose();
        this.animationRenderC = null;
    }

    public AnimationRenderingComponent getAnimationRenderC() {
        return animationRenderC;
    }

    @Override
    public int getRenderIndex() {
        return (int) transformC.z;
    }

    @Override
    public boolean isGraphicsDisposed() {
        return graphicsDisposed;
    }
}
