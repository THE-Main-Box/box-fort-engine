package official.sketchBook.engine.components_related.objects;

import com.badlogic.gdx.math.MathUtils;

public class AxisData {
    /// Dados dinâmicos de aplicação constante
    public float
        velocity,               //Quantidade de movimento do eixo
        acceleration,           //Quantidade de aceleração
        deceleration,           //Quantidade de desaceleração
        weightFactor;           //Fator de inércia por peso

    /// Constraint de movimentação
    public float
        maxVel,                 //Quantidade de movimento máxima
        maxMoveVel;             //Quantidade de movimento máxima que podemos simular num sistema de movimentação

    /// Flags de auxílio de constraint
    public boolean
        canAccelerate,          //Se podemos acelerar no eixo
        canDecelerate,          //Se podemos desacelerar no eixo
        canMove;                //Se podemos nos mover no eixo

    public AxisData() {
    }

    public AxisData(
        float deceleration,
        float weightFactor,
        float maxVel,
        float maxMoveVel,
        boolean canAccelerate,
        boolean canDecelerate,
        boolean canMove
    ) {
        this.deceleration = deceleration;

        this.weightFactor = weightFactor;

        this.maxVel = maxVel;
        this.maxMoveVel = maxMoveVel;

        this.canAccelerate = canAccelerate;
        this.canDecelerate = canDecelerate;

        this.canMove = canMove;
    }

    /// Atualiza o valor de velocidade do eixo
    public void updateAxis(float delta) {
        if (!canMove) {     //Caso não possamos nos mover resetamos a movimentação e nem tentamos executar
            resetMovement();
            return;
        }

        /*
         *   Neste código levamos em consideração o delta,
         * pois alguns valores não considerariam o deltaTime em nenhum momento,
         * por isso realizamos tal aplicação aqui
         */

        //Se puder acelerar e tivermos aceleração
        if (canAccelerate && acceleration != 0) {
            // Resistência dificulta iniciar movimento
            float effectiveAccel = acceleration / (1f + weightFactor);
            //Aplicamos a velocidade
            velocity += effectiveAccel;

            //Caso contrário e caso possamos desacelerar e tivermos com movimento
        } else if (canDecelerate && isMoving()) {
            // Resistência dificulta parar
            float effectiveDeAccel = deceleration / (1f + weightFactor * delta);

            //Aplicamos a velocidade com a constraint de desaceleração aplicada
            velocity = applyDeceleration(
                velocity,
                effectiveDeAccel * delta
            );
        }

        this.velocity = MathUtils.clamp(
            velocity,
            -maxMoveVel,
            maxMoveVel
        );

    }

    /// Aplica a desaceleração artificial
    private float applyDeceleration(float speed, float deceleration) {
        if (speed == 0 || deceleration == 0) return 0;

        // Se a velocidade é menor que o deceleration, zera
        if (Math.abs(speed) <= deceleration) return 0;

        return speed - deceleration * Math.signum(speed);
    }

    /// Limpa a aceleração
    public void cleanAcceleration() {
        this.acceleration = 0;
    }

    /// Inverte o movimento, por inverter a aceleração e velocidade
    public void reverseMovement() {
        this.velocity *= -1;
        this.acceleration *= -1;
    }

    /// Seta o valor da aceleração e velocidade pra facilitar chegar numa quantidade de movimento real específica
    public void setMovement(float value) {
        this.velocity = value;
        this.acceleration = value;
    }

    /// Torna 0 a aceleração e velocidade para parar o movimento
    public void resetMovement() {
        this.velocity = 0;
        this.acceleration = 0;
    }

    public boolean isAccelerating() {
        return this.acceleration != 0;
    }

    public boolean isMoving() {
        return this.velocity != 0;
    }

    /// Seta dados sem alterar os dados de movimentação como velocidade e aceleração, que são individuais
    public void set(AxisData data) {
        this.maxVel = data.maxVel;
        this.maxMoveVel = data.maxMoveVel;

        this.deceleration = data.deceleration;

        this.weightFactor = data.weightFactor;

        this.canAccelerate = data.canAccelerate;
        this.canDecelerate = data.canDecelerate;


        this.canMove = data.canMove;
    }
}
