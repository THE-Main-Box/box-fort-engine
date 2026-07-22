package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.LiquidInteractableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.MovementDataComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.system_utils.SubmersibleVolume;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/// Simula interação física com líquidos — aplica flutuabilidade, resistência, limites de
/// velocidade e estabilidade rotacional (busca o ponto de equilíbrio/máxima submersão).
/// Deve ser atualizado após o componente de movimentação.
///
/// ORGANIZAÇÃO DESTA CLASSE (para facilitar manutenção futura):
/// 1. Estado geral (campos)
/// 2. Ciclo principal (update / updateSimulation / applyLiquidPhysics)
/// 3. Sistema de flutuabilidade (empuxo vertical) + overshoot de velocidade residual
/// 4. Submersão por volume + ponto de aplicação de empuxo (calculados juntos, 1 passe só)
/// 5. Sistema de torque (usa floatApplicationPoint vs massCenter)
/// 6. Estado de líquido/região (quais líquidos o objeto está tocando)
/// 7. Movimento (resistência/constraints aplicados no moveC)
/// 8. Getters/setters públicos
/// 9. Finalização (dispose)
public class PhysicalMobLiquidInteractionComponent implements Component {

    // ==================================================================
    // 1. ESTADO GERAL
    // ==================================================================

    // --- booleans simples ---
    private boolean
        canInteractBuffer = false,
        canInteract = true,
        inLiquid = false;

    private boolean atSurfaceEquilibrium = false;
    private boolean wasAtSurfaceEquilibrium = false;

    private boolean
        originalValuesStored = false,
        needsUpdateStoredMovement = true;

    private boolean
        needsUpdateCurrentLiquidData = true,
        needsUpdateCurrentRegion = true,
        needsUpdatePhysicsData = true,
        needsUpdateMovement = true,
        isConstraintsDirty = true;

    private boolean rotationDirty = true;

    // --- floats simples (estado calculado a cada frame) ---
    private float
        objectDensity,
        mass,
        volume;

    private float
        floatEffectValue,
        floatEffectValueModifier;

    private float
        cachedSubmersionFraction = 1f;

    private float equilibriumSubmersionThreshold = 0f;
    private float lastProcessedRotationDegrees = Float.NaN;
    private float cachedTorqueVelocity = 0f;

    /// "Energia da mola": velocidade vertical capturada no instante em que o objeto entra
    /// no equilíbrio, decaindo gradualmente (puxada pelo drag do líquido) e somada ao
    /// empuxo normal — permite overshoot ao sair da água sem alterar targetFloat em si.
    private float residualVerticalVelocity = 0f;

    // --- constantes de calibração ---
    private static final float MIN_EQUILIBRIUM_THRESHOLD = 0.16f;
    private static final float MAX_EQUILIBRIUM_THRESHOLD = 0.95f;
    private static final float EQUILIBRIUM_CALIBRATION_FACTOR = 1.5f;
    private static final float MIN_SUBMERSION_DELTA_TO_RECALC = 0.01f;
    private static final float MIN_ROTATION_DELTA_DEGREES_TO_RECALC = 0.5f;
    private static final float RESIDUAL_VELOCITY_CUTOFF = 0.01f;

    // --- buffers reaproveitados (evita alocação a cada frame/volume) ---
    private final float[] cornerXBuffer = new float[4];
    private final float[] cornerYBuffer = new float[4];

    // --- objetos ---
    private final Vector2
        floatApplicationPoint = new Vector2(),
        massCenter = new Vector2();

    private final MovementDataComponent intermediary;
    private final MovementDataComponent storedMovementData;

    private final SubmersionAccumulator submersionAccumulator = new SubmersionAccumulator();

    private LiquidData
        highestDensityLiquidBuffer,
        highestDragLiquidBuffer;

    private LiquidRegion currentLiquidRegionBuffer;

    private LiquidInteractableObjectII object;
    private MovementComponent moveC;

