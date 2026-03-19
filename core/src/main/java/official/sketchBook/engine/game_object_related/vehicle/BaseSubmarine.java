package official.sketchBook.engine.game_object_related.vehicle;

import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.LiquidInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalGameObjectII;
import official.sketchBook.engine.components_related.movement.MovableObjectPhysicsComponent;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.physics.PhysicalMobLiquidInteractionComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;
import official.sketchBook.engine.data_manager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.game_object_related.base_game_object.BaseGameObject;
import official.sketchBook.game.util_related.constants.WorldConstants;

import java.util.List;

import static official.sketchBook.engine.util_related.helper.body.SubmarinePartBodyCreateHelper.createExternalBody;
import static official.sketchBook.engine.util_related.helper.body.SubmarinePartBodyCreateHelper.createInternalBody;

public class BaseSubmarine extends BaseGameObject
    implements
    MovableObjectII,
    LiquidInteractableObjectII,
    PhysicalGameObjectII,
    Vehicle {

    /// Componente para controle de movimentação do sub
    private MovementComponent moveC;

    /// Componente de físicva
    private PhysicsComponent physicsC;

    /// Componente de transform contendo os dados do sub completo
    private TransformComponent transformC;

    /// Componente para lidar com a interação com liquidos do submarino
    private PhysicalMobLiquidInteractionComponent liquidInteractionC;

    /// Body do submarino completo
    private Body
        internalBody,
        body;

    /// Lista de partes de submarino
    private final List<BaseSubmarinePart> subParts;


    /// Importante ter em mente que a posição passada deverá ser o centro do sub, passado em pixels
    public BaseSubmarine(
        BaseGameObjectDataManager worldDataManager,
        List<BaseSubmarinePart> subParts,
        float x,
        float y,
        float z,
        float rotation,
        boolean mirrorX,
        boolean mirrorY
    ) {
        super(worldDataManager);

        this.subParts = subParts;

        transformC = new TransformComponent(
            x,
            y,
            z,
            rotation,
            0,
            0,
            1,
            1,
            mirrorX,
            mirrorY
        );

        initObject();
    }

    @Override
    protected void initObject() {
        initComponents();
        generateBody(subParts);
    }

    private void generateBody(List<BaseSubmarinePart> parts) {
        this.internalBody = createInternalBody(
            this,
            parts,
            transformC,
            getPhysicalManager().getPhysicsWorld()
        );
        this.body = createExternalBody(
            this,
            parts,
            transformC,
            internalBody,
            getPhysicalManager().getPhysicsWorld()
        );
    }

    private void initComponents() {
        physicsC = new MovableObjectPhysicsComponent(
            this,
            0,
            0,
            0,
            0,
            0
        );

        moveC = new MovementComponent(
            this,
            WorldConstants.SubmarineConstants.DEF_MAX_SPEED_X,
            WorldConstants.SubmarineConstants.DEF_MAX_SPEED_Y,
            0,
            WorldConstants.SubmarineConstants.X_DEACCELERATION,
            WorldConstants.SubmarineConstants.Y_DEACCELERATION,
            0,
            true,
            true,
            false,
            true,
            true,
            false,
            true,
            true,
            false,
            false,
            false
        );

        liquidInteractionC = new PhysicalMobLiquidInteractionComponent(this);

        this.managerC.add(
            moveC,
            true,
            false
        );

        this.managerC.add(
            liquidInteractionC,
            true,
            false
        );

        this.managerC.add(
            physicsC,
            true,
            true
        );
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
        for (BaseSubmarinePart parts : subParts) {
            parts.dispose();
        }

        subParts.clear();

    }

    @Override
    protected void disposeCriticalData() {
        super.disposeCriticalData();

        this.body = null;
        this.internalBody = null;

        this.moveC = null;
        this.transformC = null;
        this.physicsC = null;
        this.liquidInteractionC = null;
    }

    public List<BaseSubmarinePart> getSubParts() {
        return subParts;
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

    @Override
    public Body getInternalBody() {
        return internalBody;
    }
}
