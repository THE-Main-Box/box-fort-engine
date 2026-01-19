package official.sketchBook.engine.projectile_related;

import com.badlogic.gdx.physics.box2d.World;

public class ProjectileFactory {

    public static <T extends BaseProjectile> T createByType(Class<T> type){
        try {
            return type.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao instanciar projétil do tipo: " + type.getSimpleName(), e);
        }
    }

    public static <T extends BaseProjectile> T createByType(Class<T> type, World world){
        try {
            return type.getConstructor(World.class).newInstance(world);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao instanciar projétil do tipo: " + type.getSimpleName(), e);
        }
    }

}
