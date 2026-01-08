# Update System (Logic Pipeline)

O `SingleThreadUpdateSystem` implementa a técnica de **Fixed Timestep com Acumulador**. Isso garante que a física e a lógica do jogo se comportem da mesma maneira, independentemente da taxa de quadros (FPS) do hardware.

## Ciclo de Atualização (O Acumulador)

O sistema acumula o tempo passado (`delta`) e executa o processamento em fatias constantes definidas por `FIXED_TIMESTAMP`.

### Sequência de Prioridade Interna:

Dentro de cada passo do acumulador, a ordem de atualização é crítica:

1. **WorldManager Update**: O gerenciador de mundo atualiza primeiro. Ele processa a física, as colisões e os estados internos de todos os GameObjects ativos.
    * *Nota*: O Manager tem prioridade porque os dados dos objetos (posições, estados de vida) devem estar consolidados antes que a lógica da tela tome decisões.
2. **Screen Update**: A `BaseScreen` executa sua lógica de alto nível (Inputs de navegação, eventos de script da fase, etc).
3. **Métrica de UPS**: O contador de `updates` é incrementado para rastrear a integridade do ciclo lógico.

## Estabilidade
O sistema utiliza um `MAX_ACCUMULATOR` para evitar o "espiral da morte", onde o sistema tenta recuperar muitos frames perdidos e acaba gerando ainda mais atraso.
