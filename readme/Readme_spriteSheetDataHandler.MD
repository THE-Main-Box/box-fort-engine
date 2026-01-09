# SpriteSheetDataHandler Documentation

O `SpriteSheetDataHandler` é o componente especializado em gerenciar a lógica de transformação e renderização de imagens provenientes de uma *Sprite Sheet*. Ele atua como um controlador de estado visual, garantindo que as transformações (escala, rotação e inversão) sejam aplicadas de forma consistente antes da imagem chegar ao `SpriteBatch`.

## Responsabilidades Principais

* **Gerenciamento de Transformação:** Controla a escala, rotação e transposição de coordenadas de pixels para o espaço de renderização.
* **Cálculo de Origem Dinâmico:** Gerencia o ponto de origem (Pivot) para rotações, permitindo tanto a centralização automática quanto o ajuste fino via offsets escaláveis.
* **Abstração de Canvas:** Decompõe automaticamente a textura original em quadros (*frames*) baseando-se na grade informada (colunas e linhas).

---

## Fluxo de Transformação e Renderização

O handler segue uma ordem lógica interna para garantir que a imagem não sofra distorções ou desalinhamentos durante mudanças de estado:

1.  **Cálculo de Dimensões:** Ao alterar a escala (`scaleX/Y`), o sistema recalcula o `renderWidth` e `renderHeight`.
2.  **Centralização Automática:** A origem de rotação base (`originX/Y`) é sempre redefinida para o centro geométrico da nova dimensão renderizada.
3.  **Sincronização de Offsets:** Se as flags de atualização automática estiverem ativas, os offsets de desenho e rotação são recalculados proporcionalmente à nova escala.
4.  **Aplicação de Posição:** A posição final de desenho (`xPos/yPos`) é calculada subtraindo os offsets de desenho da coordenada real do objeto.



---

## Otimizações Internas

* **Não-Propriedade de Textura (Memory Safety):** O handler recebe referências de texturas gerenciadas externamente. Isso evita a duplicação de dados na VRAM e simplifica o ciclo de vida do objeto.
* **Cálculos Reativos:** Operações custosas como a divisão de dimensões do canvas ou o recálculo de pivôs ocorrem apenas sob demanda (mudança de escala ou construção), mantendo o método `render` extremamente leve.
* **Redução de Instanciação:** O sistema evita a criação de objetos temporários (como `Vector2` ou `Rect`) durante o loop de renderização, utilizando variáveis primitivas como buffers de estado.

---

## Lógica de Offsets e Escalabilidade

A nova versão do handler introduz o gerenciamento inteligente de offsets através de flags de controle:

### 1. Draw Offset
Ajusta a renderização do sprite em relação à sua posição lógica.
* **Subtração de Posição:** A imagem é desenhada em `x - drawOffSetX`, o que permite, por exemplo, centralizar uma arte maior sobre uma caixa de colisão menor.
* **Auto-Update Scale:** Se `autoUpdateDrawOffsetScale` for true, o offset de desenho cresce ou diminui junto com a escala do sprite, mantendo o alinhamento visual relativo.

### 2. Origin Offset (Pivot)
Define o ponto exato onde a rotação acontece.
* **Ponto Neutro:** O ponto zero da rotação é o centro do sprite (`originX/Y`).
* **Offset de Rotação:** O `originOffSet` desloca esse pivô. Se `autoUpdateRotationOffsetScale` for true, esse deslocamento é ajustado conforme a escala, garantindo que o ponto de rotação (como a junta de um braço) permaneça no local correto da arte mesmo se o sprite for ampliado.

---

## Integração com SpriteBatch

O método `renderSprite` utiliza a utilidade `obtainCurrentSpriteImage` para extrair a região da textura. A inversão de eixos (`xAxisInvert`, `yAxisInvert`) é tratada na extração dos dados da imagem, o que é computacionalmente mais barato do que manipular matrizes de rotação do `SpriteBatch` para simples espelhamento.

```java
// Exemplo de configuração de um objeto escalável com pivô ajustado
handler.autoUpdateDrawOffsetScale = true;
handler.autoUpdateRotationOffsetScale = true;

handler.setScale(2.5f, 2.5f); // Os offsets serão ajustados automaticamente aqui
handler.setRotation(90f);
handler.updatePosition(playerX, playerY);
handler.renderSprite(batch, currentSprite);
