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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.Modifiers.Modifier;
import net.sourceforge.kolmafia.Modifiers.ModifierList;
import net.sourceforge.kolmafia.ModifierExpression;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.MonsterManuelRequest;
import net.sourceforge.kolmafia.request.ZapRequest;

import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.*;

import org.json.JSONException;
import org.json.JSONObject;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DebugDatabase
{
	//private static final Pattern WIKI_ITEMID_PATTERN = Pattern.compile( "Item number</a>:</b> (\\d+)<br />" );
	//private static final Pattern WIKI_DESCID_PATTERN = Pattern.compile( "<b>Description ID:</b> (\\d+)<br />" );
	private static final Pattern WIKI_PLURAL_PATTERN =
		Pattern.compile( "\\(.*?In-game plural</a>: <i>(.*?)</i>\\)", Pattern.DOTALL );
	//private static final Pattern WIKI_AUTOSELL_PATTERN = Pattern.compile( "Selling Price: <b>(\\d+) Meat.</b>" );

	/**
	 * Takes an item name and constructs the likely Wiki equivalent of that item name.
	 */

	private static final String readWikiData( final String name )
	{
		try
		{
			String url = WikiUtilities.getWikiLocation( name, WikiUtilities.ITEM_TYPE );
			HttpURLConnection connection = (HttpURLConnection) new URL( null, url ).openConnection();
			connection.setRequestProperty( "Connection", "close" ); // no need to keep-alive
			InputStream istream = connection.getInputStream();

			if ( connection.getResponseCode() != 200 )
			{
				return "";
			}
			
			byte[] bytes = ByteBufferUtilities.read( istream );
			return StringUtilities.getEncodedString( bytes, "UTF-8" );
		}
		catch ( IOException e )
		{
			return "";
		}
	}

	private static final String readApiPlural( final int itemId )
	{
		GenericRequest request = new ApiRequest( "item", itemId );
		RequestThread.postRequest( request );
		String plural = "";
		JSONObject json;
		try
		{
			json = new JSONObject( request.responseText );
			plural = (String) json.get( "plural" );
		}
		catch ( JSONException ex )
		{
			
		}
		return plural;
	}

	/**
	 * Utility method which searches for the plural version of the item on the KoL wiki.
	 */

	/*public static final void determineWikiData( final String name )
	{
		String wikiData = DebugDatabase.readWikiData( name );

		Matcher itemMatcher = DebugDatabase.WIKI_ITEMID_PATTERN.matcher( wikiData );
		if ( !itemMatcher.find() )
		{
			RequestLogger.printLine( name + " did not match a valid an item entry." );
			return;
		}

		Matcher descMatcher = DebugDatabase.WIKI_DESCID_PATTERN.matcher( wikiData );
		if ( !descMatcher.find() )
		{
			RequestLogger.printLine( name + " did not match a valid an item entry." );
			return;
		}

		RequestLogger.printLine( "item: " + name + " (#" + itemMatcher.group( 1 ) + ")" );
		RequestLogger.printLine( "desc: " + descMatcher.group( 1 ) );

		Matcher pluralMatcher = DebugDatabase.WIKI_PLURAL_PATTERN.matcher( wikiData );
		if ( pluralMatcher.find() )
		{
			RequestLogger.printLine( "plural: " + pluralMatcher.group( 1 ) );
		}

		Matcher sellMatcher = DebugDatabase.WIKI_AUTOSELL_PATTERN.matcher( wikiData );
		if ( sellMatcher.find() )
		{
			RequestLogger.printLine( "autosell: " + sellMatcher.group( 1 ) );
		}
	}*/

	// **********************************************************

	// Support for the "checkitems" command, which compares KoLmafia's
	// internal item data from what can be mined from the item description.

	private static final String ITEM_HTML = "itemhtml.txt";
	private static final String ITEM_DATA = "itemdata.txt";
	private static final StringArray rawItems = new StringArray();

	private static class ItemMap
	{
		private final String tag;
		private final int type;
		private final Map<String, String> map;

		public ItemMap( final String tag, final int type )
		{
			this.tag = tag;
			this.type = type;
			this.map = new TreeMap<String, String>( KoLConstants.ignoreCaseComparator );
		}

		public String getTag()
		{
			return this.tag;
		}

		public int getType()
		{
			return this.type;
		}

		public Map<String, String> getMap()
		{
			return this.map;
		}

		public void clear()
		{
			this.map.clear();
		}

		public void put( String name, String text )
		{
			this.map.put( name, text );
		}

		@Override
		public String toString()
		{
			return this.tag;
		}
	}

	private static final ItemMap [] ITEM_MAPS =
	{
		new ItemMap( "Food", KoLConstants.CONSUME_EAT ),
		new ItemMap( "Booze", KoLConstants.CONSUME_DRINK ),
		new ItemMap( "Spleen Toxins", KoLConstants.CONSUME_SPLEEN ),
		new ItemMap( "Hats", KoLConstants.EQUIP_HAT ),
		new ItemMap( "Weapons", KoLConstants.EQUIP_WEAPON ),
		new ItemMap( "Off-hand Items", KoLConstants.EQUIP_OFFHAND ),
		new ItemMap( "Shirts", KoLConstants.EQUIP_SHIRT ),
		new ItemMap( "Pants", KoLConstants.EQUIP_PANTS ),
		new ItemMap( "Accessories", KoLConstants.EQUIP_ACCESSORY ),
		new ItemMap( "Containers", KoLConstants.EQUIP_CONTAINER ),
		new ItemMap( "Familiar Items", KoLConstants.EQUIP_FAMILIAR ),
		new ItemMap( "Everything Else", -1 ),
	};

	private static final ItemMap findItemMap( final int type )
	{
		ItemMap other = null;
		for ( int i = 0; i < DebugDatabase.ITEM_MAPS.length; ++i )
		{
			ItemMap map = DebugDatabase.ITEM_MAPS[ i ];
			int mapType = map.getType();
			if ( mapType == type )
			{
				return map;
			}
			if ( mapType == -1 )
			{
				other = map;
			}
		}

		return other;
	}

	public static final void checkItems( final int itemId )
	{
		RequestLogger.printLine( "Loading previous data..." );
		DebugDatabase.loadScrapeData( rawItems, ITEM_HTML );

		RequestLogger.printLine( "Checking internal data..." );

		PrintStream report = DebugDatabase.openReport( ITEM_DATA );

		for ( int i = 0; i < DebugDatabase.ITEM_MAPS.length; ++i )
		{
			ItemMap map = DebugDatabase.ITEM_MAPS[ i ];
			map.clear();
		}

		// Check item names, desc ID, consumption type

		if ( itemId == 0 )
		{
			DebugDatabase.checkItems( report );
		}
		else
		{
			DebugDatabase.checkItem( itemId, report );
		}

		// Check level limits, equipment, modifiers

		DebugDatabase.checkConsumableItems( report );
		DebugDatabase.checkEquipment( report );
		DebugDatabase.checkItemModifiers( report );

		report.close();
	}

	private static final void checkItems( final PrintStream report )
	{
		Set<Integer> keys = ItemDatabase.descriptionIdKeySet();
		int lastId = 0;

		for ( Integer id : keys )
		{
			if ( id < 1 )
			{
				continue;
			}

			while ( ++lastId < id )
			{
				report.println( lastId );
			}

			DebugDatabase.checkItem( id, report );
		}

		DebugDatabase.saveScrapeData( keys.iterator(), rawItems, ITEM_HTML );
	}

	private static final void checkItem( final int itemId, final PrintStream report )
	{
		Integer id = IntegerPool.get( itemId );

		String name = ItemDatabase.getItemDataName( id );
		if ( name == null )
		{
			report.println( itemId );
			return;
		}

		String rawText = DebugDatabase.rawItemDescriptionText( itemId );

		if ( rawText == null )
		{
			report.println( "# *** " + name + " (" + itemId + ") has no description." );
			return;
		}

		String text = DebugDatabase.itemDescriptionText( rawText );
		if ( text == null )
		{
			report.println( "# *** " + name + " (" + itemId + ") has malformed description text." );
			DebugDatabase.rawItems.set( itemId, null );
			return;
		}

		String descriptionName = DebugDatabase.parseName( text );
		if ( !name.equals( descriptionName ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has description of " + descriptionName + "." );
			DebugDatabase.rawItems.set( itemId, null );
			return;

		}

		int type = ItemDatabase.getConsumptionType( itemId );
		String descType = DebugDatabase.parseType( text );
		int descPrimary = DebugDatabase.typeToPrimary( descType, false );
		if ( !typesMatch( type, descPrimary ) )
		{
			String primary = ItemDatabase.typeToPrimaryUsage( type );
			report.println( "# *** " + name + " (" + itemId + ") has primary usage of " + primary + " but is described as " + descType + "." );
		}

		int attrs = ItemDatabase.getAttributes( itemId );
		int descAttrs = DebugDatabase.typeToSecondary( descType, descPrimary, text, false );
		if ( !DebugDatabase.attributesMatch( attrs, descAttrs ) )
		{
			String secondary = ItemDatabase.attrsToSecondaryUsage( attrs );
			String descSecondary = ItemDatabase.attrsToSecondaryUsage( descAttrs );
			report.println( "# *** " + name + " (" + itemId + ") has secondary usage of " + secondary + " but is described as " + descSecondary + "." );
		}

		int price = ItemDatabase.getPriceById( itemId );
		int descPrice = DebugDatabase.parsePrice( text );
		if ( price != descPrice && ( price >= 0 || descPrice != 0 ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has price of " + price + " but should be " + descPrice + "." );
		}

		String access = ItemDatabase.getAccessById( id );
		String descAccess = DebugDatabase.parseAccess( text );
		if ( !access.equals( descAccess ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has access of " + access + " but should be " + descAccess + "." );
		}

		String image = ItemDatabase.getImage( id );
		String descImage = DebugDatabase.parseImage( rawText );
		if ( !image.equals( descImage ) )
		{
			report.println( "# *** " + name + " (" + itemId + ") has image of " + image + " but should be " + descImage + "." );
		}

		ItemMap map = DebugDatabase.findItemMap( type );
		map.put( name, text );

		String descId = ItemDatabase.getDescriptionId( id );

		// Intentionally get a null if there is not an explicit plural in the database
		String plural = ItemDatabase.getPluralById( id );

		// In fact, if the plural is simply the name + "s", suppress it.
		if ( plural != null && plural.equals( name + "s" ) )
		{
			plural = null;
		}

		report.println( ItemDatabase.itemString( itemId, name, descId, image, type, attrs, access, price, plural ) );
	}

	private static final GenericRequest DESC_ITEM_REQUEST = new GenericRequest( "desc_item.php" );

	public static final String itemDescriptionText( final int itemId, boolean forceReload )
	{
		return DebugDatabase.itemDescriptionText( DebugDatabase.rawItemDescriptionText( ItemDatabase.getDescriptionId( itemId ), forceReload ) );
	}

	public static final String rawItemDescriptionText( final int itemId )
	{
		return DebugDatabase.rawItemDescriptionText( ItemDatabase.getDescriptionId( itemId ), false );
	}

	public static final String rawItemDescriptionText( final String descId, boolean forceReload )
	{
		if ( descId == null )
		{
			return "";
		}
		int itemId = ItemDatabase.getItemIdFromDescription( descId );
		String previous = null;
		if ( itemId != -1 )
		{
			previous = DebugDatabase.rawItems.get( itemId );
		}
		if ( !forceReload && previous != null && !previous.equals( "" ) )
		{
			return previous;
		}

		DebugDatabase.DESC_ITEM_REQUEST.clearDataFields();
		DebugDatabase.DESC_ITEM_REQUEST.addFormField( "whichitem", descId );
		RequestThread.postRequest( DebugDatabase.DESC_ITEM_REQUEST );
		if ( itemId == -1 )
		{
			itemId = DebugDatabase.parseItemId( DebugDatabase.DESC_ITEM_REQUEST.responseText );
		}
		DebugDatabase.rawItems.set( itemId, DebugDatabase.DESC_ITEM_REQUEST.responseText );

		return DebugDatabase.DESC_ITEM_REQUEST.responseText;
	}

	private static final Pattern ITEM_DATA_PATTERN = Pattern.compile( "<div id=\"description\"[^>]*>(.*?)<script", Pattern.DOTALL );

	public static final String itemDescriptionText( final String rawText )
	{
		if ( rawText == null )
		{
			return null;
		}

		Matcher matcher = DebugDatabase.ITEM_DATA_PATTERN.matcher( rawText );
		return matcher.find() ? matcher.group( 1 ) : null;
	}

	// <!-- itemid: 806 -->
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "<!-- itemid: ([\\d]*) -->" );
	public static final int parseItemId( final String text )
	{
		Matcher matcher = DebugDatabase.ITEMID_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return 0;
		}

		return StringUtilities.parseInt( matcher.group( 1 ) );
	}

	private static final Pattern NAME_PATTERN = Pattern.compile( "<b>(.*?)</b>" );
	public static final String parseName( final String text )
	{
		Matcher matcher = DebugDatabase.NAME_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return "";
		}

		// One item is known to have an extra internal space
		return StringUtilities.globalStringReplace( matcher.group( 1 ), "  ", " " ).trim();
	}

	private static final Pattern PRICE_PATTERN = Pattern.compile( "Selling Price: <b>(\\d+) Meat.</b>" );
	public static final int parsePrice( final String text )
	{
		Matcher matcher = DebugDatabase.PRICE_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return 0;
		}

		return StringUtilities.parseInt( matcher.group( 1 ) );
	}

	private static final StringBuilder appendAccessTypes( StringBuilder accessTypes, String accessType )
	{
		if ( accessTypes.length() > 0 )
		{
			return accessTypes.append( "," ).append( accessType );
		}
		return accessTypes.append( accessType );
	}

	public static final String parseAccess( final String text )
	{
		StringBuilder accessTypes = new StringBuilder();

		if ( text.contains( "Quest Item" ) ||
		     text.contains( "This item will disappear at the end of the day." ) )
		{
			accessTypes = appendAccessTypes( accessTypes, ItemDatabase.QUEST_FLAG );
		}

		// Quest items cannot be gifted or traded
		else if ( text.contains( "Gift Item" ) && !text.contains( "gift package" ) )
		{
			accessTypes = appendAccessTypes( accessTypes, ItemDatabase.GIFT_FLAG );
		}

		// Gift items cannot be (normally) traded
		else if ( !text.contains( "Cannot be traded" ) )
		{
			accessTypes = appendAccessTypes( accessTypes, ItemDatabase.TRADE_FLAG );
		}

		//We shouldn't just check for "discarded", in case "discarded" appears somewhere else in the description.
		if ( !text.contains( "Cannot be discarded" ) && !text.contains( "Cannot be traded or discarded" ) )
		{
			accessTypes = appendAccessTypes( accessTypes, ItemDatabase.DISCARD_FLAG );
		}

		return accessTypes.toString();
	}

	private static final Pattern TYPE_PATTERN = Pattern.compile( "Type: <b>(.*?)</b>" );
	public static final String parseType( final String text )
	{
		Matcher matcher = DebugDatabase.TYPE_PATTERN.matcher( text );
		String type = matcher.find() ? matcher.group( 1 ) : "";
		return type.equals( "back item" ) ? "container" : type;
	}

	public static final int typeToPrimary( final String type, final boolean multi )
	{
		// Type: <b>food <font color=#999999>(crappy)</font></b>
		// Type: <b>food (decent)</b>
		// Type: <b>booze <font color=green>(good)</font></b>
		// Type: <b>food <font color=blue>(awesome)</font></b>
		// Type: <b>food <font color=blueviolet>(EPIC)</font></b>

		if ( type.equals( "" ) || type.equals( "crafting item" ) )
		{
			return KoLConstants.NO_CONSUME;
		}
		if ( type.startsWith( "food" ) || type.startsWith( "beverage" ) )
		{
			return KoLConstants.CONSUME_EAT;
		}
		if ( type.startsWith( "booze" ) )
		{
			return KoLConstants.CONSUME_DRINK;
		}
		if ( type.startsWith( "spleen item" ) )
		{
			return KoLConstants.CONSUME_SPLEEN;
		}
		if ( type.contains( "self or others" ) )
		{
			// Curse items are special
			return KoLConstants.NO_CONSUME;
		}
		if ( type.startsWith( "usable" ) || type.contains( " usable" ) || type.equals( "gift package" ) || type.equals( "potion" ) )
		{
			// Although most potions end up being multi-usable, KoL
			// almost always forgets to add that flag when the item
			// is first introduced.
			//
			// We'll assume they are single-usable unless we are
			// explicitly told otherwise in a "rel" string

			return multi ? KoLConstants.CONSUME_MULTIPLE : KoLConstants.CONSUME_USE;
		}
		if ( type.equals( "familiar equipment" ) )
		{
			return KoLConstants.EQUIP_FAMILIAR;
		}
		if ( type.startsWith( "familiar" ) )
		{
			return KoLConstants.GROW_FAMILIAR;
		}
		if ( type.startsWith( "accessory" ) )
		{
			return KoLConstants.EQUIP_ACCESSORY;
		}
		if ( type.startsWith( "container" ) )
		{
			return KoLConstants.EQUIP_CONTAINER;
		}
		if ( type.startsWith( "hat" ) )
		{
			return KoLConstants.EQUIP_HAT;
		}
		if ( type.startsWith( "shirt" ) )
		{
			return KoLConstants.EQUIP_SHIRT;
		}
		if ( type.startsWith( "pants" ) )
		{
			return KoLConstants.EQUIP_PANTS;
		}
		if ( type.contains( "weapon" ) )
		{
			return KoLConstants.EQUIP_WEAPON;
		}
		if ( type.startsWith( "off-hand item" ) )
		{
			return KoLConstants.EQUIP_OFFHAND;
		}
		return KoLConstants.NO_CONSUME;
	}

	public static final int typeToSecondary( final String type, final int primary, final String text, final boolean multi )
	{
		int attributes = 0;
		boolean usable =
			type.startsWith( "usable" ) ||
			type.contains( " usable" ) ||
			type.contains( "spleen" ) ||
			type.contains( "potion" ) ||
			type.equals( "gift package" );
		if ( type.contains( "combat" ) && type.contains( "reusable" ) )
		{
			attributes |= ItemDatabase.ATTR_COMBAT_REUSABLE;
		}
		else if ( type.contains( "combat" ) )
		{
			attributes |= ItemDatabase.ATTR_COMBAT;
		}
		else if ( type.contains( "reusable" ) )
		{
			attributes |= ItemDatabase.ATTR_REUSABLE;
		}
		if ( multi && primary != KoLConstants.CONSUME_MULTIPLE && usable )
		{
			attributes |= ItemDatabase.ATTR_MULTIPLE;
		}
		if ( !multi && primary != KoLConstants.CONSUME_USE && usable )
		{
			attributes |= ItemDatabase.ATTR_USABLE;
		}
		if ( type.contains( "self or others" ) )
		{
			attributes |= ItemDatabase.ATTR_CURSE;
		}
		if ( text.contains( "(Fancy" ) )
		{
			attributes |= ItemDatabase.ATTR_FANCY;
		}
		return attributes;
	}

	private static final boolean typesMatch( final int type, final int descType )
	{
		switch ( type )
		{
		case KoLConstants.NO_CONSUME:
			// We intentionally disallow certain items from being
			// "used" through the GUI.
			return descType == KoLConstants.NO_CONSUME ||
			       descType == KoLConstants.CONSUME_USE;
		case KoLConstants.CONSUME_EAT:
		case KoLConstants.CONSUME_DRINK:
		case KoLConstants.CONSUME_SPLEEN:
		case KoLConstants.GROW_FAMILIAR:
		case KoLConstants.EQUIP_FAMILIAR:
		case KoLConstants.EQUIP_ACCESSORY:
		case KoLConstants.EQUIP_CONTAINER:
		case KoLConstants.EQUIP_HAT:
		case KoLConstants.EQUIP_PANTS:
		case KoLConstants.EQUIP_SHIRT:
		case KoLConstants.EQUIP_WEAPON:
		case KoLConstants.EQUIP_OFFHAND:
			return descType == type;
		case KoLConstants.MESSAGE_DISPLAY:
		case KoLConstants.CONSUME_USE:
		case KoLConstants.CONSUME_MULTIPLE:
		case KoLConstants.CONSUME_AVATAR:
		case KoLConstants.INFINITE_USES:
			return descType == KoLConstants.CONSUME_USE ||
			       descType == KoLConstants.CONSUME_MULTIPLE ||
			       descType == KoLConstants.CONSUME_EAT ||
			       descType == KoLConstants.CONSUME_DRINK ||
			       descType == KoLConstants.CONSUME_AVATAR ||
			       descType == KoLConstants.NO_CONSUME;
		case KoLConstants.CONSUME_FOOD_HELPER:
		case KoLConstants.CONSUME_DRINK_HELPER:
		case KoLConstants.CONSUME_STICKER:
		case KoLConstants.CONSUME_FOLDER:
		case KoLConstants.CONSUME_POKEPILL:
			return descType == KoLConstants.NO_CONSUME ||
			       descType == KoLConstants.CONSUME_USE;
		case KoLConstants.CONSUME_CARD:
		case KoLConstants.CONSUME_SPHERE:
		case KoLConstants.CONSUME_ZAP:
			return descType == KoLConstants.NO_CONSUME;
		}
		return true;
	}

	private static final boolean attributesMatch( final int attrs, final int descAttrs )
	{
		// If the description says an item is "combat", "(reusable)" or "(on self or others)",
		// our database must mark the item as ATTR_COMBAT, ATTR_COMBAT_REUSABLE, ATTR_CURSE, 
		//
		// However, there are quite a few items that we mark with those secondary attributes that are
		// not tagged that way by KoL itself. Assume those are correct.

		if ( ( descAttrs & ItemDatabase.ATTR_COMBAT ) != 0 && ( attrs & ItemDatabase.ATTR_COMBAT ) == 0 )
		{
			return false;
		}

		if ( ( descAttrs & ItemDatabase.ATTR_COMBAT_REUSABLE ) != 0 && ( attrs & ItemDatabase.ATTR_COMBAT_REUSABLE ) == 0 )
		{
			return false;
		}

		if ( ( descAttrs & ItemDatabase.ATTR_CURSE ) != 0 && ( attrs & ItemDatabase.ATTR_CURSE ) == 0 )
		{
			return false;
		}

		// If the item is a (Fancy Cooking ingredient) or (Fancy Cocktailcrafting ingredient)
		// we must mark the item with ATTR_FANCY
		if ( ( descAttrs & ItemDatabase.ATTR_FANCY ) != ( attrs & ItemDatabase.ATTR_FANCY ) )
		{
			return false;
		}

		return true;
	}

	private static final void checkConsumableItems( final PrintStream report )
	{
		RequestLogger.printLine( "Checking level requirements..." );

		DebugDatabase.checkConsumableMap( report, DebugDatabase.findItemMap( KoLConstants.CONSUME_EAT ) );
		DebugDatabase.checkConsumableMap( report, DebugDatabase.findItemMap( KoLConstants.CONSUME_DRINK ) );
		DebugDatabase.checkConsumableMap( report, DebugDatabase.findItemMap( KoLConstants.CONSUME_SPLEEN ) );
	}

	private static final void checkConsumableMap( final PrintStream report, final ItemMap imap )
	{
		Map<String, String> map = imap.getMap();
		if ( map.size() == 0 )
		{
			return;
		}

		String tag = imap.getTag();
		int type = imap.getType();
		String  file =
			type == KoLConstants.CONSUME_EAT ? "fullness" :
			type == KoLConstants.CONSUME_DRINK ? "inebriety" :
			"spleenhit";

		RequestLogger.printLine( "Checking " + tag + "..." );

		report.println( "" );
		report.println( "# Level requirements in " + file + ".txt" );

		Object[] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			String text = map.get( name );
			DebugDatabase.checkConsumableDatum( name, type, text, report );
		}
	}

	private static final void checkConsumableDatum( final String name, final int type, final String text, final PrintStream report )
	{
		Integer requirement = ConsumablesDatabase.getLevelReqByName( name );
		int level = requirement == null ? 0 : requirement.intValue();
		int descLevel = DebugDatabase.parseLevel( text );
		if ( level != descLevel )
		{
			report.println( "# *** " + name + " requires level " + level + " but should be " + descLevel + "." );
		}

		int size =
			( type == KoLConstants.CONSUME_EAT ) ?
			ConsumablesDatabase.getFullness( name ) :
			( type == KoLConstants.CONSUME_DRINK ) ?
			ConsumablesDatabase.getInebriety( name ) :
			( type == KoLConstants.CONSUME_SPLEEN ) ?
			ConsumablesDatabase.getSpleenHit( name ) :
			1;

		int descSize = DebugDatabase.parseSize( text );
		if ( size != descSize )
		{
			report.println( "# *** " + name + " is size " + size + " but should be " + descSize + "." );
		}

		String quality = ConsumablesDatabase.getQuality( name );
		String descQuality = DebugDatabase.parseQuality( text );
		if ( !quality.equals( descQuality ) )
		{
			report.println( "# *** " + name + " is quality " + quality + " but should be " + descQuality + "." );
		}
	}

	private static final Pattern LEVEL_PATTERN = Pattern.compile( "Level required: <b>(.*?)</b>" );

	public static final int parseLevel( final String text )
	{
		Matcher matcher = DebugDatabase.LEVEL_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return 1;
		}

		return StringUtilities.parseInt( matcher.group( 1 ) );
	}

	private static final Pattern SIZE_PATTERN = Pattern.compile( "(?:Size|Potency|Toxicity): <b>(.*?)</b>" );

	public static final int parseSize( final String text )
	{
		Matcher matcher = DebugDatabase.SIZE_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return 1;
		}

		return StringUtilities.parseInt( matcher.group( 1 ) );
	}

	private static final void checkEquipment( final PrintStream report )
	{

		RequestLogger.printLine( "Checking equipment..." );

		DebugDatabase.checkEquipmentMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_HAT ) );
		DebugDatabase.checkEquipmentMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_PANTS ) );
		DebugDatabase.checkEquipmentMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_SHIRT ) );
		DebugDatabase.checkEquipmentMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_WEAPON ) );
		DebugDatabase.checkEquipmentMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_OFFHAND ) );
		DebugDatabase.checkEquipmentMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_ACCESSORY ) );
		DebugDatabase.checkEquipmentMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_CONTAINER ) );
	}

	private static final void checkEquipmentMap( final PrintStream report, ItemMap imap )
	{
		Map<String, String> map = imap.getMap();
		if ( map.size() == 0 )
		{
			return;
		}

		String tag = imap.getTag();
		RequestLogger.printLine( "Checking " + tag + "..." );

		report.println( "" );
		report.println( "# " + tag + " section of equipment.txt" );
		report.println();

		Object[] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			String text = map.get( name );
			DebugDatabase.checkEquipmentDatum( name, text, report );
		}
	}

	private static final void checkEquipmentDatum( final String name, final String text, final PrintStream report )
	{
		String type = DebugDatabase.parseType( text );
		boolean isWeapon = false, isShield = false, hasPower = false;

		if ( type.contains( "weapon" ) )
		{
			isWeapon = true;
		}
		else if ( type.contains( "shield" ) )
		{
			isShield = true;
		}
		else if ( type.contains( "hat" ) || type.contains( "pants" ) || type.contains( "shirt" ) )
		{
			hasPower = true;
		}

		int itemId = ItemDatabase.getItemId( name );
		int power;
		if ( isWeapon || hasPower )
		{
			power = DebugDatabase.parsePower( text );
		}
		else
		{
			// Until KoL puts off-hand and accessory power into the
			// description, use hand-entered "secret" value.
			power = EquipmentDatabase.getPower( itemId );
		}

		// Now check against what we already have
		int oldPower = EquipmentDatabase.getPower( itemId );
		if ( power != oldPower )
		{
			report.println( "# *** " + name + " has power " + oldPower + " but should be " + power + "." );
		}

		String weaponType = isWeapon ? DebugDatabase.parseWeaponType( type ) : "";
		String req = DebugDatabase.parseReq( text, type );

		String oldReq = EquipmentDatabase.getEquipRequirement( itemId );
		if ( !req.equals( oldReq ) )
		{
			report.println( "# *** " + name + " has requirement " + oldReq + " but should be " + req + "." );
		}

		if ( isWeapon )
		{
			int spaceIndex = weaponType.indexOf( " " );
			String oldHanded = EquipmentDatabase.getHands( itemId ) + "-handed";

			if ( spaceIndex != -1 && !weaponType.startsWith( oldHanded ) )
			{
				String handed = weaponType.substring( 0, spaceIndex );
				report.println( "# *** " + name + " is marked as " + oldHanded + " but should be " + handed + "." );
			}
		}

		EquipmentDatabase.writeEquipmentItem( report, name, power, req, weaponType, isWeapon, isShield );
	}

	private static final Pattern POWER_PATTERN = Pattern.compile( "Power: <b>(\\d+)</b>" );
	private static final Pattern DAMAGE_PATTERN_WEAPON = Pattern.compile( "Damage: <b>[\\d]+ - (\\d+)</b>" );
	public static final int parsePower( final String text )
	{
		Matcher matcher = DebugDatabase.POWER_PATTERN.matcher( text );
		// This should match non-weapon power
		if ( matcher.find() )
		{
			return StringUtilities.parseInt( matcher.group( 1 ) );
		}
		// This will match weapon damage and use it to calculate power
		matcher = DebugDatabase.DAMAGE_PATTERN_WEAPON.matcher( text );
		return matcher.find() ? ( StringUtilities.parseInt( matcher.group( 1 ) ) * 5 ) : 0;
	}

	private static final Pattern WEAPON_PATTERN = Pattern.compile( "weapon [(](.*?)[)]" );
	public static final String parseWeaponType( final String text )
	{
		Matcher matcher = DebugDatabase.WEAPON_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : "";
	}

	private static final Pattern REQ_PATTERN = Pattern.compile( "(\\w+) Required: <b>(\\d+)</b>" );
	public static final String parseReq( final String text, final String type )
	{
		Matcher matcher = DebugDatabase.REQ_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			String stat = matcher.group( 1 );
			if ( stat.equals( "Muscle" ) )
			{
				return "Mus: " + matcher.group( 2 );
			}
			if ( stat.equals( "Mysticality" ) )
			{
				return "Mys: " + matcher.group( 2 );
			}
			if ( stat.equals( "Moxie" ) )
			{
				return "Mox: " + matcher.group( 2 );
			}
		}

		if ( type.contains( "weapon" ) )
		{
			if ( type.contains( "ranged" ) )
			{
				return "Mox: 0";
			}
			else if ( type.contains( "utensil" ) ||
				  type.contains( "saucepan" ) ||
				  type.contains( "chefstaff" ) )
			{
				return "Mys: 0";
			}
			else
			{
				return "Mus: 0";
			}
		}

		return "none";
	}

	private static final Pattern FULLNESS_PATTERN = Pattern.compile( "Size: <b>(\\d+)</b>" );

	public static final int parseFullness( final String text )
	{
		Matcher matcher = DebugDatabase.FULLNESS_PATTERN.matcher( text );
		return matcher.find() ? ( StringUtilities.parseInt( matcher.group( 1 ) ) ) : 0;
	}

	private static final Pattern INEBRIETY_PATTERN = Pattern.compile( "Potency: <b>(\\d+)</b>" );

	public static final int parseInebriety( final String text )
	{
		Matcher matcher = DebugDatabase.INEBRIETY_PATTERN.matcher( text );
		return matcher.find() ? ( StringUtilities.parseInt( matcher.group( 1 ) ) ) : 0;
	}

	private static final Pattern TOXICITY_PATTERN = Pattern.compile( "Toxicity: <b>(\\d+)</b>" );

	public static final int parseToxicity( final String text )
	{
		Matcher matcher = DebugDatabase.TOXICITY_PATTERN.matcher( text );
		return matcher.find() ? ( StringUtilities.parseInt( matcher.group( 1 ) ) ) : 0;
	}

	private static final Pattern FAMILIAR_PATTERN = Pattern.compile( "Familiar: <b>(.*?)</b>" );

	public static final String parseFamiliar( final String text )
	{
		Matcher matcher = DebugDatabase.FAMILIAR_PATTERN.matcher( text );
		return matcher.find() ? ( matcher.group( 1 ) ) : "any";
	}

	private static final void checkItemModifiers( final PrintStream report )
	{
		RequestLogger.printLine( "Checking modifiers..." );

		DebugDatabase.checkItemModifierMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_HAT ) );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_PANTS ) );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_SHIRT ) );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_WEAPON ) );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_OFFHAND ) );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_ACCESSORY ) );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_CONTAINER ) );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.findItemMap( KoLConstants.EQUIP_FAMILIAR ) );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.findItemMap( KoLConstants.CONSUME_EAT ), false );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.findItemMap( KoLConstants.CONSUME_DRINK ), false );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.findItemMap( KoLConstants.CONSUME_SPLEEN ), false );
		DebugDatabase.checkItemModifierMap( report, DebugDatabase.findItemMap( -1 ), false );
	}

	private static final void checkItemModifierMap( final PrintStream report, final ItemMap imap )
	{
		DebugDatabase.checkItemModifierMap( report, imap, true );
	}

	private static final void checkItemModifierMap( final PrintStream report, final ItemMap imap, final boolean showAll )
	{
		Map<String, String> map = imap.getMap();
		if ( map.size() == 0 )
		{
			return;
		}

		String tag = imap.getTag();
		RequestLogger.printLine( "Checking " + tag + "..." );

		report.println();
		report.println( "# " + tag + " section of modifiers.txt" );
		report.println();

		Object[] keys = map.keySet().toArray();
		int type = imap.getType();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			String text = map.get( name );
			DebugDatabase.checkItemModifierDatum( name, text, type, report, showAll );
		}
	}

	private static final void checkItemModifierDatum( final String name, final String text, final int type, final PrintStream report, final boolean showAll )
	{
		ModifierList known = new ModifierList();
		ArrayList<String> unknown = new ArrayList<String>();

		// Get the known and unknown modifiers from the item description
		DebugDatabase.parseItemEnchantments( text, known, unknown, type );

		// Compare to what is already registered, logging differences
		// and substituting expressions, as appropriate.
		DebugDatabase.checkModifiers( "Item", name, known, true, report );

		// Print the modifiers in the format modifiers.txt expects.
		if ( showAll || known.size() > 0 || unknown.size() > 0 )
		{
			DebugDatabase.logModifierDatum( "Item", name, known, unknown, report );
		}
	}

	private static final void checkModifiers( final String type, final String name, final ModifierList known, final boolean appendCurrent, final PrintStream report )
	{
		// - Keep modifiers in the same order they are listed in the item description
		// - If a modifier is variable (has an expression), evaluate
		//   the expression and compare to the number in the description
		// - List extra modifiers (Familiar Effect, for example) at end
		//   of parsed modifiers in the order they appear in modifiers.txt

		// Get the existing modifiers for the name
		ModifierList existing = Modifiers.getModifierList( type, name );

		// Look at each modifier in known
		for ( Modifier modifier : known )
		{
			String key = modifier.getName();
			String value = modifier.getValue();

			Modifier current = existing.removeModifier( key );
			if ( current != null )
			{
				String currentValue = current.getValue();
				if ( currentValue == null )
				{
					continue;	// No value
				}

				if ( currentValue.contains( "[" ) )
				{
					int lbracket = currentValue.indexOf( "[" );
					int rbracket = currentValue.indexOf( "]" );

					if ( Modifiers.isNumericModifier( key ) )
					{
						// Evaluate the expression
						String expression = currentValue.substring( lbracket + 1, rbracket );

						// Kludge: KoL no longer takes Reagent Potion duration
						// into account in item descriptions.
						if ( key.equals( "Effect Duration" ) && expression.contains( "R" ) )
						{
							expression = StringUtilities.singleStringReplace( expression, "R", "5" );
						}

						ModifierExpression expr = new ModifierExpression( expression, Modifiers.getLookupName( type, name ) );
						if ( expr.hasErrors() )
						{
							report.println( expr.getExpressionErrors() );
						}
						else
						{
							int descValue = StringUtilities.parseInt( value );
							int modValue = (int)expr.eval();
							if ( descValue != modValue )
							{
								report.println( "# *** modifier " + key + ": " + currentValue + " evaluates to " + modValue + " but description says " + descValue );
							}
						}

						// Keep the expression, regardless
						modifier.setValue( currentValue );
						continue;
					}

					if ( key.equals( "Effect" ) || key.equals( "Rollover Effect" ) )
					{
						// Remove initial effect ID
						String effect = currentValue.substring( 0, lbracket ) + currentValue.substring( rbracket + 1 );

						if ( !value.equals( effect ) )
						{
							// Effect does not match
							report.println( "# *** modifier " + key + ": " + currentValue + " should be " + key + ": " + value );
						}
						else
						{
							modifier.setValue( currentValue );
						}
						continue;
					}
				}

				// If the value is not an expression, it must match exactly
				if ( !value.equals( currentValue ) )
				{
					report.println( "# *** modifier " + key + ": " + currentValue + " should be " + key + ": " + value );
				}
			}
			else if ( value == null )
			{
				report.println( "# *** new enchantment: " + key + " seen" );
			}
			else
			{
				report.println( "# *** new enchantment: " + key + ": " + value + " seen" );
			}
		}

		if ( appendCurrent )
		{
			// Add all modifiers in existing list that were not seen in description to "known"
			known.addAll( existing );
		}
		else
		{
			for ( Modifier modifier : existing )
			{
				String key = modifier.getName();
				String value = modifier.getValue();
				if ( value == null )
				{
					report.println( "# *** bogus enchantment: " + key );
				}
				else
				{
					report.println( "# *** bogus enchantment: " + key + ": " + value );
				}
			}
		}
	}

	private static final void logModifierDatum( final String type, final String name, final ModifierList known, final ArrayList<String> unknown, final PrintStream report )
	{
		for ( int i = 0; i < unknown.size(); ++i )
		{
			Modifiers.writeModifierComment( report, null, name, unknown.get( i ) );
		}

		if ( known.size() == 0 )
		{
			if ( unknown.size() == 0 )
			{
				Modifiers.writeModifierComment( report, null, name );
			}
		}
		else
		{
			Modifiers.writeModifierString( report, type, name, DebugDatabase.createModifierString( known ) );
		}
	}

	private static final String createModifierString( final ModifierList modifiers )
	{
		StringBuilder buffer = new StringBuilder();
		for ( Modifier modifier : modifiers )
		{
			if ( buffer.length() > 0 )
			{
				buffer.append( ", " );
			}
			buffer.append( modifier.toString() );
		}
		return buffer.toString();
	}

	private static final Pattern ITEM_ENCHANTMENT_PATTERN =
		Pattern.compile( "<font color=\"?blue\"?>(?!\\(awesome\\))(.*)(?:<br>)?</font>", Pattern.DOTALL );

 	public static final void parseItemEnchantments( String text, final ModifierList known, final ArrayList<String> unknown, final int type )
	{
		// KoL now includes the enchantments of the effect in the item
		// descriptions. Strip them out.
		int eindex = text.indexOf( "Effect:" );
		if ( eindex != -1 )
		{
			int spanstart = text.indexOf( "<span", eindex );
			int spanend = text.indexOf( "</span>", eindex );
			if ( spanstart != -1 && spanend != -1 )
			{
				String span = text.substring( spanstart, spanend + 7 );
				text = StringUtilities.singleStringDelete( text, span );
			}
		}

		DebugDatabase.parseStandardEnchantments( text, known, unknown, DebugDatabase.ITEM_ENCHANTMENT_PATTERN );

		// Several modifiers can appear outside the "Enchantments"
		// section of the item description.

		// If we extracted Damage Reduction from the enchantments, we
		// included shield DR as well, but for shields that have no
		// enchantments, get DR here.
		if ( !known.containsModifier( "Damage Reduction" ) )
		{
			DebugDatabase.appendModifier( known, Modifiers.parseDamageReduction( text ) );
		}

		DebugDatabase.appendModifier( known, Modifiers.parseSkill( text ) );
		DebugDatabase.appendModifier( known, Modifiers.parseSingleEquip( text ) );
		DebugDatabase.appendModifier( known, Modifiers.parseSoftcoreOnly( text ) );
		DebugDatabase.appendModifier( known, Modifiers.parseLastsOneDay( text ) );
		DebugDatabase.appendModifier( known, Modifiers.parseFreePull( text ) );
		DebugDatabase.appendModifier( known, Modifiers.parseEffect( text ) );
		DebugDatabase.appendModifier( known, Modifiers.parseEffectDuration( text ) );
		DebugDatabase.appendModifier( known, Modifiers.parseSongDuration( text ) );
		DebugDatabase.appendModifier( known, Modifiers.parseDropsItems( text ) );

		if ( type == KoLConstants.EQUIP_FAMILIAR )
		{
			String familiar = DebugDatabase.parseFamiliar( text );
			if ( familiar.equals( "any" ) )
			{
				DebugDatabase.appendModifier( known, "Generic" );
			}
		}
	}

	private static final Pattern RESTORE_RANGE_PATTERN = Pattern.compile( "(\\d+)-(\\d+) (?:HP|MP|Hit Points)" );
	private static final Pattern RESTORE_RANGE2_PATTERN = Pattern.compile( "(\\d+)-(\\d+) HP and (\\d+)-(\\d+) MP" );
	private static final Pattern RESTORE_UPTO_PATTERN = Pattern.compile( "up to (.*?) (?:HP|MP)" );

	public static final void parseRestores( final String name, final String text )
	{
		Matcher enchantMatcher = DebugDatabase.ITEM_ENCHANTMENT_PATTERN.matcher( text );
		if ( !enchantMatcher.find() )
		{
			return;
		}
		String enchant = enchantMatcher.group( 1 );
		if ( !enchant.contains( "Restores" ) && !enchant.contains( "Heals" ) )
		{
			return;
		}
		String hpmin = "0";
		String hpmax = "0";
		String mpmin = "0";
		String mpmax = "0";
		Matcher matcher = DebugDatabase.RESTORE_RANGE_PATTERN.matcher( enchant );
		if ( matcher.find() )
		{
			if ( enchant.contains( "HP" ) || enchant.contains( "Hit Points" ) )
			{
				hpmin = matcher.group( 1 );
				hpmax = matcher.group( 2 );
			}
			if ( enchant.contains( "MP" ) )
			{
				mpmin = matcher.group( 1 );
				mpmax = matcher.group( 2 );
			}
		}
		matcher = DebugDatabase.RESTORE_RANGE2_PATTERN.matcher( enchant );
		if ( matcher.find() )
		{
			hpmin = matcher.group( 1 );
			hpmax = matcher.group( 2 );
			mpmin = matcher.group( 3 );
			mpmax = matcher.group( 4 );
		}
		matcher = DebugDatabase.RESTORE_UPTO_PATTERN.matcher( enchant );
		if ( matcher.find() )
		{
			if ( enchant.contains( "HP" ) )
			{
				hpmin = hpmax = matcher.group( 1 );
			}
			if ( enchant.contains( "MP" ) )
			{
				mpmin = mpmax = matcher.group( 1 );
			}
		}
		if ( enchant.contains( "all" ) )
		{
			if ( enchant.contains( "HP" ) )
			{
				hpmin = hpmax = "[HP]";
			}
			if ( enchant.contains( "MP" ) )
			{
				mpmin = mpmax = "[MP]";
			}
		}
		RestoresDatabase.setValue( name, "item", hpmin, hpmax, mpmin, mpmax, 0, -1, null );
		String printMe = name + "\titem\t" + hpmin + "\t" + hpmax + "\t" + mpmin + "\t" + mpmax + "\t0";
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );
	}

	public static final String parseItemEnchantments( final String text, final ArrayList<String> unknown, final int type )
	{
		ModifierList known = new ModifierList();
		DebugDatabase.parseItemEnchantments( text, known, unknown, type );
		return DebugDatabase.createModifierString( known );
	}

	private static final void parseStandardEnchantments( final String text, final ModifierList known, final ArrayList<String> unknown, final Pattern pattern )
	{
		Matcher matcher = pattern.matcher( text );
		if ( !matcher.find() )
		{
			return;
		}

		StringBuffer enchantments = new StringBuffer( matcher.group(1) );

		StringUtilities.globalStringDelete(
			enchantments,
			"<b>NOTE:</b> Items that reduce the MP cost of skills will not do so by more than 3 points, in total." );
		StringUtilities.globalStringReplace( enchantments, "<br>", "\n" );
		StringUtilities.globalStringReplace( enchantments, "<Br>", "\n" );

		String[] mods = enchantments.toString().split( "\n+" );
		for ( int i = 0; i < mods.length; ++i )
		{
			String enchantment = mods[i].trim();
			if ( enchantment.equals( "" ) )
			{
				continue;
			}

			// Unfortunately, since KoL has removed any indication
			// other than blue font to indicate what is an
			// enchantment, "awesome" as a food quality matches.
			if ( enchantment.equals( "(awesome)" ) )
			{
				continue;
			}

			String mod = Modifiers.parseModifier( enchantment );
			if ( mod != null )
			{
				// Rollover Effect and Rollover Effect Duration come together
				// Modifiers parses the numeric modifier first
				if ( mod.startsWith( "Rollover Effect Duration" ) )
				{
					String effect = Modifiers.parseStringModifier( enchantment );
					if ( effect != null )
					{
						DebugDatabase.appendModifier( known, effect );
					}
				}

				// Damage Reduction can appear in several
				// places. Combine them all.
				else if ( mod.startsWith( "Damage Reduction" ) )
				{
					mod = Modifiers.parseDamageReduction( text );
				}

				DebugDatabase.appendModifier( known, mod );
				continue;
			}

			if ( !unknown.contains( enchantment ) )
			{
				unknown.add( enchantment );
			}
		}
	}

	private static final void appendModifier( final ModifierList known, final String mod )
	{
		if ( mod != null )
		{
			// If the value contains a quoted string, it can contain commas
			if ( mod.contains( "\"" ) || !mod.contains( "," ) )
			{
				known.addToModifier( DebugDatabase.makeModifier( mod ) );
				return;
			}

			// Otherwise, certain modifiers - "All Attributes: +5" - turn into multiple modifiers
			String[] mods = mod.split( "," );
			for ( int i = 0; i < mods.length; ++i )
			{
				known.addToModifier( DebugDatabase.makeModifier( mods[ i ] ) );
			}
		}
	}

	private static final Modifier makeModifier( final String mod )
	{
		int colon = mod.indexOf( ":" );
		String key = colon == -1 ? mod.trim() : mod.substring( 0, colon ).trim();
		String value = colon == -1 ? null : mod.substring( colon + 1 ).trim();
		return new Modifier( key, value );
	}

	// **********************************************************

	// Support for the "checkoutfits" command, which compares KoLmafia's
	// internal outfit data from what can be mined from the outfit
	// description.

	private static final String OUTFIT_HTML = "outfithtml.txt";
	private static final String OUTFIT_DATA = "outfitdata.txt";
	private static final StringArray rawOutfits = new StringArray();
	private static final ItemMap outfits = new ItemMap( "Outfits", 0 );

	public static final void checkOutfits()
	{
		RequestLogger.printLine( "Loading previous data..." );
		DebugDatabase.loadScrapeData( rawOutfits, OUTFIT_HTML );

		RequestLogger.printLine( "Checking internal data..." );

		PrintStream report = DebugDatabase.openReport( OUTFIT_DATA );

		DebugDatabase.outfits.clear();
		DebugDatabase.checkOutfits( report );
		DebugDatabase.checkOutfitModifierMap( report, DebugDatabase.outfits );

		report.close();
	}

	private static final void checkOutfits(final PrintStream report )
	{
		Set<Integer> keys = EquipmentDatabase.normalOutfits.keySet();
		int lastId = 0;

		for ( Integer id : keys )
		{
			if ( id < 1 )
			{
				continue;
			}

			while ( ++lastId < id )
			{
				report.println( lastId );
			}

			DebugDatabase.checkOutfit( id, report );
		}

		DebugDatabase.saveScrapeData( keys.iterator(), rawOutfits, OUTFIT_HTML );
	}

	private static final void checkOutfit( final int outfitId, final PrintStream report )
	{
		SpecialOutfit outfit = EquipmentDatabase.normalOutfits.get( outfitId );
		String name = outfit.getName();
		if ( name == null )
		{
			report.println( outfitId );
			return;
		}

		String rawText = DebugDatabase.rawOutfitDescriptionText( outfitId );

		if ( rawText == null )
		{
			report.println( "# *** " + name + " (" + outfitId + ") has no description." );
			return;
		}

		String text = DebugDatabase.outfitDescriptionText( rawText );
		if ( text == null )
		{
			report.println( "# *** " + name + " (" + outfitId + ") has malformed description text." );
			return;
		}

		String image = outfit.getImage();
		String descImage = DebugDatabase.parseImage( rawText );
		if ( image != null && !image.equals( descImage ) )
		{
			report.println( "# *** " + name + " (" + outfitId + ") has image of " + image + " but should be " + descImage + "." );
		}

		report.println( EquipmentDatabase.outfitString( outfitId, name, descImage ) );

		DebugDatabase.outfits.put( name, text );
	}

	private static final GenericRequest DESC_OUTFIT_REQUEST = new GenericRequest( "desc_outfit.php" );

	public static final String outfitDescriptionText( final int outfitId )
	{
                return DebugDatabase.outfitDescriptionText( DebugDatabase.rawOutfitDescriptionText( outfitId ) );
	}

	public static final String readOutfitDescriptionText( final int outfitId )
	{
		DebugDatabase.DESC_OUTFIT_REQUEST.clearDataFields();
		DebugDatabase.DESC_OUTFIT_REQUEST.addFormField( "whichoutfit", String.valueOf( outfitId ) );
		RequestThread.postRequest( DebugDatabase.DESC_OUTFIT_REQUEST );
		return DebugDatabase.DESC_OUTFIT_REQUEST.responseText;
	}

	public static final String rawOutfitDescriptionText( final int outfitId )
	{
		String previous = DebugDatabase.rawOutfits.get( outfitId );
		if ( previous != null && !previous.equals( "" ) )
		{
			return previous;
		}

		String text = DebugDatabase.readOutfitDescriptionText( outfitId );
		DebugDatabase.rawOutfits.set( outfitId, text );

		return text;
	}

	private static final Pattern OUTFIT_DATA_PATTERN = Pattern.compile( "<div id=\"description\"[^>]*>(.*?)</div>", Pattern.DOTALL );

	public static final String outfitDescriptionText( final String rawText )
	{
		if ( rawText == null )
		{
			return null;
		}

		Matcher matcher = DebugDatabase.OUTFIT_DATA_PATTERN.matcher( rawText );
		if ( !matcher.find() )
		{
			return null;
		}

		return matcher.group( 1 );
	}

	private static final void checkOutfitModifierMap( final PrintStream report, final ItemMap imap )
	{
		Map<String, String> map = imap.getMap();
		if ( map.size() == 0 )
		{
			return;
		}

		String tag = imap.getTag();

		report.println();
		report.println( "# " + tag + " section of modifiers.txt" );
		report.println();

		Object[] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			String text = map.get( name );
			DebugDatabase.checkOutfitModifierDatum( name, text, report );
		}
	}

	private static final void checkOutfitModifierDatum( final String name, final String text, final PrintStream report )
	{
		ModifierList known = new ModifierList();
		ArrayList<String> unknown = new ArrayList<String>();

		// Get the known and unknown modifiers from the outfit description
		DebugDatabase.parseOutfitEnchantments( text, known, unknown );

		// Compare to what is already registered.
		// Log differences and substitute formulas, as appropriate.
		DebugDatabase.checkModifiers( "Outfit", name, known, false, report );

		// Print the modifiers in the format modifiers.txt expects.
		DebugDatabase.logModifierDatum( "Outfit", name, known, unknown, report );
	}

	private static final Pattern OUTFIT_ENCHANTMENT_PATTERN =
		Pattern.compile( "<b><font color=blue>(.*)</font></b>", Pattern.DOTALL );

	public static final void parseOutfitEnchantments( final String text, final ModifierList known, final ArrayList<String> unknown )
	{
		DebugDatabase.parseStandardEnchantments( text, known, unknown, DebugDatabase.OUTFIT_ENCHANTMENT_PATTERN );
	}

	public static final String parseOutfitEnchantments( final String text, final ArrayList<String> unknown )
	{
		ModifierList known = new ModifierList();
		DebugDatabase.parseOutfitEnchantments( text, known, unknown );
		return DebugDatabase.createModifierString( known );
	}

	// **********************************************************

	// Support for the "checkeffects" command, which compares KoLmafia's
	// internal status effect data from what can be mined from the effect
	// description.

	private static final String EFFECT_HTML = "effecthtml.txt";
	private static final String EFFECT_DATA = "effectdata.txt";
	private static final StringArray rawEffects = new StringArray();
	private static final ItemMap effects = new ItemMap( "Status Effects", 0 );

	public static final void checkEffects( final int effectId )
	{
		RequestLogger.printLine( "Loading previous data..." );
		DebugDatabase.loadScrapeData( rawEffects, EFFECT_HTML );

		RequestLogger.printLine( "Checking internal data..." );

		PrintStream report = DebugDatabase.openReport( EFFECT_DATA );

		DebugDatabase.effects.clear();

		if ( effectId == 0 )
		{
			DebugDatabase.checkEffects( report );
		}
		else
		{
			DebugDatabase.checkEffect( effectId, report );
		}

		DebugDatabase.checkEffectModifiers( report );

		report.close();
	}

	private static final void checkEffects(final PrintStream report )
	{
		Set<Integer> keys = EffectDatabase.descriptionIdKeySet();
		Iterator<Integer> it = keys.iterator();

		while ( it.hasNext() )
		{
			int id = it.next().intValue();
			if ( id < 1 )
			{
				continue;
			}

			DebugDatabase.checkEffect( id, report );
		}

		DebugDatabase.saveScrapeData( keys.iterator(), rawEffects, EFFECT_HTML );
	}

	private static final void checkEffect( final int effectId, final PrintStream report )
	{
		String name = EffectDatabase.getEffectName( effectId );
		if ( name == null )
		{
			return;
		}

		String rawText = DebugDatabase.rawEffectDescriptionText( effectId );

		if ( rawText == null )
		{
			report.println( "# *** " + name + " (" + effectId + ") has no description." );
			return;
		}

		String text = DebugDatabase.effectDescriptionText( rawText );
		if ( text == null )
		{
			report.println( "# *** " + name + " (" + effectId + ") has malformed description text." );
			return;
		}

		int id = DebugDatabase.parseEffectId( text );
		if ( id != effectId )
		{
			report.println( "# *** " + name + " (" + effectId + ") should have effectId " + id + "." );
		}

		String descriptionName = DebugDatabase.parseName( text );
		if ( !name.equalsIgnoreCase( StringUtilities.getCanonicalName( descriptionName ) ) )
		{
			report.println( "# *** " + name + " (" + effectId + ") has description of " + descriptionName + "." );
			return;
		}

		String descriptionImage = DebugDatabase.parseImage( rawText );
		if ( !descriptionImage.equals( EffectDatabase.getImageName( id ) ) )
		{
			report.println( "# *** " + name + " (" + effectId + ") has image of " + descriptionImage + "." );
		}

		DebugDatabase.effects.put( name, text );
	}

	// <!-- effectid: 806 -->
	private static final Pattern EFFECTID_PATTERN = Pattern.compile( "<!-- effectid: ([\\d]*) -->" );
	public static final int parseEffectId( final String text )
	{
		Matcher matcher = DebugDatabase.EFFECTID_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return 0;
		}

		return StringUtilities.parseInt( matcher.group( 1 ) );
	}

	// http://images.kingdomofloathing.com/itemimages/hp.gif
	// http://images.kingdomofloathing.com/otherimages/folders/folder22.gif
	// http://images.kingdomofloathing.com/otherimages/sigils/workouttat.gif
	private static final Pattern IMAGE_PATTERN = Pattern.compile( "images.kingdomofloathing.com/(.*?\\.gif)" );

	public static final String parseImage( final String text )
	{
		Matcher matcher = DebugDatabase.IMAGE_PATTERN.matcher( text );
		String path = matcher.find() ? matcher.group( 1 ) : "";
		String prefix1 = "itemimages/";
		String prefix2 = "otherimages/sigils/";
		return  path.startsWith( prefix1 ) ?
			path.substring( prefix1.length() ) :
			path.startsWith( prefix2 ) ?
			path.substring( prefix2.length() ) :
			path;
	}

	// Grants Skill: <a class=hand onClick='javascript:poop("desc_skill.php?whichskill=163&self=true","skill", 350, 300)'><b>Gingerbread Mob Hit</b></a>
	private static final Pattern SKILL_ID_PATTERN = Pattern.compile( "whichskill=(\\d+)" );
	public static final int parseSkillId( final String text )
	{
		Matcher matcher = DebugDatabase.SKILL_ID_PATTERN.matcher( text );
		return matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
	}

	private static final Pattern SKILL_TYPE_PATTERN = Pattern.compile( "<b>Type:</b> (.*?)<br>" );
	public static final String parseSkillType( final String text )
	{
		Matcher matcher = DebugDatabase.SKILL_TYPE_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : "";
	}

	private static final Pattern SKILL_MP_COST_PATTERN = Pattern.compile( "<b>MP Cost:</b> (\\d+)" );
	public static final int parseSkillMPCost( final String text )
	{
		Matcher matcher = DebugDatabase.SKILL_MP_COST_PATTERN.matcher( text );
		return matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
	}

	// Gives Effect: <b><a class=nounder href="desc_effect.php?whicheffect=69dcf3d8fe46c29e7fb6075d06448c95">Your Fifteen Minutes</a>
	private static final Pattern SKILL_EFFECT_PATTERN = Pattern.compile( "Gives Effect: .*?whicheffect=([^\">]*).*?>([^<]*)" );
	public static final String parseSkillEffectName( final String text )
	{
		Matcher matcher = DebugDatabase.SKILL_EFFECT_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 2 ) : "";
	}

	public static final String parseSkillEffectId( final String text )
	{
		Matcher matcher = DebugDatabase.SKILL_EFFECT_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : "";
	}

	private static final Pattern SKILL_EFFECT_DURATION_PATTERN = Pattern.compile( "\\((\\d+) Adventures?\\)" );
	public static final int parseSkillEffectDuration( final String text )
	{
		Matcher matcher = DebugDatabase.SKILL_EFFECT_DURATION_PATTERN.matcher( text );
		return matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
	}

	private static final GenericRequest DESC_SKILL_REQUEST = new GenericRequest( "desc_skill.php" );

	public static final String skillDescriptionText( final int skillId )
	{
		return DebugDatabase.skillDescriptionText( DebugDatabase.rawSkillDescriptionText( skillId ) );
	}

	public static final String readSkillDescriptionText( final int skillId )
	{
		DebugDatabase.DESC_SKILL_REQUEST.clearDataFields();
		DebugDatabase.DESC_SKILL_REQUEST.addFormField( "whichskill", String.valueOf( skillId ) );;
		RequestThread.postRequest( DebugDatabase.DESC_SKILL_REQUEST );
		return DebugDatabase.DESC_SKILL_REQUEST.responseText;
	}

	private static final String rawSkillDescriptionText( final int skillId )
	{
		String previous = DebugDatabase.rawSkills.get( skillId );
		if ( previous != null && !previous.equals( "" ) )
		{
			return previous;
		}

		String text = DebugDatabase.readSkillDescriptionText( skillId );
		DebugDatabase.rawSkills.set( skillId, text );

		return text;
	}

	private static final Pattern SKILL_DATA_PATTERN = Pattern.compile( "<div id=\"description\"[^>]*>(.*?)</div>", Pattern.DOTALL );

	private static final String skillDescriptionText( final String rawText )
	{
		if ( rawText == null )
		{
			return null;
		}

		Matcher matcher = DebugDatabase.SKILL_DATA_PATTERN.matcher( rawText );
		if ( !matcher.find() )
		{
			return null;
		}

		return matcher.group( 1 );
	}

	// href="desc_effect.php?whicheffect=138ba5cbeccb6334a1d473710372e8d6"
	private static final Pattern EFFECT_DESCID_PATTERN = Pattern.compile( "whicheffect=(.*?)\"" );
	public static final String parseEffectDescid( final String text )
	{
		Matcher matcher = DebugDatabase.EFFECT_DESCID_PATTERN.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : "";
	}

	private static final GenericRequest DESC_EFFECT_REQUEST = new GenericRequest( "desc_effect.php" );

	public static final String effectDescriptionText( final int effectId )
	{
                return DebugDatabase.effectDescriptionText( DebugDatabase.rawEffectDescriptionText( effectId ) );
	}

	public static final String readEffectDescriptionText( final String descId )
	{
		DebugDatabase.DESC_EFFECT_REQUEST.clearDataFields();
		DebugDatabase.DESC_EFFECT_REQUEST.addFormField( "whicheffect", descId );
		RequestThread.postRequest( DebugDatabase.DESC_EFFECT_REQUEST );
		return DebugDatabase.DESC_EFFECT_REQUEST.responseText;
	}

	private static final String rawEffectDescriptionText( final int effectId )
	{
		String descId = EffectDatabase.getDescriptionId( effectId );
		if ( descId == null || descId.equals( "" ) )
		{
			return null;
		}

		String previous = DebugDatabase.rawEffects.get( effectId );
		if ( previous != null && !previous.equals( "" ) )
		{
			return previous;
		}

		String text = DebugDatabase.readEffectDescriptionText( descId );
		DebugDatabase.rawEffects.set( effectId, text );

		return text;
	}

	private static final Pattern EFFECT_DATA_PATTERN = Pattern.compile( "<div id=\"description\"[^>]*>(.*?)</div>", Pattern.DOTALL );

	private static final String effectDescriptionText( final String rawText )
	{
		if ( rawText == null )
		{
			return null;
		}

		Matcher matcher = DebugDatabase.EFFECT_DATA_PATTERN.matcher( rawText );
		if ( !matcher.find() )
		{
			return null;
		}

		return matcher.group( 1 );
	}

	private static final void checkEffectModifiers( final PrintStream report )
	{
		RequestLogger.printLine( "Checking modifiers..." );

		DebugDatabase.checkEffectModifierMap( report, DebugDatabase.effects );
	}

	private static final void checkEffectModifierMap( final PrintStream report, final ItemMap imap )
	{
		Map<String, String> map = imap.getMap();
		if ( map.size() == 0 )
		{
			return;
		}

		String tag = imap.getTag();

		report.println();
		report.println( "# " + tag + " section of modifiers.txt" );
		report.println();

		Object[] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			String text = map.get( name );
			DebugDatabase.checkEffectModifierDatum( name, text, report );
		}
	}

	private static final Pattern EFFECT_ENCHANTMENT_PATTERN =
		Pattern.compile( "<font color=blue><b>(.*)</b></font>", Pattern.DOTALL );

	public static final void parseEffectEnchantments( final String text, final ModifierList known, final ArrayList<String> unknown )
	{
		DebugDatabase.parseStandardEnchantments( text, known, unknown, DebugDatabase.EFFECT_ENCHANTMENT_PATTERN );
	}

	public static final String parseEffectEnchantments( final String text, final ArrayList<String> unknown )
	{
		ModifierList known = new ModifierList();
		DebugDatabase.parseEffectEnchantments( text, known, unknown );
		return DebugDatabase.createModifierString( known );
	}

	private static final void checkEffectModifierDatum( final String name, final String text, final PrintStream report )
	{
		ModifierList known = new ModifierList();
		ArrayList<String> unknown = new ArrayList<String>();

		// Get the known and unknown modifiers from the effect description
		DebugDatabase.parseEffectEnchantments( text, known, unknown );

		// Compare to what is already registered.
		// Log differences and substitute formulas, as appropriate.
		DebugDatabase.checkModifiers( "Effect", name, known, true, report );

		// Print the modifiers in the format modifiers.txt expects.
		DebugDatabase.logModifierDatum( "Effect", name, known, unknown, report );
	}


	// **********************************************************

	// Support for the "checkskills" command, which compares KoLmafia's
	// internal skill data with what can be mined from the skill
	// description.

	private static final String SKILL_HTML = "skillhtml.txt";
	private static final String SKILL_DATA = "skilldata.txt";
	private static final StringArray rawSkills = new StringArray();
	private static final ItemMap skills = new ItemMap( "Skills", 0 );

	// **********************************************************

	// Utilities for dealing with KoL description data

	private static final PrintStream openReport( final String fileName )
	{
		return LogStream.openStream( new File( KoLConstants.DATA_LOCATION, fileName ), true );
	}

	private static final void loadScrapeData( final StringArray array, final String fileName )
	{
		try
		{
			File saveData = new File( KoLConstants.DATA_LOCATION, fileName );
			if ( !saveData.exists() )
			{
				return;
			}

			String currentLine;
			StringBuilder currentHTML = new StringBuilder();
			BufferedReader reader = FileUtilities.getReader( saveData );
			int lines = 0;

			while ( ( currentLine = reader.readLine() ) != null && !currentLine.equals( "" ) )
			{
				lines += 1;
				currentHTML.setLength( 0 );
				int currentId = StringUtilities.parseInt( currentLine );

				do
				{
					currentLine = reader.readLine();
					currentHTML.append( currentLine );
					currentHTML.append( KoLConstants.LINE_BREAK );
				}
				while ( !currentLine.equals( "</html>" ) );

				if ( array.get( currentId ).equals( "" ) )
				{
					array.set( currentId, currentHTML.toString() );
				}
				reader.readLine();
			}

			reader.close();
		}
		catch ( Exception e )
		{
			// This shouldn't happen, but if it does, go ahead and
			// fall through.  You're done parsing.
		}
	}

	private static final void saveScrapeData( final Iterator<Integer> it, final StringArray array, final String fileName )
	{
		File file = new File( KoLConstants.DATA_LOCATION, fileName );
		PrintStream livedata = LogStream.openStream( file, true );

		while ( it.hasNext() )
		{
			int id = it.next().intValue();
			if ( id < 1 )
			{
				continue;
			}

			String description = array.get( id );
			if ( description != null && !description.equals( "" ) )
			{
				livedata.println( id );
				livedata.println( description );
			}
		}

		livedata.close();
	}

	// **********************************************************

	public static final void checkPlurals( final String parameters )
	{
		RequestLogger.printLine( "Checking plurals..." );
		PrintStream report = LogStream.openStream( new File( KoLConstants.DATA_LOCATION, "plurals.txt" ), true );
		if (!parameters.contains("-")) {
			int itemId = StringUtilities.parseInt(parameters);
			if (itemId == 0)
			{
				for (Integer id : ItemDatabase.descriptionIdKeySet())
				{
					if (id < 0)
					{
						continue;
					}
					while (++itemId < id)
					{
						report.println(itemId);
					}
					DebugDatabase.checkPlural(id, report);
				}
			}
			else
			{
				DebugDatabase.checkPlural(itemId, report);
			}
		}
		else
		{
			String[] points = parameters.split("-");
			//parseInt will return 0 for null input so bother to check split for validity
			int start = StringUtilities.parseInt(points[0]);
			int end = StringUtilities.parseInt(points[1]);
			start = Math.max(0, start);
			end = Math.min(end, ItemDatabase.maxItemId());
			for (int i = start; i < end; i++)
			{
				DebugDatabase.checkPlural(i, report);
			}
		}
		report.close();
	}

	private static final void checkPlural( final int itemId, final PrintStream report )
	{
		Integer id = IntegerPool.get( itemId );

		String name = ItemDatabase.getItemDataName( id );
		if ( name == null )
		{
			report.println( itemId );
			return;
		}
		String plural = ItemDatabase.getPluralById( itemId );
		if ( plural == null )
		{
			// If we don't have a plural, the default is to simply
			// add an "s".
			plural = "";
		}
		else if ( plural.equals( name + "s" ) )
		{
			// If we do explicitly list a plural which is the
			// default, suppress it.
			plural = "";
		}

		String displayPlural = StringUtilities.getDisplayName( plural.equals( "" ) ? name + "s" : plural );

		// Don't bother checking quest items
		String access = ItemDatabase.getAccessById( id );
		boolean logit = false;
		if ( access != null && !access.contains( ItemDatabase.QUEST_FLAG ) )
		{
			String otherPlural;
			boolean checkApi = InventoryManager.getCount( itemId ) > 1;
			if ( checkApi )
			{
				otherPlural = DebugDatabase.readApiPlural( itemId );
				if ( otherPlural.equals( "" ) )
				{
					otherPlural = name + "s";
				}
				String test = plural;
				if ( test.equals( "" ) )
				{
					test = name + "s";
				}
				if ( !test.equals( otherPlural ) )
				{
					RequestLogger.printLine( "*** " + name + ": KoLmafia plural = \"" + displayPlural + "\", KoL plural = \"" + otherPlural + "\"" );
					plural = otherPlural;
				}
			}
			else
			{
				String wikiData = DebugDatabase.readWikiData( name );
				Matcher matcher = DebugDatabase.WIKI_PLURAL_PATTERN.matcher( wikiData );
				otherPlural = matcher.find() ? matcher.group( 1 ) : "";
				otherPlural = CharacterEntities.unescape( otherPlural);
				if ( otherPlural.equals( "" ) )
				{
					// The Wiki does not list a plural. If ours is
					// non-default, log discrepancy and keep ours.
					if ( !plural.equals( "" ) )
					{
						logit = true;
					}
				}
				else if ( otherPlural.equalsIgnoreCase( "I am a Fish" ) )
				{
					RequestLogger.printLine( "*** " + name + " has bogus Wiki plural: \"" + otherPlural + "\". Ignoring." );
				}
				else if ( plural.equals( "" ) )
				{
					// The Wiki has a plural, but ours is the
					// default. If the Wiki's is NOT the default,
					// log it and tentatively accept it
					if ( !displayPlural.equals( otherPlural ) )
					{
						logit = true;
						plural = "*** " + otherPlural;
					}
				}
				else
				{
					// Both we and the Wiki have plurals. If they
					// do not agree, log it, but keep ours.
					if ( !displayPlural.equals( otherPlural ) )
					{
						logit = true;
					}
				}

				if ( logit )
				{
					RequestLogger.printLine( "*** " + name + ": KoLmafia plural = \"" + displayPlural + "\", Wiki plural = \"" + otherPlural + "\"" );
				}
			}
		}

		if ( plural.equals( "" ) )
		{
			report.println( itemId + "\t" + name );
		}
		else
		{
			report.println( itemId + "\t" + name + "\t" + plural );
		}
	}

	// **********************************************************

	public static final void checkPowers( final String option )
	{
		// We can check the power of any items in inventory or closet.
		// We'll assume that any item with a non-zero power is correct.
		// Off-hand items and accessories don't have visible power and
		// might be 0 in the database. Look them up and fix them.

		if ( StringUtilities.isNumeric( option ) )
		{
			DebugDatabase.checkPower( StringUtilities.parseInt( option ), true );
			return;
		}

		TreeSet<AdventureResult> items = new TreeSet<AdventureResult>();
		items.addAll( KoLConstants.inventory );
		items.addAll( KoLConstants.closet );
		items.addAll( EquipmentManager.allEquipmentAsList() );
		// items.addAll( KoLConstants.storage );

		if ( KoLCharacter.hasDisplayCase() && !DisplayCaseManager.collectionRetrieved )
		{
			RequestThread.postRequest( new DisplayCaseRequest() );
		}
		items.addAll( KoLConstants.collection );

		DebugDatabase.checkPowers( items, option.equals( "all" ) );
	}

	private static final void checkPowers( final Collection<AdventureResult> items, final boolean force  )
	{
		for ( AdventureResult item : items )
		{
			int itemId = item.getItemId();
			int type = ItemDatabase.getConsumptionType( itemId );
			if ( type == KoLConstants.EQUIP_OFFHAND ||
			     type == KoLConstants.EQUIP_ACCESSORY ||
			     type == KoLConstants.EQUIP_CONTAINER )
			{
				DebugDatabase.checkPower( itemId, force );
			}
		}
	}

	private static final void checkPower( final int itemId, final boolean force  )
	{
		int current = EquipmentDatabase.getPower( itemId );
		if ( !force && current != 0 )
		{
			return;
		}

		// Look it up and register it anew
		ApiRequest request = new ApiRequest( "item", itemId );
		RequestThread.postRequest( request );

		JSONObject JSON = request.JSON;
		if ( JSON == null )
		{
			AdventureResult item = ItemPool.get( itemId );
			String location = 
				KoLConstants.inventory.contains( item ) ? "inventory" :
				KoLConstants.closet.contains( item ) ? "closet" :
				KoLConstants.storage.contains( item ) ? "storage" :
				KoLConstants.collection.contains( item ) ? "display case" :
				"nowhere";
			KoLmafia.updateDisplay( "Could not look up item " + item + " from " + location );
			return;
		}

		try
		{
			int power = JSON.getInt( "power" );

			// Yes, some items really are power 0
			if ( power == 0 || power == current )
			{
				return;
			}

			String name = JSON.getString( "name" );
			String descid = JSON.getString( "descid" );
			RequestLogger.printLine( "Item \"" + name +"\" power incorrect: " + current + " should be " + power );
			ItemDatabase.registerItem( itemId, name, descid, null, power, false );
		}
		catch ( JSONException e )
		{
			KoLmafia.updateDisplay( "Error parsing JSON string!" );
			StaticEntity.printStackTrace( e );
		}
	}

	// **********************************************************

	public static final void checkShields()
	{
		DebugDatabase.checkShields( KoLConstants.inventory );
		DebugDatabase.checkShields( KoLConstants.closet );
		DebugDatabase.checkShields( KoLConstants.storage );
	}

	public static final void checkShields( final Collection items )
	{
		Iterator it = items.iterator();
		while ( it.hasNext() )
		{
			AdventureResult item = (AdventureResult)it.next();
			int itemId = item.getItemId();
			if ( !EquipmentDatabase.getItemType( itemId ).equals( "shield" ) )
			{
				continue;
			}

			ApiRequest request = new ApiRequest( "item", itemId );
			RequestThread.postRequest( request );

			JSONObject JSON = request.JSON;
			if ( JSON == null )
			{
				continue;
			}

			try
			{
				int oldPower = EquipmentDatabase.getPower( itemId );
				int correctPower = JSON.getInt( "power" );
				if ( oldPower == correctPower )
				{
					continue;
				}

				String name = JSON.getString( "name" );
				String descid = JSON.getString( "descid" );

				RequestLogger.printLine( "Shield \"" + name +"\" power incorrect: " + oldPower + " should be " + correctPower );
				ItemDatabase.registerItem( itemId, name, descid, null, correctPower, false );
			}
			catch ( JSONException e )
			{
				KoLmafia.updateDisplay( "Error parsing JSON string!" );
				StaticEntity.printStackTrace( e );
			}
		}
	}

	// **********************************************************

	public static final void checkPotions()
	{
		RequestLogger.printLine( "Loading previous data..." );
		DebugDatabase.loadScrapeData( rawItems, ITEM_HTML );

		Set keys = ItemDatabase.descriptionIdKeySet();
		Iterator it = keys.iterator();

		while ( it.hasNext() )
		{
			Integer id = ( (Integer) it.next() );
			int itemId = id.intValue();
			if ( itemId < 1 || !ItemDatabase.isUsable( itemId ) || ItemDatabase.isEquipment( itemId ) )
			{
				continue;
			}

			// Potions grant an effect. Check for a new effect.
			String itemName = ItemDatabase.getItemDataName( id );
			String effectName = Modifiers.getStringModifier( "Item", itemId, "Effect" );
			if ( !effectName.equals( "" ) && EffectDatabase.getEffectId( effectName, true ) == -1 )
			{
				String rawText = DebugDatabase.rawItemDescriptionText( itemId );
				String effectDescid = DebugDatabase.parseEffectDescid( rawText );
				EffectDatabase.registerEffect( effectName, effectDescid, "use 1 " + itemName );
			}
		}
	}

	// **********************************************************

	private static final String CONSUMABLE_DATA = "consumables.txt";

	public static final void checkConsumables()
	{
		RequestLogger.printLine( "Loading previous data..." );
		DebugDatabase.loadScrapeData( rawItems, ITEM_HTML );
		RequestLogger.printLine( "Checking internal data..." );
		PrintStream report = DebugDatabase.openReport( CONSUMABLE_DATA );
		DebugDatabase.checkConsumables( report );
		report.close();
	}

	private static final void checkConsumables( final PrintStream report )
	{
		DebugDatabase.checkConsumables( report, ConsumablesDatabase.fullnessByName, "fullness" );
		DebugDatabase.checkConsumables( report, ConsumablesDatabase.inebrietyByName, "inebriety" );
		DebugDatabase.checkConsumables( report, ConsumablesDatabase.spleenHitByName, "spleenhit" );
	}

	private static final void checkConsumables( final PrintStream report, final Map map, final String tag )
	{
		if ( map.size() == 0 )
		{
			return;
		}

		report.println( "" );
		report.println( "# Consumption data in " + tag + ".txt" );
		report.println( "#" );

		Object[] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			int size = ((Integer) map.get( name ) ).intValue();
			DebugDatabase.checkConsumable( report, name, size );
		}
	}

	private static final void checkConsumable( final PrintStream report, final String name, final int size )
	{
		int itemId = ItemDatabase.getItemId( name );
		// It is valid for items to have no itemId: sushi, Cafe offerings, and so on
		String text = itemId == -1 ? "" : DebugDatabase.itemDescriptionText( itemId, false );
		if ( text == null )
		{
			return;
		}

		int level = ConsumablesDatabase.getLevelReqByName( name ).intValue();
		String adv = ConsumablesDatabase.getAdvRangeByName( name );
		String quality = ( itemId == -1 ) ? ConsumablesDatabase.getQuality( name ) : DebugDatabase.parseQuality( text );
		String mus = ConsumablesDatabase.getMuscleByName( name );
		String mys = ConsumablesDatabase.getMysticalityByName( name );
		String mox = ConsumablesDatabase.getMoxieByName( name );
		String notes = ConsumablesDatabase.getNotes( name );

		ConsumablesDatabase.writeConsumable( report, name, size, level, quality, adv, mus, mys, mox, notes );
	}

	// Type: <b>food <font color=#999999>(crappy)</font></b>
	// Type: <b>food (decent)</b>
	// Type: <b>booze <font color=green>(good)</font></b>
	// Type: <b>food <font color=blue>(awesome)</font></b>
	// Type: <b>food <font color=blueviolet>(EPIC)</font></b>

	private static final Pattern QUALITY_PATTERN = Pattern.compile( "Type: <b>.*?\\((.*?)\\).*?</b>" );
	public static final String parseQuality( final String text )
	{
		Matcher matcher = DebugDatabase.QUALITY_PATTERN.matcher( text );
		return ConsumablesDatabase.qualityValue( matcher.find() ? matcher.group( 1 ) : "" );
	}

	// **********************************************************

	// <tr class="frow " data-stats="1" data-meat="1" data-items="1"><td valign=center><input type=radio name=newfam value=192></td><td valign=center><img src="/images/itemimages/goldmonkey.gif" class="hand fam" onClick='fam(192)'></td><td valign=top style='padding-top: .45em;'><b>Ignominious Uncguary</b>, the 20-pound Golden Monkey (400 exp, 6,107 kills) <font size="1"><br />&nbsp;&nbsp;&nbsp;&nbsp;<a class="fave" href="familiar.php?group=0&action=fave&famid=192&pwd">[unfavorite]</a>&nbsp;&nbsp;<a class="fave" href="familiar.php?&action=newfam&newfam=192&pwd">[take with you]</a></font></td><td valign=center nowrap><center><b>(</b><img src="/images/itemimages/goldbanana.gif" class=hand onClick='descitem(986943479)' align=middle><b>)</b><br><font size=1><a href='familiar.php?pwd&action=unequip&famid=192'>[unequip]</a></font></center></td></tr>

	private static final Pattern FAMILIAR_ROW_PATTERN = Pattern.compile( "<tr class=\"frow ?\"([^>]*)>.*?onClick='fam\\(([\\d]+)\\)'.*?</tr>" );

	public static final void checkFamiliarsInTerrarium( boolean showVariable )
	{
		FamiliarRequest request = new FamiliarRequest();
		RequestThread.postRequest( request );

		TreeMap<Integer,String> map = new TreeMap<Integer,String>();

		Matcher matcher = DebugDatabase.FAMILIAR_ROW_PATTERN.matcher( request.responseText );
		while ( matcher.find() )
		{
			int id = StringUtilities.parseInt( matcher.group( 2 ) );
			String powers = matcher.group( 1 ).trim();
			map.put( id, powers );
		}

		for ( Entry<Integer,String> entry : map.entrySet() )
		{
			int id = entry.getKey().intValue();
			String powers = entry.getValue();
			DebugDatabase.checkTerrariumFamiliar( id, powers, showVariable );
		}
	}

	private static final void checkTerrariumFamiliar( int id, String powers, boolean showVariable )
	{
		// KoL familiar categories
		boolean dataAttack = powers.contains( "data-attack" );
		boolean dataDefense = powers.contains( "data-defense" );
		boolean dataHPRestore = powers.contains( "data-hp_restore" );
		boolean dataItemDrops = powers.contains( "data-itemdrops" );
		boolean dataItems = powers.contains( "data-items" );
		boolean dataMeat = powers.contains( "data-meat" );
		boolean dataMPRestore = powers.contains( "data-mp_restore" );
		boolean dataOther = powers.contains( "data-other" );
		boolean dataStats = powers.contains( "data-stats" );
		boolean dataUnderwater = powers.contains( "data-underwater" );

		// KoLmafia familiar categories
		boolean block = FamiliarDatabase.isBlockType( id );
		boolean combat0 = FamiliarDatabase.isCombat0Type( id );
		boolean combat1 = FamiliarDatabase.isCombat1Type( id );
		boolean delevel = FamiliarDatabase.isDelevelType( id );
		boolean drop = FamiliarDatabase.isDropType( id );
		boolean hp0 = FamiliarDatabase.isHp0Type( id );
		boolean hp1 = FamiliarDatabase.isHp1Type( id );
		boolean item0 = FamiliarDatabase.isFairyType( id );
		boolean meat0 = FamiliarDatabase.isMeatDropType( id );
		boolean meat1 = FamiliarDatabase.isMeat1Type( id );
		boolean mp0 = FamiliarDatabase.isMp0Type( id );
		boolean mp1 = FamiliarDatabase.isMp1Type( id );
		boolean none = FamiliarDatabase.isNoneType( id );
		boolean other0 = FamiliarDatabase.isOther0Type( id );
		boolean other1 = FamiliarDatabase.isOther1Type( id );
		boolean passive = FamiliarDatabase.isPassiveType( id );
		boolean stat0 = FamiliarDatabase.isVolleyType( id );
		boolean stat1 = FamiliarDatabase.isSombreroType( id );
		boolean stat2 = FamiliarDatabase.isStat2Type( id );
		boolean stat3 = FamiliarDatabase.isStat3Type( id );
		boolean underwater = FamiliarDatabase.isUnderwaterType( id );
		boolean variable = FamiliarDatabase.isVariableType( id );

		String name = FamiliarDatabase.getFamiliarName( id );
		String prefix = "*** familiar #" + id + " (" + name + "): KoL says ";

		// Check KoL categories
		if ( dataAttack && !( combat0 || combat1 ) )
		{
			String message =
				!variable ? "'attack' but we have neither 'combat0' nor 'combat1'" :
				showVariable ? "'attack' but we say 'variable'" :
				null;
			if ( message != null )
			{
				RequestLogger.printLine( prefix + message );
			}
		}
		if ( dataDefense && !( block || delevel || other0 ) )
		{
			String message =
				!variable ? "'defense' but we have none of 'block', 'delevel', or 'other0'" :
				showVariable ? "'defense' but we say 'variable'" :
				null;
			if ( message != null )
			{
				RequestLogger.printLine( prefix + message );
			}
		}
		if ( dataHPRestore && !( hp0 || hp1 ) )
		{
			String message =
				!variable ? "'hp_restore' but we have neither 'hp0' nor 'hp1'" :
				showVariable ? "'hp_restore' but we say 'variable'" :
				null;
			if ( message != null )
			{
				RequestLogger.printLine( prefix + message );
			}
		}
		if ( dataItemDrops && !item0 )
		{
			String message =
				!variable ? "'itemdrops' but we do not have 'item0'" :
				showVariable ? "'itemdrops' but we say 'variable'" :
				null;
			if ( message != null )
			{
				RequestLogger.printLine( prefix + message );
			}
		}
		if ( dataItems && !drop )
		{
			String message =
				!variable ? "'item' but we do not have 'drop'" :
				showVariable ? "'item' but we say 'variable'" :
				null;
			if ( message != null )
			{
				RequestLogger.printLine( prefix + message );
			}
		}
		if ( dataMeat && !( meat0 || meat1 ) )
		{
			String message =
				!variable ? "'meat' but we have neither 'meat0' nor 'meat1'" :
				showVariable ? "'meat' but we say 'variable'" :
				null;
			if ( message != null )
			{
				RequestLogger.printLine( prefix + message );
			}
		}
		if ( dataMPRestore && !( mp0 || mp1 ) )
		{
			String message =
				!variable ? "'mp_restore' but we have neither 'mp0' nor 'mp1'" :
				showVariable ? "'mp_restore' but we say 'variable'" :
				null;
			if ( message != null )
			{
				RequestLogger.printLine( prefix + message );
			}
		}
		if ( dataOther && !( none || other0 || other1 || passive ) )
		{
			String message =
				!variable ? "'other' but we have none of 'none', 'other0', 'other1',or 'passive'" :
				showVariable ? "'other' but we say 'variable'" :
				null;
			if ( message != null )
			{
				RequestLogger.printLine( prefix + message );
			}
		}
		if ( dataStats && !( stat0 || stat1 || stat2 || stat3 || passive ) )
		{
			String message =
				!variable ? "'stats' but we have none of 'stat0', 'stat1', 'stat2', or 'stat3'" :
				showVariable ? "'stats' but we say 'variable'" :
				null;
			if ( message != null )
			{
				RequestLogger.printLine( prefix + message );
			}
		}
		if ( dataUnderwater && !underwater )
		{
			String message =
				!variable ? "'underwater' but we do not have 'underwater'" :
				showVariable ? "'underwater' but we say 'variable'" :
				null;
			if ( message != null )
			{
				RequestLogger.printLine( prefix + message );
			}
		}

		// Check KoLmafia categories
		if ( block && !dataDefense )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'block' but KoL does not say 'defense'" );
		}
		if ( combat0 && !dataAttack )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'combat0' but KoL does not say 'attack'" );
		}
		if ( combat1 && !dataAttack )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'combat1' but KoL does not say 'attack'" );
		}
		if ( delevel && !dataDefense )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'delevel' but KoL does not say 'defense'" );
		}
		if ( drop && !dataItems )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'drop' but KoL does not say 'items'" );
		}
		if ( hp0 && !dataHPRestore )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'hp0' but KoL does not say 'hp_restore'" );
		}
		if ( hp1 && !dataHPRestore )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'hp1' but KoL does not say 'hp_restore'" );
		}
		if ( item0 && !dataItemDrops )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'item0' but KoL does not say 'itemdrops'" );
		}
		if ( meat0 && !dataMeat )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'meat0' but KoL does not say 'meat'" );
		}
		if ( meat1 && !dataMeat )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'meat1' but KoL does not say 'meat'" );
		}
		if ( mp0 && !dataMPRestore )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'mp0' but KoL does not say 'mp_restore'" );
		}
		if ( mp1 && !dataMPRestore )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'mp1' but KoL does not say 'mp_restore'" );
		}
		if ( none && !dataOther )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'none' but KoL does not say 'other'" );
		}
		if ( other0 && !( dataOther || dataDefense ) )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'other0' but KoL does not say 'other' or 'defense'" );
		}
		if ( other1 && !dataOther )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'other1' but KoL does not say 'other'" );
		}
		if ( passive && !dataOther )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'passive' but KoL does not say 'other'" );
		}
		if ( stat0 && !dataStats )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'stat0' but KoL does not say 'stats'" );
		}
		if ( stat1 && !dataStats )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'stat1' but KoL does not say 'stats'" );
		}
		if ( stat2 && !dataStats )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'stat2' but KoL does not say 'stats'" );
		}
		if ( stat3 && !dataStats )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'stat3' but KoL does not say 'stats'" );
		}
		if ( underwater && !dataUnderwater )
		{
			RequestLogger.printLine( "*** familiar #" + id + " (" + name + "): KoLmafia has 'underwater' but KoL does not say 'underwater'" );
		}
	}

	public static final void checkFamiliarImages()
	{
		// Get familiar images from the familiar description
		boolean changed = false;
		for ( int i = 1; i <= FamiliarDatabase.maxFamiliarId; ++i )
		{
			changed |= DebugDatabase.checkFamiliarImage( i );
		}

		// FamiliarDatabase.saveDataOverride();
	}

	private static final Pattern FAMILIAR_IMAGE_PATTERN = Pattern.compile( "images\\.kingdomofloathing\\.com/itemimages/(.*?\\.gif)" );
	private static final boolean checkFamiliarImage( final int id )
	{
		String file = "desc_familiar.php?which=" + String.valueOf( id );
		GenericRequest request = new GenericRequest( file );
		RequestThread.postRequest( request );
		String text = request.responseText;
		if ( text == null )
		{
			RequestLogger.printLine( "*** no description for familiar #" + id );
			return false;
		}

		boolean changed = false;
		Matcher matcher = FAMILIAR_IMAGE_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			String oldImage = FamiliarDatabase.getFamiliarImageLocation( id );
			String newImage = matcher.group( 1 );
			if ( !oldImage.equals( newImage ) )
			{
				RequestLogger.printLine( "*** familiar #" + id + " has image " + oldImage + " but KoL says it is " + newImage );
				FamiliarDatabase.setFamiliarImageLocation( id, newImage );
				changed = true;
			}
		}

		return changed;
	}

	// **********************************************************

	public static final void checkConsumptionData()
	{
		RequestLogger.printLine( "Checking consumption data..." );

		PrintStream writer = LogStream.openStream( new File( KoLConstants.DATA_LOCATION, "consumption.txt" ), true );

		DebugDatabase.checkEpicure( writer );
		DebugDatabase.checkMixologist( writer );

		writer.close();
	}

	private static final String EPICURE = "http://kol.coldfront.net/tools/epicure/export_data.php";

	private static final void checkEpicure( final PrintStream writer )
	{
		RequestLogger.printLine( "Connecting to Well-Tempered Epicure..." );
		Document doc = getXMLDocument( EPICURE );

		if ( doc == null )
		{
			return;
		}

		writer.println( KoLConstants.FULLNESS_VERSION );
		writer.println( "# Data provided courtesy of the Garden of Earthly Delights" );
		writer.println( "# The Well-Tempered Epicure: " + EPICURE );
		writer.println();
		writer.println( "# Food" + "\t" + "Fullness" + "\t" + "Level Req" + "\t" + "Adv" + "\t" + "Musc" + "\t" + "Myst" + "\t" + "Moxie" );
		writer.println();

		NodeList elements = doc.getElementsByTagName( "iteminfo" );

		for ( int i = 0; i < elements.getLength(); i++ )
		{
			Node element = elements.item( i );
			checkFood( element, writer );
		}
	}

	private static final void checkFood( final Node element, final PrintStream writer )
	{
		String name= "";
		String advs= "";
		String musc= "";
		String myst= "";
		String mox= "";
		String fullness= "";
		String level= "";

		for ( Node node = element.getFirstChild(); node != null; node = node.getNextSibling() )
		{
			String tag = node.getNodeName();
			Node child = node.getFirstChild();

			if ( tag.equals( "title" ) )
			{
				name = DebugDatabase.getStringValue( child );
			}
			else if ( tag.equals( "advs" ) )
			{
				advs = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "musc" ) )
			{
				musc = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "myst" ) )
			{
				myst = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "mox" ) )
			{
				mox = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "fullness" ) )
			{
				fullness = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "level" ) )
			{
				level = DebugDatabase.getNumericValue( child );
			}
		}

		String line = name + "\t" + fullness + "\t" + level + "\t" + advs + "\t" + musc + "\t" + myst + "\t" + mox;

		int present = ConsumablesDatabase.getFullness( name );

		if ( present == 0 )
		{
			writer.println( "# Unknown food:" );
			writer.print( "# " );
		}
		else
		{
			String note = ConsumablesDatabase.getNotes( name );
			if ( note != null )
			{
				line = line + "\t" + note;
			}
		}

		writer.println( line );
	}

	private static final String MIXOLOGIST = "http://kol.coldfront.net/tools/mixology/export_data.php";

	private static final void checkMixologist( final PrintStream writer )
	{
		RequestLogger.printLine( "Connecting to Well-Tempered Mixologist..." );
		Document doc = getXMLDocument( MIXOLOGIST );

		if ( doc == null )
		{
			return;
		}

		writer.println( KoLConstants.INEBRIETY_VERSION );
		writer.println( "# Data provided courtesy of the Garden of Earthly Delights" );
		writer.println( "# The Well-Tempered Mixologist: " + MIXOLOGIST );
		writer.println();
		writer.println( "# Drink" + "\t" + "Inebriety" + "\t" + "Level Req" + "\t" + "Adv" + "\t" + "Musc" + "\t" + "Myst" + "\t" + "Moxie" );
		writer.println();

		NodeList elements = doc.getElementsByTagName( "iteminfo" );

		for ( int i = 0; i < elements.getLength(); i++ )
		{
			Node element = elements.item( i );
			checkBooze( element, writer );
		}
	}

	private static final void checkBooze( final Node element, final PrintStream writer )
	{
		String name= "";
		String advs= "";
		String musc= "";
		String myst= "";
		String mox= "";
		String drunk= "";
		String level= "";

		for ( Node node = element.getFirstChild(); node != null; node = node.getNextSibling() )
		{
			String tag = node.getNodeName();
			Node child = node.getFirstChild();

			if ( tag.equals( "title" ) )
			{
				name = DebugDatabase.getStringValue( child );
			}
			else if ( tag.equals( "advs" ) )
			{
				advs = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "musc" ) )
			{
				musc = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "myst" ) )
			{
				myst = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "mox" ) )
			{
				mox = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "drunk" ) )
			{
				drunk = DebugDatabase.getNumericValue( child );
			}
			else if ( tag.equals( "level" ) )
			{
				level = DebugDatabase.getNumericValue( child );
			}
		}


		String line = name + "\t" + drunk + "\t" + level + "\t" + advs + "\t" + musc + "\t" + myst + "\t" + mox;

		int present = ConsumablesDatabase.getInebriety( name );

		if ( present == 0 )
		{
			writer.println( "# Unknown booze:" );
			writer.print( "# " );
		}
		else
		{
			String note = ConsumablesDatabase.getNotes( name );
			if ( note != null )
			{
				line = line + "\t" + note;
			}
		}

		writer.println( line );
	}

	private static final String getStringValue( final Node node )
	{
		return StringUtilities.getEntityEncode( node.getNodeValue().trim() );
	}

	private static final String getNumericValue( final Node node )
	{
		String value = node.getNodeValue().trim();

		int sign = value.startsWith( "-" ) ? -1 : 1;
		if ( sign == -1 )
		{
			value = value.substring( 1 );
		}

		int dash = value.indexOf( "-" );
		if ( dash == -1 )
		{
			return String.valueOf( sign * StringUtilities.parseInt( value ) );
		}

		int first = sign * StringUtilities.parseInt( value.substring( 0, dash) );
		int second = StringUtilities.parseInt( value.substring( dash + 1 ) );
		return String.valueOf( first ) + "-" + String.valueOf( second );
	}

	private static final Document getXMLDocument( final String uri )
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try
		{
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse( uri );
		}
		catch ( Exception e )
		{
			RequestLogger.printLine( "Failed to parse XML document from \"" + uri + "\": " + e.getMessage() );
		}

		return null;
	}

	public static final void checkPulverizationData()
	{
		RequestLogger.printLine( "Checking pulverization data..." );

		PrintStream writer = LogStream.openStream( new File( KoLConstants.DATA_LOCATION, "pulvereport.txt" ), true );

		DebugDatabase.checkAnvil( writer );

		writer.close();
	}

	private static final String ANVIL = "http://kol.coldfront.net/tools/anvil/export_data.php";

	private static final void checkAnvil( final PrintStream writer )
	{
		RequestLogger.printLine( "Connecting to Well-Tempered Anvil..." );
		Document doc = getXMLDocument( ANVIL );

		if ( doc == null )
		{
			return;
		}

		writer.println( KoLConstants.PULVERIZE_VERSION );
		writer.println( "# Data provided courtesy of the Garden of Earthly Delights" );
		writer.println( "# The Well-Tempered Anvil: " + ANVIL );
		writer.println();

		NodeList elements = doc.getElementsByTagName( "iteminfo" );

		HashSet<Integer> seen = new HashSet<Integer>();
		for ( int i = 0; i < elements.getLength(); i++ )
		{
			Node element = elements.item( i );
			checkPulverize( element, writer, seen );
		}

		for ( int id = 1; id <= ItemDatabase.maxItemId(); ++id )
		{
			int pulver = EquipmentDatabase.getPulverization( id );
			if ( pulver != -1 && !seen.contains( IntegerPool.get( id ) ) )
			{
				String name = ItemDatabase.getItemName( id );
				writer.println( name + ": not listed in anvil" );
			}
		}
	}

	private static final void checkPulverize( final Node element, final PrintStream writer,
		HashSet<Integer> seen )
	{
		String name= "";
		int id = -1;
		int yield = -1;
		boolean cansmash = false;
		boolean confirmed = false;
		boolean twinkly = false;
		boolean hot = false;
		boolean cold = false;
		boolean stench = false;
		boolean spooky = false;
		boolean sleaze = false;

		for ( Node node = element.getFirstChild(); node != null; node = node.getNextSibling() )
		{
			String tag = node.getNodeName();
			Node child = node.getFirstChild();

			if ( tag.equals( "cansmash" ) )
			{
				cansmash = DebugDatabase.getStringValue( child ).equals( "y" );
			}
			else if ( tag.equals( "confirmed" ) )
			{
				confirmed = DebugDatabase.getStringValue( child ).equals( "y" );
			}
			else if ( tag.equals( "title" ) )
			{
				name = DebugDatabase.getStringValue( child );
			}
			else if ( tag.equals( "kolid" ) )
			{
				id = StringUtilities.parseInt( DebugDatabase.getNumericValue( child ) );
				seen.add( IntegerPool.get( id ) );
			}
			else if ( tag.equals( "yield" ) )
			{
				yield = StringUtilities.parseInt( DebugDatabase.getNumericValue( child ) );
			}
			else if ( tag.equals( "cold" ) )
			{
				cold = !DebugDatabase.getStringValue( child ).equals( "0" );
			}
			else if ( tag.equals( "hot" ) )
			{
				hot = !DebugDatabase.getStringValue( child ).equals( "0" );
			}
			else if ( tag.equals( "sleazy" ) )
			{
				sleaze = !DebugDatabase.getStringValue( child ).equals( "0" );
			}
			else if ( tag.equals( "spooky" ) )
			{
				spooky = !DebugDatabase.getStringValue( child ).equals( "0" );
			}
			else if ( tag.equals( "stinky" ) )
			{
				stench = !DebugDatabase.getStringValue( child ).equals( "0" );
			}
			else if ( tag.equals( "twinkly" ) )
			{
				twinkly = !DebugDatabase.getStringValue( child ).equals( "0" );
			}
		}

		if ( id < 1 )
		{
			writer.println( name + ": anvil doesn't know ID, so can't check" );
			return;
		}
		int pulver = EquipmentDatabase.getPulverization( id );
		if ( !name.equalsIgnoreCase( ItemDatabase.getItemName( id ) ) )
		{
			writer.println( name + ": doesn't match mafia name: " + 
				ItemDatabase.getItemName( id ) );
		}
		name = ItemDatabase.getItemName( id );
		if ( !confirmed )
		{
			name = "(unconfirmed) " + name;
		}
		if ( pulver == -1 )
		{
			if ( cansmash )
			{
				writer.println( name + ": anvil says this is smashable" );
			}
			return;
		}
		if ( !cansmash )
		{
			writer.println( name + ": anvil says this is not smashable" );
			return;
		}
		if ( pulver == ItemPool.USELESS_POWDER )
		{
			if ( yield != 1 || twinkly || hot || cold || stench || spooky || sleaze )
			{
				writer.println( name + ": anvil says something other than useless powder" );
			}
			return;
		}
		if ( yield == 1 && !(twinkly || hot || cold || stench || spooky || sleaze ) )
		{
			writer.println( name + ": anvil says useless powder" );
			return;
		}
		if ( pulver == ItemPool.EPIC_WAD )
		{
			if ( yield != 10 )
			{
				writer.println( name + ": anvil says something other than epic wad" );
			}
			return;
		}
		if ( yield == 10 )
		{
			writer.println( name + ": anvil says epic wad" );
			return;
		}
		if ( pulver == ItemPool.ULTIMATE_WAD )
		{
			if ( yield != 11 )
			{
				writer.println( name + ": anvil says something other than ultimate wad" );
			}
			return;
		}
		if ( yield == 11 )
		{
			writer.println( name + ": anvil says ultimate wad" );
			return;
		}
		if ( pulver == ItemPool.SEA_SALT_CRYSTAL )
		{
			if ( yield != 12 )
			{
				writer.println( name + ": anvil says something other than sea salt crystal" );
			}
			return;
		}
		if ( yield == 12 )
		{
			writer.println( name + ": anvil says sea salt crystal" );
			return;
		}
		if ( pulver >= 0 )
		{
			writer.println( name + ": I don't know how anvil would say " +
				ItemDatabase.getItemName( pulver ) );
			return;
		}
		if ( yield < 1 || yield > 12 )
		{
			writer.println( name + ": anvil said yield=" + yield + ", wut?" );
			return;
		}
		if ( (pulver & EquipmentDatabase.ELEM_TWINKLY) != 0 )
		{
			if ( !twinkly )
			{
				writer.println( name + ": anvil didn't say twinkly" );
			}
			return;
		}
		else if ( twinkly )
		{
			writer.println( name + ": anvil said twinkly" );
			return;
		}


		if ( (pulver & EquipmentDatabase.ELEM_HOT) != 0 )
		{
			if ( !hot )
			{
				writer.println( name + ": anvil didn't say hot" );
			}
			return;
		}
		else if ( hot )
		{
			writer.println( name + ": anvil said hot" );
			return;
		}
		if ( (pulver & EquipmentDatabase.ELEM_COLD) != 0 )
		{
			if ( !cold )
			{
				writer.println( name + ": anvil didn't say cold" );
			}
			return;
		}
		else if ( cold )
		{
			writer.println( name + ": anvil said cold" );
			return;
		}
		if ( (pulver & EquipmentDatabase.ELEM_STENCH) != 0 )
		{
			if ( !stench )
			{
				writer.println( name + ": anvil didn't say stench" );
			}
			return;
		}
		else if ( stench )
		{
			writer.println( name + ": anvil said stench" );
			return;
		}
		if ( (pulver & EquipmentDatabase.ELEM_SPOOKY) != 0 )
		{
			if ( !spooky )
			{
				writer.println( name + ": anvil didn't say spooky" );
			}
			return;
		}
		else if ( spooky )
		{
			writer.println( name + ": anvil said spooky" );
			return;
		}
		if ( (pulver & EquipmentDatabase.ELEM_SLEAZE) != 0 )
		{
			if ( !sleaze )
			{
				writer.println( name + ": anvil didn't say sleaze" );
			}
			return;
		}
		else if ( sleaze )
		{
			writer.println( name + ": anvil said sleaze" );
			return;
		}
		int myyield = 1;
		while ( (pulver & EquipmentDatabase.YIELD_1P) == 0 )
		{
			myyield++;
		}
		if ( yield != myyield )
		{
			writer.println( name + ": anvil said yield is " + yield + ", not " + myyield );
		}
	}

	private static final Pattern ZAPGROUP_PATTERN = Pattern.compile( "Template:ZAP .*?</a>.*?<td>.*?<td>" );
	private static final Pattern ZAPITEM_PATTERN = Pattern.compile( ">([^<]+)</a>" );

	public static final void checkZapGroups()
	{
		RequestLogger.printLine( "Checking zap groups..." );
		PrintStream report = LogStream.openStream( new File( KoLConstants.DATA_LOCATION, "zapreport.txt" ), true );

		String[] groups = DebugDatabase.ZAPGROUP_PATTERN.split(
			DebugDatabase.readWikiData( "Zapping" ) );
		for ( int i = 1; i < groups.length; ++i )
		{
			String group = groups[ i ];
			int pos = group.indexOf( "</td>" );
			if ( pos != -1 )
			{
				group = group.substring( 0, pos );
			}
			Matcher m = DebugDatabase.ZAPITEM_PATTERN.matcher( group );
			ArrayList<String> items = new ArrayList<String>();
			while ( m.find() )
			{
				items.add( m.group( 1 ) );
			}
			if ( items.size() > 1 )
			{
				DebugDatabase.checkZapGroup( items, report );
			}
		}
		report.close();
	}

	private static void checkZapGroup( ArrayList<String> items, PrintStream report )
	{
		String firstItem = items.get( 0 );
		int itemId = ItemDatabase.getItemId( firstItem );

		if ( itemId == -1 )
		{
			report.println( "Group with unrecognized item: " + firstItem );
			return;
		}
		String[] zapgroup = ZapRequest.getZapGroup( itemId );
		if ( zapgroup.length == 0 )
		{
			report.println( "New group:" );
			Iterator<String> i = items.iterator();
			while ( i.hasNext() )
			{
				report.print( i.next() );
				report.print( ", " );
			}
			report.println( "" );
			return;
		}
		ArrayList<String> existing = new ArrayList<String>();
		existing.addAll( Arrays.asList( zapgroup ) );
		existing.removeAll( items );
		items.removeAll( Arrays.asList( zapgroup ) );
		if ( items.size() == 0 && existing.size() == 0 )
		{
			report.println( "Group OK: " + firstItem );
			return;
		}
		report.println( "Modified group: " + firstItem );
		report.println( "Added:" );
		Iterator<String> i = items.iterator();
		while ( i.hasNext() )
		{
			report.print( i.next() );
			report.print( ", " );
		}
		report.println( "" );
		report.println( "Removed:" );
		i = existing.iterator();
		while ( i.hasNext() )
		{
			report.print( i.next() );
			report.print( ", " );
		}
		report.println( "" );
	}

	// Check Monster Manuel

	public static final void checkManuel()
	{
		RequestLogger.printLine( "Checking Monster Manuel..." );
		DebugDatabase.checkManuelPage( "a" );
		DebugDatabase.checkManuelPage( "b" );
		DebugDatabase.checkManuelPage( "c" );
		DebugDatabase.checkManuelPage( "d" );
		DebugDatabase.checkManuelPage( "e" );
		DebugDatabase.checkManuelPage( "f" );
		DebugDatabase.checkManuelPage( "g" );
		DebugDatabase.checkManuelPage( "h" );
		DebugDatabase.checkManuelPage( "i" );
		DebugDatabase.checkManuelPage( "j" );
		DebugDatabase.checkManuelPage( "k" );
		DebugDatabase.checkManuelPage( "l" );
		DebugDatabase.checkManuelPage( "m" );
		DebugDatabase.checkManuelPage( "n" );
		DebugDatabase.checkManuelPage( "o" );
		DebugDatabase.checkManuelPage( "p" );
		DebugDatabase.checkManuelPage( "q" );
		DebugDatabase.checkManuelPage( "r" );
		DebugDatabase.checkManuelPage( "s" );
		DebugDatabase.checkManuelPage( "t" );
		DebugDatabase.checkManuelPage( "u" );
		DebugDatabase.checkManuelPage( "v" );
		DebugDatabase.checkManuelPage( "w" );
		DebugDatabase.checkManuelPage( "x" );
		DebugDatabase.checkManuelPage( "y" );
		DebugDatabase.checkManuelPage( "z" );
		DebugDatabase.checkManuelPage( "-" );
	}

	private static final void checkManuelPage( final String page )
	{
		RequestLogger.printLine( "Page " + page.toUpperCase() );
		MonsterManuelRequest request = new MonsterManuelRequest( page );
		RequestThread.postRequest( request );
	}
}
