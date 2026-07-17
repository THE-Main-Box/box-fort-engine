package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.MathUtils;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.SimpleLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.MovementDataComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

/// Simula interação física com líquidos — aplica flutuabilidade, resistência e limites de velocidade.
/// Deve ser atualizado após o componente de movimentação.
public class PhysicalMobLiquidInteractionComponent implements Component {

    /// Referência ao objeto dono
    private SimpleLiquidInteractableObjectII object;

    /// Componente de movimentação do objeto dono
    private MovementComponent moveC;

    /// Mapa para identificação de liquidos -> regiões de contato (fixtures)
    private final IdentityHashMap<LiquidData, ArrayList<LiquidRegion>> liquidAndRegionMap = new IdentityHashMap<>();

    private LiquidData
        highestDensityLiquidBuffer,
        highestDragLiquidBuffer;

    private LiquidRegion currentLiquidRegionBuffer;

    // --- Flags de estado ---

    private boolean
        canInteractBuffer = false,      // Buffer do canInteract — restaurado após armazenar movimento
        canInteract = true,             // Se pode simular interação com líquidos
        inLiquid = false;               // Se está fisicamente dentro de um líquido

    private boolean
        originalValuesStored = false,        // Se os valores originais de movimentação foram armazenados
        needsUpdateStoredMovement = true;    // Se deve rearmazenar os valores de movimentação

    private boolean
        needsUpdateCurrentLiquidData = true, // Se precisa atualizar os dados de líquido atual (mais denso / maior drag)
        needsUpdateCurrentRegion = true,     // Se precisa atualizar a região atual (submersão)
        needsUpdatePhysicsData = true,       // Se precisa recalcular objectDensity (mass/volume mudaram)
        needsUpdateMovement = true,          // Se precisamos atualizar os dados de efeito de movimentação em líquido
        isConstraintsDirty = true;           // Se as constraints precisam ser reaplicadas no moveC

    // --- Dados físicos do objeto ---

    private float
        objectDensity,                  // Densidade cacheada do objeto (mass / volume) — só recalcula via dirty flag
        mass,                           // Massa do objeto
        volume;                         // Volume do objeto

    private float
        floatEffectValue,               // Força de flutuabilidade calculada
        floatEffectValueModifier;       // Modificador externo de flutuabilidade

    private float
        dragMultiplier = 1.0f,          // Multiplicador de resistência ao movimento
        cachedSubmersionFraction = 1f;  // Fração de submersão cacheada — evita recálculo todo frame

    /// Última posição Y conhecida do objeto e última região usada para calcular a
    /// submersão — usados para pular updateSubmersionFraction() quando nada relevante
    /// mudou desde o último frame (maior ganho de otimização do pipeline, já que esse
    /// método rodava incondicionalmente todo frame antes desta revisão).
    private float lastSubmersionCheckObjectY = Float.NaN;
    private LiquidRegion lastSubmersionCheckRegion = null;

    /// Dados de movimentação calculados para o líquido atual (resistência, limites, gravidade)
    private final MovementDataComponent intermediary;

    /// Snapshot dos dados de movimentação originais — restaurados ao sair do líquido
    private final MovementDataComponent storedMovementData;

    private float equilibriumSubmersionThreshold = 0f;

    private static final float MIN_EQUILIBRIUM_THRESHOLD = 0.16f;

    /// Teto de segurança — não faz sentido o threshold ultrapassar 100% de submersão.
    private static final float MAX_EQUILIBRIUM_THRESHOLD = 0.95f;

    private static final float EQUILIBRIUM_CALIBRATION_FACTOR = 1.5f;

    private boolean disposed = false;

    public PhysicalMobLiquidInteractionComponent(SimpleLiquidInteractableObjectII object) {
        this.object = object;
        this.moveC = object.getMoveC();
        this.storedMovementData = new MovementDataComponent();
        this.intermediary = new MovementDataComponent();
        // Armazena os valores originais de movimentação na primeira oportunidade
        this.updateCurrentStoredMovementValues();
    }

    // --- Pipeline ---

    /// Restaura canInteract e armazena valores de movimentação quando solicitado
    private void updateStoredMovement() {
        if (!needsUpdateStoredMovement) return;
        storeCurrentMovementValues();
        canInteract = canInteractBuffer;
        needsUpdateStoredMovement = false;
    }

    @Override
    public void update(float delta) {
        updateLiquidState();
        updateSimulation(delta);
        updateStoredMovement();

        if (!inLiquid) return;
        object.inLiquidUpdate();
    }

    @Override
    public void postUpdate() {
    }

    @Override
    public void initObject() {
    }

    /// Recalcula a fração de submersão de equilíbrio teórica (Arquimedes calibrado).
    /// Chamado sempre que objectDensity ou o líquido de maior densidade mudam.
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

