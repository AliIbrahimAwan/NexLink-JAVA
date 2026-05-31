package nexlink.client.networking;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import nexlink.client.gui.ChatWindow;

/**
 * Optimized ClientListener to prevent text-buffer stream corruption.
 * @author ALI
 */
public class ClientListener implements Runnable {

    private ChatWindow chatWindow;
    private Socket socket;
    private InputStream socketIn; // Read directly from the raw stream

    // 🛠️ Simplified constructor - We drop BufferedReader to protect file data!
    public ClientListener(Socket socket, Object dummy, ChatWindow window) {
        this.socket = socket;
        this.chatWindow = window;
        try {
            this.socketIn = socket.getInputStream();
        } catch (IOException e) {
            System.out.println("❌ Failed to get input stream: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Read text commands cleanly without consuming file bytes
                String response = readLineCustom(socketIn);
                if (response == null) {
                    break; // Stream ended gracefully
                }

                // ---------------- PROTOCOL 1: Live User List Sync ----------------
                if (response.startsWith("USER_LIST|")) {
                    String data = response.substring(10);
                    String[] displayStrings = data.split(",");

                    java.awt.EventQueue.invokeLater(() -> {
                        javax.swing.DefaultListModel<String> model = new javax.swing.DefaultListModel<>();
                        for (String item : displayStrings) {
                            if (!item.trim().isEmpty()) {
                                model.addElement(item.trim());
                            }
                        }
                        chatWindow.getUserList().setModel(model);
                    });
                    continue;
                }

                // ---------------- PROTOCOL 2: Load Chat History ----------------
                if (response.startsWith("LOAD_HISTORY|")) {
                    String[] parts = response.split("\\|", 2);
                    if (parts.length == 2) {
                        String historyData = parts[1].trim();

                        java.awt.EventQueue.invokeLater(() -> {
                            javax.swing.JTextPane txtArea = chatWindow.getMessagesTextArea();
                            if (txtArea != null) {
                                if (historyData.equals("EMPTY")) {
                                    txtArea.setText("");
                                } else {
                                    String[] individualMessages = historyData.split(java.util.regex.Pattern.quote("[MSG_SEP]"));
                                    StringBuilder sb = new StringBuilder();
                                    for (String msg : individualMessages) {
                                        sb.append(msg).append("\n");
                                    }
                                    txtArea.setText(sb.toString().trim());
                                    txtArea.setCaretPosition(txtArea.getDocument().getLength());
                                }
                            }
                        });
                    }
                    continue;
                }

                // ---------------- PROTOCOL 3: Live Incoming Messages ----------------
                if (response.startsWith("INCOMING_MSG|")) {
                    String[] tokens = response.split("\\|", 3);
                    if (tokens.length == 3) {
                        String sender = tokens[1];
                        String text = tokens[2];

                        java.awt.EventQueue.invokeLater(() -> {
                            javax.swing.JTextPane txtArea = chatWindow.getMessagesTextArea();
                            
                            
                            
                            
                            if (txtArea != null) {
                                String currentText = txtArea.getText().trim();
                                if (currentText.isEmpty()) {
                                    txtArea.setText(sender + ": " + text);
                                } else {
                                    txtArea.setText(currentText + "\n" + sender + ": " + text);
                                }
                                txtArea.setCaretPosition(txtArea.getDocument().getLength());
                            }
                        });
                    }
                    continue;
                }

                // ---------------- 📂 PROTOCOL 4: Live Incoming File Streams ----------------
                if (response.startsWith("FILE_INCOMING|")) {
                    String[] tokens = response.split("\\|", 4);
                    if (tokens.length == 4) {
                        String senderName = tokens[1];
                        String fileName = tokens[2];
                        long fileSize = Long.parseLong(tokens[3]);

                        downloadIncomingFile(senderName, fileName, fileSize);
                    }
                    continue;
                }
            }
        } catch (IOException e) {
            System.out.println("Connection closed.");
        }
    }

    /**
     * Reads a single text line by byte boundaries to keep binary streams perfectly untouched.
     */
    private String readLineCustom(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') {
                break;
            }
            if (c != '\r') {
                sb.append((char) c);
            }
        }
        if (sb.length() == 0 && c == -1) {
            return null;
        }
        return sb.toString();
    }

    private void downloadIncomingFile(String senderName, String fileName, long fileSize) {
        java.io.FileOutputStream fileOut = null;
        try {
            java.io.File downloadDir = new java.io.File("Downloads");
            if (!downloadDir.exists()) {
                downloadDir.mkdir();
            }

            java.io.File targetFile = new java.io.File(downloadDir, fileName);
            fileOut = new java.io.FileOutputStream(targetFile);

            byte[] buffer = new byte[4096];
            long bytesRemaining = fileSize;
            int bytesRead;


            // Now that bytes aren't being stolen by BufferedReader, this loop runs flawlessly!
            while (bytesRemaining > 0 && 
                  (bytesRead = socketIn.read(buffer, 0, (int) Math.min(buffer.length, bytesRemaining))) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                bytesRemaining -= bytesRead;
            }
            
            fileOut.flush();
            fileOut.close(); // Save completely to disk
            

            
            String updateMessage = "📎 [System]: Received '" + fileName + "' from " + senderName + ". Placed inside Downloads folder.";
chatWindow.appendSystemNotification(updateMessage);


        } catch (IOException e) {
            System.out.println("❌ File download failed: " + e.getMessage());
        } finally {
            if (fileOut != null) {
                try { fileOut.close(); } catch (IOException ex) {}
            }
        }
    }

    private String getFileChecksum(java.io.File file) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;
            
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();
            
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Checksum Error: " + e.getMessage();
        }
    }

}