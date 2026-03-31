package official.sketchBook.engine.game_object_related.vehicle;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.MultiRenderableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderableObjectII;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.game_object_related.base_game_object.BaseGameObject;

import java.util.List;

public class Submarine extends BaseGameObject implements
    Vehicle,
    MultiRenderableObjectII{

    private List<SubmarineNode> submarineNodes;

    public int renderIndex;

    private boolean
        graphicsDisposed = false,
        inScreen;

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
        for (int i = 0; i < submarineNodes.size(); i++) {
            SubmarineNode node = submarineNodes.get(i);
            node.initObject();
            node.setVehicle(this);
        }
    }

    @Override
    public void update(float delta) {
        updateComponents(delta);

        for (int i = 0; i < submarineNodes.size(); i++) {
            submarineNodes.get(i).update(delta);
        }
    }

    @Override
    public void postUpdate() {
        postUpdateComponents();

        for (int i = 0; i < submarineNodes.size(); i++) {
            submarineNodes.get(i).postUpdate();
        }
    }

    @Override
    public void updateVisuals(float delta) {
        for(int i = 0; i < submarineNodes.size(); i++){
            submarineNodes.get(i).updateVisuals(delta);
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        for(int i = 0; i < submarineNodes.size(); i++){
            submarineNodes.get(i).render(batch);
        }
    }

    @Override
    protected void onObjectDestruction() {

    }

    @Override
    protected void disposeGeneralData() {
        for (int i = 0; i < submarineNodes.size(); i++) {
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
    public void disposeGraphics() {
        if(graphicsDisposed) return;

        for(int i = 0; i < submarineNodes.size(); i++){
            submarineNodes.get(i).disposeGraphics();
        }

        graphicsDisposed = true;
    }

    @Override
    public List<SubmarineNode> getSections() {
        return submarineNodes;
    }

    @Override
    public int getRenderIndex() {
        return renderIndex;
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
        return null;
    }

    @Override
    public List<SubmarineNode> getRenderableObjList() {
        return submarineNodes;
    }
}
