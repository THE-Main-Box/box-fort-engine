package official.sketchBook.engine.components_related.system_utils;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.UpdateSystem;
import official.sketchBook.engine.dataManager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.screen_related.BaseScreen;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.FIXED_TIMESTAMP;
import static official.sketchBook.game.util_related.constants.PhysicsConstants.MAX_ACCUMULATOR;


public class MultiThreadUpdateSystem implements UpdateSystem {
    private final BaseGameObjectDataManager gameObjectManager;

    private final MultiThreadUpdateModule threadModule;
    private final BaseScreen screen;

    public MultiThreadUpdateSystem(
            BaseGameObjectDataManager gameObjectManager,
            BaseScreen screen
    ) {
        this.gameObjectManager = gameObjectManager;
        this.screen = screen;

        this.threadModule = new MultiThreadUpdateModule(gameObjectManager, screen);
        this.threadModule.startWorker();
    }


    @Override
    public void update(float delta) {
        this.threadModule.executeUpdate(delta);
        if (screen != null) {
            screen.updateScreen(delta);
        }
    }

    @Override
    public int getUpdatesMetric() {
        return this.threadModule.getUpdatesCount();
    }

    @Override
    public void resetUpdateMetric() {
        this.threadModule.resetUpdatesCount();
    }

    /**
     * Main thread: Faz o dispose normalmente.
     * <p>
     * NÃO tenta fazer dispose de recursos OpenGL na thread secundária.
     * O manager fica responsável por seu próprio dispose (que acontece na main thread).
     */
    @Override
    public void dispose() {
        this.threadModule.stopWorker();
        if(isWorldManagerAccessible()){
            this.gameObjectManager.dispose();
        }
    }

    public boolean isWorldManagerAccessible() {
        return this.gameObjectManager != null;
    }

}
