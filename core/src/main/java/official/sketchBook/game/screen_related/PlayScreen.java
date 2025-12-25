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
import official.sketchBook.engine.camera_related.utils.CameraUtils;
import official.sketchBook.engine.components_related.system_utils.MultiThreadUpdateSystem;
import official.sketchBook.engine.components_related.system_utils.SingleThreadRenderSystem;
import official.sketchBook.engine.components_related.system_utils.SingleThreadUpdateSystem;
import official.sketchBook.engine.screen_related.BaseScreen;
import official.sketchBook.game.dataManager_related.WorldDataManager;
import official.sketchBook.game.gameObject_related.Player;

import static official.sketchBook.game.util_related.constants.DebugC.show_fps_ups_metrics;
import static official.sketchBook.game.util_related.constants.PhysicsC.*;

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

        this.uiCameraManager = CameraUtils.createScreenCamera();
        this.gameCameraManager = CameraUtils.createScreenCamera();

        this.font = new BitmapFont();
        this.font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        this.worldManager = new WorldDataManager(
            new World(
                new Vector2(),
                true
            ),
            FIXED_TIMESTAMP,
            VELOCITY_ITERATIONS,
            POSITION_ITERATIONS
        );

        this.renderSystem = new SingleThreadRenderSystem(
            this,
            this.app.gameBatch,
            this.app.uiBatch
        );

        this.updateSystem = new SingleThreadUpdateSystem(
            worldManager,
            this
        );

        player = new Player(
            worldManager
        );
    }

    @Override
    public void updateScreen(float delta) {
        if(Gdx.input.isKeyPressed(
            Input.Keys.ESCAPE
        )){
            player.destroy();
            worldManager.destroyManager();
        }
    }

    @Override
    public void postScreenUpdate() {

    }

    @Override
    public void updateVisuals(float delta) {
        uiCameraManager.getCamera().update();
        gameCameraManager.getCamera().update();
    }


    @Override
    public void drawGame(SpriteBatch batch) {
        // Aplica a câmera do JOGO
        batch.setProjectionMatrix(gameCameraManager.getCamera().combined);

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
