package official.sketchBook.game.dataManager_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.dataManager_related.BaseWorldDataManager;
import official.sketchBook.engine.world_gen.PlayableRoom;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.game.util_related.constants.RenderingC.TILES_VIEW_HEIGHT;
import static official.sketchBook.game.util_related.constants.RenderingC.TILES_VIEW_WIDTH;

public class WorldDataManager extends BaseWorldDataManager {

    private PlayableRoom currentRoom;

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
        currentRoom = new PlayableRoom(
            physicsWorld
        );

        currentRoom.initRoomGrid(
            initBaseTileMap()
        );

        currentRoom.insertNewTile(
            1,
            1
        );

        currentRoom.createTileBodies();


//        currentRoom.printMatrixContent(
//            currentRoom.getGrid()
//        );
//
//        currentRoom.printMatrixContent(
//            currentRoom.getBodyIdGrid()
//        );

    }

    @Override
    public void render(SpriteBatch batch) {
        super.render(batch);
    }

    private int[][] initBaseTileMap() {
//        TILES_IN_WIDTH = 39;
//        TILES_IN_HEIGHT = 21;

        int[][] toReturn = new int[TILES_VIEW_HEIGHT][TILES_VIEW_WIDTH];

        for (int y = 0; y < TILES_VIEW_HEIGHT; y++) {
            for (int x = 0; x < TILES_VIEW_WIDTH; x++) {
                toReturn[y][x] = 0;

                List<Boolean> canCreate = new ArrayList<>();
                canCreate.add(y >= TILES_VIEW_HEIGHT - 4); // chão
                canCreate.add(y == 0); //teto
                canCreate.add(x == 0);//parede esquerda
                canCreate.add(x == TILES_VIEW_WIDTH - 1); // parede direita
                canCreate.add(x == TILES_VIEW_WIDTH - 20 && y >= TILES_VIEW_HEIGHT - 8);//parede de testes


                for (boolean value : canCreate) {
                    if (value) {
                        toReturn[y][x] = 1;
                        break;
                    }
                }

            }
        }

        return toReturn;
    }

    @Override
    protected void onManagerDestruction() {
        System.out.println("destruindo manager");
    }

    @Override
    protected void disposeGeneralData() {
        currentRoom.dispose();
    }
}
