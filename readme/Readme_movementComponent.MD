# Movement System Documentation

Este documento descreve o funcionamento do sistema de movimentação da engine, focado no gerenciamento de velocidades,
acelerações e inércia para objetos espaciais.

## Interface MovableObjectII

A interface `MovableObjectII` é o contrato essencial para qualquer entidade que deseje possuir capacidades de locomoção.
Ela não contém lógica de movimento por si só,
mas garante que o objeto possua os componentes necessários para tal:

* **TransformComponent:** Fornece os dados de posição (X, Y) que serão manipulados.
* **MovementComponent:** O motor lógico que processa as velocidades e acelerações.

Qualquer classe que implemente esta interface
torna-se um alvo válido para os sistemas de física e controladores de input.

---

## MovementComponent

O `MovementComponent` é um componente lógico que atua como um provedor de estado cinemático.
Ele não apenas armazena valores de velocidade, mas gerencia como esses valores evoluem ao longo do tempo
através de um algoritmo de aceleração e desaceleração.

### Lógica Interna de Processamento

O componente processa cada eixo de forma independente, mas seguindo o mesmo fluxo lógico:

1.  **Validação de Movimento:** Antes de qualquer cálculo, o sistema verifica as flags `canMove`. Se o movimento em
2. um eixo estiver desabilitado,
3. os métodos `resetXMovement` ou `resetYMovement` são chamados,
4. zerando instantaneamente tanto a velocidade quanto a aceleração acumulada para evitar comportamentos residuais incoerentes.

2.  **Gerenciamento de Aceleração:**
    * **Com Aceleração Ativa:** Se o objeto tem permissão para acelerar e possui um valor de aceleração interna diferente de zero (`isAccelerating`), esse valor é somado diretamente à velocidade atual.
    * **Sem Aceleração Interna:** Caso não haja uma intenção de aceleração (mesmo que a permissão exista), o valor de aceleração interna é forçado a zero. Isso interrompe o fluxo de ganho de velocidade imediatamente.

3.  **Desaceleração e Inércia:**
    * Quando a aceleração cessa, o sistema verifica se o objeto ainda está em movimento (`isMoving`) e se possui permissão para desacelerar.
    * Se permitido, o algoritmo aplica uma redução gradual baseada no `xDeceleration` ou `yDeceleration` multiplicado pelo `delta time`.
    * O algoritmo de desaceleração é inteligente: ele garante que a velocidade não ultrapasse o valor zero (invertendo a direção) e utiliza a função sinal (`Math.signum`) para aplicar a força de resistência na direção oposta ao movimento atual.

4.  **Sistema de Limite de Velocidade (Clamping):**
    * Após todos os cálculos de ganho ou perda, o sistema aplica um "Clamp".
    * A velocidade é comparada com os limites `xMaxSpeed` e `yMaxSpeed`. Se o valor calculado exceder esses limites (seja positivo ou negativo), ele é truncado para o valor máximo permitido, garantindo estabilidade física e lógica.

---

## Funcionalidades e Aplicação

* **autoApplySpeed:** Uma variável crítica que determina o comportamento do componente. Se ativa, o componente aplica automaticamente a velocidade calculada ao `TransformComponent` do objeto durante o `update`. Caso desativada, o componente serve apenas como um buffer de dados para que sistemas externos (como Box2D ou resolutores de colisão customizados) consultem as velocidades desejadas.
* **Reset de Estado:** O componente permite resetar os eixos de forma isolada, limpando inércia e intenção de movimento sem afetar o outro eixo.
* **Isolamento de Eixo:** As flags permitem comportamentos complexos, como um objeto que pode ser empurrado horizontalmente (`canMoveX`), mas ignora gravidade ou forças verticais (`canMoveY = false`).
