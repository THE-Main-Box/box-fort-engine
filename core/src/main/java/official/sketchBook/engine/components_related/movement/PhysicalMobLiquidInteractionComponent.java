package official.sketchBook.engine.components_related.movement;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.LiquidInteractableObjectII;
import official.sketchBook.engine.liquid_related.model.LiquidData;

import java.util.ArrayList;
import java.util.List;

/// Aplicar após a atualização do componente de movimentação
public class PhysicalMobLiquidInteractionComponent implements Component {
    /// Componente de movimentação
    private MovementComponent moveC;

    /// Buffer de liquidos, irá determinar os liquidos que precisaremos iterar
    private List<LiquidData> liquidBuffer = new ArrayList<>();

    /// flag de estado auxiliar
    private boolean
        inLiquid,
        needsRecalculation;

    /// Valores de correspondência a dados de movimentação
    private float
        mass,                               //Fator de massa do objeto
        volume,                             //Fator de volume
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
        this.moveC = object.getMoveC();
    }

    @Override
    public void update(float delta) {
        if (!inLiquid) return;

        if (needsRecalculation) {
            recalculateLiquidEffects();
        }

        applyLiquidEffects();
    }

    @Override
    public void postUpdate() {

    }

    private void applyLiquidEffects() {
        moveC.yAccel += boyancyFactor + boyancyModifier;
    }

    /**
     * Adiciona um líquido ao buffer.
     */
    public void addLiquid(LiquidData liquid) {
        if (liquid == null) return;

        // Não adiciona duplicado
        for (LiquidData existing : liquidBuffer) {
            if (existing.id == liquid.id) {
                return;
            }
        }

        liquidBuffer.add(liquid);

        storeOriginalMovementValues();

        inLiquid = true;
        needsRecalculation = true;
    }

    /**
     * Remove um líquido do buffer.
     */
    public void removeLiquid(LiquidData liquid) {
        if (liquid == null) return;

        liquidBuffer.removeIf(
            l -> l.id == liquid.id
        );

        if (liquidBuffer.isEmpty()) {
            inLiquid = false;
        }
        needsRecalculation = true;

        restartOriginalMovementValues();
    }

    /**
     * Armazena valores originais do MovementComponent.
     * Chamado na primeira entrada em um líquido.
     */
    private void storeOriginalMovementValues() {
        //Se não for o primeiro liquido ignoramos
        if (liquidBuffer.size() > 1) return;

        this.originalYMaxSpeed = moveC.yMaxSpeed;
        this.originalXMaxSpeed = moveC.xMaxSpeed;
        this.originalRMaxSpeed = moveC.rMaxSpeed;

        this.originalXDeceleration = moveC.xDeceleration;
        this.originalYDeceleration = moveC.yDeceleration;
        this.originalRDeceleration = moveC.rDeceleration;

        this.originalGravityScale = moveC.gravityScale;
        this.originallyGravityAffected = moveC.gravityAffected;

        this.originalXResistanceWeight = moveC.xResistanceWeight;
        this.originalYResistanceWeight = moveC.yResistanceWeight;
        this.originalRResistanceWeight = moveC.rResistanceWeight;

    }

    private void restartOriginalMovementValues() {
        if (!(liquidBuffer.isEmpty())) return;

        this.moveC.yMaxSpeed = originalYMaxSpeed;
        this.moveC.xMaxSpeed = originalXMaxSpeed;
        this.moveC.rMaxSpeed = originalRMaxSpeed;

        this.moveC.xDeceleration = originalXDeceleration;
        this.moveC.yDeceleration = originalYDeceleration;
        this.moveC.rDeceleration = originalRDeceleration;

        this.moveC.gravityScale = originalGravityScale;
        this.moveC.gravityAffected = originallyGravityAffected;

        this.moveC.xResistanceWeight = originalXResistanceWeight;
        this.moveC.yResistanceWeight = originalYResistanceWeight;
        this.moveC.rResistanceWeight = originalRResistanceWeight;
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

        //Buffers temporários
        LiquidData
            currentLiquid,              //Liquido atual
            currentLiquidByDensity,     //Liquido por densidade
            currentLiquidByResistance;  //Liquido por resistencia

        // Damos prioridade ao primeiro liquido
        currentLiquidByDensity = currentLiquidByResistance = liquidBuffer.get(0);

        //Se o buffer for maior que 1 iteramos, se não evitamos de entrar no loop
        if (liquidBuffer.size() > 1) {
            //Percorre o buffer
            for (int i = 0; i < liquidBuffer.size(); i++) {
                currentLiquid = liquidBuffer.get(i);

                //Tenta atualizar o liquido prioritário de densidade
                if (currentLiquid.density > currentLiquidByDensity.density) {
                    currentLiquidByDensity = currentLiquid;
                }

                //Tenta atualizar o liquido prioritário de resistência
                if (currentLiquid.resistance > currentLiquidByResistance.resistance) {
                    currentLiquidByResistance = currentLiquid;
                }

            }

        }

        // Calcula boyancy (usa densidade maior)
        calculateBoyancy(currentLiquidByDensity);

        // Calcula resistência (usa resistência maior)
        calculateResistance(currentLiquidByResistance);

        // Calcula limites de velocidade (usa densidade maior)
        calculateSpeedLimits(currentLiquidByDensity);

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

    public void setResistanceMultiplier(float resistanceMultiplier) {
        this.resistanceMultiplier = resistanceMultiplier;
        needsRecalculation = true;
    }

    public void setNeedsRecalculation(boolean needsRecalculation) {
        this.needsRecalculation = needsRecalculation;
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
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
