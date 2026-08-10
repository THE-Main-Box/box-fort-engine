package official.sketchBook.engine.util_related.serialization.base_context;

import official.sketchBook.engine.data_manager_related.BaseGameObjectDataManager;

public interface GameObjectContext<M extends BaseGameObjectDataManager> {

    GOContextData<M> getGameObjectContextData();

    class GOContextData<M extends BaseGameObjectDataManager> {
        public final M worldDataManager;

        public GOContextData(M worldDataManager) {
            this.worldDataManager = worldDataManager;
        }
    }
}
