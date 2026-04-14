package official.sketchBook.engine.liquid_related.model;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Fixture;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MultiLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.SimpleLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.MultiRenderableObjectII;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.game_object_related.base_game_object.BaseRoomGameObject;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoomLiquid extends BaseRoomGameObject implements Liquid, MultiRenderableObjectII {

    public final List<LiquidRegion> regionList;
    public final List<Fixture> fixtureList;

    private LiquidData liquidData;

    private Set<SimpleLiquidInteractableObjectII>
        currentInsideBuffer = new HashSet<>();
    private Set<SimpleLiquidInteractableObjectII> insideSet = new HashSet<>();

    private boolean
        inScreen;


    public RoomLiquid(
        PhysicalGameObjectDataManager worldDataManager,
        PlayableRoom ownerRoom,
        LiquidData data,
        List<LiquidRegion> regionList
    ) {
        super(
            worldDataManager,
            ownerRoom,
            RoomObjectScope.LOCAL
        );

        this.liquidData = data;
        this.regionList = regionList;

        this.fixtureList = new ArrayList<>();

        initObject();
    }

    @Override
    protected void initObject() {

    }

    @Override
    public void update(float delta) {
        super.update(delta);

        List<BaseRoomGameObject> objList = ownerRoom.roomGameObjectList;
        currentInsideBuffer.clear();

        // Coleta quem está dentro AGORA
        for (int i = 0; i < objList.size(); i++) {
            BaseRoomGameObject obj = objList.get(i);

            if (obj instanceof SimpleLiquidInteractableObjectII) {
                processSimpleObject((SimpleLiquidInteractableObjectII) obj, currentInsideBuffer);
            }

            if (obj instanceof MultiLiquidInteractableObjectII) {
                processCompositeObject((MultiLiquidInteractableObjectII) obj, currentInsideBuffer);
            }
        }

        // Detecta ENTRADAS: quem tá em currentInsideBuffer mas NÃO tá em insideSet
        for (SimpleLiquidInteractableObjectII obj : currentInsideBuffer) {
            if (!insideSet.contains(obj)) {
                obj.getLiquidInteractionC().addLiquid(liquidData);
                insideSet.add(obj);  // ← Adiciona ao estado persistente
            }
        }

        // Detecta SAÍDAS: quem tá em insideSet mas NÃO tá em currentInsideBuffer
        List<SimpleLiquidInteractableObjectII> toRemove = new ArrayList<>();
        for (SimpleLiquidInteractableObjectII obj : insideSet) {
            if (!currentInsideBuffer.contains(obj)) {
                toRemove.add(obj);
            }
        }

        // Remove quem saiu
        for (SimpleLiquidInteractableObjectII obj : toRemove) {
            obj.getLiquidInteractionC().removeLiquid(liquidData);
            insideSet.remove(obj);  // ← Remove do estado persistente
        }
    }

    /// Processa objeto simples (implementa LiquidInteractableObjectII)
    private void processSimpleObject(SimpleLiquidInteractableObjectII obj, Set<SimpleLiquidInteractableObjectII> currentInside) {
        TransformComponent t = obj.getTransformC();
        if (t == null) return;

        if (isInsideLiquid(t)) {
            currentInside.add(obj);

            // ENTROU: só chama se não tava dentro antes
            if (!insideSet.contains(obj)) {
                obj.getLiquidInteractionC().addLiquid(liquidData);
            }
        }
    }

    /// Processa objeto composto (implementa MultiLiquidInteractableObject)
    private void processCompositeObject(
        MultiLiquidInteractableObjectII composite,
        Set<SimpleLiquidInteractableObjectII> currentInside
    ) {
        List<? extends SimpleLiquidInteractableObjectII> liquidObjs = composite.getLiquidIObj();

        for (int i = 0; i < liquidObjs.size(); i++) {
            SimpleLiquidInteractableObjectII obj = liquidObjs.get(i);
            TransformComponent t = obj.getTransformC();
            if (t == null) continue;

            if (isInsideLiquid(t)) {
                currentInside.add(obj);

                // ENTROU
                if (!insideSet.contains(obj)) {
                    obj.getLiquidInteractionC().addLiquid(liquidData);
                }
            }
        }
    }

    /// AABB simples: detecta overlap entre objeto e qualquer região do líquido
    private boolean isInsideLiquid(TransformComponent t) {
        float x = t.x;
        float y = t.y;
        float w = t.width;
        float h = t.height;

        for (int i = 0; i < regionList.size(); i++) {
            LiquidRegion r = regionList.get(i);

            // ← Copia locais pra evitar múltiplas chamadas de getter
            float rx = r.getX();
            float ry = r.getY();
            float rw = r.getWidth();
            float rh = r.getHeight();

            if (x < rx + rw && x + w > rx && y < ry + rh && y + h > ry) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void disposeGeneralData() {


    }

    @Override
    protected void disposeCriticalData() {
        super.disposeCriticalData();

        this.regionList.clear();
        this.fixtureList.clear();

        this.liquidData = null;
    }

    public PhysicalGameObjectDataManager getPhysicalManager() {
        return (PhysicalGameObjectDataManager) this.worldDataManager;
    }

    public List<LiquidRegion> getRegionList() {
        return regionList;
    }

    @Override
    public LiquidData getLiquidData() {
        return liquidData;
    }

    @Override
    public int getRenderIndex() {
        return 0;
    }

    @Override
    public void updateVisuals(float delta) {
        for (int i = 0; i < regionList.size(); i++){
            regionList.get(i).updateVisuals(delta);
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        for (int i = 0; i < regionList.size(); i++){
            regionList.get(i).render(batch);

            worldDataManager.toRender.add(
                regionList.get(i).getTransformC()
            );
        }
    }

    @Override
    public boolean canRender() {
        return true;
    }

    @Override
    public boolean isInScreen() {
        return inScreen;
    }

    @Override
    public void setInScreen(boolean inScreen) {
        this.inScreen = inScreen;
    }

    @Override
    public TransformComponent getTransformC() {
        return null;
    }

    @Override
    public void disposeGraphics() {

    }

    @Override
    public List<LiquidRegion> getRenderableObjList() {
        return regionList;
    }
}
