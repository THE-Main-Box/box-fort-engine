package official.sketchBook.engine.projectile_related.util;

import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.projectile_related.models.BaseProjectile;
import official.sketchBook.engine.projectile_related.pool.GlobalProjectilePool;

public class Emitter implements Disposable {

    private GlobalProjectilePool globalPool;
    private Class<? extends BaseProjectile> projectileType;

    private boolean configured = false;
    private boolean disposed = false;

    public Emitter(GlobalProjectilePool globalPool) {
        this.globalPool = globalPool;
    }

    public void configure(Class<? extends BaseProjectile> projectileType) {
        this.projectileType = projectileType;
        this.configured = this.projectileType != null;
    }

    /// Obtém um projétil
    public BaseProjectile obtain() {
        if (!configured || globalPool == null) {
            return null;
        }

        return globalPool.returnProjectileRequested(projectileType);
    }


    @Override
    public void dispose() {
        if (disposed) return;

        globalPool = null;
        projectileType = null;

        disposed = true;
    }
}
