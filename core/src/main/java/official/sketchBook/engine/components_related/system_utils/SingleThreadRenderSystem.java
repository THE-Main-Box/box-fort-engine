package official.sketchBook.engine.components_related.system_utils;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.RenderSystem;
import official.sketchBook.engine.dataManager_related.BaseGameObjectDataManager;
import official.sketchBook.engine.screen_related.BaseScreen;

public class SingleThreadRenderSystem implements RenderSystem {
    /// Referência à tela dona
    private final BaseScreen screen;
    /// Referência ao gerenciador de objetos de jogo
    private final BaseGameObjectDataManager gameObjectManager;

    /// Referência à camera a ser usada para renderizar as coisas
    private final Camera
        gameCamera,
        uiCamera;

    /// Batchs de renderização
    private final SpriteBatch
        gameBatch,
        uiBatch;

    /// Flags que determina se podemos renderizar as pipelines correspondentes
    private final boolean
        renderGame,
        renderUi;

    /// Flag para determinar se podemos acessar o gerenciador de objetos
    private final boolean canAccessWorldManager;

    public SingleThreadRenderSystem(
        BaseScreen screen,
        BaseGameObjectDataManager gameObjectManager,
        SpriteBatch gameBatch,
        Camera gameCamera,
        SpriteBatch uiBatch,
        Camera uiCamera
    ) {
        this.screen = screen;
        this.gameObjectManager = gameObjectManager;
        this.gameBatch = gameBatch;
        this.uiBatch = uiBatch;

        this.uiCamera = uiCamera;
        this.gameCamera = gameCamera;

        this.renderGame = gameBatch != null && gameCamera != null;
        this.renderUi = uiBatch != null && uiCamera != null;
        this.canAccessWorldManager = gameObjectManager != null;
    }

    /// Loop de renderização
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


    /// Atualiza dados visuais antes da renderização de fato
    @Override
    public void updateVisuals(float delta) {
        if (canAccessWorldManager) {
            gameObjectManager.updateVisuals(delta);
        }

        //Atualiza os visuais
        screen.updateVisuals(delta);

    }

    /// Limpa a tela com uma cor em específico
    protected void cleanScreen() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
    }

    /// Prepara o batch e renderiza o jogo
    protected void drawGame(SpriteBatch batch) {
        if (!renderGame) return;

        //Precisamos atualizar a camera e a projection matrix antes de inicializar o batch
        gameCamera.update();
        batch.setProjectionMatrix(gameCamera.combined);

        batch.begin();

        if (canAccessWorldManager) {
            gameObjectManager.render(batch);
        }

        batch.end();

        //Temos que separar o que é renderizado de jogo e o que é renderizado de tela

        batch.begin();
        screen.drawGame(batch);
        batch.end();
    }

    /// Prepara o batch e renderiza a ui
    protected void drawUI(SpriteBatch batch) {
        if (!renderUi) return;

        uiCamera.update();
        batch.setProjectionMatrix(uiCamera.combined);

        batch.begin();
        screen.drawUI(batch);
        batch.end();
    }
}
