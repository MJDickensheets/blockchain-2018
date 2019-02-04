import java.util.ArrayList;
import java.util.Enumeration;
public class Block {

    private int blk_number;
    private int nonce;
    private String prev;

    private ArrayList<Transaction> transactions;


    public Block(int blk_number, String prev) {
        this.blk_number = blk_number;
        this.nonce = 0;
        this.prev = prev;
        this.transactions = new ArrayList<>();
    }

    //Increment nonce
    public void updateNonce(){
        nonce++;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public void setPrev(String prev) {
        this.prev = prev;
    }

    public double balance(String name){
        double balance = 0;
        for(int i = 0; i < transactions.size(); i++){
            Transaction t = transactions.get(i);
            if(t.sender.equals(name)){
                balance -= t.amount;
            }
            else if(t.recipient.equals(name)){
                balance += t.amount;
            }
        }
        return balance;
    }

    //Add transaction to ledger
    public void addTransaction(double amount, String from, String to){
            Transaction t = new Transaction(from, to, amount);
            transactions.add(t);
    }

    //Return length of ledger
    public int numTrans(){
        return transactions.size();
    }

    public String toString(){
        String s = "";
        s += blk_number + " ";
        s += nonce + " ";
        s += prev + " ";
        for(int i = 0; i < transactions.size(); i++)
            s += transactions.get(i).toString() + " ";
        return s;
    }

    //Define Transaction
    private class Transaction{
        String sender;
        String recipient;
        double amount;

        public Transaction(String sender, String recipient, double amount) {
            this.sender = sender;
            this.recipient = recipient;
            this.amount = amount;
        }

        public String toString(){
            String s = "";
            s += amount + " ";
            s += sender + " ";
            s += recipient;
            return s;
        }
    }
}