    private void updateSimulation(float delta) {

        boolean shouldSimulate = canInteract && !liquidAndRegionMap.isEmpty();

        applyChange(shouldSimulate);

        if (!shouldSimulate)
            return;

        updatePhysicsData();
        updateMovementData();
        updateSubmersionFraction();

        applyLiquidPhysics(delta);
    }

    private void recalculatePhysicsData() {
        objectDensity = (volume > 0f) ? (mass / volume) : Float.MAX_VALUE;
    }

    private void updatePhysicsData() {
        if (!needsUpdatePhysicsData) return;

        recalculatePhysicsData();
        recalculateEquilibriumThreshold();

        // A densidade mudou, então o efeito de flutuabilidade precisa ser recalculado também
        needsUpdateMovement = true;
        needsUpdatePhysicsData = false;
    }

    private void updateMovementData() {
        if (!needsUpdateMovement) return;

        recalculateMovementData();

        isConstraintsDirty = true;

        needsUpdateMovement = false;
    }

    private void applyLiquidPhysics(float delta) {

        if (cachedSubmersionFraction <= 0f) {
            resetSimulatedFloatation();
            return;
        }

        calculateFloatEffect(highestDensityLiquidBuffer, delta);

        applyConstraints();

        applyFloat();
    }

    private void applyFloat() {
        if (cachedSubmersionFraction <= 0f) return;

        float targetFloat =
            (floatEffectValue + floatEffectValueModifier)
                * cachedSubmersionFraction;

        moveC.dataComponent.yAxis.setMovement(targetFloat);
    }

    private void recalculateMovementData() {
        prepareIntermediary();
        calculateResistance(highestDragLiquidBuffer);
    }

    /// Calcula o quanto o objeto está submerso na região líquida atual (0-1).
    /// Usa apenas as dimensões da região (surfaceY) — não depende de densidade/drag.
    ///
    /// OTIMIZAÇÃO: pula o recálculo inteiro se nem a posição Y do objeto nem a região
    /// de referência mudaram desde a última checagem — este era o único método do
    /// pipeline que rodava incondicionalmente todo frame, mesmo com o objeto parado.
    private void updateSubmersionFraction() {
        if (currentLiquidRegionBuffer == null) {
            cachedSubmersionFraction = 0f;
            lastSubmersionCheckRegion = null;
            return;
        }

        TransformComponent t = object.getTransformC();
        float objectBottomY = t.y;

        boolean regionChanged = currentLiquidRegionBuffer != lastSubmersionCheckRegion;
        boolean positionUnchanged = !regionChanged
            && Float.compare(objectBottomY, lastSubmersionCheckObjectY) == 0;

        if (positionUnchanged) return; // nada mudou, cachedSubmersionFraction continua válido

        lastSubmersionCheckObjectY = objectBottomY;
        lastSubmersionCheckRegion = currentLiquidRegionBuffer;

        // A superfície do líquido é o TOPO da região (y + height), não a base (y)
        float surfaceY = currentLiquidRegionBuffer.getY() + currentLiquidRegionBuffer.getHeight();

        float objectTopY = objectBottomY + t.height;

        // totalmente submerso: o topo do objeto está abaixo da superfície
        if (objectTopY <= surfaceY) {
            cachedSubmersionFraction = 1f;
            return;
        }

        // totalmente fora: a base do objeto está acima da superfície
        if (objectBottomY >= surfaceY) {
            cachedSubmersionFraction = 0f;
            return;
        }

        // parcialmente submerso: fração da altura do objeto que está abaixo da superfície
        cachedSubmersionFraction =
            (surfaceY - objectBottomY) / t.height;
    }

