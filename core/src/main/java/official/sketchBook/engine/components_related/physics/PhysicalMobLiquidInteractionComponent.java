package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.MathUtils;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.SimpleLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.MovementDataComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.liquid_related.model.LiquidData;

import java.util.*;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.BOYANCY_THRESHOLD;

/// Simula interação física com líquidos — aplica flutuabilidade, resistência e limites de velocidade.
/// Deve ser atualizado após o componente de movimentação.
public class PhysicalMobLiquidInteractionComponent implements Component {

    /// Referência ao objeto dono
    private SimpleLiquidInteractableObjectII object;

    /// Componente de movimentação do objeto dono
    private MovementComponent moveC;

    /// Líquidos atualmente em contato com o objeto
    private List<LiquidData> liquidBuffer = new ArrayList<>();

    /// Set de ids para O(1) na verificação de duplicatas
    private Set<Integer> liquidIdSet = new HashSet<>();

    /// Contador de fixtures em contato por líquido — evita remoção prematura em objetos com múltiplas fixtures
    private Map<Integer, Integer> liquidContactCount = new HashMap<>();

    // --- Flags de estado ---

    private boolean
        canInteractBuffer = false,      // Buffer do canInteract — restaurado após armazenar movimento
        canInteract = true,             // Se pode simular interação com líquidos
        neutralBuoyancy = false,        // Se deve ignorar flutuabilidade (ex: jogador em modo neutro)
        inLiquid = false,               // Se está fisicamente dentro de um líquido
        originalValuesStored = false,   // Se os valores originais de movimentação foram armazenados
        updateStoredMovement = true,    // Se deve rearmazenar os valores de movimentação
        needsRecalculation = true;      // Se precisa recalcular efeitos do líquido

    // --- Dados físicos do objeto ---

    private float
        mass,                           // Massa do objeto
        volume,                         // Volume do objeto
        floatEffectValue,                  // Força de flutuabilidade calculada
        floatEffectValueModifier,          // Modificador externo de flutuabilidade
        resistanceMultiplier = 1.0f;    // Multiplicador de resistência ao movimento

    /// Dados de movimentação calculados para o líquido atual (resistência, limites, gravidade)
    private final MovementDataComponent intermediary;

    /// Snapshot dos dados de movimentação originais — restaurados ao sair do líquido
    private final MovementDataComponent storedMovementData;

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

    /// Executa a simulação física do líquido quando aplicável
    private void applyPhysics(boolean shouldSimulate) {
        if (!shouldSimulate) return;

        // Recalcula efeitos apenas quando o estado do líquido mudou
        if (needsRecalculation) {
            recalculateLiquidEffects();
            needsRecalculation = false;
        }

        // Aplica constraints de resistência e limites de velocidade
        applyConstraints();

        // Aplica força de flutuabilidade
        applyBoyancy();

        object.inLiquidUpdate();
    }

    /// Aplica resistência, limites de velocidade e gravidade do intermediary no moveC.
    /// Não toca na velocidade atual — apenas nas constraints.
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

    /// Aplica flutuabilidade proporcional à fração submersa do objeto.
    /// Adiciona resistência extra perto da superfície para evitar saltos.
    private void applyBoyancy() {
        if (neutralBuoyancy) {
            resetFlotation();
            return;
        }

        float submersionFraction = calculateSubmersionFraction();
        if (submersionFraction <= 0f) return;

        // Aumenta desaceleração no eixo Y conforme o objeto se aproxima da superfície
        float surfaceResistance = (1f - submersionFraction) * intermediary.yAxis.deceleration;
        moveC.dataComponent.yAxis.deceleration = intermediary.yAxis.deceleration + surfaceResistance;

        float currentBoyancy = MathUtils.clamp(
            (floatEffectValue + floatEffectValueModifier) * submersionFraction,
            -moveC.dataComponent.yAxis.maxMoveVel,
            moveC.dataComponent.yAxis.maxMoveVel
        );

        if (Math.abs(currentBoyancy) < BOYANCY_THRESHOLD) return;

        moveC.dataComponent.yAxis.setMovement(
            moveC.dataComponent.yAxis.velocity + currentBoyancy
        );
    }

    /// Calcula a fração do objeto que está submersa no líquido mais denso do buffer.
    /// Retorna 1 se totalmente submerso, 0 se totalmente fora, fração entre 0-1 se parcial.
    private float calculateSubmersionFraction() {
        TransformComponent t = object.getTransformC();
        if (t == null || liquidBuffer.isEmpty()) return 1f;

        float surfaceY = liquidBuffer.get(0).surfaceY;
        float objectBottomY = t.y;
        float objectTopY = t.y + t.height;

        if (objectTopY <= surfaceY) return 1f;
        if (objectBottomY >= surfaceY) return 0f;

        return MathUtils.clamp(
            (surfaceY - objectBottomY) / t.height,
            0f,
            1f
        );
    }

    /// Gerencia transições de entrada e saída do líquido.
    /// Ao entrar: notifica o objeto. Ao sair: restaura movimentação e reseta flutuabilidade.
    private void applyChange(boolean shouldSimulate) {
        if (shouldSimulate && !inLiquid) {
            inLiquid = true;
            object.onLiquidEnter();
        } else if (!shouldSimulate && inLiquid) {
            inLiquid = false;
            restartStoredMovementValues();
            moveC.dataComponent.yAxis.resetMovement();
            resetFlotation();
            object.onLiquidExit();
        }
    }

