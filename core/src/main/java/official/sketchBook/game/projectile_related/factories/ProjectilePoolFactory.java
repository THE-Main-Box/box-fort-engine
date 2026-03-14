package official.sketchBook.game.projectile_related.factories;

import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.data_manager_related.util.RenderableObjectManager;
import official.sketchBook.engine.projectile_related.models.BaseProjectile;
import official.sketchBook.engine.util_related.pools.GlobalProjectilePool;
import official.sketchBook.game.projectile_related.model.Bullet;
import official.sketchBook.game.util_related.pools.PhysicalProjectilePool;

import java.util.HashMap;
import java.util.Map;

public class ProjectilePoolFactory {

    public static Map<Class<? extends BaseProjectile>, GlobalProjectilePool.ProjectilePoolFactory<?>> factoryMap;

    static {
        factoryMap = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static void initPoolFactories(RenderableObjectManager renderTree, World physicsWorld){
        factoryMap.put(Bullet.class,
            type -> new PhysicalProjectilePool(renderTree, physicsWorld, type)
        );
    }

    public static void applyFactories(GlobalProjectilePool pool){
        pool.setFactories(factoryMap);
    }

}
