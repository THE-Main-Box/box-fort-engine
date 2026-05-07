# TransformComponent Documentation

O `TransformComponent` é o componente fundamental de dados espaciais da engine. Ele atua como o repositório central de informações sobre a presença física, dimensões e orientação de um objeto no mundo 2D.

Sua responsabilidade é puramente estrutural e de provimento de dados, servindo como uma "fonte da verdade" que sistemas de Renderização, Física e Movimentação consultam e modificam.

## Estrutura de Dados e Funcionalidades

O componente gerencia cinco categorias principais de dados e fornece métodos utilitários para facilitar o acesso a pontos espaciais críticos:

### 1. Posicionamento (Coordenadas)
* **X e Y:** Representam a posição de origem (ancorada no canto inferior esquerdo) do objeto no plano bidimensional, em pixels.
* **Z (Depth):** Utilizado para o gerenciamento de camadas de renderização (Z-Index), permitindo o controle de sobreposição de elementos.

### 2. Dimensões Dinâmicas (Bounding Box)
* **Width e Height:** Definem a largura e altura do objeto.
* **Escalabilidade:** Através do método `setScale(x, y)`, o componente ajusta as dimensões reais do objeto multiplicando os valores base pela escala fornecida. Isso garante que sistemas de colisão AABB (Axis-Aligned Bounding Box) sempre leiam o tamanho atualizado do objeto no mundo.

### 3. Métodos de Conveniência (Spatial Helpers)
O componente fornece abstrações para cálculos comuns de detecção e posicionamento:
* **getHalfWidth / getHalfHeight:** Retorna o raio do objeto (metade da dimensão), essencial para detecção de colisão por raio.
* **getCenterX / getCenterY:** Retorna as coordenadas do centro exato do objeto, facilitando o cálculo de distância entre entidades e o foco da câmera.

### 4. Orientação e Rotação
* **Rotation:** Armazena a orientação atual do objeto em **graus**.

### 5. Percepção e Inversão (Flipping)
* **xAxisInverted / yAxisInverted:** Flags booleanas para espelhamento de sprites (ex: inverter o olhar do personagem sem alterar a lógica de coordenadas ou a arte original).



---

## Papel na Pipeline de Execução

Como este componente é um "Data Container", ele serve como o ponto de sincronização final da pipeline:

1.  **Lógica/Input:** Define a intenção de movimento ou escala.
2.  **Processamento:** O `MovementComponent` ou sistemas de Física alteram os valores de `x`, `y` ou chamam `setScale`.
3.  **Renderização:** O sistema de renderização lê o estado final do `TransformComponent` para posicionar, escalar e rotacionar o sprite corretamente na tela.

---

## Notas de Implementação

* **Validação de Escala:** O método `setScale` impede a aplicação de escalas menores ou iguais a zero, lançando uma `IllegalArgumentException`. Isso evita comportamentos anômalos em cálculos de área e na renderização da GPU.
* **Unidade de Medida:** Todos os valores são `float` para precisão sub-pixel, permitindo movimentos suaves (interpolação) mesmo em artes com baixa resolução de pixels.
* **Ciclo de Vida:** Implementa a interface `Component`, com suporte a `dispose` para remoção segura da memória.
