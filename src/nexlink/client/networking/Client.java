package nexlink.client.networking;

import java.net.Socket;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Scanner;
import nexlink.client.auth.LoginScreenLogic;
import nexlink.client.auth.LoginScreen;
import nexlink.client.auth.RegisterScreenLogic;
import nexlink.client.auth.RegisterScreen;
import nexlink.client.gui.ChatWindow;

public class Client {

    public static void main(String[] args) {

        Socket socket = null;
        OutputStreamWriter outputStreamWriter = null;
        BufferedWriter bufferedWriter = null;
        BufferedReader bufferedReader = null;

        try {
            socket = new Socket("localhost", 2222);

            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
            bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            // create logic bridge
            LoginScreenLogic loginLogic = new LoginScreenLogic();

            // show login screen
            final LoginScreen loginScreenRef[] = new LoginScreen[1];
            java.awt.EventQueue.invokeLater(() -> {
                loginScreenRef[0] = new LoginScreen(loginLogic);
                loginScreenRef[0].setVisible(true);
            });

            String userName = null;
            int currentOption = 1;

            outerLoop:
            while (true) {

                if (currentOption == 1) {
                    // LOGIN LOOP
                    while (true) {
                        // wait for GUI button click
                        synchronized (loginLogic.lock) {
                            loginLogic.lock.wait();
                        }

                        // register button clicked
                        if (loginLogic.isRegisterTriggered) {
                            loginLogic.isRegisterTriggered = false;
                            currentOption = 2;

                            // tell server switching to register
                            bufferedWriter.write("SWITCH_TO_REGISTER");
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                            break; // Drops to the outerLoop to switch to option 2
                        }

                        // login attempt
                        bufferedWriter.write(loginLogic.getUsername());
                        bufferedWriter.newLine();
                        bufferedWriter.flush();

                        bufferedWriter.write(loginLogic.getPassword());
                        bufferedWriter.newLine();
                        bufferedWriter.flush();

                        String check = bufferedReader.readLine();

                        if (check.equals("11")) {
                            userName = loginLogic.getUsername();
                            System.out.println("Login successful! Welcome " + userName);
                            loginLogic.isAuthenticated = true;

                            // close login screen
                            if (loginScreenRef[0] != null) {
                                loginScreenRef[0].dispose();
                            }

                            break outerLoop;
                        } else {
                            javax.swing.JOptionPane.showMessageDialog(null, "Incorrect Credentials");
                        }
                    }
                }

                if (currentOption == 2) {
                    // REGISTER
                    RegisterScreenLogic registerLogic = new RegisterScreenLogic();

                    final RegisterScreen[] registerScreenRef = new RegisterScreen[1];
                    java.awt.EventQueue.invokeLater(() -> {
                        registerScreenRef[0] = new RegisterScreen(registerLogic);
                        registerScreenRef[0].setVisible(true);
                    });

                    while (true) {
                        // wait for GUI button click
                        synchronized (registerLogic.lock) {
                            registerLogic.lock.wait();
                        }

                        bufferedWriter.write("START_REGISTRATION");
                        bufferedWriter.newLine();
                        bufferedWriter.flush();

                        bufferedWriter.write(registerLogic.getUsername());
                        bufferedWriter.newLine();
                        bufferedWriter.flush();

                        bufferedWriter.write(registerLogic.getPassword());
                        bufferedWriter.newLine();
                        bufferedWriter.flush();

                        String response = bufferedReader.readLine();

                        if (response.equals("1")) {
                            javax.swing.JOptionPane.showMessageDialog(null, "Registration successful! Please login.");
                            registerLogic.isRegistered = true;
                            currentOption = 1;

                            // Disposes the registration window completely
                            if (registerScreenRef[0] != null) {
                                registerScreenRef[0].dispose();
                            }

                            bufferedWriter.write("1");
                            bufferedWriter.newLine();
                            bufferedWriter.flush();

                            // show login screen again
                            java.awt.EventQueue.invokeLater(() -> {
                                loginLogic.isRegisterTriggered = false;
                                if (loginScreenRef[0] != null) {
                                    loginScreenRef[0].dispose(); // Clear old login references if any
                                }
                                loginScreenRef[0] = new LoginScreen(loginLogic);
                                loginScreenRef[0].setVisible(true);
                            });

                            // FIXED: Clear execution completely out of the authentication state machine
                            // This ensures variables re-evaluate perfectly without data leakage.
                            continue outerLoop;
                        } else {
                            javax.swing.JOptionPane.showMessageDialog(null, "Username taken. Try again.");
                        }
                    }
                }
            }
            
            
            

            final String finalUser = userName;
            final BufferedWriter finalWriter = bufferedWriter;
            final BufferedReader finalReader = bufferedReader;

            ChatWindow chatWindow = new ChatWindow(finalUser,socket ,finalWriter, finalReader);

            // start listener now
ClientListener listener = new ClientListener(socket, null, chatWindow);
new Thread(listener).start();

            //Use invokeLater ONLY to safely handle the visual display update
            java.awt.EventQueue.invokeLater(() -> {

                chatWindow.setVisible(true); // 
            });

            
             while (true) {
                Thread.sleep(10000); 
            }           



        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
                if (outputStreamWriter != null) {
                    outputStreamWriter.close();
                }
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
