package official.sketchBook.engine.projectile_related.pool;

import official.sketchBook.engine.components_related.objects.TimerComponent;
import official.sketchBook.engine.projectile_related.models.BaseProjectile;
import official.sketchBook.game.projectile_related.pool.ProjectilePool;

import java.util.HashMap;
import java.util.Map;

import static official.sketchBook.game.util_related.constants.WorldConstants.ProjectilePoolConstants.POOL_CLEAN_INTERVAL_S;
import static official.sketchBook.game.util_related.constants.WorldConstants.ProjectilePoolConstants.POOL_REMOVE_INTERVAL_S;

public class GlobalProjectilePool {
    /// Mapa de pools de projéteis por tipo
    private final Map<Class<? extends BaseProjectile>, ProjectilePool<? extends BaseProjectile>> poolMap;
    /// Mapa de criadores de pools
    private Map<Class<? extends BaseProjectile>, ProjectilePoolFactory<?>> factories;

    /// Tempo de limpeza de pools
    private final TimerComponent poolCleanTimer;

    /// Tempo de destruição de pools
    private final TimerComponent poolDeleteTimer;

    private boolean disposed = false;

    public GlobalProjectilePool() {
        poolMap = new HashMap<>();
        factories = new HashMap<>();

        this.poolCleanTimer = new TimerComponent(POOL_CLEAN_INTERVAL_S);
        this.poolCleanTimer.start();

        this.poolDeleteTimer = new TimerComponent(POOL_REMOVE_INTERVAL_S);
        this.poolDeleteTimer.start();
    }

    public void update(float delta) {
        if (poolCleanTimer.isFinished()) {
            //limpa pool e reseta o temporizador
            cleanPools();
            poolCleanTimer.reset();
        }

        if (poolDeleteTimer.isFinished()) {
            //Deleta as pools inativas e reseta o temporizador
            deleteEmptyPools();
            poolDeleteTimer.reset();
        }

        poolDeleteTimer.update(delta);
        poolCleanTimer.update(delta);
    }

    /// Limpa os projéteis inativos de todas as pools
    private void cleanPools() {
        //Converte para uma array para não termos que usar um iterator
        Object[] pools = poolMap.values().toArray();
        for (int i = 0; i < pools.length; i++) {
            (
                (ProjectilePool<?>) pools[i]
            ).clear();
        }
    }

    /// Limpa todas as pools sem projéteis ativos ou inativos
    private void deleteEmptyPools() {

        poolMap.values().removeIf(
            pool ->
                pool.getActiveProjectiles().size == 0
                    && pool.getFreeCount() == 0
        );
    }

    /// Atualiza todos os projéteis ativos
    public void updatePoolProjectiles(float delta) {
        Object[] pools = poolMap.values().toArray();
        for (int i = 0; i < pools.length; i++) {
            (
                (ProjectilePool<?>) pools[i]
            ).updateActiveProjectiles(delta);
        }
    }

    /// Pós atualiza todos os projéteis ativos
    public void postUpdateProjectiles() {
        Object[] pools = poolMap.values().toArray();
        for (int i = 0; i < pools.length; i++) {
            (
                (ProjectilePool<?>) pools[i]
            ).postUpdateActiveProjectiles();
        }
    }

    /// Limpa todos os projéteis de todas as pools
    public void killAllPoolProjectiles() {
        Object[] pools = poolMap.values().toArray();
        for (int i = 0; i < pools.length; i++) {
            (
                (ProjectilePool<?>) pools[i]
            ).destroyAllProjectiles();
        }
    }

    /// Cria pool para o tipo especificado caso não exista
    @SuppressWarnings("unchecked")
    private <T extends BaseProjectile> ProjectilePool<T> createPoolIfAbsent(Class<T> type) {
        return (ProjectilePool<T>) poolMap.computeIfAbsent(
            type,
            t -> {
                ProjectilePoolFactory<T> factory =
                    (ProjectilePoolFactory<T>) factories.get(t);

                if (factory == null) {
                    throw new IllegalStateException("No pool factory registered for " + t);
                }

                return factory.create(type);
            }
        );
    }

    /// Retorna projétil requisitado, criando pool se necessário
    @SuppressWarnings("unchecked")
    public <T extends BaseProjectile> T returnProjectileRequested(Class<T> type) {
        ProjectilePool<T> pool = (ProjectilePool<T>) poolMap.get(type);

        if (pool != null && !pool.canSpawnNewProjectile()) return null;

        pool = createPoolIfAbsent(type); // só cria se necessário
        return pool.obtain();
    }

    public void dispose() {
        if (disposed) return;

        for (ProjectilePool<?> pool : poolMap.values()) {
            pool.dispose();
        }

        poolMap.clear();
        factories.clear();

        disposed = true;
    }

    public int getTotalInactiveProjectiles() {
        int value = 0;
        Object[] pools = poolMap.values().toArray();
        for (int i = 0; i < pools.length; i++) {
            value += ((ProjectilePool<?>) pools[i]).getFreeCount();
        }
        return value;
    }

    public int getTotalActiveProjectiles() {
        int value = 0;
        Object[] pools = poolMap.values().toArray();
        for (int i = 0; i < pools.length; i++) {
            value += ((ProjectilePool<?>) pools[i]).getActiveProjectiles().size;
        }
        return value;
    }

    public ProjectilePool<?> getPoolOf(Class<? extends BaseProjectile> projectile) {
        return poolMap.get(projectile);
    }

    public int getTotalPools() {
        return poolMap.size();
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void setFactories(Map<Class<? extends BaseProjectile>, ProjectilePoolFactory<?>> factories) {
        this.factories = factories;
    }

    public interface ProjectilePoolFactory<T extends BaseProjectile> {
        ProjectilePool<T> create(Class<T> type);
    }
}
