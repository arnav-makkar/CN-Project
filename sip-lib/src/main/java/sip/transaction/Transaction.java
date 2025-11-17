// Peers SIP Softphone - GPL v3 License

package sip.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;

import sip.Logger;
import sip.transport.SipRequest;
import sip.transport.SipResponse;
import sip.transport.TransportManager;


public abstract class Transaction {

    public static final char ID_SEPARATOR = '|';
    
    protected String branchId;
    protected String method;
    
    protected SipRequest request;
    protected List<SipResponse> responses;
    
    protected Timer timer;
    protected TransportManager transportManager;
    protected TransactionManager transactionManager;

    protected Logger logger;

    protected Transaction(String branchId, String method, Timer timer,
            TransportManager transportManager,
            TransactionManager transactionManager, Logger logger) {
        this.branchId = branchId;
        this.method = method;
        this.timer = timer;
        this.transportManager = transportManager;
        this.transactionManager = transactionManager;
        this.logger = logger;
        responses = Collections.synchronizedList(new ArrayList<SipResponse>());
    }

    protected String getId() {
        StringBuffer buf = new StringBuffer();
        buf.append(branchId).append(ID_SEPARATOR);
        buf.append(method);
        return buf.toString();
    }

    public SipResponse getLastResponse() {
        if (responses.isEmpty()) {
            return null;
        }
        return responses.get(responses.size() - 1);
    }

    public SipRequest getRequest() {
        return request;
    }
    
}
