package official.sketchBook.engine.components_related.system_utils;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.UpdateSystem;
import official.sketchBook.engine.data_manager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.screen_related.BaseScreen;

import static official.sketchBook.game.util_related.constants.GameConfigConstants.UPDATE_TIME_SCALE;

public class SingleThreadUpdateSystem implements UpdateSystem {

    /**
     * Contador fracionário acumulado por TIME_SCALE. Quando cruza 1.0,
     * dispara uma execução e desconta 1.0 — mesma ideia de um accumulator,
     * só que contando "quantidade de updates", não tempo. delta em si
     * nunca é alterado; cada execução recebe o delta real do LibGDX.
     */
    private float executionCredit = 0f;

    private int updates = 0;

    private final BaseGameObjectDataManager gameObjectManager;
    private final BaseScreen screen;

    public SingleThreadUpdateSystem(
        BaseGameObjectDataManager gameObjectManager,
        BaseScreen screen
    ) {
        this.gameObjectManager = gameObjectManager;
        this.screen = screen;
    }

    @Override
    public void update(float delta) {
        executionCredit += UPDATE_TIME_SCALE;

        // TIME_SCALE >= 1 (acelerado ou normal): pode disparar mais de
        // uma execução por chamada. TIME_SCALE < 1 (slowmotion): a
        // maioria das chamadas não acumula crédito suficiente e é pulada.
        while (executionCredit >= 1f) {
            executeUpdate(delta);
            executionCredit -= 1f;
        }
    }

    private void executeUpdate(float delta) {
        if (gameObjectManager != null) {
            gameObjectManager.update(delta);
            gameObjectManager.postUpdate();
        }

        screen.updateScreen(delta);
        updates++;
    }

    public BaseGameObjectDataManager getGameObjectManager() {
        return gameObjectManager;
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
        if (gameObjectManager != null) {
            gameObjectManager.dispose();
            gameObjectManager.disposeGraphics();
        }
    }

    public boolean hasWorldManager() {
        return gameObjectManager != null;
    }
}
