package com.data1;

import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.opencsv.exceptions.CsvException;

public class AppScreen {
    private JFrame frame = new JFrame("Data Manager App");
    private JButton organizeButton = new JButton("Organize Files"), processButton = new JButton("Process files");
    private JTextField tf = new JTextField();
    private final int DIMENSION_HEIGHT = 500, DIMENSION_WIDTH = 800;
    private Image icon = new ImageIcon("UTAustinInternship/dataset1/images/longhornsWhite.png").getImage();
    private JLabel background = new JLabel(new ImageIcon("UTAustinInternship/dataset1/images/appBackground.png"));

    public AppScreen() {
        setButtons();
        setTextField();
        configureFrame();
        frame.setVisible(true);
    }

    public void updateStatus(String message){
        tf.setText(message+"\n"+tf.getText());
    }

    private void configureFrame() {
        frame.setIconImage(icon);
        frame.setSize(DIMENSION_WIDTH, DIMENSION_HEIGHT);
        frame.setResizable(false);
        // background.setBounds(0, 0, 800, 500);
        frame.add(background);
    }

    private void setButtons() {
        organizeButton.setBounds(DIMENSION_WIDTH / 6, DIMENSION_HEIGHT / 3, 200, 40);
        processButton.setBounds(DIMENSION_WIDTH * 1 / 2, DIMENSION_HEIGHT / 3, 200, 40);
        organizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(
                            "https://account.box.com/api/oauth2/authorize?client_id=g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv&redirect_uri=https://google.com&response_type=code"));
                    String authcode = getAuthcode();
                    new FileOrganizer(authcode);
                } catch (IOException | InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });
        processButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(
                            "https://account.box.com/api/oauth2/authorize?client_id=g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv&redirect_uri=https://google.com&response_type=code"));
                    JPanel daySelection = new JPanel();
                    JTextField year, month, days;
                    daySelection.add(new JLabel("Year: "));
                    daySelection.add(year = new JTextField(5));
                    daySelection.add(Box.createHorizontalStrut(10));
                    daySelection.add(new JLabel("Month :"));
                    daySelection.add(month = new JTextField(5));
                    daySelection.add(Box.createHorizontalStrut(10));
                    daySelection.add(new JLabel("Days :"));
                    daySelection.add(days = new JTextField(5));
                    JOptionPane.showConfirmDialog(frame, daySelection,
                            "Enter date range: ", JOptionPane.OK_CANCEL_OPTION);
                    String yearInput = year.getText(), monthInput = month.getText(),
                            dayInput = days.getText();
                    String authcode = getAuthcode();
                    new FileProcess(yearInput, monthInput, dayInput, authcode);
                } catch (IOException | CsvException | InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });
        frame.add(organizeButton);
        frame.add(processButton);
    }

    private void setTextField() {
        tf.setBounds(DIMENSION_WIDTH / 8, DIMENSION_HEIGHT / 2, DIMENSION_WIDTH * 3 / 4, DIMENSION_HEIGHT / 3);
        tf.setEditable(false);
        tf.setAutoscrolls(true);
        tf.setBackground(new Color(255, 255, 255, 200));
        frame.add(tf);
    }

    private String getAuthcode() {
        JPasswordField authField = new JPasswordField(5);
        JPanel authPanel = new JPanel();
        authPanel.add(new JLabel("Enter authcode: "));
        authPanel.add(authField);
        JOptionPane.showConfirmDialog(frame, authPanel, "OAuth2.0 Authorization",
                JOptionPane.OK_CANCEL_OPTION);
        return new String(authField.getPassword());
    }
}
