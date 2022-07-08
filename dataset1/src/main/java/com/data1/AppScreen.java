package com.data1;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.opencsv.exceptions.CsvException;

public class AppScreen {
    private JFrame frame = new JFrame("Data Manager App");
    private final int DIMENSION_HEIGHT = 500, DIMENSION_WIDTH = 800;
    private final String AUTH_URL = "https://account.box.com/api/oauth2/authorize?client_id=g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv&redirect_uri=https://google.com&response_type=code";
    private static JTextArea statusField = new JTextArea();
    private JButton organizeButton = new JButton("Organize Files"), processButton = new JButton("Process Data");
    private JLabel background = new JLabel(new ImageIcon("UTAustinInternship/dataset1/images/appBackground.png"));
    private Image icon = new ImageIcon("UTAustinInternship/dataset1/images/longhornsWhite.png").getImage();

    public AppScreen() {
        setButtons();
        setTextArea();
        configureFrame();
        frame.setVisible(true);
    }

    public static void updateStatus(String message, boolean newline) {
        statusField.setText(newline ? message + "\n" + statusField.getText() : message + statusField.getText());
        statusField.update(statusField.getGraphics());
    }

    private void configureFrame() {
        frame.setIconImage(icon);
        frame.setSize(DIMENSION_WIDTH, DIMENSION_HEIGHT);
        frame.setResizable(false);
        frame.add(background);
    }

    private void setButtons() {
        organizeButton.setBounds(DIMENSION_WIDTH * 3 / 16, DIMENSION_HEIGHT * 7 / 20, 200, 40);
        organizeButton.setFont(new Font("Helvetica Neue", Font.BOLD, 16));
        organizeButton.setForeground(new Color(191, 87, 0));
        organizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(AUTH_URL));
                    String authcode = getAuthcode();
                    new FileOrganizer(authcode);
                } catch (IOException | InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });
        processButton.setBounds(DIMENSION_WIDTH * 9 / 16, DIMENSION_HEIGHT * 7 / 20, 200, 40);
        processButton.setFont(new Font("Helvetica Neue", Font.BOLD, 16));
        processButton.setForeground(new Color(191, 87, 0));
        processButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String[] range = getDateRange();
                    String yearInput = range[0], monthInput = range[1], dayInput = range[2], startInput = range[3];
                    updateStatus("Enter authcode obtained from browser", true);
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(AUTH_URL));
                    String authcode = getAuthcode();
                    new FileProcess(yearInput, monthInput, dayInput, startInput, authcode);
                } catch (IOException | CsvException | InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });
        frame.add(organizeButton);
        frame.add(processButton);
    }

    private void setTextArea() {
        statusField.setBounds(DIMENSION_WIDTH / 8, DIMENSION_HEIGHT / 2, DIMENSION_WIDTH * 3 / 4, DIMENSION_HEIGHT / 3);
        statusField.setEditable(false);
        statusField.setFont(new Font("Courier New", Font.PLAIN, 14));
        statusField.setBackground(new Color(60, 50, 50));
        statusField.setForeground(Color.WHITE);
        statusField.setBorder(new EmptyBorder(new Insets(15, 15, 15, 15)));
        statusField.setText("Welcome to the data manager!");
        frame.add(statusField);
    }

    private String getAuthcode() {
        JPasswordField authField = new JPasswordField(20);
        authField.setSize(20, 1);
        JPanel authPanel = new JPanel();
        authPanel.setLayout(new BoxLayout(authPanel, BoxLayout.Y_AXIS));
        authPanel.add(new JLabel("Enter authcode: "));
        authPanel.add(authField);
        JOptionPane.showConfirmDialog(frame, authPanel, "OAuth 2.0 Authorization",
                JOptionPane.OK_CANCEL_OPTION);
        updateStatus(" ✓ ", false);
        return new String(authField.getPassword());
    }

    private String[] getDateRange() {
        String[] range = new String[4];
        String[] years = { "2019", "2020" };
        JPanel daySelection = new JPanel();
        JComboBox<String> year;
        JTextField month, days, startDate;
        daySelection.add(new JLabel("Year: "));
        daySelection.add(year = new JComboBox<String>(years));
        daySelection.add(Box.createHorizontalStrut(10));
        daySelection.add(new JLabel("Month :"));
        daySelection.add(month = new JTextField(2));
        daySelection.add(Box.createHorizontalStrut(10));
        daySelection.add(new JLabel("Days :"));
        daySelection.add(days = new JTextField(2));
        daySelection.add(Box.createHorizontalStrut(10));
        daySelection.add(new JLabel("Start date :"));
        daySelection.add(startDate = new JTextField("1", 2));
        updateStatus("Enter date range to aggregate", true);
        JOptionPane.showConfirmDialog(frame, daySelection,
                "Enter date range: ", JOptionPane.OK_CANCEL_OPTION);
        range[0] = (String) year.getSelectedItem();
        range[1] = month.getText();
        range[2] = days.getText();
        range[3] = startDate.getText();
        updateStatus(" ✓ ", false);
        return range;
    }
}
