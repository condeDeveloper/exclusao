package br.com.conde.exclusao;

import java.util.List;

/**
 * OS PROTOCOLOS, DO MAIS INGÊNUO AO CORRETO.
 *
 * <p>Estão aqui tanto os que funcionam quanto os que não funcionam, de propósito. Um protocolo
 * quebrado só ensina alguma coisa quando a varredura mostra <b>em que sequência exata</b> ele
 * quebra, e para isso ele precisa existir.
 */
public final class Protocolos {

    private Protocolos() {}

    public static List<Protocolo> todos() {
        return List.of(new Alternancia(), new SoFlag(), new FlagComRecuo(), new TrocaPartida(),
                new TrocaAtomica(), new Peterson(), new Dekker(), new Padaria());
    }

    /* ===================== 1. alternancia estrita ===================== */

    /**
     * SÓ O TURNO: cada um espera a sua vez, e a vez alterna.
     *
     * <p>A exclusão é óbvia e sai de graça. O defeito está em outro lugar: a vez de quem não
     * quer entrar continua sendo a vez dele.
     */
    public static final class Alternancia extends Protocolo {

        @Override
        public String nome() {
            return "alternancia estrita";
        }

        @Override
        public int[] memoriaInicial() {
            return new int[] {0};
        }

        @Override
        public int critica() {
            return 2;
        }

        @Override
        public int passos() {
            return 4;
        }

        @Override
        public Estado passo(Estado atual, int p) {
            int[] pc = atual.pc.clone();
            int[] mem = atual.mem.clone();

            switch (atual.pc[p]) {
                case 1 -> {
                    if (mem[0] != p) return mesmo(atual);

                    pc[p] = 2;
                }
                case 2 -> pc[p] = 3;
                case 3 -> {
                    mem[0] = 1 - p;
                    pc[p] = 0;
                }
                default -> throw new IllegalStateException("passo " + atual.pc[p]);
            }

            return new Estado(pc, mem, atual.voltas);
        }
    }

    /* ===================== 2. so a bandeira ===================== */

    /**
     * SÓ A BANDEIRA: levanto a minha, espero a do outro abaixar.
     *
     * <p>A exclusão funciona. O que não funciona é levantar a bandeira antes de olhar a do outro.
     */
    public static final class SoFlag extends Protocolo {

        @Override
        public String nome() {
            return "so a bandeira";
        }

        @Override
        public int[] memoriaInicial() {
            return new int[] {0, 0};
        }

        @Override
        public int critica() {
            return 3;
        }

        @Override
        public int passos() {
            return 5;
        }

        @Override
        public Estado passo(Estado atual, int p) {
            int outro = 1 - p;
            int[] pc = atual.pc.clone();
            int[] mem = atual.mem.clone();

            switch (atual.pc[p]) {
                case 1 -> {
                    mem[p] = 1;
                    pc[p] = 2;
                }
                case 2 -> {
                    if (mem[outro] == 1) return mesmo(atual);

                    pc[p] = 3;
                }
                case 3 -> pc[p] = 4;
                case 4 -> {
                    mem[p] = 0;
                    pc[p] = 0;
                }
                default -> throw new IllegalStateException("passo " + atual.pc[p]);
            }

            return new Estado(pc, mem, atual.voltas);
        }
    }

    /* ===================== 3. bandeira com recuo ===================== */

    /**
     * BANDEIRA COM RECUO: se o outro já está tentando, abaixo a minha e tento de novo.
     *
     * <p>Esta é a correção óbvia do travamento do protocolo anterior, e ela realmente tira o
     * travamento. O que ela põe no lugar é outra coisa.
     */
    public static final class FlagComRecuo extends Protocolo {

        @Override
        public String nome() {
            return "bandeira com recuo";
        }

        @Override
        public int[] memoriaInicial() {
            return new int[] {0, 0};
        }

        @Override
        public int critica() {
            return 4;
        }

        @Override
        public int passos() {
            return 6;
        }

