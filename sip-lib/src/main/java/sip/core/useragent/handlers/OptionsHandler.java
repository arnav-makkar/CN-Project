// Peers SIP Softphone - GPL v3 License

package sip.core.useragent.handlers;

import java.io.IOException;
import java.util.Random;

import sip.Logger;
import sip.sdp.SessionDescription;
import sip.RFC3261;
import sip.Utils;
import sip.core.useragent.UserAgent;
import sip.syntaxencoding.SipHeaderFieldName;
import sip.syntaxencoding.SipHeaderFieldValue;
import sip.syntaxencoding.SipHeaders;
import sip.transaction.ServerTransaction;
import sip.transaction.ServerTransactionUser;
import sip.transaction.TransactionManager;
import sip.transport.SipRequest;
import sip.transport.SipResponse;
import sip.transport.TransportManager;

public class OptionsHandler extends MethodHandler
        implements ServerTransactionUser {

    public static final int MAX_PORTS = 65536;

    public OptionsHandler(UserAgent userAgent,
            TransactionManager transactionManager,
            TransportManager transportManager, Logger logger) {
        super(userAgent, transactionManager, transportManager, logger);
    }

    public void handleOptions(SipRequest sipRequest) {
        SipResponse sipResponse = buildGenericResponse(sipRequest,
                RFC3261.CODE_200_OK, RFC3261.REASON_200_OK);
        int localPort = new Random().nextInt(MAX_PORTS);
        try {
            SessionDescription sessionDescription =
                sdpManager.createSessionDescription(null, localPort);
            sipResponse.setBody(sessionDescription.toString().getBytes());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        SipHeaders sipHeaders = sipResponse.getSipHeaders();
        sipHeaders.add(new SipHeaderFieldName(RFC3261.HDR_CONTENT_TYPE),
                new SipHeaderFieldValue(RFC3261.CONTENT_TYPE_SDP));
        sipHeaders.add(new SipHeaderFieldName(RFC3261.HDR_ALLOW),
                new SipHeaderFieldValue(Utils.generateAllowHeader()));
        ServerTransaction serverTransaction =
            transactionManager.createServerTransaction(
                sipResponse, userAgent.getSipPort(), RFC3261.TRANSPORT_UDP,
                this, sipRequest);
        serverTransaction.start();
        serverTransaction.receivedRequest(sipRequest);
        serverTransaction.sendReponse(sipResponse);
    }

    @Override
    public void transactionFailure() {
        // TODO Auto-generated method stub
        
    }

}
