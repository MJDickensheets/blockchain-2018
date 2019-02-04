import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AWSClient {

    final AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
    final AmazonSQS sqs;
    String queueUrl;

    private int nodeID;

    public AWSClient(int nodeID){
        builder.setRegion("us-east-1");
        this.sqs = builder.build();
        this.nodeID = nodeID;
    }

    public void createQueue(){
        //Queue Creation
        System.out.println("Creating queue for node " + nodeID);
        final CreateQueueRequest createQueueRequest = new CreateQueueRequest("Queue"+nodeID);
        final String queueURL = sqs.createQueue(createQueueRequest).getQueueUrl();
        this.queueUrl = queueURL;

        //List queues
        System.out.println("Waiting for all nodes to come online...");
        try {
            while (sqs.listQueues().getQueueUrls().size() < 4) {
                TimeUnit.SECONDS.sleep(1);
            }
        }catch(InterruptedException e){
            System.out.println("Caught Interrupt Exception");
        }
        System.out.println("Queues online:");
        for (final String globalqueueURL : sqs.listQueues().getQueueUrls()) {
            System.out.println("  QueueUrl: " + globalqueueURL);
        }
    }

    public void sendMessage(String url, String s){
        //Send Message
        sqs.sendMessage(url, s);

    }

    public String receiveMessage(){
        final ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        final List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();

        if(!messages.isEmpty()) {
            //print message
            String contents = messages.get(0).getBody();
            //delete message
            sqs.deleteMessage(new DeleteMessageRequest(queueUrl, messages.get(0).getReceiptHandle()));

            return contents;
        }
        return null;

    }

    public void purgeQueue(){
        PurgeQueueRequest request = new PurgeQueueRequest(queueUrl);
        sqs.purgeQueue(request);
    }
}
