package official.sketchBook.engine.util_related.serialization;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formato de dado intermedi�rio gen�rico, usado por QUALQUER Savable
 * (jogador, submarino, node, part).
 * <p>
 * Por baixo � um Map<String,Object>, mas todo cast/unboxing fica
 * centralizado AQUI, uma �nica vez ? o resto do c�digo nunca faz
 * (Float) obj na m�o.
 * <p>
 * N�O sabe nada sobre arquivos/JSON ? isso fica isolado em
 * {@link SaveDataJson}.
 */
public class SaveData {

    private final Map<String, Object> values;

    public SaveData() {
        this.values = new LinkedHashMap<>();
    }

    private SaveData(Map<String, Object> values) {
        this.values = values;
    }

    public static SaveData of(Map<String, Object> raw) {
        return new SaveData(raw);
    }

    public static SaveData empty() {
        return new SaveData();
    }

    // ============================================================
    // ESCRITA ? fluente
    // ============================================================

    public SaveData put(String key, float value) {
        values.put(key, value);
        return this;
    }

    public SaveData put(String key, int value) {
        values.put(key, value);
        return this;
    }

    public SaveData put(String key, boolean value) {
        values.put(key, value);
        return this;
    }

    public SaveData put(String key, String value) {
        values.put(key, value);
        return this;
    }

    public SaveData put(String key, Vector2 value) {
        if (value == null) return this;
        SaveData vecData = new SaveData();
        vecData.put("x", value.x);
        vecData.put("y", value.y);
        values.put(key, vecData);
        return this;
    }

    /**
     * Embute outro Savable's SaveData diretamente ? isso � o mecanismo
     * usado, por exemplo, pra um state de submarino referenciar sua
     * blueprint: {@code put("blueprint", blueprintSavable.save())}.
     */
    public SaveData put(String key, SaveData nested) {
        values.put(key, nested);
        return this;
    }

    public SaveData putList(String key, List<SaveData> list) {
        values.put(key, new ArrayList<Object>(list));
        return this;
    }

    public SaveData putStringList(String key, List<String> list) {
        values.put(key, new ArrayList<Object>(list));
        return this;
    }

