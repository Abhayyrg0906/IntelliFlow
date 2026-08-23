package com.intelliflow.ui.views;

import javax.swing.*;

public abstract class BaseView extends JPanel {
    /**
     * Called whenever the view becomes active in the CardLayout.
     * Use this to reload data from the services and repaint components.
     */
    public abstract void refresh();
}
