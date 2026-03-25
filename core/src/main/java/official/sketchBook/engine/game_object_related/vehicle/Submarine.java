package official.sketchBook.engine.game_object_related.vehicle;

import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.game_object_related.base_game_object.BaseGameObject;

import java.util.List;

public class Submarine extends BaseGameObject implements Vehicle {

    private List<SubmarineNode> submarineNodes;

    /// Importante ter em mente que a posição passada deverá ser o centro do sub, passado em pixels
    public Submarine(
        PhysicalGameObjectDataManager worldDataManager,
        List<SubmarineNode> submarineNodes
    ) {
        super(worldDataManager);

        this.submarineNodes = submarineNodes;

        initObject();
    }

    @Override
    protected void initObject() {
        for(int i = 0; i < submarineNodes.size(); i ++){
            submarineNodes.get(i).initObject(this);
        }
    }

    @Override
    public void update(float delta) {
        updateComponents(delta);

        for(int i = 0; i < submarineNodes.size(); i ++){
            submarineNodes.get(i).update(delta);
        }
    }

    @Override
    public void postUpdate() {
        postUpdateComponents();

        for(int i = 0; i < submarineNodes.size(); i ++){
            submarineNodes.get(i).postUpdate();
        }
    }

    @Override
    protected void onObjectDestruction() {

    }

    @Override
    protected void disposeGeneralData() {
        for(int i = 0; i < submarineNodes.size(); i ++){
            submarineNodes.get(i).dispose();
        }
    }

    @Override
    protected void disposeCriticalData() {
        super.disposeCriticalData();

        submarineNodes.clear();
        submarineNodes = null;
    }

    @Override
    public List<SubmarineNode> getSections() {
        return submarineNodes;
    }
}
