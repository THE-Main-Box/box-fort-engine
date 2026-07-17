package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.MathUtils;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.SimpleLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.MovementDataComponent;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;

import java.util.ArrayList;
import java.util.IdentityHashMap;

/// Simula interação física com líquidos — aplica flutuabilidade, resistência e limites de velocidade.
/// Deve ser atualizado após o componente de movimentação.
public class PhysicalMobLiquidInteractionComponent implements Component {

    /// Refer�ncia ao objeto dono
    private SimpleLiquidInteractableObjectII object;

    /// Componente de movimenta��o do objeto dono
    private MovementComponent moveC;

    /// Mapa para identificação de liquidos
    private final IdentityHashMap<LiquidData, ArrayList<LiquidRegion>> liquidAndRegionMap = new IdentityHashMap<>();

    private LiquidData
        highestDensityLiquidBuffer,
        highestDragLiquidBuffer;

    private LiquidRegion currentLiquidRegionBuffer;

    // --- Flags de estado ---

    private boolean
        canInteractBuffer = false,      // Buffer do canInteract — restaurado ap�s armazenar movimento
        canInteract = true,             // Se pode simular intera��o com l�quidos
        inLiquid = false;               // Se est� fisicamente dentro de um l�quido

    private boolean
        originalValuesStored = false,   // Se os valores originais de movimenta��o foram armazenados
        needsUpdateStoredMovement = true;    // Se deve rearmazenar os valores de movimenta��o

    private boolean
        needsUpdateCurrentLiquidData = true, // Se precisa atualizar os dados de líquido atual (mais denso / maior drag)
        needsUpdateCurrentRegion = true,     // Se precisa atualizar a região atual (submersão)
        needsUpdatePhysicsData = true,       // Se precisa recalcular objectDensity (mass/volume mudaram)
        needsUpdateMovement = true,          // Se precisamos atualizar os dados de efeito de movimentação em líquido
        isConstraintsDirty = true;           // Se as constraints precisam ser reaplicadas no moveC

    // --- Dados f�sicos do objeto ---

    private float
        objectDensity,                  // Densidade cacheada do objeto (mass / volume) — só recalcula via dirty flag
        mass,                           // Massa do objeto
        volume;                         // Volume do objeto

    private float
        floatEffectValue,               // For�a de flutuabilidade calculada
        floatEffectValueModifier;       // Modificador externo de flutuabilidade

    private float
        dragMultiplier = 1.0f,    // Multiplicador de resist�ncia ao movimento
        cachedSubmersionFraction = 1f;  // Fra��o de submers�o cacheada — evita rec�lculo todo frame

    /// Dados de movimenta��o calculados para o l�quido atual (resist�ncia, limites, gravidade)
    private final MovementDataComponent intermediary;

    /// Snapshot dos dados de movimenta��o originais — restaurados ao sair do l�quido
    private final MovementDataComponent storedMovementData;

    private float equilibriumSubmersionThreshold = 0f;

    private static final float MIN_EQUILIBRIUM_THRESHOLD = 0.16f;

    /// Teto de segurança — não faz sentido o threshold ultrapassar 100% de submersão.
    private static final float MAX_EQUILIBRIUM_THRESHOLD = 0.95f;

    private static final float EQUILIBRIUM_CALIBRATION_FACTOR = 1.5f; // ~0.082 * 1.95 ≈ 0.16

    private boolean disposed = false;

    public PhysicalMobLiquidInteractionComponent(SimpleLiquidInteractableObjectII object) {
        this.object = object;
        this.moveC = object.getMoveC();
        this.storedMovementData = new MovementDataComponent();
        this.intermediary = new MovementDataComponent();
        // Armazena os valores originais de movimenta��o na primeira oportunidade
        this.updateCurrentStoredMovementValues();
    }

    // --- Pipeline ---

    /// Restaura canInteract e armazena valores de movimenta��o quando solicitado
    private void updateStoredMovement() {
        //Se não precsarmos atualizar este dado ignoramos tudo
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

    // Novo método, chamado sempre que objectDensity ou highestDensityLiquidBuffer mudam:
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

        if (!needsUpdateMovement)
            return;

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

        calculateResistance(
            highestDragLiquidBuffer
        );

    }

    private void updateSubmersionFraction() {
        if (currentLiquidRegionBuffer == null) {
            cachedSubmersionFraction = 0f;
            return;
        }

        // A superfície do líquido é o TOPO da região (y + height), não a base (y)
        float surfaceY = currentLiquidRegionBuffer.getY() + currentLiquidRegionBuffer.getHeight();

        float objectBottomY = object.getTransformC().y;
        float objectTopY = objectBottomY + object.getTransformC().height;

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
            (surfaceY - objectBottomY) / object.getTransformC().height;
    }

