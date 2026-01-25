package official.sketchBook.engine.components_related.projectile;

import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.system_utils.ComponentManagerComponent;
import official.sketchBook.engine.projectile_related.models.BaseProjectile;
import official.sketchBook.engine.projectile_related.util.CollisionDataBuffer;
import official.sketchBook.engine.util_related.enumerators.Direction;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

public class ProjectileControllerComponent implements Component {
    protected BaseProjectile projectile;

    /// Flags de configuração de projétil
    public boolean
        moveOnStart,            //Se devemos aplicar a movimentação no projétil ao ativar ele
        useTrajectory,          //Se devemos usar cálculos de trajetória para determinar a movimentação
        continuousDetection;    //Se a detecção de colisão é contínua

    /// Flags de estado auxiliares
    protected boolean colliding;

    public CollisionDataBuffer
        lastCollisionEndBuffer,       //Buffer da última saída de colisão
        lastCollisionStartBuffer;     //Buffer da última entrada de colisão

    private boolean disposed = false;
    private boolean reset = false;

    private ComponentManagerComponent managerC;
    private ProjectileMovementLockComponent lockC;

    public ProjectileControllerComponent(BaseProjectile projectile) {
        this.projectile = projectile;

        this.lastCollisionStartBuffer = new CollisionDataBuffer();
        this.lastCollisionEndBuffer = new CollisionDataBuffer();

        this.managerC = new ComponentManagerComponent();

        this.lockC = new ProjectileMovementLockComponent(
            this,
            projectile.getMoveC()
        );

        managerC.add(
            lockC,
            true,
            false
        );

    }

    @Override
    public void update(float delta) {
        managerC.update(delta);
    }


    @Override
    public void postUpdate() {
        managerC.postUpdate();
    }

    public void start() {
        this.reset = false;
    }

    public void reset() {
        if (reset) return;

        lastCollisionStartBuffer.reset();
        lastCollisionEndBuffer.reset();

        this.colliding = false;

        this.reset = true;
    }

    public void markStartOfCollision(
        GameObjectTag targetTag,
        Direction collDir,
        Vector2 selfPos,
        Vector2 targetPos,
        Vector2 collisionNormal
    ) {
        this.lastCollisionStartBuffer.buff(
            targetTag,
            collDir,
            selfPos,
            targetPos,
            collisionNormal
        );

        this.lastCollisionEndBuffer.reset();

        this.colliding = true;

        this.projectile.onCollisionDetection();
    }

    public void markEndOfCollision(
        GameObjectTag targetTag,
        Direction collDir,
        Vector2 selfPos,
        Vector2 targetPos,
        Vector2 collisionNormal
    ) {
        this.lastCollisionEndBuffer.buff(
            targetTag,
            collDir,
            selfPos,
            targetPos,
            collisionNormal
        );

        this.lastCollisionStartBuffer.reset();

        this.colliding = false;

        this.projectile.onEndCollisionDetection();
    }

    @Override
    public void dispose() {
        if (!reset || disposed) return;
        managerC.dispose();
        nullifyReferences();
        disposed = true;
    }

    @Override
    public void nullifyReferences() {
        projectile = null;
        managerC = null;
        lockC = null;

        lastCollisionStartBuffer = null;
        lastCollisionEndBuffer = null;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    public boolean isColliding() {
        return colliding;
    }

    public boolean isContinuousDetection() {
        return continuousDetection;
    }

    public ProjectileMovementLockComponent getLockC() {
        return lockC;
    }
}
