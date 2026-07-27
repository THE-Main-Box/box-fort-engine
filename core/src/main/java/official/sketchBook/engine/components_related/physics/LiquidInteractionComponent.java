package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.LIOBase;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.MovementDataComponent;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;

import java.util.ArrayList;
import java.util.IdentityHashMap;

public abstract class LiquidInteractionComponent implements Component {

    protected final MovementComponent moveC;

    /// Flag de estado de interação
    protected boolean
        canInteractBuffer = false,  //Buffer original de interação
        canInteract = true,         //Se podemos interagir atualmente
        inLiquid = false;           //Se estamos dentro de algum liquido atualmente

    /// Flag de estado de update de dados originais
    protected boolean
        originalValuesStored = false,       //Se os dados originais foram armazenados
        needsUpdateStoredMovement = true;   //Se precisamos atualizar para novos dados

    /// Flags de atualização de simulação
    protected boolean
        needsUpdateCurrentLiquidData = true,    //Se precisamos atualizar os dados de liquido atual
        needsUpdateCurrentRegion = true,        //Se precisamos atualizar a região atual
        needsUpdatePhysicsData = true,          //Se precisamos atualizar os dados de física
        needsUpdateMovement = true,             //Se precisamos atualizar o movimento
        isConstraintsDirty = true;              //Se precisamos atualizar as constraints por estarem desatualizadas

    /// Valores do objeto para simulação
    protected float
        objectDensity,      //Densidade total do objeto
        mass,               //Massa do objeto
        volume;             //Volume total do objeto

    /// Valores para a simulação em si
    protected float
        cachedSubmersionFraction = 1f,  //Submersão total do objeto
        floatEffectValue,               //Flutuabilidade a ser aplicada
        floatEffectValueModifier;       //Modificador de flutuabilidade a ser somado

    /// Ponto de aplicação da flutuabilidade do objeto
    protected final Vector2
        centerOfMass = new Vector2(),
        floatApplicationPoint = new Vector2();

    /// Armazenamento de dados de movimentação
    protected final MovementDataComponent
        storedMovementData = new MovementDataComponent(),
        intermediary = new MovementDataComponent();

    /// Dados de liquido a serem interagidos
    protected LiquidData
        highestDensityLiquidBuffer,
        highestDragLiquidBuffer;

    protected final IdentityHashMap<LiquidData, ArrayList<LiquidRegion>> liquidAndRegionMap = new IdentityHashMap<>();

    protected LIOBase owner;

    public LiquidInteractionComponent(LIOBase owner, MovementComponent moveC) {
        this.moveC = moveC;
        this.owner = owner;
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

    protected abstract void updateSimulation(float delta);

    protected void updateLiquidState() {
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

    protected void applyChange(boolean shouldSimulate) {
        if (shouldSimulate && !inLiquid) {
            inLiquid = true;

            onLiquidEnter();
            owner.onLiquidEnter();
            return;
        }

        if (!shouldSimulate && inLiquid) {
            inLiquid = false;
            restartStoredMovementValues();
            resetFlotation();

            onLiquidExit();
            owner.onLiquidExit();
        }
    }

    protected abstract void onLiquidEnter();
    protected abstract void onLiquidExit();

    protected abstract void updateCurrentRegion();

    protected abstract void updateCurrentLiquidData();

    protected void updateStoredMovement() {
        if (!needsUpdateStoredMovement) return;
        storeCurrentMovementValues();
        canInteract = canInteractBuffer;
        needsUpdateStoredMovement = false;
    }

    protected void storeCurrentMovementValues() {
        if (originalValuesStored) return;
        this.storedMovementData.set(moveC.dataComponent);
        originalValuesStored = true;
    }

    protected void restartStoredMovementValues() {
        if (!originalValuesStored) return;
        this.moveC.dataComponent.set(storedMovementData);
        isConstraintsDirty = true;
    }

    protected void prepareIntermediary() {
        intermediary.set(storedMovementData);
    }

    protected void recalculateMovementData() {
        prepareIntermediary();
        calculateResistance(highestDragLiquidBuffer);
    }

    protected void calculateResistance(LiquidData data) {
        if (data == null) return;

        float drag = data.drag;

        intermediary.xAxis.weightFactor = drag;
        intermediary.yAxis.weightFactor = drag;
        intermediary.rAxis.weightFactor = drag;

        intermediary.xAxis.deceleration = storedMovementData.xAxis.deceleration + drag;
        intermediary.yAxis.deceleration = storedMovementData.yAxis.deceleration + drag;
        intermediary.rAxis.deceleration = storedMovementData.rAxis.deceleration + drag;
    }

    protected void applyConstraints() {
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

    public void updateCurrentStoredMovementValues() {
        this.needsUpdateStoredMovement = true;
        this.canInteractBuffer = this.canInteract;
        this.canInteract = false;
        this.originalValuesStored = false;
    }


    protected void markUpdateSimulationData() {
        needsUpdatePhysicsData = true;
        needsUpdateMovement = true;
        isConstraintsDirty = true;
    }

    protected void resetFlotation() {
        floatEffectValue = 0;
        floatEffectValueModifier = 0;
    }

    protected void resetSimulatedFloatation() {
        floatEffectValue = 0;
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

    public void setMass(float mass) {
        if (mass < 0) return;
        this.mass = mass;
        markUpdateSimulationData();
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        markUpdateSimulationData();
    }

    public Vector2 getCenterOfMass() {
        return centerOfMass;
    }

    public void updateCenterOfMass(float x, float y){
        this.centerOfMass.set(x, y);
    }

    public void updateCenterOfMass(Vector2 centerOfMass){
        this.centerOfMass.set(centerOfMass);
    }

    public float getCachedSubmersionFraction() {
        return cachedSubmersionFraction;
    }

    public Vector2 getFloatApplicationPoint() {
        return floatApplicationPoint;
    }

    public float getFloatEffect() {
        return floatEffectValue;
    }

    public float getFloatEffectValueModifier() {
        return floatEffectValueModifier;
    }

    public void setFloatingEffectModifier(float v) {
        this.floatEffectValueModifier = v;
    }

    public boolean isInLiquid() {
        return inLiquid;
    }

    public boolean hasFloatation() {
        return floatEffectValueModifier != 0 || floatEffectValue != 0;
    }

}
