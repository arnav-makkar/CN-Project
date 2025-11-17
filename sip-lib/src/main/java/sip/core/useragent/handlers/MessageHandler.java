// Peers SIP Softphone - GPL v3 License

package sip.core.useragent.handlers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

import sip.Logger;
import sip.RFC3261;
import sip.core.useragent.UserAgent;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldValue;
import sip.syntaxencoding.SipHeaderParamName;
import sip.syntaxencoding.SipHeaders;
import sip.transaction.ServerTransaction;
import sip.transaction.ServerTransactionUser;
import sip.transaction.TransactionManager;
import sip.transport.SipRequest;
import sip.transport.SipResponse;
import sip.transport.TransportManager;

public class MessageHandler extends MethodHandler implements ServerTransactionUser {

    public MessageHandler(UserAgent userAgent,
            TransactionManager transactionManager,
            TransportManager transportManager, Logger logger) {
        super(userAgent, transactionManager, transportManager, logger);
    }

    public void handleMessage(SipRequest sipRequest) {
        // Send 200 OK response
        SipResponse sipResponse = buildGenericResponse(sipRequest,
                RFC3261.CODE_200_OK, RFC3261.REASON_200_OK);
        
        ServerTransaction serverTransaction =
            transactionManager.createServerTransaction(
                sipResponse, userAgent.getSipPort(), RFC3261.TRANSPORT_UDP,
                this, sipRequest);
        serverTransaction.start();
        serverTransaction.receivedRequest(sipRequest);
        serverTransaction.sendReponse(sipResponse);
        
        // Parse message body
        byte[] body = sipRequest.getBody();
        if (body != null && body.length > 0) {
            SipHeaders headers = sipRequest.getSipHeaders();
            SipHeaderFieldValue contentType = headers.get(
                    new SipHeaderFieldName(RFC3261.HDR_CONTENT_TYPE));
            String boundaryDebug = contentType != null ? contentType.getParam(new SipHeaderParamName("boundary")) : null;
            logger.debug("MESSAGE handler: body length=" + body.length + ", contentType=" + (contentType != null ? contentType.toString() : "null") + ", boundaryParam=" + boundaryDebug);
            
            String messageText = null;
            byte[] attachment = null;
            String attachmentName = null;
            String attachmentContentType = null;
            
            if (contentType != null && contentType.getValue().toLowerCase().startsWith("multipart/mixed")) {
                // Parse multipart message with explicit boundary scanning
                try {
                    String bodyStr = new String(body);
                    logger.debug("MESSAGE handler raw body:\n" + bodyStr);
                    String boundary = extractBoundary(contentType);
                    if (boundary == null) {
                        boundary = extractBoundaryFromBody(bodyStr);
                        logger.debug("MESSAGE handler: fallback boundary from body=" + boundary);
                    }
                    if (boundary != null) {
                        MultipartResult result = parseMultipartBody(bodyStr, boundary);
                        messageText = result.text;
                        attachment = result.attachment;
                        attachmentName = result.attachmentName;
                        attachmentContentType = result.attachmentContentType;
                    } else {
                        logger.error("MESSAGE handler: boundary not found in Content-Type");
                    }
                } catch (Exception e) {
                    logger.error("Error parsing multipart message", e);
                }
            } else {
                // Plain text message
                messageText = new String(body);
            }
            
            // Notify listener
            if (userAgent.getSipListener() != null) {
                userAgent.getSipListener().messageReceived(sipRequest, messageText, 
                        attachment, attachmentName, attachmentContentType);
            }
        } else {
            logger.debug("MESSAGE handler: body empty");
        }
    }
    
    private String extractBoundary(SipHeaderFieldValue contentType) {
        if (contentType == null) {
            return null;
        }
        String boundary = contentType.getParam(new SipHeaderParamName("boundary"));
        if (boundary != null && !boundary.isEmpty()) {
            return trimQuotes(boundary);
        }
        String raw = contentType.toString();
        if (raw != null) {
            int idx = raw.toLowerCase(Locale.ROOT).indexOf("boundary=");
            if (idx >= 0) {
                String value = raw.substring(idx + 9).trim();
                int semicolon = value.indexOf(';');
                if (semicolon >= 0) {
                    value = value.substring(0, semicolon).trim();
                }
                return trimQuotes(value);
            }
        }
        return null;
    }
    
    private String trimQuotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.length() > 1) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("\"") && trimmed.length() > 1) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
    
    private String extractBoundaryFromBody(String body) {
        if (body == null) {
            return null;
        }
        String[] lines = body.split("\r\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") && trimmed.length() > 2) {
                String candidate = trimmed.substring(2);
                if (candidate.endsWith("--")) {
                    candidate = candidate.substring(0, candidate.length() - 2);
                }
                if (!candidate.isEmpty()) {
                    return candidate;
                }
            }
        }
        return null;
    }
    
    private String extractFilename(String part) {
        int filenameIndex = part.indexOf("filename=\"");
        if (filenameIndex != -1) {
            int endIndex = part.indexOf("\"", filenameIndex + 10);
            if (endIndex != -1) {
                return part.substring(filenameIndex + 10, endIndex);
            }
        }
        return "attachment";
    }
    
    private String extractContentType(String part) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(part.getBytes())));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Content-Type:")) {
                    String ct = line.substring(13).trim();
                    // Remove any parameters
                    int semicolon = ct.indexOf(';');
                    if (semicolon != -1) {
                        ct = ct.substring(0, semicolon).trim();
                    }
                    return ct;
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting content type", e);
        }
        return "application/octet-stream";
    }

    @Override
    public void transactionFailure() {
        // Handle transaction failure if needed
    }

    private MultipartResult parseMultipartBody(String bodyStr, String boundary) {
        MultipartResult result = new MultipartResult();
        String boundaryMarker = "--" + boundary;
        String[] sections = bodyStr.split(Pattern.quote(boundaryMarker));
        for (String rawSection : sections) {
            String part = rawSection.trim();
            if (part.isEmpty() || "--".equals(part)) {
                continue;
            }
            if (part.startsWith("--")) {
                part = part.substring(2).trim();
                if (part.isEmpty()) {
                    continue;
                }
            }
            int headerEnd = part.indexOf("\r\n\r\n");
            if (headerEnd == -1) {
                continue;
            }
            String headersSection = part.substring(0, headerEnd);
            String partBody = part.substring(headerEnd + 4).trim();
            String lowerHeaders = headersSection.toLowerCase(Locale.ROOT);
            if (lowerHeaders.contains("content-transfer-encoding: base64")) {
                logger.debug("MESSAGE handler: attachment part detected");
                result.attachmentName = extractFilename(headersSection);
                result.attachmentContentType = extractContentType(headersSection);
                result.attachment = Base64.getDecoder().decode(partBody);
                logger.debug("MESSAGE handler: decoded attachment bytes=" + (result.attachment != null ? result.attachment.length : 0));
            } else if (lowerHeaders.contains("content-type: text/plain")) {
                logger.debug("MESSAGE handler: text part detected");
                result.text = partBody;
            }
        }
        return result;
    }

    private static class MultipartResult {
        private String text;
        private byte[] attachment;
        private String attachmentName;
        private String attachmentContentType;
    }
}

