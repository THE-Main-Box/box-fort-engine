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
        canInteractBuffer = false,
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

    /// Flags de auxilio
    private boolean disposed = false;

    public PhysicalMobLiquidInteractionComponent(LiquidInteractableObjectII object) {
        this.object = object;
        this.moveC = object.getMoveC();

        this.storedMovementData = new MovementDataComponent();
        this.intermediary = new MovementDataComponent();

        this.updateCurrentStoredMovementValues();
    }

    /// Atualiza a referencia de dados de constraint armazenados
    private void updateStoredMovement() {
        //Se precisamos atualizar iteramos
        if (!updateStoredMovement) return;

        //Armazenamos o dado atual
        storeCurrentMovementValues();

        //Restauramos a flag ao estado de poder interagir
        canInteract = canInteractBuffer;
        updateStoredMovement = false;
    }

    @Override
    public void update(float delta) {
        //Valida se podemos realizar a simulação
        final boolean shouldSimulate = canInteract && !liquidBuffer.isEmpty();

        applyChange(shouldSimulate);

        applyPhysics(shouldSimulate);

        updateStoredMovement();

    }

    /// Realizamos as aplicações relacionadas a simulação do liquido
    private void applyPhysics(boolean shouldSimulate) {
        //Se não podemos simular a interação com liquido, não fazemos isso
        if (!shouldSimulate) return;

        prepareIntermediary();

        // Recalculamos os dados de simulação e aplicamos os limites
        if (needsRecalculation) {
            //Recalcula no intermediary os dados de constraint a serem aplicados
            recalculateLiquidEffects();

            needsRecalculation = false;
        }

        //Aplicamos os dados da simulação que ainda não foram aplicados
        applyLiquidEffects();

        //Aplicamos a intermediary no objeto
        applyCalculatedIntermediary();

        object.inLiquidUpdate();
    }

    /// Aplica as mudanças de estado quando entra ou sai de um liquido
    private void applyChange(boolean shouldSimulate) {
        //Caso haja uma diferença de estados, quer dizer que neste frame acabamos de entrar em um liquido
        //O shouldsimulate já contém o estado de estar dentro de liquido ou não
        if (shouldSimulate && !inLiquid) {
            //Atualizamos o estado de estar em liquido
            inLiquid = true;
            object.onLiquidEnter();
            //Se não podemos simular, ou seja se não podemos interagir,
            // ou não tivermos dentro de um liquido e estivermos com um inLiquid,
            // quer dizer que acabamos de sair de um liquido
        } else if (!shouldSimulate && inLiquid) {
            //Atualizamos o estado para não refletir incorretamente que estamos dentro de um liquido
            inLiquid = false;
            //Restauramos a movimentação original do objeto
            restartStoredMovementValues();

            //Reseta os dados de flutuabilidade, pois não iremos usar e precisaremos deles limpos mais pra frente
            resetBoyancy();

            object.onLiquidExit();
        }
    }

    @Override
    public void postUpdate() {

    }

    /// Aplica a simulação de física no corpo do objeto em mãos
    private void applyLiquidEffects() {
        //Se temos uma flutuabilidade neutra
        if (neutralBuoyancy) {
            intermediary.gravityAffected = false;   // Desligamos a gravidade
            resetBoyancy();                         // Resetamos os dados de flutuabilidade
        }

        //Dependendo dos casos, se estivermos neutros em flutuabilidade, isso não irá afetar em nada
        updateTotalBoyancyEffect();  // Atualiza o efeito total

        // Impedimos que a flutuabilidade afete o objeto caso seja pequena demais para ser percebida
        if (Math.abs(totalBoyancyEffect) < BOYANCY_THRESHOLD) return;

        //Aplicamos no eixo da intermediary, para ser aplicada futuramente
        intermediary.yAxis.velocity += totalBoyancyEffect;
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
            -intermediary.yAxis.maxMoveVel,
            intermediary.yAxis.maxMoveVel
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
        //Marcamos para recalcular pois precisamos lidar com a simulação do novo liquido
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
                //Precisamos recalcular pois o liquido já foi removido e pode ter outro esperando para ser calculado
//                needsRecalculation = true;
                return;
            }
        }
    }

    /// Armazena valores atuais do MovementComponent pra poder restaurar depois
    private void storeCurrentMovementValues() {
        if (originalValuesStored) return;

        // Setamos uma copia que não será modificada
        this.storedMovementData.set(moveC.dataComponent);

        originalValuesStored = true;
    }

    /// Restaura os valores de movimentação original no MovementComponent
    private void restartStoredMovementValues() {
        if (!originalValuesStored) return;

        //Passamos para o moveC o dado armazenado de constraints de movimentação de fora do liquido
        this.moveC.dataComponent.set(storedMovementData);
    }

    private void prepareIntermediary() {
        intermediary.set(storedMovementData);  // Cópia fresca do original
    }

    /// Aplica o intermediário calculado pro moveC (APLICAÇÃO)
    /// Aplica o intermediário calculado pro moveC (APLICAÇÃO)
    private void applyCalculatedIntermediary() {
        moveC.dataComponent.set(intermediary);
    }

    /// Calcula os efeitos dos líquidos no intermediário
    private void recalculateLiquidEffects() {
        //Gera os liquidos que podem ser interpretados de modo isolado e os prepara
        LiquidData
            curLiq,                         //Liquido atual
            densLiq = liquidBuffer.get(0),  //Liquido mais denso
            resLiq = densLiq;               //Liquido mais resistente

        //Se o buffer tiver mais que um liquido, iteramos para identificar qual é o que devemos dar prioridade
        if (liquidBuffer.size() > 1) {
            for (int i = 1; i < liquidBuffer.size(); i++) {
                //Atualizamos o liquido atual
                curLiq = liquidBuffer.get(i);

                //Caso tenhamos encontrado um liquido com densidade maior, atualizamos este
                if (curLiq.density > densLiq.density) {
                    densLiq = curLiq;
                }

                //Caso tenhamos encontrado um liquido com resistencia de movimento maior, atualizamos este
                if (curLiq.resistance > resLiq.resistance) {
                    resLiq = curLiq;
                }
            }
        }

        // Prepara intermediário com dados originais
        prepareIntermediary();

        //Calculamos a flutuabilidade com base no liquido mais denso
        calculateBoyancy(densLiq);
        //Calculamos a resistencia com base no liquido mais resistente
        calculateResistance(resLiq);
        //Calculamos o limite de velocidade com base no liquido mais denso
        calculateSpeedLimits(densLiq);

        //resetamos a flag
        needsRecalculation = false;
    }

    /// Calcula a resistencia de movimento do liquido de maior resistencia
    private void calculateResistance(LiquidData data) {
        //Descobrimos a resistencia real a aplicar
        float resistance = data.resistance * resistanceMultiplier;

        //Atualizamos o weight factor de todos os eixos com base na resistencia de movimento
        intermediary.xAxis.weightFactor = resistance;
        intermediary.yAxis.weightFactor = resistance;
        intermediary.rAxis.weightFactor = resistance;

        //Atualiza a desaceleração com base na resistencia e os dados de movimento originais
        intermediary.xAxis.deceleration = resistance + storedMovementData.xAxis.deceleration;
        intermediary.yAxis.deceleration = resistance + storedMovementData.yAxis.deceleration;
        intermediary.rAxis.deceleration = resistance + storedMovementData.rAxis.deceleration;
    }

    /// Calcula limites de velocidade de movimentação (Não físicos, já que isso daí é outro departamento)
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

    /// Marca dentro da pipeline para poder armazenar as novas constraints
    public void updateCurrentStoredMovementValues() {
        //Marca para a pipeline armazenar os novos dados
        this.updateStoredMovement = true;

        //Armazena a flag de interação atual
        this.canInteractBuffer = this.canInteract;

        //torna a flag de interação falsa para dar brecha para interação
        this.canInteract = false;

        //Dá a permissão para armazenar os dados novamente
        this.originalValuesStored = false;
    }

    public void resetBoyancy() {
        totalBoyancyEffect = 0;
        boyancyEffect = 0;
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
