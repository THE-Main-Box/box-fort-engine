package official.sketchBook.engine.game_object_related.sub_related;

import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.LiquidInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalGameObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.movement.PhysicalMobLiquidInteractionComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;
import official.sketchBook.engine.data_manager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.game_object_related.base_game_object.BaseGameObject;

import java.util.List;

public class BaseSubmarine extends BaseGameObject
    implements
    MovableObjectII,
    LiquidInteractableObjectII,
    PhysicalGameObjectII
{

    /// Componente para controle de movimentação do sub
    private MovementComponent moveC;

    /// Componente de físicva
    private PhysicsComponent physicsC;

    /// Componente de transform contendo os dados do sub completo
    private TransformComponent transformC;

    /// Componente para lidar com a interação com liquidos do submarino
    private PhysicalMobLiquidInteractionComponent liquidInteractionC;

    /// Body do submarino completo
    private Body body;

    /// Lista de partes de submarino
    private List<BaseSubmarineParts> subParts;


    public BaseSubmarine(BaseGameObjectDataManager worldDataManager) {
        super(worldDataManager);
    }

    @Override
    protected void initObject() {

    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void postUpdate() {

    }

    @Override
    protected void onObjectDestruction() {

    }

    @Override
    public void onObjectAndBodyPosSync() {

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
    protected void disposeGeneralData() {

    }

    @Override
    protected void disposeCriticalData() {
        super.disposeCriticalData();

        this.body = null;
        this.moveC = null;
        this.transformC = null;
        this.physicsC = null;
        this.liquidInteractionC = null;
    }

    @Override
    public PhysicalGameObjectDataManager getPhysicalManager() {
        return (PhysicalGameObjectDataManager) this.worldDataManager;
    }

    @Override
    public TransformComponent getTransformC() {
        return transformC;
    }

    @Override
    public MovementComponent getMoveC() {
        return moveC;
    }

    @Override
    public PhysicsComponent getPhysicsC() {
        return physicsC;
    }

    @Override
    public PhysicalMobLiquidInteractionComponent getLiquidInteractionC() {
        return liquidInteractionC;
    }

    @Override
    public Body getBody() {
        return body;
    }
}
