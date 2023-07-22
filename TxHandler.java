import java.util.*;

public class TxHandler {

    public UTXOPool ledger;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.ledger = new UTXOPool(utxoPool);
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
        // IMPLEMENT THIS
        // (1)
        for (Transaction.Input input : tx.getInputs()){
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (!ledger.contains(utxo)) {
                return false;
            }
        }

        // (2)
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output output = ledger.getTxOutput(utxo);
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) {
                return false;
            }
        }

        // (3)
        HashSet<UTXO> claimedUTXOs = new HashSet<>();
        for (Transaction.Input input : tx.getInputs()) {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (claimedUTXOs.contains(utxo)) {
                return false;
            }
            claimedUTXOs.add(utxo);
        }

        // (4)
        for(Transaction.Output output : tx.getOutputs()){
            if(output.value < 0){
                return false;
            }
        }

        // (5)
        double input_sum = 0;
        for(Transaction.Input input : tx.getInputs()){
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output output = ledger.getTxOutput(utxo);
            input_sum += output.value;
        }
        double output_sum = 0;
        for(Transaction.Output output: tx.getOutputs()){
            output_sum += output.value;
        }

        return input_sum >= output_sum;

    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> validTxs = new ArrayList<>();

        for(Transaction tx: possibleTxs){
            if(isValidTx(tx)){
                validTxs.add(tx);

                for(Transaction.Input input: tx.getInputs()){
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    this.ledger.removeUTXO(utxo);
                }

                for(int i = 0; i < tx.numOutputs(); i++){
                    Transaction.Output output = tx.getOutput(i);
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    this.ledger.addUTXO(utxo, output);
                }
            }
        }

        return validTxs.toArray(new Transaction[validTxs.size()]);
    }

}
