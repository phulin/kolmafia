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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class BountyDatabase
{
	private static final ArrayList<String> bountyNames = new ArrayList<String>();
	private static final Map<String, String> bountyByPlural = new HashMap<String, String>();
	private static final Map<String, String> pluralByName = new HashMap<String, String>();
	private static final Map<String, String> typeByName = new HashMap<String, String>();
	private static final Map<String, String> imageByName = new HashMap<String, String>();
	private static final Map<String, String> numberByName = new HashMap<String, String>();
	private static final Map<String, String> monsterByName = new HashMap<String, String>();
	private static final Map<String, String> nameByMonster = new HashMap<String, String>();
	private static final Map<String, String> locationByName = new HashMap<String, String>();

	public static String [] canonicalNames;
	private static final Map<String, String> canonicalToName = new HashMap<String, String>();

	static
	{
		BountyDatabase.reset();
	}

	public static void reset()
	{
		BountyDatabase.bountyNames.clear();
		BountyDatabase.bountyByPlural.clear();
		BountyDatabase.pluralByName.clear();
		BountyDatabase.typeByName.clear();
		BountyDatabase.imageByName.clear();
		BountyDatabase.numberByName.clear();
		BountyDatabase.monsterByName.clear();
		BountyDatabase.nameByMonster.clear();
		BountyDatabase.locationByName.clear();

		BountyDatabase.readData();
		BountyDatabase.buildCanonicalNames();
	}

	private static void readData()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "bounty.txt", KoLConstants.BOUNTY_VERSION );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 7 )
			{
				continue;
			}
			
			String name = data[ 0 ];
			BountyDatabase.bountyNames.add( name );
			BountyDatabase.bountyByPlural.put( data[ 1 ], name );
			BountyDatabase.pluralByName.put( name, data[ 1 ] );
			BountyDatabase.typeByName.put( name, data[ 2 ] );
			BountyDatabase.imageByName.put( name, data[ 3 ] );
			BountyDatabase.numberByName.put( name, data[ 4 ] );
			BountyDatabase.monsterByName.put( name, data[ 5 ] );
			BountyDatabase.nameByMonster.put( data[ 5 ], name );
			BountyDatabase.locationByName.put( name, data[ 6 ] );
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static void buildCanonicalNames()
	{
		BountyDatabase.canonicalNames = new String[ BountyDatabase.bountyNames.size() ];
		for ( int i = 0; i < BountyDatabase.canonicalNames.length; ++i )
		{
			String name = BountyDatabase.bountyNames.get( i );
			String canonical = StringUtilities.getCanonicalName( name );
			BountyDatabase.canonicalNames[ i ] = canonical;
			BountyDatabase.canonicalToName.put( canonical, name );
		}
		Arrays.sort( BountyDatabase.canonicalNames );
	}

	public static final List<String> getMatchingNames( final String substring )
	{
		return StringUtilities.getMatchingNames( BountyDatabase.canonicalNames, substring );
	}

	public static final String canonicalToName( final String canonical )
	{
		String name = BountyDatabase.canonicalToName.get( canonical );
		return name == null ? "" : name;
	}

	public static final void setValue( String name, String plural, String type, String image, int number, String monster, String location )
	{
		BountyDatabase.bountyNames.add( name );
		BountyDatabase.bountyByPlural.put( plural, name );
		BountyDatabase.pluralByName.put( name, plural );
		BountyDatabase.typeByName.put( name, type );
		BountyDatabase.imageByName.put( name, image );
		BountyDatabase.numberByName.put( name, Integer.toString( number ) );
		BountyDatabase.monsterByName.put( name, monster );
		BountyDatabase.nameByMonster.put( monster, name );
		if ( location != null )
		{
			BountyDatabase.locationByName.put( name, location );
		}
		BountyDatabase.buildCanonicalNames();

		String printMe = "Unknown bounty:";
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );
		printMe = "--------------------";
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );
		if ( location != null )
		{
			printMe = name + "\t" + plural + "\t" + type + "\t" + image + "\t" + String.valueOf( number ) + "\t" + monster + "\t" + location;
		}
		else
		{
			printMe = name + "\t" + plural + "\t" + type + "\t" + image + "\t" + String.valueOf( number ) + "\t" + monster + "\tunknown";
		}
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );
		printMe = "--------------------";
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );
	}

	public static final String[] entrySet()
	{
		return BountyDatabase.bountyNames.toArray( new String[ BountyDatabase.bountyNames.size() ] );
	}

	public static final String getName( String plural )
	{
		if ( plural == null || plural.equals( "" ) )
		{
			return null;
		}
		
		return BountyDatabase.bountyByPlural.get( plural );
	}

	public static final String getNameByMonster( String monster )
	{
		if ( monster == null || monster.equals( "" ) )
		{
			return null;
		}
		
		return BountyDatabase.nameByMonster.get( monster );
	}

	public static final String getPlural( String name )
	{
		if ( name == null || name.equals( "" ) )
		{
			return null;
		}
		
		return BountyDatabase.pluralByName.get( name );
	}

	public static final String getType( String name )
	{
		if ( name == null || name.equals( "" ) )
		{
			return null;
		}

		return BountyDatabase.typeByName.get( name );
	}

	public static final String getImage( String name )
	{
		if ( name == null || name.equals( "" ) )
		{
			return null;
		}

		return BountyDatabase.imageByName.get( name );
	}

	public static final int getNumber( String name )
	{
		if ( name == null || name.equals( "" ) )
		{
			return 0;
		}
		
		String numberString = BountyDatabase.numberByName.get( name );
		return numberString == null ? 0 : (int) StringUtilities.parseInt( numberString );
	}

	public static final String getMonster( String name )
	{
		if ( name == null || name.equals( "" ) )
		{
			return null;
		}

		return BountyDatabase.monsterByName.get( name );
	}

	public static final String getLocation( String name )
	{
		if ( name == null || name.equals( "" ) )
		{
			return null;
		}

		return BountyDatabase.locationByName.get( name );
	}

	public static final boolean checkBounty( String pref )
	{
		String currentBounty = Preferences.getString( pref );
		int bountySeparator = currentBounty.indexOf( ":" );
		if ( bountySeparator != -1 )
		{
			String bountyName = currentBounty.substring( 0, bountySeparator );
			if ( "null".equals( bountyName ) )
			{
				Preferences.setString( pref, "" );
				return false;
			}
			if ( bountyName != null && !bountyName.equals( "" ) )
			{
				int currentBountyCount = StringUtilities.parseInt( currentBounty.substring( bountySeparator + 1 ) );
				if ( currentBountyCount == BountyDatabase.getNumber( bountyName ) )
				{
					return true;
				}
			}
		}
		return false;
	}
}
