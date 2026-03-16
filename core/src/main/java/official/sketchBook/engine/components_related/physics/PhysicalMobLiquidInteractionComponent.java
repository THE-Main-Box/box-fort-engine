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
        canInteractWithLiquid = true,       //Se podemos interagir com líquidos
        neutralBuoyancy = false,            //Se estamos neutralmente boiantes
        inLiquid = false,                   //Se estamos físicamente na área de um líquido
        needsRecalculation = true;          //Se precisamos recalcular a simulação de física

    /// Valores de correspondência a dados de movimentação
    private float
        mass,                               //Fator de massa do objeto
        volume,                             //Fator de volume
        totalBoyancyEffect,                 //Efeito de flutuabilidade a ser aplicado no objeto
        boyancyFactor,                      // O quanto iremos flutuar
        boyancyModifier,                    //Modificador dinamico para flutuabilidade
        resistanceMultiplier = 1.0f;        // O quanto iremos responder à resistência de movimentação do liquido

    /// Valores originais de movimentação ao entrar no primeiro líquido
    private float
        originalXResistanceWeight,      //Constante de simulador de peso original do eixo x
        originalYResistanceWeight,      //Constante de simulador de peso original do eixo y
        originalRResistanceWeight,      //Constante de simulador de peso original do eixo r
        originalGravityScale,           //Constante de escala de gravidade original
        originalXMaxSpeed,              //Constante de velocidade máxima no eixo x
        originalYMaxSpeed,              //Constante de velocidade máxima no eixo y
        originalRMaxSpeed,              //Constante de velocidade máxima de rotação
        originalXDeceleration,          //Constante de desaceleração no eixo x
        originalYDeceleration,          //Constante de desaceleração no eixo y
        originalRDeceleration;          //Constante de desaceleração de rotação

    private boolean originallyGravityAffected;

    private boolean disposed = false;

    public PhysicalMobLiquidInteractionComponent(LiquidInteractableObjectII object) {
        this.object = object;
        this.moveC = object.getMoveC();
    }

    @Override
    public void update(float delta) {
        if (!inLiquid) return;

        if (needsRecalculation) {
            recalculateLiquidEffects();
        }

        applyLiquidEffects();

        object.inLiquidUpdate();
    }

    @Override
    public void postUpdate() {

    }

    private void applyLiquidEffects() {
        if (neutralBuoyancy) {
            moveC.gravityAffected = false;
            totalBoyancyEffect = 0;
            boyancyFactor = 0;
//            return;
        }

        updateTotalBoyancyEffect();

        if (Math.abs(totalBoyancyEffect) < BOYANCY_THRESHOLD) return;

        moveC.setySpeed(totalBoyancyEffect);
    }

    private void updateTotalBoyancyEffect(){
        totalBoyancyEffect += boyancyFactor + boyancyModifier;
        totalBoyancyEffect = Math.max(
            -moveC.yMaxSpeed,
            Math.min(
                moveC.yMaxSpeed,
                totalBoyancyEffect
            )
        );
    }

    /**
     * Adiciona um líquido ao buffer.
     */
    public void addLiquid(LiquidData liquid) {
        if (liquid == null) return;

        // Percorre todos os objetos da lista
        for (int i = 0; i < liquidBuffer.size(); i++) {
            //Se o id do atual for igual ao id do objeto que foi passado
            if (liquidBuffer.get(i).id
                ==
                liquid.id
                //Retornamos para não adicionar na lista
            ) return;

        }

        liquidBuffer.add(liquid);

        if (liquidBuffer.size() == 1) {
            storeOriginalMovementValues();
        }

        object.onLiquidEnter();

        inLiquid = true;
        needsRecalculation = true;
    }


    /// Remove um líquido do buffer
    public void removeLiquid(LiquidData liquid) {
        if (liquid == null) return;


        // Percorre todos os objetos da lista
        for (int i = liquidBuffer.size() - 1; i >= 0; i--) {
            //Se o id do atual for igual ao id do objeto que foi passado
            if (liquidBuffer.get(i).id
                ==
                liquid.id
            ) {
                //Removemos
                liquidBuffer.remove(i);
                break;
            }

        }

        //Chama um callBack personalizado do dono
        object.onLiquidExit();

        //Caso tenhamos removido o último liquido
        if (liquidBuffer.isEmpty()) {
            //Marcamos que não estamos dentro de um liquido
            inLiquid = false;
            //Resetamos os valores de movimentação em terra
            restartOriginalMovementValues();

            /*
             *  Resetamos a movimentação no eixo y, para garantir que não aja uma movimentação exagerada,
             *   após sairmos de liquidos
             */
            moveC.resetYMovement();
            //Resetamos o buffer de simulação de flutuabilidade
            totalBoyancyEffect = 0;

            return;
        }

        //Só tentamos recalcular caso ainda haja liquidos
        needsRecalculation = true;
    }

    /**
     * Armazena valores originais do MovementComponent.
     * <p>>
     * Chamado na primeira entrada de um líquido.
     */
    private void storeOriginalMovementValues() {

        //Armazena as velocidades máximas de cada eixo
        this.originalYMaxSpeed = moveC.yMaxSpeed;
        this.originalXMaxSpeed = moveC.xMaxSpeed;
        this.originalRMaxSpeed = moveC.rMaxSpeed;

        //Armazena as desacelerações de cada eixo
        this.originalXDeceleration = moveC.xDeceleration;
        this.originalYDeceleration = moveC.yDeceleration;
        this.originalRDeceleration = moveC.rDeceleration;

        //Armazena as flags e valores relacionadas à gravidade
        this.originalGravityScale = moveC.gravityScale;
        this.originallyGravityAffected = moveC.gravityAffected;

        //Armazena a resistência-peso original de cada eixo
        this.originalXResistanceWeight = moveC.xResistanceWeight;
        this.originalYResistanceWeight = moveC.yResistanceWeight;
        this.originalRResistanceWeight = moveC.rResistanceWeight;
    }

    /**
     * Restaura os valores de movimentação original no MovementComponent.
     * <p>
     * Chamado para restaurar a movimentação fora de liquido
     */
    private void restartOriginalMovementValues() {

        //Restaura a velocidade máxima de cada eixo
        this.moveC.yMaxSpeed = originalYMaxSpeed;
        this.moveC.xMaxSpeed = originalXMaxSpeed;
        this.moveC.rMaxSpeed = originalRMaxSpeed;

        //Restaura a desaceleração de cada eixo
        this.moveC.xDeceleration = originalXDeceleration;
        this.moveC.yDeceleration = originalYDeceleration;
        this.moveC.rDeceleration = originalRDeceleration;

        //Restaura as flags e fatores de gravidade
        this.moveC.gravityScale = originalGravityScale;
        this.moveC.gravityAffected = originallyGravityAffected;

        //Restaura os valores de resistência-peso em cada um dos eixos
        this.moveC.xResistanceWeight = originalXResistanceWeight;
        this.moveC.yResistanceWeight = originalYResistanceWeight;
        this.moveC.rResistanceWeight = originalRResistanceWeight;
    }

    /**
     * Calcula os efeitos dos líquidos no buffer.
     * Chamado quando needsRecalculation é true.
     */
    private void recalculateLiquidEffects() {
        //Ignoramos caso não estejamos em liquido ou o buffer não esteja com dados
        if (!inLiquid || liquidBuffer.isEmpty()) {
            needsRecalculation = false;
            return;
        }

        //Buffers temporários
        LiquidData
            tmpCurrentLiquid,              //Liquido atual
            tmpCurrentLiquidByDensity,     //Liquido por densidade
            tmpCurrentLiquidByResistance;  //Liquido por resistencia

        // Damos prioridade ao primeiro liquido
        tmpCurrentLiquidByDensity = tmpCurrentLiquidByResistance = liquidBuffer.get(0);

        //Se o buffer for maior que 1 iteramos, se não evitamos de entrar no loop
        if (liquidBuffer.size() > 1) {
            //Percorre o buffer
            for (int i = 0; i < liquidBuffer.size(); i++) {
                tmpCurrentLiquid = liquidBuffer.get(i);

                //Tenta atualizar o liquido prioritário de densidade
                if (tmpCurrentLiquid.density > tmpCurrentLiquidByDensity.density) {
                    tmpCurrentLiquidByDensity = tmpCurrentLiquid;
                }

                //Tenta atualizar o liquido prioritário de resistência
                if (tmpCurrentLiquid.resistance > tmpCurrentLiquidByResistance.resistance) {
                    tmpCurrentLiquidByResistance = tmpCurrentLiquid;
                }

            }

        }

        // Calcula boyancy (usa densidade maior)
        calculateBoyancy(tmpCurrentLiquidByDensity);

        // Calcula resistência (usa resistência maior)
        calculateResistance(tmpCurrentLiquidByResistance);

        // Calcula limites de velocidade (usa densidade maior)
        calculateSpeedLimits(tmpCurrentLiquidByDensity);

        needsRecalculation = false;
    }

    /// Calcula a flutuabilidade a ser aplicada
    private void calculateBoyancy(LiquidData data) {
        // Densidade do objeto = massa / volume
        float objectDensity = (volume > 0) ? mass / volume : Float.MAX_VALUE;

        // Diferença de densidade determina direção e força
        // Positivo = sobe, Negativo = desce
        boyancyFactor = (data.density - objectDensity) * volume;
    }

    /// Calcula os dados para simulação de resistencia de liquidos
    private void calculateResistance(LiquidData data) {
        float resistance = data.resistance * resistanceMultiplier;

        // Inércia: dificulta acelerar e parar
        moveC.xResistanceWeight = resistance;
        moveC.yResistanceWeight = resistance;
        moveC.rResistanceWeight = resistance;

        // Resistência ativa do líquido: aumenta a desaceleração base
        moveC.xDeceleration = originalXDeceleration + resistance;
        moveC.yDeceleration = originalYDeceleration + resistance;
        moveC.rDeceleration = originalRDeceleration + resistance;
    }

    /// Calcula as velocidades máximas
    private void calculateSpeedLimits(LiquidData data) {
        moveC.xMaxSpeed = Math.min(originalXMaxSpeed, data.maxMoveSpeed);
        moveC.rMaxSpeed = Math.min(originalRMaxSpeed, data.maxMoveSpeed);
        moveC.yMaxSpeed = Math.min(originalYMaxSpeed, data.maxSinkSpeed);
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

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
