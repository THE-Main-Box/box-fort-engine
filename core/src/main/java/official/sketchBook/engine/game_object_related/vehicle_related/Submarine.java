package official.sketchBook.engine.game_object_related.vehicle_related;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.MultiLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.CompositeRenderableObjectII;
import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.game_object_related.base_game_object.BaseRoomGameObject;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

import java.util.List;

public class Submarine extends BaseRoomGameObject implements
    MultiLiquidInteractableObjectII,
    Vehicle,
    CompositeRenderableObjectII{

    private final List<SubmarineNode> submarineNodeList;

    public int renderIndex;

    /// Importante ter em mente que a posição passada deverá ser o centro do sub, passado em pixels
    public Submarine(
        PhysicalGameObjectDataManager worldDataManager,
        PlayableRoom ownerRoom,
        List<SubmarineNode> submarineNodeList
    ) {
        super(
            worldDataManager,
            ownerRoom,
            RoomObjectScope.GLOBAL
        );

        this.submarineNodeList = submarineNodeList;

        initObject();
    }

    @Override
    public void initObject() {
        for (int i = 0; i < submarineNodeList.size(); i++) {
            SubmarineNode node = submarineNodeList.get(i);
            node.initObject();
            node.setVehicle(this);
        }
    }

    @Override
    public void update(float delta) {
        updateComponents(delta);

        for (int i = 0; i < submarineNodeList.size(); i++) {
            submarineNodeList.get(i).update(delta);
        }
    }

    @Override
    public void postUpdate() {
        postUpdateComponents();

        for (int i = 0; i < submarineNodeList.size(); i++) {
            submarineNodeList.get(i).postUpdate();
        }
    }

    @Override
    protected void onObjectDestruction() {

    }

    @Override
    protected void disposeGeneralData() {
        for (int i = 0; i < submarineNodeList.size(); i++) {
            submarineNodeList.get(i).dispose();
        }
    }

    @Override
    protected void disposeCriticalData() {
        super.disposeCriticalData();
    }

    @Override
    public List<SubmarineNode> getSections() {
        return submarineNodeList;
    }

    @Override
    public int getRenderIndex() {
        return renderIndex;
    }

    @Override
    public List<SubmarineNode> getRenderableObjList() {
        return submarineNodeList;
    }


    @Override
    public void onLiquidExit() {

    }

    @Override
    public void onLiquidEnter() {

    }

    @Override
    public void inLiquidUpdate() {

    }

    @Override
    public List<SubmarineNode> getLiquidIObj() {
        return submarineNodeList;
    }
}
