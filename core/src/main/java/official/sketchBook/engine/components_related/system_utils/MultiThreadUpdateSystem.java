package official.sketchBook.engine.components_related.system_utils;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.UpdateSystem;
import official.sketchBook.engine.dataManager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.screen_related.BaseScreen;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.FIXED_TIMESTAMP;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.MAX_ACCUMULATOR;

public class MultiThreadUpdateSystem implements UpdateSystem, Runnable {
    private float accumulator = 0f;
    private float lastRealDelta = 0f;  // ← Rastreia o delta real do último frame
    private int updates = 0;

    private final BaseGameObjectDataManager gameObjectManager;
    private final BaseScreen screen;

    private final Thread updateThread;
    private volatile boolean running = true;

    // Flag que garante que render thread não começa até update terminar
    private volatile boolean updateComplete = false;

    private float pendingDelta = 0f;
    private boolean hasWork = false;

    public MultiThreadUpdateSystem(
            BaseGameObjectDataManager gameObjectManager,
            BaseScreen screen
    ) {
        this.gameObjectManager = gameObjectManager;
        this.screen = screen;

        this.updateThread = new Thread(this, "Update-System-Thread");
        this.updateThread.setDaemon(true);
        this.updateThread.start();
    }

    /**
     * Main thread: Chamada a cada frame do render.
     * Sinaliza o work thread e ESPERA por ele completar.
     */
    @Override
    public void update(float delta) {
        // Sinaliza que tem delta para processar
        synchronized (this) {
            this.pendingDelta = delta;  // ← Armazena o delta REAL deste frame
            this.hasWork = true;
            this.notify();
        }

        // CRÍTICO: Espera o update thread completar antes de render começar
        // Isso evita race conditions
        waitForUpdateComplete();
    }

    /**
     * Main thread: Espera o update thread sinalizar que terminou.
     */
    private void waitForUpdateComplete() {
        synchronized (this) {
            while (!updateComplete) {
                try {
                    this.wait();
                } catch (InterruptedException ignored) {
                }
            }
            // Reseta para o próximo ciclo
            updateComplete = false;
        }
    }

    /**
     * Update thread: Roda em thread separada, executando toda a lógica.
     */
    @Override
    public void run() {
        while (running) {
            float delta;

            // Aguarda ter work
            synchronized (this) {
                while (!hasWork && running) {
                    try {
                        this.wait();
                    } catch (InterruptedException ignored) {
                    }
                }

                // Pega o delta pendente e marca como processado
                delta = pendingDelta;
                pendingDelta = 0f;
                hasWork = false;
            }

            // Executa o update (fora do synchronized para não bloquear main thread)
            executeUpdate(delta);

            // Sinaliza que terminou - main thread pode renderizar agora
            synchronized (this) {
                updateComplete = true;
                this.notifyAll();
            }
        }
    }

    /**
     * Update thread: Executa toda a lógica de update.
     * <p>
     * IMPORTANTE: Replica a lógica exata do SingleThreadUpdateSystem
     * - Acumula delta para física com fixed timestep
     * - Usa delta REAL para screen logic
     */
    private void executeUpdate(float delta) {
        // Limita o delta acumulado para evitar "jumps" grandes
        accumulator += Math.min(delta, MAX_ACCUMULATOR);

        // Fixed timestep loop para física
        while (accumulator >= FIXED_TIMESTAMP) {
            // Atualiza física e movimento com FIXED_TIMESTAMP
            if (isWorldManagerAccessible()) {
                gameObjectManager.update(FIXED_TIMESTAMP);
                gameObjectManager.postUpdate();
            }

            // Atualiza lógica de screen com delta REAL
            // (mesmo delta que seria usado se não fosse multi-frame update)
            screen.updateScreen(delta);

            // Subtrai do acumulador
            accumulator -= FIXED_TIMESTAMP;
            updates++;
        }

        // Rastreia o delta real para possível uso futuro
        this.lastRealDelta = delta;
    }

    @Override
    public int getUpdatesMetric() {
        return updates;
    }

    @Override
    public void resetUpdateMetric() {
        updates = 0;
    }

    @Override
    public void dispose() {
        running = false;

        // Acorda a thread de update se estiver dormindo
        synchronized (this) {
            this.notify();
        }

        // Aguarda ela terminar completamente
        syncThreadToMain();

        // Dispose dos recursos
        executeDispose();
    }

    private void executeDispose() {
        if (isWorldManagerAccessible()) {
            gameObjectManager.dispose();
        }
    }

    private void syncThreadToMain() {
        try {
            updateThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isWorldManagerAccessible() {
        return this.gameObjectManager != null;
    }

    public float getLastRealDelta() {
        return lastRealDelta;
    }

}
