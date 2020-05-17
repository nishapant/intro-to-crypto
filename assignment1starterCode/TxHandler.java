import java.util.ArrayList;
import java.util.HashSet;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool utxoPool;

    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        
        double inputsAmount = 0;
        double outputsAmount = 0;
        UTXOPool currentPool = new UTXOPool();

        for(int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = inputs.get(i);
            UTXO current = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output output = utxoPool.getTxOutput(current);
           
            if(!utxoPool.contains(current)) {
                return false;
            }
           
            if(!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) {
                return false;
            }

            if(currentPool.contains(current)) {
                return false;
            }

            currentPool.addUTXO(current, output);
            inputsAmount += output.value; 
        }

        for(int i = 0; i < tx.numOutputs(); i++) {
            Transaction.Output output = outputs.get(i);
            if(output.value < 0) {
                return false;
            }
            outputsAmount += output.value;
        }

        return (inputsAmount >= outputsAmount);
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        HashSet<Transaction> validTxs = new HashSet<>();

        for(Transaction tx : possibleTxs) {
            if(isValidTx(tx)) {
                validTxs.add(tx);
                for(int i = 0; i < tx.numInputs(); i++) {
                    Transaction.Input input = tx.getInputs().get(i);
                    UTXO current = new UTXO(input.prevTxHash, input.outputIndex);
                    utxoPool.removeUTXO(current);

                }

                for(int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output output = tx.getOutputs().get(i);
                    UTXO current = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(current, output);
                }
            }
        }
        Transaction[] validTxArray = new Transaction[validTxs.size()];
        return validTxs.toArray(validTxArray);
    }

}
