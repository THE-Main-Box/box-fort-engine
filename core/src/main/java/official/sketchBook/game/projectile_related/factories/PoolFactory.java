package official.sketchBook.game.projectile_related.factories;

import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.dataManager_related.util.RenderableObjectManager;
import official.sketchBook.engine.projectile_related.models.BaseProjectile;
import official.sketchBook.engine.projectile_related.models.PhysicalProjectile;
import official.sketchBook.engine.projectile_related.pool.GlobalProjectilePool;
import official.sketchBook.game.projectile_related.model.Bullet;
import official.sketchBook.game.projectile_related.pool.PhysicalProjectilePool;

import java.util.HashMap;
import java.util.Map;

public class PoolFactory {

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