    public SaveData putIntList(String key, List<Integer> list) {
        values.put(key, new ArrayList<Object>(list));
        return this;
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getIntList(String key) {
        Object v = values.get(key);
        if (!(v instanceof List)) return new ArrayList<>();
        List<Integer> out = new ArrayList<>();
        for (Object item : (List<Object>) v) {
            if (item instanceof Number) out.add(((Number) item).intValue());
        }
        return out;
    }

    // ============================================================
    // LEITURA ? sempre com default, nunca explode por campo ausente
    // ============================================================

    public float getFloat(String key, float defaultValue) {
        Object v = values.get(key);
        return (v instanceof Number) ? ((Number) v).floatValue() : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        Object v = values.get(key);
        return (v instanceof Number) ? ((Number) v).intValue() : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object v = values.get(key);
        return (v instanceof Boolean) ? (Boolean) v : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        Object v = values.get(key);
        return (v instanceof String) ? (String) v : defaultValue;
    }

    public Vector2 getVector2(String key, Vector2 defaultValue) {
        Object v = values.get(key);
        if (!(v instanceof SaveData)) return defaultValue;
        SaveData vecData = (SaveData) v;
        return new Vector2(vecData.getFloat("x", 0f), vecData.getFloat("y", 0f));
    }

    /** Nunca retorna null ? permite encadeamento seguro tipo data.getSaveData("x").getFloat(...). */
    public SaveData getSaveData(String key) {
        Object v = values.get(key);
        return (v instanceof SaveData) ? (SaveData) v : new SaveData();
    }

    /**
     * Vers�o que diferencia "ausente" de "presente" ? necess�rio pro
     * padr�o de "tentativa de cast" tipo REST: caller decide o que
     * fazer quando o campo referenciado (ex: blueprint embutida) nem
     * existe.
     */
    public SaveData getSaveDataOrNull(String key) {
        Object v = values.get(key);
        return (v instanceof SaveData) ? (SaveData) v : null;
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public List<SaveData> getList(String key) {
        Object v = values.get(key);
        if (!(v instanceof List)) return new ArrayList<>();
        List<SaveData> out = new ArrayList<>();
        for (Object item : (List<Object>) v) {
            if (item instanceof SaveData) out.add((SaveData) item);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object v = values.get(key);
        if (!(v instanceof List)) return new ArrayList<>();
        List<String> out = new ArrayList<>();
        for (Object item : (List<Object>) v) {
            if (item instanceof String) out.add((String) item);
        }
        return out;
    }

    // ============================================================
    // LEITURA OBRIGAT�RIA ? lan�a SaveDataException se a chave estiver
    // ausente ou com tipo errado. Use pra campos SEM default seguro
    // (ex: id/uuid de uma inst�ncia, refer�ncias por nome que a
    // reconstru��o depende 100%) ? diferente dos getters acima, que
    // sempre devolvem algo mesmo com dado corrompido.
    // <p>
    // A regra pr�tica: se um valor default errado pudesse gerar um
    // objeto "funcional mas fisicamente/logicamente incorreto" sem
    // avisar ningu�m (ex: um node em posi��o 0,0 porque centerX sumiu
    // do JSON), o campo � candidato a required. Se um default plaus�vel
    // realmente faz sentido (ex: oxygen ausente = 100, cheio), o
    // getter opcional continua sendo a escolha certa.
    // ============================================================

    public float getFloatRequired(String key) {
        Object v = values.get(key);
        if (!(v instanceof Number)) {
            throw new SaveDataException(key, "esperado n�mero, encontrado " + describe(v));
        }
        return ((Number) v).floatValue();
    }

    public int getIntRequired(String key) {
        Object v = values.get(key);
        if (!(v instanceof Number)) {
            throw new SaveDataException(key, "esperado n�mero, encontrado " + describe(v));
        }
        return ((Number) v).intValue();
    }

    public boolean getBooleanRequired(String key) {
        Object v = values.get(key);
        if (!(v instanceof Boolean)) {
            throw new SaveDataException(key, "esperado boolean, encontrado " + describe(v));
        }
        return (Boolean) v;
    }

    public String getStringRequired(String key) {
        Object v = values.get(key);
        if (!(v instanceof String) || ((String) v).isEmpty()) {
            throw new SaveDataException(key, "esperada string n�o-vazia, encontrado " + describe(v));
        }
        return (String) v;
    }

    public Vector2 getVector2Required(String key) {
        Object v = values.get(key);
        if (!(v instanceof SaveData)) {
            throw new SaveDataException(key, "esperado bloco {x,y}, encontrado " + describe(v));
        }
        SaveData vecData = (SaveData) v;
        return new Vector2(vecData.getFloatRequired("x"), vecData.getFloatRequired("y"));
    }

    public SaveData getSaveDataRequired(String key) {
        Object v = values.get(key);
        if (!(v instanceof SaveData)) {
            throw new SaveDataException(key, "esperado bloco aninhado, encontrado " + describe(v));
        }
        return (SaveData) v;
    }

    /**
     * Lista obrigat�ria de strings n�o-vazia ? use pra refer�ncias tipo
     * "parts" de um node: um node sem NENHUMA part referenciada j� �
     * sinal de corrup��o, n�o um caso v�lido de "node vazio".
     */
    public List<String> getStringListRequired(String key) {
        List<String> list = getStringList(key);
        if (list.isEmpty()) {
            throw new SaveDataException(key, "esperada lista de strings n�o-vazia, encontrado " + describe(values.get(key)));
        }
        return list;
    }

    private static String describe(Object v) {
        if (v == null) return "ausente";
        return v.getClass().getSimpleName() + "(" + v + ")";
    }

    // ============================================================
    // TYPE KEY ? metadado usado pelo SavableTypeRegistry pra descobrir
    // sozinho qual SavableType<T> reconstr�i este SaveData, sem quem
    // pede o load precisar saber de antem�o a classe concreta.
    // "__type" fica FORA do namespace de campos normais de prop�sito
    // (prefixo duplo underscore) ? evita colis�o acidental com um
    // campo leg�timo chamado "type" que alguma classe queira usar.
    // ============================================================

    private static final String TYPE_KEY_FIELD = "__type";

    public SaveData putTypeKey(String key) {
        return put(TYPE_KEY_FIELD, key);
    }

    /** Retorna null se ausente ? um SaveData sem type key � v�lido (ex: um bloco aninhado tipo Vector2). */
    public String getTypeKey() {
        return getString(TYPE_KEY_FIELD, null);
    }

    public Map<String, Object> raw() {
        return values;
    }
}
