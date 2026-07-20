package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.SimpleLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.MovementDataComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.toPixels;

/// Simula interação física com líquidos — aplica flutuabilidade, resistência, limites de
/// velocidade e torque de estabilidade rotacional.
/// Deve ser atualizado após o componente de movimentação.
public class PhysicalMobLiquidInteractionComponent implements Component {

    /// Referência ao objeto dono
    private SimpleLiquidInteractableObjectII object;

    /// Componente de movimentação do objeto dono
    private MovementComponent moveC;

    /// Mapa para identificação de liquidos -> regiões de contato (fixtures)
    private final IdentityHashMap<LiquidData, ArrayList<LiquidRegion>> liquidAndRegionMap = new IdentityHashMap<>();

    private LiquidData
        highestDensityLiquidBuffer,
        highestDragLiquidBuffer;

    /// Região usada apenas para saber o quanto o objeto está submerso (surfaceY), não para densidade/drag
    private LiquidRegion currentLiquidRegionBuffer;

    // --- Flags de estado ---

    private boolean
        canInteractBuffer = false,      // Buffer do canInteract — restaurado após armazenar movimento
        canInteract = true,             // Se pode simular interação com líquidos
        inLiquid = false;               // Se está fisicamente dentro de um líquido

    private boolean
        originalValuesStored = false,        // Se os valores originais de movimentação foram armazenados
        needsUpdateStoredMovement = true;    // Se deve rearmazenar os valores de movimentação

    private boolean
        needsUpdateCurrentLiquidData = true, // Se precisa atualizar os dados de líquido atual (mais denso / maior drag)
        needsUpdateCurrentRegion = true,     // Se precisa atualizar a região atual (submersão)
        needsUpdatePhysicsData = true,       // Se precisa recalcular objectDensity (mass/volume mudaram)
        needsUpdateMovement = true,          // Se precisamos atualizar os dados de efeito de movimentação em líquido
        isConstraintsDirty = true;           // Se as constraints precisam ser reaplicadas no moveC

    // --- Dados físicos do objeto ---

    private float
        objectDensity,                  // Densidade cacheada do objeto (mass / volume) — só recalcula via dirty flag
        mass,                           // Massa do objeto
        volume;                         // Volume do objeto

    private float
        floatEffectValue,               // Força de flutuabilidade calculada
        floatEffectValueModifier;       // Modificador externo de flutuabilidade

    private float
        dragMultiplier = 1.0f,          // Multiplicador de resistência ao movimento
        cachedSubmersionFraction = 1f;  // Fração de submersão cacheada — evita recálculo todo frame

    /// Centro de massa do objeto, em pixels, relativo à origem do mundo (mesma origem
    /// do TransformComponent). Por padrão coincide com o centro geométrico do
    /// TransformComponent, mas pode ser sobrescrito via updateMassCenter() para
    /// corpos compostos/assimétricos (ex: SubmarineNode com múltiplas SubmarinePart
    /// de massas diferentes) — o braço de alavanca do torque de flutuação é medido a
    /// partir deste ponto, não do centro geométrico puro.
    private final Vector2 massCenter = new Vector2();
    private boolean massCenterOverridden = false;

    /// Dados de movimentação calculados para o líquido atual (resistência, limites, gravidade)
    private final MovementDataComponent intermediary;

    /// Snapshot dos dados de movimentação originais — restaurados ao sair do líquido
    private final MovementDataComponent storedMovementData;

    private float equilibriumSubmersionThreshold = 0f;

    private static final float MIN_EQUILIBRIUM_THRESHOLD = 0.16f;

    /// Teto de segurança — não faz sentido o threshold ultrapassar 100% de submersão.
    private static final float MAX_EQUILIBRIUM_THRESHOLD = 0.95f;

    private static final float EQUILIBRIUM_CALIBRATION_FACTOR = 1.5f;

    // --- Torque de estabilidade rotacional ---

    /// Ângulo de amostragem vizinho (graus) usado para decidir a direção do torque:
    /// comparamos a submersão no ângulo atual contra +SAMPLE e -SAMPLE, e o lado com
    /// MAIOR submersão (mais estável, menos volume fora d'água) indica a direção pra
    /// onde o torque deve empurrar a rotação.
    private static final float TORQUE_SAMPLE_ANGLE_DEGREES = 5f;

