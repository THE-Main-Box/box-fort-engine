package official.sketchBook.engine.projectile_related;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.PPM;

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

    public BaseProjectile obtain(
        float x,
        float y,
        float rotation
    ) {
        if (!configured || globalPool == null) {
            return null;
        }

        BaseProjectile p = globalPool.returnProjectileRequested(projectileType);

        if (p == null) {
            return null;
        }

        p.getTransformC().x = x;
        p.getTransformC().y = y;
        p.getTransformC().rotation = rotation;

        if(p instanceof PhysicalObjectII){
            PhysicalObjectII pp = (PhysicalObjectII) p;
            pp.getBody().setTransform(
                p.getTransformC().x / PPM,
                p.getTransformC().y/ PPM,
                p.getTransformC().rotation
            );
        }

        return p;
    }


    @Override
    public void dispose() {
        if (disposed) return;

        globalPool = null;
        projectileType = null;

        disposed = true;
    }
}
