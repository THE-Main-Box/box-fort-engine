package official.sketchBook.engine.projectile_related.util;

import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.util_related.enumerators.Direction;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

public class CollisionDataBuffer {

    /// Última direção da última iteração
    public Direction lastDirection;

    /// Identificador do último objeto que tivemos uma colisão
    public GameObjectTag lastCollisionWith;

    /// Vetores de valores de colisão
    public final Vector2
        collisionSelfPos,       //Posição do dono deste buffer
        collisionTargetPos,     //Posição do objeto que colidimos
        collisionNormalValue;   //Normal da colisão

    private boolean reset;

    public CollisionDataBuffer() {
        collisionSelfPos = new Vector2();
        collisionTargetPos = new Vector2();

        collisionNormalValue = new Vector2();

        reset = false;
    }

    /// Realiza um reset dos dados armazenados
    public void reset() {
        if (reset) return;

        this.lastCollisionWith = null;
        this.lastDirection = null;

        this.collisionSelfPos.setZero();
        this.collisionTargetPos.setZero();
        this.collisionNormalValue.setZero();

        this.reset = true;
    }

    /**
     * Atualizamos os dados a respeito da colisão
     *
     * @param targetTag          tag de objeto de quem nós colidimos, pode ser null infelizmente,
     *                           então não dependa desse valor para executar código crítico
     * @param collisionDirection direção da colisão do ponto de vista do dono deste buffer
     * @param selfPos            posição do dono desse buffer quando a colisão foi detectada
     * @param targetPos          posição da body que houve a colisão quando detectamos uma colisão
     * @param collisionNormal    ângulo normal da colisão
     */
    public void buff(
        GameObjectTag targetTag,
        Direction collisionDirection,
        Vector2 selfPos,
        Vector2 targetPos,
        Vector2 collisionNormal
    ) {
        this.lastCollisionWith = targetTag;
        this.collisionSelfPos.set(selfPos);
        this.collisionTargetPos.set(targetPos);
        this.collisionNormalValue.set(collisionNormal);
        this.lastDirection = collisionDirection;

        this.reset = false;
    }

    public boolean isReset() {
        return reset;
    }
}
