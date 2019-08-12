package us.myles.ViaVersion.glowstone.platform;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import us.myles.ViaVersion.GlowstonePlugin;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.platform.ViaPlatformLoader;
import us.myles.ViaVersion.glowstone.listeners.UpdateListener;
import us.myles.ViaVersion.glowstone.providers.GlowstoneBlockConnectionProvider;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.blockconnections.providers.BlockConnectionProvider;

import java.util.HashSet;
import java.util.Set;

public class GlowstoneViaLoader implements ViaPlatformLoader {
    private GlowstonePlugin plugin;

    private Set<Listener> listeners = new HashSet<>();
    private Set<BukkitTask> tasks = new HashSet<>();

    public GlowstoneViaLoader(GlowstonePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(storeListener(listener), plugin);
    }

    public <T extends Listener> T storeListener(T listener) {
        listeners.add(listener);
        return listener;
    }

    @Override
    public void load() {
        // Update Listener
        registerListener(new UpdateListener());

        /* Base Protocol */
        registerListener(new Listener() {
            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent e) {
                Via.getManager().removePortedClient(e.getPlayer().getUniqueId());
            }
        });

        /* Providers */
        if (Via.getConfig().getBlockConnectionMethod().equalsIgnoreCase("world")) {
            Via.getManager().getProviders().use(BlockConnectionProvider.class, new GlowstoneBlockConnectionProvider());
        }
    }

    @Override
    public void unload() {
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
        for (BukkitTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
    }
}
