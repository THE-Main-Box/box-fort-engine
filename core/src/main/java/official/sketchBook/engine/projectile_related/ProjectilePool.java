package official.sketchBook.engine.projectile_related;

import official.sketchBook.engine.util_related.custom_utils.CustomPool;

public class ProjectilePool<T extends BaseProjectile> extends CustomPool<T> {

    protected Class<T> projectileType;

    @Override
    protected T newObject() {
        return ProjectileFactory.createByType(projectileType);
    }
}
