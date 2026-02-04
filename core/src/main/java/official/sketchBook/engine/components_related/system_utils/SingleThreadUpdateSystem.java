package official.sketchBook.engine.components_related.system_utils;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.UpdateSystem;
import official.sketchBook.engine.dataManager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.screen_related.BaseScreen;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.FIXED_TIMESTAMP;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.MAX_ACCUMULATOR;

public class SingleThreadUpdateSystem implements UpdateSystem {
    private float accumulator = 0;
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
        accumulator += Math.min(delta, MAX_ACCUMULATOR);

        while (accumulator >= FIXED_TIMESTAMP) {
            // Atualiza o mundo se existir
            if (gameObjectManager != null) {
                gameObjectManager.update(FIXED_TIMESTAMP);
                gameObjectManager.postUpdate();
            }

            //Atualiza a screen
            screen.updateScreen(delta);

            // Subtrai do acumulador
            accumulator -= FIXED_TIMESTAMP;
            updates++;
        }

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
        if(gameObjectManager != null){
            gameObjectManager.dispose();
            gameObjectManager.disposeGraphics();
        }
    }

    public boolean hasWorldManager() {
        return gameObjectManager != null;
    }
}
