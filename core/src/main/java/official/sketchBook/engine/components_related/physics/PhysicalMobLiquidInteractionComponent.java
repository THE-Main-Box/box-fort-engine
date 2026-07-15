package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.IntSet;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.SimpleLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.MovementDataComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.liquid_related.model.LiquidData;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.BOYANCY_THRESHOLD;

/// Simula interação física com líquidos — aplica flutuabilidade, resistência e limites de velocidade.
/// Deve ser atualizado após o componente de movimentação.
public class PhysicalMobLiquidInteractionComponent implements Component {

    /// Refer�ncia ao objeto dono
    private SimpleLiquidInteractableObjectII object;

    /// Componente de movimenta��o do objeto dono
    private MovementComponent moveC;

    /// L�quidos atualmente em contato com o objeto
    private List<LiquidData> liquidBuffer = new ArrayList<>();

    /// Set de ids para O(1) na verifica��o de duplicatas — sem autoboxing
    private IntSet liquidIdSet = new IntSet();

    /// Contador de fixtures em contato por l�quido — sem autoboxing
    private IntIntMap liquidContactCount = new IntIntMap();

    // --- Flags de estado ---

    private boolean
        canInteractBuffer = false,      // Buffer do canInteract — restaurado ap�s armazenar movimento
        canInteract = true,             // Se pode simular intera��o com l�quidos
        neutralBuoyancy = false,        // Se deve ignorar flutuabilidade (ex: jogador em modo neutro)
        inLiquid = false,               // Se est� fisicamente dentro de um l�quido
        originalValuesStored = false,   // Se os valores originais de movimenta��o foram armazenados
        updateStoredMovement = true,    // Se deve rearmazenar os valores de movimenta��o
        needsRecalculation = true,      // Se precisa recalcular efeitos do l�quido
        constraintsDirty = true;        // Se as constraints precisam ser reaplicadas no moveC

    // --- Dados f�sicos do objeto ---

    private float
        mass,                           // Massa do objeto
        volume;                         // Volume do objeto

    private float
        floatEffectValue,               // For�a de flutuabilidade calculada
        floatEffectValueModifier;       // Modificador externo de flutuabilidade

    private float
        resistanceMultiplier = 1.0f,    // Multiplicador de resist�ncia ao movimento
        cachedSubmersionFraction = 1f,  // Fra��o de submers�o cacheada — evita rec�lculo todo frame
        lastCenterY = Float.MAX_VALUE;  // Dirty data de posição y

    private static final float SUBMERSION_RECALC_THRESHOLD = 0.5f; // pixels

    /// Dados de movimenta��o calculados para o l�quido atual (resist�ncia, limites, gravidade)
    private final MovementDataComponent intermediary;