    private final IdentityHashMap<LiquidData, ArrayList<LiquidRegion>> liquidAndRegionMap = new IdentityHashMap<>();

    private boolean disposed = false;

    /// Acumulador reaproveitado a cada frame por updateSubmersionAndApplicationData —
    /// evita alocar um objeto novo por objeto físico a cada tick (importante para múltiplos
    /// objetos rodando a 60/s). Os campos são resetados no início de cada passe, não recriados.
    private static final class SubmersionAccumulator {
        float weightedFraction;
        float totalArea;
        float weightedX;
        float weightedY;
        float totalWeight;
    }

    public PhysicalMobLiquidInteractionComponent(LiquidInteractableObjectII object) {
        this.object = object;
        this.moveC = object.getMoveC();
        this.storedMovementData = new MovementDataComponent();
        this.intermediary = new MovementDataComponent();
        this.updateCurrentStoredMovementValues();
    }

    // ==================================================================
    // 2. CICLO PRINCIPAL
    // ==================================================================

    @Override
    public void update(float delta) {
        updateLiquidState();
        updateSimulation(delta);
        updateStoredMovement();

        if (!inLiquid) return;
        object.inLiquidUpdate();
    }

    /// Ponto de entrada de cada frame: recalcula dados físicos (se dirty), estado de
    /// submersão/torque, e por fim aplica tudo no moveC. A ordem importa: dados de física
    /// (densidade, threshold) precisam estar prontos antes de calcular submersão, que por
    /// sua vez precisa estar pronta antes de aplicar empuxo/torque.
    private void updateSimulation(float delta) {
        boolean shouldSimulate = canInteract && !liquidAndRegionMap.isEmpty();

        applyChange(shouldSimulate);

        if (!shouldSimulate) return;

        updatePhysicsData();
        updateMovementData();
        updateRotationDirtyState();
        updateSubmersionAndApplicationData();
        updateSurfaceEquilibriumState();
        updateResidualVerticalVelocity();

        applyLiquidPhysics(delta);
    }

    /// Aplica os efeitos finais no moveC: empuxo vertical e torque de correção rotacional.
    private void applyLiquidPhysics(float delta) {
        if (cachedSubmersionFraction <= 0f) {
            resetSimulatedFloatation();
            return;
        }

        calculateFloatEffect(highestDensityLiquidBuffer, delta);
        applyConstraints();
        applyFloat(delta);

        updateTorqueVelocity(delta);
        applyTorque();
    }

    @Override
    public void postUpdate() {
    }

    @Override
    public void initObject() {
    }

    // ==================================================================
    // 3. SISTEMA DE FLUTUABILIDADE (empuxo vertical) + OVERSHOOT
    // ==================================================================

    /// True quando o objeto está no ponto mínimo de submersão que a flutuação natural
    /// permite (perto do equilíbrio de densidade). Nesse estado, calculateFloatEffect troca
    /// de "convergência suave" para "resposta direta", evitando acúmulo de força residual
    /// que causaria oscilação perto da superfície.
    private void updateSurfaceEquilibriumState() {
        atSurfaceEquilibrium = cachedSubmersionFraction <= equilibriumSubmersionThreshold;
    }

    /// Captura a velocidade vertical no instante em que o objeto entra no equilíbrio pela
    /// primeira vez (transição false->true) — essa é a "energia da mola" do overshoot.
    /// Fora do equilíbrio, ou já consumida, o residual permanece zerado.
    private void updateResidualVerticalVelocity() {
        boolean justEnteredEquilibrium = atSurfaceEquilibrium && !wasAtSurfaceEquilibrium;

        if (justEnteredEquilibrium) {
            residualVerticalVelocity = moveC.dataComponent.yAxis.velocity;
        }

        if (!atSurfaceEquilibrium) {
            residualVerticalVelocity = 0f;
        }

        wasAtSurfaceEquilibrium = atSurfaceEquilibrium;
    }

