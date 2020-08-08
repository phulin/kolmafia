/**
 * Copyright (c) 2005-2020, KoLmafia development team
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.listener.NamedListenerRegistry;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SpinMasterLatheRequest
	extends CoinMasterRequest
{
	public static final String master = "Your SpinMaster&trade; lathe"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( SpinMasterLatheRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getNewMap();
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( SpinMasterLatheRequest.master );

	// Since there are four different currencies, we need to have a map from
	// itemId to item/count of currency; an AdventureResult.
	private static final Map<Integer, AdventureResult> buyCosts = new TreeMap<>();

	private static final Set<Integer> unlockedItems = new HashSet<>();

	public static final CoinmasterData YOUR_SPINMASTER_LATHE =
		new CoinmasterData(
			SpinMasterLatheRequest.master,
			"lathe",
			SpinMasterLatheRequest.class,
			null,
			null,
			false,
			null,
			null,
			null,
			SpinMasterLatheRequest.itemRows,
			"shop.php?whichshop=lathe",
			"buyitem",
			SpinMasterLatheRequest.buyItems,
			SpinMasterLatheRequest.buyPrices,
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
			)
		{
			@Override
			public final boolean availableItem( final int itemId )
			{
				return unlockedItems.contains( itemId );
			}
			@Override
			public AdventureResult itemBuyPrice( final int itemId )
			{
				return SpinMasterLatheRequest.buyCosts.get( IntegerPool.get( itemId ) );
			}
		};

	public static final AdventureResult SPINMASTER = ItemPool.get( ItemPool.SPINMASTER, 1 );
	public static final AdventureResult FLIMSY_HARDWOOD_SCRAPS = ItemPool.get( ItemPool.FLIMSY_HARDWOOD_SCRAPS, 1 );
	public static final AdventureResult DREADSYLVANIAN_HEMLOCK = ItemPool.get( ItemPool.DREADSYLVANIAN_HEMLOCK, 1 );
	public static final AdventureResult SWEATY_BALSAM = ItemPool.get( ItemPool.SWEATY_BALSAM, 1 );
	public static final AdventureResult ANCIENT_REDWOOD = ItemPool.get( ItemPool.ANCIENT_REDWOOD, 1 );
	public static final AdventureResult WORMWOOD_STICK = ItemPool.get( ItemPool.WORMWOOD_STICK, 1 );

	// Manually set up the map and change the currency, as need
	static
	{
		Map<Integer, Integer> map = CoinmastersDatabase.getBuyPrices( SpinMasterLatheRequest.master );
		for ( Entry<Integer, Integer> entry : CoinmastersDatabase.getBuyPrices( SpinMasterLatheRequest.master ).entrySet() )
		{
			int itemId = entry.getKey().intValue();
			int price = entry.getValue().intValue();
			AdventureResult cost = null;
			switch ( itemId )
			{
			default:
				cost = FLIMSY_HARDWOOD_SCRAPS.getInstance( price );
				break;
			case ItemPool.HEMLOCK_HELM:
				cost = DREADSYLVANIAN_HEMLOCK.getInstance( price );
				break;
			case ItemPool.BALSAM_BARREL:
				cost = SWEATY_BALSAM.getInstance( price );
				break;
			case ItemPool.REDWOOD_RAIN_STICK:
				cost = ANCIENT_REDWOOD.getInstance( price );
				break;
			case ItemPool.WORMWOOD_WEDDING_RING:
				cost = WORMWOOD_STICK.getInstance( price );
				break;
			}
			buyCosts.put( itemId, cost );
		}
	}

	public SpinMasterLatheRequest()
	{
		super( SpinMasterLatheRequest.YOUR_SPINMASTER_LATHE );
	}

	public SpinMasterLatheRequest( final String action )
	{
		super( SpinMasterLatheRequest.YOUR_SPINMASTER_LATHE, action );
	}

	public SpinMasterLatheRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( SpinMasterLatheRequest.YOUR_SPINMASTER_LATHE, buying, attachments );
	}

	public SpinMasterLatheRequest( final boolean buying, final AdventureResult attachment )
	{
		super( SpinMasterLatheRequest.YOUR_SPINMASTER_LATHE, buying, attachment );
	}

	public SpinMasterLatheRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( SpinMasterLatheRequest.YOUR_SPINMASTER_LATHE, buying, itemId, quantity );
	}

	@Override
	public void run()
	{
		if ( this.action != null )
		{
			this.addFormField( "pwd" );
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		SpinMasterLatheRequest.parseResponse( this.getURLString(), this.responseText );
	}

	// <tr rel="10587"><td valign=center></td><td><img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/latheblowgun.gif" class="hand pop" rel="desc_item.php?whichitem=835216330" onClick='javascript:descitem(835216330)'></td><td valign=center><a onClick='javascript:descitem(835216330)'><b>beechwood blowgun</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td><td><img src=https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/lathescraps.gif width=30 height=30 onClick='javascript:descitem(303504638)' alt="flimsy hardwood scraps" title="flimsy hardwood scraps"></td><td><b>1</b>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td valign=center><input class="button doit multibuy "  type=button rel='shop.php?whichshop=lathe&action=buyitem&quantity=1&whichrow=1163&pwd=34f70eafe497cf6ce0dbb1524c8cb0ea' value='Lathe'></td></tr>

	private static final Pattern ITEM_PATTERN =
		Pattern.compile( "<tr rel=\"(\\d+)\">.*?onClick='javascript:descitem\\((\\d+)\\)'>.*?<b>(.*?)</b>.*?title=\"(.*?)\".*?<b>([\\d,]+)</b>.*?whichrow=(\\d+)", Pattern.DOTALL );

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=lathe" ) )
		{
			return;
		}

		// Learn new items by simply visiting the SpinMaster Lathe
		// Refresh the Coin Master inventory every time we visit.

		CoinmasterData data = SpinMasterLatheRequest.YOUR_SPINMASTER_LATHE;
		List<AdventureResult> items = new ArrayList<>();
		Map<Integer, AdventureResult> costs = new TreeMap<>();
		Map<Integer, Integer> rows = new TreeMap<>();

		SpinMasterLatheRequest.unlockedItems.clear();

		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int itemId = StringUtilities.parseInt( matcher.group(1) );
			Integer iitemId = IntegerPool.get( itemId );
			String descId = matcher.group(2);
			String itemName = matcher.group(3);
			String currency = matcher.group(4);
			int price = StringUtilities.parseInt( matcher.group(5) );
			int row = StringUtilities.parseInt( matcher.group(6) );

			String match = ItemDatabase.getItemDataName( itemId );
			if ( match == null || !match.equals( itemName ) )
			{
				ItemDatabase.registerItem( itemId, itemName, descId );
			}

			// Add it to the unlocked items
			SpinMasterLatheRequest.unlockedItems.add( itemId );

			AdventureResult item = ItemPool.get( itemId, PurchaseRequest.MAX_QUANTITY );
			items.add( item );
			AdventureResult cost = ItemPool.get( currency, price );
			costs.put( iitemId, cost );
			if ( !SpinMasterLatheRequest.itemRows.containsKey( iitemId ) )
			{
				NPCPurchaseRequest.learnCoinmasterItem( master, itemName, String.valueOf( price ), String.valueOf( row ) );
			}
			rows.put( iitemId, IntegerPool.get( row ) );
		}

		SpinMasterLatheRequest.buyItems.clear();
		SpinMasterLatheRequest.buyItems.addAll( items );
		SpinMasterLatheRequest.buyCosts.clear();
		SpinMasterLatheRequest.buyCosts.putAll( costs );
		SpinMasterLatheRequest.itemRows.clear();
		SpinMasterLatheRequest.itemRows.putAll( rows );

		// Register the purchase requests, now that we know what is available
		data.registerPurchaseRequests();
		NamedListenerRegistry.fireChange( "(coinmaster)" );

		int itemId = CoinMasterRequest.extractItemId( data, location );
		if ( itemId == -1 )
		{
			// Simple visit
			CoinMasterRequest.parseBalance( data, responseText );
			return;
		}
		Preferences.setBoolean( "_spinmasterLatheVisited", true );

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static String accessible()
	{
		if ( !InventoryManager.hasItem( SpinMasterLatheRequest.SPINMASTER ) )
		{
			return "You don't own a " + SpinMasterLatheRequest.SPINMASTER.getName();
		}

		return null;
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=lathe" ) )
		{
			return false;
		}

		CoinmasterData data = SpinMasterLatheRequest.YOUR_SPINMASTER_LATHE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
