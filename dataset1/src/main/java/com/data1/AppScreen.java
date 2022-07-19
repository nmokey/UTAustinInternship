package com.data1;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;

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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.box.sdk.BoxAPIConnection;
import com.opencsv.exceptions.CsvException;

public class AppScreen {
    private JFrame frame = new JFrame("Dataset Manager");
    private final int DIMENSION_HEIGHT = 500, DIMENSION_WIDTH = 800;
    private final String AUTH_URL = "https://account.box.com/api/oauth2/authorize?client_id=g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv&redirect_uri=https://google.com&response_type=code";
    private static JTextArea statusField = new JTextArea();
    private JButton organizeButton = new JButton("Organize Files"), processButton = new JButton("Process Data"),
            aggregateButton = new JButton("Aggregate Data");
    private JLabel background = new JLabel(new ImageIcon(ClassLoader.getSystemResource("appBackground.png")));
    private ImageIcon img = new ImageIcon(ClassLoader.getSystemResource("longhornsWhite.png"));
    private JScrollPane scroller;
    private String[] dateRange = new String[4];
    private String authcode;
    private boolean hasAPI = false;
    private BoxAPIConnection api;

    public AppScreen() throws IOException {
        setButtons();
        setTextArea();
        configureFrame();
        frame.setVisible(true);
    }

    public static void updateStatus(String message) {
        statusField.setText(message + "\n" + statusField.getText());
        statusField.update(statusField.getGraphics());
        statusField.setCaretPosition(0);
    }

    public static void completeTask() {
        statusField.setText(" ✓ " + statusField.getText());
        statusField.update(statusField.getGraphics());
        statusField.setCaretPosition(0);
    }

    public static void failTask() {
        statusField.setText(" ✗ " + statusField.getText());
        statusField.update(statusField.getGraphics());
        statusField.setCaretPosition(0);
    }

    private void configureFrame() throws IOException {
        frame.setIconImage(img.getImage());
        frame.setSize(DIMENSION_WIDTH, DIMENSION_HEIGHT);
        frame.setResizable(false);
        frame.add(background);
    }

