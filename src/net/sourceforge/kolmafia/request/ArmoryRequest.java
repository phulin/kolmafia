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

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.Limitmode;

public class ArmoryRequest
	extends CoinMasterRequest
{
	public static final String master = "The Armory";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( ArmoryRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( ArmoryRequest.master );
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( ArmoryRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) Coins-spiracy" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.COINSPIRACY, 1 );
	public static final CoinmasterData ARMORY =
		new CoinmasterData(
			ArmoryRequest.master,
			"armory",
			ArmoryRequest.class,
			"Coinspiracy",
			null,
			false,
			ArmoryRequest.TOKEN_PATTERN,
			ArmoryRequest.COIN,
			null,
			ArmoryRequest.itemRows,
			"shop.php?whichshop=si_shop3",
			"buyitem",
			ArmoryRequest.buyItems,
			ArmoryRequest.buyPrices,
			null,
			null,
			null,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			null,
			null,
			true
			);

	public ArmoryRequest()
	{
		super( ArmoryRequest.ARMORY );
	}

	public ArmoryRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( ArmoryRequest.ARMORY, buying, attachments );
	}

	public ArmoryRequest( final boolean buying, final AdventureResult attachment )
	{
		super( ArmoryRequest.ARMORY, buying, attachment );
	}

	public ArmoryRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( ArmoryRequest.ARMORY, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		ArmoryRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=si_shop3" ) )
		{
			return;
		}

		CoinmasterData data = ArmoryRequest.ARMORY;

		String action = GenericRequest.getAction( urlString );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, urlString, responseText );
			return;
		}

		// Parse current coin balances
		CoinMasterRequest.parseBalance( data, responseText );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=si_shop3" ) )
		{
			return false;
		}

		CoinmasterData data = ArmoryRequest.ARMORY;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		if ( !Preferences.getBoolean( "_spookyAirportToday" ) && !Preferences.getBoolean( "spookyAirportAlways" ) )
		{
			return "You don't have access to Conspiracy Island";
		}
		if ( Limitmode.limitZone( "Conspiracy Island" ) )
		{
			return "You cannot currently access Conspiracy Island";
		}
		if ( !Preferences.getBoolean( "armoryUnlocked" ) )
		{
			return "The Armory is locked";
		}
		return null;
	}
}
