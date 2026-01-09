package official.sketchBook.game.screen_related;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.AppMain;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;
import official.sketchBook.engine.components_related.system_utils.SingleThreadRenderSystem;
import official.sketchBook.engine.components_related.system_utils.SingleThreadUpdateSystem;
import official.sketchBook.engine.screen_related.BaseScreen;
import official.sketchBook.game.dataManager_related.WorldDataManager;
import official.sketchBook.game.gameObject_related.Player;
import official.sketchBook.game.util_related.body.world_gen.RoomBodyDataFactory;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.game.util_related.constants.DebugC.*;
import static official.sketchBook.game.util_related.constants.PhysicsC.*;
import static official.sketchBook.game.util_related.constants.RenderingC.*;

public class PlayScreen extends BaseScreen {
    private OrthographicCameraManager uiCameraManager;
    private OrthographicCameraManager gameCameraManager;
    private BitmapFont font;

    private WorldDataManager worldManager;

    private Player player;

    public PlayScreen(AppMain app) {
        super(app);
    }

    @Override
    protected void initSystems() {
        super.initSystems();
        this.uiCameraManager = new OrthographicCameraManager(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );

        this.gameCameraManager = new OrthographicCameraManager(
            VIRTUAL_WIDTH_PX,
            VIRTUAL_HEIGHT_PX
        );

        gameCameraManager.setCameraOffsetLimit(
            999999,
            999999
        );

        updateZoom(0.5f);
        this.gameCameraManager.setZoom(zoom);

        this.font = new BitmapFont();
        this.font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        this.worldManager = new WorldDataManager(
            new World(
                new Vector2(
                    0,
                    -80f),
                true
            ),
            FIXED_TIMESTAMP,
            VELOCITY_ITERATIONS,
            POSITION_ITERATIONS
        );

        this.renderSystem = new SingleThreadRenderSystem(
            this,
            worldManager,
            this.app.gameBatch,
            this.gameCameraManager.getCamera(),
            this.app.uiBatch,
            this.uiCameraManager.getCamera()
        );

        this.updateSystem = new SingleThreadUpdateSystem(
            worldManager,
            this
        );

        player = new Player(
            250,
            40,
            0,
            0,
            16,
            16,
            1f,
            1,
            false,
            false,
            worldManager
        );

        int[][] tmpMap = initBaseTileMap();
        RoomBodyDataFactory.createRoomBodies(
            tmpMap,
            worldManager.getPhysicsWorld()
        );

    }

    private int[][] initBaseTileMap() {
//        TILES_IN_WIDTH = 39;
//        TILES_IN_HEIGHT = 21;

        int[][] toReturn = new int[TILES_VIEW_HEIGHT][TILES_VIEW_WIDTH];

        for (int y = 0; y < TILES_VIEW_HEIGHT; y++) {
            for (int x = 0; x < TILES_VIEW_WIDTH; x++) {
                toReturn[y][x] = 0;

                List<Boolean> canCreate = new ArrayList<>();
                canCreate.add(y >= TILES_VIEW_HEIGHT - 2); // chão
                canCreate.add(y == 0); //teto
                canCreate.add(x == 0);//parede esquerda
                canCreate.add(x == TILES_VIEW_WIDTH - 1); // parede direita
                canCreate.add(x == TILES_VIEW_WIDTH - 10 && y >= TILES_VIEW_HEIGHT - 4);//parede de testes


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
    public void updateScreen(float delta) {
//        if (Gdx.input.isKeyPressed(
//            Input.Keys.ESCAPE
//        )) {
//            worldManager.destroyManager();
//        }

        if (change_of_zoom_allowed) {
            if (Gdx.input.isKeyPressed(
                Input.Keys.U
            )) {
                updateZoom(zoom - 0.1f);
                this.gameCameraManager.setZoom(zoom);
            }
            if (Gdx.input.isKeyPressed(
                Input.Keys.J
            )) {
                updateZoom(zoom + 0.1f);
                this.gameCameraManager.setZoom(zoom);
            }
        }

        if (player != null) {
            gameCameraManager.trackObjectByOffset(
                player.getTransformC().getCenterX(),
                player.getTransformC().getCenterY()
            );
        }

    }

    @Override
    public void updateVisuals(float delta) {

    }


    @Override
    public void drawGame(SpriteBatch batch) {
        if (show_hit_boxes && worldManager.isPhysicsWorldExists()) {
            worldManager.renderWorldHitboxes(
                gameCameraManager.getCamera()
            );
        }
    }

    @Override
    public void drawUI(SpriteBatch batch) {
        // Aplica a câmera da UI (Coordenadas de tela fixas)
        batch.setProjectionMatrix(uiCameraManager.getCamera().combined);

        if (show_fps_ups_metrics) {
            font.draw(batch, "FPS: " + getFps(), 10, this.screenHeightInPx - 10);
            font.draw(batch, "UPS: " + getUps(), 10, this.screenHeightInPx - 30);
        }
    }

    @Override
    public void show() {
        // Agora a PlayScreen é mostrada após o MenuScreen dar o comando
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        resizeUiCamera(uiCameraManager, width, height);
        gameCameraManager.updateViewport(width, height);
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
    }
}
