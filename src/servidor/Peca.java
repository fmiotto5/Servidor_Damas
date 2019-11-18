package servidor;

import util.Cor;

import java.io.Serializable;

public class Peca implements Serializable {
    private Cor cor;
    private int i, j;
    private boolean isKing;

    public Peca(Cor cor, int i, int j){
        this.cor = cor;
        this.i = i;
        this.j = j;
        this.isKing = false;
    }

    public Cor getCor() {
        return cor;
    }

    public void setCor(Cor cor) {
        this.cor = cor;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public int getJ() {
        return j;
    }

    public void setJ(int j) {
        this.j = j;
    }

    public boolean isKing() {
        return isKing;
    }

    public void setKing(boolean king) {
        isKing = king;
    }
}
