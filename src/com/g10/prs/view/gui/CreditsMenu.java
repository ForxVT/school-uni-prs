package com.g10.prs.view.gui;

import javax.swing.*;

/** Menu that show the credits */
public class CreditsMenu extends GuiMenu {

    /** class constructor */
    public CreditsMenu() {
        super("Crédits");
    }

    /** show the content */
    @Override
    protected void drawContent() {
        JPanel contentPanel = new JPanel();
        contentPanel.add(new Label(("Programmeurs").toUpperCase(), 18, 0, 0, 0, 25).asJLabel());
        contentPanel.add(new Label("<html>Hugo KINDEL<br>Maxime JAUROYON</html>").asJLabel());
        panel.add(contentPanel);

        JPanel contentPanel2 = new JPanel();
        contentPanel2.add(new Label("Merci d'avoir joué à Pet Rescue Saga !", 18, 25, 0, 0, 0).asJLabel());
        panel.add(contentPanel2);
    }
}
