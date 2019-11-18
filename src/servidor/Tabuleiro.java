package servidor;

import util.Cor;

import java.io.Serializable;

public class Tabuleiro implements Serializable {
    public Peca[][] tabuleiro = new Peca[8][8];
    private int qtdeBrancas;
    private int qtdePretas;

    public Tabuleiro(){
        inicializaTabuleiro();
    }

    private void inicializaTabuleiro(){
        int i , j;
        qtdeBrancas = 12;
        qtdePretas = 12;
        //inicializa peças do jogador 1
        for(i = 0;i < 3;i ++){
                if(i % 2 == 0)
                    j = 1;
                else
                    j = 0;
            for(;j < 8;j += 2){
                tabuleiro[i][j] = new Peca(Cor.PRETA, i, j);
            }
        }

        //inicializa peças do jogador 2
        for(i = 5;i < 8;i ++){
            if(i % 2 == 0)
                j = 1;
            else
                j = 0;
            for(;j < 8;j += 2){
                tabuleiro[i][j] = new Peca(Cor.BRANCA, i, j);
            }
        }
    }

    public Peca getPeca(int i, int j){
        Peca p = this.tabuleiro[i][j];
        return p;
    }

    public int getQtdeBrancas() {
        return qtdeBrancas;
    }

    public void setQtdeBrancas(int qtdeBrancas) {
        this.qtdeBrancas = qtdeBrancas;
    }

    public int getQtdePretas() {
        return qtdePretas;
    }

    public void setQtdePretas(int qtdePretas) {
        this.qtdePretas = qtdePretas;
    }
}