        @Override
        public Estado passo(Estado atual, int p) {
            int outro = 1 - p;
            int[] pc = atual.pc.clone();
            int[] mem = atual.mem.clone();

            switch (atual.pc[p]) {
                case 1 -> {
                    mem[p] = 1;
                    pc[p] = 2;
                }
                case 2 -> pc[p] = mem[outro] == 1 ? 3 : 4;
                case 3 -> {
                    mem[p] = 0;
                    pc[p] = 1;
                }
                case 4 -> pc[p] = 5;
                case 5 -> {
                    mem[p] = 0;
                    pc[p] = 0;
                }
                default -> throw new IllegalStateException("passo " + atual.pc[p]);
            }

            return new Estado(pc, mem, atual.voltas);
        }
    }

    /* ===================== 4. a trava partida em dois passos ===================== */

    /**
     * A TRAVA COM A LEITURA E A ESCRITA SEPARADAS.
     *
     * <p>É o mesmo algoritmo da trava atômica, palavra por palavra, com uma diferença só: ler a
     * trava e marcá-la são dois passos em vez de um. Esse é o repositório inteiro numa linha.
     */
    public static final class TrocaPartida extends Protocolo {

        @Override
        public String nome() {
            return "trava lida e escrita em 2 passos";
        }

        @Override
        public int[] memoriaInicial() {
            return new int[] {0, 0, 0};
        }

        @Override
        public int critica() {
            return 4;
        }

        @Override
        public int passos() {
            return 6;
        }

        @Override
        public Estado passo(Estado atual, int p) {
            int[] pc = atual.pc.clone();
            int[] mem = atual.mem.clone();

            switch (atual.pc[p]) {
                case 1 -> {
                    mem[1 + p] = mem[0];
                    pc[p] = 2;
                }
                case 2 -> pc[p] = mem[1 + p] == 0 ? 3 : 1;
                case 3 -> {
                    mem[0] = 1;
                    pc[p] = 4;
                }
                case 4 -> pc[p] = 5;
                case 5 -> {
                    mem[0] = 0;
                    pc[p] = 0;
                }
                default -> throw new IllegalStateException("passo " + atual.pc[p]);
            }

            return new Estado(pc, mem, atual.voltas);
        }
    }

    /* ===================== 5. a trava atomica ===================== */

    /** A MESMA TRAVA, com a leitura e a escrita num passo só. */
    public static final class TrocaAtomica extends Protocolo {

        @Override
        public String nome() {
            return "trava atomica (1 passo)";
        }

        @Override
        public int[] memoriaInicial() {
            return new int[] {0};
        }

        @Override
        public int critica() {
            return 2;
        }

        @Override
        public int passos() {
            return 4;
        }

        @Override
        public Estado passo(Estado atual, int p) {
            int[] pc = atual.pc.clone();
            int[] mem = atual.mem.clone();

            switch (atual.pc[p]) {
                case 1 -> {
                    if (mem[0] == 1) return mesmo(atual);

                    mem[0] = 1;
                    pc[p] = 2;
                }
                case 2 -> pc[p] = 3;
                case 3 -> {
                    mem[0] = 0;
                    pc[p] = 0;
                }
                default -> throw new IllegalStateException("passo " + atual.pc[p]);
            }

            return new Estado(pc, mem, atual.voltas);
        }
    }

    /* ===================== 6. peterson ===================== */

    /**
     * PETERSON: levanta a bandeira e <b>cede a vez</b>.
     *
     * <p>Ceder a vez é o passo que parece inofensivo e é o que resolve. Quem escreve por último
     * perde o desempate, e como só um pode escrever por último, só um espera.
     */
    public static final class Peterson extends Protocolo {

        @Override
        public String nome() {
            return "peterson";
        }

        @Override
        public int[] memoriaInicial() {
            return new int[] {0, 0, 0};
        }

        @Override
        public int critica() {
            return 4;
        }

        @Override
        public int passos() {
            return 6;
        }

