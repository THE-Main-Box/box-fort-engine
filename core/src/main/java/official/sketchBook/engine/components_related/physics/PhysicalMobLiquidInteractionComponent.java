package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.MathUtils;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.LiquidInteractableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.AxisData;
import official.sketchBook.engine.components_related.objects.MovementDataComponent;
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
    private MovementDataComponent
        storedMovementData;

    private AxisData
        xAxis,
        yAxis,
        rAxis;

    /// Flags de auxilio
    private boolean
        disposed = false;

    public PhysicalMobLiquidInteractionComponent(LiquidInteractableObjectII object) {
        this.object = object;
        this.moveC = object.getMoveC();

        this.storedMovementData = new MovementDataComponent();

        this.xAxis = new AxisData();
        this.yAxis = new AxisData();
        this.rAxis = new AxisData();
    }

    private void updateAxisReferences() {
        if (moveC != null && moveC.dataComponent != null) {
            this.xAxis = moveC.dataComponent.xAxis;
            this.yAxis = moveC.dataComponent.yAxis;
            this.rAxis = moveC.dataComponent.rAxis;
        }
    }

    @Override
    public void update(float delta) {
        updateAxisReferences();

        final boolean shouldSimulate = canInteract && !liquidBuffer.isEmpty();

        applyChange(shouldSimulate);
        applyPhysics(shouldSimulate, needsRecalculation);
    }

    private void applyChange(boolean isInsideLiquid) {
        //Caso haja uma diferença nos dados de estar dentro de um liquido anteriormente e poder estar agora
        if (isInsideLiquid && !inLiquid) {
            inLiquid = true;
            object.onLiquidEnter(liquidBuffer.get(0));
        } else if (!isInsideLiquid && inLiquid) {
            inLiquid = false;

            restartStoredMovementValues();

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

    /// Aplica a simulação de física no corpo do objeto em mãos
    private void applyLiquidEffects() {
        if (neutralBuoyancy) {
            moveC.dataComponent.gravityAffected = false;
            totalBoyancyEffect = 0;
            boyancyFactor = 0;
        }

        updateTotalBoyancyEffect();

        if (Math.abs(totalBoyancyEffect) < BOYANCY_THRESHOLD) return;

//        moveC.setySpeed(totalBoyancyEffect);
        yAxis.velocity += totalBoyancyEffect;
    }

    /// Atualiza o valor de flutuabilidade
    private void updateTotalBoyancyEffect() {
        //Determinamos que fator de flutuabilidade final será uma soma contínua de:
        totalBoyancyEffect +=
            boyancyFactor +         //Fator original
                boyancyModifier;    //Modificador

        //Limitamos o valor final com o limite de movimentação determinado no sistema de movimentação

//        totalBoyancyEffect = MathUtils.clamp(
//            totalBoyancyEffect,
//            yAxis.maxMoveVel,
//            -yAxis.maxMoveVel
//        );

        totalBoyancyEffect = Math.max(
            -yAxis.maxMoveVel,
            Math.min(
                yAxis.maxMoveVel,
                totalBoyancyEffect
            )
        );
    }

    /**
     * Adiciona um líquido ao buffer e marca para recalcular a simulação.
     */
    public void addLiquid(LiquidData liquid) {
        if (liquid == null) return;

        //Adicionamos apenas caso já não exista no buffer
        for (int i = 0; i < liquidBuffer.size(); i++) {
            if (liquidBuffer.get(i).id == liquid.id) return;
        }

        liquidBuffer.add(liquid);

        needsRecalculation = true;
    }

    /**
     * Remove um líquido do buffer e marca para recalcular a simulação.
     */
    public void removeLiquid(LiquidData liquid) {
        if (liquid == null) return;

        //Removemos apenas se encontrarmos
        for (int i = liquidBuffer.size() - 1; i >= 0; i--) {
            if (liquidBuffer.get(i).id == liquid.id) {
                liquidBuffer.remove(i);
                needsRecalculation = true;
                return;
            }
        }

    }

    /// Prepara o ambiente e atualiza os dados de movimentação armazenados
    public void updateCurrentMovementValues(boolean reset) {
        this.setCanInteract(false);

        if (reset)
            this.resetCurrentMovementValues();
        else
            this.storeCurrentMovementValues();

        this.setCanInteract(true);
    }

    /**
     * Armazena valores atuais do MovementComponent.
     * É Importante ter em mente que este irá armazenar os dados
     */
    private void storeCurrentMovementValues() {
        if (originalValuesStored || !canInteract) return;

        this.storedMovementData.set(
            moveC.dataComponent
        );

        originalValuesStored = true;
    }

    private void resetCurrentMovementValues() {
        //Se não tivermos dados armazenados, ou pudermos retornar, ambos se tornam motivos para sair da função
        if (!originalValuesStored || canInteract) return;

        this.storedMovementData.reset();

        originalValuesStored = false;
    }

    /**
     * Restaura os valores de movimentação original no MovementComponent.
     * Chamado para restaurar a movimentação fora de liquido.
     */
    private void restartStoredMovementValues() {
        if (!originalValuesStored) return;

        this.moveC.
            dataComponent.set(
                storedMovementData
            );

        moveC.dataComponent.xAxis.resetMovement();
        moveC.dataComponent.yAxis.resetMovement();
        moveC.dataComponent.rAxis.resetMovement();
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

        xAxis.weightFactor = storedMovementData.xAxis.weightFactor + resistance;
        yAxis.weightFactor = storedMovementData.yAxis.weightFactor + resistance;
        rAxis.weightFactor = storedMovementData.rAxis.weightFactor + resistance;

        xAxis.deceleration = storedMovementData.xAxis.deceleration + resistance;
        yAxis.deceleration = storedMovementData.yAxis.deceleration + resistance;
        rAxis.deceleration = storedMovementData.rAxis.deceleration + resistance;
    }

    /// Calcula as velocidades máximas
    private void calculateSpeedLimits(LiquidData data) {
        xAxis.maxMoveVel = Math.min(
            storedMovementData.xAxis.maxMoveVel,
            data.maxMoveSpeed
        );

        yAxis.maxMoveVel = Math.min(
            storedMovementData.yAxis.maxMoveVel,
            data.maxSinkSpeed
        );

        rAxis.maxMoveVel = Math.min(
            storedMovementData.rAxis.maxMoveVel,
            data.maxMoveSpeed
        );
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
