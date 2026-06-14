package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.MathUtils;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.SimpleLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.MovementDataComponent;
import official.sketchBook.engine.liquid_related.model.LiquidData;

import java.util.*;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.BOYANCY_THRESHOLD;

/// Aplicar após a atualização do componente de movimentação
public class PhysicalMobLiquidInteractionComponent implements Component {

    /// Referência ao objeto dono
    private SimpleLiquidInteractableObjectII object;

    /// Componente de movimentação
    private MovementComponent moveC;

    /// Buffer de liquidos
    private List<LiquidData> liquidBuffer = new ArrayList<>();
    private Set<Integer> liquidIdSet = new HashSet<>();

    /// flags de constraints e auxiliares
    private boolean
        canInteractBuffer = false,
        canInteract = true,
        neutralBuoyancy = false,
        inLiquid = false,
        originalValuesStored = false,
        updateStoredMovement = true,
        needsRecalculation = true;

    /// Valores de correspondência a dados de movimentação
    private float
        mass,
        volume,
        boyancyEffect,
        boyancyEffectModifier,
        resistanceMultiplier = 1.0f;

    /// Valores de movimentação a serem usados como padrão
    private final MovementDataComponent
        intermediary,
        storedMovementData;

    private boolean disposed = false;

    public PhysicalMobLiquidInteractionComponent(SimpleLiquidInteractableObjectII object) {
        this.object = object;
        this.moveC = object.getMoveC();
        this.storedMovementData = new MovementDataComponent();
        this.intermediary = new MovementDataComponent();
        this.updateCurrentStoredMovementValues();
    }

    // --- Pipeline ---

    private void updateStoredMovement() {
        if (!updateStoredMovement) return;
        storeCurrentMovementValues();
        canInteract = canInteractBuffer;
        updateStoredMovement = false;
    }

    @Override
    public void update(float delta) {
        updateSimulation();
    }

    @Override
    public void postUpdate() {}

    @Override
    public void initObject() {}

    private void updateSimulation() {
        final boolean shouldSimulate = canInteract && !liquidBuffer.isEmpty();
        applyChange(shouldSimulate);
        applyPhysics(shouldSimulate);
        updateStoredMovement();
    }

    /// Realizamos as aplicações relacionadas a simulação do liquido
    private void applyPhysics(boolean shouldSimulate) {
        if (!shouldSimulate) return;

        if (needsRecalculation) {
            recalculateLiquidEffects();
            needsRecalculation = false;
        }

        applyConstraints();   // aplica resistência, limites, gravidade do intermediary no moveC
        applyBoyancy();       // aplica flutuabilidade diretamente no moveC

        object.inLiquidUpdate();
    }
    /// Aplica apenas as constraints do intermediary no moveC, sem tocar na velocidade
    private void applyConstraints() {
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
    }

    /// Aplica flutuabilidade diretamente no moveC
    private void applyBoyancy() {
        if (neutralBuoyancy) {
            resetBoyancy();
            return;
        }

        float currentBoyancy = MathUtils.clamp(
            boyancyEffect + boyancyEffectModifier,
            -moveC.dataComponent.yAxis.maxMoveVel,
            moveC.dataComponent.yAxis.maxMoveVel
        );

        if (Math.abs(currentBoyancy) < BOYANCY_THRESHOLD) return;

        moveC.dataComponent.yAxis.setMovement(
            moveC.dataComponent.yAxis.velocity + currentBoyancy
        );
    }
    /// Aplica as mudanças de estado quando entra ou sai de um liquido
    private void applyChange(boolean shouldSimulate) {
        if (shouldSimulate && !inLiquid) {
            inLiquid = true;
            object.onLiquidEnter();
        } else if (!shouldSimulate && inLiquid) {
            inLiquid = false;
            restartStoredMovementValues();
            moveC.dataComponent.yAxis.resetMovement();
            resetBoyancy();
            object.onLiquidExit();
        }
    }

    /// Aplica flutuabilidade no intermediary
    private void applyLiquidEffects() {
        if (neutralBuoyancy) {
            intermediary.gravityAffected = false;
            resetBoyancy();
        }

        float currentBoyancy = MathUtils.clamp(
            boyancyEffect + boyancyEffectModifier,
            -intermediary.yAxis.maxMoveVel,
            intermediary.yAxis.maxMoveVel
        );

        if (Math.abs(currentBoyancy) < BOYANCY_THRESHOLD) return;

        // Soma a flutuabilidade na velocidade atual do moveC e aplica no intermediary
        intermediary.yAxis.setMovement(
            moveC.dataComponent.yAxis.velocity + currentBoyancy
        );
    }

