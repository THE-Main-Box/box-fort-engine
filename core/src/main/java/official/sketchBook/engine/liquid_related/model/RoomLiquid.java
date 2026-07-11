package official.sketchBook.engine.liquid_related.model;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.*;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.MultiLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.liquid.SimpleLiquidInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.CompositeRenderableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.OptmizedRenderableObjectII;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.game_object_related.base_game_object.BaseRoomGameObject;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.engine.util_related.helper.body.FixtureData;
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.game.util_related.constants.DebugConstants;

import java.util.*;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.LIQUID;
import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.LIQUID_SUBMERGEABLE;

public class RoomLiquid extends BaseRoomGameObject implements Liquid, CompositeRenderableObjectII, OptmizedRenderableObjectII {

    public final List<LiquidRegion> regionList;
    public final List<Fixture> fixtureList;

    private LiquidData liquidData;
    private Body liquidBody;

    private final Set<SimpleLiquidInteractableObjectII>
        currentInsideBuffer = new HashSet<>(),
        insideSet = new HashSet<>();

    /// Bounds gerais do líquido (otimização)
    private float minX, minY, maxX, maxY;

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

        computeBounds(); // <<< otimização
        initObject();
    }

    @Override
    public void initObject() {
        World world = getPhysicalManager().getPhysicsWorld();

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(0, 0);

        liquidBody = world.createBody(bodyDef);
        liquidBody.setUserData(new GameObjectTag(ObjectType.LIQUID, this));

        for (int i = 0; i < regionList.size(); i++) {
            LiquidRegion r = regionList.get(i);

            FixtureData data = new FixtureData(
                0, 0,
                r.getX() + r.getWidth() / 2f,  // centro X
                r.getY() + r.getHeight() / 2f, // centro Y
                0,
                r.getWidth(),
                r.getHeight(),
                LIQUID.bit(),
                LIQUID_SUBMERGEABLE.bit(),
                false,
                true // sensor
            );

            List<Fixture> fixtures = BodyCreatorHelper.createFixturesFromData(data, liquidBody);

            for (int j = 0; j < fixtures.size(); j++) {
                fixtures.get(j).setUserData(
                    new GameObjectTag(ObjectType.LIQUID, this)
                );
            }

            fixtureList.addAll(fixtures);
        }
    }

    /// Calcula AABB geral do líquido
    private void computeBounds() {
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;

        for (int i = 0; i < regionList.size(); i++) {
            LiquidRegion r = regionList.get(i);

            float rx = r.getX();
            float ry = r.getY();
            float rw = r.getWidth();
            float rh = r.getHeight();

            if (rx < minX) minX = rx;
            if (ry < minY) minY = ry;
            if (rx + rw > maxX) maxX = rx + rw;
            if (ry + rh > maxY) maxY = ry + rh;
        }

        liquidData.surfaceY = maxY; // maxY já está em pixels
    }

    @Override
    public void update(float delta) {
        super.update(delta);

//        checkRoomObjectLiquidInteraction();
    }

    private void checkRoomObjectLiquidInteraction(){
        List<BaseRoomGameObject> objList = ownerRoom.roomGameObjectList;
        currentInsideBuffer.clear();

        // Coleta quem está dentro AGORA
        for (int i = 0; i < objList.size(); i++) {
            BaseRoomGameObject obj = objList.get(i);

            // evita dupla detecção no mesmo frame
            if (obj instanceof MultiLiquidInteractableObjectII) {
                processCompositeObject((MultiLiquidInteractableObjectII) obj, currentInsideBuffer);
            } else if (obj instanceof SimpleLiquidInteractableObjectII) {
                processSimpleObject((SimpleLiquidInteractableObjectII) obj, currentInsideBuffer);
            }
        }

        // Detecta ENTRADAS
        for (SimpleLiquidInteractableObjectII obj : currentInsideBuffer) {
            if (!insideSet.contains(obj)) {
                obj.getLiquidInteractionC().addLiquid(liquidData);
            }
        }

        // Detecta SAÍDAS
        for (SimpleLiquidInteractableObjectII obj : insideSet) {
            if (!currentInsideBuffer.contains(obj)) {
                obj.getLiquidInteractionC().removeLiquid(liquidData);
            }
        }

        // Replace: insideSet vira cópia de currentInsideBuffer
        insideSet.clear();
        insideSet.addAll(currentInsideBuffer);
    }

    /// Processa objeto simples (implementa LiquidInteractableObjectII)
    private void processSimpleObject(SimpleLiquidInteractableObjectII obj, Set<SimpleLiquidInteractableObjectII> currentInside) {
        TransformComponent t = obj.getTransformC();
        if (t == null) return;

        if (isInsideLiquid(t)) {
            currentInside.add(obj);
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
            }
        }
    }

    /// AABB simples: detecta overlap entre objeto e qualquer região do líquido
    private boolean isInsideLiquid(TransformComponent t) {
        float x = t.x;
        float y = t.y;
        float w = t.width;
        float h = t.height;

        // early reject (ganho grande)
        if (x + w < minX || x > maxX || y + h < minY || y > maxY) {
            return false;
        }

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
        regionList.clear();
        fixtureList.clear();
        if (liquidBody != null) {
            liquidBody.getWorld().destroyBody(liquidBody);
            liquidBody = null;
        }
        liquidData = null;
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

            if(!DebugConstants.show_hit_boxes) return;
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
