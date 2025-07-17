package com.dremixam.communityleaders.client.gui;

import com.dremixam.communityleaders.network.CharterResponsePacket;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CharterScreen extends Screen {
    private final String charterContent;
    private final String acceptButtonText;
    private final String declineButtonText;
    private final String checkboxText;
    private final String declineMessage;

    private CheckboxWidget acceptCheckbox;
    private ButtonWidget acceptButton;
    private ButtonWidget declineButton;

    private int scrollOffset = 0;
    private final int maxScrollOffset;

    public CharterScreen(String title, String content, String acceptBtn, String declineBtn, String checkboxTxt, String declineMsg) {
        super(Text.literal(title));
        this.charterContent = content;
        this.acceptButtonText = acceptBtn;
        this.declineButtonText = declineBtn;
        this.checkboxText = checkboxTxt;
        this.declineMessage = declineMsg;

        // Calculer le scroll maximum basé sur le nombre de lignes
        String[] lines = content.split("\n");
        this.maxScrollOffset = Math.max(0, lines.length - 15); // Affiche environ 15 lignes
    }

    @Override
    protected void init() {
        // Calculer les dimensions de la fenêtre
        int windowWidth = this.width - 80;
        int windowHeight = this.height - 80;
        int windowX = 40;
        int windowY = 20;

        int centerX = this.width / 2;
        int bottomY = windowY + windowHeight - 35; // Juste milieu : ni trop haut, ni trop bas

        // Checkbox "J'accepte" - centrée dans la fenêtre
        this.acceptCheckbox = new CheckboxWidget(centerX - 100, bottomY - 20, 200, 20,
                Text.literal(checkboxText), false) {
            @Override
            public void onPress() {
                super.onPress();
                acceptButton.active = this.isChecked();
            }
        };
        this.addDrawableChild(acceptCheckbox);

        // Bouton "J'accepte" - dans la fenêtre
        this.acceptButton = ButtonWidget.builder(Text.literal(acceptButtonText), button -> {
            // Envoyer un paquet au serveur pour confirmer l'acceptation
            CharterResponsePacket.sendAcceptPacket();
            this.close();
        })
        .dimensions(centerX - 80, bottomY, 70, 20)
        .build();
        this.acceptButton.active = false; // Désactivé par défaut
        this.addDrawableChild(acceptButton);

        // Bouton "Je refuse" - dans la fenêtre
        this.declineButton = ButtonWidget.builder(Text.literal(declineButtonText), button -> {
            // Envoyer un paquet de refus au serveur au lieu de déconnecter directement
            CharterResponsePacket.sendDeclinePacket();
        })
        .dimensions(centerX + 10, bottomY, 70, 20)
        .build();
        this.addDrawableChild(declineButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Fond de terre de Minecraft (comme dans les menus)
        this.renderBackground(context);

        // Fond de la fenêtre de charte (gris foncé avec bordure)
        int windowWidth = this.width - 80;
        int windowHeight = this.height - 80; // Plus petit pour laisser de la place
        int windowX = 40;
        int windowY = 20; // Plus haut

        // Ombre de la fenêtre
        context.fill(windowX + 4, windowY + 4, windowX + windowWidth + 4, windowY + windowHeight + 4, 0x88000000);

        // Fond de la fenêtre
        context.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0xFF2D2D30);

        // Bordure de la fenêtre
        context.fill(windowX, windowY, windowX + windowWidth, windowY + 2, 0xFF404040); // Top
        context.fill(windowX, windowY, windowX + 2, windowY + windowHeight, 0xFF404040); // Left
        context.fill(windowX + windowWidth - 2, windowY, windowX + windowWidth, windowY + windowHeight, 0xFF202020); // Right
        context.fill(windowX, windowY + windowHeight - 2, windowX + windowWidth, windowY + windowHeight, 0xFF202020); // Bottom

        // Titre avec fond
        int titleY = windowY + 15;
        context.fill(windowX + 10, titleY - 5, windowX + windowWidth - 10, titleY + 15, 0xFF404040);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, titleY, 0xFFFFFF);

        // Zone de contenu de la charte
        int contentX = windowX + 20;
        int contentY = windowY + 50;
        int contentWidth = windowWidth - 40;
        int contentHeight = windowHeight - 120; // Plus petit pour laisser de la place aux boutons

        // Fond de la zone de texte
        context.fill(contentX - 5, contentY - 5, contentX + contentWidth + 5, contentY + contentHeight + 5, 0xFF1E1E1E);

        // Contenu de la charte avec scroll
        String[] lines = charterContent.split("\n");
        int lineHeight = 12;
        int maxLines = contentHeight / lineHeight;

        // Clipper le texte dans la zone de contenu
        context.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);

        for (int i = scrollOffset; i < Math.min(lines.length, scrollOffset + maxLines); i++) {
            int y = contentY + (i - scrollOffset) * lineHeight;
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                context.drawTextWithShadow(this.textRenderer, line, contentX, y, 0xFFFFFF);
            }
        }

        context.disableScissor();

        // Indicateur de scroll si nécessaire
        if (maxScrollOffset > 0) {
            // Barre de scroll visuelle
            int scrollBarX = windowX + windowWidth - 15;
            int scrollBarY = contentY;
            int scrollBarHeight = contentHeight;
            int scrollThumbHeight = Math.max(10, scrollBarHeight * maxLines / lines.length);
            int scrollThumbY = scrollBarY + (scrollBarHeight - scrollThumbHeight) * scrollOffset / maxScrollOffset;

            // Fond de la barre de scroll
            context.fill(scrollBarX, scrollBarY, scrollBarX + 8, scrollBarY + scrollBarHeight, 0xFF404040);
            // Thumb de scroll
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
        // Bloquer toutes les touches sauf les touches de navigation de l'interface
        if (keyCode == 256) { // Échap - bloqué
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // Bloquer toutes les touches
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // Bloquer la saisie de caractères
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Autoriser seulement les clics sur les widgets de l'interface
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Empêcher la fermeture avec Échap
    }

    @Override
    public boolean shouldPause() {
        return true; // Mettre le jeu en pause si c'est un monde solo
    }

    @Override
    public void removed() {
        // Ne rien faire quand l'écran est retiré - empêche le retour au jeu
        super.removed();
    }
}
