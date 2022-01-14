import com.example.MainCore;
import net.bytebuddy.agent.ByteBuddyAgent;

import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        ByteBuddyAgent.attach(new File(System.getProperty("com.example.agent")),
                ByteBuddyAgent.ProcessProvider.ForCurrentVm.INSTANCE);
        MainCore.doMain(args);
    }
}
