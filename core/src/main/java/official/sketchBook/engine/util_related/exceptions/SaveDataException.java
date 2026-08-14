package official.sketchBook.engine.util_related.exceptions;

/**
 * Lan�ada quando um campo OBRIGAT�rio est� ausente ou com tipo errado
 * dentro de um SaveData sendo lido por um {@code getXxxRequired(...)}.
 * <p>
 * Existe como classe pr�pria (em vez de reusar IllegalStateException
 * gen�rica) por dois motivos:
 * <p>
 * 1) Permite pegar especificamente erros de VALIDA��O DE SAVE num
 * catch, sem acidentalmente capturar outro IllegalStateException que
 * n�o tenha nada a ver com dado corrompido.
 * <p>
 * 2) Carrega a CHAVE que falhou como campo pr�prio, n�o s� dentro da
 * mensagem de texto ? isso permite quem pega a exce��o (ex: uma tela
 * de "save corrompido, escolha outro") mostrar informa��o estruturada
 * sem fazer parsing de string.
 */
public class SaveDataException extends RuntimeException {

    private final String key;

    public SaveDataException(String key, String reason) {
        super("Campo obrigat�rio invalido em SaveData: '" + key + "' ? " + reason);
        this.key = key;
    }

    private SaveDataException(String key, String message, Throwable cause) {
        super(message, cause);
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * Reempacota esta exce��o adicionando um n�vel de contexto na
     * frente da mensagem (ex: "node_0001" antes de subir pro
     * Submarine dono, que adiciona "submarine_010101010" antes de
     * subir mais um n�vel). Preserva a causa original via {@code
     * getCause()} ? o stacktrace completo continua dispon�vel, s� a
     * mensagem de topo fica leg�vel como uma trilha:
     * <pre>
     * ao carregar Submarine 'submarine_010101010' -> ao carregar
     * SubmarineNode 'node_0001' -> Campo obrigat�rio invalido em
     * SaveData: 'centerX' ? esperado n�mero, encontrado ausente
     * </pre>
     * Isso substitui repetir o mesmo bloco try/catch manualmente em
     * cada classe Savable que tenha filhos ? cada n�vel s� chama
     * {@code withContext("identifica��o do objeto atual")} uma vez.
     */
    public SaveDataException withContext(String label) {
        return new SaveDataException(key, "ao carregar " + label + " -> " + getMessage(), this);
    }
}
