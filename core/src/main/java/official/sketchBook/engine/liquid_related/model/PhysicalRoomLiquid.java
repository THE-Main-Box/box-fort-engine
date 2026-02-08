package official.sketchBook.engine.liquid_related.model;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.Disposable;
import official.sketchBook.engine.dataManager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.dataManager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.gameObject_related.BaseRoomGameObject;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;
import official.sketchBook.engine.util_related.enumerators.RoomObjectScope;
import official.sketchBook.engine.world_gen.model.PlayableRoom;

import java.util.ArrayList;
import java.util.List;

public class PhysicalRoomLiquid extends BaseRoomGameObject implements ILiquid {

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
        RoomObjectScope roomScope,
        float density,
        float resistance,
        float maxSinkSpeed,
        float maxRiseSpeed,
        List<LiquidRegion> regionList
    ) {
        super(
            worldDataManager,
            ownerRoom,
            roomScope
        );

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

}
