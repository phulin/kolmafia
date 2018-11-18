package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.ISVNAdminEventHandler;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;
import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositorySynchronize;


public class SvnRepositorySynchronizeImpl extends SvnRepositoryOperationRunner<SVNAdminEvent, SvnRepositorySynchronize> implements ISVNAdminEventHandler {

    @Override
    protected SVNAdminEvent run() throws SVNException {
        SVNAdminClient ac = new SVNAdminClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        ac.setEventHandler(this);
                
        ac.doSynchronize(getOperation().getToURL());
        
        return getOperation().first();
    }

    public void handleAdminEvent(SVNAdminEvent event, double progress) throws SVNException {
        getOperation().receive(SvnTarget.fromURL(getOperation().getToURL()), event);
    }
}
