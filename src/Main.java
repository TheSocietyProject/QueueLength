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
            if (ReClient.ReClientCache.INSTANCE.playerListEntries.size() != 0) {
                testForData();
            }
        }, 500L, 500L, TimeUnit.MILLISECONDS);
    }

    @SimpleEventHandler
    public void onEvent(ChatReceivedEvent e){
        String msg = e.getMessageText();
        long time = e.getTimeRecieved();

        if(msg.startsWith(Main.queueMsg))
            lastChatMsg = time + ": " + msg.split(Main.queueMsg)[1];

        if(firstChatMsg == null)
            firstChatMsg = lastChatMsg;

    }

    public void testForData(){
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

        try{

            String pos = lines[5].split(": ")[1];
            pos = pos.substring(2);

            time = lines[6].split(": ")[1].substring(2);

            position = Integer.parseInt(pos);

            // if it was able to parse the pos the data is saveable and the bot can be restarted

            save(position, time);

            getReMinecraft().reLaunch();

        } catch(Exception e){
            //logger.log("e: " + e);
            // would spam the chat otherwise...
        }



    }

    public void save(int pos, String time){

        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();

        String filename = s + "/data/QueueLengthLog" + LocalDateTime.now().getYear() + "-" + LocalDateTime.now().getMonthValue() + "-" + LocalDateTime.now().getDayOfMonth() + ".txt";
        logger.log("[SpeedLogger]: flushing data: " + pos + " and " + time + " " + firstChatMsg + " " + lastChatMsg);
        try {

            File f = new File(filename);
            if(!f.exists()){
                new File(s + "\\data").mkdir();//.createNewFile();
            }
            FileWriter fw = new FileWriter(filename, true);
            BufferedWriter bw = new BufferedWriter(fw);

            long now = System.currentTimeMillis();
            String line = now + ": " + pos + " :: " + time;
            if(firstChatMsg != null)
                line += " :: " + firstChatMsg + " :: " + lastChatMsg;

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
