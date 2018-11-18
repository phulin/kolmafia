/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/**
 * SELECT op_depth, repos_id, repos_path, presence, kind, revision, checksum,
 * translated_size, changed_revision, changed_date, changed_author, depth,
 * symlink_target, last_mod_time, properties
 * FROM nodes
 * WHERE wc_id = ?1 AND local_relpath = ?2
 * ORDER BY op_depth DESC
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectNodeInfo extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    public SVNWCDbSelectNodeInfo(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.op_depth);
        fields.add(SVNWCDbSchema.NODES__Fields.repos_id);
        fields.add(SVNWCDbSchema.NODES__Fields.repos_path);
        fields.add(SVNWCDbSchema.NODES__Fields.presence);
        fields.add(SVNWCDbSchema.NODES__Fields.kind);
        fields.add(SVNWCDbSchema.NODES__Fields.revision);
        fields.add(SVNWCDbSchema.NODES__Fields.checksum);
        fields.add(SVNWCDbSchema.NODES__Fields.translated_size);
        fields.add(SVNWCDbSchema.NODES__Fields.changed_revision);
        fields.add(SVNWCDbSchema.NODES__Fields.changed_date);
        fields.add(SVNWCDbSchema.NODES__Fields.changed_author);
        fields.add(SVNWCDbSchema.NODES__Fields.depth);
        fields.add(SVNWCDbSchema.NODES__Fields.symlink_target);
        fields.add(SVNWCDbSchema.NODES__Fields.last_mod_time);
        fields.add(SVNWCDbSchema.NODES__Fields.properties);
    }

    @Override
    protected ISqlJetCursor openCursor() throws SVNException {
        ISqlJetCursor cursor = super.openCursor();
        if (cursor != null) {
            try {
                cursor = cursor.reverse();
            } catch (SqlJetException e) {
                SVNSqlJetDb.createSqlJetError(e);
            }
        }
        return cursor;
    }
    
}
