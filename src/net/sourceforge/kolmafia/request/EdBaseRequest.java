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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EdBaseRequest
	extends PlaceRequest
{
	public EdBaseRequest()
	{
		super( "edbase" );
	}

	public EdBaseRequest( final String action )
	{
		super( "edbase", action );
	}

	public EdBaseRequest( final String action, final boolean followRedirects )
	{
		super( "edbase", action, followRedirects );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		String action = GenericRequest.getAction( urlString );

		if ( action == null )
		{
			return;
		}
	}

	private static final Pattern BOOK_PATTERN = Pattern.compile( "You may memorize (\\d+) more pages" );

	public static final void inspectBook( final String responseText )
	{
		int edPoints = Preferences.getInteger( "edPoints" );

		// If we know that we have enough edPoints to get all the
		// skills, don't bother checking.
		if ( edPoints >= 20 )
		{
			return;
		}

		// You read from the Book of the Undying.  You may memorize 21 more pages.
		Matcher matcher = EdBaseRequest.BOOK_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			// Assume that the displayed value includes one point
			// for your current run + any accumulated points + points gained by levelling
			int levelPoints = Math.min( 15, KoLCharacter.getLevel() );
			levelPoints = levelPoints - (int) ( levelPoints / 3 );
			int skillsKnown = 0;
			for ( int i = 17000 ; i <= 17020 ; i++ )
			{
				if ( KoLCharacter.hasSkill( i ) )
				{
					skillsKnown++;
				}
			}
			int newEdPoints = StringUtilities.parseInt( matcher.group( 1 ) ) + skillsKnown - levelPoints;
			if ( newEdPoints > edPoints )
			{
				Preferences.setInteger( "edPoints", newEdPoints );
			}
		}
	}

	private static final Pattern WISDOM_PATTERN = Pattern.compile( "Impart Wisdom unto Current Servant.*?(\\d+) remain" );

	public static final void inspectServants( final String responseText )
	{
		int edPoints = Preferences.getInteger( "edPoints" );

		// If we know that we have enough edPoints to get all the
		// imbumentations, don't bother checking.
		if ( edPoints >= 30 )
		{
			return;
		}

		// "Impart Wisdom unto Current Servant (+100xp, 2 remain)"
		Matcher matcher = EdBaseRequest.WISDOM_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			int newEdPoints = StringUtilities.parseInt( matcher.group( 1 ) ) + 20;
			if ( newEdPoints > edPoints )
			{
				Preferences.setInteger( "edPoints", newEdPoints );
			}
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) || !urlString.contains( "edbase" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );

		// We have nothing special to do for other simple visits.
		if ( action == null )
		{
			return true;
		}

		if ( action.equals( "edbase_book" ) )
		{
			RequestLogger.updateSessionLog( "Visiting The Book of the Undying" );
			return true;
		}

		if ( action.equals( "edbase_door" ) )
		{
			RequestLogger.updateSessionLog( "Visiting The Servants' Quarters" );
			return true;
		}

		return false;
	}
}
