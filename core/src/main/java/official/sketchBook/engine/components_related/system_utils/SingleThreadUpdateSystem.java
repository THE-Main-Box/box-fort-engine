package official.sketchBook.engine.components_related.system_utils;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.UpdateSystem;
import official.sketchBook.engine.dataManager_related.BaseWorldDataManager;
import official.sketchBook.engine.screen_related.BaseScreen;

import static official.sketchBook.game.util_related.constants.PhysicsC.FIXED_TIMESTAMP;
import static official.sketchBook.game.util_related.constants.PhysicsC.MAX_ACCUMULATOR;

public class SingleThreadUpdateSystem implements UpdateSystem {
    private float accumulator = 0;
    private int updates = 0;
    private final BaseWorldDataManager worldManager;
    private final BaseScreen screen;

    public SingleThreadUpdateSystem(
        BaseWorldDataManager worldManager,
        BaseScreen screen
    ) {
        this.worldManager = worldManager;
        this.screen = screen;
    }
    @Override
    public void update(float delta) {
        accumulator += Math.min(delta, MAX_ACCUMULATOR);

        while (accumulator >= FIXED_TIMESTAMP) {
            // Atualiza o mundo se existir
            if (worldManager != null) {
                worldManager.update(FIXED_TIMESTAMP);
            }

            //Atualiza a screen
            screen.updateScreen(delta);

            // Subtrai do acumulador
            accumulator -= FIXED_TIMESTAMP;
            updates++;
        }

    }

    @Override
    public void postUpdate() {
        if(worldManager != null) {
            worldManager.postUpdateGameObjects();
        }

        //Pós atualização da screen
        screen.postScreenUpdate();
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
        if(worldManager != null){
            worldManager.dispose();
        }
    }

    public boolean hasWorldManager() {
        return worldManager != null;
    }
}
