package official.sketchBook.engine.gameObject_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderAbleObject;
import official.sketchBook.engine.components_related.objects.AnimationRenderingComponent;
import official.sketchBook.engine.dataManager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

public abstract class AnimatedRenderableRoomGameObject extends BaseRoomGameObject implements RenderAbleObject {

    protected AnimationRenderingComponent animationRenderC;

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

        initObject();
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

    public AnimationRenderingComponent getAnimationRenderC() {
        return animationRenderC;
    }

    @Override
    public int getRenderIndex() {
        return (int) transformC.z;
    }
}