    private void applyConstraints() {
        if (!isConstraintsDirty) return;

        moveC.dataComponent.xAxis.weightFactor = intermediary.xAxis.weightFactor;
        moveC.dataComponent.yAxis.weightFactor = intermediary.yAxis.weightFactor;
        moveC.dataComponent.rAxis.weightFactor = intermediary.rAxis.weightFactor;

        moveC.dataComponent.xAxis.deceleration = intermediary.xAxis.deceleration;
        moveC.dataComponent.yAxis.deceleration = intermediary.yAxis.deceleration;
        moveC.dataComponent.rAxis.deceleration = intermediary.rAxis.deceleration;

        moveC.dataComponent.xAxis.maxMoveVel = intermediary.xAxis.maxMoveVel;
        moveC.dataComponent.yAxis.maxMoveVel = intermediary.yAxis.maxMoveVel;
        moveC.dataComponent.rAxis.maxMoveVel = intermediary.rAxis.maxMoveVel;

        moveC.dataComponent.gravityAffected = intermediary.gravityAffected;
        moveC.dataComponent.gravityScale = intermediary.gravityScale;

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

        // Perto da superfície (submersão abaixo do limiar), o empuxo teórico já é
        // pequeno de qualquer forma — mas convergir gradualmente (+=) ainda deixa
        // resíduo de frames anteriores empurrando o valor interno para cima, mesmo
        // que a fração aplicada em applyFloat() seja pequena. Isso causa o efeito de
        // "10% viram 20%, viram 30%..." até estourar. Abaixo do limiar, paramos de
        // acumular e travamos floatEffectValue diretamente no alvo já achatado pela
        // submersão, então applyFloat() nunca recebe um valor crescendo sem controle.
        if (cachedSubmersionFraction < equilibriumSubmersionThreshold) {
            floatEffectValue = targetFloatEffect * cachedSubmersionFraction;
            return;
        }

        floatEffectValue += (targetFloatEffect - floatEffectValue) * Math.min(delta, 1f);

        // Nunca ultrapassa o alvo teórico em módulo
        float maxValue = Math.abs(targetFloatEffect);

        if (floatEffectValue > maxValue)
            floatEffectValue = maxValue;
        else if (floatEffectValue < -maxValue)
            floatEffectValue = -maxValue;
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

    private void updateCurrentRegion() {

        // Não há líquidos em contato
        if (liquidAndRegionMap.isEmpty()) {
            currentLiquidRegionBuffer = null;
            return;
        }

        float objectCenterX = object.getTransformC().getCenterX();
        float objectCenterY = object.getTransformC().getCenterY();

        LiquidRegion closestRegion = null;
        float closestDistance = Float.MAX_VALUE;

        for (ArrayList<LiquidRegion> list : liquidAndRegionMap.values()) {
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

    private void updateCurrentLiquidData() {

        // Não estamos em nenhum líquido
        if (liquidAndRegionMap.isEmpty()) {
            highestDensityLiquidBuffer = null;
            highestDragLiquidBuffer = null;
            return;
        }

        LiquidData highestDensity = null;
        LiquidData highestDrag = null;

        for (LiquidData data : liquidAndRegionMap.keySet()) {

            // Primeiro líquido encontrado
            if (highestDensity == null) {
                highestDensity = data;
                highestDrag = data;
                continue;
            }

            // Atualiza o líquido mais denso
            if (data.density > highestDensity.density)
                highestDensity = data;

            // Atualiza o líquido de maior drag
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
            list = new ArrayList<>();
            liquidAndRegionMap.put(liquidData, list);

            // Novo líquido no sistema, precisamos recalcular densidade/drag de referência
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

    /// Armazena snapshot dos valores atuais de movimentação para restaurar ao sair do líquido
    private void storeCurrentMovementValues() {
        if (originalValuesStored) return;
        this.storedMovementData.set(moveC.dataComponent);
        originalValuesStored = true;
    }

    /// Restaura os valores de movimentação originais armazenados antes de entrar no líquido
    private void restartStoredMovementValues() {
        if (!originalValuesStored) return;
        this.moveC.dataComponent.set(storedMovementData);
        isConstraintsDirty = true; // Força reaplicar constraints após restaurar
    }

    /// Prepara o intermediary como cópia limpa dos dados originais antes do recálculo
    private void prepareIntermediary() {
        intermediary.set(storedMovementData);
    }

    // --- Recalculation ---

    /// Calcula resistência ao movimento e inércia com base no líquido mais resistente (maior drag)
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
        needsUpdatePhysicsData = true;   // recalcula objectDensity (mass/volume)
        needsUpdateMovement = true;      // recalcula intermediary (resistência/constraints)
        isConstraintsDirty = true;       // força reaplicar constraints no moveC mesmo se os valores não mudarem
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
        if(mass <0) return;
        this.mass = mass;
        markUpdateSimulationData();
    }

    public void setVolume(float volume) {
        this.volume = volume;
        markUpdateSimulationData();
    }

    public void setFloatingEffectModifier(float v) {
        this.floatEffectValueModifier = v;
        // sem markUpdateSimulationData() — é forçado, aplicado direto por frame
    }

    public void setDragMultiplier(float v) {
        this.dragMultiplier = v;
        markUpdateSimulationData();
    }

    /// Marca para rearmazenar valores de movimenta��o na pr�xima atualiza��o.
    /// Usado quando as constraints de movimenta��o mudam externamente.
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
        //Retorna true caso algum desses valores sejam diferentes de 0
        return
            floatEffectValueModifier != 0
                ||
                floatEffectValue != 0;
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
