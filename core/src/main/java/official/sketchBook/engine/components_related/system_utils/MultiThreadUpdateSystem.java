package official.sketchBook.engine.components_related.system_utils;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.UpdateSystem;
import official.sketchBook.engine.dataManager_related.BaseWorldDataManager;
import official.sketchBook.engine.screen_related.BaseScreen;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static official.sketchBook.game.util_related.constants.PhysicsC.FIXED_TIMESTAMP;
import static official.sketchBook.game.util_related.constants.PhysicsC.MAX_ACCUMULATOR;

public class MultiThreadUpdateSystem implements UpdateSystem {

    /// Thread pool (1 thread para updates)
    private final ExecutorService updateExecutor;

    /// Sincronização entre render e update threads
    private volatile CountDownLatch updateLatch;

    /// Acumulador (compartilhado, precisa ser sincronizado)
    private volatile float accumulator = 0;
    private volatile int updates = 0;

    private final BaseWorldDataManager worldManager;
    private final BaseScreen screen;

    /// Lock para proteger acumulador
    private final Object accumulatorLock = new Object();

    public MultiThreadUpdateSystem(
        BaseWorldDataManager worldManager,
        BaseScreen screen
    ) {
        this.worldManager = worldManager;
        this.screen = screen;

        // Cria thread pool com 1 thread para updates
        this.updateExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "GameUpdateThread");
            thread.setDaemon(false);
            return thread;
        });
    }

    @Override
    public void update(float delta) {
        // Cria latch para aguardar update terminar
        updateLatch = new CountDownLatch(1);

        // Envia tarefa para thread de update
        updateExecutor.submit(() -> {
            try {
                performUpdate(delta);
            } finally {
                // Sinaliza que update terminou
                updateLatch.countDown();
            }
        });

        // IMPORTANTE: Render thread aguarda aqui
        try {
            updateLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Update thread foi interrompida!");
        }
    }

    /// Realiza o update propriamente dito (em thread separada)
    private void performUpdate(float delta) {
        synchronized (accumulatorLock) {
            accumulator += Math.min(delta, MAX_ACCUMULATOR);

            while (accumulator >= FIXED_TIMESTAMP) {
                // Atualiza o mundo se existir
                if (worldManager != null) {
                    worldManager.update(FIXED_TIMESTAMP);
                }

                // Atualiza a screen (em thread de update)
                screen.updateScreen(FIXED_TIMESTAMP);

                // Subtrai do acumulador
                accumulator -= FIXED_TIMESTAMP;
                updates++;
            }
        }
    }

    @Override
    public void postUpdate() {
        // PostUpdate deve rodar na thread principal (render)
        // ou ser sincronizado adequadamente

        synchronized (accumulatorLock) {
            if (worldManager != null) {
                worldManager.postUpdateGameObjects();
            }

            // Pós atualização da screen
            screen.postScreenUpdate();
        }
    }

    public BaseWorldDataManager getWorldManager() {
        return worldManager;
    }

    public int getUpdatesMetric() {
        return updates;
    }

    @Override
    public void resetUpdateMetric() {
        this.updates = 0;
    }

    @Override
    public void dispose() {
        // 1. Encerramos a thread de processamento primeiro
        // Isso impede que novas tarefas de update sejam iniciadas
        shutdown();

        // 2. Limpamos o WorldManager
        // Como o Manager já tem proteção contra double-dispose, é seguro chamar aqui
        if (worldManager != null) {
            worldManager.dispose();
        }

        // 3. Limpeza de sincronização
        // Garantimos que o Latch não deixe nenhuma thread de renderização travada
        if (updateLatch != null) {
            while (updateLatch.getCount() > 0) {
                updateLatch.countDown();
            }
        }

        System.out.println("MultiThreadUpdateSystem: Recursos internos liberados e Thread encerrada.");
    }

    public boolean hasWorldManager() {
        return worldManager != null;
    }

    /// Shutdown seguro da thread (Melhorado para ser chamado internamente ou externamente)
    public void shutdown() {
        if (updateExecutor.isShutdown()) return;

        updateExecutor.shutdown();
        try {
            // Aguarda as tarefas pendentes terminarem por um breve período
            if (!updateExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                updateExecutor.shutdownNow(); // Força o encerramento se demorar demais
            }
        } catch (InterruptedException e) {
            updateExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