    /// Aplica o decaimento da mola (puxado pelo drag do líquido, sem constante nova) e
    /// devolve o valor deste frame antes de decair — para ser somado ao empuxo normal.
    private float consumeResidualVerticalVelocity(float delta) {
        if (residualVerticalVelocity == 0f) return 0f;

        float drag = highestDragLiquidBuffer != null ? highestDragLiquidBuffer.drag : 0f;

        float valueThisFrame = residualVerticalVelocity;

        residualVerticalVelocity -= (residualVerticalVelocity *drag) * Math.min(delta, 1f);

        if (Math.abs(residualVerticalVelocity) < RESIDUAL_VELOCITY_CUTOFF) {
            residualVerticalVelocity = 0f;
        }

        return valueThisFrame - drag * 2;
    }

    /// Recalcula o threshold teórico de submersão em equilíbrio, a partir da densidade
    /// relativa do objeto frente ao líquido mais denso presente.
    private void recalculateEquilibriumThreshold() {
        if (highestDensityLiquidBuffer == null || highestDensityLiquidBuffer.density <= 0f) {
            equilibriumSubmersionThreshold = MIN_EQUILIBRIUM_THRESHOLD;
            return;
        }

        float theoreticalFraction = objectDensity / highestDensityLiquidBuffer.density;

        equilibriumSubmersionThreshold = MathUtils.clamp(
            theoreticalFraction * EQUILIBRIUM_CALIBRATION_FACTOR,
            MIN_EQUILIBRIUM_THRESHOLD,
            MAX_EQUILIBRIUM_THRESHOLD
        );
    }

    private void recalculatePhysicsData() {
        objectDensity = (volume > 0f) ? (mass / volume) : Float.MAX_VALUE;
    }

    private void updatePhysicsData() {
        if (!needsUpdatePhysicsData) return;

        recalculatePhysicsData();
        recalculateEquilibriumThreshold();

        needsUpdateMovement = true;
        needsUpdatePhysicsData = false;
    }

    private void updateMovementData() {
        if (!needsUpdateMovement) return;

        recalculateMovementData();
        isConstraintsDirty = true;
        needsUpdateMovement = false;
    }

    /// Entrega o empuxo vertical calculado para o moveC — a pipeline de movimento cuida do
    /// resto (aceleração, clamps, etc). targetFloat nunca é alterado pelo residual: o
    /// overshoot é somado organicamente num único setMovement, não injetado por fora.
    private void applyFloat(float delta) {
        if (cachedSubmersionFraction <= 0f) return;

        float targetFloat = (floatEffectValue + floatEffectValueModifier) * cachedSubmersionFraction;

        float residual = atSurfaceEquilibrium ? consumeResidualVerticalVelocity(delta) : 0f;

        moveC.dataComponent.yAxis.setMovement(targetFloat + residual);
    }

    /// Calcula floatEffectValue: fora do equilíbrio, converge suavemente ao valor-alvo
    /// (evita saltos); no equilíbrio, é diretamente proporcional à fração de submersão
    /// (resposta imediata, sem inércia acumulada — isso evita o objeto "quicar" perto da
    /// superfície).
    private void calculateFloatEffect(LiquidData data, float delta) {
        if (data == null || volume <= 0f) return;

        float targetFloatEffect = (data.density - objectDensity) * volume;

        if (atSurfaceEquilibrium) {
            floatEffectValue = targetFloatEffect * cachedSubmersionFraction;
            return;
        }

        floatEffectValue += (targetFloatEffect - floatEffectValue) * Math.min(delta, 1f);

        if (targetFloatEffect >= 0f) {
            if (floatEffectValue > targetFloatEffect) floatEffectValue = targetFloatEffect;
            else if (floatEffectValue < -targetFloatEffect) floatEffectValue = -targetFloatEffect;
        } else {
            if (floatEffectValue < targetFloatEffect) floatEffectValue = targetFloatEffect;
            else if (floatEffectValue > -targetFloatEffect) floatEffectValue = -targetFloatEffect;
        }
    }