    /// Multiplicador de conversão da diferença de submersão amostrada para o valor de
    /// aceleração angular aplicado em rAxis.acceleration. Ajustável para tuning.
    private float torqueStrength = 1f;

    /// Buffer reutilizável para os 4 cantos do retângulo rotacionado, evita alocação por frame.
    private final float[] cornerXBuffer = new float[4];
    private final float[] cornerYBuffer = new float[4];

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
        if (!needsUpdateStoredMovement) return;
        storeCurrentMovementValues();
        canInteract = canInteractBuffer;
        needsUpdateStoredMovement = false;
    }

    @Override
    public void update(float delta) {
        updateLiquidState();
        updateSimulation(delta);
        updateStoredMovement();

        if (!inLiquid) return;
        object.inLiquidUpdate();
    }

    @Override
    public void postUpdate() {
    }

    @Override
    public void initObject() {
    }

    /// Recalcula a fração de submersão de equilíbrio teórica (Arquimedes calibrado).
    /// Chamado sempre que objectDensity ou o líquido de maior densidade mudam.
    private void recalculateEquilibriumThreshold() {
        if (highestDensityLiquidBuffer == null || highestDensityLiquidBuffer.density <= 0f) {
            equilibriumSubmersionThreshold = MIN_EQUILIBRIUM_THRESHOLD;
            return;
        }

        float theoreticalFraction = objectDensity / highestDensityLiquidBuffer.density;

        equilibriumSubmersionThreshold = MathUtils.clamp(
            theoreticalFraction * EQUILIBRIUM_CALIBRATION_FACTOR,
            MIN_EQUILIBRIUM_THRESHOLD,
            MAX_EQUILIBRIUM_THRESHOLD
        );
    }

    private void updateSimulation(float delta) {

        boolean shouldSimulate = canInteract && !liquidAndRegionMap.isEmpty();

        applyChange(shouldSimulate);

        if (!shouldSimulate)
            return;

        updatePhysicsData();
        updateMovementData();
        updateSubmersionFraction();

        applyLiquidPhysics(delta);

        applyTorqueStability(delta);
    }

    private void recalculatePhysicsData() {
        objectDensity = (volume > 0f) ? (mass / volume) : Float.MAX_VALUE;
    }

    private void updatePhysicsData() {
        if (!needsUpdatePhysicsData) return;

        recalculatePhysicsData();
        recalculateEquilibriumThreshold();

        // A densidade mudou, então o efeito de flutuabilidade precisa ser recalculado também
        needsUpdateMovement = true;
        needsUpdatePhysicsData = false;
    }

    private void updateMovementData() {
        if (!needsUpdateMovement) return;

        recalculateMovementData();

        isConstraintsDirty = true;

        needsUpdateMovement = false;
    }

    private void applyLiquidPhysics(float delta) {

        if (cachedSubmersionFraction <= 0f) {
            resetSimulatedFloatation();
            return;
        }

        calculateFloatEffect(highestDensityLiquidBuffer, delta);

        applyConstraints();

        applyFloat();
    }

    private void applyFloat() {
        if (cachedSubmersionFraction <= 0f) return;

        float targetFloat =
            (floatEffectValue + floatEffectValueModifier)
                * cachedSubmersionFraction;

        moveC.dataComponent.yAxis.setMovement(targetFloat);
    }

    private void recalculateMovementData() {
        prepareIntermediary();
        calculateResistance(highestDragLiquidBuffer);
    }

    /// Calcula o quanto o objeto está submerso na região líquida atual (0-1).
    /// Usa apenas as dimensões da região (surfaceY) — não depende de densidade/drag.
    private void updateSubmersionFraction() {
        if (currentLiquidRegionBuffer == null) {
            cachedSubmersionFraction = 0f;
            return;
        }

        cachedSubmersionFraction = calculateSubmersionFractionAtAngle(
            object.getTransformC().rotation
        );
    }

    /// Calcula a fração de submersão (0-1) do retângulo do objeto, ROTACIONADO pelo
    /// ângulo informado (em graus), contra a superfície horizontal do líquido atual.
    ///
    /// Usa o MENOR e o MAIOR Y entre os 4 cantos rotacionados como topo/base efetivos
    /// do retângulo — isso reduz exatamente à fórmula linear original quando o ângulo
    /// é 0° (retângulo alinhado), e generaliza corretamente para qualquer rotação.
    ///
    /// NOTA: uma versão anterior deste método calculava a MÉDIA da fração de submersão
    /// de cada canto individualmente (clamped por canto). Isso parecia razoável mas
    /// divergia sistematicamente da fórmula linear original mesmo em 0° de rotação
    /// (chegava a ser metade do valor correto em alguns pontos), porque cada canto
    /// clampado perde a informação de "distância total entre topo e base" — a média de
    /// clamps independentes não é equivalente a min/max seguido de uma única razão.
    private float calculateSubmersionFractionAtAngle(float angleDegrees) {
        if (currentLiquidRegionBuffer == null) return 0f;

        float surfaceY = currentLiquidRegionBuffer.getY() + currentLiquidRegionBuffer.getHeight();

        computeRotatedCorners(angleDegrees, cornerXBuffer, cornerYBuffer);

        TransformComponent t = object.getTransformC();
        float height = t.height;
        if (height <= 0f) return 0f;

        float minY = cornerYBuffer[0];
        float maxY = cornerYBuffer[0];
        for (int i = 1; i < 4; i++) {
            float y = cornerYBuffer[i];
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        // totalmente submerso: o topo (maior Y) está abaixo da superfície
        if (maxY <= surfaceY) return 1f;

        // totalmente fora: a base (menor Y) está acima da superfície
        if (minY >= surfaceY) return 0f;

        // parcialmente submerso: fração da altura efetiva (maxY - minY) abaixo da superfície
        float effectiveHeight = maxY - minY;
        if (effectiveHeight <= 0f) return 0f;

        return MathUtils.clamp((surfaceY - minY) / effectiveHeight, 0f, 1f);
    }

    /// Calcula os 4 cantos do retângulo do objeto (definido por TransformComponent:
    /// x, y, width, height), rotacionados ao redor do centro de massa pelo ângulo
    /// informado (em graus). Escreve o resultado nos arrays outX/outY (tamanho 4).
    ///
    /// Ordem dos cantos: 0 = inferior-esquerdo, 1 = inferior-direito,
    /// 2 = superior-direito, 3 = superior-esquerdo (sentido anti-horário a partir do
    /// canto inferior-esquerdo local).
    private void computeRotatedCorners(float angleDegrees, float[] outX, float[] outY) {
        TransformComponent t = object.getTransformC();

        float halfW = t.width * 0.5f;
        float halfH = t.height * 0.5f;

        // Centro geométrico do retângulo (pode diferir do centro de massa)
        float geomCenterX = t.x + halfW;
        float geomCenterY = t.y + halfH;

        // Pivot de rotação: usa o centro de massa customizado se foi definido
        // (updateMassCenter), senão cai no centro geométrico padrão — importante para
        // objetos simples (ex: Player) que nunca chamam updateMassCenter().
        float pivotX = massCenterOverridden ? massCenter.x : geomCenterX;
        float pivotY = massCenterOverridden ? massCenter.y : geomCenterY;

        float rad = angleDegrees * MathUtils.degreesToRadians;
        float cos = MathUtils.cos(rad);
        float sin = MathUtils.sin(rad);

        // Cantos locais relativos ao centro GEOMÉTRICO, antes da rotação
        float[] localX = {-halfW, halfW, halfW, -halfW};
        float[] localY = {-halfH, -halfH, halfH, halfH};

        for (int i = 0; i < 4; i++) {
            // Posição absoluta do canto sem rotação
            float absX = geomCenterX + localX[i];
            float absY = geomCenterY + localY[i];

            // Rotacionamos ao redor do PIVOT (centro de massa), não do centro geométrico
            float relX = absX - pivotX;
            float relY = absY - pivotY;

            float rotatedX = relX * cos - relY * sin;
            float rotatedY = relX * sin + relY * cos;

            outX[i] = pivotX + rotatedX;
            outY[i] = pivotY + rotatedY;
        }
    }

    /// Calcula e aplica o torque de estabilidade rotacional, baseado na comparação de
    /// submersão entre o ângulo atual e dois ângulos vizinhos (+/- TORQUE_SAMPLE_ANGLE).
    /// O lado com MAIOR submersão (mais volume abaixo d'água, mais estável) indica a
    /// direção pra onde a rotação deve ser empurrada. A força é aplicada via
    /// rAxis.acceleration, deixando o próprio AxisData resolver a convergência suave
    /// com resistência (weightFactor/deceleration), em vez de setar rotação direto —
    /// isso evita o mesmo problema de "salto" que já resolvemos para o eixo Y.
    private void applyTorqueStability(float delta) {
        if (!moveC.dataComponent.rAxis.canMove) return;
        if (currentLiquidRegionBuffer == null || cachedSubmersionFraction <= 0f) {
            moveC.dataComponent.rAxis.cleanAcceleration();
            return;
        }

        float currentAngle = object.getTransformC().rotation;

        float fractionAtPositive = calculateSubmersionFractionAtAngle(currentAngle + TORQUE_SAMPLE_ANGLE_DEGREES);
        float fractionAtNegative = calculateSubmersionFractionAtAngle(currentAngle - TORQUE_SAMPLE_ANGLE_DEGREES);

        // Diferença de submersão entre girar pra um lado ou outro. Positivo significa
        // que girar no sentido positivo (+) aumenta a submersão (mais estável) —
        // então o torque deve empurrar nessa direção.
        float stabilityGradient = fractionAtPositive - fractionAtNegative;

        // Sem gradiente relevante (objeto já no ponto de equilíbrio local, ou
        // simetria perfeita) — não aplica torque, deixa o rAxis desacelerar naturalmente.
        if (Math.abs(stabilityGradient) < 0.0001f) {
            moveC.dataComponent.rAxis.cleanAcceleration();
            return;
        }

        float torqueAccel = toPixels(stabilityGradient * torqueStrength * cachedSubmersionFraction);

        moveC.dataComponent.rAxis.setMovement(-torqueAccel);
    }

    private void applyConstraints() {
        if (!isConstraintsDirty) return;

        // Cacheia a referência local para evitar resolver moveC.dataComponent repetidamente
        MovementDataComponent target = moveC.dataComponent;

        target.xAxis.weightFactor = intermediary.xAxis.weightFactor;
        target.yAxis.weightFactor = intermediary.yAxis.weightFactor;
        target.rAxis.weightFactor = intermediary.rAxis.weightFactor;

        target.xAxis.deceleration = intermediary.xAxis.deceleration;
        target.yAxis.deceleration = intermediary.yAxis.deceleration;
        target.rAxis.deceleration = intermediary.rAxis.deceleration;

        target.xAxis.maxMoveVel = intermediary.xAxis.maxMoveVel;
        target.yAxis.maxMoveVel = intermediary.yAxis.maxMoveVel;
        target.rAxis.maxMoveVel = intermediary.rAxis.maxMoveVel;

        target.gravityAffected = intermediary.gravityAffected;
        target.gravityScale = intermediary.gravityScale;

        isConstraintsDirty = false;
    }

    /// Calcula a força de flutuabilidade simulada com base na diferença de densidade
    /// entre objeto e líquido, integrada suavemente ao longo do tempo (delta).
    /// O floatEffectValueModifier NÃO participa dessa integração — é somado
    /// separadamente em applyFloat(), como um valor forçado por frame.
    private void calculateFloatEffect(LiquidData data, float delta) {
        if (data == null || volume <= 0f) return;

        float targetFloatEffect =
            (data.density - objectDensity) * volume;

        // Perto da superfície (submersão abaixo do limiar), travamos o valor
        // diretamente no alvo já achatado pela submersão, evitando acúmulo residual.
        if (cachedSubmersionFraction < equilibriumSubmersionThreshold) {
            floatEffectValue = targetFloatEffect * cachedSubmersionFraction;
            return;
        }

        floatEffectValue += (targetFloatEffect - floatEffectValue) * Math.min(delta, 1f);

        // Nunca ultrapassa o alvo teórico em módulo — reaproveita o sinal de
        // targetFloatEffect em vez de recalcular Math.abs duas vezes.
        if (targetFloatEffect >= 0f) {
            if (floatEffectValue > targetFloatEffect) floatEffectValue = targetFloatEffect;
            else if (floatEffectValue < -targetFloatEffect) floatEffectValue = -targetFloatEffect;
        } else {
            if (floatEffectValue < targetFloatEffect) floatEffectValue = targetFloatEffect;
            else if (floatEffectValue > -targetFloatEffect) floatEffectValue = -targetFloatEffect;
        }
    }

    private void applyChange(boolean shouldSimulate) {

        if (shouldSimulate && !inLiquid) {
            inLiquid = true;
            object.onLiquidEnter();
            return;
        }

        if (!shouldSimulate && inLiquid) {
            inLiquid = false;
            restartStoredMovementValues();
            resetFlotation();
            moveC.dataComponent.yAxis.resetMovement();
            moveC.dataComponent.rAxis.cleanAcceleration();
            object.onLiquidExit();
        }
    }

    private void updateLiquidState() {

        if (needsUpdateCurrentRegion) {
            updateCurrentRegion();
            needsUpdateCurrentRegion = false;
        }

        if (needsUpdateCurrentLiquidData) {
            updateCurrentLiquidData();
            needsUpdateMovement = true;
            needsUpdateCurrentLiquidData = false;
        }
    }

    /// Encontra a região líquida mais próxima do centro do objeto.
    private void updateCurrentRegion() {

        if (liquidAndRegionMap.isEmpty()) {
            currentLiquidRegionBuffer = null;
            return;
        }

        // Fast path: única região em contato, não há o que comparar
        if (liquidAndRegionMap.size() == 1) {
            ArrayList<LiquidRegion> onlyList = liquidAndRegionMap.values().iterator().next();
            if (onlyList.size() == 1) {
                currentLiquidRegionBuffer = onlyList.get(0);
                return;
            }
        }

        TransformComponent t = object.getTransformC();
        float objectCenterX = t.getCenterX();
        float objectCenterY = t.getCenterY();

        LiquidRegion closestRegion = null;
        float closestDistance = Float.MAX_VALUE;

        for (Map.Entry<LiquidData, ArrayList<LiquidRegion>> entry : liquidAndRegionMap.entrySet()) {
            ArrayList<LiquidRegion> list = entry.getValue();

            for (int i = 0; i < list.size(); i++) {

                LiquidRegion region = list.get(i);
                if (region == null) continue;

                float regionCenterX = region.getX() + (region.getWidth() * 0.5f);
                float regionCenterY = region.getY() + (region.getHeight() * 0.5f);

                float dx = objectCenterX - regionCenterX;
                float dy = objectCenterY - regionCenterY;

                float distance = (dx * dx) + (dy * dy);

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestRegion = region;
                }
            }
        }

        currentLiquidRegionBuffer = closestRegion;
    }

    /// Encontra o líquido de maior densidade e o de maior drag entre os líquidos em contato.
    private void updateCurrentLiquidData() {

        if (liquidAndRegionMap.isEmpty()) {
            highestDensityLiquidBuffer = null;
            highestDragLiquidBuffer = null;
            recalculateEquilibriumThreshold();
            return;
        }

        if (liquidAndRegionMap.size() == 1) {
            LiquidData onlyData = liquidAndRegionMap.keySet().iterator().next();
            highestDensityLiquidBuffer = onlyData;
            highestDragLiquidBuffer = onlyData;
            recalculateEquilibriumThreshold();
            return;
        }

        LiquidData highestDensity = null;
        LiquidData highestDrag = null;

        for (LiquidData data : liquidAndRegionMap.keySet()) {

            if (highestDensity == null) {
                highestDensity = data;
                highestDrag = data;
                continue;
            }

            if (data.density > highestDensity.density)
                highestDensity = data;

            if (data.drag > highestDrag.drag)
                highestDrag = data;
        }

        highestDensityLiquidBuffer = highestDensity;
        highestDragLiquidBuffer = highestDrag;

        recalculateEquilibriumThreshold();
    }

    // --- Liquid buffer ---

    /// Registra contato com um líquido/região. Só marca para recálculo quando necessário.
    public void addLiquid(LiquidData liquidData, LiquidRegion region) {
        if (region == null || liquidData == null) return;

        ArrayList<LiquidRegion> list = liquidAndRegionMap.get(liquidData);

        if (list == null) {
            list = new ArrayList<>(4);
            liquidAndRegionMap.put(liquidData, list);
            needsUpdateCurrentLiquidData = true;
        }

        list.add(region);
        needsUpdateCurrentRegion = true;
    }

    /// Remove contato com um líquido/região. Só remove os dados do líquido quando a última região sai.
    public void removeLiquid(LiquidData liquidData, LiquidRegion region) {
        if (liquidData == null || region == null) return;

        ArrayList<LiquidRegion> list = liquidAndRegionMap.get(liquidData);

        if (list == null) {
            needsUpdateCurrentLiquidData = true;
            return;
        }

        list.remove(region);
        needsUpdateCurrentRegion = true;

        if (list.isEmpty()) {
            liquidAndRegionMap.remove(liquidData);
            needsUpdateCurrentLiquidData = true;
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
        isConstraintsDirty = true;
    }

    private void prepareIntermediary() {
        intermediary.set(storedMovementData);
    }

    // --- Recalculation ---

    private void calculateResistance(LiquidData data) {
        if (data == null) return;

        float drag = data.drag * dragMultiplier;

        intermediary.xAxis.weightFactor = drag;
        intermediary.yAxis.weightFactor = drag;
        intermediary.rAxis.weightFactor = drag;

        intermediary.xAxis.deceleration = storedMovementData.xAxis.deceleration + drag;
        intermediary.yAxis.deceleration = storedMovementData.yAxis.deceleration + drag;
        intermediary.rAxis.deceleration = storedMovementData.rAxis.deceleration + drag;
    }

    // --- Getters / Setters ---

    private void markUpdateSimulationData() {
        needsUpdatePhysicsData = true;
        needsUpdateMovement = true;
        isConstraintsDirty = true;
    }

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

    public float getDragMultiplier() {
        return dragMultiplier;
    }

    public boolean isInLiquid() {
        return inLiquid;
    }

    public void setMass(float mass) {
        if (mass < 0) return;
        this.mass = mass;
        markUpdateSimulationData();
    }

    public void setVolume(float volume) {
        this.volume = volume;
        markUpdateSimulationData();
    }

    public void setFloatingEffectModifier(float v) {
        this.floatEffectValueModifier = v;
    }

    public void setDragMultiplier(float v) {
        this.dragMultiplier = v;
        markUpdateSimulationData();
    }

    public float getTorqueStrength() {
        return torqueStrength;
    }

    public void setTorqueStrength(float torqueStrength) {
        this.torqueStrength = torqueStrength;
    }

    /// Atualiza o centro de massa (em pixels, mesma origem do TransformComponent) usado
    /// como pivot para o cálculo do torque de estabilidade rotacional. Deve ser chamado
    /// sempre que a distribuição de massa do objeto mudar (ex: SubmarineNode após
    /// recalcular a massa de suas SubmarinePart, incluindo passageiros a bordo).
    ///
    /// Se nunca chamado, o pivot usado é o centro GEOMÉTRICO do TransformComponent
    /// (comportamento padrão, correto para objetos simples/simétricos como o Player).
    public void updateMassCenter(float x, float y) {
        this.massCenter.set(x, y);
        this.massCenterOverridden = true;
    }

    /// Reseta o centro de massa para acompanhar o centro geométrico do
    /// TransformComponent automaticamente (comportamento padrão).
    public void clearMassCenterOverride() {
        this.massCenterOverridden = false;
    }

    public Vector2 getMassCenter() {
        return massCenter;
    }

    /// Marca para rearmazenar valores de movimentação na próxima atualização.
    public void updateCurrentStoredMovementValues() {
        this.needsUpdateStoredMovement = true;
        this.canInteractBuffer = this.canInteract;
        this.canInteract = false;
        this.originalValuesStored = false;
    }

    private void resetSimulatedFloatation() {
        floatEffectValue = 0;
    }

    public void resetFlotation() {
        floatEffectValue = 0;
        floatEffectValueModifier = 0;
    }

    public boolean hasFloatation() {
        return floatEffectValueModifier != 0 || floatEffectValue != 0;
    }

    // --- Dispose ---

    @Override
    public void dispose() {
        if (disposed) return;
        liquidAndRegionMap.clear();
        nullifyReferences();
        disposed = true;
    }

    public void nullifyReferences() {
        moveC = null;
        object = null;
    }
}
