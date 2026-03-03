package official.sketchBook.engine.game_object_related.sub_related;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
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
import official.sketchBook.game.util_related.constants.WorldConstants;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.*;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;

public class BaseSubmarine extends BaseGameObject
    implements
    MovableObjectII,
    LiquidInteractableObjectII,
    PhysicalGameObjectII {

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
    private final List<BaseSubmarineParts> subParts = new ArrayList<>();


    /// Importante ter em mente que a posição passada deverá ser o centro do sub, passado em pixels
    public BaseSubmarine(
        BaseGameObjectDataManager worldDataManager,
        float x,
        float y,
        float z,
        float rotation,
        boolean mirrorX,
        boolean mirrorY
    ) {
        super(worldDataManager);

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

    private void generateBody(List<BaseSubmarineParts> parts) {
        // cria a body vazia na posição do sub
        BodyDef bodyDef = new BodyDef();

        bodyDef.type = BodyDef
            .BodyType
            .DynamicBody;

        bodyDef.position.set(
            transformC.x / PPM,
            transformC.y / PPM
        );

        this.body = getPhysicalManager()
            .getPhysicsWorld()
            .createBody(bodyDef);

        // itera as partes e adiciona as fixtures
        for (BaseSubmarineParts part : parts) {
            for (FixtureDef def : part.fixtureDataList) {
                Fixture f = body.createFixture(def);
                f.setUserData(part); // referência à parte para identificar no contato
                def.shape.dispose();
            }
        }
    }

    private void initComponents() {
        physicsC = new PhysicsComponent(
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
        for(BaseSubmarineParts parts : subParts){
            parts.dispose();
        }

        subParts.clear();

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

    public List<BaseSubmarineParts> getSubParts() {
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
}
