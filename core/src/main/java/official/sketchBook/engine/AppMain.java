package official.sketchBook.engine;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.game.screen_related.MenuScreen;
import official.sketchBook.game.screen_related.PlayScreen;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class AppMain extends Game {

    public SpriteBatch gameBatch;
    public SpriteBatch uiBatch;

    private MenuScreen menuScreen;
    private PlayScreen playScreen;

    //TODO: Adicionar a capacidade de realizar dispose de dados estáticos

    @Override
    public void create() {
        gameBatch = new SpriteBatch();
        uiBatch = new SpriteBatch();

        menuScreen = new MenuScreen(this);
        playScreen = new PlayScreen(this);

        this.setScreen(menuScreen);
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

    public MenuScreen getMenuScreen() {
        return menuScreen;
    }

    public PlayScreen getPlayScreen() {
        return playScreen;
    }
}
