package official.sketchBook.engine.game_object_related.vehicle_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.InteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.PhysicalLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.PhysicalObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.vehicle.SubmarinePassenger;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.vehicle.VehiclePassenger;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.ObjectMassContributor;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.OptmizedRenderableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderableObjectII;
import official.sketchBook.engine.components_related.interact.InteractableObjectManagerComponent;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.physics.MovableObjectPhysicsComponent;
import official.sketchBook.engine.components_related.physics.PhysicalLiquidInteractionComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;
import official.sketchBook.engine.components_related.system_utils.RenderableAndDefaultComponentManagerComponent;
import official.sketchBook.engine.components_related.system_utils.UpdateRateLimiter;
import official.sketchBook.engine.components_related.vehicle.VehicleBaseComponent;
import official.sketchBook.game.util_related.constants.GameConfigConstants;
import official.sketchBook.game.util_related.constants.WorldConstants;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.engine.util_related.helper.body.SubmarinePartBodyCreateHelper.createExternalBody;
import static official.sketchBook.engine.util_related.helper.body.SubmarinePartBodyCreateHelper.createInternalBody;
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

    /**
     * Lista de objetos que entraram/saíram do submarino via contato físico
     * (VehicleContactListener). Trata identidade e ciclo de vida de
     * "estar dentro do veículo". Todo SubmarinePassenger que entra também
     * é registrado como contribuinte de massa em onPassengerEnter — a
     * lista de contribuintes continua existindo separadamente para
     * permitir contribuintes que NÃO são passageiros (ex: algo preso ao
     * casco sem trigger de contato).
     */
    private final List<SubmarinePassenger>
        pendingRemove = new ArrayList<>(),
        pendingAdd = new ArrayList<>(),
        passengerList = new ArrayList<>();

    /**
     * Lista de contribuintes de massa — sistema separado de passengerList,
     * conectado por política padrão em onPassengerEnter/onPassengerExit.
     * Permite registrar contribuintes que não são passageiros via
     * addMassContributor/removeMassContributor diretamente.
     */
    private final List<ObjectMassContributor>
        contributorPendingRemove = new ArrayList<>(),
        contributorPendingAdd = new ArrayList<>(),
        contributorList = new ArrayList<>();

    private final List<VehicleBaseComponent> vehicleComponentList = new ArrayList<>();

    /// Componente para controle de movimentação do sub a partir de velocidade
    private MovementComponent moveC;

    /// Componente de física, para controle da física através de pipelines já existentes
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

    // ============================================================
    // CACHE DA CONTRIBUIÇÃO DO CASCO
    // ============================================================

    /**
     * Marca que a contribuição do casco (SubmarinePart) precisa ser
     * recalculada. Só deve ser marcado true quando algo no casco
     * realmente muda (ex: dano estrutural alterando massa/volume de uma
     * parte) — NÃO por entrada/saída de passageiros ou contribuintes.
     * <p>
     * Começa true para forçar o primeiro cálculo em initObject().
     */
    private boolean hullContributionDirty = true;

    private float cachedHullMass = 0f;
    private float cachedHullVolume = 0f;
    private float cachedHullWeightedX = 0f;
    private float cachedHullWeightedY = 0f;
    private boolean cachedHullValid = false;

    /**
     * Rate limiter para o recálculo de massa/centro de massa. Evita
     * reprocessar contribuintes e reaplicar MassData no Box2D todo
     * frame sem necessidade — roda na taxa definida por
     * GameConfigConstants.PASSENGER_POSITION_MASS_CALC_RATE (padrão
     * FPS_TARGET / 2).
     */
    private final UpdateRateLimiter massRecalcLimiter =
        new UpdateRateLimiter(GameConfigConstants.PASSENGER_POSITION_MASS_CALC_RATE, true);

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

    // ============================================================
    // MASSA / CENTRO DE MASSA
    // ============================================================

    /**
     * Marca a contribuição do casco como suja, forçando recálculo na
     * próxima chamada de recalculateMass(). Chame isso apenas quando
     * SubmarinePart tiver sua massa, volume ou centro alterados (ex:
     * dano estrutural) — nunca por causa de passageiros/contribuintes.
     */
    public void markHullContributionDirty() {
        hullContributionDirty = true;
    }

    /**
     * Recalcula a contribuição do casco (SubmarinePart) e armazena em
     * cache. Só executa de fato se hullContributionDirty estiver true —
     * caso contrário é um no-op O(1).
     */
    private void recalculateHullContribution() {
        if (!hullContributionDirty) return;

        float totalMass = 0f;
        float totalVolume = 0f;
        float weightedX = 0f;
        float weightedY = 0f;
        boolean valid = false;

        for (int i = 0; i < physicalParts.size(); i++) {
            SubmarinePart part = physicalParts.get(i);
            if (!part.isBoundsCalculated()) continue;

            float centerX = part.getCenterX();
            float centerY = part.getCenterY();
            float volume = part.getVolume();
            float mass = part.getTotalMass();

            totalVolume += volume;
            totalMass += mass;
            weightedX += centerX * mass;
            weightedY += centerY * mass;

            valid = true;
        }

        cachedHullMass = totalMass;
        cachedHullVolume = totalVolume;
        cachedHullWeightedX = weightedX;
        cachedHullWeightedY = weightedY;
        cachedHullValid = valid;

        hullContributionDirty = false;
    }

    /**
     * Recalcula massa total, volume total e centro de massa combinado do
     * submarino, somando a contribuição cacheada do casco com a soma live
     * dos contribuintes de massa (contributorList).
     * <p>
     * totalVolume reflete APENAS o casco — contribuintes não deslocam
     * volume adicional de água, então não participam do cálculo de
     * empuxo, só do cálculo de massa/centro de massa.
     * <p>
     * cos/sin do ângulo do node são calculados UMA vez por chamada, fora
     * do loop de contribuintes, já que todos compartilham o mesmo
     * referencial (internalBody) naquele instante — evita recalcular
     * trigonometria por contribuinte (custo real com 20-40 contribuintes
     * simultâneos).
     */
    public void recalculateMass() {
        recalculateHullContribution();

        float totalMass = cachedHullMass;
        float totalVolume = cachedHullVolume;
        float weightedCenterX = cachedHullWeightedX;
        float weightedCenterY = cachedHullWeightedY;

        boolean hasValidContribution = cachedHullValid;

        int contributorCount = contributorList.size();

        if (contributorCount > 0) {
            Vector2 nodePos = internalBody.getPosition();
            float nodeAngle = internalBody.getAngle();
            float cos = MathUtils.cos(-nodeAngle);
            float sin = MathUtils.sin(-nodeAngle);
            float nodeX = nodePos.x;
            float nodeY = nodePos.y;

            for (int i = 0; i < contributorCount; i++) {
                ObjectMassContributor contributor = contributorList.get(i);

                if (!contributor.isContributing()) continue;

                float contribMass = contributor.getContributionMass();
                if (contribMass <= 0f) continue;

                Vector2 worldPoint = contributor.getContributionPoint();
                if (worldPoint == null) continue;

                float relX = worldPoint.x - nodeX;
                float relY = worldPoint.y - nodeY;

                float localX = relX * cos - relY * sin;
                float localY = relX * sin + relY * cos;

                totalMass += contribMass;
                weightedCenterX += localX * contribMass;
                weightedCenterY += localY * contribMass;

                hasValidContribution = true;
            }
        }

        if (!hasValidContribution || totalMass <= 0f) return;

        float centerX = weightedCenterX / totalMass;
        float centerY = weightedCenterY / totalMass;

        liquidInteractionC.setMass(totalMass);
        liquidInteractionC.setVolume(totalVolume);
        liquidInteractionC.updateCenterOfMass(centerX, centerY);
    }

    //TO-DO:Adicionar sistema para lidar com objetos anexados de outros nodes... possívelmente
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

    // ============================================================
    // PASSAGEIROS (identidade / ciclo de vida do veículo)
    // ============================================================

    @Override
    public void onPassengerEnter(VehiclePassenger passenger) {
        if (!(passenger instanceof SubmarinePassenger)) return;
        SubmarinePassenger sp = (SubmarinePassenger) passenger;

        pendingRemove.remove(sp); // cancela remoção pendente, se houver (entrou nesse frame de novo)

        if (!passengerList.contains(sp) && !pendingAdd.contains(sp)) {
            pendingAdd.add(sp);
        }

        // sp já É um ObjectMassContributor por herança de SubmarinePassenger —
        // sem necessidade de cast a partir do parâmetro genérico "passenger"
        addMassContributor(sp);
    }

    @Override
    public void onPassengerExit(VehiclePassenger passenger) {
        if (!(passenger instanceof SubmarinePassenger)) return;
        SubmarinePassenger sp = (SubmarinePassenger) passenger;

        pendingAdd.remove(sp); // cancela entrada pendente, se houver (saiu antes de ser processado)

        if (passengerList.contains(sp) && !pendingRemove.contains(sp)) {
            pendingRemove.add(sp);
        }

        removeMassContributor(sp);
    }

    // ============================================================
    // CONTRIBUINTES DE MASSA
    // ============================================================

    /**
     * Registra um contribuinte de massa. Idempotente e seguro de chamar
     * a qualquer momento, inclusive durante callbacks de física — a
     * mutação real da lista só ocorre em applyPendingContributorChanges(),
     * fora do step físico.
     */
    public void addMassContributor(ObjectMassContributor contributor) {
        if (contributor == null) return;

        contributorPendingRemove.remove(contributor);

        if (contributorList.contains(contributor) || contributorPendingAdd.contains(contributor)) return;

        contributorPendingAdd.add(contributor);
    }

    /**
     * Remove um contribuinte de massa. Mesma garantia de segurança que
     * addMassContributor — enfileira a remoção, não muta a lista na hora.
     */
    public void removeMassContributor(ObjectMassContributor contributor) {
        if (contributor == null) return;

        contributorPendingAdd.remove(contributor);

        if (!contributorList.contains(contributor) || contributorPendingRemove.contains(contributor)) return;

        contributorPendingRemove.add(contributor);
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

    /**
     * As filas de passageiro/contribuinte são sempre aplicadas todo
     * frame (O(1) quando vazias). O que é limitado por taxa é
     * recalculateMass() em si, via massRecalcLimiter — esse é o passo
     * caro (soma de contribuintes + reaplicação de MassData no Box2D).
     */
    public void update(float delta) {
        applyPendingPassengerChanges();
        applyPendingContributorChanges();

        managerC.update(delta);

        if (massRecalcLimiter.shouldUpdate(delta)) {
            recalculateMass();
        }
    }

    private void applyPendingPassengerChanges() {
        applyPending(pendingAdd, pendingRemove, passengerList);
    }

    private void applyPendingContributorChanges() {
        applyPending(contributorPendingAdd, contributorPendingRemove, contributorList);
    }

    /**
     * Aplica filas de add/remove pendentes numa lista alvo, de forma
     * genérica e reutilizável. Só toca a lista alvo se houver algo
     * pendente. Remoção via swap-com-último-elemento (O(1) após a busca)
     * é segura aqui porque a ordem de passengerList/contributorList
     * nunca importa para o cálculo de massa.
     * <p>
     * Chamado exclusivamente fora de callbacks de física (no início de
     * update()) — garante que a lista alvo nunca é mutada em um instante
     * em que o Box2D possa estar iterando/validando estado.
     */
    private static <T> void applyPending(List<T> toAdd, List<T> toRemove, List<T> target) {
        if (toRemove.isEmpty() && toAdd.isEmpty()) return;

        for (int i = 0; i < toRemove.size(); i++) {
            removeUnordered(target, toRemove.get(i));
        }
        toRemove.clear();

        for (int i = 0; i < toAdd.size(); i++) {
            T item = toAdd.get(i);
            if (!target.contains(item)) {
                target.add(item);
            }
        }
        toAdd.clear();
    }

    /**
     * Remove trocando pelo último elemento e removendo o último índice —
     * O(n) de busca + O(1) de remoção, em vez de O(n) busca + O(n) shift
     * do ArrayList.remove(Object) padrão. Seguro porque a ordem da lista
     * nunca é significativa para passageiros/contribuintes.
     */
    private static <T> void removeUnordered(List<T> list, T item) {
        int index = list.indexOf(item);
        if (index < 0) return;

        int lastIndex = list.size() - 1;
        if (index != lastIndex) {
            list.set(index, list.get(lastIndex));
        }
        list.remove(lastIndex);
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
        pendingAdd.clear();
        pendingRemove.clear();

        contributorList.clear();
        contributorPendingAdd.clear();
        contributorPendingRemove.clear();

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