    // ==================================================================
    // 4. SUBMERSÃO POR VOLUME + PONTO DE APLICAÇÃO DE EMPUXO
    //
    // Os dois dados abaixo (cachedSubmersionFraction e floatApplicationPoint) dependem do
    // mesmo cálculo por volume (fração de submersão via corners rotacionados), então são
    // calculados juntos num único passe pela lista de volumes — evita repetir a mesma
    // trigonometria duas vezes por frame quando os dois precisam ser atualizados.
    //
    // floatApplicationPoint é puramente geométrico (sem massa/densidade) e só é consumido
    // pelo sistema de torque (seção 5) — não influencia o empuxo vertical.
    // ==================================================================

    /// Verifica se a rotação mudou o suficiente desde o último processamento para justificar
    /// recalcular o ponto de aplicação de empuxo. A fração de submersão (para o empuxo
    /// vertical) NÃO usa essa flag — ela precisa refletir profundidade a cada frame, mesmo
    /// parado rotacionalmente. Só floatApplicationPoint (usado exclusivamente pelo torque)
    /// se beneficia desse guard.
    private void updateRotationDirtyState() {
        float currentRotation = object.getTransformC().getRotation();

        if (Float.isNaN(lastProcessedRotationDegrees)) {
            rotationDirty = true;
            lastProcessedRotationDegrees = currentRotation;
            return;
        }

        float delta = Math.abs(currentRotation - lastProcessedRotationDegrees);
        rotationDirty = delta > MIN_ROTATION_DELTA_DEGREES_TO_RECALC;

        if (rotationDirty) {
            lastProcessedRotationDegrees = currentRotation;
        }
    }

    /// Passe único sobre a lista de SubmersibleVolume: calcula a fração de submersão de cada
    /// volume UMA VEZ, e reaproveita o resultado para dois consumidores — a fração agregada
    /// do corpo inteiro (sempre) e o ponto de aplicação de empuxo (só quando a rotação mudou
    /// o bastante e o objeto pode rotacionar). cos/sin são calculados uma única vez aqui fora
    /// do loop, em vez de recalculados por volume.
    private void updateSubmersionAndApplicationData() {
        if (currentLiquidRegionBuffer == null) {
            cachedSubmersionFraction = 0f;
            return;
        }

        List<SubmersibleVolume> volumes = object.getSubmersibleVolumeList();

        if (volumes == null || volumes.isEmpty()) {
            cachedSubmersionFraction = 1f;
            return;
        }

        boolean shouldUpdateApplicationPoint = moveC.dataComponent.rAxis.canMove && rotationDirty;

        float rotationDegrees = object.getTransformC().getRotation();
        float rad = rotationDegrees * MathUtils.degreesToRadians;
        float cos = MathUtils.cos(rad);
        float sin = MathUtils.sin(rad);

        resetSubmersionAccumulator();

        for (int i = 0; i < volumes.size(); i++) {
            accumulateVolumeSubmersion(volumes.get(i), cos, sin, shouldUpdateApplicationPoint);
        }

        finalizeSubmersionFraction();

        if (shouldUpdateApplicationPoint) {
            finalizeFloatApplicationPoint();
        }
    }

    private void resetSubmersionAccumulator() {
        submersionAccumulator.weightedFraction = 0f;
        submersionAccumulator.totalArea = 0f;
        submersionAccumulator.weightedX = 0f;
        submersionAccumulator.weightedY = 0f;
        submersionAccumulator.totalWeight = 0f;
    }

