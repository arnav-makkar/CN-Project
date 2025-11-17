// Peers SIP Softphone - GPL v3 License

package sip;

import sip.Logger;

public abstract class AbstractState {
    
    protected String id;
    protected Logger logger;
    
    public AbstractState(String id, Logger logger) {
        this.id = id;
        this.logger = logger;
    }

    public void log(AbstractState state) {
        StringBuffer buf = new StringBuffer();
        buf.append("SM ").append(id).append(" [");
        buf.append(JavaUtils.getShortClassName(this.getClass())).append(" -> ");
        buf.append(JavaUtils.getShortClassName(state.getClass())).append("] ");
        buf.append(new Exception().getStackTrace()[1].getMethodName());
        logger.debug(buf.toString());
    }
    
}
