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

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.PokefamData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.HTMLParserUtils;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

public class FamTeamRequest
	extends GenericRequest
{
	private static final Pattern ACTIVE_PATTERN = Pattern.compile( "<div class=\"(slot[^\"]+)\" data-pos=\"(\\d+)\"><div class=\"fambox\" data-id=\"(\\d+)\">(.*?)</div></div>", Pattern.DOTALL );
	private static final Pattern BULLPEN_PATTERN = Pattern.compile( "<div class=\"fambox\" data-id=\"(\\d+)\"[^>]+>(.*?)</div>", Pattern.DOTALL );
	private static final Pattern FAMTYPE_PATTERN = Pattern.compile( "class=tiny>Lv. (\\d+) (.*?)</td>" );

	public static final Pattern FAM_PATTERN = Pattern.compile( "fam=([^&]*)" );
	public static final Pattern IID_PATTERN = Pattern.compile( "iid=([^&]*)" );
	public static final Pattern SLOT_PATTERN = Pattern.compile( "slot=([^&]*)" );

	public enum PokeBoost
	{
		NONE( "None" ),
		POWER( "Power" ),
		HP( "HP" ),
		ARMOR( "Armor" ),
		REGENERATING( "Regenerating" ),
		SMART( "Smart" ),
		SPIKED( "Spiked" ),
		;

		private final String name;

		private PokeBoost( String name )
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return this.name;
		}

	}

	private static Map<PokeBoost, Integer> boostToItemId = new HashMap<PokeBoost, Integer>();
	private static Map<Integer, PokeBoost> itemIdToBoost = new HashMap<Integer, PokeBoost>();
	private static Map<String, PokeBoost> nameToBoost = new HashMap<String, PokeBoost>();

	private static void addBoost( PokeBoost boost, Integer id )
	{
		FamTeamRequest.boostToItemId.put( boost, id );
		FamTeamRequest.itemIdToBoost.put( id, boost );
		FamTeamRequest.nameToBoost.put( boost.toString(), boost );
	}

	static
	{
		addBoost( PokeBoost.POWER, ItemPool.METANDIENONE );
		addBoost( PokeBoost.HP, ItemPool.RIBOFLAVIN );
		addBoost( PokeBoost.ARMOR, ItemPool.BRONZE );
		addBoost( PokeBoost.REGENERATING, ItemPool.GINSENG );
		addBoost( PokeBoost.SMART, ItemPool.PIRACETAM );
		addBoost( PokeBoost.SPIKED, ItemPool.ULTRACALCIUM );
	}

	public static PokeBoost getPokeBoost( String race )
	{
		String boosts = Preferences.getString( "pokefamBoosts" );
		int start = boosts.indexOf( race );
		if ( start == -1 )
		{
			return PokeBoost.NONE;
		}
		int colon = boosts.indexOf( ":", start );
		if ( colon == -1 )
		{
			return PokeBoost.NONE;
		}
		int end = boosts.indexOf( "|", colon );
		String name = end == -1 ? boosts.substring( colon + 1 ) : boosts.substring( colon + 1, end );
		PokeBoost boost = FamTeamRequest.nameToBoost.get( name );
		return boost == null ? PokeBoost.NONE : boost;
	}

	private static int getFamId( final String urlString )
	{
		Matcher matcher = FamTeamRequest.FAM_PATTERN.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( GenericRequest.decodeField( matcher.group( 1 ) ) ) : -1;
	}

	private static int getItemId( final String urlString )
	{
		Matcher matcher = FamTeamRequest.IID_PATTERN.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( GenericRequest.decodeField( matcher.group( 1 ) ) ) : -1;
	}

	private static int getSlot( final String urlString )
	{
		Matcher matcher = FamTeamRequest.SLOT_PATTERN.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( GenericRequest.decodeField( matcher.group( 1 ) ) ) : -1;
	}

	public FamTeamRequest()
	{
		super( "famteam.php" );
	}

	public FamTeamRequest( String race, PokeBoost boost)
	{
		super( "famteam.php" );
		this.addFormField( "action", "feed" );
		this.addFormField( "fam", String.valueOf( FamiliarDatabase.getFamiliarId( race ) ) );
		this.addFormField( "iid", String.valueOf( FamTeamRequest.boostToItemId.get( boost ) ) );
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		KoLmafia.updateDisplay( "Retrieving familiar data..." );
		super.run();
		return;
	}

	@Override
	public void processResults()
	{
		if ( !KoLCharacter.inPokefam() )
		{
			return;
		}

		if ( !FamTeamRequest.parseResponse( this.getURLString(), this.responseText ) )
		{
			// *** Have more specific error message?
			KoLmafia.updateDisplay( MafiaState.ERROR, "Familiar request unsuccessful." );
			return;
		}
	}

	public static final boolean parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "famteam.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action != null )
		{
			if ( action.equals( "feed" ) )
			{
				// Familiar powered up.
				// You can't give that familiar any more pills.
				if ( responseText.contains( "Familiar powered up." ) )
				{
					int fam = getFamId( urlString );
					int iid = getItemId( urlString );
					if ( fam != -1 && iid != -1 )
					{
						StringBuilder buffer = new StringBuilder( Preferences.getString( "pokefamBoosts" ) );
						if ( buffer.length() > 0 )
						{
							buffer.append( "|" );
						}
						buffer.append( FamiliarDatabase.getFamiliarName( fam ) );
						buffer.append( ":" );
						String boost = FamTeamRequest.itemIdToBoost.get( iid ).toString();

						// A KoL bug allows you to add an attribute that the familiar naturally has.
						// It still counts as the sole powerup for the familiar, but we don't want
						// to remove it when looking at the famteam or fambattle, so use "None"
						PokefamData data = FamiliarDatabase.getPokeDataById( fam );
						if ( data != null && data.getAttribute().equals( boost ) )
						{
							boost = "None";
						}

						buffer.append( boost );
						Preferences.setString( "pokefamBoosts", buffer.toString() );

						// Remove from inventory
						ResultProcessor.processResult( ItemPool.get( iid, -1 ) );
					}
				}
			}
			else if ( action.equals( "slot" ) )
			{
				// Parse the page to see where familiars ended up.
			}
		}

		Matcher matcher = FamTeamRequest.ACTIVE_PATTERN.matcher( responseText );
		boolean logit = false;	// Preferences.getBoolean( "logCleanedHTML" );
		while ( matcher.find() )
		{
			String state = matcher.group( 1 );
			int slot = StringUtilities.parseInt( matcher.group( 2 ) ) - 1;
			int id = StringUtilities.parseInt( matcher.group( 3 ) );

			if ( id == 0 )
			{
				KoLCharacter.setPokeFam( slot, FamiliarData.NO_FAMILIAR );
				continue;
			}

			String famtable = matcher.group( 4 );

			Matcher fmatcher = FamTeamRequest.FAMTYPE_PATTERN.matcher( famtable );
			if ( !fmatcher.find() )
			{
				continue;	// Huh?
			}

			int level = StringUtilities.parseInt( fmatcher.group( 1 ) );
			String name = fmatcher.group( 2 );

			// See what we can learn about this familiar
			// RequestLogger.printLine( "Process familiar " + name );
			FamTeamRequest.parsePokeTeamData( famtable, logit );

			FamiliarData familiar = KoLCharacter.findFamiliar( id );
			if ( familiar == null )
			{
				// Add new familiar to list
				familiar = new FamiliarData( id, name, level );
			}
			else
			{
				// Update existing familiar
				familiar.update( name, level );
			}
			KoLCharacter.addFamiliar( familiar );
			KoLCharacter.setPokeFam( slot, familiar );
		}

		matcher = FamTeamRequest.BULLPEN_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int id = StringUtilities.parseInt( matcher.group( 1 ) );
			String famtable = matcher.group( 2 );

			Matcher fmatcher = FamTeamRequest.FAMTYPE_PATTERN.matcher( famtable );
			if ( !fmatcher.find() )
			{
				continue;	// Huh?
			}

			int level = StringUtilities.parseInt( fmatcher.group( 1 ) );
			String name = fmatcher.group( 2 );

			// See what we can learn about this familiar
			// RequestLogger.printLine( "Process familiar " + name );
			FamTeamRequest.parsePokeTeamData( famtable, logit );

			FamiliarData.registerFamiliar( id, name, level );
		}

		return true;
	}

	// Make an HTML cleaner
	private static final HtmlCleaner cleaner = HTMLParserUtils.configureDefaultParser();
	static
	{
		CleanerProperties props = cleaner.getProperties();
		// prune things, perhaps
	};

	private static final TagNode cleanPokeTeamHTML( final String text )
	{
		try
		{
			// Clean the HTML on this response page
			return cleaner.clean( text );
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
		}
		return null;
	}