    /// Processa um único volume: calcula sua fração de submersão (reaproveitando cos/sin já
    /// prontos) e acumula os dados necessários para os dois consumidores finais.
    private void accumulateVolumeSubmersion(
        SubmersibleVolume volume,
        float cos,
        float sin,
        boolean shouldUpdateApplicationPoint
    ) {
        float fraction = calculateVolumeSubmersionFraction(volume, cos, sin);
        float area = volume.getArea();
        float weightedArea = fraction * area;

        volume.updateSubmersionFraction(fraction, MIN_SUBMERSION_DELTA_TO_RECALC);

        submersionAccumulator.weightedFraction += weightedArea;
        submersionAccumulator.totalArea += area;

        if (!shouldUpdateApplicationPoint) return;

        submersionAccumulator.weightedX += volume.getCenterXToBody() * weightedArea;
        submersionAccumulator.weightedY += volume.getCenterYToBody() * weightedArea;
        submersionAccumulator.totalWeight += weightedArea;
    }

    private void finalizeSubmersionFraction() {
        cachedSubmersionFraction = submersionAccumulator.totalArea > 0f
            ? MathUtils.clamp(submersionAccumulator.weightedFraction / submersionAccumulator.totalArea, 0f, 1f)
            : 0f;
    }

    /// Ponto de aplicação = média ponderada (fração × área) da posição local de cada volume.
    /// Suave por natureza: nenhum volume "vence" discretamente, a contribuição de cada um
    /// cresce/diminui gradualmente conforme afunda mais ou menos.
    private void finalizeFloatApplicationPoint() {
        if (submersionAccumulator.totalWeight <= 0f) return;

        floatApplicationPoint.set(
            submersionAccumulator.weightedX / submersionAccumulator.totalWeight,
            submersionAccumulator.weightedY / submersionAccumulator.totalWeight
        );
    }

