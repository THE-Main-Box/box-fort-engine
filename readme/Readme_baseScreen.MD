# Screen System and Game Loop Lifecycle

O `BaseScreen` é a espinha dorsal da aplicação, responsável por organizar o Game Loop e delegar as responsabilidades de lógica e visual para sistemas especializados.

## Estrutura da BaseScreen

A `BaseScreen` implementa a interface `Screen` do LibGDX, mas redireciona o fluxo padrão para dois sistemas proprietários:
1. **UpdateSystem**: Lida com a lógica física e estados dos objetos.
2. **RenderSystem**: Lida com a interpolação visual e desenho na tela.

### O Fluxo de Execução (Method: render)

A engine segue uma sequência rigorosa para garantir a consistência dos dados:

1. **updateSystem.update(delta)**: Processa a lógica em passos fixos (Fixed Timestep).
2. **updateMetrics(delta)**: Calcula o desempenho (FPS e UPS) de forma isolada.
3. **renderSystem.render(delta)**: Processa a saída visual.

## Principais Responsabilidades da Screen

* **updateScreen**: Método abstrato onde a lógica específica da tela reside. É chamado dentro do loop de atualização fixa.
* **updateVisuals**: Atualiza estados puramente cosméticos (como timers de animação ou efeitos de partículas) imediatamente antes do desenho.
* **drawGame / drawUI**: Separação clara entre a renderização do mundo (coordenadas de jogo) e da interface (coordenadas de tela).

---

## Métricas de Desempenho

O sistema diferencia dois tipos de métricas essenciais para o monitoramento:
* **FPS (Frames Per Second)**: Quantas vezes o `RenderSystem` é executado por segundo.
* **UPS (Updates Per Second)**: Quantas vezes a lógica interna foi processada. Em cenários de baixo desempenho, o UPS tenta se manter estável enquanto o FPS cai.
