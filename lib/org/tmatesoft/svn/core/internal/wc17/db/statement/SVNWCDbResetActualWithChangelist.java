/*
 * ====================================================================
 * Copyright (c) 2004-2011 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;

/**
 * REPLACE INTO actual_node ( wc_id, local_relpath, parent_relpath, changelist)
 * VALUES (?1, ?2, ?3, ?4);
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbResetActualWithChangelist extends SVNSqlJetInsertStatement {

    public SVNWCDbResetActualWithChangelist(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE, SqlJetConflictAction.REPLACE);
    }

    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.wc_id.toString(), getBind(1));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath.toString(), getBind(2));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.parent_relpath.toString(), getBind(3));
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.changelist.toString(), getBind(4));
        return values;
    }

}
