package official.sketchBook.engine.camera_related.utils;

import com.badlogic.gdx.Gdx;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;

public class CameraUtils {
    public static OrthographicCameraManager createScreenCamera() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        OrthographicCameraManager manager = new OrthographicCameraManager(w, h);
        manager.getCamera().position.set(w / 2f, h / 2f, 0);
        manager.getCamera().update();
        return manager;
    }

}
