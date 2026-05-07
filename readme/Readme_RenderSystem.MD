# Render System (Visual Pipeline)

O `SingleThreadRenderSystem` é responsável por preparar o ambiente gráfico e desenhar as camadas de jogo e interface de forma isolada e organizada.

## Pipeline de Renderização

A execução segue esta ordem linear:

### 1. Preparação (Clean & Update)
* **cleanScreen**: Limpa o buffer de vídeo com a cor base.
* **updateVisuals**: Percorre o `WorldManager` chamando o `updateVisuals` de todos os objetos renderizáveis e, em seguida, o da `BaseScreen`. Isso garante que interpolações visuais estejam prontas.

### 2. Camada de Jogo (World Space)
* **Sorting**: O sistema solicita ao `WorldManager` a ordenação dos objetos (`sortRenderables`) baseada no índice de renderização (Z-Index).
* **World Drawing**: Itera sobre os objetos ativos e chama `obj.render(batch)`.
* **Screen Game Drawing**: Após os objetos do mundo, a tela pode desenhar elementos adicionais que pertencem ao espaço do jogo.



### 3. Camada de Interface (UI Space)
* **Projection Matrix**: A câmera é trocada para a `uiCamera`, garantindo que a interface não sofra com o zoom ou deslocamento da câmera de jogo.
* **UI Drawing**: Chama `screen.drawUI(batch)`.

## Isolamento de Batch
O sistema utiliza chamadas explícitas de `batch.begin()` e `batch.end()` para separar as projeções de matrizes da câmera de jogo e da câmera de UI, evitando erros de escala e posicionamento nos elementos da interface.
