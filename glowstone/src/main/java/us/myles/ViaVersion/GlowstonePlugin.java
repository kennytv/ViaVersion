package us.myles.ViaVersion;

import com.google.gson.JsonObject;
import lombok.Getter;
import net.glowstone.GlowServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaAPI;
import us.myles.ViaVersion.api.command.ViaCommandSender;
import us.myles.ViaVersion.api.configuration.ConfigurationProvider;
import us.myles.ViaVersion.api.platform.TaskId;
import us.myles.ViaVersion.api.platform.ViaPlatform;
import us.myles.ViaVersion.dump.PluginInfo;
import us.myles.ViaVersion.glowstone.commands.GlowstoneCommandHandler;
import us.myles.ViaVersion.glowstone.commands.GlowstoneCommandSender;
import us.myles.ViaVersion.glowstone.platform.*;
import us.myles.ViaVersion.util.GsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GlowstonePlugin extends JavaPlugin implements ViaPlatform {

    private GlowstoneCommandHandler commandHandler;
    @Getter
    private GlowstoneViaConfig conf;
    @Getter
    private ViaAPI<Player> api = new GlowstoneViaAPI(this);
    private List<Runnable> queuedTasks = new ArrayList<>();
    private List<Runnable> asyncQueuedTasks = new ArrayList<>();

    public GlowstonePlugin() {
        // Command handler
        commandHandler = new GlowstoneCommandHandler();
        // Init platform
        Via.init(ViaManager.builder()
                .platform(this)
                .commandHandler(commandHandler)
                .injector(new GlowstoneViaInjector())
                .loader(new GlowstoneViaLoader(this))
                .build());
        // Config magic
        conf = new GlowstoneViaConfig();
    }

    @Override
    public void onEnable() {
        new Thread(this::waitforInit).start();

        getCommand("viaversion").setExecutor(commandHandler);
        getCommand("viaversion").setTabCompleter(commandHandler);

        // Run queued tasks
        for (Runnable r : queuedTasks) {
            Bukkit.getScheduler().runTask(this, r);
        }
        queuedTasks.clear();

        // Run async queued tasks
        for (Runnable r : asyncQueuedTasks) {
            Bukkit.getScheduler().runTaskAsynchronously(this, r);
        }
        asyncQueuedTasks.clear();
    }

    private void waitforInit() {
        GlowServer server = (GlowServer) getServer();
        while (server.getNetworkServer() == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Via.getPlatform().getLogger().info("Injecting...");
        Via.getManager().init();
    }

    @Override
    public void onDisable() {
        Via.getManager().destroy();
    }

    @Override
    public String getPlatformName() {
        return Bukkit.getServer().getName();
    }

    @Override
    public String getPlatformVersion() {
        return Bukkit.getServer().getVersion();
    }

    @Override
    public String getPluginVersion() {
        return getDescription().getVersion();
    }

    @Override
    public TaskId runAsync(Runnable runnable) {
        if (isPluginEnabled()) {
            return new GlowstoneTaskId(getServer().getScheduler().runTaskAsynchronously(this, runnable).getTaskId());
        } else {
            asyncQueuedTasks.add(runnable);
            return new GlowstoneTaskId(null);
        }
    }

    @Override
    public TaskId runSync(Runnable runnable) {
        if (isPluginEnabled()) {
            return new GlowstoneTaskId(getServer().getScheduler().runTask(this, runnable).getTaskId());
        } else {
            queuedTasks.add(runnable);
            return new GlowstoneTaskId(null);
        }
    }

    @Override
    public TaskId runSync(Runnable runnable, Long ticks) {
        return new GlowstoneTaskId(getServer().getScheduler().runTaskLater(this, runnable, ticks).getTaskId());
    }

    @Override
    public TaskId runRepeatingSync(Runnable runnable, Long ticks) {
        return new GlowstoneTaskId(getServer().getScheduler().runTaskTimer(this, runnable, 0, ticks).getTaskId());
    }

    @Override
    public void cancelTask(TaskId taskId) {
        if (taskId == null) return;
        if (taskId.getObject() == null) return;
        if (taskId instanceof GlowstoneTaskId) {
            getServer().getScheduler().cancelTask((Integer) taskId.getObject());
        }
    }

    @Override
    public ViaCommandSender[] getOnlinePlayers() {
        ViaCommandSender[] array = new ViaCommandSender[Bukkit.getOnlinePlayers().size()];
        int i = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            array[i++] = new GlowstoneCommandSender(player);
        }
        return array;
    }

    @Override
    public void sendMessage(UUID uuid, String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    @Override
    public boolean kickPlayer(UUID uuid, String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.kickPlayer(message);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isPluginEnabled() {
        return Bukkit.getPluginManager().getPlugin("ViaVersion").isEnabled();
    }

    @Override
    public ConfigurationProvider getConfigurationProvider() {
        return conf;
    }

    @Override
    public void onReload() {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            getLogger().severe("ViaVersion is already loaded, we're going to kick all the players...");
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer(ChatColor.translateAlternateColorCodes('&', getConf().getReloadDisconnectMsg()));
            }

        } else {
            getLogger().severe("ViaVersion is already loaded, this should work fine. If you get any console errors, try rebooting.");
        }
    }

    @Override
    public JsonObject getDump() {
        JsonObject platformSpecific = new JsonObject();

        List<PluginInfo> plugins = new ArrayList<>();
        for (Plugin p : Bukkit.getPluginManager().getPlugins())
            plugins.add(new PluginInfo(p.isEnabled(), p.getDescription().getName(), p.getDescription().getVersion(), p.getDescription().getMain(), p.getDescription().getAuthors()));

        platformSpecific.add("plugins", GsonUtil.getGson().toJsonTree(plugins));

        return platformSpecific;
    }

    @Override
    public boolean isOldClientsAllowed() {
        return true;
    }
}