    /// Fração de submersão de um único volume: acha os 4 corners rotacionados e compara
    /// contra a superfície do líquido. Recebe cos/sin prontos (calculados uma vez por frame
    /// em updateSubmersionAndApplicationData) em vez de recalcular por volume.
    private float calculateVolumeSubmersionFraction(SubmersibleVolume volume, float cos, float sin) {
        float surfaceY = currentLiquidRegionBuffer.getY() + currentLiquidRegionBuffer.getHeight();

        computeRotatedVolumeCorners(volume, cos, sin, cornerXBuffer, cornerYBuffer);

        float minY = cornerYBuffer[0];
        float maxY = cornerYBuffer[0];
        for (int i = 1; i < 4; i++) {
            float y = cornerYBuffer[i];
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        if (maxY <= surfaceY) return 1f;
        if (minY >= surfaceY) return 0f;

        float effectiveHeight = maxY - minY;
        if (effectiveHeight <= 0f) return 0f;

        return MathUtils.clamp((surfaceY - minY) / effectiveHeight, 0f, 1f);
    }

    /// Rotaciona os 4 corners do volume em torno do centro geométrico do corpo (o mesmo
    /// pivô que o TransformComponent usa para rotação visual). Puramente geométrico:
    /// massCenter e floatApplicationPoint não participam aqui — só entram no torque (seção 5).
    private void computeRotatedVolumeCorners(SubmersibleVolume volume, float cos, float sin, float[] outX, float[] outY) {
        TransformComponent t = object.getTransformC();

        float halfW = volume.getWidth() * 0.5f;
        float halfH = volume.getHeight() * 0.5f;

        float bodyGeomCenterX = t.x + t.width * 0.5f;
        float bodyGeomCenterY = t.y + t.height * 0.5f;

        float volumeCenterX = bodyGeomCenterX + volume.getCenterXToBody();
        float volumeCenterY = bodyGeomCenterY + volume.getCenterYToBody();

        float[] localX = {-halfW, halfW, halfW, -halfW};
        float[] localY = {-halfH, -halfH, halfH, halfH};

        for (int i = 0; i < 4; i++) {
            float absX = volumeCenterX + localX[i];
            float absY = volumeCenterY + localY[i];

            float relX = absX - bodyGeomCenterX;
            float relY = absY - bodyGeomCenterY;

            float rotatedX = relX * cos - relY * sin;
            float rotatedY = relX * sin + relY * cos;

            outX[i] = bodyGeomCenterX + rotatedX;
            outY[i] = bodyGeomCenterY + rotatedY;
        }
    }

    // ==================================================================
    // 5. SISTEMA DE TORQUE (usa floatApplicationPoint vs massCenter)
    // ==================================================================

    /// Deriva uma velocidade angular suavizada a partir do braço de alavanca entre
    /// floatApplicationPoint (centro de empuxo) e massCenter (centro de massa), reprojetado
    /// pela rotação atual do corpo. Segue o mesmo padrão de calculateFloatEffect: converge
    /// gradualmente fora do equilíbrio, e é amortecida pela fração de submersão dentro dele.
    private void updateTorqueVelocity(float delta) {
        if (!moveC.dataComponent.rAxis.canMove) {
            cachedTorqueVelocity = 0f;
            return;
        }

        float targetTorque = calculateTorque();

        if (atSurfaceEquilibrium) {
            cachedTorqueVelocity = targetTorque * cachedSubmersionFraction;
            return;
        }

        cachedTorqueVelocity += (targetTorque - cachedTorqueVelocity) * Math.min(delta, 1f);
    }

    /// Braço de alavanca em espaço local (floatApplicationPoint - massCenter), reprojetado
    /// pela rotação atual do corpo. Como a força de empuxo é puramente vertical, apenas o
    /// componente X do braço rotacionado contribui para o torque (τ = armX * força).
    private float calculateTorque() {
        float localArmX = floatApplicationPoint.x - massCenter.x;
        float localArmY = floatApplicationPoint.y - massCenter.y;

        float rotationDegrees = object.getTransformC().getRotation();
        float rad = rotationDegrees * MathUtils.degreesToRadians;
        float cos = MathUtils.cos(rad);
        float sin = MathUtils.sin(rad);

        float rotatedArmX = localArmX * cos - localArmY * sin;

        return rotatedArmX * floatEffectValue;
    }

    /// Entrega a velocidade angular calculada para o moveC, do mesmo jeito que applyFloat
    /// entrega o empuxo para o yAxis — deixando a pipeline existente processar o valor, sem
    /// escrever diretamente em transformC.
    private void applyTorque() {
        if (!moveC.dataComponent.rAxis.canMove) return;

        moveC.dataComponent.rAxis.setMovement(cachedTorqueVelocity);
    }

    // ==================================================================
    // 6. ESTADO DE LÍQUIDO/REGIÃO
    // ==================================================================

    private void applyChange(boolean shouldSimulate) {
        if (shouldSimulate && !inLiquid) {
            inLiquid = true;
            object.onLiquidEnter();
            return;
        }

        if (!shouldSimulate && inLiquid) {
            inLiquid = false;
            restartStoredMovementValues();
            resetFlotation();
            residualVerticalVelocity = 0;
            moveC.dataComponent.yAxis.resetMovement();
            moveC.dataComponent.rAxis.resetMovement();
            object.onLiquidExit();
        }
    }

    private void updateLiquidState() {
        if (needsUpdateCurrentRegion) {
            updateCurrentRegion();
            needsUpdateCurrentRegion = false;
        }

        if (needsUpdateCurrentLiquidData) {
            updateCurrentLiquidData();
            needsUpdateMovement = true;
            needsUpdateCurrentLiquidData = false;
        }
    }

    /// Escolhe a região de líquido mais próxima do centro do objeto, entre todas as regiões
    /// registradas. Atalho quando só existe uma região no total (caso comum).
    private void updateCurrentRegion() {
        if (liquidAndRegionMap.isEmpty()) {
            currentLiquidRegionBuffer = null;
            return;
        }

        if (liquidAndRegionMap.size() == 1) {
            ArrayList<LiquidRegion> onlyList = liquidAndRegionMap.values().iterator().next();
            if (onlyList.size() == 1) {
                currentLiquidRegionBuffer = onlyList.get(0);
                return;
            }
        }

        TransformComponent t = object.getTransformC();
        float objectCenterX = t.getCenterX();
        float objectCenterY = t.getCenterY();

        LiquidRegion closestRegion = null;
        float closestDistance = Float.MAX_VALUE;

        for (Map.Entry<LiquidData, ArrayList<LiquidRegion>> entry : liquidAndRegionMap.entrySet()) {
            ArrayList<LiquidRegion> list = entry.getValue();

            for (int i = 0; i < list.size(); i++) {
                LiquidRegion region = list.get(i);
                if (region == null) continue;

                float regionCenterX = region.getX() + (region.getWidth() * 0.5f);
                float regionCenterY = region.getY() + (region.getHeight() * 0.5f);

                float dx = objectCenterX - regionCenterX;
                float dy = objectCenterY - regionCenterY;

                float distance = (dx * dx) + (dy * dy);

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestRegion = region;
                }
            }
        }

        currentLiquidRegionBuffer = closestRegion;
    }

