/**
 * Copyright (c) 2005-2018, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.svn;

import java.io.File;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

public class CheckoutRunnable
	implements Runnable
{
	private SVNURL repo;

	public CheckoutRunnable( SVNURL url )
	{
		this.repo = url;
	}

	public void run()
	{
		if ( !KoLmafia.permitsContinue() )
			return;

		String UUID = SVNManager.getFolderUUID( repo );

		if ( UUID == null )
		{
			return;
		}

		KoLmafia.updateDisplay( "Starting Checkout..." );

		if ( SVNManager.validateRepo( repo ) )
		{
			return;
		}

		File WCDir = SVNManager.doDirSetup( UUID );
		if ( WCDir == null )
		{
			RequestLogger.printLine( MafiaState.ERROR, "Something went wrong creating directories..." );
			return;
		}

		if ( !KoLmafia.permitsContinue() )
			return;

		try
		{
			SVNManager.SVN_LOCK.lock();
			SVNManager.checkout( repo, SVNRevision.HEAD, WCDir, true );
		}
		catch ( SVNException e )
		{
			error( e, "SVN ERROR during checkout operation.  Aborting..." );
			return;
		}
		finally
		{
			SVNManager.SVN_LOCK.unlock();
		}

		RequestLogger.printLine();
		RequestLogger.printLine( "Successfully checked out working copy." );
	}

	private static void error( SVNException e, String addMessage )
	{
		RequestLogger.printLine( e.getErrorMessage().getMessage() );
		if ( addMessage != null )
			KoLmafia.updateDisplay( MafiaState.ERROR, addMessage );
	}

}
