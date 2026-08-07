package official.sketchBook.engine.util_related.serialization;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registro central �nico: String key -> f�brica de {@link
 * SaveDataInstance} (o DTO), N�O da classe de jogo final.
 * <p>
 * Cada classe savable do jogo registra AQUI, uma �nica vez (bloco
 * {@code static} ou m�todo {@code registerType(registry)} chamado uma
 * vez no boot), como criar um DTO VAZIO daquele tipo ? {@code
 * Supplier<SaveDataInstance<?, ?>>}, tipicamente s� {@code
 * PlayerSaveData::new}.
 * <p>
 * Fluxo de load: l� o JSON -> SaveData bruto -> pega o "__type" ->
 * pede ao registry um DTO vazio daquele tipo -> chama
 * {@code dto.loadFields(data)} pra popular ? devolve o DTO j� pronto.
 * Quem pediu ent�o chama {@code dto.newInstance(context)} quando tiver
 * o contexto (World, room) dispon�vel ? o registry nunca precisa saber
 * desse contexto, ele s� entrega DTOs.
 * <p>
 * A key � String, n�o Class&lt;T&gt; ? mesma decis�o de antes: permite
 * duas keys apontarem pra DTOs diferentes mesmo quando a classe de
 * jogo final � a mesma (ex: "submarine_enemy" vs "submarine_player").
 * <p>
 * MEM�RIA: um registro custa uma entrada de Map com uma refer�ncia de
 * m�todo (ex: {@code EnemySaveData::new}) ? NENHUM objeto de jogo, DTO
 * populado, World ou coisa pesada � criado no registro em si. O
 * Supplier s� roda (criando um DTO VAZIO) quando algu�m efetivamente
 * pede um load daquele tipo. Registrar 50 tipos de inimigo que talvez
 * nunca sejam carregados numa partida custa, na pr�tica, nada ?
 * "poluir a mem�ria" nunca foi um risco real desse registro, o risco
 * real (objetos de jogo pesados vivos sem necessidade) j� � resolvido
 * pelo ciclo normal de dispose() do BaseGameObjectDataManager, uma
 * camada completamente separada desta.
 * <p>
 * GLOBAL POR DESIGN: {@link #GLOBAL} � a inst�ncia �nica do jogo
 * inteiro, criada estaticamente aqui mesmo. Isso � seguro porque cada
 * classe savable (Player, SubmarineNode, ...) se registra sozinha, uma
 * vez, atrav�s de um bloco {@code static { }} na PR�PRIA classe ? ex:
 * <pre>{@code
 * public class Player extends ... {
 *     static {
 *         SaveDataInstanceRegistry.GLOBAL.register("player", PlayerSaveData::new);
 *     }
 *     ...
 * }
 * }</pre>
 * Isso funciona sem risco de ordem de inicializa��o neste projeto
 * porque nenhum c�digo pode carregar um save ANTES do
 * GameObjectDataManager/boot do jogo terminar de subir ? e esse boot
 * j� acontece antes de qualquer Player/SubmarineNode ser tocado pela
 * primeira vez. "O que � do Player fica no Player": a classe cuida do
 * pr�prio registro, sem exigir uma lista central que algu�m precisa
 * lembrar de manter atualizada a cada classe savable nova.
 */
public class SaveDataInstanceRegistry {

    /**
     * Inst�ncia �nica do jogo inteiro. Ver nota de mem�ria/ordem de
     * inicializa��o acima ? isso � seguro neste projeto porque nada
     * carrega um save antes do boot terminar.
     */
    public static final SaveDataInstanceRegistry GLOBAL = new SaveDataInstanceRegistry();

    private final Map<String, Supplier<SaveDataInstance<?, ?>>> factories = new HashMap<>();

    public void register(String key, Supplier<SaveDataInstance<?, ?>> factory) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Type key n�o pode ser vazia");
        }
        if (factories.containsKey(key)) {
            throw new IllegalStateException(
                "SaveDataInstance j� registrado pra key: " + key + " ? use uma key diferente."
            );
        }
        factories.put(key, factory);
    }

    /**
     * Cria um DTO vazio da key informada e j� popula via loadFields ?
     * usado internamente por load(SaveData) e tamb�m �til direto
     * quando voc� j� sabe a key (ex: resolvendo uma sub-refer�ncia
     * dentro de outro DTO, tipo SubmarineNode resolvendo suas parts).
     */
    @SuppressWarnings("unchecked")
    public <T, C> SaveDataInstance<T, C> loadByKey(String key, SaveData data) {
        Supplier<SaveDataInstance<?, ?>> factory = require(key);
        SaveDataInstance<T, C> dto = (SaveDataInstance<T, C>) factory.get();
        dto.loadFields(data);
        return dto;
    }

    /**
     * Cria e popula um DTO usando a type key EMBUTIDA no pr�prio
     * SaveData ? quem chama n�o precisa saber de antem�o qual DTO �.
     * O cast final pro tipo esperado fica por conta de quem chama,
     * exatamente como j� acontece hoje com {@code (SubmarinePassenger)
     * passengerTag.owner} em outros pontos do c�digo.
     */
    public <T, C> SaveDataInstance<T, C> load(SaveData data) {
        String key = data.getTypeKey();
        if (key == null) {
            throw new SaveDataException(
                "__type",
                "ausente ? SaveData n�o carrega metadado de tipo, n�o d� pra descobrir qual DTO usar"
            );
        }
        return loadByKey(key, data);
    }

    /**
     * Serializa uma inst�ncia de jogo J� EXISTENTE atrav�s do DTO
     * registrado sob a key, e j� embute a type key no resultado.
     */
    @SuppressWarnings("unchecked")
    public <T> SaveData save(String key, T instance) {
        Supplier<SaveDataInstance<?, ?>> factory = require(key);
        SaveDataInstance<T, ?> dto = (SaveDataInstance<T, ?>) factory.get();
        return dto.save(instance).putTypeKey(key);
    }

    public boolean isRegistered(String key) {
        return factories.containsKey(key);
    }

    private Supplier<SaveDataInstance<?, ?>> require(String key) {
        Supplier<SaveDataInstance<?, ?>> factory = factories.get(key);
        if (factory == null) {
            throw new IllegalStateException("Nenhum SaveDataInstance registrado pra key: " + key);
        }
        return factory;
    }
}
