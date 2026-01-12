package official.sketchBook.engine.gameObject_related;

import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.dataManager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

public abstract class BaseRoomGameObject extends BaseGameObject {

    protected TransformComponent transformC;
    protected PlayableRoom ownerRoom;
    public final RoomObjectScope roomScope;

    public BaseRoomGameObject(
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
        super(worldDataManager);

        this.roomScope = roomScope;

        this.transformC = TransformComponent.initNewTransformComponent(
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
        setOwnerRoom(ownerRoom);

    }

    @Override
    public void update(float delta) {
        updateComponents(delta);
    }

    @Override
    public void postUpdate() {
        postUpdateComponents();
    }

    @Override
    protected void onObjectDestruction() {
        if(ownerRoom != null){
            ownerRoom.removeRoomObject(this);
        }
    }

    public void onRoomSwitch(PlayableRoom oldRoom, PlayableRoom newRoom){
        setOwnerRoom(newRoom);
    }

    public PlayableRoom getOwnerRoom() {
        return ownerRoom;
    }

    public void setOwnerRoom(PlayableRoom ownerRoom) {
        this.ownerRoom = ownerRoom;
        if(ownerRoom != null){
            this.ownerRoom.addNewRoomGameObject(this);
        }
    }

    public TransformComponent getTransformC() {
        return transformC;
    }
}