    // --- Liquid buffer ---

    /// Registra contato com um líquido. Só adiciona ao buffer na primeira fixture em contato.
    public void addLiquid(LiquidData liquid) {
        if (liquid == null) return;
        int count = liquidContactCount.getOrDefault(liquid.id, 0);
        liquidContactCount.put(liquid.id, count + 1);
        if (count == 0) {
            if (liquidIdSet.add(liquid.id)) {
                liquidBuffer.add(liquid);
                needsRecalculation = true;
            }
        }
    }

    /// Remove contato com um líquido. Só remove do buffer quando a última fixture sai.
    public void removeLiquid(LiquidData liquid) {
        if (liquid == null) return;
        if (!liquidContactCount.containsKey(liquid.id)) return;
        int count = liquidContactCount.get(liquid.id) - 1;
        if (count <= 0) {
            liquidContactCount.remove(liquid.id);
            if (liquidIdSet.remove(liquid.id)) {
                for (int i = liquidBuffer.size() - 1; i >= 0; i--) {
                    if (liquidBuffer.get(i).id == liquid.id) {
                        liquidBuffer.remove(i);
                        needsRecalculation = true;
                        return;
                    }
                }
            }
        } else {
            liquidContactCount.put(liquid.id, count);
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
    }

    /// Prepara o intermediary como cópia limpa dos dados originais antes do recálculo
    private void prepareIntermediary() {
        intermediary.set(storedMovementData);
    }

    // --- Recalculation ---

    /// Recalcula todos os efeitos do líquido no intermediary.
    /// Usa o líquido mais denso para flutuabilidade e limites, o de maior resistência para arrasto.
    private void recalculateLiquidEffects() {
        LiquidData
            curLiq,
            densLiq = liquidBuffer.get(0),
            resLiq = densLiq;

        for (int i = 1; i < liquidBuffer.size(); i++) {
            curLiq = liquidBuffer.get(i);
            if (curLiq.density > densLiq.density) densLiq = curLiq;
            if (curLiq.resistance > resLiq.resistance) resLiq = curLiq;
        }

        prepareIntermediary();
        calculateFloatEffect(densLiq);
        calculateResistance(resLiq);
        calculateSpeedLimits(densLiq);
    }

    /// Calcula resistência ao movimento e inércia com base no líquido mais resistente
    private void calculateResistance(LiquidData data) {
        float resistance = data.resistance * resistanceMultiplier;
        intermediary.xAxis.weightFactor = resistance;
        intermediary.yAxis.weightFactor = resistance;
        intermediary.rAxis.weightFactor = resistance;
        intermediary.xAxis.deceleration = resistance + storedMovementData.xAxis.deceleration;
        intermediary.yAxis.deceleration = resistance + storedMovementData.yAxis.deceleration;
        intermediary.rAxis.deceleration = resistance + storedMovementData.rAxis.deceleration;
    }

    /// Limita velocidades máximas de acordo com as propriedades do líquido
    private void calculateSpeedLimits(LiquidData data) {
        intermediary.xAxis.maxMoveVel = Math.min(storedMovementData.xAxis.maxMoveVel, data.maxMoveSpeed);
        intermediary.yAxis.maxMoveVel = Math.min(storedMovementData.yAxis.maxMoveVel, data.maxSinkSpeed);
        intermediary.rAxis.maxMoveVel = Math.min(storedMovementData.rAxis.maxMoveVel, data.maxMoveSpeed);
    }

    /// Calcula força de flutuabilidade com base na diferença de densidade entre objeto e líquido
    private void calculateFloatEffect(LiquidData data) {
        float objectDensity = (volume > 0) ? mass / volume : Float.MAX_VALUE;
        floatEffectValue = (data.density - objectDensity) * volume;
    }

    // --- Getters / Setters ---

    public boolean isCanInteract() { return canInteract; }

    public void setCanInteract(boolean canInteract) {
        if (this.canInteract == canInteract) return;
        this.canInteract = canInteract;
    }

    public float getMass() { return mass; }
    public float getVolume() { return volume; }
    public float getFloatEffect() { return floatEffectValue; }
    public float getFloatEffectValueModifier() { return floatEffectValueModifier; }
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

    public void setFloatingEffectModifier(float boyancyEffectModifier) {
        this.floatEffectValueModifier = boyancyEffectModifier;
        needsRecalculation = true;
    }

    public void setNeutralFloating(boolean neutral) {
        this.neutralBuoyancy = neutral;
        needsRecalculation = true;
    }

    public void setResistanceMultiplier(float resistanceMultiplier) {
        this.resistanceMultiplier = resistanceMultiplier;
        needsRecalculation = true;
    }

    /// Marca para rearmazenar valores de movimentação na próxima atualização.
    /// Usado quando as constraints de movimentação mudam externamente.
    public void updateCurrentStoredMovementValues() {
        this.updateStoredMovement = true;
        this.canInteractBuffer = this.canInteract;
        this.canInteract = false;
        this.originalValuesStored = false;
    }

    public void resetFlotation() {
        floatEffectValue = 0;
    }

    // --- Dispose ---

    @Override
    public void dispose() {
        if (disposed) return;
        liquidBuffer.clear();
        liquidIdSet.clear();
        liquidContactCount.clear();
        nullifyReferences();
        disposed = true;
    }

    public void nullifyReferences() {
        liquidBuffer = null;
        liquidIdSet = null;
        liquidContactCount = null;
        moveC = null;
        object = null;
    }
}