    private void applyConstraints() {
        if (!isConstraintsDirty) return;

        // Cacheia a referência local para evitar resolver moveC.dataComponent repetidamente
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

    /// Calcula a força de flutuabilidade simulada com base na diferença de densidade
    /// entre objeto e líquido, integrada suavemente ao longo do tempo (delta).
    /// O floatEffectValueModifier NÃO participa dessa integração — é somado
    /// separadamente em applyFloat(), como um valor forçado por frame.
    private void calculateFloatEffect(LiquidData data, float delta) {
        if (data == null || volume <= 0f) return;

        float targetFloatEffect =
            (data.density - objectDensity) * volume;

        // Perto da superfície (submersão abaixo do limiar), travamos o valor
        // diretamente no alvo já achatado pela submersão, evitando acúmulo residual.
        if (cachedSubmersionFraction < equilibriumSubmersionThreshold) {
            floatEffectValue = targetFloatEffect * cachedSubmersionFraction;
            return;
        }

        floatEffectValue += (targetFloatEffect - floatEffectValue) * Math.min(delta, 1f);

        // Nunca ultrapassa o alvo teórico em módulo — reaproveita o sinal de
        // targetFloatEffect em vez de recalcular Math.abs duas vezes.
        if (targetFloatEffect >= 0f) {
            if (floatEffectValue > targetFloatEffect) floatEffectValue = targetFloatEffect;
            else if (floatEffectValue < -targetFloatEffect) floatEffectValue = -targetFloatEffect;
        } else {
            if (floatEffectValue < targetFloatEffect) floatEffectValue = targetFloatEffect;
            else if (floatEffectValue > -targetFloatEffect) floatEffectValue = -targetFloatEffect;
        }
    }

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
            moveC.dataComponent.yAxis.resetMovement();
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

    /// Encontra a região líquida mais próxima do centro do objeto.
    ///
    /// OTIMIZAÇÃO: early-exit quando só existe uma única região no sistema (caso comum
    /// para a maioria dos objetos, que só tocam um líquido por vez) — evita o cálculo
    /// de distância euclidiana desnecessário quando não há nada para comparar.
    private void updateCurrentRegion() {

        if (liquidAndRegionMap.isEmpty()) {
            currentLiquidRegionBuffer = null;
            return;
        }

        // Fast path: única região em contato, não há o que comparar
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

    /// Encontra o líquido de maior densidade e o de maior drag entre os líquidos em contato.
    ///
    /// OTIMIZAÇÃO: early-exit quando só existe um único líquido no sistema (caso comum),
    /// evitando a iteração de comparação desnecessária.
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

            if (data.density > highestDensity.density)
                highestDensity = data;

            if (data.drag > highestDrag.drag)
                highestDrag = data;
        }

        highestDensityLiquidBuffer = highestDensity;
        highestDragLiquidBuffer = highestDrag;

        recalculateEquilibriumThreshold();
    }

    // --- Liquid buffer ---

    /// Registra contato com um líquido/região. Só marca para recálculo quando necessário.
    public void addLiquid(LiquidData liquidData, LiquidRegion region) {
        if (region == null || liquidData == null) return;

        ArrayList<LiquidRegion> list = liquidAndRegionMap.get(liquidData);

        if (list == null) {
            list = new ArrayList<>(4); // capacidade inicial pequena — poucas regiões por líquido é o caso comum
            liquidAndRegionMap.put(liquidData, list);
            needsUpdateCurrentLiquidData = true;
        }

        list.add(region);
        needsUpdateCurrentRegion = true;
    }

    /// Remove contato com um líquido/região. Só remove os dados do líquido quando a última região sai.
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

    // --- Movement data ---

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

    // --- Recalculation ---

    private void calculateResistance(LiquidData data) {
        if (data == null) return;

        float drag = data.drag * dragMultiplier;

        intermediary.xAxis.weightFactor = drag;
        intermediary.yAxis.weightFactor = drag;
        intermediary.rAxis.weightFactor = drag;

        intermediary.xAxis.deceleration = storedMovementData.xAxis.deceleration + drag;
        intermediary.yAxis.deceleration = storedMovementData.yAxis.deceleration + drag;
        intermediary.rAxis.deceleration = storedMovementData.rAxis.deceleration + drag;
    }

    // --- Getters / Setters ---

    private void markUpdateSimulationData() {
        needsUpdatePhysicsData = true;
        needsUpdateMovement = true;
        isConstraintsDirty = true;
    }

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

    public float getVolume() {
        return volume;
    }

    public float getFloatEffect() {
        return floatEffectValue;
    }

    public float getFloatEffectValueModifier() {
        return floatEffectValueModifier;
    }

    public float getDragMultiplier() {
        return dragMultiplier;
    }

    public boolean isInLiquid() {
        return inLiquid;
    }

    public void setMass(float mass) {
        if (mass < 0) return;
        this.mass = mass;
        markUpdateSimulationData();
    }

    public void setVolume(float volume) {
        this.volume = volume;
        markUpdateSimulationData();
    }

    public void setFloatingEffectModifier(float v) {
        this.floatEffectValueModifier = v;
    }

    public void setDragMultiplier(float v) {
        this.dragMultiplier = v;
        markUpdateSimulationData();
    }

    public void updateCurrentStoredMovementValues() {
        this.needsUpdateStoredMovement = true;
        this.canInteractBuffer = this.canInteract;
        this.canInteract = false;
        this.originalValuesStored = false;
    }

    private void resetSimulatedFloatation() {
        floatEffectValue = 0;
    }

    public void resetFlotation() {
        floatEffectValue = 0;
        floatEffectValueModifier = 0;
    }

    public boolean hasFloatation() {
        return floatEffectValueModifier != 0 || floatEffectValue != 0;
    }

    // --- Dispose ---

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
