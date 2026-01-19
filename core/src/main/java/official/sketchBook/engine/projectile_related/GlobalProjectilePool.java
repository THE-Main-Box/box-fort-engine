package official.sketchBook.engine.projectile_related;

import official.sketchBook.engine.components_related.objects.TimerComponent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static official.sketchBook.game.util_related.constants.WorldConstants.ProjectilePoolConstants.POOL_CLEAN_INTERVAL_MS;
import static official.sketchBook.game.util_related.constants.WorldConstants.ProjectilePoolConstants.POOL_REMOVE_INTERVAL_MS;

public class GlobalProjectilePool {
    /// Mapa de pools de projéteis por tipo
    private final Map<Class<? extends BaseProjectile>, ProjectilePool<? extends BaseProjectile>> poolMap;
    /// Mapa de criadores de pools
    private final Map<Class<? extends BaseProjectile>, ProjectilePoolFactory<?>> factories;

    /// Tempo de limpeza de pools
    private final TimerComponent poolCleanTimer;

    /// Tempo de destruição de pools
    private final TimerComponent poolDeleteTimer;

    private boolean disposed = false;

    public GlobalProjectilePool() {
        poolMap = new HashMap<>();
        factories = new HashMap<>();

        this.poolCleanTimer = new TimerComponent(POOL_CLEAN_INTERVAL_MS);
        this.poolCleanTimer.start();

        this.poolDeleteTimer = new TimerComponent(POOL_REMOVE_INTERVAL_MS);
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
        for (ProjectilePool<? extends BaseProjectile> pool : poolMap.values()) {
            pool.clear();
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
    public void updatePoolProjectiles(float delta){
        for (ProjectilePool<?> pool : poolMap.values()) {
            pool.updateActiveProjectiles(delta);
        }
    }

    /// Pós atualiza todos os projéteis ativos
    public void postUpdateProjectiles(){
        for (ProjectilePool<?> pool : poolMap.values()) {
            pool.postUpdateActiveProjectiles();
        }
    }

    /// Limpa todos os projéteis de todas as pools
    public void killAllPoolProjectiles(){
        for (ProjectilePool<?> pool : poolMap.values()) {
            pool.destroyAllProjectiles();
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

    public void dispose(){
        if(disposed) return;
        for(ProjectilePool<?> pool : poolMap.values()){
            pool.dispose();
        }

        poolMap.clear();
        factories.clear();

        disposed = true;
    }

    public int getTotalInactiveProjectiles() {
        int value = 0;

        for (ProjectilePool<? extends BaseProjectile> pool : poolMap.values()) {
            value += pool.getFreeCount();//Para cada pool adicionamos os valores da suas listas ativas aqui dentro
        }

        return value;
    }

    public ProjectilePool<?> getPoolOf(Class<? extends BaseProjectile> projectile){
        return poolMap.get(projectile);
    }

    public int getTotalPools() {
        return poolMap.size();
    }

    public boolean isDisposed() {
        return disposed;
    }

    public <T extends BaseProjectile> void registerFactory(
        Class<T> type,
        ProjectilePoolFactory<T> factory
    ) {
        factories.put(type, factory);
    }


    public interface ProjectilePoolFactory<T extends BaseProjectile> {
        ProjectilePool<T> create(Class<T> type);
    }
}
