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
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

public class TrapperRequest
	extends CoinMasterRequest
{
	public static String master = "The Trapper"; 
	public static LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( TrapperRequest.master );
	private static Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( TrapperRequest.master );
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( TrapperRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "([\\d,]+) yeti fur" );
	public static final AdventureResult YETI_FUR = ItemPool.get( ItemPool.YETI_FUR, 1 );
	public static final CoinmasterData TRAPPER =
		new CoinmasterData(
			TrapperRequest.master,
			"trapper",
			TrapperRequest.class,
			"yeti fur",
			"no yeti furs",
			false,
			TrapperRequest.TOKEN_PATTERN,
			TrapperRequest.YETI_FUR,
			null,
			TrapperRequest.itemRows,
			"shop.php?whichshop=trapper",
			"buyitem",
			TrapperRequest.buyItems,
			TrapperRequest.buyPrices,
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

	public TrapperRequest()
	{
		super( TrapperRequest.TRAPPER );
	}

	public TrapperRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( TrapperRequest.TRAPPER, buying, attachments );
	}

	public TrapperRequest( final boolean buying, final AdventureResult attachment )
	{
		super( TrapperRequest.TRAPPER, buying, attachment );
	}

	public TrapperRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( TrapperRequest.TRAPPER, buying, itemId, quantity );
	}

	public TrapperRequest( final int itemId, final int quantity )
	{
		this( true, itemId, quantity );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		// I'm plumb stocked up on everythin' 'cept yeti furs, Adventurer.
		// If you've got any to trade, I'd be much obliged."
		if ( responseText.contains( "yeti furs" ) )
		{
			Preferences.setInteger( "lastTr4pz0rQuest", KoLCharacter.getAscensions() );
			QuestDatabase.setQuestProgress( Quest.TRAPPER, QuestDatabase.FINISHED );
		}
		CoinMasterRequest.parseResponse( TrapperRequest.TRAPPER, urlString, responseText );
	}

	public static final boolean registerRequest( final String urlString )
	{
		// shop.php?pwd&whichshop=trapper
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=trapper" ) )
		{
			return false;
		}

		CoinmasterData data = TrapperRequest.TRAPPER;
		return CoinMasterRequest.registerRequest( data, urlString );
	}

	public static String accessible()
	{
		if ( KoLCharacter.getLevel() < 8 )
		{
			return "You haven't met the Trapper yet";
		}
		if ( !KoLCharacter.getTrapperQuestCompleted() )
		{
			return "You have unfinished business with the Trapper";
		}
		if ( KoLCharacter.inZombiecore() )
		{
			return "The trapper won't be back for quite a while";
		}
		return null;
	}
}
