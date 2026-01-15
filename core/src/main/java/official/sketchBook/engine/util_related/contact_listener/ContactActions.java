package official.sketchBook.engine.util_related.contact_listener;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.WorldManifold;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.util_related.enumerators.Direction;

public class ContactActions {
    private static final float MIN_VEL_THRESHOLD = 0.05f;
    private static final Vector2 tmpSpeed = new Vector2();

    /**
     * Resolve a direção do impacto baseada na Normal do contato.
     * Importante: A normal do Box2D aponta da FixtureA para a FixtureB.
     */
    public static Direction getCollisionDirection(Contact contact) {
        if (contact == null || !contact.isTouching()) return Direction.STILL;

        WorldManifold worldManifold = contact.getWorldManifold();
        Vector2 normal = worldManifold.getNormal();

        float x = normal.x;
        float y = normal.y;
        float threshold = MIN_VEL_THRESHOLD;

        // Lógica de quadrantes para direções
        //Se o eixo y for maior que o valor mínimo então quer dizer que estamos direcionados para baixo
        if (y > threshold) {
            //Se o x for pra esquerda ou direita retornamos o eixo correspondente
            if (x < -threshold) return Direction.DOWN_LEFT;
            if (x > threshold) return Direction.DOWN_RIGHT;
            //Caso o treshold estiver dentro dos limites lidamos com a direção para baixo
            return Direction.DOWN;
            //Se o eixo y for menor que o valor minimo então estamos com uma colisão para cima
        } else if (y < -threshold) {
            //Realizamos mais uma validação, mas dessa vez retornamos no eixo superior
            if (x < -threshold) return Direction.UP_LEFT;
            if (x > threshold) return Direction.UP_RIGHT;
            //Se o eixo x estiver dentro dos limites retornamos como sendo para cima
            return Direction.UP;
            //Caso o eixo y esteja dentro dos limites a colisão veio da esquerda ou direita
        } else {
            //tentamos descobrir qual a direção
            if (x > threshold) return Direction.LEFT;
            if (x < -threshold) return Direction.RIGHT;
        }

        //Se mesmo depois de todas essas validações, chegamos até aqui,
        // é porque o treshold está neutro e a direção foi neutra
        return Direction.STILL;
    }

    /**
     * Zera a velocidade lógica caso o objeto esteja tentando se mover contra um obstáculo.
     */
    public static void handleBlockedMovement(Direction dir, MovableObjectII mob) {
        if (dir == Direction.STILL || mob == null) return;

        MovementComponent moveC = mob.getMoveC();
        Body body = mob instanceof PhysicalObjectII ?
            ((PhysicalObjectII) mob).getBody() :
            null;

        //Se a direção da batida em relação ao objeto for na esquerda e estivermos nos movendo naquela direção
        boolean leftBlocked = dir.isLeft()
            && moveC.xSpeed < -MIN_VEL_THRESHOLD;

        //Se a direção da batida em relação ao objeto for na direita e estivermos nos movendo naquela direção
        boolean rightBlocked = dir.isRight() &&
            moveC.xSpeed > MIN_VEL_THRESHOLD;

        //Se a direção da batida em relação ao objeto for pra cima e estivermos nos movendo naquela direção
        boolean upBlocked = dir.isUp() && moveC.ySpeed > MIN_VEL_THRESHOLD;
        //Se a direção da batida em relação ao objeto for pra baixo e estivermos nos movendo naquela direção
        boolean downBlocked = dir.isDown() && moveC.ySpeed < -MIN_VEL_THRESHOLD;

        if (body == null) {
            // Resetamos a velocidade do eixo X
            if (leftBlocked || rightBlocked) {
                moveC.resetXMovement();
            }

            // Resetamos a velocidade do eixo Y
            if (upBlocked || downBlocked) {
                moveC.resetYMovement();
            }
        } else {
            tmpSpeed.set(body.getLinearVelocity());

            if (leftBlocked || rightBlocked) {
                moveC.resetXMovement();

                body.setLinearVelocity(
                    0,
                    tmpSpeed.y
                );
            }

            if (upBlocked || downBlocked) {
                moveC.resetYMovement();

                body.setLinearVelocity(
                    tmpSpeed.x,
                    0
                );
            }
        }
    }

    public static void applyDefaultFrictionLogic(Contact contact) {
        if (contact == null) return; // Evita NullPointerException

        WorldManifold worldManifold = contact.getWorldManifold();
        if (worldManifold == null) return; // Precaução extra

        float normalX = worldManifold.getNormal().x;
        float normalY = worldManifold.getNormal().y;

        // Se o normal é quase para os lados (lateral forte), reduzir atrito
        if (Math.abs(normalX) > 0.8f && Math.abs(normalY) < 0.5f) {
            // Batendo mais de lado do que de cima
            contact.setFriction(0f); // Sem atrito nas paredes
        }
        // Se o normal é para cima (chão forte)
        else if (normalY > 0.8f) {
            contact.setFriction(0.8f); // Chão normal, atrito alto para não escorregar
        }
        else {
            // Casos meio inclinados (tipo quinas), colocar um atrito médio
            contact.setFriction(0.4f);
        }
    }

}
