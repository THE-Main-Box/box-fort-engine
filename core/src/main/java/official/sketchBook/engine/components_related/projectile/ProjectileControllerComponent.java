package official.sketchBook.engine.components_related.projectile;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.projectile_related.models.BaseProjectile;

public class ProjectileControllerComponent implements Component {
    protected BaseProjectile projectile;

    private boolean disposed = false;

    public ProjectileControllerComponent(BaseProjectile projectile) {
        this.projectile = projectile;
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void postUpdate() {

    }

    @Override
    public void dispose() {
        if(disposed) return;
        nullifyReferences();
        disposed = true;
    }

    @Override
    public void nullifyReferences() {
        projectile = null;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
