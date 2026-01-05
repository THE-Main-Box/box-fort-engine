package official.sketchBook.engine.components_related.system_utils;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.RenderSystem;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderAbleObject;
import official.sketchBook.engine.dataManager_related.BaseWorldDataManager;
import official.sketchBook.engine.screen_related.BaseScreen;
import official.sketchBook.game.util_related.constants.DebugC;

import static official.sketchBook.game.util_related.constants.DebugC.show_hit_boxes;

public class SingleThreadRenderSystem implements RenderSystem {
    private final BaseScreen screen;
    private final BaseWorldDataManager worldManager;

    private final SpriteBatch gameBatch;
    private final SpriteBatch uiBatch;

    private final boolean renderGame;
    private final boolean renderUi;
    private final boolean worldManagerExists;

    public SingleThreadRenderSystem(
        BaseScreen screen,
        BaseWorldDataManager worldManager,
        SpriteBatch gameBatch,
        SpriteBatch uiBatch
    ) {
        this.screen = screen;
        this.worldManager = worldManager;
        this.gameBatch = gameBatch;
        this.uiBatch = uiBatch;

        this.renderGame = gameBatch != null;
        this.renderUi = uiBatch != null;
        this.worldManagerExists = worldManager != null;
    }

    @Override
    public void render(float delta) {

        //Limpa a tela
        cleanScreen();

        //Atualiza os visuais
        updateVisuals(delta);

        //Tenta renderizar o jogo
        drawGame(gameBatch);

        //Tenta renderizar a ui
        drawUI(uiBatch);

    }

    /// Limpa a tela com uma cor em específico
    protected void cleanScreen() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
    }

    /// Prepara o batch e renderiza o jogo
    protected void drawGame(SpriteBatch batch) {
        if (!renderGame) return;

        batch.begin();

        if (worldManagerExists) {
            worldManager.sortRenderables();
            for (int i = 0; i < worldManager.getRenderAbleObjectList().size(); i++) {
                RenderAbleObject obj = worldManager.getRenderAbleObjectList().get(i);

                if (obj.isPendingRemoval()) {
                    // Opcional: remover da lista de render aqui ou deixar o Manager limpar
                    continue;
                }

                obj.render(batch);
            }
        }

        screen.drawGame(batch);

        batch.end();
    }

    /// Prepara o batch e renderiza a ui
    protected void drawUI(SpriteBatch batch) {
        if (!renderUi) return;

        batch.begin();
        screen.drawUI(batch);
        batch.end();
    }

    @Override
    public void updateVisuals(float delta) {
        screen.updateVisuals(delta);

        if (!worldManagerExists) return;
        for (RenderAbleObject object : worldManager.getRenderAbleObjectList()) {
            object.updateVisuals(delta);
        }

    }
}
