package official.sketchBook.engine;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;
import official.sketchBook.game.screen_related.MenuScreen;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class AppMain extends Game {

    public SpriteBatch gameBatch;
    public SpriteBatch uiBatch;

    //TODO: Adicionar a capacidade de realizar dispose de dados estaticos

    @Override
    public void create() {
        gameBatch = new SpriteBatch();
        uiBatch = new SpriteBatch();

        this.setScreen(new MenuScreen(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        gameBatch.dispose();
        uiBatch.dispose();

        if (screen != null) {
            screen.dispose();
        }
    }


}
