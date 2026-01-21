package official.sketchBook.game.projectile_related.factories;

import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.projectile_related.models.BaseProjectile;
import official.sketchBook.game.projectile_related.pool.ProjectilePool;

public class ProjectileFactory {

    /**
     * Intancia um projétil com base no genérics passado sobre ele
     *
     * @param type tipo do projétil que iremos instanciar
     * @param pool pool dona do projétil
     */
    public static <T extends BaseProjectile> T createByType(
        Class<T> type,
        ProjectilePool<T> pool
    ) {
        try {
            return type.getConstructor(
                ProjectilePool.class
            ).newInstance(pool);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao instanciar projétil do tipo: " + type.getSimpleName(), e);
        }
    }

    public static <T extends BaseProjectile> T createByType(
        Class<T> type,
        ProjectilePool<T> pool,
        World world
    ) {
        try {
            return type
                .getConstructor(ProjectilePool.class, World.class)
                .newInstance(pool, world);
        } catch (Exception e) {
            throw new RuntimeException(
                "Falha ao instanciar projétil do tipo: " + type.getSimpleName(),
                e
            );
        }
    }


}
