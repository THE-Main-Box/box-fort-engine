package official.sketchBook.engine.liquid_related.model;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.dataManager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.gameObject_related.BaseRoomGameObject;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

import java.util.List;

import static official.sketchBook.engine.util_related.helper.body.LiquidBodyCreatorHelper.createLiquidFixture;

public class PhysicalRoomLiquid extends BaseRoomGameObject implements ILiquid {

    private World world;

    private Body body;

    private List<LiquidRegion> regionList;

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
        bodyDef.type = BodyDef.BodyType.StaticBody;

        this.body = world.createBody(bodyDef);

        // Cria fixtures para cada região
        for (LiquidRegion region : regionList) {
            createLiquidFixture(
                region,
                body,
                this
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

        this.body = null;
        this.world = null;
        this.regionList = null;
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
