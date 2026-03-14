package official.sketchBook.engine.components_related.ray_cast;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import official.sketchBook.engine.util_related.pools.CustomPool;

public class RayCastData implements CustomPool.Poolable {
    /// Fixture detectada pelo rayCast
    public Fixture fixture;

    /// Vetores de valor de rayCast
    public final Vector2
        worldPoint = new Vector2(),     //Ponto de contato no mundo
        startPoint = new Vector2(),     //Ponto de início
        endPoint = new Vector2();       //Ponto de fim

    /// Normal (ângulo) em vector2 da colisão
    public final Vector2 normal = new Vector2();

    /// Fração do quanto que o rayCast teve que percorrer para acertar o alvo
    public float fraction;

    /// Flags de auxilio de estado
    private boolean
        hasHit = false,     //Se houve detecção de colisão
        reset = false;      //Se foi resetada


    public void set(
        Fixture fixture,
        Vector2 startPoint,
        Vector2 endPoint,
        Vector2 normal,
        float fraction
    ) {
        this.fixture = fixture;
        this.startPoint.set(startPoint);
        this.endPoint.set(endPoint);
        this.normal.set(normal);
        this.fraction = fraction;
        this.hasHit = true;
        this.reset = false;
    }

    public void set(
        Fixture fixture,
        float x1,
        float y1,
        float x2,
        float y2,
        Vector2 worldPoint,
        Vector2 normal,
        float fraction
    ) {
        this.fixture = fixture;
        this.startPoint.set(x1, y1);
        this.endPoint.set(x2, y2);
        this.worldPoint.set(worldPoint);
        this.normal.set(normal);
        this.fraction = fraction;
        this.hasHit = true;
        this.reset = false;
    }

    @Override
    public void reset() {
        if (reset) return;

        startPoint.setZero();
        endPoint.setZero();
        normal.setZero();

        fixture = null;

        hasHit = false;
        reset = true;
    }

    @Override
    public void destroy() {
        reset();
    }

    @Override
    public boolean isReset() {
        return reset;
    }

    public boolean hasHit() {
        return hasHit;
    }
}
