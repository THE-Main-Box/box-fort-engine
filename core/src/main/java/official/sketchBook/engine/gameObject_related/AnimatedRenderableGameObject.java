package official.sketchBook.engine.gameObject_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.animation_related.ObjectAnimationPlayer;
import official.sketchBook.engine.animation_related.SpriteSheetDataHandler;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderAbleObject;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.dataManager_related.BaseWorldDataManager;

import java.util.List;

public abstract class AnimatedRenderableGameObject extends BaseGameObject implements RenderAbleObject {

    protected TransformComponent transformC;

    protected List<SpriteSheetDataHandler> spriteHandlerList;
    protected List<ObjectAnimationPlayer> animationPlayerList;

    public boolean isRenderDimensionEqualsToObject = true;

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
        BaseWorldDataManager worldDataManager
    ) {
        super(worldDataManager);
        this.initTransformComponent(
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

    /// Inicia um novo componente de transformação
    protected void initTransformComponent(
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
        if (spriteHandlerList.isEmpty() || animationPlayerList.isEmpty()) return;

        for (int i = 0; i < spriteHandlerList.size(); i++) {
            SpriteSheetDataHandler currentHandler = spriteHandlerList.get(i);
            ObjectAnimationPlayer currentAnimationPlayer = animationPlayerList.get(i);

            currentHandler.updatePosition(
                transformC.x,
                transformC.y
            );

            currentHandler.setRotation(
                transformC.rotation
            );

            currentHandler.xAxisInvert = transformC.xAxisInverted;
            currentHandler.yAxisInvert = transformC.yAxisInverted;

            if (isRenderDimensionEqualsToObject) {
                currentHandler.renderWidth = transformC.width;
                currentHandler.renderHeight = transformC.height;
            }

            if (currentAnimationPlayer != null) {
                currentAnimationPlayer.update(delta);
            }

        }

    }

    @Override
    public void render(SpriteBatch batch) {
        if (spriteHandlerList.isEmpty() || animationPlayerList.isEmpty()) return;

        //renderizamos primeiro tudo o que tivermos para renderizar do objeto do jogador
        for (int i = 0; i < spriteHandlerList.size(); i++) {

            //Obtemos o nosso handler e chamamos para renderizar
            spriteHandlerList.get(i).renderSprite(
                batch,
                animationPlayerList.get(i).getCurrentSprite()
            );

        }
    }

    @Override
    public TransformComponent getTransformC() {
        return transformC;
    }

    @Override
    public int getRenderIndex() {
        return (int) transformC.z;
    }

    public List<SpriteSheetDataHandler> getSpriteHandlerList() {
        return spriteHandlerList;
    }

    public List<ObjectAnimationPlayer> getAnimationPlayerList() {
        return animationPlayerList;
    }
}
