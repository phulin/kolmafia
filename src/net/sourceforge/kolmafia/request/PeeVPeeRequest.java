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

import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureMultiResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.PvpManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PeeVPeeRequest
	extends GenericRequest
{
	public static final String[] WIN_MESSAGES =
		new String[] { "50 CHARACTER LIMIT BREAK!", "HERE'S YOUR CHEETO, MOTHER!*$#ER.", "If you want it back, I'll be in my tent.", "PWNED LIKE CRAPSTORM." };

	public static final String[] LOSE_MESSAGES =
		new String[] { "OMG HAX H4X H5X!!", "Please return my pants.", "How do you like my Crotch-To-Your-Foot style?", "PWNED LIKE CRAPSTORM." };

	private static final Pattern ATTACKS_PATTERN =
		Pattern.compile( "You have (\\d+) fight" );

	private static final Pattern CHALLENGE_PATTERN1 =
		Pattern.compile( "<div class=\"fight\"><a.*?who=(\\d+)\"><b>(.*?)</b></a> calls out <a.*?who=(\\d+)\"><b>(.*?)</b></a> for battle!" );

	private static final Pattern CHALLENGE_PATTERN2 =
		Pattern.compile( "<a.*?who=(\\d+)\">(.*?)</a> vs <a.*?who=(\\d+)\">(.*?)</a>" );

	private static final Pattern WIN_PATTERN1 =
		Pattern.compile( "<span[^>]*><b>(.*?)</b> won the fight, <b>(\\d+)</b> to <b>(\\d+)</b>!" );
	private static final Pattern WIN_PATTERN2 =
		Pattern.compile( "align=\"center\"><b>(.*?)</b> Wins!</td>" );

	private static final Pattern SWAGGER_PATTERN = 
		Pattern.compile( "You gain a little swagger <b>\\([+](\\d)\\)</b>" );

	public static final Pattern RANKED_PATTERN = Pattern.compile( "ranked=([^&]*)" );
	public static final Pattern WHO_PATTERN = Pattern.compile( "who=([^&]*)" );
	public static final Pattern STANCE_PATTERN = Pattern.compile( "stance=([^&]*)" );
	public static final Pattern MISSION_PATTERN = Pattern.compile( "attacktype=([^&]*)" );

	public PeeVPeeRequest()
	{
		super( "peevpee.php" );
	}
	
	public PeeVPeeRequest( final String place )
	{
		super( "peevpee.php" );
		this.addFormField( "place", place );
	}
	
	public PeeVPeeRequest( final String opponent, final int stance, final String mission )
	{
		super( "peevpee.php" );
		
		this.addFormField( "action", "fight" );
		this.addFormField( "place", "fight" );
		this.addFormField( "attacktype", mission );
		// ranked=1 for normal, 2 for harder
		this.addFormField( "ranked", "1" );
		this.addFormField( "stance", String.valueOf( stance ) );
		this.addFormField( "who", opponent );
		
		String win = Preferences.getString( "defaultFlowerWinMessage" );
		String lose = Preferences.getString( "defaultFlowerLossMessage" );

		if ( win.equals( "" ) )
		{
			win = PeeVPeeRequest.WIN_MESSAGES[ KoLConstants.RNG.nextInt( PeeVPeeRequest.WIN_MESSAGES.length ) ];
		}
		if ( lose.equals( "" ) )
		{
			lose =
				PeeVPeeRequest.LOSE_MESSAGES[ KoLConstants.RNG.nextInt( PeeVPeeRequest.LOSE_MESSAGES.length ) ];
		}
		
		this.addFormField( "winmessage", win );
		this.addFormField( "losemessage", lose );
	}
	
	public void setTarget( final String target )
	{
		this.addFormField( "who", target );
	}
	
	public void setTargetType( final String type )
	{
		this.addFormField( "ranked", type );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( location.contains( "place=shop" ) || location.contains( "action=buy" ) )
		{
			SwaggerShopRequest.parseResponse( location, responseText );
			return;
		}
		
		if ( location.contains( "place=fight" ) )
		{
			Matcher attacksMatcher = PeeVPeeRequest.ATTACKS_PATTERN.matcher( responseText );
			if ( attacksMatcher.find() )
			{
				KoLCharacter.setAttacksLeft( StringUtilities.parseInt( attacksMatcher.group( 1 ) ) );
				KoLCharacter.setHippyStoneBroken( true );
			}
			else if ( responseText.contains( "You're out of fights!" ) )
			{
				KoLCharacter.setAttacksLeft( 0 );
				KoLCharacter.setHippyStoneBroken( true );
			}
			else if ( responseText.contains( "Magical Mystical Hippy Stone" ) )
			{
				KoLCharacter.setHippyStoneBroken( false );
			}

			if ( location.contains( "action=fight" ) )
			{
				// You may not attack players who are in Hardcore mode unless you are in Hardcore mode yourself.
				// You can't attack a player against whom you've already won a fight today.
				// You can't attack somebody in the same clan as you.
				// Sorry, I couldn't find the player "sdfsdfs".
				// You know, once you start hurting yourself, you won't be able to stop,
				// and you'll end up working for James Spader as a secretary, and
				// constantly letting him spank you. Let's not start down that road.
				//
				// Include <tr><td> to avoid matching player-supplied messages
				//
				// On the other hand, the following is a valid message from a loss:
				// You gain 0 Strengthliness.

				if ( responseText.contains( "<tr><td>You may not" ) ||
				     responseText.contains( "<tr><td>You can't" ) ||
				     responseText.contains( "<tr><td>You know" ) ||
				     responseText.contains( "<tr><td>Sorry" ) )
				{
					RequestLogger.printLine( "Invalid target" );
					return;
				}


				// <tr><td><p>Before entering combat, you must pledge your allegiance to a clan for the season.
				if ( responseText.contains( "<td><p>Before entering combat" ) )
				{
					KoLmafia.updateDisplay( MafiaState.ABORT, "You need to pledge allegiance to a clan first." );
					return;
				}

				Matcher swaggerMatcher = PeeVPeeRequest.SWAGGER_PATTERN.matcher( responseText );
				if ( swaggerMatcher.find() )
				{
					Preferences.increment( "availableSwagger", Integer.parseInt( swaggerMatcher.group(1) ) );
				}

				boolean compactResults = false;
				Matcher challengeMatcher = PeeVPeeRequest.CHALLENGE_PATTERN1.matcher( responseText );
				Matcher winMatcher;
				boolean won = false;
				//int id1 = 0;
				String me = null;
				//int id2 = 0;
				String you = null;
				int result1 = 0;
				int result2 = 0;

				if ( challengeMatcher.find() )
				{
					//id1 = Integer.parseInt( challengeMatcher.group( 1 ) );
					me = challengeMatcher.group( 2 );
					//id2 = Integer.parseInt( challengeMatcher.group( 3 ) );
					you = challengeMatcher.group( 4 );
				}
				else
				{
					compactResults = true;
					challengeMatcher = PeeVPeeRequest.CHALLENGE_PATTERN2.matcher( responseText );
					if ( challengeMatcher.find() )
					{
						//id1 = Integer.parseInt( challengeMatcher.group( 1 ) );
						me = challengeMatcher.group( 2 );
						//id2 = Integer.parseInt( challengeMatcher.group( 3 ) );
						you = challengeMatcher.group( 4 );
					}
				}

				if ( !compactResults )
				{
					winMatcher = PeeVPeeRequest.WIN_PATTERN1.matcher( responseText );
					if ( winMatcher.find() )
					{
						String winner = winMatcher.group( 1 );
						won = winner.equals( me );
						result1 = Integer.parseInt( winMatcher.group( 2 ) );
						result2 = Integer.parseInt( winMatcher.group( 3 ) );
					}
				}
				else
				{
					winMatcher = PeeVPeeRequest.WIN_PATTERN2.matcher( responseText );
					if ( winMatcher.find() )
					{
						String winner = winMatcher.group( 1 );
						won = winner.equals( me );
					}
				}

				if ( you == null )
				{
					// Something went wrong.  Ideally we won't get here, but this will at least
					// prevent looping through failed attacks
					KoLmafia.updateDisplay( MafiaState.ABORT, "Something went wrong with executing your PvP fights" );
					return;
				}
				
				StringBuilder buf = new StringBuilder( "You challenged " );
				buf.append( you );
				buf.append( " and " );
				buf.append( won ? "won" : "lost" );
				buf.append( " the PvP fight" );
				if ( !compactResults )
				{
					buf.append( ", " );
					buf.append( String.valueOf( won ? result1 : result2 ) );
					buf.append( " to " );
					buf.append( String.valueOf( won ? result2 : result1 ) );
					buf.append( "!" );
				}
				String message = buf.toString();
				RequestLogger.printLine( message );
				RequestLogger.updateSessionLog( message);

				if ( won )
				{
					Preferences.setString( "currentPvpVictories", Preferences.getString( "currentPvpVictories" ) + you + "," );
				}
				else if ( !compactResults )
				{
					PeeVPeeRequest.parseStatLoss( responseText );
				}
			}
			else if ( !PvpManager.stancesKnown )
			{
				PvpManager.parseStances( responseText );
			}
			return;
		}

		if ( location.contains( "action=smashstone" ) )
		{
			if ( responseText.contains( "You shatter" ) )
			{
				KoLCharacter.setAttacksLeft( 10 );
				KoLCharacter.setHippyStoneBroken( true );
			}
		}
	}
	
	private static final String STAT_STRING = KoLCharacter.getUserName().toLowerCase() + " lost ";
	
	private static final void parseStatLoss( final String responseText )
	{
		String[] blocks = responseText.split( "<td>" );
		for ( int i = 0; i < blocks.length; ++i )
		{
			if ( blocks[i].toLowerCase().indexOf( STAT_STRING ) != 0 )
			{
				continue;
			}
			String printedStatMessage = blocks[i].substring( 0, blocks[i].indexOf( ".</td>" ) );
			int index = printedStatMessage.lastIndexOf( " lost " );
			String statMessage = printedStatMessage.substring( index + 6 );
			String[] stats = statMessage.split( " " );
			int statsLost = -1 * Integer.parseInt( stats[0] );
			String statname = stats[1];
			int[] gained =
				{ AdventureResult.MUS_SUBSTAT.contains( statname ) ? statsLost : 0,
				  AdventureResult.MYS_SUBSTAT.contains( statname ) ? statsLost : 0,
				  AdventureResult.MOX_SUBSTAT.contains( statname ) ? statsLost : 0 };
			AdventureResult result = new AdventureMultiResult( AdventureResult.SUBSTATS, gained );
			ResultProcessor.processResult( result );
			RequestLogger.printLine( printedStatMessage );
		}
	}

	public static final void parseItems( final String responseText )
	{
		// This doesn't work in compact mode
		Matcher itemMatcher = ResultProcessor.ITEM_TABLE_PATTERN.matcher( responseText );
		if ( itemMatcher.find() )
		{
			String relString = itemMatcher.group( 1 );
			AdventureResult item = ItemDatabase.itemFromRelString( relString );

			ResultProcessor.processItem( false, "You acquire an item:", item, (List<AdventureResult>) null );
		}
	}

	private static final String getField( final Pattern pattern, final String urlString )
	{
		Matcher matcher = pattern.matcher( urlString );
		return matcher.find() ? matcher.group(1) : null;
	}

	private static final String getOpponent( final String who, final String ranked )
	{
		if ( who != null && !who.equals( "" ) )
		{
			return who;
		}

		if ( ranked != null && ranked.equals( "1" ) )
		{
			return "a random opponent";
		}

		if ( ranked != null && ranked.equals( "2" ) )
		{
			return "a random stronger opponent";
		}

		return "an unknown opponent";
	}

	private static final String getMission( final String mission )
	{
		return ( mission == null ) ?
			"an unknown mission" :
			mission.equals( "lootwhatever" ) ?
			"loot" :
			mission;
	}

	private static final String getStance( final String stanceString )
	{
		String  stanceName =
			stanceString != null ?
			PvpManager.findStance( StringUtilities.parseInt( stanceString ) ) :
			null;
		return stanceName != null ? stanceName : "an unknown stance";
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "peevpee.php" ) )
		{
			return false;
		}

		String place = PeeVPeeRequest.getField( GenericRequest.PLACE_PATTERN, urlString );
		String action = PeeVPeeRequest.getField( GenericRequest.ACTION_PATTERN, urlString );

		// Don't log visits to the container document
		if ( place == null && action == null )
		{
			return true;
		}

		if ( place == null )
		{
			return false;
		}
		
		if ( place.equals( "rules" ) || place.equals( "boards" ) || place.equals( "logs" ) )
		{
			return true;
		}

		if ( place.equals( "shop" ) )
		{
			return SwaggerShopRequest.registerRequest( urlString );
		}

		if ( action == null )
		{
			return true;
		}

		if ( place.equals( "fight" ) )
		{
			if ( action.equals( "fight" ) )
			{
				String ranked = PeeVPeeRequest.getField( PeeVPeeRequest.RANKED_PATTERN, urlString );
				String who = PeeVPeeRequest.getField( PeeVPeeRequest.WHO_PATTERN, urlString );
				String stance = PeeVPeeRequest.getField( PeeVPeeRequest.STANCE_PATTERN, urlString );
				String mission = PeeVPeeRequest.getField( PeeVPeeRequest.MISSION_PATTERN, urlString );

				StringBuilder buf = new StringBuilder();
				buf.append( "Attack " );
				buf.append( PeeVPeeRequest.getOpponent( who, ranked ) );
				buf.append( " for " );
				buf.append( PeeVPeeRequest.getMission( mission ) );
				buf.append( " via " );
				buf.append( PeeVPeeRequest.getStance( stance ) );

				String message = buf.toString();
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( message );
			}
			return true;
		}

		// Log anything else, for now
		return false;
	}
}
