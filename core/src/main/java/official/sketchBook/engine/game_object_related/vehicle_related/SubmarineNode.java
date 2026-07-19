package official.sketchBook.engine.game_object_related.vehicle_related;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.InteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.PhysicalObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.SimpleLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.OptmizedRenderableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.interact.InteractableObjectManagerComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.physics.MovableObjectPhysicsComponent;
import official.sketchBook.engine.components_related.physics.PhysicalMobLiquidInteractionComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;
import official.sketchBook.engine.components_related.system_utils.RenderableAndDefaultComponentManagerComponent;
import official.sketchBook.engine.components_related.vehicle.VehicleBaseComponent;
import official.sketchBook.engine.data_manager_related.BaseGameObjectDataManager;
import official.sketchBook.game.util_related.constants.DebugConstants;
import official.sketchBook.game.util_related.constants.WorldConstants;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.engine.util_related.helper.body.SubmarinePartBodyCreateHelper.createExternalBody;
import static official.sketchBook.engine.util_related.helper.body.SubmarinePartBodyCreateHelper.createInternalBody;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.toMeters;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.toPixels;

public class SubmarineNode
    implements
    MovableObjectII,
    SimpleLiquidInteractableObjectII,
    PhysicalObjectII,
    VehicleSection,
    OptmizedRenderableObjectII,
    Disposable {

    private World physicsWorld;

    /// Referência ao veículo dono desse node
    private Submarine vehicle;

    /// Lista de partes físicas
    private final List<SubmarinePart> physicalParts;

    private final List<VehicleBaseComponent> vehicleComponentList;

    /// Dado de massa atual
    private final MassData massData = new MassData();

    /// Componente para controle de movimentação do sub a partir de velocidade
    private MovementComponent moveC;

    /// Componente de física, para controle da física atravéz de pipelines já existentes
    private PhysicsComponent physicsC;

    /// Componente de transform contendo os dados de dimensões do nó
    private TransformComponent transformC;

    /// Componente para lidar com a interação com liquidos do submarino
    private PhysicalMobLiquidInteractionComponent liquidInteractionC;

    /// Gerênciador de componentes lógicos de funcionamento de objeto
    private final RenderableAndDefaultComponentManagerComponent managerC;

    private InteractableObjectManagerComponent interactableObjectManagerC;

    /// Body do submarino completo
    private Body
        internalBody,
        body;

    /// Dados bufferizados de velocidade para sincronização de objetos internos
    private float
        lastPosX = 0f,      //Antiga velocidade do eixo X
        lastPosY = 0f,      //Antiga velocidade do eixo Y
        velX = 0f,          //Atual velocidade do eixo X
        velY = 0f;          //Atual velocidade do eixo Y

    /// Flags de auxilio de estado
    private boolean
        inScreen,
        velInitialized = false,
        graphicsDisposed = false,
        disposed = false;

    /// Indíce de renderização
    public int renderIndex;

    public SubmarineNode(
        World physicsWorld,
        List<SubmarinePart> physicalParts,
        float centerX,
        float centerY,
        float centerZ,
        float rotation,
        boolean mirrorX,
        boolean mirrorY
    ) {

        this.physicsWorld = physicsWorld;

        this.physicalParts = physicalParts;

        this.vehicleComponentList = new ArrayList<>();

        for (SubmarinePart part : this.physicalParts) {
            part.setSection(this);
        }

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

        this.managerC = new RenderableAndDefaultComponentManagerComponent();

    }

    /// Inicialização de objeto
    public void initObject() {
        generateBody();
        initComponents();

        physicsC.halfWidth = transformC.getHalfWidth();
        physicsC.halfHeight = transformC.getHalfHeight();

        recalculateMass();

    }

    private void generateBody() {
        this.internalBody = createInternalBody(
            this,
            physicalParts,
            transformC,
            physicsWorld
        );

        this.body = createExternalBody(
            this,
            physicalParts,
            transformC,
            physicsWorld
        );

        calculateNodeDimensions();

    }

    public void calculateNodeDimensions() {
        if (physicalParts == null || physicalParts.isEmpty()) return;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        boolean hasValidPart = false;

        for (SubmarinePart part : physicalParts) {
            if (!part.isBoundsCalculated()) {
                SubmarinePart.calculateAndStoreBounds(part);
            }

            if (!part.isBoundsCalculated()) continue;

            hasValidPart = true;

            // Os bounds já estão em METROS, então comparamos direto
            if (part.internalMinX < minX) minX = part.internalMinX;
            if (part.internalMinY < minY) minY = part.internalMinY;
            if (part.internalMaxX > maxX) maxX = part.internalMaxX;
            if (part.internalMaxY > maxY) maxY = part.internalMaxY;
        }

        if (!hasValidPart) return;

        // Agora sim convertemos UMA VEZ SÓ de metros pra pixels
        float worldWidth = toPixels(maxX - minX);
        float worldHeight = toPixels(maxY - minY);

        transformC.width = worldWidth;
        transformC.height = worldHeight;
    }

    private void initComponents() {
        moveC = new MovementComponent(
            this,
            WorldConstants.SubmarineConstants.DEF_MAX_MOVE_SPEED_X,
            WorldConstants.SubmarineConstants.DEF_MAX_MOVE_SPEED_Y,
            WorldConstants.SubmarineConstants.DEF_MAX_MOVE_SPEED_R,
            WorldConstants.SubmarineConstants.DEF_MAX_SPEED_X,
            WorldConstants.SubmarineConstants.DEF_MAX_SPEED_Y,
            WorldConstants.SubmarineConstants.DEF_MAX_SPEED_R,
            WorldConstants.SubmarineConstants.X_DEACCELERATION,
            WorldConstants.SubmarineConstants.Y_DEACCELERATION,
            WorldConstants.SubmarineConstants.R_DEACCELERATION,
            true,
            true,
            true,
            false,
            true,
            false,
            false,
            false,
            false,
            false,
            true
        );

        liquidInteractionC = new PhysicalMobLiquidInteractionComponent(this);

        MovableObjectPhysicsComponent vPhysicsC = new MovableObjectPhysicsComponent(
            this,
            0,
            0,
            0,
            0,
            0
        );

//        vPhysicsC.autoApplyMovement = false;

        physicsC = vPhysicsC;

        interactableObjectManagerC = new InteractableObjectManagerComponent(internalBody);

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
            false
        );

        this.managerC.add(
            interactableObjectManagerC,
            false,
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

        applyLiquidYAxisToBody();
    }

    private void applyLiquidYAxisToBody() {
        float yVelocity = moveC.dataComponent.yAxis.velocity;

        if (yVelocity == 0f) return;

        Vector2 currentVel = body.getLinearVelocity();

        // Só sobrescreve o componente Y da velocidade, preserva X (controlado pelo motor)
        body.setLinearVelocity(currentVel.x, yVelocity);
    }

    public void postUpdate() {
        managerC.postUpdate();

        internalBody.setTransform(
            body.getPosition(),
            body.getAngle()
        );

        internalBody.setLinearVelocity(body.getLinearVelocity());

        physicsC.postUpdate();

        updateVelocity();
    }

    private void updateVelocity() {
        final float delta = physicsC.getDeltaTime();

        // evita divisão desnecessária
        if (delta == 0f) return;

        final Vector2 pos = body.getPosition();
        final float currentX = pos.x;
        final float currentY = pos.y;

        if (!velInitialized) {
            lastPosX = currentX;
            lastPosY = currentY;
            velX = 0f;
            velY = 0f;
            velInitialized = true;
            return;
        }

        final float invDelta = 1f / delta;

        velX = (currentX - lastPosX) * invDelta;
        velY = (currentY - lastPosY) * invDelta;

        lastPosX = currentX;
        lastPosY = currentY;
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
            float width = part.internalMaxX - part.internalMinX;
            float height = part.internalMaxY - part.internalMinY;

            float volume = width * height;
            float mass = part.getTotalMass();

            // Acumulamos tudo
            totalVolume += volume;
            totalMass += mass;
            weightedCenterX += centerX * mass;
            weightedCenterY += centerY * mass;
        }

        if (totalMass <= 0) return;

        // Aplicamos no box2d
        massData.mass = totalMass;
        massData.center.set(
            weightedCenterX / totalMass,
            weightedCenterY / totalMass
        );
        float w = toMeters(transformC.width);
        float h = toMeters(transformC.height);
        massData.I = totalMass * (w * w + h * h) / 12f;

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
    public int getRenderIndex() {
        return renderIndex;
    }

    @Override
    public void updateVisuals(float delta) {
        this.managerC.updateVisuals(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        //Chama o sistema de renderização dos componentes renderizáveis
        this.managerC.render(batch);

        if (!DebugConstants.show_hit_boxes) return;
        BaseGameObjectDataManager.toRender.add(
            this.transformC
        );
    }

    public void addVehicleComponent(
        VehicleBaseComponent component,
        boolean toRender,
        boolean toUpdate,
        boolean toPostUpdate
    ) {
        this.vehicleComponentList.add(component);

        if (toRender && component instanceof RenderableObjectII) this.managerC.addToRender(component);

        if(component instanceof InteractableObjectII) {
            this.interactableObjectManagerC.addToList((InteractableObjectII) component);
        }

        this.managerC.add(
            component,
            toUpdate,
            toPostUpdate
        );
    }

    @Override
    public boolean canRender() {
        return inScreen;
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
    public Vehicle getVehicle() {
        return vehicle;
    }

    @Override
    public Body getTriggerBody() {
        return interactableObjectManagerC.getTriggerBody();
    }

    public float getVelX() {
        return velX;
    }

    public float getVelY() {
        return velY;
    }

    public void setVehicle(Vehicle vehicle) {
        if (vehicle == this.vehicle ||
            !(vehicle instanceof Submarine) ||
            this.vehicle != null
        )
            return;
        this.vehicle = (Submarine) vehicle;
    }

    @Override
    public void disposeGraphics() {
        if (graphicsDisposed) return;

        managerC.disposeGraphics();

        graphicsDisposed = true;
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

        for (VehicleBaseComponent component : vehicleComponentList) {
            component.dispose();
        }

        vehicleComponentList.clear();
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

    }

    @Override
    public boolean hasInternalArea() {
        return true;
    }
}