    private void setButtons() {
        organizeButton.setBounds(DIMENSION_WIDTH / 16, DIMENSION_HEIGHT * 7 / 20, 200, 40);
        organizeButton.setFont(new Font("Helvetica Neue", Font.BOLD, 16));
        organizeButton.setForeground(new Color(191, 87, 0));
        organizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    updateStatus("Enter authcode obtained from browser");
                    if (getAPI()) {
                        new FileOrganizer(api);
                    } else {
                        failTask();
                    }
                } catch (IOException | InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });
        processButton.setBounds(DIMENSION_WIDTH * 3 / 8, DIMENSION_HEIGHT * 7 / 20, 200, 40);
        processButton.setFont(new Font("Helvetica Neue", Font.BOLD, 16));
        processButton.setForeground(new Color(191, 87, 0));
        processButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (getProcessDateRange()) {
                        String yearInput = dateRange[0], monthInput = dateRange[1], dayInput = dateRange[2],
                                startInput = dateRange[3];
                        updateStatus("Enter authcode obtained from browser");
                        if (getAPI()) {
                            new FileProcessor(yearInput, monthInput, dayInput, startInput, api);
                        } else {
                            failTask();
                        }
                    } else {
                        failTask();
                    }
                } catch (IOException | CsvException | InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });
        aggregateButton.setBounds(DIMENSION_WIDTH * 11 / 16, DIMENSION_HEIGHT * 7 / 20, 200, 40);
        aggregateButton.setFont(new Font("Helvetica Neue", Font.BOLD, 16));
        aggregateButton.setForeground(new Color(191, 87, 0));
        aggregateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (getAggregateDateRange()) {
                        String yearInput = dateRange[0], monthInput = dateRange[1];
                        updateStatus("Enter authcode obtained from browser");
                        if (getAPI()) {
                            new FileAggregator(yearInput, monthInput, api);
                        } else {
                            failTask();
                        }
                    } else {
                        failTask();
                    }
                } catch (IOException | CsvException | InterruptedException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });
        frame.add(organizeButton);
        frame.add(processButton);
        frame.add(aggregateButton);
    }

    private void setTextArea() {
        scroller = new JScrollPane(statusField, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBounds(DIMENSION_WIDTH / 8, DIMENSION_HEIGHT / 2, DIMENSION_WIDTH * 3 / 4, DIMENSION_HEIGHT / 3);
        scroller.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroller.setBorder(null);
        statusField.setEditable(false);
        statusField.setFont(new Font("Courier New", Font.PLAIN, 14));
        statusField.setBackground(new Color(60, 50, 50));
        statusField.setForeground(Color.WHITE);
        statusField.setBorder(new EmptyBorder(new Insets(15, 15, 15, 15)));
        statusField.setText("Welcome to the data manager!");
        frame.add(scroller);
    }

    private boolean getAPI() throws IOException {
        if (this.hasAPI) {
            return true;
        }
        java.awt.Desktop.getDesktop().browse(java.net.URI.create(AUTH_URL));
        JPasswordField authField = new JPasswordField(20);
        authField.setSize(20, 1);
        authField.setMaximumSize(new Dimension(authField.getMaximumSize().width, authField.getMinimumSize().height));
        JPanel authPanel = new JPanel();
        authPanel.setLayout(new BoxLayout(authPanel, BoxLayout.Y_AXIS));
        authPanel.add(new JLabel("Enter authcode: "));
        authPanel.add(authField);
        if (JOptionPane.showConfirmDialog(frame, authPanel, "OAuth 2.0 Authorization",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            completeTask();
            authcode = new String(authField.getPassword());
            AppScreen.updateStatus("Establishing API connection");
            api = new BoxAPIConnection(
                    "g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv",
                    "nhg2Qi0VeZX767uhWySRt7KywKu0uKgm",
                    authcode);
            AppScreen.completeTask();
            // api = new BoxAPIConnection("DEVTOKEN"); // for testing
            hasAPI = true;
            return true;
        }
        return false;
    }

    private boolean getProcessDateRange() {
        String[] years = { "2019", "2020" };
        String[] months = { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" };
        JPanel daySelection = new JPanel();
        JComboBox<String> year, month;
        JTextField days, startDate;
        daySelection.add(new JLabel("Start date:"));
        daySelection.add(month = new JComboBox<String>(months));
        daySelection.add(new JLabel("/"));
        daySelection.add(startDate = new JTextField("1", 2));
        daySelection.add(new JLabel("/"));
        daySelection.add(year = new JComboBox<String>(years));
        daySelection.add(Box.createHorizontalStrut(20));
        daySelection.add(new JLabel("Number of days :"));
        daySelection.add(days = new JTextField(1));
        updateStatus("Enter date range to process");
        if (JOptionPane.showConfirmDialog(frame, daySelection,
                "Date Range Input: ", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            dateRange[0] = (String) year.getSelectedItem();
            dateRange[1] = (String) month.getSelectedItem();
            dateRange[2] = days.getText();
            dateRange[3] = startDate.getText();
            completeTask();
            return true;
        }
        return false;
    }

    private boolean getAggregateDateRange() {
        String[] years = { "2019", "2020" };
        String[] months = { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" };
        JPanel daySelection = new JPanel();
        JComboBox<String> year, month;
        daySelection.add(new JLabel("Year: "));
        daySelection.add(year = new JComboBox<String>(years));
        daySelection.add(Box.createHorizontalStrut(10));
        daySelection.add(new JLabel("Month: "));
        daySelection.add(month = new JComboBox<String>(months));
        updateStatus("Enter month to aggregate");
        if (JOptionPane.showConfirmDialog(frame, daySelection,
                "Month Input: ", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            dateRange[0] = (String) year.getSelectedItem();
            dateRange[1] = (String) month.getSelectedItem();
            completeTask();
            return true;
        }
        return false;
    }
}
