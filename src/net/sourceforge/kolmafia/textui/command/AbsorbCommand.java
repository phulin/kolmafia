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

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.request.CharPaneRequest;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

public class AbsorbCommand
	extends AbstractCommand
{
	public AbsorbCommand()
	{
		this.usage = " <item> - absorb item.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( !KoLCharacter.inNoobcore() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You are not in a Gelatinous Noob run" );
			return;
		}

		if ( parameters.length() == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "No items specified." );
			return;
		}

		if ( KoLCharacter.getAbsorbs() >= KoLCharacter.getAbsorbsLimit() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Cannot absorb items at present." );
			return;
		}

		AdventureResult match = ItemFinder.getFirstMatchingItem( parameters, Match.ABSORB );
		if ( match == null )
		{
			return;
		}

		int itemId = match.getItemId();

		// If not in inventory, try to retrieve it (if it's in inventory, doesn't matter if outside Standard)
		if ( !InventoryManager.hasItem( match, true ) &&
		     !InventoryManager.retrieveItem( match ) &&
		     match.getCount( KoLConstants.inventory ) == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Item not accessible." );
			return;
		}

		// Absorb the item
		RequestThread.postRequest( new GenericRequest( "inventory.php?absorb=" + itemId + "&ajax=1", false ) );

		// Parse the charpane for updated absorb info
		RequestThread.postRequest( new CharPaneRequest() );
		// update "Hatter" daily deed
		if ( ItemDatabase.isHat( itemId ) )
		{
			PreferenceListenerRegistry.firePreferenceChanged( "(hats)" );
		}
	}
}
