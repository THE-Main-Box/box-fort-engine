package official.sketchBook.engine.components_related.input_system.infra;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.util_related.enumerators.InputRole;

import java.util.EnumMap;
import java.util.Map;

public class InputCommandReceiverComponent implements Component {

    /// Mapa imutável de comandos indexados pela role, gerado uma única vez na criação
    private final Map<InputRole, InputCommand<?>> commandMap;

    private boolean disposed = false;

    public InputCommandReceiverComponent() {
        this.commandMap = new EnumMap<>(InputRole.class);
    }

    /// Registra um comando para uma role específica, chamado apenas durante a inicialização do objeto dono
    public void addCommand(InputRole role, InputCommand<?> command) {
        if (role == null || command == null) return;
        commandMap.put(role, command);
    }

    /// Busca tipada de um comando, evita cast manual em quem consome
    @SuppressWarnings("unchecked")
    public <T> InputCommand<T> getCommand(InputRole role) {
        return (InputCommand<T>) commandMap.get(role);
    }

    /// Verifica se o objeto possui um comando para essa role
    public boolean hasCommand(InputRole role) {
        return commandMap.containsKey(role);
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public void postUpdate() {
    }

    @Override
    public void initObject() {
    }

    @Override
    public void dispose() {
        if (disposed) return;

        commandMap.clear();

        disposed = true;
    }
}
