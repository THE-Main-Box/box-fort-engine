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

    /// Referência ao gerenciador de objetos de jogo
    private final BaseGameObjectDataManager gameObjectManager;
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

        /*
         *Podemos apenas renderizar a pipeline correspondente de game ou ui
         *  caso tenhamos um batch sendo passado,
         *  e uma camera para podermos visualizar o que está no batch
         */
        this.renderGame = gameBatch != null && gameCamera != null;
        this.renderUi = uiBatch != null && uiCamera != null;

        //Podemos apenas iterar pelo manager, caso ele exista
        this.canAccessWorldManager = gameObjectManager != null;
    }

    /// Loop de desenho, onde realizamos a renderização do que foi marcado para ser renderizado
    @Override
    public void draw(float delta) {

        //Limpa a tela, em preparo para a próxima renderização
        cleanScreen();

        //Atualiza os visuais para preparar pra renderização
        updateVisuals(delta);

        //Tenta renderizar o jogo
        drawGame(gameBatch);

        //Tenta renderizar a ui
        drawUI(uiBatch);

    }


    /// Atualiza dados visuais antes da renderização
    @Override
    public void updateVisuals(float delta) {
        //Se pudermos acessar o manager de gameObjects
        // iremos tentar atualizar os visuais dos objetos que podem ser renderizados
        if (canAccessWorldManager) {
            gameObjectManager.updateVisuals(delta);
        }

        //Atualiza os visuais da tela
        screen.updateVisuals(delta);

    }

    /// Limpa a tela com uma cor em específico
    protected void cleanScreen() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
    }

    /// Prepara o batch e renderiza o jogo
    protected void drawGame(SpriteBatch batch) {
        //Só prosseguimos se queremos renderizar o jogo e temos os sistemas necessários
        if (!renderGame) return;

        //Precisamos atualizar a camera e a projection matrix antes de inicializar o batch
        gameCamera.update();
        batch.setProjectionMatrix(gameCamera.combined);

        //Caso possamos acessar o manager de objetos fazemos a preparação para renderizar tal
        if (canAccessWorldManager) {
            batch.begin();
            gameObjectManager.render(batch);
            batch.end();
        }

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

    public void dispose(){
    }
}
