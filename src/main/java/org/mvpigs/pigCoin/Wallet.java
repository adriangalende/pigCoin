package org.mvpigs.pigCoin;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Wallet {
    private PublicKey address;
    private PrivateKey SK;
    private double totalInput = 0.0d;
    private double totalOutput = 0.0d;
    private double balance = 0.0d;
    private ArrayList<Transaction> inputTransactions;
    private ArrayList<Transaction> outputTransactions;

    public void setAddress(PublicKey address) {
        this.address = address;
    }

    public PublicKey getAddress() {
        return this.address;
    }

    public void setSK(PrivateKey SK) {
        this.SK = SK;
    }

    public PrivateKey getSK() {
        return this.SK;
    }

    public void setTotalInput(double input) {
        this.totalInput = input;
    }

    public void setTotalOutput(double output) {
        this.totalOutput = output;
    }

    public void generateKeyPair() {
        KeyPair pair = GenSig.generateKeyPair();
        setAddress(pair.getPublic());
        setSK(pair.getPrivate());
    }

    public double getTotalInput() {
        return this.totalInput;
    }

    public double getTotalOutput() {
        return this.totalOutput;
    }

    public double getBalance() {
        return this.balance;
    }

    public void setBalance(){
        updateBalance();
    }

    public void updateBalance() {
        double balance = getTotalInput() - getTotalOutput();
        if (balance >= 0) {
            this.balance = balance;
        }
    }

    @Override
    public String toString() {
        String output = "";
        output += " \n Wallet = " + getAddress().hashCode() + "\n";
        output += " total input: " + getTotalInput() + "\n";
        output += " total output: " + getTotalOutput() + "\n";
        output += " Balance: " + getBalance() + "\n";
        return output;
    }

    public void loadCoins(BlockChain blockChain) {
        setTotalOutput(blockChain.loadWallet(getAddress())[1]);
        setTotalInput(blockChain.loadWallet(getAddress())[0]);
        setBalance();
    }

    public void loadInputTransactions(BlockChain blockChain) {
        setInputTransactions(blockChain.loadInputTransactions(getAddress()));   
    }

    public void setInputTransactions(ArrayList<Transaction> inputTransactions){
        this.inputTransactions = new ArrayList();
        this.inputTransactions = inputTransactions;
    }

    public ArrayList<Transaction> getInputTransactions() {
        return this.inputTransactions;
    }

    public void loadOutputTransactions(BlockChain blockChain) {
        setOutputTransactions(blockChain.loadOutputTransactions(getAddress()));   
    }

    public void setOutputTransactions(ArrayList<Transaction> outputTransaction) {
        this.outputTransactions = new ArrayList();
        this.outputTransactions = outputTransaction;
    }

    public ArrayList<Transaction> getOutputTransactions() {
        return this.outputTransactions;
    }

    public boolean esTransaccionConsumida(String hash) {
        for (Transaction transaccion : getOutputTransactions()) {
            if (transaccion.getPrevHash().equals(hash)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Double> getTransaccionesConsumidas() {
        Map<String, Double> transaccionesConsumidas = new TreeMap();

        for (Transaction transaccion : getInputTransactions()) {
            // no encontramos el hash de la transaccion actual en la lista de
            // output transactions
            if (esTransaccionConsumida(transaccion.getHash())) {
                transaccionesConsumidas.put(transaccion.getHash(), transaccion.getPigCoins());
            }
        }
        return transaccionesConsumidas;
    }

    public Map<String, Double> collectCoins(double pigCoins) {

        Map<String, Double> transaccionesConsumidas = getTransaccionesConsumidas();
        Map<String, Double> transaccionesDisponibles = new TreeMap();
        Transaction transaccion;
        if (getBalance() >= pigCoins) {
            int i = 0;
            while (pigCoins > 0) {
                transaccion = getInputTransactions().get(i);
                if (!(transaccionesConsumidas.containsKey(transaccion.getHash()))) {
                    if (pigCoins == transaccion.getPigCoins()) {
                        transaccionesDisponibles.put(transaccion.getHash(), transaccion.getPigCoins());
                        pigCoins = 0;
                    } else if (pigCoins < transaccion.getPigCoins()) {
                        transaccionesDisponibles.put(transaccion.getHash(), pigCoins);
                        //CHANGE ADDRESS
                        double cambio = transaccion.getPigCoins() - pigCoins;
                        transaccionesDisponibles.put("CA_" + transaccion.getHash(), cambio);
                        pigCoins = 0;
                    } else {
                        transaccionesDisponibles.put(transaccion.getHash(), transaccion.getPigCoins());
                        pigCoins -= transaccion.getPigCoins();
                    }
                }
                i++;
            }
        }

        return transaccionesDisponibles;
    }

    public byte[] signTransaction(String message) {
        return GenSig.sign(getSK(), message);
    }

    public void sendCoins(PublicKey address, double pigcoins, String message, BlockChain bChain) {
        Map<String, Double> consumedCoins = collectCoins(pigcoins);
        byte[] messageSignature = signTransaction(message);
        bChain.processTransactions(getAddress(), address, consumedCoins, message, messageSignature);
    }

}