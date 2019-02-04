import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class POSMiner {

    private int node_id;
    private ArrayList<POSBlock> chain;
    private POSBlock current;
    private PrintWriter writer;

    private static double p = 0.3;

    private int stake;
    private boolean voted;
    private int votes;
    private int voter_stake;
    private double p_choice;
    private boolean refresh;
    private boolean leader;

    //AWS
    public AWSClient queue;
    private String urlBase = "https://sqs.us-east-1.amazonaws.com/150940258986/Queue";

    //Clients
    private String[] clients = {"Clarence", "Vivian", "Beatrice", "Daniel", "Bryce", "Savanna", "Mary", "Michael"};

    public POSMiner(int node_id) {
        this.node_id = node_id;
        this.chain = new ArrayList<>();
        this.queue = new AWSClient(node_id);
        queue.createQueue();
        queue.purgeQueue();
        this.current = new POSBlock(1, 0);
        try {
            this.writer = new PrintWriter("blockchain_" + node_id + ".txt", "UTF-8");
        }catch (IOException e) {System.out.println("PrintWriter Creation failed");}
        this.stake = 100;
        this.voted = false;
        this.votes = 0;
        this.voter_stake = 0;
        this.refresh = false;
        this.leader = false;
    }

    public void init(){
        for(int i = 0; i < clients.length; i++){
            current.addTransaction(100, "Bank", clients[i]);
        }
        chain.add(current);
        writeToFile(current.toString());
    }

    public void generateTransaction(){
        Random r = new Random();
        while(current.numTrans()!=5) {
            int from = r.nextInt(8);
            int to = r.nextInt(8);
            //make sure sender and recipient are different
            while (from == to) {
                to = r.nextInt(8);
            }
            double amount = r.nextDouble() * 100;
            if (checkValid(clients[from], amount)) {
                current.addTransaction(amount, clients[from], clients[to]);
            }
        }
    }

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

    public void createBlock(){
        Random r = new Random();
        p_choice = r.nextDouble();
        if(p_choice < p){
            System.out.println("Creating block... requesting votes...");
            current = new POSBlock(chain.size()+1, node_id);
            for(int i = 0; i < 5; i++){
                generateTransaction();
            }
            requestVote();
        }

    }

    public void wait_state(){
        long time = System.currentTimeMillis();
        while(System.currentTimeMillis() - time < 10000 && !refresh){
            if(checkIncoming()){
                time = System.currentTimeMillis();
            }
        }
        voted = false;
        votes = 0;
        voter_stake = 0;
        refresh = false;
        leader = false;
        clearQueue();
    }

    public boolean checkIncoming(){
        String message = queue.receiveMessage();
        if(message != null){
            Scanner s = new Scanner(message);
            String type = s.next();
            //System.out.println(type);
            if(type.equals("requestvote")){
                //System.out.println("Received requestvote");
                receivedVoteRequest(s.nextInt());
                return true;
            }
            else if(type.equals("verification")){
                receivedVerificationRequest(message);
                return true;
            }
            else if(type.equals("signatures")){
                receiveSignatures(message);
                return true;
            }
            else if(type.equals("vote")){
                receivedVote();
                return true;
            }
            else if(type.equals("signing")){
                receivedSignature(message);
                return true;
            }
        }
        return false;
    }

    public void requestVote(){
        String output = "requestvote " + node_id;
        for(int i = 1; i < 5; i++){
            if(i!=node_id) queue.sendMessage(urlBase+i, output);
        }
        //vote for self
        voted = true;
        votes++;
    }

    public void receivedVote(){
        votes++;
        if(votes > 2 && !leader){
            System.out.println("Secured majority of votes, requesting verification...");
            leader = true;
            requestVerification();
        }
    }

    public void requestVerification(){
        String output = "verification " + node_id + " " + p_choice + " ";
        output += current.toString();

        for(int i = 1; i < 5; i++){
            if(i!=node_id) queue.sendMessage(urlBase+i, output);
        }
        voter_stake += stake;
    }

    public void receivedSignature(String response){
        Scanner s = new Scanner(response);
        // skip first word
        s.next();
        int id = s.nextInt();
        current.addVerifier(id);
        voter_stake += s.nextInt();
        if(voter_stake > current.transactionTotal()){
            stake += 10;
            chain.add(current);
            writeToFile(current.toString());
            String output = "signatures ";
            ArrayList<Integer> signatures = current.getVerifiers();
            for(int i = 0; i < signatures.size(); i++){
                output += signatures.get(i) + " ";
            }
            for(int i = 1; i < 5; i++){
                if(i!=node_id) queue.sendMessage(urlBase + i, output);
            }
            System.out.println("Received " + output +". Stake: " + voter_stake + "Total transactions: " + current.transactionTotal());
            System.out.println("Added block " + chain.size());
            refresh = true;
        }
    }

    public void receivedVoteRequest(int req_id){
        if(!voted){
            voted = true;
            String output = "vote " + node_id;
            queue.sendMessage(urlBase+req_id, output);
            System.out.println("Voted for " + req_id);
        }
    }

    public void receivedVerificationRequest(String request){
        Scanner s = new Scanner(request);
        //skip first word in message
        s.next();
        int var_id = s.nextInt();
        double p = s.nextDouble();
        int block_num = s.nextInt();
        s.nextInt();
        current = new POSBlock(block_num, var_id);
        for(int i=0; i < 5; i++){
            double amount = s.nextDouble();
            String sender = s.next();
            String recipient = s.next();
            if(checkValid(sender, amount)){
                current.addTransaction(amount, sender, recipient);
            }
            else{
                return;
            }
        }
        //Check p value
        if(p > 0.3) return;
        System.out.println("Verified no double spending, p = " + p + " < 0.3");
        String output = "signing " + node_id + " " + stake;
        queue.sendMessage(urlBase + var_id, output);
    }

    public void receiveSignatures(String signatures){
        Scanner s = new Scanner(signatures);
        //skip first word in message
        s.next();
        while(s.hasNextInt()){
            int id = s.nextInt();
            current.addVerifier(id);
            if(id == node_id) stake += 10;
        }
        chain.add(current);
        System.out.println("Added block " + chain.size());
        writeToFile(current.toString());
        refresh = true;
    }

    public void clearQueue(){
        while(queue.receiveMessage()!=null);
    }

    public void writeToFile(String s){

        writer.println(s + "\n");
        writer.flush();
    }

}