        @Override
        public Estado passo(Estado atual, int p) {
            int outro = 1 - p;
            int[] pc = atual.pc.clone();
            int[] mem = atual.mem.clone();

            switch (atual.pc[p]) {
                case 1 -> {
                    mem[p] = 1;
                    pc[p] = 2;
                }
                case 2 -> {
                    mem[2] = outro;
                    pc[p] = 3;
                }
                case 3 -> {
                    if (mem[outro] == 1 && mem[2] == outro) return mesmo(atual);

                    pc[p] = 4;
                }
                case 4 -> pc[p] = 5;
                case 5 -> {
                    mem[p] = 0;
                    pc[p] = 0;
                }
                default -> throw new IllegalStateException("passo " + atual.pc[p]);
            }

            return new Estado(pc, mem, atual.voltas);
        }
    }

    /* ===================== 7. dekker ===================== */

    /** DEKKER: o primeiro que funcionou, de 1962, e o mais comprido deles. */
    public static final class Dekker extends Protocolo {

        @Override
        public String nome() {
            return "dekker";
        }

        @Override
        public int[] memoriaInicial() {
            return new int[] {0, 0, 0};
        }

        @Override
        public int critica() {
            return 7;
        }

        @Override
        public int passos() {
            return 10;
        }

        @Override
        public Estado passo(Estado atual, int p) {
            int outro = 1 - p;
            int[] pc = atual.pc.clone();
            int[] mem = atual.mem.clone();

            switch (atual.pc[p]) {
                case 1 -> {
                    mem[p] = 1;
                    pc[p] = 2;
                }
                case 2 -> pc[p] = mem[outro] == 0 ? 7 : 3;
                case 3 -> pc[p] = mem[2] == p ? 2 : 4;
                case 4 -> {
                    mem[p] = 0;
                    pc[p] = 5;
                }
                case 5 -> {
                    if (mem[2] != p) return mesmo(atual);

                    pc[p] = 6;
                }
                case 6 -> {
                    mem[p] = 1;
                    pc[p] = 2;
                }
                case 7 -> pc[p] = 8;
                case 8 -> {
                    mem[2] = outro;
                    pc[p] = 9;
                }
                case 9 -> {
                    mem[p] = 0;
                    pc[p] = 0;
                }
                default -> throw new IllegalStateException("passo " + atual.pc[p]);
            }

            return new Estado(pc, mem, atual.voltas);
        }
    }

    /* ===================== 8. a padaria ===================== */

    /**
     * A PADARIA DE LAMPORT: pego uma senha maior que a do outro, e entra quem tem a menor.
     *
     * <p>Com empate, o número do processo desempata. A senha é escolhida sem atomicidade
     * nenhuma, e é por isso que existe o passo "estou escolhendo".
     */
    public static final class Padaria extends Protocolo {

        @Override
        public String nome() {
            return "padaria de lamport";
        }

        @Override
        public int[] memoriaInicial() {
            return new int[] {0, 0, 0, 0};
        }

        @Override
        public int critica() {
            return 6;
        }

        @Override
        public int passos() {
            return 8;
        }

        @Override
        public Estado passo(Estado atual, int p) {
            int outro = 1 - p;
            int[] pc = atual.pc.clone();
            int[] mem = atual.mem.clone();

            switch (atual.pc[p]) {
                case 1 -> {
                    mem[p] = 1;
                    pc[p] = 2;
                }
                case 2 -> {
                    mem[2 + p] = mem[2 + outro] + 1;
                    pc[p] = 3;
                }
                case 3 -> {
                    mem[p] = 0;
                    pc[p] = 4;
                }
                case 4 -> {
                    if (mem[outro] == 1) return mesmo(atual);

                    pc[p] = 5;
                }
                case 5 -> {
                    int meu = mem[2 + p];
                    int dele = mem[2 + outro];

                    if (!(dele == 0 || meu < dele || (meu == dele && p < outro)))
                        return mesmo(atual);

                    pc[p] = 6;
                }
                case 6 -> pc[p] = 7;
                case 7 -> {
                    mem[2 + p] = 0;
                    pc[p] = 0;
                }
                default -> throw new IllegalStateException("passo " + atual.pc[p]);
            }

            return new Estado(pc, mem, atual.voltas);
        }
    }
}
