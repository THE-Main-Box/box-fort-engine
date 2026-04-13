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
        updateStoredMovement = true,        //Se devemos marcar para atualizar os valores de movimentação armazenados
        needsRecalculation = true;          // Se precisamos recalcular a simulação de física

    /// Valores de correspondência a dados de movimentação
    private float
        mass,                               // Fator de massa do objeto
        volume,                             // Fator de volume
        totalBoyancyEffect,                 // Efeito de flutuabilidade a ser aplicado no objeto
        boyancyEffect,                      // O quanto iremos flutuar
        boyancyEffectModifier,              // Modificador dinamico para flutuabilidade
        resistanceMultiplier = 1.0f;        // O quanto iremos responder à resistência de movimentação do liquido

    /// Valores de movimentação a serem usados como padrão
    private final MovementDataComponent
        intermediary,
        storedMovementData;

    /// Referência aos dados de eixo dos eixos armazenados (cache pra evitar indireção)
    private final AxisData
        ixAxis, iyAxis, irAxis,
        cxAxis, cyAxis, crAxis,
        sxAxis, syAxis, srAxis;

    /// Flags de auxilio
    private boolean disposed = false;

    public PhysicalMobLiquidInteractionComponent(LiquidInteractableObjectII object) {
        this.object = object;
        this.moveC = object.getMoveC();

        this.storedMovementData = new MovementDataComponent();
        this.intermediary = new MovementDataComponent();

        this.ixAxis = intermediary.xAxis;
        this.iyAxis = intermediary.yAxis;
        this.irAxis = intermediary.rAxis;

        this.sxAxis = storedMovementData.xAxis;
        this.syAxis = storedMovementData.yAxis;
        this.srAxis = storedMovementData.rAxis;

        // Cache dos eixos atuais pra evitar acessos múltiplos via indireção
        this.cxAxis = moveC.dataComponent.xAxis;
        this.cyAxis = moveC.dataComponent.yAxis;
        this.crAxis = moveC.dataComponent.rAxis;

        this.updateCurrentStoredMovementValues();
    }

    private void updateStoredMovement() {
        if (!updateStoredMovement) return;

        storeCurrentMovementValues();

        canInteract = true;
        updateStoredMovement = false;
    }

    @Override
    public void update(float delta) {
        updateStoredMovement();

        final boolean isInsideLiquid = !liquidBuffer.isEmpty();  // Checa se tem liquido no buffer
        final boolean shouldSimulate = canInteract && isInsideLiquid;  // Pode simular?

        applyChange(isInsideLiquid, shouldSimulate);
        applyPhysics(shouldSimulate, needsRecalculation);
    }

    /// Aplica as mudanças de estado quando entra ou sai de um liquido
    private void applyChange(boolean isInsideLiquid, boolean shouldSimulate) {
        //Caso haja uma diferença nos dados de estar dentro de um liquido anteriormente e poder estar agora
        if (isInsideLiquid && !inLiquid && shouldSimulate) {
            inLiquid = true;  // Marca que estamos dentro

            object.onLiquidEnter();
        } else if (!shouldSimulate || inLiquid) {  // Saiu ou não pode mais simular
            inLiquid = false;  // Marca que saímos

            restartStoredMovementValues();  // Restaura aos valores originais

            totalBoyancyEffect = 0;  // Zera os efeitos
            boyancyEffect = 0;

            object.onLiquidExit();
        }
    }

    /// Realizamos as aplicações relacionadas a simulação do liquido
    private void applyPhysics(boolean shouldSimulate, boolean needsRecalculation) {
        //Se não podemos simular a interação com liquido, não fazemos isso
        if (!shouldSimulate) return;

        // Recalculamos os dados de simulação e aplicamos os limites
        if (needsRecalculation) {
            recalculateLiquidEffects();
            applyCalculatedIntermediary();
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
        if (neutralBuoyancy) {  // Se estamos neutro, desligamos gravidade
            moveC.dataComponent.gravityAffected = false;  // Grav desligada
            totalBoyancyEffect = 0;  // E zeramos a flutuabilidade
            boyancyEffect = 0;
        }

        updateTotalBoyancyEffect();  // Atualiza o efeito total

        if (Math.abs(totalBoyancyEffect) < BOYANCY_THRESHOLD) return;  // Se é muito pequeno, ignora

        cyAxis.velocity += totalBoyancyEffect;  // Aplica no eixo Y (usando cache)
    }

    /// Atualiza o valor de flutuabilidade, acumulando modificadores e limitando
    private void updateTotalBoyancyEffect() {
        //Determinamos que fator de flutuabilidade final será uma soma contínua de:
        totalBoyancyEffect +=
            boyancyEffect +         //Fator original
                boyancyEffectModifier;    //Modificador

        //Limitamos o valor final com o limite de movimentação determinado no sistema de movimentação
        totalBoyancyEffect = MathUtils.clamp(
            totalBoyancyEffect,
            -iyAxis.maxMoveVel,
            iyAxis.maxMoveVel
        );
    }

    /**
     * Adiciona um líquido ao buffer e marca para recalcular a simulação.
     */
    public void addLiquid(LiquidData liquid) {
        if (liquid == null) return;

        //Adicionamos apenas caso já não exista no buffer
        for (int i = 0; i < liquidBuffer.size(); i++) {
            if (liquidBuffer.get(i).id == liquid.id) return;  // Já existe, sai
        }

        liquidBuffer.add(liquid);
        needsRecalculation = true;
    }

    /**
     * Remove um líquido do buffer e marca para recalcular a simulação.
     */
    public void removeLiquid(LiquidData liquid) {
        if (liquid == null) return;

        //Removemos apenas se encontrarmos (backwards pra não quebrar índice)
        for (int i = liquidBuffer.size() - 1; i >= 0; i--) {
            if (liquidBuffer.get(i).id == liquid.id) {
                liquidBuffer.remove(i);
                needsRecalculation = true;
                return;
            }
        }
    }

    /// Armazena valores atuais do MovementComponent pra poder restaurar depois
    /// Armazena valores atuais do MovementComponent pra poder restaurar depois
    private void storeCurrentMovementValues() {
        if (originalValuesStored) return;

        // Cria uma cópia imutável
        this.storedMovementData.set(moveC.dataComponent.cpy());

        originalValuesStored = true;
    }

    /// Restaura os valores de movimentação original no MovementComponent
    private void restartStoredMovementValues() {
        if (!originalValuesStored) return;

        // Cria uma cópia NOVA dos dados, não referência
        MovementDataComponent cleanCopy = storedMovementData.cpy();
        this.moveC.dataComponent.set(cleanCopy);

        // Limpa intermediário também
        intermediary.set(storedMovementData);

        needsRecalculation = true;
    }

    private void prepareIntermediary(){
        intermediary.set(storedMovementData);  // Cópia fresca do original
    }

    /// Aplica o intermediário calculado pro moveC (APLICAÇÃO)
    /// Aplica o intermediário calculado pro moveC (APLICAÇÃO)
    private void applyCalculatedIntermediary() {
        // Cria cópia do intermediário pra não contaminar referências
        MovementDataComponent copyToApply = intermediary.cpy();
        moveC.dataComponent.set(copyToApply);
    }

    /// Calcula os efeitos dos líquidos no intermediário
    private void recalculateLiquidEffects() {
        if (!inLiquid || liquidBuffer.isEmpty()) {
            needsRecalculation = false;
            return;
        }

        // Prepara intermediário com dados originais
        prepareIntermediary();

        LiquidData tmpCurrentLiquidByDensity = liquidBuffer.get(0);
        LiquidData tmpCurrentLiquidByResistance = tmpCurrentLiquidByDensity;

        if (liquidBuffer.size() > 1) {
            for (int i = 1; i < liquidBuffer.size(); i++) {
                LiquidData tmpCurrentLiquid = liquidBuffer.get(i);

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

    /// Calcula resistencia NO INTERMEDIÁRIO
    private void calculateResistance(LiquidData data) {
        float resistance = data.resistance * resistanceMultiplier;

        intermediary.xAxis.weightFactor = resistance;
        intermediary.yAxis.weightFactor = resistance;
        intermediary.rAxis.weightFactor = resistance;

        intermediary.xAxis.deceleration = storedMovementData.xAxis.deceleration + resistance;
        intermediary.yAxis.deceleration = storedMovementData.yAxis.deceleration + resistance;
        intermediary.rAxis.deceleration = storedMovementData.rAxis.deceleration + resistance;
    }

    /// Calcula limites NO INTERMEDIÁRIO
    private void calculateSpeedLimits(LiquidData data) {
        intermediary.xAxis.maxMoveVel = Math.min(
            storedMovementData.xAxis.maxMoveVel,
            data.maxMoveSpeed
        );

        intermediary.yAxis.maxMoveVel = Math.min(
            storedMovementData.yAxis.maxMoveVel,
            data.maxSinkSpeed
        );

        intermediary.rAxis.maxMoveVel = Math.min(
            storedMovementData.rAxis.maxMoveVel,
            data.maxMoveSpeed
        );
    }

  /// Calcula a flutuabilidade a ser aplicada
    private void calculateBoyancy(LiquidData data) {
        float objectDensity = (volume > 0) ? mass / volume : Float.MAX_VALUE;  // Densidade = massa / volume
        boyancyEffect = (data.density - objectDensity) * volume;  // Diferença de densidade vezes volume
    }

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

    public boolean isCanInteract() {
        return canInteract;
    }

    public void setCanInteract(boolean canInteract) {
        if (this.canInteract == canInteract) return;    //Se o valor passado for o mesmo, ignoramos

        //Atribuimos o novo valor
        this.canInteract = canInteract;

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

    public float getBoyancyEffect() {
        return boyancyEffect;
    }

    public float getBoyancyEffectModifier() {
        return boyancyEffectModifier;
    }

    public float getResistanceMultiplier() {
        return resistanceMultiplier;
    }

    public boolean isInLiquid() {
        return inLiquid;
    }

    public void updateCurrentStoredMovementValues() {
        this.updateStoredMovement = true;
        this.canInteract = false;
        this.originalValuesStored = false;
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
