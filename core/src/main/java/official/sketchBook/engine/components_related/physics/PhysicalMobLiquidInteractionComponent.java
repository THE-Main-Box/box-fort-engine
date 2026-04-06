package official.sketchBook.engine.components_related.physics;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.LiquidInteractableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.liquid_related.model.LiquidData;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.BOYANCY_THRESHOLD;

/// Aplicar após a atualização do componente de movimentação
public class PhysicalMobLiquidInteractionComponent implements Component {

    /// Referência ao objeto dono
    private LiquidInteractableObjectII object;

    /// Componente de movimentação
    private MovementComponent moveC;

    /// Buffer de liquidos, irá determinar os liquidos que precisaremos iterar
    private List<LiquidData> liquidBuffer = new ArrayList<>();

    /// flags de constraints e auxiliares
    private boolean
        wasInLiquid = false,                //Se estávamos dentro de um liquido a pouco tempo
        canInteract = true,                 // Se podemos interagir com líquidos
        neutralBuoyancy = false,            // Se estamos neutralmente boiantes
        inLiquid = false,                   // Se estamos físicamente na área de um líquido
        originalValuesStored = false,       // Se armazenamos os valores de movimentação
        needsRecalculation = true;          // Se precisamos recalcular a simulação de física

    /// Valores de correspondência a dados de movimentação
    private float
        mass,                               // Fator de massa do objeto
        volume,                             // Fator de volume
        totalBoyancyEffect,                 // Efeito de flutuabilidade a ser aplicado no objeto
        boyancyFactor,                      // O quanto iremos flutuar
        boyancyModifier,                    // Modificador dinamico para flutuabilidade
        resistanceMultiplier = 1.0f;        // O quanto iremos responder à resistência de movimentação do liquido

    /// Valores originais de movimentação ao entrar no primeiro líquido
    private float
        originalXResistanceWeight,
        originalYResistanceWeight,
        originalRResistanceWeight,
        originalGravityScale,
        originalXMaxSpeed,
        originalYMaxSpeed,
        originalRMaxSpeed,
        originalXDeceleration,
        originalYDeceleration,
        originalRDeceleration;

    private boolean originallyGravityAffected;

    /// Flags de auxilio
    private boolean
        disposed = false;

    public PhysicalMobLiquidInteractionComponent(LiquidInteractableObjectII object) {
        this.object = object;
        this.moveC = object.getMoveC();
    }

    @Override
    public void update(float delta) {
        final boolean shouldSimulate = canInteract && !liquidBuffer.isEmpty();

        applyChange(shouldSimulate);
        applyPhysics(shouldSimulate, needsRecalculation);
    }

    private void applyChange(boolean isInsideLiquid){
        //Caso haja uma diferença nos dados de estar dentro de um liquido anteriormente e poder estar agora
        if (isInsideLiquid && !inLiquid) {
            storeOriginalMovementValues();
            inLiquid = true;
            object.onLiquidEnter(liquidBuffer.get(0));
        }
        else if (!isInsideLiquid && inLiquid) {
            inLiquid = false;

            restartOriginalMovementValues();
            moveC.resetYMovement();

            totalBoyancyEffect = 0;
            boyancyFactor = 0;

            object.onLiquidExit(null);
        }
    }

    /// Realizamos as aplicações relacionadas a simulação do liquido
    private void applyPhysics(
        boolean shouldSimulate,
        boolean needsRecalculation
    ) {

        //Se não podemos simular a interação com liquido, não fazemos isso
        if (!shouldSimulate) return;

        // Recalculamos os dados de simulação e aplicamos os limites
        if (needsRecalculation) {
            recalculateLiquidEffects();
        }

        //Aplicamos os dados da simulação que ainda não foram aplicados
        applyLiquidEffects();

        //Realizamos um update interno do objeto
        object.inLiquidUpdate();
    }

    @Override
    public void postUpdate() {

    }

