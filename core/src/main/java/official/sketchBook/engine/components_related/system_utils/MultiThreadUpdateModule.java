package official.sketchBook.engine.components_related.system_utils;

import official.sketchBook.engine.dataManager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.screen_related.BaseScreen;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.FIXED_TIMESTAMP;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.MAX_ACCUMULATOR;

public class MultiThreadUpdateModule implements Runnable {

    private float accumulator = 0f;
    private float lastRealDelta = 0f;
    private int updates = 0;

    // Referências (leitura apenas)
    private final BaseGameObjectDataManager gameObjectManager;
    private final BaseScreen screen;

    // Sincronização
    private volatile boolean running = false;
    private volatile boolean updateComplete = false;

    private float pendingDelta = 0f;
    private boolean hasWork = false;

    // Thread que este módulo gerencia
    private Thread workerThread;

    public MultiThreadUpdateModule(
        BaseGameObjectDataManager gameObjectManager,
        BaseScreen screen
    ) {
        this.gameObjectManager = gameObjectManager;
        this.screen = screen;
    }

    /**
     * Inicia o worker thread.
     * Deve ser chamado uma vez na inicialização.
     */
    public void startWorker() {
        if (running) {
            System.err.println("Worker já está rodando!");
            return;
        }

        running = true;
        this.workerThread = new Thread(this, "Update-Pipeline-Worker");
        this.workerThread.setDaemon(true);
        this.workerThread.start();
    }

    /**
     * Para o worker thread.
     * Deve ser chamado no dispose.
     */
    public void stopWorker() {
        if (!running) {
            System.err.println("Worker não está rodando!");
            return;
        }

        running = false;

        synchronized (this) {
            this.notify();
        }

        // Aguarda a thread terminar
        try {
            workerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Main thread: Sinaliza o worker para executar update com este delta.
     */
    public void executeUpdate(float delta) {
        synchronized (this) {
            this.pendingDelta = delta;
            this.hasWork = true;
            this.notify();
        }

        // ESPERA o worker completar
        waitForCompletion();
    }

    /**
     * Main thread: Espera o worker completar.
     */
    private void waitForCompletion() {
        synchronized (this) {
            while (!updateComplete) {
                try {
                    this.wait();
                } catch (InterruptedException ignored) {
                }
            }
            updateComplete = false;
        }
    }

    /**
     * Worker thread: Loop principal que roda em thread separada.
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

                // Se foi sinalizado para parar durante a espera
                if (!running) break;

                delta = pendingDelta;
                pendingDelta = 0f;
                hasWork = false;
            }

            // Executa a pipeline de update (fora de synchronized)
            executeUpdatePipeline(delta);

            // Sinaliza que terminou
            synchronized (this) {
                updateComplete = true;
                this.notifyAll();
            }
        }
    }

    /**
     * Worker thread: Executa apenas a pipeline de update.
     * <p>
     * Sem acesso a dispose, add, remove - apenas lê e atualiza.
     */
    private void executeUpdatePipeline(float delta) {
        accumulator += Math.min(delta, MAX_ACCUMULATOR);

        // Fixed timestep loop para física
        while (accumulator >= FIXED_TIMESTAMP) {
            // Physics update
            if (gameObjectManager != null) {
                gameObjectManager.update(FIXED_TIMESTAMP);
                gameObjectManager.postUpdate();
            }


            accumulator -= FIXED_TIMESTAMP;
            updates++;
        }

        this.lastRealDelta = delta;
    }

    // ========== Getters para Métricas ==========

    public int getUpdatesCount() {
        return updates;
    }

    public void resetUpdatesCount() {
        updates = 0;
    }

    public float getLastRealDelta() {
        return lastRealDelta;
    }

    public boolean isRunning() {
        return running;
    }
}
