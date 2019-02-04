import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;
import java.util.Random;
import java.io.PrintWriter;
import java.util.Scanner;
import java.io.File;

public class Miner {

    private int node_id;
    private ArrayList<Block> chain;
    private Block current;
    private PrintWriter writer;

    //AWS
    public AWSClient queue;
    private String urlBase = "https://sqs.us-east-1.amazonaws.com/150940258986/Queue";

    //Clients
    private String[] clients = {"Clarence", "Vivian", "Beatrice", "Daniel", "Bryce", "Savanna", "Mary", "Michael"};

    public Miner(int node_id){
        this.node_id = node_id;
        this.chain = new ArrayList<Block>();
        this.current = new Block(1, "0000000000000000000000000000000000000000000000000000000000000000");
        try {
            this.writer = new PrintWriter("blockchain_" + node_id + ".txt", "UTF-8");
        }catch (IOException e) {System.out.println("PrintWriter Creation failed");}
        this.queue = new AWSClient(node_id);
        queue.createQueue();
        queue.purgeQueue();
    }

    public void init(){
        for(int i = 0; i < clients.length; i++){
            current.addTransaction(100, "Bank", clients[i]);
        }
        mine();
    }

    public void mine(){
        System.out.println("Mining...");
        try {
            String hash = hash(current.toString());
            while(!hash.substring(0,3).equals("000")){
                //Break if new block received
                if(checkIncoming()) return;
                current.updateNonce();
                hash = hash(current.toString());
                //System.out.println(hash);
            }
            System.out.println("Adding Block " + (chain.size()+1));
            writeToFile(current.toString());
            chain.add(current);
            //check if chain has just initialized
            if(chain.size() > 1) sendBlock(current);
            current = new Block(chain.size()+1, hash);
        }catch(NoSuchAlgorithmException e){ System.out.println("No Such Algorithm"); }
    }

    public void tryMining(){
        if(current.numTrans()==5){
            mine();
        }
    }

    public void generateTransaction(){
        Random r = new Random();
        int from = r.nextInt(8);
        int to = r.nextInt(8);
        //make sure sender and recipient are different
        while(from == to){
            to = r.nextInt(8);
        }
        double amount = r.nextDouble() * 100;
        if(checkValid(clients[from], amount)){
            current.addTransaction(amount, clients[from], clients[to]);
        }

    }

    //traverse chain to see if a spender has the liquidity to make the transaction
    public boolean checkValid(String from, double amount){
        double balance = 0;
        //check transactions in current block
        balance += current.balance(from);
        //check transactions in previous blocks
        for(int i = chain.size()-1; i >= 0; i--){
            balance += chain.get(i).balance(from);
            if(balance > amount) return true;
        }
        return false;
    }

    public boolean checkIncoming(){
        String message = queue.receiveMessage();
        if(message != null){
            System.out.println("Received block!");
            addBlock(message);
            return true;
        }
        return false;
    }

    public void addBlock(String message){
        Scanner s = new Scanner(message);
        int block_number = s.nextInt();
        int nonce = s.nextInt();
        String prev = s.next();

        Block incoming = new Block(block_number, prev);
        incoming.setNonce(nonce);

        for(int i = 0; i < 5; i++){
            //import transactions
            incoming.addTransaction(s.nextDouble(), s.next(), s.next());
        }



        try {
            if(prev.equals(hash(chain.get(chain.size()-1).toString()))){
                chain.add(incoming);
                writeToFile(incoming.toString());
                current = new Block(chain.size() + 1, hash(message));
                System.out.println("Incoming block verified. Adding to chain.");
            }
        }catch(NoSuchAlgorithmException e){}
    }

    public void sendBlock(Block b){
        String output = b.toString();
        for(int i = 1; i < 5; i++){
            if(i!=node_id) queue.sendMessage(urlBase+i, output);
        }
    }

    public void writeToFile(String s){

        writer.println(s);
        writer.flush();
    }

    public String hash(String input) throws NoSuchAlgorithmException{
        MessageDigest hasher = MessageDigest.getInstance( "SHA-256" );
        hasher.update(input.getBytes(StandardCharsets.UTF_8));
        byte[] hashed = hasher.digest();

        return String.format("%064x", new BigInteger(1, hashed));
    }
}
