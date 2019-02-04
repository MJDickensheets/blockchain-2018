import java.lang.reflect.Array;
import java.util.ArrayList;

public class POSBlock {

    private int blk_number;
    private ArrayList<Transaction> transactions;
    private int creator;
    private ArrayList<Integer> verifiers;

    public POSBlock(int blk_number, int creator){
        this.blk_number = blk_number;
        this.creator = creator;
        this.transactions = new ArrayList<>();
        this.verifiers = new ArrayList<>();
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

    public void addTransaction(double amount, String from, String to){
        Transaction t = new Transaction(from, to, amount);
        transactions.add(t);
    }

    public int numTrans(){
        return transactions.size();
    }

    public double transactionTotal(){
        double total = 0;
        for(int i = 0; i < transactions.size(); i++){
            total += transactions.get(i).amount;
        }
        return total;
    }

    public void addVerifier(Integer v){
        verifiers.add(v);
    }
    public ArrayList<Integer> getVerifiers(){
        return verifiers;
    }

    public String toString(){
        String s = "";
        s += blk_number + " ";
        s += creator + " ";
        for(int i = 0; i < transactions.size(); i++)
            s += transactions.get(i).toString() + " ";
        for(int i = 0; i < verifiers.size(); i++)
            s += verifiers.get(i) + " ";
        return s;
    }


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