    private void applyLiquidEffects() {
        if (!canInteract) return;

        if (neutralBuoyancy) {
            moveC.gravityAffected = false;
            totalBoyancyEffect = 0;
            boyancyFactor = 0;
        }

        updateTotalBoyancyEffect();

        if (Math.abs(totalBoyancyEffect) < BOYANCY_THRESHOLD) return;

//        moveC.setySpeed(totalBoyancyEffect);
        moveC.setySpeed(moveC.ySpeed + totalBoyancyEffect);
    }

    private void updateTotalBoyancyEffect() {
        totalBoyancyEffect += boyancyFactor + boyancyModifier;
        totalBoyancyEffect = Math.max(
            -moveC.yMaxMoveSpeed,
            Math.min(moveC.yMaxMoveSpeed, totalBoyancyEffect)
        );
    }

    /**
     * Adiciona um líquido ao buffer.
     */
    public void addLiquid(LiquidData liquid) {
        if (liquid == null) return;

        for (int i = 0; i < liquidBuffer.size(); i++) {
            if (liquidBuffer.get(i).id == liquid.id) return;
        }

        liquidBuffer.add(liquid);

        needsRecalculation = true;
    }

    /**
     * Remove um líquido do buffer.
     */
    public void removeLiquid(LiquidData liquid) {
        if (liquid == null) return;

        for (int i = liquidBuffer.size() - 1; i >= 0; i--) {
            if (liquidBuffer.get(i).id == liquid.id) {
                liquidBuffer.remove(i);
                break;
            }
        }

        needsRecalculation = true;
    }

    /**
     * Armazena valores originais do MovementComponent.
     * Chamado na primeira entrada de um líquido.
     */
    private void storeOriginalMovementValues() {
        if (originalValuesStored || !canInteract) return;

        this.originalYMaxSpeed = moveC.yMaxMoveSpeed;
        this.originalXMaxSpeed = moveC.xMaxMoveSpeed;
        this.originalRMaxSpeed = moveC.rMaxMoveSpeed;

        this.originalXDeceleration = moveC.xDeceleration;
        this.originalYDeceleration = moveC.yDeceleration;
        this.originalRDeceleration = moveC.rDeceleration;

        this.originalGravityScale = moveC.gravityScale;
        this.originallyGravityAffected = moveC.gravityAffected;

        this.originalXResistanceWeight = moveC.xResistanceWeight;
        this.originalYResistanceWeight = moveC.yResistanceWeight;
        this.originalRResistanceWeight = moveC.rResistanceWeight;

        originalValuesStored = true;
    }

    /**
     * Restaura os valores de movimentação original no MovementComponent.
     * Chamado para restaurar a movimentação fora de liquido.
     */
    private void restartOriginalMovementValues() {
        if (!originalValuesStored) return;

        this.moveC.yMaxMoveSpeed = originalYMaxSpeed;
        this.moveC.xMaxMoveSpeed = originalXMaxSpeed;
        this.moveC.rMaxMoveSpeed = originalRMaxSpeed;

        this.moveC.xDeceleration = originalXDeceleration;
        this.moveC.yDeceleration = originalYDeceleration;
        this.moveC.rDeceleration = originalRDeceleration;

        this.moveC.gravityScale = originalGravityScale;
        this.moveC.gravityAffected = originallyGravityAffected;

        this.moveC.xResistanceWeight = originalXResistanceWeight;
        this.moveC.yResistanceWeight = originalYResistanceWeight;
        this.moveC.rResistanceWeight = originalRResistanceWeight;

        originalValuesStored = false;
    }

    /**
     * Calcula os efeitos dos líquidos no buffer.
     * Chamado quando needsRecalculation é true.
     */
    private void recalculateLiquidEffects() {
        if (!inLiquid || liquidBuffer.isEmpty()) {
            needsRecalculation = false;
            return;
        }

        LiquidData tmpCurrentLiquid, tmpCurrentLiquidByDensity, tmpCurrentLiquidByResistance;

        tmpCurrentLiquidByDensity = tmpCurrentLiquidByResistance = liquidBuffer.get(0);

        if (liquidBuffer.size() > 1) {
            for (int i = 0; i < liquidBuffer.size(); i++) {
                tmpCurrentLiquid = liquidBuffer.get(i);

                if (tmpCurrentLiquid.density > tmpCurrentLiquidByDensity.density) {
                    tmpCurrentLiquidByDensity = tmpCurrentLiquid;
                }

                if (tmpCurrentLiquid.resistance > tmpCurrentLiquidByResistance.resistance) {
                    tmpCurrentLiquidByResistance = tmpCurrentLiquid;
                }
            }
        }

        calculateBoyancy(tmpCurrentLiquidByDensity);
        calculateResistance(tmpCurrentLiquidByResistance);
        calculateSpeedLimits(tmpCurrentLiquidByDensity);

        needsRecalculation = false;
    }

