package official.sketchBook.engine.projectile_related;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import official.sketchBook.engine.util_related.custom_utils.CustomPool;

import static official.sketchBook.game.util_related.constants.WorldConstants.ProjectilePoolConstants.MAX_PROJECTILE_DESTRUCTION_PER_POOL;
import static official.sketchBook.game.util_related.constants.WorldConstants.ProjectilePoolConstants.MAX_PROJECTILE_PER_POOL;


public class ProjectilePool<T extends BaseProjectile> extends CustomPool<T> implements Disposable {

    protected Array<T> activeProjectiles;
    protected Class<T> projectileType;

    private boolean disposed;

    public ProjectilePool(Class<T> projectileType) {
        super(16, MAX_PROJECTILE_PER_POOL);
        this.projectileType = projectileType;
        this.activeProjectiles = new Array<>();
    }

    @Override
    protected T newObject() {
        return ProjectileFactory.createByType(projectileType, this);
    }

    /// faz com que todos os projéteis ativos sejam desativados
    public void releaseAllProjectiles() {
        //Percorre a lista de projéteis ativos de cima pra baixo
        for (int i = activeProjectiles.size - 1; i >= 0; i--) {
            free(activeProjectiles.get(i));
        }
    }

    /// Desativa e destroi todos os projéteis
    public void destroyAllProjectiles(){
        releaseAllProjectiles();
        destroyAllInactiveProjectiles();
    }

    /// Destróis todos os projéteis inativos em batchs de acordo com o limite
    public void destroyInactiveProjectilesInBatch(){
        int count = 0;
        //Percorre a lista de projéteis livres de cima pra baixo
        for (int i = getFreeCount() - 1; i >= 0 && count < MAX_PROJECTILE_DESTRUCTION_PER_POOL; i--) {
            //descarta o projétil, pois estamos destruindo
            discard(freeObjects.get(i));
            //Aumenta o contador para garantir que estejamos dentro dos limites
            count++;
        }
    }

    /// Destrói todos os projéteis inativos
    public void destroyAllInactiveProjectiles() {
        super.clear();
    }

    /// Removemos da lista de projéteis ativos e realocamos para dentro da lista de inativos
    @SuppressWarnings("unchecked")
    public void free(BaseProjectile projectile) {
        super.free((T) projectile);
        activeProjectiles.removeValue((T) projectile, true);
    }

    public void updateActiveProjectiles(float delta){
        for (T proj : activeProjectiles) {
            proj.update(delta);
        }
    }

    public void postUpdateActiveProjectiles(){
        for (T proj : activeProjectiles) {
            proj.postUpdate();
        }
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposeProjectiles();
        clearLists();
        disposed = true;
    }

    protected void disposeProjectiles(){
        destroyAllProjectiles();
    }
    protected void clearLists(){
        freeObjects.clear();
        activeProjectiles.clear();
    }

    @SuppressWarnings("unchecked")
    public void addToActive(BaseProjectile proj) {
        this.activeProjectiles.add((T) proj);
    }

    public boolean canSpawnNewProjectile() {
        return activeProjectiles.size < max;
    }

    public Array<T> getActiveProjectiles() {
        return activeProjectiles;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public Class<T> getProjectileType() {
        return projectileType;
    }
}
