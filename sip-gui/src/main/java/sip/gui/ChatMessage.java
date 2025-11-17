// Peers SIP Softphone - GPL v3 License

package sip.gui;

public class ChatMessage {
    private String sender;
    private String text;
    private byte[] attachment;
    private String attachmentName;
    private String contentType;
    private long timestamp;
    private boolean isSent;

    public ChatMessage(String sender, String text, byte[] attachment, 
            String attachmentName, String contentType, boolean isSent) {
        this.sender = sender;
        this.text = text;
        this.attachment = attachment;
        this.attachmentName = attachmentName;
        this.contentType = contentType;
        this.timestamp = System.currentTimeMillis();
        this.isSent = isSent;
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public byte[] getAttachment() {
        return attachment;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isSent() {
        return isSent;
    }

    public boolean hasAttachment() {
        return attachment != null && attachment.length > 0;
    }
}

