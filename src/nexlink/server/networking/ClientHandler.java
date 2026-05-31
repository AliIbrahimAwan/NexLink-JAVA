while (true) {
                MessagesDAO messagesDAO = new MessagesDAO();

                // 💡 Read from the socket ONLY ONCE at the start of the loop
                String incomingRequest = bufferedReader.readLine();

                // If the client cleanly disconnected or dropped, exit the loop
                if (incomingRequest == null) {
                    break;
                }

                // ---------------- PROTOCOL 1: User List Request ----------------
                if (incomingRequest.equals("REQ_USER_LIST")) {
                    sendLiveUserList();
                    continue;
                }

                // ---------------- PROTOCOL 2: Chat History Request ----------------
                if (incomingRequest.startsWith("REQ_HISTORY|")) {
                    String[] tokens = incomingRequest.split("\\|", 2);

                    if (tokens.length == 2) {
                        String targetReceiver = tokens[1];
                        System.out.println("Fetching history between [" + this.senderName + "] and [" + targetReceiver + "]");

                        List<String> chatHistory = messagesDAO.getChatHistory(this.senderName, targetReceiver);

                        if (chatHistory == null || chatHistory.isEmpty()) {
                            bufferedWriter.write("LOAD_HISTORY|EMPTY");
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                            System.out.println("ℹ️ No previous history found. Sent EMPTY signal.");
                        } else {
                            StringBuilder historyPacket = new StringBuilder("LOAD_HISTORY|");
                            for (int i = 0; i < chatHistory.size(); i++) {
                                historyPacket.append(chatHistory.get(i));
                                if (i < chatHistory.size() - 1) {
                                    historyPacket.append("[MSG_SEP]");
                                }
                            }
                            bufferedWriter.write(historyPacket.toString());
                            bufferedWriter.newLine();
                            bufferedWriter.flush();

                            System.out.println("✅ Sent " + chatHistory.size() + " historical messages to [" + this.senderName + "]");
                        }
                    }
                    continue; // Pass smoothly to the next loop cycle
                }

                // ---------------- PROTOCOL 3: Targeted Chat Messaging ----------------
                if (incomingRequest.startsWith("MSG|")) {
                    System.out.println("entered if 2");

                    String[] tokens = incomingRequest.split("\\|", 3);

                    if (tokens.length == 3) {
                        String targetReceiver = tokens[1];
                        String textMessage = tokens[2];

                        System.out.println(textMessage + " to " + targetReceiver);

                        ClientHandler recipientHandler = Server.activeClients.get(targetReceiver);
                        Message message = new Message(senderName, targetReceiver, textMessage);
                        messagesDAO.saveMessage(message);

                        if (recipientHandler != null) {
                            recipientHandler.receiveForwardedMessage("INCOMING_MSG|" + senderName + "|" + textMessage);
                        } else {
                            this.receiveForwardedMessage("INCOMING_MSG|System|" + targetReceiver + " is currently offline.");
                        }
                    }
                    continue;
                }

