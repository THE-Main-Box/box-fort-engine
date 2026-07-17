package official.sketchBook.engine.components_related.physics;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.SimpleLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.objects.MovementDataComponent;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;

import java.util.ArrayList;
import java.util.IdentityHashMap;

/// Simula interação física com líquidos — aplica flutuabilidade, resistência e limites de velocidade.
/// Deve ser atualizado após o componente de movimentação.
public class PhysicalMobLiquidInteractionComponent implements Component {

    /// Refer�ncia ao objeto dono
    private SimpleLiquidInteractableObjectII object;

    /// Componente de movimenta��o do objeto dono
    private MovementComponent moveC;

    /// Mapa para identificação de liquidos
    private final IdentityHashMap<LiquidData, ArrayList<LiquidRegion>> liquidAndRegionMap = new IdentityHashMap<>();

    private LiquidData
        highestDensityLiquidBuffer,
        highestDragLiquidBuffer;

    private LiquidRegion currentLiquidRegionBuffer;

    // --- Flags de estado ---

    private boolean
        canInteractBuffer = false,      // Buffer do canInteract — restaurado ap�s armazenar movimento
        canInteract = true,             // Se pode simular intera��o com l�quidos
        inLiquid = false;               // Se est� fisicamente dentro de um l�quido

    private boolean
        originalValuesStored = false,   // Se os valores originais de movimenta��o foram armazenados
        updateStoredMovement = true;    // Se deve rearmazenar os valores de movimenta��o

    private boolean
        needsUpdateCurrentLiquidData = true, //Se precisa atualizar os dados de liquido atual
        needsUpdateCurrentRegion = true,     //Se precisa atualizar a região atual
        needsUpdatePhysicsData = true,
        needsUpdateMovement = true,          //Se precisamos atualizar os dados de movimentação
        isConstraintsDirty = true;        // Se as constraints precisam ser reaplicadas no moveC

    // --- Dados f�sicos do objeto ---

    private float
        objectDensity,
        mass,                           // Massa do objeto
        volume;                         // Volume do objeto

    private float
        floatEffectValue,               // For�a de flutuabilidade calculada
        floatEffectValueModifier;       // Modificador externo de flutuabilidade