    /// Determina qual líquido tem a maior densidade (usado para empuxo) e qual tem o maior
    /// drag (usado para resistência), entre todos os líquidos que o objeto está tocando.
    private void updateCurrentLiquidData() {
        if (liquidAndRegionMap.isEmpty()) {
            highestDensityLiquidBuffer = null;
            highestDragLiquidBuffer = null;
            recalculateEquilibriumThreshold();
            return;
        }

        if (liquidAndRegionMap.size() == 1) {
            LiquidData onlyData = liquidAndRegionMap.keySet().iterator().next();
            highestDensityLiquidBuffer = onlyData;
            highestDragLiquidBuffer = onlyData;
            recalculateEquilibriumThreshold();
            return;
        }

        LiquidData highestDensity = null;
        LiquidData highestDrag = null;

        for (LiquidData data : liquidAndRegionMap.keySet()) {
            if (highestDensity == null) {
                highestDensity = data;
                highestDrag = data;
                continue;
            }

            if (data.density > highestDensity.density) highestDensity = data;
            if (data.drag > highestDrag.drag) highestDrag = data;
        }

        highestDensityLiquidBuffer = highestDensity;
        highestDragLiquidBuffer = highestDrag;

        recalculateEquilibriumThreshold();
    }

    public void addLiquid(LiquidData liquidData, LiquidRegion region) {
        if (region == null || liquidData == null) return;

        ArrayList<LiquidRegion> list = liquidAndRegionMap.get(liquidData);

        if (list == null) {
            list = new ArrayList<>(4);
            liquidAndRegionMap.put(liquidData, list);
            needsUpdateCurrentLiquidData = true;
        }

        list.add(region);
        needsUpdateCurrentRegion = true;
    }

    public void removeLiquid(LiquidData liquidData, LiquidRegion region) {
        if (liquidData == null || region == null) return;

        ArrayList<LiquidRegion> list = liquidAndRegionMap.get(liquidData);

        if (list == null) {
            needsUpdateCurrentLiquidData = true;
            return;
        }

        list.remove(region);
        needsUpdateCurrentRegion = true;

        if (list.isEmpty()) {
            liquidAndRegionMap.remove(liquidData);
            needsUpdateCurrentLiquidData = true;
        }
    }

    // ==================================================================
    // 7. MOVIMENTO (resistência/constraints aplicados no moveC)
    // ==================================================================

    private void updateStoredMovement() {
        if (!needsUpdateStoredMovement) return;
        storeCurrentMovementValues();
        canInteract = canInteractBuffer;
        needsUpdateStoredMovement = false;
    }

    private void storeCurrentMovementValues() {
        if (originalValuesStored) return;
        this.storedMovementData.set(moveC.dataComponent);
        originalValuesStored = true;
    }

    private void restartStoredMovementValues() {
        if (!originalValuesStored) return;
        this.moveC.dataComponent.set(storedMovementData);
        isConstraintsDirty = true;
    }

    private void prepareIntermediary() {
        intermediary.set(storedMovementData);
    }

