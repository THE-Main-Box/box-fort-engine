package official.sketchBook.engine.game_object_related.vehicle;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.LiquidInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.physics.MovableObjectPhysicsComponent;
import official.sketchBook.engine.components_related.physics.PhysicalMobLiquidInteractionComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;
import official.sketchBook.engine.components_related.physics.VehiclePhysicsComponent;
import official.sketchBook.engine.components_related.system_utils.ComponentManagerComponent;
import official.sketchBook.engine.util_related.helper.body.SubmarinePartBodyCreateHelper;
import official.sketchBook.game.util_related.constants.WorldConstants;

import java.util.List;

import static official.sketchBook.engine.util_related.helper.body.SubmarinePartBodyCreateHelper.*;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;

public class SubmarineNode
    implements
    MovableObjectII,
    LiquidInteractableObjectII,
    PhysicalObjectII,
    Vehicle,
    Disposable {

    private World physicsWorld;

    /// Componente para controle de movimentação do sub a partir de velocidade
    private MovementComponent moveC;

    /// Componente de física, para controle da física atravéz de pipelines já existentes
    private PhysicsComponent physicsC;

    /// Componente de transform contendo os dados de dimensões do nó
    private TransformComponent transformC;

    /// Componente para lidar com a interação com liquidos do submarino
    private PhysicalMobLiquidInteractionComponent liquidInteractionC;

    /// Body do submarino completo
    private Body
        internalBody,
        body;

    private ComponentManagerComponent managerC;

    /// Lista de nós de massa
    private final List<SubmarinePart> physicalParts;

    /// Dado de massa atual
    private final MassData massData = new MassData();

    /// Massa total do node
    private boolean disposed = false;

    public SubmarineNode(
        World physicsWorld,
        List<SubmarinePart> submarineParts,
        float centerX,
        float centerY,
        float centerZ,
        float rotation,
        boolean mirrorX,
        boolean mirrorY
    ) {

        this.physicsWorld = physicsWorld;
        this.physicalParts = submarineParts;

        transformC = new TransformComponent(
            centerX,
            centerY,
            centerZ,
            rotation,
            0,
            0,
            1,
            1,
            mirrorX,
            mirrorY
        );

    }

    public void initObject() {
        initComponents();
        generateBody(physicalParts);
    }

    private void generateBody(List<SubmarinePart> parts) {
        this.internalBody = createInternalBody(
            this,
            parts,
            transformC,
            physicsWorld
        );
        this.body = createExternalBody(
            this,
            parts,
            transformC,
            physicsWorld
        );

        recalculateMass();
    }

    private void initComponents() {
        this.managerC = new ComponentManagerComponent();

        VehiclePhysicsComponent vPhysicsC = new VehiclePhysicsComponent(
            this,
            0,
            0,
            0,
            0,
            0
        );

//        vPhysicsC.autoApplyMovement = false;
        vPhysicsC.autoConstraintR = false;

        physicsC = vPhysicsC;

        moveC = new MovementComponent(
            this,
            WorldConstants.SubmarineConstants.DEF_MAX_SPEED_X,
            WorldConstants.SubmarineConstants.DEF_MAX_SPEED_Y,
            WorldConstants.SubmarineConstants.DEF_MAX_SPEED_R,
            WorldConstants.SubmarineConstants.X_DEACCELERATION,
            WorldConstants.SubmarineConstants.Y_DEACCELERATION,
            WorldConstants.SubmarineConstants.R_DEACCELERATION,
            true,
            true,
            true,
            true,
            true,
            false,
            false,
            true,
            false,
            false,
            true
        );

        liquidInteractionC = new PhysicalMobLiquidInteractionComponent(this);

        liquidInteractionC.setCanInteractWithLiquid(false);

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
    public void onLiquidExit() {
    }

    @Override
    public void onLiquidEnter() {

    }

    @Override
    public void inLiquidUpdate() {

    }

    @Override
    public void onObjectAndBodyPosSync() {

    }

    public void update(float delta) {
        managerC.update(delta);
    }

    public void postUpdate() {
        managerC.postUpdate();

        internalBody.setTransform(
            body.getPosition(),
            body.getAngle()
        );
        internalBody.setLinearVelocity(body.getLinearVelocity());
    }

    public void recalculateMass() {
        float totalMass = 0;
        float totalVolume = 0;
        float weightedCenterX = 0;
        float weightedCenterY = 0;

        SubmarinePart part;
        for (int i = 0; i < physicalParts.size(); i++) {
            part = physicalParts.get(i);
            if (!part.isBoundsCalculated()) continue;

            // Centro geométrico da parte
            float centerX = (part.internalMinX + part.internalMaxX) / 2f;
            float centerY = (part.internalMinY + part.internalMaxY) / 2f;

            // Volume e massa da parte
            float width  = (part.internalMaxX - part.internalMinX) * PPM;
            float height = (part.internalMaxY - part.internalMinY) * PPM;
            float volume = width * height;
            float mass   = part.getTotalMass();

            // Acumulamos tudo
            totalVolume        += volume;
            totalMass          += mass;
            weightedCenterX    += centerX * mass;
            weightedCenterY    += centerY * mass;
        }

        if (totalMass <= 0) return;

        // Aplicamos no box2d
        massData.mass = totalMass;
        massData.center.set(
            weightedCenterX / totalMass,
            weightedCenterY / totalMass
        );
        massData.I = body.getInertia();
        body.setMassData(massData);

        // Atualizamos o simulador de interação com líquidos
        liquidInteractionC.setMass(totalMass);
        liquidInteractionC.setVolume(totalVolume);
    }

    @Override
    public Body getBody() {
        return body;
    }

    @Override
    public Body getInternalBody() {
        return internalBody;
    }

    @Override
    public MovementComponent getMoveC() {
        return moveC;
    }

    @Override
    public TransformComponent getTransformC() {
        return transformC;
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
    public void dispose() {
        if (disposed) return;

        componentsDispose();

        nullifyReferences();
        disposed = true;
    }

    private void componentsDispose() {
        managerC.dispose();

        for (SubmarinePart parts : physicalParts) {
            parts.dispose();
        }

        physicalParts.clear();
        physicsWorld.destroyBody(internalBody);

    }

    private void nullifyReferences() {

        this.physicsWorld = null;

        this.internalBody = null;
        this.body = null;

        this.moveC = null;
        this.transformC = null;
        this.physicsC = null;
        this.liquidInteractionC = null;

        this.managerC = null;

    }
}
