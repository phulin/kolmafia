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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.ResultProcessor;

public class WaxGlobRequest
	extends CreateItemRequest
{
	public WaxGlobRequest( final Concoction conc )
	{
		super( "choice.php", conc );
		this.addFormField( "whichchoice", "1218" );
		this.addFormField( "option", WaxGlobRequest.itemIdToOption( conc.getItemId() ) );
	}

	private static String itemIdToOption( final int itemId )
	{
		return  itemId == ItemPool.MINIATURE_CANDLE ? "1" :
			itemId == ItemPool.WAX_HAND ? "2" :
			itemId == ItemPool.WAX_FACE ? "3" :
			itemId == ItemPool.WAX_PANCAKE ? "4" :
			itemId == ItemPool.WAX_BOOZE ? "5" :
			"6";
	}

	private static String optionToName( final int option )
	{
		return  option == 1 ? "miniature candle" :
			option == 2 ? "wax hand" :
			option == 3 ? "wax face" :
			option == 4 ? "wax pancake" :
			option == 4 ? "wax booze" :
			"unknown";
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	@Override
	public void run()
	{
		// Attempt to retrieve the ingredients
		if ( !this.makeIngredients() )
		{
			return;
		}

		int count = this.getQuantityNeeded();
		String name = this.getName();

		KoLmafia.updateDisplay( "Creating " + count + " " + name + "..." );

		GenericRequest useRequest = new GenericRequest( "inv_use.php" );
		useRequest.addFormField( "whichitem", String.valueOf( ItemPool.WAX_GLOB ) );
		useRequest.run();

		for ( int i = 0; i < count; ++i )
		{
			super.run();
		}

		GenericRequest closeRequest = new GenericRequest( "choice.php" );
		closeRequest.addFormField( "whichchoice", "1218" );
		closeRequest.addFormField( "option", "6" );
		closeRequest.run();
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) || !urlString.contains( "whichchoice=1218" ) )
		{
			return false;
		}

		return true;
	}
}
