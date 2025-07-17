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

        // Recalculate scroll limits when window is resized
        int contentHeight = windowHeight - 120; // Same calculation as in render()
        int lineHeight = 12;
        int maxLines = contentHeight / lineHeight;
        String[] lines = rulesContent.split("\n");
        this.maxScrollOffset = Math.max(0, lines.length - maxLines);

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

        // Rules content with scroll
        String[] lines = rulesContent.split("\n");
        int lineHeight = 12;
        int maxLines = contentHeight / lineHeight;

        // Recalculate maxScrollOffset based on actual displayable lines
        this.maxScrollOffset = Math.max(0, lines.length - maxLines);

        // Clip text within content area
        context.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);

        for (int i = scrollOffset; i < Math.min(lines.length, scrollOffset + maxLines); i++) {
            int y = contentY + (i - scrollOffset) * lineHeight;
            String line = lines[i].trim();
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
            int scrollThumbHeight = Math.max(10, scrollBarHeight * maxLines / lines.length);
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
}
