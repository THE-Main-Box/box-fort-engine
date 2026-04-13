package official.sketchBook.engine.liquid_related.model;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.LiquidInteractableObjectII;
import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.game_object_related.base_game_object.BaseRoomGameObject;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static official.sketchBook.engine.util_related.helper.body.LiquidBodyCreatorHelper.createLiquidFixture;

public class PhysicalRoomLiquid extends BaseRoomGameObject implements Liquid {

    private World world;

    private Body body;

    public final List<LiquidRegion> regionList;
    public final List<Fixture> fixtureList;

    private LiquidData liquidData;

    public PhysicalRoomLiquid(
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
        PhysicalGameObjectDataManager manager = getPhysicalManager();

        if (!manager.isPhysicsWorldExists()) return;

        this.world = manager.getPhysicsWorld();
        createBody();

    }

    private void createBody() {
        //Criamos uma body estatica
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;
        bodyDef.allowSleep = true;
        bodyDef.gravityScale = 0;

        this.body = world.createBody(bodyDef);

        // Cria fixtures para cada região
        for (LiquidRegion region : regionList) {
            fixtureList.add(
                createLiquidFixture(
                    region,
                    body,
                    this
                )
            );
        }
    }

    @Override
    protected void disposeGeneralData() {
        this.world.destroyBody(this.body);
    }

    @Override
    protected void disposeCriticalData() {
        super.disposeCriticalData();

        this.regionList.clear();
        this.fixtureList.clear();

        this.body = null;
        this.world = null;
        this.liquidData = null;
    }

    public PhysicalGameObjectDataManager getPhysicalManager() {
        return (PhysicalGameObjectDataManager) this.worldDataManager;
    }

    public Body getBody() {
        return body;
    }

    public List<LiquidRegion> getRegionList() {
        return regionList;
    }

    @Override
    public LiquidData getLiquidData() {
        return liquidData;
    }
}
