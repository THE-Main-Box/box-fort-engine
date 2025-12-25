package official.sketchBook.game.dataManager_related;

import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.dataManager_related.BaseWorldDataManager;

public class WorldDataManager extends BaseWorldDataManager {

    public WorldDataManager(
        World physicsWorld,
        float timeStep,
        int velIterations,
        int posIterations
    ) {
        super(physicsWorld, timeStep, velIterations, posIterations);
    }

    @Override
    protected void setupSystems() {

    }

    @Override
    protected void onManagerDestruction() {
        System.out.println("destruindo manager");
    }
}