    // --- Liquid buffer ---

    public void addLiquid(LiquidData liquid) {
        if (liquid == null) return;
        if (!liquidIdSet.add(liquid.id)) return;
        liquidBuffer.add(liquid);
        needsRecalculation = true;
    }

    public void removeLiquid(LiquidData liquid) {
        if (liquid == null) return;
        if (!liquidIdSet.remove(liquid.id)) return;
        for (int i = liquidBuffer.size() - 1; i >= 0; i--) {
            if (liquidBuffer.get(i).id == liquid.id) {
                liquidBuffer.remove(i);
                needsRecalculation = true;
                return;
            }
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
    }

    private void prepareIntermediary() {
        intermediary.set(storedMovementData);
    }

    // --- Recalculation ---

    private void recalculateLiquidEffects() {
        LiquidData
            curLiq,
            densLiq = liquidBuffer.get(0),
            resLiq = densLiq;

        if (liquidBuffer.size() > 1) {
            for (int i = 1; i < liquidBuffer.size(); i++) {
                curLiq = liquidBuffer.get(i);
                if (curLiq.density > densLiq.density) densLiq = curLiq;
                if (curLiq.resistance > resLiq.resistance) resLiq = curLiq;
            }
        }

        prepareIntermediary();
        calculateBoyancy(densLiq);
        calculateResistance(resLiq);
        calculateSpeedLimits(densLiq);
    }

    private void calculateResistance(LiquidData data) {
        float resistance = data.resistance * resistanceMultiplier;
        intermediary.xAxis.weightFactor = resistance;
        intermediary.yAxis.weightFactor = resistance;
        intermediary.rAxis.weightFactor = resistance;
        intermediary.xAxis.deceleration = resistance + storedMovementData.xAxis.deceleration;
        intermediary.yAxis.deceleration = resistance + storedMovementData.yAxis.deceleration;
        intermediary.rAxis.deceleration = resistance + storedMovementData.rAxis.deceleration;
    }

    private void calculateSpeedLimits(LiquidData data) {
        intermediary.xAxis.maxMoveVel = Math.min(storedMovementData.xAxis.maxMoveVel, data.maxMoveSpeed);
        intermediary.yAxis.maxMoveVel = Math.min(storedMovementData.yAxis.maxMoveVel, data.maxSinkSpeed);
        intermediary.rAxis.maxMoveVel = Math.min(storedMovementData.rAxis.maxMoveVel, data.maxMoveSpeed);
    }

    private void calculateBoyancy(LiquidData data) {
        float objectDensity = (volume > 0) ? mass / volume : Float.MAX_VALUE;
        boyancyEffect = (data.density - objectDensity) * volume;
    }

    // --- Getters / Setters ---

    public boolean isCanInteract() { return canInteract; }

    public void setCanInteract(boolean canInteract) {
        if (this.canInteract == canInteract) return;
        this.canInteract = canInteract;
    }

    public float getMass() { return mass; }
    public float getVolume() { return volume; }
    public float getBoyancyEffect() { return boyancyEffect; }
    public float getBoyancyEffectModifier() { return boyancyEffectModifier; }
    public float getResistanceMultiplier() { return resistanceMultiplier; }
    public boolean isInLiquid() { return inLiquid; }
    public List<LiquidData> getLiquidBuffer() { return liquidBuffer; }

    public void setMass(float mass) {
        this.mass = mass;
        needsRecalculation = true;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        needsRecalculation = true;
    }

    public void setBoyancyEffectModifier(float boyancyEffectModifier) {
        this.boyancyEffectModifier = boyancyEffectModifier;
        needsRecalculation = true;
    }

    public void setNeutralBuoyancy(boolean neutral) {
        this.neutralBuoyancy = neutral;
        needsRecalculation = true;
    }

    public void setResistanceMultiplier(float resistanceMultiplier) {
        this.resistanceMultiplier = resistanceMultiplier;
        needsRecalculation = true;
    }

    public void updateCurrentStoredMovementValues() {
        this.updateStoredMovement = true;
        this.canInteractBuffer = this.canInteract;
        this.canInteract = false;
        this.originalValuesStored = false;
    }

    public void resetBoyancy() {
        boyancyEffect = 0;
    }

    // --- Dispose ---

    @Override
    public void dispose() {
        if (disposed) return;
        liquidBuffer.clear();
        liquidIdSet.clear();
        nullifyReferences();
        disposed = true;
    }

    public void nullifyReferences() {
        liquidBuffer = null;
        liquidIdSet = null;
        moveC = null;
        object = null;
    }
}
