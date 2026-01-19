package official.sketchBook.engine.projectile_related;

import com.badlogic.gdx.physics.box2d.World;

public class PhysicalProjectilePool<T extends PhysicalProjectile> extends ProjectilePool<T>{
    private World world;

    public PhysicalProjectilePool(Class<T> projectileType) {
        super(projectileType);
    }


    public World getWorld() {
        return world;
    }
}
