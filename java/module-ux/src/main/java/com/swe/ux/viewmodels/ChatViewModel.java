package com.swe.ux.viewmodels;

import com.swe.chat.FileMessageSerializer;
import com.swe.chat.MessageVM;
import com.swe.controller.Meeting.UserProfile;
import com.swe.controller.RPCinterface.AbstractRPC;
import com.swe.controller.RPC;
import com.swe.ux.model.ChatMessage;

import com.swe.chat.ChatMessageSerializer;
import com.swe.ux.model.FileMessage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.Instant;

/**
 * The ViewModel for the Chat module.
 * Handles all frontend state, formatting, and communication with the Backend via RPC.
 */
public class ChatViewModel {

    // --- Dependencies & State ---
    private static ChatViewModel INSTANCE;
    private final AbstractRPC rpc;
    // TODO: In a real app, get this from your AuthService
    private final String userEmail;
    private final String userDisplayName;

//    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB

    private final Map<String, MessageVM> messageHistory = new ConcurrentHashMap<>();
    private String currentReplyId = null;
    private File attachedFile = null;

    // --- View Callbacks ---
    private Consumer<MessageVM> onMessageAdded;
    private Consumer<String> onReplyStateChange;
    private Runnable onClearInput;
    private Consumer<String> onMessageRemoved;
    private Consumer<String> onAttachmentSet;
    private Consumer<String> onShowErrorDialog;
    private Consumer<String> onShowSuccessDialog;

    // --- Constructor (Dependency Injection) ---
    public ChatViewModel(AbstractRPC RPC, UserProfile userProfile) {
        this.rpc = RPC;
        this.userEmail = userProfile.getEmail();
        this.userDisplayName = userProfile.getDisplayName();
        // Subscribe to incoming messages broadcast from the Core
        this.rpc.subscribe("chat:new-message", this::handleBackendTextMessage);
        this.rpc.subscribe("chat:file-metadata-received", this::handleBackendFileMetadata);
        this.rpc.subscribe("chat:file-saved-success", this::handleFileSaveSuccess);
        this.rpc.subscribe("chat:file-saved-error", this::handleFileSaveError);
        this.rpc.subscribe("chat:message-deleted", this::handleBackendDelete);
    }


