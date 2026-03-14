package official.sketchBook.engine.util_related.pools;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.Disposable;
import official.sketchBook.engine.components_related.ray_cast.RayCastData;

import static official.sketchBook.game.util_related.constants.DebugConstants.show_ray_cast;


public class RayCastPool extends CustomPool<RayCastData> implements Disposable {
    /// Instancia única
    private static RayCastPool instance;

    /// Mundo onde iremos realizar o casting
    private World world;

    /// Renderizador para debug
    private ShapeRenderer debugRenderer;

    private boolean disposed = false;

    private RayCastPool(World world) {
        super(16);
        this.world = world;
        this.debugRenderer = new ShapeRenderer();
    }

    @Override
    protected RayCastData newObject() {
        return new RayCastData();
    }

    public RayCastData cast(
        float x1,
        float y1,
        float x2,
        float y2,
        boolean includeSensors
    ) {
        RayCastData result = obtain();
        result.reset();

        world.rayCast(
            (fixture, point, normal, fraction) -> {
                if (!includeSensors && fixture.isSensor()) return -1;

                result.set(
                    fixture,
                    x1, y1,
                    x2, y2,
                    point,
                    normal,
                    fraction
                );
                return 0;
            },
            x1, y1,
            x2, y2
        );

        // Se não houve hit ainda armazenamos os pontos para debug
        if (!result.hasHit()) {
            result.startPoint.set(x1, y1);
            result.endPoint.set(x2, y2);
        }

        return result;
    }

    public void renderDebug(Matrix4 projectionMatrix) {

        if (!show_ray_cast || activeObjects.isEmpty()) return;

        debugRenderer.setProjectionMatrix(projectionMatrix);
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        debugRenderer.setColor(Color.RED);

        for (int i = 0; i < activeObjects.size; i++) {
            RayCastData ray = activeObjects.get(i);
            debugRenderer.line(
                ray.startPoint.x,
                ray.startPoint.y,
                ray.endPoint.x,
                ray.endPoint.y
            );
        }

        debugRenderer.end();
    }

    @Override
    public void dispose() {
        if (disposed) return;

        this.clear();

        debugRenderer.dispose();

        world = null;
        instance = null;
        debugRenderer = null;

        disposed = true;
    }

    public static RayCastPool getInstance(World world) {
        if (instance == null) {
            instance = new RayCastPool(world);
        }
        return instance;
    }

    public static RayCastPool getInstance() {
        return instance;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
