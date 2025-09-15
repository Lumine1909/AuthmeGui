package io.github.lumine1909.authmegui;

import fr.xephi.authme.api.v3.AuthMeApi;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.*;

public class AuthmeGui extends JavaPlugin implements Listener {

    public static AuthmeGui plugin;

    public static final Set<String> preLoginPlayers = new HashSet<>();
    public static Set<String> enabledPlayers;

    private static final Key KEY = Key.key("authmegui:listener");
    private static final Field field$ServerCommonPacketListenerImpl$closed;
    private static final Set<String> failedPlayers = new HashSet<>();

    private final ConfigHandler config = new ConfigHandler(this);

    static {
        try {
            field$ServerCommonPacketListenerImpl$closed = ServerCommonPacketListenerImpl.class.getDeclaredField("closed");
            field$ServerCommonPacketListenerImpl$closed.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        plugin = this;
        enabledPlayers = config.getEnabledPlayers();
        Bukkit.getPluginManager().registerEvents(this, this);
        ChannelInitializeListenerHolder.addListener(KEY, this::injectChannel);
        new CommandHandler(this);
    }

    @Override
    public void onDisable() {
        ChannelInitializeListenerHolder.removeListener(KEY);
        config.saveEnabledPlayers(enabledPlayers);
    }

    public void injectChannel(Channel channel) {

        channel.pipeline().addBefore("packet_handler", "authmegui_handler", new ChannelDuplexHandler() {
            private final Connection connection = (Connection) channel.pipeline().get("packet_handler");
            private String name;
            private boolean enabled = true;
            private volatile boolean success = false;

            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (enabled && msg instanceof ClientboundFinishConfigurationPacket) {
                    field$ServerCommonPacketListenerImpl$closed.set(connection.getPacketListener(), false);
                    ctx.writeAndFlush(buildDialogPacket(name));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!success) {
                            ctx.executor().submit(() -> ctx.writeAndFlush(ClientboundFinishConfigurationPacket.INSTANCE));
                        }
                        failedPlayers.add(name);
                    }, 600);
                    return;
                }
                super.write(ctx, msg, promise);
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (enabled && msg instanceof ClientIntentionPacket packet) {
                    if (packet.protocolVersion() < 771) {
                        channel.pipeline().remove("authmegui_handler");
                        enabled = false;
                    }
                }
                if (enabled && msg instanceof ServerboundHelloPacket(String name, UUID profileId)) {
                    this.name = name;
                    if (!enabledPlayers.contains(name)) {
                        channel.pipeline().remove("authmegui_handler");
                        enabled = false;
                    }
                }
                if (enabled && msg instanceof ServerboundCustomClickActionPacket(ResourceLocation id, Optional<Tag> payload)) {
                    if (id.toString().equals("authmegui:login") && payload.isPresent()) {
                        String password = ((CompoundTag) payload.get()).getStringOr("password", "");
                        if (AuthMeApi.getInstance().checkPassword(name, password)) {
                            ctx.writeAndFlush(ClientboundFinishConfigurationPacket.INSTANCE);
                            preLoginPlayers.add(name);
                            success = true;
                            channel.pipeline().remove("authmegui_handler");
                        } else {
                            ctx.writeAndFlush(new ClientboundDisconnectPacket(Component.literal("密码错误").withColor(0xee0000)));
                        }
                    } else if (id.toString().equals("authmegui:quit")) {
                        ctx.writeAndFlush(new ClientboundDisconnectPacket(Component.literal("退出登录").withColor(0xee0000)));
                    }
                    return;
                }
                super.channelRead(ctx, msg);
            }
        });
    }


    private static ClientboundShowDialogPacket buildDialogPacket(String name) {
        var dialog = new MultiActionDialog(
            new CommonDialogData(
                Component.literal("登录 SenCraft 服务器").withColor(0x66ccff),
                Optional.of(Component.literal("欢迎你 " + name).withColor(0xccffcc)),
                false, true, DialogAction.CLOSE,
                List.of(),
                List.of(new Input("password", new TextInput(200, Component.literal("密码"), true, "", 200, Optional.empty())))
            ),
            List.of(new ActionButton(
                new CommonButtonData(Component.literal("登录"), Optional.empty(), 100),
                Optional.of(new CustomAll(ResourceLocation.parse("authmegui:login"), Optional.empty()))
            )),
            Optional.of(new ActionButton(
                new CommonButtonData(Component.literal("退出"), Optional.empty(), 100),
                Optional.of(new CustomAll(ResourceLocation.parse("authmegui:quit"), Optional.empty()))
            )),
            3
        );
        return new ClientboundShowDialogPacket(Holder.direct(dialog));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (preLoginPlayers.contains(e.getPlayer().getName())) {
            AuthMeApi.getInstance().forceLogin(e.getPlayer());
            preLoginPlayers.remove(e.getPlayer().getName());
        }
        if (failedPlayers.contains(e.getPlayer().getName())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> e.getPlayer().sendMessage(net.kyori.adventure.text.Component.text("[AuthmeGui] Gui登录超时, 如果你没有看到登录Gui, 请报告管理员", NamedTextColor.RED)), 10);
            failedPlayers.remove(e.getPlayer().getName());
        }
    }
}
