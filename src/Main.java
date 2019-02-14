import com.sasha.eventsys.SimpleEventHandler;
import com.sasha.eventsys.SimpleListener;
import com.sasha.reminecraft.api.RePlugin;
import com.sasha.reminecraft.api.event.ChatReceivedEvent;
import com.sasha.reminecraft.client.ReClient;
import com.sasha.reminecraft.logging.ILogger;
import com.sasha.reminecraft.logging.LoggerBuilder;

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

    private boolean restarting = false;

    private boolean onServer = false;

    private String lastChatMsg, firstChatMsg;

    public static final String queueMsg = "Position in queue: ";


    public ILogger logger = LoggerBuilder.buildProperLogger("QueueLengthLog");

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

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

    public void testForData(){
        if(restarting)
            return;



        String[] lines = ReClient.ReClientCache.INSTANCE.tabHeader.getFullText().split("\n");


        /*
        [QueueSpeedometerPlugin / INFO] tabHeaderText:
            º7ºoºl2BUILDERSºr
            º7ºoºl2TOOLS     ºr

            º62b2t is full
            º6  Position in queue: ºl130
            º6       Estimated time: ºl33m



         */
        int position = -1;
        String time = null;

        if(onServer)
            restart(-1, "already joined the server");


        try{

            String pos = lines[5].split(": ")[1];
            pos = pos.substring(2);

            time = lines[6].split(": ")[1].substring(2);

            position = Integer.parseInt(pos);

            // if it was able to parse the pos the data is saveable and the bot can be restarted

            restart(position, time);



        } catch(Exception e){
            //logger.log("e: " + e);
            // would spam the chat otherwise...
        }



    }


    public void restart(int pos, String time){

        // restarting var so it doesnt save anything additional while restarting
        restarting = true;
        save(pos, time, lastChatMsg, firstChatMsg);
        getReMinecraft().reLaunch();
        restarting = false;


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
        lastChatMsg = null;
        firstChatMsg = null;


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
