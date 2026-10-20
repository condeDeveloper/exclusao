package br.com.conde.exclusao;

/**
 * UM PROTOCOLO DE EXCLUSÃO MÚTUA ESCRITO COMO MÁQUINA DE PASSOS ATÔMICOS.
 *
 * <p>Esta é a decisão que faz o repositório inteiro funcionar: cada linha do algoritmo vira um
 * passo numerado, e um passo é a unidade que não pode ser partida. Escrever o algoritmo assim
 * deixa a atomicidade <b>visível</b>, em vez de escondida na linguagem. É por isso que dá para
 * medir a diferença entre um teste-e-ajuste atômico e o mesmo código com a leitura e a escrita
 * separadas: eles só diferem em quantos passos ocupam.
 *
 * <p>São dois processos, que é onde moram os algoritmos clássicos.
 */
public abstract class Protocolo {

    public static final int PROCESSOS = 2;

    public abstract String nome();

    /** O valor inicial da memória compartilhada. */
    public abstract int[] memoriaInicial();

    /** O número do passo que é a seção crítica. */
    public abstract int critica();

    /** Quantos passos o programa tem, contando o 0 da seção não crítica. */
    public abstract int passos();

    /**
     * O PASSO do processo p, atômico.
     *
     * <p>Devolver o próprio estado recebido significa espera ocupada: o processo tentou, não
     * passou, e vai tentar de novo. A varredura lê isso como um laço no grafo.
     */
    public abstract Estado passo(Estado atual, int p);

    /** Está no protocolo de entrada: já quis entrar e ainda não entrou. */
    public boolean querEntrar(int pc) {
        return pc >= 1 && pc < critica();
    }

    public boolean naCritica(int pc) {
        return pc == critica();
    }

    protected static Estado mesmo(Estado atual) {
        return atual;
    }
}
