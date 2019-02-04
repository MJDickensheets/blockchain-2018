import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
public class Driver {

    public static void main (String [] args) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        Scanner s = new Scanner(System.in);
        System.out.println("Enter Node ID");
        int node_id = s.nextInt();

        System.out.println("Proof of Work? (W) Proof of Stake? (S)");
        String choice = s.next();

        boolean quit = false;
        int timeout = 2;

        if(choice.equals("W")||choice.equals("w")) {
            Miner m = new Miner(node_id);
            Random r = new Random();
            m.init();

            while (!quit) {

                try {
                    long startTime = System.currentTimeMillis();
                    while ((System.currentTimeMillis() - startTime) < timeout * 1000 && !in.ready()) {
                        try {
                            TimeUnit.SECONDS.sleep(r.nextInt(1));
                            m.generateTransaction();
                            m.tryMining();
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    if (in.ready()) {
                        String cmd = in.readLine();

                        if (cmd.equals("q") || cmd.equals("q\n")) {
                            quit = true;
                        }
                    }
                } catch (IOException e) {
                }

            }
        }
        else{
            POSMiner m = new POSMiner(node_id);
            m.init();

            while(!quit){
                try {
                    long startTime = System.currentTimeMillis();
                    while ((System.currentTimeMillis() - startTime) < timeout * 1000 && !in.ready()) {
                        System.out.println("Starting new time period.");
                        m.createBlock();
                        m.wait_state();
                    }
                    if (in.ready()) {
                        String cmd = in.readLine();

                        if (cmd.equals("q") || cmd.equals("q\n")) {
                            quit = true;
                        }
                    }
                } catch (IOException e) {
                }
            }
        }


    }
}
