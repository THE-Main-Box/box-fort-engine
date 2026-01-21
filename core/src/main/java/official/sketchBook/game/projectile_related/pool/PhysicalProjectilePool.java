package official.sketchBook.game.projectile_related.pool;

import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.dataManager_related.util.RenderableObjectManager;
import official.sketchBook.engine.projectile_related.models.BaseProjectile;
import official.sketchBook.game.projectile_related.factories.ProjectileFactory;
import official.sketchBook.engine.projectile_related.models.PhysicalProjectile;

public class PhysicalProjectilePool<T extends PhysicalProjectile> extends ProjectilePool<T> {
    private World world;

    public PhysicalProjectilePool(
        RenderableObjectManager renderTree,
        World world,
        Class<T> projectileType
    ) {
        super(
            renderTree,
            projectileType
        );
        this.world = world;
    }

    @Override
    protected T newObject() {
        return ProjectileFactory.createByType(projectileType, this, world);
    }

    @Override
    protected void nullifyReferences() {
        super.nullifyReferences();
        world = null;
    }

    public World getWorld() {
        return world;
    }
}
