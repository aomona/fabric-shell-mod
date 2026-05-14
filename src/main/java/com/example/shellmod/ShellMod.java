package com.example.shellmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

public class ShellMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("shellmod");

    @Override
    public void onInitialize() {
        LOGGER.info("ShellMod initialized");

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            var dispatcher = server.getCommandManager().getDispatcher();
            dispatcher.register(
                literal("shell")
                    .requires(source -> source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.fromLevel(4))))
                    .then(argument("cmd", greedyString())
                        .executes(ctx -> ShellCommand.execute(ctx.getSource(), getString(ctx, "cmd")))
                    )
            );
        });
    }
}
