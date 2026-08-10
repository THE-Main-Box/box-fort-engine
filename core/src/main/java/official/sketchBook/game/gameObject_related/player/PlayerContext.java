package official.sketchBook.game.gameObject_related.player;

import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.util_related.serialization.base_context.GameObjectContext;
import official.sketchBook.engine.util_related.serialization.base_context.LiquidInteractionObjectContext;
import official.sketchBook.engine.util_related.serialization.base_context.RoomObjectContext;
import official.sketchBook.engine.util_related.serialization.base_context.TransformContext;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

public class PlayerContext implements
    GameObjectContext<PhysicalGameObjectDataManager>,
    RoomObjectContext,
    TransformContext,
    LiquidInteractionObjectContext {

    private final GOContextData<PhysicalGameObjectDataManager> gameObjectData;
    private final ROContextData roomObjectData;
    private final TContextData transformData;
    private final LIOContextData liquidInteractData;

    public PlayerContext(
        PhysicalGameObjectDataManager worldDataManager,
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
        boolean mirrorX,
        boolean mirrorY,
        float mass,
        float volume,
        boolean canInteract
    ) {
        this.gameObjectData = new GOContextData<>(
            worldDataManager
        );

        this.roomObjectData = new ROContextData(
            ownerRoom,
            roomScope
        );
        this.liquidInteractData = new LIOContextData(
            mass,
            volume,
            canInteract
        );

        this.transformData = new TContextData(
            x,
            y,
            z,
            rotation,
            width,
            height,
            scaleX,
            scaleY,
            mirrorX,
            mirrorY
        );
    }

    @Override
    public ROContextData getRoomObjectContextData() {
        return roomObjectData;
    }

    @Override
    public GOContextData<PhysicalGameObjectDataManager> getGameObjectContextData() {
        return gameObjectData;
    }

    @Override
    public TContextData getTransformContextData() {
        return transformData;
    }

    @Override
    public LIOContextData getLiquidInteractionContextData() {
        return liquidInteractData;
    }
}

