package com.intelliflow;

import com.intelliflow.ui.MainFrame;

import javax.swing.*;
import java.awt.*;

public class App {
    public static void main(String[] args) {
        // Run Swing GUI on the Event Dispatch Thread (EDT)
        EventQueue.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                System.err.println("Critical error launching IntelliFlow application: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