    private void recalculateMovementData() {
        prepareIntermediary();
        calculateResistance(highestDragLiquidBuffer);
    }

    private void calculateResistance(LiquidData data) {
        if (data == null) return;

        float drag = data.drag;

        intermediary.xAxis.weightFactor = drag;
        intermediary.yAxis.weightFactor = drag;
        intermediary.rAxis.weightFactor = drag;

        intermediary.xAxis.deceleration = storedMovementData.xAxis.deceleration + drag;
        intermediary.yAxis.deceleration = storedMovementData.yAxis.deceleration + drag;
        intermediary.rAxis.deceleration = storedMovementData.rAxis.deceleration + drag;
    }

    private void applyConstraints() {
        if (!isConstraintsDirty) return;

        MovementDataComponent target = moveC.dataComponent;

        target.xAxis.weightFactor = intermediary.xAxis.weightFactor;
        target.yAxis.weightFactor = intermediary.yAxis.weightFactor;
        target.rAxis.weightFactor = intermediary.rAxis.weightFactor;

        target.xAxis.deceleration = intermediary.xAxis.deceleration;
        target.yAxis.deceleration = intermediary.yAxis.deceleration;
        target.rAxis.deceleration = intermediary.rAxis.deceleration;

        target.xAxis.maxMoveVel = intermediary.xAxis.maxMoveVel;
        target.yAxis.maxMoveVel = intermediary.yAxis.maxMoveVel;
        target.rAxis.maxMoveVel = intermediary.rAxis.maxMoveVel;

        target.gravityAffected = intermediary.gravityAffected;
        target.gravityScale = intermediary.gravityScale;

        isConstraintsDirty = false;
    }

    private void markUpdateSimulationData() {
        needsUpdatePhysicsData = true;
        needsUpdateMovement = true;
        isConstraintsDirty = true;
    }

    private void resetSimulatedFloatation() {
        floatEffectValue = 0;
    }

    public void updateCurrentStoredMovementValues() {
        this.needsUpdateStoredMovement = true;
        this.canInteractBuffer = this.canInteract;
        this.canInteract = false;
        this.originalValuesStored = false;
    }

    public void resetFlotation() {
        floatEffectValue = 0;
        floatEffectValueModifier = 0;
    }

    // ==================================================================
    // 8. GETTERS / SETTERS PÚBLICOS
    // ==================================================================

    public boolean isCanInteract() {
        return canInteract;
    }

    public void setCanInteract(boolean canInteract) {
        if (this.canInteract == canInteract) return;
        this.canInteract = canInteract;
    }

    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        if (mass < 0) return;
        this.mass = mass;
        markUpdateSimulationData();
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        markUpdateSimulationData();
    }

    public float getFloatEffect() {
        return floatEffectValue;
    }

    public float getFloatEffectValueModifier() {
        return floatEffectValueModifier;
    }

    public void setFloatingEffectModifier(float v) {
        this.floatEffectValueModifier = v;
    }

    public boolean isInLiquid() {
        return inLiquid;
    }

    public boolean isAtSurfaceEquilibrium() {
        return atSurfaceEquilibrium;
    }

    public boolean hasFloatation() {
        return floatEffectValueModifier != 0 || floatEffectValue != 0;
    }

    public Vector2 getMassCenter() {
        return massCenter;
    }

    public void updateMassCenter(float x, float y) {
        this.massCenter.set(x, y);
    }

    public Vector2 getFloatApplicationPoint() {
        return floatApplicationPoint;
    }

    public float getTorque() {
        return cachedTorqueVelocity;
    }

    public float getResidualVerticalVelocity() {
        return residualVerticalVelocity;
    }

    // ==================================================================
    // 9. FINALIZAÇÃO
    // ==================================================================

    @Override
    public void dispose() {
        if (disposed) return;
        liquidAndRegionMap.clear();
        nullifyReferences();
        disposed = true;
    }

    public void nullifyReferences() {
        moveC = null;
        object = null;
    }
}