    /// Snapshot dos dados de movimenta��o originais — restaurados ao sair do l�quido
    private final MovementDataComponent storedMovementData;

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
        if (!updateStoredMovement) return;
        storeCurrentMovementValues();
        canInteract = canInteractBuffer;
        updateStoredMovement = false;
    }

    @Override
    public void update(float delta) {
        updateSimulation(delta);
    }

    @Override
    public void postUpdate() {
    }

    @Override
    public void initObject() {
    }

    private void calculateFloatEffect(
        LiquidData data,
        float deltaTime
    ) {

        float objectDensity =
            (volume > 0)
                ? mass / volume
                : Float.MAX_VALUE;


        float targetFloatEffect =
            (data.density - objectDensity)
                * volume;

        floatEffectValue += targetFloatEffect * deltaTime;


        floatEffectValue = MathUtils.clamp(

            floatEffectValue,

            -Math.abs(targetFloatEffect),

            Math.abs(targetFloatEffect)

        );

    }

    private void updateSimulation(float delta) {
        //Pode simular não é só a capacidade de interagir com liquido, mas também se temos contato com liquidos
        final boolean shouldSimulate = canInteract && !liquidBuffer.isEmpty();
        applyChange(shouldSimulate);
        applyPhysics(shouldSimulate, delta);
        updateStoredMovement();
    }

    /// Executa a simula��o f�sica do l�quido quando aplic�vel
    private void applyPhysics(
        boolean shouldSimulate,
        float deltaTime
    ) {

        if (!shouldSimulate)
            return;

        updateSubmersionFraction();


        if (cachedSubmersionFraction <= 0f) {

            resetFlotation();

            return;

        }


        if (needsRecalculation) {

            recalculateLiquidEffects(deltaTime);

            needsRecalculation = false;
            constraintsDirty = true;

        }


        calculateFloatEffect(
            liquidBuffer.get(0),
            deltaTime
        );


        applyConstraints();

        applyBoyancy();

    }

    /// Aplica resist�ncia, limites de velocidade e gravidade do intermediary no moveC.
    /// S� executa quando as constraints foram marcadas como sujas — evita escritas desnecess�rias todo frame.
    private void applyConstraints() {
        if (!constraintsDirty) return;

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

        constraintsDirty = false;
    }

    /// Aplica flutuabilidade proporcional � fra��o submersa do objeto.
    /// Adiciona resist�ncia extra perto da superf�cie para evitar saltos.
    private void applyBoyancy() {
        if (neutralBuoyancy) {
            resetFlotation();
            return;
        }

        if (cachedSubmersionFraction <= 0f) return;

        /// Resist�ncia extra pr�ximo da superf�cie
        float surfaceResistance = (1f - cachedSubmersionFraction) * intermediary.yAxis.deceleration;
        moveC.dataComponent.yAxis.deceleration = intermediary.yAxis.deceleration + surfaceResistance;

        /// Flutuabilidade proporcional � submers�o atual
        float targetFloatation = MathUtils.clamp(
            floatEffectValue * cachedSubmersionFraction,
            -moveC.dataComponent.yAxis.maxMoveVel,
            moveC.dataComponent.yAxis.maxMoveVel
        );

        if (Math.abs(targetFloatation) < BOYANCY_THRESHOLD) return;

        moveC.dataComponent.yAxis.setMovement(targetFloatation);
    }

    /// Atualiza o cache da fra��o submersa.
    /// Calcula a fra��o do objeto que est� submersa no l�quido mais denso do buffer.
    /// Retorna 1 se totalmente submerso, 0 se totalmente fora, fra��o entre 0-1 se parcial.
    private void updateSubmersionFraction() {
        TransformComponent t = object.getTransformC();
        if (t == null || liquidBuffer.isEmpty()) {
            cachedSubmersionFraction = 1f;
            return;
        }

        float centerY = t.getCenterY();

        // Só recalcula se o objeto se moveu o suficiente verticalmente
        if (Math.abs(centerY - lastCenterY) < SUBMERSION_RECALC_THRESHOLD) return;

        lastCenterY = centerY;

        float surfaceY = liquidBuffer.get(0).surfaceY;
        float objectBottomY = centerY - t.getHalfHeight();
        float objectTopY = centerY + t.getHalfHeight();

        if (objectTopY <= surfaceY) {
            cachedSubmersionFraction = 1f;
            return;
        }
        if (objectBottomY >= surfaceY) {
            cachedSubmersionFraction = 0f;
            return;
        }

        cachedSubmersionFraction = MathUtils.clamp(
            (surfaceY - objectBottomY) / t.height, 0f, 1f
        );
    }

    /// Gerencia transi��es de entrada e sa�da do l�quido.
    /// Ao entrar: notifica o objeto. Ao sair: restaura movimenta��o e reseta flutuabilidade.
    private void applyChange(boolean shouldSimulate) {
        //Se podemos simular e não estávamos em liquido
        if (shouldSimulate && !inLiquid) {
            //Atualiza a flag de liquido
            inLiquid = true;
            //Chama para lidar com a entrada no liquido
            object.onLiquidEnter();

            //Caso não possamos interagir e estejamos em liquido
        } else if (!shouldSimulate && inLiquid) {
            //Marcamos a flag para false
            inLiquid = false;

            //Restauramos a movimentação antes de entrar nos liquidos
            restartStoredMovementValues();
            //Resetamos os dados de flutuabilidade ao sair de um liquido
            resetFlotation();

            //Resetamos a aceleração e velocidade para impedir uma aceleração infinita
            moveC.dataComponent.yAxis.resetMovement();
            //Chama o callback para lidar com a saída de um liquido
            object.onLiquidExit();
        }
    }

    // --- Liquid buffer ---

    /// Registra contato com um l�quido. S� adiciona ao buffer na primeira fixture em contato.
    public void addLiquid(LiquidData liquid) {
        if (liquid == null) return;
        int count = liquidContactCount.get(liquid.id, 0);
        liquidContactCount.put(liquid.id, count + 1);
        if (count == 0 && liquidIdSet.add(liquid.id)) {
            liquidBuffer.add(liquid);
            needsRecalculation = true;
        }
    }

    /// Remove contato com um l�quido. S� remove do buffer quando a �ltima fixture sai.
    public void removeLiquid(LiquidData liquid) {
        if (liquid == null) return;
        int count = liquidContactCount.get(liquid.id, 0);
        if (count <= 0) return;
        if (count == 1) {
            liquidContactCount.remove(liquid.id, 0);
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
            liquidContactCount.put(liquid.id, count - 1);
        }
    }

    // --- Movement data ---

    /// Armazena snapshot dos valores atuais de movimenta��o para restaurar ao sair do l�quido
    private void storeCurrentMovementValues() {
        if (originalValuesStored) return;
        this.storedMovementData.set(moveC.dataComponent);
        originalValuesStored = true;
    }

    /// Restaura os valores de movimenta��o originais armazenados antes de entrar no l�quido
    private void restartStoredMovementValues() {
        if (!originalValuesStored) return;
        this.moveC.dataComponent.set(storedMovementData);
        constraintsDirty = true;    // For�a reaplicar constraints ap�s restaurar
    }

    /// Prepara o intermediary como c�pia limpa dos dados originais antes do rec�lculo
    private void prepareIntermediary() {
        intermediary.set(storedMovementData);
    }

    // --- Recalculation ---

    /// Recalcula todos os efeitos do l�quido no intermediary.
    /// Usa o l�quido mais denso para flutuabilidade e limites, o de maior resist�ncia para arrasto.
    private void recalculateLiquidEffects(float delta) {
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
        calculateFloatEffect(
            densLiq,
            delta
        );
        calculateResistance(resLiq);
        calculateSpeedLimits(densLiq);
    }

    /// Calcula resist�ncia ao movimento e in�rcia com base no l�quido mais resistente
    private void calculateResistance(LiquidData data) {
        float resistance = data.resistance * resistanceMultiplier;
        intermediary.xAxis.weightFactor = resistance;
        intermediary.yAxis.weightFactor = resistance;
        intermediary.rAxis.weightFactor = resistance;
        intermediary.xAxis.deceleration = resistance + storedMovementData.xAxis.deceleration;
        intermediary.yAxis.deceleration = resistance + storedMovementData.yAxis.deceleration;
        intermediary.rAxis.deceleration = resistance + storedMovementData.rAxis.deceleration;
    }

    /// Limita velocidades m�ximas de acordo com as propriedades do l�quido
    private void calculateSpeedLimits(LiquidData data) {
        intermediary.xAxis.maxMoveVel = Math.min(storedMovementData.xAxis.maxMoveVel, data.maxMoveSpeed);
        intermediary.yAxis.maxMoveVel = Math.min(storedMovementData.yAxis.maxMoveVel, data.maxSinkSpeed);
        intermediary.rAxis.maxMoveVel = Math.min(storedMovementData.rAxis.maxMoveVel, data.maxMoveSpeed);
    }

    // --- Getters / Setters ---

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

    public float getResistanceMultiplier() {
        return resistanceMultiplier;
    }

    public boolean isInLiquid() {
        return inLiquid;
    }

    public List<LiquidData> getLiquidBuffer() {
        return liquidBuffer;
    }

    public void setMass(float mass) {
        this.mass = mass;
        needsRecalculation = true;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        needsRecalculation = true;
    }

    public void setFloatingEffectModifier(float v) {
        this.floatEffectValueModifier = v;
        needsRecalculation = true;
    }

    public void setNeutralFloating(boolean neutral) {
        this.neutralBuoyancy = neutral;
        needsRecalculation = true;
    }

    public void setResistanceMultiplier(float v) {
        this.resistanceMultiplier = v;
        needsRecalculation = true;
    }

    /// Marca para rearmazenar valores de movimenta��o na pr�xima atualiza��o.
    /// Usado quando as constraints de movimenta��o mudam externamente.
    public void updateCurrentStoredMovementValues() {
        this.updateStoredMovement = true;
        this.canInteractBuffer = this.canInteract;
        this.canInteract = false;
        this.originalValuesStored = false;
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
