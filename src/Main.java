import com.sasha.eventsys.SimpleEventHandler;
import com.sasha.eventsys.SimpleListener;
import com.sasha.reminecraft.ReMinecraft;
import com.sasha.reminecraft.api.RePlugin;
import com.sasha.reminecraft.api.event.ChatReceivedEvent;
import com.sasha.reminecraft.client.ReClient;
import com.sasha.reminecraft.logging.ILogger;
import com.sasha.reminecraft.logging.LoggerBuilder;
import com.sasha.reminecraft.util.TextMessageColoured;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends RePlugin implements SimpleListener {

    public static final String queueMsg = "Position in queue: ";


    private boolean onServer = false;

    private String lastChatMsg, firstChatMsg;



    public ILogger logger = LoggerBuilder.buildProperLogger("QueueLengthLog");

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);


    @Override
    public void onPluginInit() {
        this.getReMinecraft().EVENT_BUS.registerListener(this);
    }

    @Override
    public void onPluginEnable() {
        executor.scheduleAtFixedRate(() -> {
            testForData();
        }, 2000L, 500L, TimeUnit.MILLISECONDS);
    }

    @SimpleEventHandler
    public void onEvent(ChatReceivedEvent e){
        String msg = e.getMessageText();
        long time = e.getTimeRecieved();

        if(msg.startsWith("<"))
            onServer = true;

        if(msg.startsWith(Main.queueMsg))
            lastChatMsg = time + ": " + msg.split(Main.queueMsg)[1];

        if(firstChatMsg == null)
            firstChatMsg = lastChatMsg;

    }

    public synchronized void testForData(){

        if(onServer) {
            restart(-1, "already joined the server");
            onServer = false;
            return;
        }


        String[] lines = ReClient.ReClientCache.INSTANCE.tabHeader.getFullText().split("\n");


        /*
        tabHeaderText:
            º7ºoºl2BUILDERSºr
            º7ºoºl2TOOLS     ºr

            º62b2t is full
            º6  Position in queue: ºl130
            º6       Estimated time: ºl33m



         */
        int position = -1;
        String time = null;



        try{

            if(!lines[1].equals("QueueLengthData saved: "))
                ReClient.ReClientCache.INSTANCE.tabHeader = TextMessageColoured.from("\n&7 QueueLengthData saving\n");


                String pos = lines[5].split(": ")[1];
            pos = pos.substring(2);

            time = lines[6].split(": ")[1].substring(2);

            position = Integer.parseInt(pos);


            ReClient.ReClientCache.INSTANCE.tabHeader = TextMessageColoured.from("\nQueueLengthData saved: \n" + position + " " + time + "\n");


            // if it was able to parse the pos the data is saveable and the bot can be restarted
            restart(position, time);



        } catch(Exception e){
            //logger.log("e: " + e);
            // would spam the chat otherwise...
        }


    }


    public void restart(int pos, String time){

        save(pos, time, lastChatMsg, firstChatMsg);

        ReconnectManager.reconnect();
        logger.log("should have restarted now...");

        lastChatMsg = null;
        firstChatMsg = null;

    }

    public void save(int pos, String time, String last, String first){

        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();

        String filename = s + "/data/QueueLengthLog" + LocalDateTime.now().getYear() + "-" + LocalDateTime.now().getMonthValue() + "-" + LocalDateTime.now().getDayOfMonth() + ".txt";
        logger.log("flushing data: " + pos + " and " + time + " " + first + " " + last);
        try {

            File f = new File(filename);
            if(!f.exists()){
                new File(s + "\\data").mkdir();//.createNewFile();
            }
            FileWriter fw = new FileWriter(filename, true);
            BufferedWriter bw = new BufferedWriter(fw);

            long now = System.currentTimeMillis();
            String line = now + ": " + pos + " :: " + time;
            if(first != null)
                line += " :: " + first + " :: " + last;

            bw.newLine();
            bw.write(line);
            bw.flush();

            fw.flush();
            bw.close();
            fw.close();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    @Override
    public void onPluginDisable() {

    }

    @Override
    public void onPluginShutdown() {

    }

    @Override
    public void registerCommands() {

    }

    @Override
    public void registerConfig() {

    }
}

