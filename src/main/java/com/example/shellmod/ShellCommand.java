package com.example.shellmod;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShellCommand {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public static int execute(ServerCommandSource source, String cmd) {
        source.sendFeedback(() -> Text.literal("[Shell] 実行: " + cmd).formatted(Formatting.GRAY), false);

        EXECUTOR.submit(() -> {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            pb.redirectErrorStream(true);
            pb.environment().put("TERM", "xterm-256color");
            pb.environment().put("COLORTERM", "truecolor");
            pb.environment().put("FORCE_COLOR", "1");
            pb.environment().put("CLICOLOR_FORCE", "1");

            try {
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String rawLine = line;
                        sendFeedback(source, () -> Text.literal("[Shell] ").append(AnsiColorParser.parse(rawLine)));
                    }
                }

                int exitCode = process.waitFor();

                Formatting codeColor = (exitCode == 0) ? Formatting.GREEN : Formatting.RED;
                final int finalExitCode = exitCode;
                sendFeedback(source, () -> Text.literal("[Shell] 終了コード: " + finalExitCode).formatted(codeColor));

            } catch (IOException e) {
                sendError(source, Text.literal("[Shell] IOエラー: " + e.getMessage()).formatted(Formatting.RED));
                ShellMod.LOGGER.error("Shell command IO error", e);
            } catch (InterruptedException e) {
                sendError(source, Text.literal("[Shell] 実行が中断されました").formatted(Formatting.RED));
                ShellMod.LOGGER.error("Shell command interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                sendError(source, Text.literal("[Shell] 予期しないエラー: " + e.getMessage()).formatted(Formatting.RED));
                ShellMod.LOGGER.error("Shell command unexpected error", e);
            }
        });

        return 1;
    }

    private static void sendFeedback(ServerCommandSource source, Supplier<Text> message) {
        source.getServer().execute(() -> source.sendFeedback(message, false));
    }

    private static void sendError(ServerCommandSource source, Text message) {
        source.getServer().execute(() -> source.sendError(message));
    }
}
