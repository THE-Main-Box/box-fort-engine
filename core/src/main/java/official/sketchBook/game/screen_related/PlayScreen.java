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
import official.sketchBook.engine.world_gen.model.PlayableRoom;
import official.sketchBook.game.dataManager_related.GameObjectDataManager;
import official.sketchBook.game.gameObject_related.Player;

import static official.sketchBook.game.util_related.constants.DebugC.*;
import static official.sketchBook.game.util_related.constants.PhysicsC.*;
import static official.sketchBook.game.util_related.constants.RenderingC.*;

public class PlayScreen extends BaseScreen {
    private OrthographicCameraManager uiCameraManager;
    private OrthographicCameraManager gameCameraManager;
    private BitmapFont font;

    private GameObjectDataManager worldManager;

    public PlayScreen(AppMain app) {
        super(app);
    }

    @Override
    protected void initSystems() {
        super.initSystems();

        //Cria as câmeras
        this.uiCameraManager = new OrthographicCameraManager(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );

        this.gameCameraManager = new OrthographicCameraManager(
            VIRTUAL_WIDTH_PX,
            VIRTUAL_HEIGHT_PX
        );

        this.font = new BitmapFont();
        this.font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        //Cria o manager do mundo
        this.worldManager = new GameObjectDataManager(
            new World(
                new Vector2(
                    0,
                    -80f
                ),
                true
            ),
            FIXED_TIMESTAMP,
            VELOCITY_ITERATIONS,
            POSITION_ITERATIONS
        );

        //AGORA passa a câmera ao manager (que já tem seus limites configurados)
        worldManager.setGameCamera(gameCameraManager);

        gameCameraManager.setCameraOffsetLimit(
            -30,
            worldManager.getCurrentRoom().roomWidthPx / 2,
            -30,
            worldManager.getCurrentRoom().roomHeightPx / 2
        );

        updateZoom(0.5f);
        this.gameCameraManager.setZoom(zoom);

        //Cria os sistemas de render e update
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

        //Cria o jogador principal e Informa ao manager qual é o jogador principal
        worldManager.mainPlayer = new Player(
            worldManager,
            worldManager.getCurrentRoom(),
            250,
            40,
            0,
            0,
            16,
            16,
            1f,
            1,
            false,
            false
        );
    }


    @Override
    public void updateScreen(float delta) {
        if (Gdx.input.isKeyPressed(
            Input.Keys.ESCAPE
        )) {
//            PlayableRoom room = new PlayableRoom(
//                0,
//                0,
//                0,
//                worldManager.getPhysicsWorld()
//            );
//
//            worldManager.setCurrentRoom(
//                room
//            );

            worldManager.destroyManager();
//            worldManager.removeGameObject(worldManager.mainPlayer);
        }

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

        Player player = worldManager.mainPlayer;

        if (Gdx.input.isKeyPressed(
            Input.Keys.F
        )) {
            new Player(
                worldManager,
                worldManager.getCurrentRoom(),
                player.getTransformC().x + 50,
                player.getTransformC().y,
                player.getTransformC().z + 1,
                0,
                player.getTransformC().width,
                player.getTransformC().height,
                player.getTransformC().getScaleX(),
                player.getTransformC().getScaleY(),
                player.getTransformC().mirrorX,
                player.getTransformC().mirrorY

            );
        }

    }

    @Override
    public void updateVisuals(float delta) {

    }


    @Override
    public void drawGame(SpriteBatch batch) {
        //Tentamos desenhar as hitboxes
        if (show_hit_boxes && worldManager.isPhysicsWorldExists()) {
            worldManager.renderWorldHitboxes(
                gameCameraManager.getCamera()
            );
        }
    }

    @Override
    public void drawUI(SpriteBatch batch) {

        if (show_fps_ups_metrics) {
            font.draw(                              //Mostra o fps
                batch,
                "FPS: " + getFps(),
                10,
                this.screenHeightInPx - 10
            );
            font.draw(                              //Mostra o ups
                batch,
                "UPS: " + getUps(),
                10,
                this.screenHeightInPx - 30
            );
        }

        if (show_active_game_objects) {
            font.draw(                              //Mostra a quantidade de objetos ativos
                batch,
                "Objects: " + worldManager.getGameObjectList().size(),
                10,
                this.screenHeightInPx - 50
            );
        }
    }

    @Override
    public void show() {

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
