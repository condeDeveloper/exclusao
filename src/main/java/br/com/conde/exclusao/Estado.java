package br.com.conde.exclusao;

import java.util.Arrays;

/**
 * UM INSTANTE DO SISTEMA INTEIRO: onde cada processo está, o que a memória compartilhada guarda
 * e quantas voltas cada processo já completou.
 *
 * <p>O estado é imutável e tem igualdade por conteúdo. Isso é o que permite a varredura: dois
 * caminhos diferentes que chegam à mesma configuração são o mesmo nó do grafo, e o número de
 * estados para de crescer.
 */
public final class Estado {

    /** O processo encerrou na seção não crítica e não volta mais. */
    public static final int PARADO = -1;

    public final int[] pc;
    public final int[] mem;
    public final int[] voltas;

    public Estado(int[] pc, int[] mem, int[] voltas) {
        this.pc = pc;
        this.mem = mem;
        this.voltas = voltas;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) return true;

        if (!(outro instanceof Estado e)) return false;

        return Arrays.equals(pc, e.pc) && Arrays.equals(mem, e.mem)
                && Arrays.equals(voltas, e.voltas);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Arrays.hashCode(pc) + Arrays.hashCode(mem)) + Arrays.hashCode(voltas);
    }

    @Override
    public String toString() {
        return "pc=" + Arrays.toString(pc) + " mem=" + Arrays.toString(mem);
    }
}
