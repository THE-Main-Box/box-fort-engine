# TransformComponent Documentation

O `TransformComponent` é o componente fundamental de dados espaciais da engine. Ele atua como o repositório central de informações sobre a presença física e a orientação de um objeto no mundo 2D.

Ao contrário de outros componentes, o `TransformComponent` não possui lógica interna de processamento no método `update`. Sua responsabilidade é puramente estrutural: fornecer uma "fonte da verdade" para coordenadas e dimensões que outros sistemas (como Renderização, Física e Movimentação) consultarão e modificarão.

## Estrutura de Dados

O componente armazena e gerencia quatro categorias principais de dados:

### 1. Posicionamento (Coordenadas)
* **X e Y:** Representam a posição do objeto no plano bidimensional, utilizando **pixels** como unidade de medida.
* **Z (Depth):** Embora a engine seja 2D, o eixo Z é utilizado para o gerenciamento de camadas de renderização (Z-Index), permitindo o controle de sobreposição de sprites (ordenagem do que é desenhado na frente ou atrás).

### 2. Dimensões (Bounding Box)
* **Width e Height:** Definem a largura e altura do objeto em pixels. Estes valores são essenciais para cálculos de colisão AABB (Axis-Aligned Bounding Box) e para determinar a escala de renderização das texturas.

### 3. Orientação e Rotação
* **Rotation:** Armazena a orientação atual do objeto em **graus**. Este valor é frequentemente convertido para radianos internamente por sistemas de física ou funções trigonométricas de renderização da GPU.

### 4. Percepção e Inversão (Flipping)
* **xAxisInverted / yAxisInverted:** Flags booleanas que controlam a inversão da percepção visual do objeto.
    * O uso primário é o espelhamento de sprites (ex: fazer um personagem olhar para a esquerda ou direita) sem a necessidade de alterar a textura original ou a lógica de coordenadas.

## Papel na Pipeline de Execução

Como este componente é um "Data Container", ele serve como o ponto de sincronização final da pipeline:

1.  O **MovementComponent** calcula a nova velocidade.
2.  O sistema de **Física** (ou o próprio Movement) aplica esse deslocamento nos campos `x` e `y` do `TransformComponent`.
3.  O sistema de **Renderização** lê os campos `x`, `y`, `rotation` e as flags de inversão para desenhar o sprite na tela.



## Notas de Implementação

* **Unidade de Medida:** Todos os valores de posição e dimensão são tratados como `float` para permitir precisão sub-pixel durante cálculos de interpolação, embora a renderização final possa ser alinhada à grade de pixels da tela.
* **Ciclo de Vida:** Implementa a interface `Component`, garantindo que seja devidamente limpo através do método `dispose` quando o objeto pai é removido da memória.