/*
<html>
  <head>
  <body>
    <!-- hi -->
    <!-- hi -->
    <table class="">
      <tbody>
        <tr>
          <td rowspan="2">
            <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/familiar32.gif">
          <td class="tiny" width="150">
            6655321 Grrl
          <td rowspan="2" width="120">
            <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/blacksword.gif">
          <td rowspan="2" align="center" width="60">
            <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/spectacles.gif" alt="Smart:  This familiar gains 2 XP per battle." title="Smart:  This familiar gains 2 XP per battle.">
          <td rowspan="2" width="150">
            <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/blackheart.gif">
            <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/blackheart.gif">
        <tr>
          <td class="tiny">
            Lv. 1 Clockwork Grapefruit
        <tr>
          <td height="10">
        <tr>
          <td>
          <td colspan="5" class="small" valign="center">
            <b>
              Skills:
            <span title="Deal [power] damage to the frontmost enemy.">
              [Bonk]
            &nbsp;&nbsp;
            <span title="Heal itself by [power]">
              [Regrow]
            &nbsp;&nbsp;
*/
	
	private static final void parsePokeTeamData( final String famtable, boolean logit )
	{
		TagNode node = FamTeamRequest.cleanPokeTeamHTML( famtable );
		if ( node == null )
		{
			return;
		}

		if ( RequestLogger.isDebugging() && logit )
		{
			HTMLParserUtils.logHTML( node );
		}

		// Familiars on this page have the same format as enemy familiars in a fambattle
		FightRequest.parsePokefam( node, true, true );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "famteam.php" ) )
		{
			return false;
		}

		if ( urlString.equals( "famteam.php" ) )
		{
			// Visiting the terrarium
			return true;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			// Unknown non-action. Log URL
			return false;
		}

		if (  action.equals( "feed" ) )
		{
			String race = FamiliarDatabase.getFamiliarName( FamTeamRequest.getFamId( urlString ) );
			String item = ItemDatabase.getItemName( FamTeamRequest.getItemId( urlString ) );
			String printme = "Feeding " + item + " to " + race;

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( printme );
			return true;
		}

		if (  action.equals( "slot" ) )
		{
			String race = FamiliarDatabase.getFamiliarName( FamTeamRequest.getFamId( urlString ) );
			int slot = FamTeamRequest.getSlot( urlString );
			String printme = "Putting  " + race + " into slot " + slot + " of your Pokefam team";

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( printme );
			return true;
		}

		// Unknown action. Log the URL
		return false;
	}
}