    // Add this helper method inside ChatViewModel
    private String formatToLocalTime(LocalDateTime utcDateTime) {
        if (utcDateTime == null) return "";
        return utcDateTime.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * DEBUG: Prints the current state of message history to the console.
     */
    public void printMessageHistory() {
        System.out.println("\n====== DEBUG: MESSAGE HISTORY DUMP (" + messageHistory.size() + " items) ======");

        if (messageHistory.isEmpty()) {
            System.out.println("(History is empty)");
        } else {
            // Iterate over every message in the map
            messageHistory.values().forEach(vm -> {
                System.out.println("------------------------------------------------");
                System.out.println("ID        : " + vm.messageId);
                System.out.println("Time      : " + vm.timestamp);
                System.out.println("Sender    : " + vm.username + (vm.isSentByMe ? " (Me)" : ""));

                if (vm.isFileMessage()) {
                    System.out.println("Type      : FILE");
                    System.out.println("File Name : " + vm.fileName);
                    System.out.println("Caption   : " + vm.content);
                } else {
                    System.out.println("Type      : TEXT");
                    System.out.println("Content   : " + vm.content);
                }

                System.out.println("ReplyToID : " + vm.replyToId);
            });
        }
        System.out.println("============================================================\n");
    }

    /**
     * HANDLER 1: Text Message from Backend
     */
    private byte[] handleBackendTextMessage(byte[] data) {
        ChatMessage message = ChatMessageSerializer.deserialize(data);
        // Convert UTC LocalDateTime to User's System Default TimeZone
        String localTime = formatToLocalTime(message.getTimestamp());

        handleIncomingMessage(
                message.getMessageId(),
                message.getUserId(),
                message.getSenderDisplayName(),
                localTime,
                message.getReplyToMessageId(),
                message.getContent(),  // Text content
                null,                  // No file
                0,                     // No file size
                null                   // No file bytes
        );
        return new byte[0];
    }

    /**
     * HANDLER 2: File Metadata from Backend (KEY!)
     *
     * Backend sends ONLY metadata - NO file data
     */
    private byte[] handleBackendFileMetadata(byte[] data) {
        System.out.println("[FRONT] Received file metadata (no data attached)");

        try {
            FileMessage message = FileMessageSerializer.deserialize(data);

            if (message == null || message.getFileName() == null) {
                System.err.println("[FRONT] Invalid file metadata");
                return new byte[0];
            }

            // Extract compressed size for UI display
            long compressedSize = (message.getFileContent() != null)
                    ? message.getFileContent().length
                    : 0;

            // Convert UTC to Local Time
            String localTime = formatToLocalTime(message.getTimestamp());

            handleIncomingMessage(
                    message.getMessageId(),
                    message.getUserId(),
                    message.getSenderDisplayName(),
                    localTime,
                    message.getReplyToMessageId(),
                    message.getCaption(),    // Caption
                    message.getFileName(),   // File name
                    compressedSize,          // Size
                    null                     // NO file content!
            );
        } catch (Exception e) {
            System.err.println("[FRONT] Failed to deserialize file metadata: " + e.getMessage());
            e.printStackTrace();
        }

        return new byte[0];
    }

    /**
     * HANDLER 3: File Save Success
     */
    private byte[] handleFileSaveSuccess(byte[] data) {
        String message = new String(data, StandardCharsets.UTF_8);
        if (onShowSuccessDialog != null) {
            onShowSuccessDialog.accept("File saved successfully!\n" + message);
        }
        return new byte[0];
    }

    /**
     * HANDLER 4: File Save Error
     */
    private byte[] handleFileSaveError(byte[] data) {
        String message = new String(data, StandardCharsets.UTF_8);
        if (onShowErrorDialog != null) {
            onShowErrorDialog.accept("Failed to save file: " + message);
        }
        return new byte[0];
    }

    private byte[] handleBackendDelete(byte[] data) {
        try {
            // 1. Decode the ID
            String rawId = new String(data, StandardCharsets.UTF_8);

            // 2. SANITIZE: Important! Trim whitespace/newlines that might break the match
            String messageId = rawId.trim();

            System.out.println("[FRONT] Received DELETE signal for ID: '" + messageId + "'");

            // 3. Look up the message
            MessageVM oldMsg = messageHistory.get(messageId);

            if (oldMsg != null) {
                // 4. Create the "Deleted" replacement
                MessageVM deletedMsg = new MessageVM(
                        oldMsg.messageId,
                        oldMsg.username,
                        "<i>This message was deleted</i>", // The specific text your View looks for
                        null,           // Clear filename
                        0,              // Clear size
                        null,           // Clear file content
                        oldMsg.timestamp,
                        oldMsg.isSentByMe,
                        null,           // Remove quote
                        null            // Remove reply ID
                );

                // 5. Update History
                messageHistory.put(messageId, deletedMsg);

                // 6. Trigger the View Update
                if (onMessageAdded != null) {
                    // This forces the View to swap the bubble
                    onMessageAdded.accept(deletedMsg);
                }
                System.out.println("[FRONT] Message deleted successfully in View.");
            } else {
                System.err.println("[FRONT] DELETE FAILED: Message ID '" + messageId + "' not found in history.");
                // Optional: Print keys to see what IS in history
                // System.out.println("Available keys: " + messageHistory.keySet());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    public void send(String messageText) {
        if (this.attachedFile != null) {
            sendFileMessage(this.attachedFile, messageText);
        } else if (messageText != null && !messageText.trim().isEmpty()) {
            sendTextMessage(messageText);
        }

        if (onClearInput != null) onClearInput.run();
        cancelReply();
        cancelAttachment();
    }

    private void sendTextMessage(String messageText) {
        final String messageId = UUID.randomUUID().toString();
        final ChatMessage messageToSend = new ChatMessage(
                messageId,
                this.userEmail,
                this.userDisplayName,
                messageText,
                this.currentReplyId
        );

        byte[] messageBytes = ChatMessageSerializer.serialize(messageToSend);
        sendRpc("chat:send-text", messageBytes);

        String localTimeFormatted = formatToLocalTime(messageToSend.getTimestamp());

        // Optimistic update
        handleIncomingMessage(
                messageToSend.getMessageId(),
                messageToSend.getUserId(),
                messageToSend.getSenderDisplayName(),
                localTimeFormatted,
                messageToSend.getReplyToMessageId(),
                messageToSend.getContent(),
                null, 0, null
        );
        printMessageHistory();
    }

    /**
     * SEND FILE - Sends ONLY the path to backend
     */
    private void sendFileMessage(File file, String caption) {
        String cleanPath = file.getAbsolutePath().trim();
        if (cleanPath.startsWith("*")) {
            cleanPath = cleanPath.substring(1).trim();
        }

        if (cleanPath == null || cleanPath.isEmpty()) {
            if (onShowErrorDialog != null) {
                onShowErrorDialog.accept("File path is empty or invalid!");
            }
            return;
        }

        final String messageId = UUID.randomUUID().toString();

        // Create PATH-MODE FileMessage
        final FileMessage messageToSend = new FileMessage(
                messageId,
                this.userEmail,
                this.userDisplayName,
                caption,
                file.getName(),
                cleanPath,  //  PATH, not bytes
                this.currentReplyId
        );

        byte[] messageBytes = FileMessageSerializer.serialize(messageToSend);
        sendRpc("chat:send-file", messageBytes);

        // Optimistic UI update
        handleIncomingMessage(
                messageToSend.getMessageId(),
                messageToSend.getUserId(),
                messageToSend.getSenderDisplayName(),
                messageToSend.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm")),
                messageToSend.getReplyToMessageId(),
                caption,
                file.getName(),
                file.length(),  // Show original size
                null            // NO file content
        );
    }


    public void downloadFile(MessageVM fileMessage) {
        if (fileMessage == null || !fileMessage.isFileMessage()) {
            if (onShowErrorDialog != null) {
                onShowErrorDialog.accept("Invalid file message");
            }
            return;
        }

        System.out.println("[FRONT] User clicked 'Save'. Requesting backend to decompress and save.");

        byte[] messageIdBytes = fileMessage.messageId.getBytes(StandardCharsets.UTF_8);

        CompletableFuture.runAsync(() -> {
            try {
                this.rpc.call("chat:save-file-to-disk", messageIdBytes)
                        .thenAccept(response -> {
                            System.out.println("[FRONT] Backend finished saving file");
                        })
                        .exceptionally(e -> {
                            System.err.println("[FRONT] Failed to request save: " + e.getMessage());
                            if (onShowErrorDialog != null) {
                                onShowErrorDialog.accept("Save failed: " + e.getMessage());
                            }
                            return null;
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void handleIncomingMessage(
            String messageId, String userEmail, String senderDisplayName,
            String formattedTime, String replyToId,
            String content,              // Text or Caption
            String fileName,             // File name
            long compressedFileSize,     // Compressed size (metadata)
            byte[] fileContent           // NULL except when needed
    ) {
        if (messageHistory.containsKey(messageId)) {
            System.out.println("[FRONT] Duplicate message ignored: " + messageId);
            return;
        }

        final boolean isSentByMe = userEmail.equals(this.userEmail);
        final String username = isSentByMe ? "You" : senderDisplayName;

        String quotedContent = null;
        if (replyToId != null) {
            final MessageVM repliedTo = messageHistory.get(replyToId);
            if (repliedTo != null) {
                String sender = repliedTo.isSentByMe ? "You" : repliedTo.username;
                String contentSnippet = repliedTo.isFileMessage() ?
                        "File: " + repliedTo.fileName :
                        repliedTo.content.substring(0, Math.min(repliedTo.content.length(), 20)) + "...";
                quotedContent = "Replying to " + sender + ": " + contentSnippet;
            } else {
                quotedContent = "Reply to unavailable message";
            }
        }

        MessageVM vm = new MessageVM(
                messageId,
                username,
                content,
                fileName,
                compressedFileSize,
                fileContent,  // Usually NULL!
                formattedTime,
                isSentByMe,
                quotedContent,
                replyToId
        );

        messageHistory.put(vm.messageId, vm);
        if (onMessageAdded != null) {
            onMessageAdded.accept(vm);
        }
        printMessageHistory();
    }

    public void userSelectedFileToAttach(File selectedFile) {
        if (selectedFile == null) return;

        if (selectedFile.length() == 0) {
            if (onShowErrorDialog != null) {
                onShowErrorDialog.accept("Cannot send an empty file.");
            }
            return;
        }

//        if (selectedFile.length() > MAX_FILE_SIZE_BYTES) {
//            if (onShowErrorDialog != null) {
//                onShowErrorDialog.accept("File is too large (Max 50MB).");
//            }
//            return;
//        }

        this.attachedFile = selectedFile;
        if (onAttachmentSet != null) {
            onAttachmentSet.accept("Attached: " + selectedFile.getName());
        }
    }

    // In ChatViewModel.java

    public void deleteMessage(MessageVM messageToDelete) {
        if (messageToDelete == null || !messageToDelete.isSentByMe) return;

        // 1. Send RPC to backend (keep this)
        byte[] messageIdBytes = messageToDelete.messageId.getBytes(StandardCharsets.UTF_8);
        sendRpc("chat:delete-message", messageIdBytes);

        // 2. CREATE THE "DELETED" VERSION OF THE MESSAGE
        // We convert the file or text message into a simple text message with italics
        MessageVM deletedMsg = new MessageVM(
                messageToDelete.messageId,
                messageToDelete.username,
                "<i>This message was deleted</i>", // HTML italics for styling
                null,           // FileName is gone
                0,              // Size is 0
                null,           // Content is null
                messageToDelete.timestamp,
                messageToDelete.isSentByMe,
                null,
                null// Remove any quoted reply content
        );

        // 3. UPDATE THE HISTORY (Do not remove!)
        messageHistory.put(messageToDelete.messageId, deletedMsg);

        // 4. NOTIFY VIEW TO UPDATE (Use onMessageAdded, NOT onMessageRemoved)
        // Your View logic already handles updating if the ID exists
        if (onMessageAdded != null) {
            onMessageAdded.accept(deletedMsg);
        }

        // 5. Handle Reply cancellation logic
        if (this.currentReplyId != null && this.currentReplyId.equals(messageToDelete.messageId)) {
            cancelReply();
        }
    }





    public void cancelAttachment() {
        this.attachedFile = null;
        if (onAttachmentSet != null) onAttachmentSet.accept(null);
    }

    private void sendRpc(String endpoint, byte[] data) {
        CompletableFuture.runAsync(() -> {
            try {
                this.rpc.call(endpoint, data)
                        .thenAccept(responseBytes -> {
                            if (responseBytes != null && responseBytes.length > 0) {
                                System.out.println("[FRONT] Received response from backend");
                            }
                        })
                        .exceptionally(e -> {
                            System.err.println("[FRONT] RPC call failed: " + e.getMessage());
                            if (onShowErrorDialog != null) {
                                onShowErrorDialog.accept("RPC call failed: " + e.getMessage());
                            }
                            return null;
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    // --- User Actions ---

    public void startReply(MessageVM messageToReply) {
        this.currentReplyId = messageToReply.messageId;
        if (onReplyStateChange != null) {
            String quoteText = messageToReply.isFileMessage() ?
                    "Replying to file: " + messageToReply.fileName :
                    "Replying to " + messageToReply.username + ": " +
                            messageToReply.content.substring(0, Math.min(messageToReply.content.length(), 20)) + "...";
            onReplyStateChange.accept(quoteText);
        }
    }

    public void cancelReply() {
        this.currentReplyId = null;
        if (onReplyStateChange != null) {
            onReplyStateChange.accept(null);
        }
    }

    // --- View Bindings ---
    public void setOnMessageAdded(Consumer<MessageVM> listener) { this.onMessageAdded = listener; }
    public void setOnReplyStateChange(Consumer<String> listener) { this.onReplyStateChange = listener; }
    public void setOnClearInput(Runnable listener) { this.onClearInput = listener; }
    public void setOnMessageRemoved(Consumer<String> listener) { this.onMessageRemoved = listener; }
    public void setOnAttachmentSet(Consumer<String> listener) { this.onAttachmentSet = listener; }
    public void setOnShowErrorDialog(Consumer<String> listener) { this.onShowErrorDialog = listener; }
    public void setOnShowSuccessDialog(Consumer<String> listener) { this.onShowSuccessDialog = listener; }
}