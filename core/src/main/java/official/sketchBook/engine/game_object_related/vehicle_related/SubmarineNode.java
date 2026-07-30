package official.sketchBook.engine.game_object_related.vehicle_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.InteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.PhysicalLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.PhysicalObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.vehicle.SubmarinePassenger;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.vehicle.VehiclePassenger;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.OptmizedRenderableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderableObjectII;
import official.sketchBook.engine.components_related.interact.InteractableObjectManagerComponent;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.physics.MovableObjectPhysicsComponent;
import official.sketchBook.engine.components_related.physics.PhysicalLiquidInteractionComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;
import official.sketchBook.engine.components_related.system_utils.RenderableAndDefaultComponentManagerComponent;
import official.sketchBook.engine.components_related.vehicle.VehicleBaseComponent;
import official.sketchBook.game.util_related.constants.WorldConstants;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static official.sketchBook.engine.util_related.helper.body.SubmarinePartBodyCreateHelper.createExternalBody;
import static official.sketchBook.engine.util_related.helper.body.SubmarinePartBodyCreateHelper.createInternalBody;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.toMeters;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.toPixels;

public class SubmarineNode
    implements
    MovableObjectII,
    PhysicalLiquidInteractableObjectII,
    PhysicalObjectII,
    VehicleSection,
    OptmizedRenderableObjectII,
    Disposable {

    /// Referência ao veículo dono desse node
    private Submarine submarine;

    /// Lista de partes físicas
    private final List<SubmarinePart> physicalParts;

    /// Lista de objetos que podem entrar e interagir com o lado interno do submarino, alterando a massa e outras coisas
    private final List<SubmarinePassenger> passengerList;

    /**
     * Posição relativa (em metros, espaço local do internalBody, SEM
     * rotação) de cada passageiro atualmente dentro do submarino. Chave é
     * o próprio passageiro — garante identidade única, evitando adicionar
     * ou remover contribuições de massa incorretamente. O Vector2 é
     * reaproveitado a cada recálculo, não realocado.
     * <p>
     * A massa e o volume de cada passageiro NÃO são copiados para cá —
     * são sempre lidos direto de passenger.getLiquidInteractionC() no
     * momento do recálculo, para nunca ter duas fontes de verdade
     * divergentes.
     */
    private final Map<SubmarinePassenger, Vector2> passengerRelativePosition = new IdentityHashMap<>();

    /// Componente para controle de movimentação do sub a partir de velocidade
    private MovementComponent moveC;

    /// Componente de física, para controle da física atravéz de pipelines já existentes
    private PhysicsComponent physicsC;

    /// Componente de transform contendo os dados de dimensões do nó
    private TransformComponent transformC;

    /// Componente para lidar com a interação com liquidos do submarino
    private PhysicalLiquidInteractionComponent liquidInteractionC;

    /// Gerênciador de componentes lógicos de funcionamento de objeto
    private final RenderableAndDefaultComponentManagerComponent managerC;

    private InteractableObjectManagerComponent interactableObjectManagerC;

    /// Referência ao mundo físico
    private World physicsWorld;

    /// Body do submarino completo
    private Body
        internalBody,
        body;

    /// Dados bufferizados de velocidade para sincronização de objetos internos
    private float
        lastPosX = 0f,
        lastPosY = 0f,
        velX = 0f,
        velY = 0f;

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
        this.passengerList = new ArrayList<>();

        for (int i = 0; i < physicalParts.size(); i++) {
            physicalParts.get(i).setSection(this);
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

    private final List<VehicleBaseComponent> vehicleComponentList;

    public void initObject() {
        generateBody();
        initComponents();

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

        this.body.setBullet(true);
        this.internalBody.setBullet(true);

    }

    /**
     * Recalcula massa total, volume total e centro de massa combinado do
     * submarino, somando a contribuição do casco (SubmarinePart) com a
     * contribuição de cada passageiro atualmente dentro dele.
     * <p>
     * Massa/volume de cada passageiro são lidos diretamente do
     * LiquidInteractionComponent dele (nunca copiados/cacheados aqui) —
     * ele é sempre a fonte de verdade da própria massa. Isso evita
     * dessincronização: se o peso de um passageiro mudar por qualquer
     * motivo (ex: ele largou um item), o próximo recalculateMass() já
     * reflete isso automaticamente, sem precisar de nenhum evento extra
     * de sincronização entre os dois lados.
     */
    public void recalculateMass() {
        float totalMass = 0f;
        float totalVolume = 0f;
        float weightedCenterX = 0f;
        float weightedCenterY = 0f;

        boolean hasValidContribution = false;

        // ---- casco (SubmarinePart) ----
        for (int i = 0; i < physicalParts.size(); i++) {
            SubmarinePart part = physicalParts.get(i);
            if (!part.isBoundsCalculated()) continue;

            float centerX = part.getCenterX();
            float centerY = part.getCenterY();

            float volume = part.getVolume();
            float mass = part.getTotalMass();

            totalVolume += volume;
            totalMass += mass;
            weightedCenterX += centerX * mass;
            weightedCenterY += centerY * mass;

            hasValidContribution = true;
        }

        // ---- passageiros ----
        for (int i = 0; i < passengerList.size(); i++) {
            SubmarinePassenger passenger = passengerList.get(i);

            float passengerMass = passenger.getLiquidInteractionC().getMass();
            if (passengerMass <= 0f) continue;

            Vector2 relativePos = updatePassengerRelativePosition(passenger);
            if (relativePos == null) continue;

            float passengerVolume = passenger.getLiquidInteractionC().getVolume();

            totalMass += passengerMass;
            totalVolume += passengerVolume;
            weightedCenterX += relativePos.x * passengerMass;
            weightedCenterY += relativePos.y * passengerMass;

            hasValidContribution = true;
        }

        if (!hasValidContribution || totalMass <= 0f) return;

        float centerX = weightedCenterX / totalMass;
        float centerY = weightedCenterY / totalMass;

        liquidInteractionC.setMass(totalMass);
        liquidInteractionC.setVolume(totalVolume);
        liquidInteractionC.updateCenterOfMass(centerX, centerY);
    }

    /**
     * Converte a posição atual do body do passageiro (mundo, pixels via
     * Box2D em metros) para o espaço local do internalBody do submarino
     * (metros, sem rotação) — mesmo referencial usado por
     * SubmarinePart.getCenterX()/getCenterY(), então os dois podem ser
     * somados diretamente na média ponderada.
     * <p>
     * Atualiza (não realoca) o Vector2 já associado ao passageiro no map.
     * Retorna null se o passageiro não tiver mais um body válido (ex:
     * disposed entre o enter e este recálculo — defensivo).
     */
    private Vector2 updatePassengerRelativePosition(SubmarinePassenger passenger) {
        Body passengerBody = passenger.getBody();
        if (passengerBody == null) return null;

        Vector2 relativePos = passengerRelativePosition.computeIfAbsent(passenger, k -> new Vector2());
        // não deveria acontecer se onPassengerEnter sempre popula o
        // map antes de recalculateMass ser chamado, mas defensivo
        // contra ordens de chamada inesperadas

        Vector2 worldPos = passengerBody.getPosition();
        Vector2 nodePos = internalBody.getPosition();
        float nodeAngle = internalBody.getAngle();

        float relXMeters = worldPos.x - nodePos.x;
        float relYMeters = worldPos.y - nodePos.y;

        // desrotaciona pela rotação atual do submarino — mesmo tratamento
        // usado em SubmarinePart.findPartContaining, para cair no mesmo
        // espaço local (sem rotação) onde centerX/centerY das parts vivem
        float cos = MathUtils.cos(-nodeAngle);
        float sin = MathUtils.sin(-nodeAngle);

        float localX = relXMeters * cos - relYMeters * sin;
        float localY = relXMeters * sin + relYMeters * cos;

        relativePos.set(localX, localY);
        return relativePos;
    }

    //TO-DO:Adicionar sistema para lidar com objetos anexados de outros nodes... possévelmente
    public void calculateNodeDimensions() {
        if (physicalParts == null || physicalParts.isEmpty()) return;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        boolean hasValidPart = false;

        for (int i = 0; i < physicalParts.size(); i++) {
            SubmarinePart part = physicalParts.get(i);

            if (!part.isBoundsCalculated()) {
                part.updateBounds();
            }

            if (!part.isBoundsCalculated()) {
                continue;
            }

            hasValidPart = true;

            if (part.getInternalMinX() < minX) minX = part.getInternalMinX();
            if (part.getInternalMinY() < minY) minY = part.getInternalMinY();
            if (part.getInternalMaxX() > maxX) maxX = part.getInternalMaxX();
            if (part.getInternalMaxY() > maxY) maxY = part.getInternalMaxY();
        }

        if (!hasValidPart) return;

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
            true,
            true,
            true,
            true,
            true,
            true,
            false,
            true
        );

        this.physicsC = new MovableObjectPhysicsComponent(
            this,
            0,
            0,
            0,
            0,
            0,
            true,
            false
        );

        physicsC.halfWidth = transformC.getHalfWidth();
        physicsC.halfHeight = transformC.getHalfHeight();

        liquidInteractionC = new PhysicalLiquidInteractionComponent(this);

        interactableObjectManagerC = new InteractableObjectManagerComponent(internalBody);

        this.managerC.add(moveC, true, false);
        this.managerC.add(liquidInteractionC, true, false);
        this.managerC.add(physicsC, true, false);
        this.managerC.add(interactableObjectManagerC, false, true);
    }

    @Override
    public void onPassengerEnter(VehiclePassenger passenger) {
        if (!(passenger instanceof SubmarinePassenger)) return;

        SubmarinePassenger submarinePassenger = (SubmarinePassenger) passenger;

        // evita duplicar entrada se onPassengerEnter for chamado duas
        // vezes para o mesmo passageiro (ex: eventos de trigger repetidos)
        if (passengerRelativePosition.containsKey(submarinePassenger)) return;

        passengerList.add(submarinePassenger);
        passengerRelativePosition.put(submarinePassenger, new Vector2());

        recalculateMass();
    }

    @Override
    public void onPassengerExit(VehiclePassenger passenger) {
        if (!(passenger instanceof SubmarinePassenger)) return;

        SubmarinePassenger submarinePassenger = (SubmarinePassenger) passenger;

        boolean removed = passengerList.remove(submarinePassenger);

        if (removed) {
            recalculateMass();
        }
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

        recalculateMass();
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
        this.managerC.render(batch);
    }

    public void addVehicleComponent(
        VehicleBaseComponent component,
        boolean toRender,
        boolean toUpdate,
        boolean toPostUpdate
    ) {
        this.vehicleComponentList.add(component);

        if (toRender && component instanceof RenderableObjectII) this.managerC.addToRender(component);

        if (component instanceof InteractableObjectII) {
            this.interactableObjectManagerC.addToList((InteractableObjectII) component);
        }

        this.managerC.add(component, toUpdate, toPostUpdate);
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
    public PhysicalLiquidInteractionComponent getLiquidInteractionC() {
        return liquidInteractionC;
    }

    @Override
    public Vehicle getVehicle() {
        return submarine;
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
        if (vehicle == this.submarine ||
            !(vehicle instanceof Submarine) ||
            this.submarine != null
        )
            return;
        this.submarine = (Submarine) vehicle;
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
        passengerList.clear();
        passengerRelativePosition.clear();

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
