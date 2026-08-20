package official.sketchBook.engine.util_related.serialization.instantiation;

import official.sketchBook.engine.util_related.exceptions.SaveDataException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


public class SaveDataInstanceRegistry {

    /**
     * Inst�ncia �nica do jogo inteiro. Ver nota de mem�ria/ordem de
     * inicializa��o acima ? isso � seguro neste projeto porque nada
     * carrega um save antes do boot terminar.
     */
    public static final SaveDataInstanceRegistry GLOBAL = new SaveDataInstanceRegistry();

    private final Map<String, Supplier<SaveDataInstance<?>>> factories = new HashMap<>();

    public void register(String key, Supplier<SaveDataInstance<?>> factory) {
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
    public <T> SaveDataInstance<T> loadByKey(String key, SaveData data) {
        Supplier<SaveDataInstance<?>> factory = require(key);
        SaveDataInstance<T> dto = (SaveDataInstance<T>) factory.get();
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
    public <T> SaveDataInstance<T> load(SaveData data) {
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
        Supplier<SaveDataInstance<?>> factory = require(key);
        SaveDataInstance<T> dto = (SaveDataInstance<T>) factory.get();
        return dto.save(instance).putTypeKey(key);
    }

    public boolean isRegistered(String key) {
        return factories.containsKey(key);
    }

    private Supplier<SaveDataInstance<?>> require(String key) {
        Supplier<SaveDataInstance<?>> factory = factories.get(key);
        if (factory == null) {
            throw new IllegalStateException("Nenhum SaveDataInstance registrado pra key: " + key);
        }
        return factory;
    }
}
