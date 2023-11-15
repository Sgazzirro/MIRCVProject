package it.unipi.utils;

public class EliasFanoStruct {
    private int U;
    private int n;
    private byte [] lowBytes;
    private byte [] highBytes;

    public EliasFanoStruct(int u, int n, byte[] lowBytes, byte[] highBytes) {
        U = u;
        this.n = n;
        this.lowBytes = lowBytes;
        this.highBytes = highBytes;
    }

    public int getU() {
        return U;
    }

    public void setU(int u) {
        U = u;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public byte[] getLowBytes() {
        return lowBytes;
    }

    public void setLowBytes(byte[] lowBytes) {
        this.lowBytes = lowBytes;
    }

    public byte[] getHighBytes() {
        return highBytes;
    }

    public void setHighBytes(byte[] highBytes) {
        this.highBytes = highBytes;
    }
}
