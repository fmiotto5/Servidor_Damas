package util;

import servidor.Tabuleiro;

import java.io.Serializable;

public class Mensagem implements Serializable {
    private Tabuleiro tabuleiro;
    private int iOrig, jOrig;
    private int iDest, jDest;
    private int idJogador;
    private Status s;

    public Tabuleiro getTabuleiro() {
        return tabuleiro;
    }

    public void setTabuleiro(Tabuleiro tabuleiro) {
        this.tabuleiro = tabuleiro;
    }

    public int getiOrig() {
        return iOrig;
    }

    public void setiOrig(int iOrig) {
        this.iOrig = iOrig;
    }

    public int getjOrig() {
        return jOrig;
    }

    public void setjOrig(int jOrig) {
        this.jOrig = jOrig;
    }

    public int getiDest() {
        return iDest;
    }

    public void setiDest(int iDest) {
        this.iDest = iDest;
    }

    public int getjDest() {
        return jDest;
    }

    public void setjDest(int jDest) {
        this.jDest = jDest;
    }

    public Status getS() {
        return s;
    }

    public void setS(Status s) {
        this.s = s;
    }

    public int getIdJogador() {
        return idJogador;
    }

    public void setIdJogador(int idJogador) {
        this.idJogador = idJogador;
    }
}
