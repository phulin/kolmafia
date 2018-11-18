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

import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;

/**
 * INSERT INTO nodes ( wc_id, local_relpath, op_depth, parent_relpath, repos_id,
 * repos_path, revision, presence, depth, kind, changed_revision, changed_date,
 * changed_author, checksum, properties, translated_size, last_mod_time,
 * symlink_target ) SELECT wc_id, local_relpath, ?3 AS op_depth, parent_relpath,
 * repos_id, repos_path, revision, presence, depth, kind, changed_revision,
 * changed_date, changed_author, checksum, properties, translated_size,
 * last_mod_time, symlink_target FROM nodes WHERE wc_id = ?1 AND local_relpath =
 * ?2 AND op_depth = 0;
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbInsertWorkingNodeFromBaseCopy extends SVNSqlJetInsertStatement {

    protected SVNSqlJetSelectStatement select;

    public SVNWCDbInsertWorkingNodeFromBaseCopy(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        select = new SVNSqlJetSelectStatement(sDb, SVNWCDbSchema.NODES) {

            protected Object[] getWhere() throws SVNException {
                return new Object[] {
                        getBind(1), getBind(2), 0
                };
            };

            protected boolean isFilterPassed() throws SVNException {
                if (isColumnNull(SVNWCDbSchema.NODES__Fields.wc_id) && getBind(1) != null) {
                    return false;
                }
                if (isColumnNull(SVNWCDbSchema.NODES__Fields.local_relpath) && getBind(2) != null) {
                    return false;
                }
                return getColumn(SVNWCDbSchema.NODES__Fields.wc_id).equals(getBind(1)) && getColumn(SVNWCDbSchema.NODES__Fields.local_relpath).equals(getBind(2))
                        && getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == 0;

            };

        };
    }

    public long exec() throws SVNException {
        select.bindf("is", getBind(1), getBind(2));
        long n = 0;
        try {
            while (select.next()) {
                super.exec();
            }
        } finally {
            select.reset();
        }
        return n;
    }

    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String, Object> rowValues = select.getRowValues();
        rowValues.put(SVNWCDbSchema.NODES__Fields.op_depth.toString(), getBind(3));
        return rowValues;
    }

}
