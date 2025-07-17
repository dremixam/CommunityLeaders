package com.dremixam.communityleaders.client.gui;

import com.dremixam.communityleaders.network.RulesResponsePacket;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class RulesScreen extends Screen {
    private final String rulesContent;
    private final String acceptButtonText;
    private final String declineButtonText;
    private final String checkboxText;
    private final String declineMessage;

    private CheckboxWidget acceptCheckbox;
    private ButtonWidget acceptButton;
    private ButtonWidget declineButton;

    private int scrollOffset = 0;
    private int maxScrollOffset; // Remove final to allow recalculation

    public RulesScreen(String title, String content, String acceptBtn, String declineBtn, String checkboxTxt, String declineMsg) {
        super(Text.literal(title));
        this.rulesContent = content;
        this.acceptButtonText = acceptBtn;
        this.declineButtonText = declineBtn;
        this.checkboxText = checkboxTxt;
        this.declineMessage = declineMsg;

        // Calculate max scroll based on number of lines - will be recalculated in render()
        String[] lines = content.split("\n");
        this.maxScrollOffset = Math.max(0, lines.length - 15); // Initial estimate
    }

    @Override
    protected void init() {
        // Calculate window dimensions
        int windowWidth = this.width - 80;
        int windowHeight = this.height - 80;
        int windowX = 40;
        int windowY = 20;

        // Recalculate scroll limits when window is resized using word wrapping
        int contentHeight = windowHeight - 120; // Same calculation as in render()
        int contentWidth = windowWidth - 40;
        int lineHeight = 12;
        int maxLines = contentHeight / lineHeight;

        // Apply word wrapping to calculate accurate line count
        String[] originalLines = rulesContent.split("\n");
        java.util.List<String> wrappedLines = new java.util.ArrayList<>();
        int maxWidth = contentWidth - 10; // Leave some margin

        for (String originalLine : originalLines) {
            if (originalLine.trim().isEmpty()) {
                wrappedLines.add(""); // Preserve empty lines
                continue;
            }

            // Check if line fits within width
            if (this.textRenderer.getWidth(originalLine) <= maxWidth) {
                wrappedLines.add(originalLine);
            } else {
                // Split long line into multiple lines with formatting preservation
                String[] words = originalLine.split(" ");
                StringBuilder currentLine = new StringBuilder();
                String activeFormatting = ""; // Track active formatting codes

                for (int i = 0; i < words.length; i++) {
                    String word = words[i];

                    // Check if next word starts with ":" to treat current word + space + ":" as non-breakable
                    boolean nextWordStartsWithColon = (i + 1 < words.length) && words[i + 1].startsWith(":");
                    String testWord = word;

                    if (nextWordStartsWithColon) {
                        // Treat word + space + colon as a single unit
                        testWord = word + " " + words[i + 1];
                    }

                    String testLine = currentLine.length() == 0 ? activeFormatting + testWord : currentLine + " " + testWord;

                    if (this.textRenderer.getWidth(testLine) <= maxWidth) {
                        currentLine = new StringBuilder(testLine);
                        // Update active formatting based on this word
                        activeFormatting = extractActiveFormatting(testLine);

                        // Skip next word if we combined it with current word
                        if (nextWordStartsWithColon) {
                            i++;
                        }
                    } else {
                        // Current line is full, start a new one
                        if (currentLine.length() > 0) {
                            wrappedLines.add(currentLine.toString());
                            currentLine = new StringBuilder(activeFormatting + testWord);
                            activeFormatting = extractActiveFormatting(currentLine.toString());

                            // Skip next word if we combined it with current word
                            if (nextWordStartsWithColon) {
                                i++;
                            }
                        } else {
                            // Single word is too long, force it on its own line
                            wrappedLines.add(activeFormatting + testWord);
                            activeFormatting = extractActiveFormatting(activeFormatting + testWord);

                            // Skip next word if we combined it with current word
                            if (nextWordStartsWithColon) {
                                i++;
                            }
                        }
                    }
                }

                // Add remaining text
                if (currentLine.length() > 0) {
                    wrappedLines.add(currentLine.toString());
                }
            }
        }

        this.maxScrollOffset = Math.max(0, wrappedLines.size() - maxLines);

        // Adjust current scroll position if it's now out of bounds
        if (this.scrollOffset > this.maxScrollOffset) {
            this.scrollOffset = this.maxScrollOffset;
        }

        int centerX = this.width / 2;
        int bottomY = windowY + windowHeight - 35; // Middle ground: not too high, not too low

        // "I accept" checkbox - centered in the window
        this.acceptCheckbox = new CheckboxWidget(centerX - 100, bottomY - 25, 200, 20,
                Text.literal(checkboxText), false) {
            @Override
            public void onPress() {
                super.onPress();
                acceptButton.active = this.isChecked();
            }
        };
        this.addDrawableChild(acceptCheckbox);

        // "I accept" button - inside the window
        this.acceptButton = ButtonWidget.builder(Text.literal(acceptButtonText), button -> {
                    // Send packet to server to confirm acceptance
                    RulesResponsePacket.sendAcceptPacket();
                    this.close();
                })
                .dimensions(centerX - 80, bottomY + 5, 70, 20)
                .build();
        this.acceptButton.active = false; // Disabled by default
        this.addDrawableChild(acceptButton);

        // "I decline" button - inside the window
        this.declineButton = ButtonWidget.builder(Text.literal(declineButtonText), button -> {
                    // Send decline packet to server instead of disconnecting directly
                    RulesResponsePacket.sendDeclinePacket();
                })
                .dimensions(centerX + 10, bottomY + 5, 70, 20)
                .build();
        this.addDrawableChild(declineButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Minecraft dirt background (like in menus)
        this.renderBackground(context);

        // Rules window background (dark gray with border)
        int windowWidth = this.width - 80;
        int windowHeight = this.height - 80; // Smaller to leave space
        int windowX = 40;
        int windowY = 20; // Higher up

        // Window shadow
        context.fill(windowX + 4, windowY + 4, windowX + windowWidth + 4, windowY + windowHeight + 4, 0x88000000);

        // Window background
        context.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0xFF2D2D30);

        // Window border
        context.fill(windowX, windowY, windowX + windowWidth, windowY + 2, 0xFF404040); // Top
        context.fill(windowX, windowY, windowX + 2, windowY + windowHeight, 0xFF404040); // Left
        context.fill(windowX + windowWidth - 2, windowY, windowX + windowWidth, windowY + windowHeight, 0xFF202020); // Right
        context.fill(windowX, windowY + windowHeight - 2, windowX + windowWidth, windowY + windowHeight, 0xFF202020); // Bottom

        // Title with background
        int titleY = windowY + 15;
        context.fill(windowX + 10, titleY - 5, windowX + windowWidth - 10, titleY + 15, 0xFF404040);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, titleY, 0xFFFFFF);

        // Rules content area
        int contentX = windowX + 20;
        int contentY = windowY + 50;
        int contentWidth = windowWidth - 40;
        int contentHeight = windowHeight - 120; // Smaller to leave space for buttons

        // Text area background
        context.fill(contentX - 5, contentY - 5, contentX + contentWidth + 5, contentY + contentHeight + 5, 0xFF1E1E1E);

        // Rules content with scroll and word wrapping
        String[] originalLines = rulesContent.split("\n");
        int lineHeight = 12;
        int maxLines = contentHeight / lineHeight;

        // Apply word wrapping to create final display lines
        java.util.List<String> wrappedLines = new java.util.ArrayList<>();
        int maxWidth = contentWidth - 10; // Leave some margin

        for (String originalLine : originalLines) {
            if (originalLine.trim().isEmpty()) {
                wrappedLines.add(""); // Preserve empty lines
                continue;
            }

            // Check if line fits within width
            if (this.textRenderer.getWidth(originalLine) <= maxWidth) {
                wrappedLines.add(originalLine);
            } else {
                // Split long line into multiple lines with formatting preservation
                String[] words = originalLine.split(" ");
                StringBuilder currentLine = new StringBuilder();
                String activeFormatting = ""; // Track active formatting codes

                for (int i = 0; i < words.length; i++) {
                    String word = words[i];

                    // Check if next word starts with ":" to treat current word + space + ":" as non-breakable
                    boolean nextWordStartsWithColon = (i + 1 < words.length) && words[i + 1].startsWith(":");
                    String testWord = word;

                    if (nextWordStartsWithColon) {
                        // Treat word + space + colon as a single unit
                        testWord = word + " " + words[i + 1];
                    }

                    String testLine = currentLine.length() == 0 ? activeFormatting + testWord : currentLine + " " + testWord;

                    if (this.textRenderer.getWidth(testLine) <= maxWidth) {
                        currentLine = new StringBuilder(testLine);
                        // Update active formatting based on this word
                        activeFormatting = extractActiveFormatting(testLine);

                        // Skip next word if we combined it with current word
                        if (nextWordStartsWithColon) {
                            i++;
                        }
                    } else {
                        // Current line is full, start a new one
                        if (currentLine.length() > 0) {
                            wrappedLines.add(currentLine.toString());
                            currentLine = new StringBuilder(activeFormatting + testWord);
                            activeFormatting = extractActiveFormatting(currentLine.toString());

                            // Skip next word if we combined it with current word
                            if (nextWordStartsWithColon) {
                                i++;
                            }
                        } else {
                            // Single word is too long, force it on its own line
                            wrappedLines.add(activeFormatting + testWord);
                            activeFormatting = extractActiveFormatting(activeFormatting + testWord);

                            // Skip next word if we combined it with current word
                            if (nextWordStartsWithColon) {
                                i++;
                            }
                        }
                    }
                }

                // Add remaining text
                if (currentLine.length() > 0) {
                    wrappedLines.add(currentLine.toString());
                }
            }
        }

        // Recalculate maxScrollOffset based on wrapped lines
        this.maxScrollOffset = Math.max(0, wrappedLines.size() - maxLines);

        // Clip text within content area
        context.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);

        for (int i = scrollOffset; i < Math.min(wrappedLines.size(), scrollOffset + maxLines); i++) {
            int y = contentY + (i - scrollOffset) * lineHeight;
            String line = wrappedLines.get(i).trim();
            if (!line.isEmpty()) {
                context.drawTextWithShadow(this.textRenderer, line, contentX, y, 0xFFFFFF);
            }
        }

        context.disableScissor();

        // Scroll indicator if necessary
        if (maxScrollOffset > 0) {
            // Visual scroll bar
            int scrollBarX = windowX + windowWidth - 15;
            int scrollBarY = contentY;
            int scrollBarHeight = contentHeight;
            int scrollThumbHeight = Math.max(10, scrollBarHeight * maxLines / wrappedLines.size());
            int scrollThumbY = scrollBarY + (scrollBarHeight - scrollThumbHeight) * scrollOffset / maxScrollOffset;

            // Scroll bar background
            context.fill(scrollBarX, scrollBarY, scrollBarX + 8, scrollBarY + scrollBarHeight, 0xFF404040);
            // Scroll thumb
            context.fill(scrollBarX + 1, scrollThumbY, scrollBarX + 7, scrollThumbY + scrollThumbHeight, 0xFF808080);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (maxScrollOffset > 0) {
            if (amount > 0 && scrollOffset > 0) {
                scrollOffset--;
                return true;
            } else if (amount < 0 && scrollOffset < maxScrollOffset) {
                scrollOffset++;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Block all keys except interface navigation keys
        if (keyCode == 256) { // Escape - blocked
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // Block all keys
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // Block character input
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Only allow clicks on interface widgets
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Prevent closing with Escape
    }

    @Override
    public boolean shouldPause() {
        return true; // Pause game if in singleplayer world
    }

    @Override
    public void removed() {
        // Do nothing when screen is removed - prevents returning to game
        super.removed();
    }

    /**
     * Extracts active formatting codes from a text string to preserve them across line breaks
     */
    private String extractActiveFormatting(String text) {
        StringBuilder activeFormatting = new StringBuilder();
        boolean bold = false, italic = false, underlined = false, strikethrough = false, obfuscated = false;
        String color = "";

        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§') {
                char code = text.charAt(i + 1);
                switch (code) {
                    // Colors
                    case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f':
                        color = "§" + code;
                        // Color codes reset all formatting
                        bold = italic = underlined = strikethrough = obfuscated = false;
                        break;
                    // Formatting codes
                    case 'l': bold = true; break;           // Bold
                    case 'o': italic = true; break;         // Italic
                    case 'n': underlined = true; break;     // Underlined
                    case 'm': strikethrough = true; break; // Strikethrough
                    case 'k': obfuscated = true; break;     // Obfuscated
                    case 'r': // Reset - clear everything
                        color = "";
                        bold = italic = underlined = strikethrough = obfuscated = false;
                        break;
                }
                i++; // Skip the next character as it's the formatting code
            }
        }

        // Build the active formatting string
        activeFormatting.append(color);
        if (bold) activeFormatting.append("§l");
        if (italic) activeFormatting.append("§o");
        if (underlined) activeFormatting.append("§n");
        if (strikethrough) activeFormatting.append("§m");
        if (obfuscated) activeFormatting.append("§k");

        return activeFormatting.toString();
    }
}
