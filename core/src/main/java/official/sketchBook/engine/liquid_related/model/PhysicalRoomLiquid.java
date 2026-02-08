package official.sketchBook.engine.liquid_related.model;

import com.badlogic.gdx.physics.box2d.*;
import official.sketchBook.engine.dataManager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.gameObject_related.BaseRoomGameObject;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;
import official.sketchBook.engine.util_related.enumerators.CollisionLayers;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

import java.util.List;

import static official.sketchBook.engine.util_related.helper.CollisionBitImplantation.apply;
import static official.sketchBook.engine.util_related.helper.body.LiquidBodyCreatorHelper.createLiquidFixture;

public class PhysicalRoomLiquid extends BaseRoomGameObject implements ILiquid {

    private String name;

    private int id;

    private World world;

    private final float
        density,
        resistance,
        maxSinkSpeed,
        maxRiseSpeed;

    private Body body;

    private List<LiquidRegion> regionList;

    public PhysicalRoomLiquid(
        PhysicalGameObjectDataManager worldDataManager,
        PlayableRoom ownerRoom,
        String name,
        int id,
        float density,
        float resistance,
        float maxSinkSpeed,
        float maxRiseSpeed,
        List<LiquidRegion> regionList
    ) {
        super(
            worldDataManager,
            ownerRoom,
            RoomObjectScope.LOCAL
        );

        this.name = name;
        this.id = id;
        this.density = density;
        this.resistance = resistance;
        this.maxSinkSpeed = maxSinkSpeed;
        this.maxRiseSpeed = maxRiseSpeed;

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

    }

    @Override
    public String toString() {
        return "PhysicalRoomLiquid{" +
            "name='" + name + '\'' +
            ", id=" + id +
            ", density=" + density +
            ", resistance=" + resistance +
            ", maxSinkSpeed=" + maxSinkSpeed +
            ", maxRiseSpeed=" + maxRiseSpeed +
            '}';
    }

    @Override
    public float getDensity() {
        return density;
    }

    @Override
    public float getResistance() {
        return resistance;
    }

    @Override
    public float getMaxSinkSpeed() {
        return maxSinkSpeed;
    }

    @Override
    public float getMaxRiseSpeed() {
        return maxRiseSpeed;
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

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }
}
