package official.sketchBook.engine.components_related.movement;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.liquid_related.model.LiquidData;

import java.util.ArrayList;
import java.util.List;

public class PhysicalMobLiquidInteractionComponent implements Component {

    private MovementComponent moveC;

    /// Buffer de liquidos, irá determinar os liquidos que precisaremos iterar
    private List<LiquidData> liquidBuffer = new ArrayList<>();

    /// flag de estado auxiliar
    private boolean
        inLiquid,
        needsRecalculation;

    /// Valores de correspondência a dados de movimentação
    public float
        buoyancyFactor = 0,                 // O quanto iremos responder à flutuabilidade
        resistanceMultiplier = 1.0f;        // O quanto iremos responder à resistência de movimentação do liquido

    /// Valores originais de movimentação ao entrar no primeiro líquido
    private float
        originalGravityScale,           //Constante de escala de gravidade original
        originalXMaxSpeed,              //Constante de velocidade máxima no eixo x
        originalYMaxSpeed,              //Constante de velocidade máxima no eixo y
        originalRMaxSpeed,              //Constante de velocidade máxima de rotação
        originalXDeceleration,        //Constante de desaceleração no eixo x
        originalYDeceleration,        //Constante de desaceleração no eixo y
        originalRDeceleration;          //Constante de desaceleração de rotação

    private boolean originalyGravityAffected;

    private boolean disposed = false;

    @Override
    public void update(float delta) {

    }

    @Override
    public void postUpdate() {

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
        if (!(liquidBuffer.size() == 1)) return;

        this.originalYMaxSpeed = moveC.yMaxSpeed;
        this.originalXMaxSpeed = moveC.xMaxSpeed;
        this.originalRMaxSpeed = moveC.rMaxSpeed;

        this.originalXDeceleration = moveC.xDeceleration;
        this.originalYDeceleration = moveC.yDeceleration;
        this.originalRDeceleration = moveC.rDeceleration;

        this.originalGravityScale = moveC.gravityScale;
        this.originalyGravityAffected = moveC.gravityAffected;

    }

    private void restartOriginalMovementValues() {
        if (!(liquidBuffer.isEmpty())) return;

        moveC.yMaxSpeed = originalYMaxSpeed;
        moveC.xMaxSpeed = originalXMaxSpeed;
        moveC.rMaxSpeed = originalRMaxSpeed;

        moveC.xDeceleration = originalXDeceleration;
        moveC.yDeceleration = originalYDeceleration;
        moveC.rDeceleration = originalRDeceleration;

        moveC.gravityScale = originalGravityScale;
        moveC.gravityAffected = originalyGravityAffected;
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
            for (int i = 0; i < liquidBuffer.size(); i++) {
                currentLiquid = liquidBuffer.get(i);

                if (currentLiquid.density > currentLiquidByDensity.density) {
                    currentLiquidByDensity = currentLiquid;
                }
                if (currentLiquid.resistance > currentLiquidByResistance.resistance) {
                    currentLiquidByResistance = currentLiquid;
                }
            }
        }

        // Calcula buoyancy (usa densidade maior)
        calculateBuoyancy(currentLiquidByDensity);

        // Calcula resistência (usa resistência maior)
        calculateResistance(currentLiquidByResistance);

        // Calcula limites de velocidade (usa densidade maior)
        calculateSpeedLimits(currentLiquidByDensity);

        needsRecalculation = false;
    }

    private void calculateBuoyancy(LiquidData data) {
    }

    private void calculateResistance(LiquidData data) {
    }

    private void calculateSpeedLimits(LiquidData data) {
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
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