    private float
        resistanceMultiplier = 1.0f,    // Multiplicador de resist�ncia ao movimento
        cachedSubmersionFraction = 1f;  // Fra��o de submers�o cacheada — evita rec�lculo todo frame
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
        updateLiquidState();
        updateSimulation(delta);
        updateStoredMovement();
    }

    @Override
    public void postUpdate() {
    }

    @Override
    public void initObject() {
    }

    private void updateSimulation(float delta){

        boolean shouldSimulate =
            canInteract
                &&
                !liquidAndRegionMap.isEmpty();


        applyChange(
            shouldSimulate
        );


        if(!shouldSimulate)
            return;


        updateMovementData();

        updatePhysicsData();

        updateSubmersionFraction();


        applyLiquidPhysics(
            delta
        );

    }

    private void recalculatePhysicsData(){

        if(volume <= 0){

            objectDensity =
                Float.MAX_VALUE;

            return;

        }


        objectDensity =
            mass / volume;

    }

    private void updatePhysicsData(){

        if(!needsUpdatePhysicsData)
            return;


        recalculatePhysicsData();


        needsUpdatePhysicsData =
            false;

    }

    private void updateMovementData(){

        if(!needsUpdateMovement)
            return;

        recalculateMovementData();

        isConstraintsDirty = true;

        needsUpdateMovement = false;

    }


    private void applyLiquidPhysics(
        float delta
    ){

        if(cachedSubmersionFraction <= 0f){

            resetFlotation();

            return;

        }


        calculateFloatEffect(
            highestDensityLiquidBuffer,
            delta
        );


        applyConstraints();

        applyFloat();

    }

    private void applyFloat() {

        if (cachedSubmersionFraction <= 0f)
            return;


        float targetFloat =

            floatEffectValue

                *

                cachedSubmersionFraction;


        moveC.dataComponent.yAxis.setMovement(
            targetFloat
        );

    }

    private void recalculateMovementData() {

        prepareIntermediary();

        calculateResistance(
            highestDragLiquidBuffer
        );

    }

    private void updateSubmersionFraction() {

        if (currentLiquidRegionBuffer == null) {

            cachedSubmersionFraction = 0f;

            return;

        }


        float surfaceY =
            currentLiquidRegionBuffer.getY();


        float objectTopY =
            object.getTransformC().y;


        float objectBottomY =

            objectTopY

                +

                object.getTransformC().height;


        // totalmente submerso
        if (objectBottomY <= surfaceY) {

            cachedSubmersionFraction = 1f;

            return;

        }


        // totalmente fora
        if (objectTopY >= surfaceY) {

            cachedSubmersionFraction = 0f;

            return;

        }


        cachedSubmersionFraction =

            (surfaceY - objectTopY)

                /

                object.getTransformC().height;


    }

    private void applyConstraints() {

        if (!isConstraintsDirty)
            return;


        moveC.dataComponent.xAxis.weightFactor =
            intermediary.xAxis.weightFactor;

        moveC.dataComponent.yAxis.weightFactor =
            intermediary.yAxis.weightFactor;

        moveC.dataComponent.rAxis.weightFactor =
            intermediary.rAxis.weightFactor;


        moveC.dataComponent.xAxis.deceleration =
            intermediary.xAxis.deceleration;

        moveC.dataComponent.yAxis.deceleration =
            intermediary.yAxis.deceleration;

        moveC.dataComponent.rAxis.deceleration =
            intermediary.rAxis.deceleration;


        moveC.dataComponent.xAxis.maxMoveVel =
            intermediary.xAxis.maxMoveVel;

        moveC.dataComponent.yAxis.maxMoveVel =
            intermediary.yAxis.maxMoveVel;

        moveC.dataComponent.rAxis.maxMoveVel =
            intermediary.rAxis.maxMoveVel;


        moveC.dataComponent.gravityAffected =
            intermediary.gravityAffected;

        moveC.dataComponent.gravityScale =
            intermediary.gravityScale;


        isConstraintsDirty = false;

    }

    private void calculateFloatEffect(
        LiquidData data,
        float delta
    ) {

        if (data == null || volume <= 0f)
            return;

        float objectDensity =
            mass / volume;


        float targetFloatEffect =

            (data.density - objectDensity)

                *

                volume

                *

                floatEffectValueModifier;


        floatEffectValue +=
            targetFloatEffect * delta;


        float maxValue =
            Math.abs(targetFloatEffect);


        if (floatEffectValue > maxValue)
            floatEffectValue = maxValue;

        else if (floatEffectValue < -maxValue)
            floatEffectValue = -maxValue;

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

        // Não há líquidos em contato
        if (liquidAndRegionMap.isEmpty()) {
            currentLiquidRegionBuffer = null;
            return;
        }

        float objectCenterX =
            object.getTransformC().getCenterX();

        float objectCenterY =
            object.getTransformC().getCenterY();

        LiquidRegion closestRegion = null;
        float closestDistance = Float.MAX_VALUE;

        for (ArrayList<LiquidRegion> list : liquidAndRegionMap.values()) {

            for (int i = 0; i < list.size(); i++) {

                LiquidRegion region = list.get(i);

                if (region == null)
                    continue;

                float regionCenterX =
                    region.getX() + (region.getWidth() * 0.5f);

                float regionCenterY =
                    region.getY() + (region.getHeight() * 0.5f);

                float dx =
                    objectCenterX - regionCenterX;

                float dy =
                    objectCenterY - regionCenterY;

                float distance =
                    (dx * dx) + (dy * dy);

                if (distance < closestDistance) {

                    closestDistance = distance;
                    closestRegion = region;

                }

            }

        }

        currentLiquidRegionBuffer = closestRegion;

    }

    private void updateCurrentLiquidData() {

        //Não estamos em nenhum líquido
        if (liquidAndRegionMap.isEmpty()) {

            highestDensityLiquidBuffer = null;
            highestDragLiquidBuffer = null;

            return;

        }

        LiquidData highestDensity = null;
        LiquidData highestDrag = null;

        for (LiquidData data : liquidAndRegionMap.keySet()) {

            //Primeiro líquido encontrado
            if (highestDensity == null) {

                highestDensity = data;
                highestDrag = data;

                continue;

            }

            //Atualiza o líquido mais denso
            if (data.density > highestDensity.density)
                highestDensity = data;

            //Atualiza o líquido de maior drag
            if (data.drag > highestDrag.drag)
                highestDrag = data;

        }

        highestDensityLiquidBuffer = highestDensity;
        highestDragLiquidBuffer = highestDrag;

    }

    // --- Liquid buffer ---

    /// Registra contato com um l�quido. S� adiciona ao buffer na primeira fixture em contato.
    public void addLiquid(
        LiquidData liquidData,
        LiquidRegion region
    ) {
        if (region == null || liquidData == null) return;

        //Tentamos obter uma lista de regiões com base nos dados de liquido passado
        ArrayList<LiquidRegion> list = liquidAndRegionMap.get(liquidData);

        //Se não houver o dado do liquido
        if (list == null) {
            //Criamos uma lista
            list = new ArrayList<>();

            //Adicionamos na lista
            liquidAndRegionMap.put(
                liquidData,
                list
            );

            //Marcamos para reccalcular, já que precisamos lidar com os dados do novo liquido
            needsUpdateCurrentLiquidData = true;
        }

        //Adicionamos a nova região no sistema
        list.add(region);
        needsUpdateCurrentRegion = true;

    }

    /// Remove contato com um l�quido. S� remove do buffer quando a �ltima fixture sai.
    public void removeLiquid(
        LiquidData liquidData,
        LiquidRegion region
    ) {

        if (liquidData == null || region == null)
            return;


        //Obtemos a lista
        ArrayList<LiquidRegion> list =
            liquidAndRegionMap.get(liquidData);


        //Se esta não existir, ignoramos
        if (list == null) {
            needsUpdateCurrentLiquidData = true;
            return;
        }
        //Removemos a região da lista
        list.remove(region);
        needsUpdateCurrentRegion = true;

        //Se esta estiver vazia
        if (list.isEmpty()) {

            //Removemos os dados de liquido daquela região
            liquidAndRegionMap.remove(
                liquidData
            );

            //Marcamos para recalculo
            needsUpdateCurrentLiquidData = true;

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
        isConstraintsDirty = true;    // For�a reaplicar constraints ap�s restaurar
    }

    /// Prepara o intermediary como c�pia limpa dos dados originais antes do rec�lculo
    private void prepareIntermediary() {
        intermediary.set(storedMovementData);
    }

    // --- Recalculation ---

    /// Calcula resist�ncia ao movimento e in�rcia com base no l�quido mais resistente
    private void calculateResistance(LiquidData data) {

        float drag =
            data.drag
                *
                resistanceMultiplier;


        intermediary.xAxis.weightFactor =
            drag;

        intermediary.yAxis.weightFactor =
            drag;

        intermediary.rAxis.weightFactor =
            drag;


        intermediary.xAxis.deceleration =
            storedMovementData.xAxis.deceleration
                +
                drag;

        intermediary.yAxis.deceleration =
            storedMovementData.yAxis.deceleration
                +
                drag;

        intermediary.rAxis.deceleration =
            storedMovementData.rAxis.deceleration
                +
                drag;
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

    public void setMass(float mass) {
        this.mass = mass;
        needsUpdateMovement = true;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        needsUpdateMovement = true;
    }

    public void setFloatingEffectModifier(float v) {
        this.floatEffectValueModifier = v;
        needsUpdateMovement = true;
    }

    public void setResistanceMultiplier(float v) {
        this.resistanceMultiplier = v;
        needsUpdateMovement = true;
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
        nullifyReferences();
        disposed = true;
    }

    public void nullifyReferences() {

        moveC = null;
        object = null;
    }
}
