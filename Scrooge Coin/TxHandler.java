import java.util.*;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
	private UTXOPool uPool;
	
    public TxHandler(UTXOPool utxoPool) {
        uPool = new UTXOPool(utxoPool);
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
    	double inputValue = 0;
    	double outputValue = 0;
    	UTXOPool newUTxoPool = new UTXOPool();
    	
    	for (int j = 0; j < tx.numInputs(); j++) {
    		Transaction.Input ip = tx.getInput(j);
    		UTXO utxo = new UTXO(ip.prevTxHash, ip.outputIndex);
    		Transaction.Output op = uPool.getTxOutput(utxo);
    		if (!uPool.contains(utxo)) return false;
    		if (!Crypto.verifySignature(op.address, tx.getRawDataToSign(j), ip.signature))
    			return false;
    		if (newUTxoPool.contains(utxo)) return false;
    		newUTxoPool.addUTXO(utxo, op);
    		inputValue += op.value; 
    	}
    	for (Transaction.Output op : tx.getOutputs()) {
    		if (op.value < 0) return false;
    		outputValue += op.value;
    	}
    	return inputValue >= outputValue;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        HashSet<Transaction> hashSet = new HashSet<Transaction>();
        
        for (Transaction tx : possibleTxs) {
        	if (isValidTx(tx)) {
        		hashSet.add(tx);
        		
        		ArrayList<Transaction.Input> inputs = tx.getInputs();
        		for (Transaction.Input ip : inputs) {
        			UTXO utxo = new UTXO(ip.prevTxHash, ip.outputIndex);
        			uPool.removeUTXO(utxo);
        		}
        		for (int i = 0; i < tx.numOutputs(); i++) {
        			Transaction.Output output = tx.getOutput(i);
        			UTXO utxo = new UTXO(tx.getHash(), i);
        			uPool.addUTXO(utxo, output);
        		}
        	}
        }
        Transaction[] acceptedTx = new Transaction[hashSet.size()];
        acceptedTx = hashSet.toArray(acceptedTx);
        return acceptedTx;
    }

}