    /// Calcula a flutuabilidade a ser aplicada
    private void calculateBoyancy(LiquidData data) {
        float objectDensity = (volume > 0) ? mass / volume : Float.MAX_VALUE;
        boyancyFactor = (data.density - objectDensity) * volume;
    }

    /// Calcula os dados para simulação de resistencia de liquidos
    private void calculateResistance(LiquidData data) {
        float resistance = data.resistance * resistanceMultiplier;

        moveC.xResistanceWeight = resistance;
        moveC.yResistanceWeight = resistance;
        moveC.rResistanceWeight = resistance;

        moveC.xDeceleration = originalXDeceleration + resistance;
        moveC.yDeceleration = originalYDeceleration + resistance;
        moveC.rDeceleration = originalRDeceleration + resistance;
    }

    /// Calcula as velocidades máximas
    private void calculateSpeedLimits(LiquidData data) {
        moveC.xMaxMoveSpeed = Math.min(originalXMaxSpeed, data.maxMoveSpeed);
        moveC.rMaxMoveSpeed = Math.min(originalRMaxSpeed, data.maxMoveSpeed);
        moveC.yMaxMoveSpeed = Math.min(originalYMaxSpeed, data.maxSinkSpeed);
    }

    public void setMass(float mass) {
        this.mass = mass;
        needsRecalculation = true;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        needsRecalculation = true;
    }

    public void setBoyancyModifier(float boyancyModifier) {
        this.boyancyModifier = boyancyModifier;
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

    public boolean isCanInteract() {
        return canInteract;
    }

    public void setCanInteract(boolean canInteract) {
        if (this.canInteract == canInteract) return;    //Se o valor passado for o mesmo, ignoramos

        //Atribuimos o novo valor
        this.canInteract = canInteract;

        //Como o valor será diferente, precisamos recalcular algumas coisas
        this.needsRecalculation = true;
    }

    public void setNeedsRecalculation(boolean needsRecalculation) {
        this.needsRecalculation = needsRecalculation;
    }

    public float getMass() {
        return mass;
    }

    public float getVolume() {
        return volume;
    }

    public float getBoyancyFactor() {
        return boyancyFactor;
    }

    public float getBoyancyModifier() {
        return boyancyModifier;
    }

    public float getResistanceMultiplier() {
        return resistanceMultiplier;
    }

    public boolean isInLiquid() {
        return inLiquid;
    }

    public float getOriginalXResistanceWeight() {
        return originalXResistanceWeight;
    }

    public float getOriginalYResistanceWeight() {
        return originalYResistanceWeight;
    }

    public float getOriginalRResistanceWeight() {
        return originalRResistanceWeight;
    }

    public float getOriginalGravityScale() {
        return originalGravityScale;
    }

    public float getOriginalXMaxSpeed() {
        return originalXMaxSpeed;
    }

    public float getOriginalYMaxSpeed() {
        return originalYMaxSpeed;
    }

    public float getOriginalRMaxSpeed() {
        return originalRMaxSpeed;
    }

    public float getOriginalXDeceleration() {
        return originalXDeceleration;
    }

    public float getOriginalYDeceleration() {
        return originalYDeceleration;
    }

    public float getOriginalRDeceleration() {
        return originalRDeceleration;
    }

    public boolean isOriginallyGravityAffected() {
        return originallyGravityAffected;
    }

    @Override
    public void dispose() {
        if (disposed) return;

        liquidBuffer.clear();
        nullifyReferences();

        disposed = true;
    }

    @Override
    public void nullifyReferences() {
        liquidBuffer = null;
        moveC = null;
        object = null;
    }
}
