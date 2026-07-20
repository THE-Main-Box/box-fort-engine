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

/// Simula interação física com líquidos — aplica flutuabilidade, resistência, limites de
/// velocidade e estabilidade rotacional (busca o ângulo de equilíbrio/máxima submersão).
/// Deve ser atualizado após o componente de movimentação.
public class PhysicalMobLiquidInteractionComponent implements Component {

    private SimpleLiquidInteractableObjectII object;
    private MovementComponent moveC;

    private final IdentityHashMap<LiquidData, ArrayList<LiquidRegion>> liquidAndRegionMap = new IdentityHashMap<>();

    private LiquidData
        highestDensityLiquidBuffer,
        highestDragLiquidBuffer;

    private LiquidRegion currentLiquidRegionBuffer;

    private boolean
        canInteractBuffer = false,
        canInteract = true,
        inLiquid = false;

    private boolean
        originalValuesStored = false,
        needsUpdateStoredMovement = true;

    private boolean
        needsUpdateCurrentLiquidData = true,
        needsUpdateCurrentRegion = true,
        needsUpdatePhysicsData = true,
        needsUpdateMovement = true,
        isConstraintsDirty = true;

    private float
        objectDensity,
        mass,
        volume;

    private float
        floatEffectValue,
        floatEffectValueModifier;

    private float
        dragMultiplier = 1.0f,
        cachedSubmersionFraction = 1f;

    private final Vector2 massCenter = new Vector2();
    private boolean massCenterOverridden = false;

    private final MovementDataComponent intermediary;
    private final MovementDataComponent storedMovementData;

    private float equilibriumSubmersionThreshold = 0f;

    private static final float MIN_EQUILIBRIUM_THRESHOLD = 0.16f;
    private static final float MAX_EQUILIBRIUM_THRESHOLD = 0.95f;
    private static final float EQUILIBRIUM_CALIBRATION_FACTOR = 1.5f;

    private static final float EQUILIBRIUM_SCAN_STEP_DEGREES = 15f;

    private float equilibriumAngleDegrees = 0f;
    private boolean needsRecalculateEquilibriumAngle = true;

    private static final float EQUILIBRIUM_ANGLE_TOLERANCE_DEGREES = 1f;

    private float torqueStrength = 1f;

    private final float[] cornerXBuffer = new float[4];
    private final float[] cornerYBuffer = new float[4];

    private boolean disposed = false;

    public PhysicalMobLiquidInteractionComponent(SimpleLiquidInteractableObjectII object) {
        this.object = object;
        this.moveC = object.getMoveC();
        this.storedMovementData = new MovementDataComponent();
        this.intermediary = new MovementDataComponent();
        this.updateCurrentStoredMovementValues();
    }

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

        if (!shouldSimulate) return;

        updatePhysicsData();
        updateMovementData();
        updateSubmersionFraction();

        applyLiquidPhysics(delta);
    }

    private void recalculatePhysicsData() {
        objectDensity = (volume > 0f) ? (mass / volume) : Float.MAX_VALUE;
    }

    private void updatePhysicsData() {
        if (!needsUpdatePhysicsData) return;

        recalculatePhysicsData();
        recalculateEquilibriumThreshold();

        needsUpdateMovement = true;
        needsRecalculateEquilibriumAngle = true;
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

        float targetFloat = (floatEffectValue + floatEffectValueModifier) * cachedSubmersionFraction;
        moveC.dataComponent.yAxis.setMovement(targetFloat);
    }

    private void recalculateMovementData() {
        prepareIntermediary();
        calculateResistance(highestDragLiquidBuffer);
    }

    private void updateSubmersionFraction() {
        if (currentLiquidRegionBuffer == null) {
            cachedSubmersionFraction = 0f;
            return;
        }

        cachedSubmersionFraction = calculateSubmersionFractionAtAngle(object.getTransformC().rotation);
    }

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

        if (maxY <= surfaceY) return 1f;
        if (minY >= surfaceY) return 0f;

        float effectiveHeight = maxY - minY;
        if (effectiveHeight <= 0f) return 0f;

        return MathUtils.clamp((surfaceY - minY) / effectiveHeight, 0f, 1f);
    }

    private void computeRotatedCorners(float angleDegrees, float[] outX, float[] outY) {
        TransformComponent t = object.getTransformC();

        float halfW = t.width * 0.5f;
        float halfH = t.height * 0.5f;

        float geomCenterX = t.x + halfW;
        float geomCenterY = t.y + halfH;

        float pivotX = massCenterOverridden ? massCenter.x : geomCenterX;
        float pivotY = massCenterOverridden ? massCenter.y : geomCenterY;

        float rad = angleDegrees * MathUtils.degreesToRadians;
        float cos = MathUtils.cos(rad);
        float sin = MathUtils.sin(rad);

        float[] localX = {-halfW, halfW, halfW, -halfW};
        float[] localY = {-halfH, -halfH, halfH, halfH};

        for (int i = 0; i < 4; i++) {
            float absX = geomCenterX + localX[i];
            float absY = geomCenterY + localY[i];

            float relX = absX - pivotX;
            float relY = absY - pivotY;

            float rotatedX = relX * cos - relY * sin;
            float rotatedY = relX * sin + relY * cos;

            outX[i] = pivotX + rotatedX;
            outY[i] = pivotY + rotatedY;
        }
    }

    private void applyConstraints() {
        if (!isConstraintsDirty) return;

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

    private void calculateFloatEffect(LiquidData data, float delta) {
        if (data == null || volume <= 0f) return;

        float targetFloatEffect = (data.density - objectDensity) * volume;

        if (cachedSubmersionFraction < equilibriumSubmersionThreshold) {
            floatEffectValue = targetFloatEffect * cachedSubmersionFraction;
            return;
        }

        floatEffectValue += (targetFloatEffect - floatEffectValue) * Math.min(delta, 1f);

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
            moveC.dataComponent.rAxis.resetMovement();
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

    private void updateCurrentRegion() {
        if (liquidAndRegionMap.isEmpty()) {
            currentLiquidRegionBuffer = null;
            return;
        }

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
            needsRecalculateEquilibriumAngle = true;
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

            if (data.density > highestDensity.density) highestDensity = data;
            if (data.drag > highestDrag.drag) highestDrag = data;
        }

        highestDensityLiquidBuffer = highestDensity;
        highestDragLiquidBuffer = highestDrag;

        recalculateEquilibriumThreshold();
        needsRecalculateEquilibriumAngle = true;
    }

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

    public float getEquilibriumAngleDegrees() {
        return equilibriumAngleDegrees;
    }

    public void updateMassCenter(float x, float y) {
        this.massCenter.set(x, y);
        this.massCenterOverridden = true;
        this.needsRecalculateEquilibriumAngle = true;
    }

    public void clearMassCenterOverride() {
        this.massCenterOverridden = false;
        this.needsRecalculateEquilibriumAngle = true;
    }

    public Vector2 getMassCenter() {
        return massCenter;
    }

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
