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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RestoreExpression;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.StandardRequest;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.Limitmode;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class RestoresDatabase
{
	private static final ArrayList<String> restoreNames = new ArrayList<String>();
	private static final Map<String, String> typeByName = new HashMap<String, String>();
	private static final Map<String, String> hpMinByName = new HashMap<String, String>();
	private static final Map<String, String> hpMaxByName = new HashMap<String, String>();
	private static final Map<String, String> mpMinByName = new HashMap<String, String>();
	private static final Map<String, String> mpMaxByName = new HashMap<String, String>();
	private static final Map<String, Integer> advCostByName = new HashMap<String, Integer>();
	private static final Map<String, String> usesLeftByName = new HashMap<String, String>();
	private static final Map<String, String> notesByName = new HashMap<String, String>();

	static
	{
		RestoresDatabase.reset();
	}

	public static void reset()
	{
		RestoresDatabase.restoreNames.clear();
		RestoresDatabase.typeByName.clear();
		RestoresDatabase.hpMinByName.clear();
		RestoresDatabase.hpMaxByName.clear();
		RestoresDatabase.mpMinByName.clear();
		RestoresDatabase.mpMaxByName.clear();
		RestoresDatabase.advCostByName.clear();
		RestoresDatabase.usesLeftByName.clear();
		RestoresDatabase.notesByName.clear();

		RestoresDatabase.readData();
	}

	private static void readData()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "restores.txt", KoLConstants.RESTORES_VERSION );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 7 )
			{
				continue;
			}
			
			String name = data[ 0 ];
			RestoresDatabase.restoreNames.add( name );
			RestoresDatabase.typeByName.put( name, data[ 1 ] );
			RestoresDatabase.hpMinByName.put( name, data[ 2 ] );
			RestoresDatabase.hpMaxByName.put( name, data[ 3 ] );
			RestoresDatabase.mpMinByName.put( name, data[ 4 ] );
			RestoresDatabase.mpMaxByName.put( name, data[ 5 ] );
			int advCost = StringUtilities.parseInt( data[ 6 ] );
			RestoresDatabase.advCostByName.put( name, IntegerPool.get( advCost ) );
			
			if ( data.length > 7 )
			{
				RestoresDatabase.usesLeftByName.put( name, data[ 7 ] );
			}
			else
			{
				RestoresDatabase.usesLeftByName.put( name, "unlimited" );
			}
			
			if ( data.length > 8 )
			{
				RestoresDatabase.notesByName.put( name, data[ 8 ] );
			}
			else
			{
				RestoresDatabase.notesByName.put( name, "" );
			}
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

	public static final void setValue( final String name, final String type, final String hpMin, final String hpMax, 
				final String mpMin, final String mpMax, final int advCost, final int usesLeft, final String notes )
	{
		RestoresDatabase.typeByName.put( name, type );
		RestoresDatabase.hpMinByName.put( name, hpMin );
		RestoresDatabase.hpMaxByName.put( name, hpMax );
		RestoresDatabase.mpMinByName.put( name, mpMin );
		RestoresDatabase.mpMaxByName.put( name, mpMax );
		RestoresDatabase.advCostByName.put( name, IntegerPool.get( advCost ) );
		if ( usesLeft != -1 )
		{
			RestoresDatabase.usesLeftByName.put( name, Integer.toString( usesLeft ) );
		}
		if ( notes != null )
		{
			RestoresDatabase.notesByName.put( name, notes );
		}
	}

	private static final int getValue( final String stringValue, final String name )
	{
		if ( stringValue == null )
		{
			return -1;
		}
		int lb = stringValue.indexOf( "[" );
		if ( lb == -1 )
		{
			return Integer.parseInt( stringValue );
		}
		int rb = stringValue.indexOf( "]", lb );
		RestoreExpression expr = new RestoreExpression( stringValue.substring( lb + 1, rb ), name );
		if( expr.hasErrors() )
		{
			KoLmafia.updateDisplay( "Error in restores.txt for item " + name + ", invalid expression " + stringValue );
			return -1;
		}
		return (int) expr.eval();
	}

	public static final String getType( final String name )
	{
		if ( name == null )
		{
			return null;
		}

		return RestoresDatabase.typeByName.get( name );
	}

	public static final int getHPMin( final String name )
	{
		if ( name == null || ( KoLCharacter.isEd() && !RestoresDatabase.edSafe( name ) ) )
		{
			return 0;
		}

		if ( name.equals( "Campground" ) || name.equals( "Free rests" ) )
		{
			return KoLCharacter.getRestingHP();
		}

		String hpMin = RestoresDatabase.hpMinByName.get( name );
		if ( hpMin == null )
		{
			return 0;
		}
		return (int) Math.floor( RestoresDatabase.getValue( hpMin, name ) );
	}

	public static final int getHPMax( final String name )
	{
		if ( name == null || ( KoLCharacter.isEd() && !RestoresDatabase.edSafe( name ) ) )
		{
			return 0;
		}

		if ( name.equals( "Campground" ) || name.equals( "Free rests" ) )
		{
			return KoLCharacter.getRestingHP();
		}

		String hpMax = RestoresDatabase.hpMaxByName.get( name );
		if ( hpMax == null )
		{
			return 0;
		}
		return (int) Math.ceil( RestoresDatabase.getValue( hpMax, name ) );
	}

	public static final int getMPMin( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		if ( name.equals( "Campground" ) || name.equals( "Free rests" ) )
		{
			return KoLCharacter.getRestingMP();
		}

		String mpMin = RestoresDatabase.mpMinByName.get( name );
		if ( mpMin == null )
		{
			return 0;
		}
		return (int) Math.floor( RestoresDatabase.getValue( mpMin, name ) );
	}

	public static final Integer getMPMax( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		if ( name.equals( "Campground" ) || name.equals( "Free rests" ) )
		{
			return KoLCharacter.getRestingMP();
		}

		String mpMax = RestoresDatabase.mpMaxByName.get( name );
		if ( mpMax == null )
		{
			return 0;
		}
		return (int) Math.ceil( RestoresDatabase.getValue( mpMax, name ) );
	}

	public static final double getHPAverage( final String name )
	{
		if ( name == null || ( KoLCharacter.isEd() && !RestoresDatabase.edSafe( name ) ) )
		{
			return 0;
		}

		return ( RestoresDatabase.getHPMax( name ) + RestoresDatabase.getHPMin( name ) ) / 2.0;
	}

	public static final double getMPAverage( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		return ( RestoresDatabase.getMPMax( name ) + RestoresDatabase.getMPMin( name ) ) / 2.0;
	}

	public static final String getHPRange( final String name )
	{
		if ( name == null || ( KoLCharacter.isEd() && !RestoresDatabase.edSafe( name ) ) )
		{
			return "";
		}

		int hpMin = RestoresDatabase.getHPMin( name );
		int hpMax = RestoresDatabase.getHPMax( name );
		if ( hpMin == 0 && hpMax == 0 )
		{
			return "";
		}
		if ( hpMin == hpMax )
		{
			return Integer.toString( hpMin );
		}
		return ( Integer.toString( hpMin ) + "-" + Integer.toString( hpMax ) );
	}

	public static final String getMPRange( final String name )
	{
		if ( name == null )
		{
			return "";
		}

		int mpMin = RestoresDatabase.getMPMin( name );
		int mpMax = RestoresDatabase.getMPMax( name );
		if ( mpMin == 0 && mpMax == 0 )
		{
			return "";
		}
		if ( mpMin == mpMax )
		{
			return Integer.toString( mpMin );
		}
		return ( Integer.toString( mpMin ) + "-" + Integer.toString( mpMax ) );
	}

	public static final int getAdvCost( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		Integer advCost = RestoresDatabase.advCostByName.get( name );
		if ( advCost == null )
		{
			return 0;
		}
		return ( (int) advCost );
	}

	public static final int getUsesLeft( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		if ( name.equals( "Free rests" ) )
		{
			return Math.max( KoLCharacter.freeRestsAvailable() - Preferences.getInteger( "timesRested" ), 0 );
		}
		String usesLeft = RestoresDatabase.usesLeftByName.get( name );
		if ( usesLeft == null )
		{
			return 0;
		}
		if ( usesLeft.equals( "unlimited" ) )
		{
			return -1;
		}
		return (int) Math.floor( RestoresDatabase.getValue( usesLeft, name ) );
	}

	public static final String getNotes( final String name )
	{
		if ( name == null )
		{
			return null;
		}

		String notes = RestoresDatabase.notesByName.get( name );
		if ( notes == null)
		{
			return "";
		}
		return notes;
	}

	public static final Boolean isRestore( final int itemId )
	{
		String name = ItemDatabase.getItemName( itemId );
		if ( name == null )
		{
			return false;
		}
		String type = RestoresDatabase.getType( name );
		if ( type == null )
		{
			return false;
		}
		return type.equalsIgnoreCase( "item" );
	}

	public static final Boolean restoreAvailable( final String name, final Boolean purchaseable )
	{
		String type = RestoresDatabase.getType( name );
		
		if( type.equals( "item" ) )
		{
			int itemId = ItemDatabase.getItemId( name );
			if ( purchaseable )
			{
				return ( ItemDatabase.isTradeable( itemId ) || InventoryManager.getAccessibleCount( itemId ) > 0 );
			}
			return ( InventoryManager.getAccessibleCount( itemId ) > 0 );
		}
		if ( type.equals( "skill" ) )
		{
			return KoLCharacter.hasSkill( name );
		}
		if ( type.equals( "loc" ) )
		{
			if( name.equals( "A Relaxing Hot Tub" ) )
			{
				return InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0 &&
					( !KoLCharacter.inBadMoon() || KoLCharacter.kingLiberated() ) &&
					!Limitmode.limitClan();
			}
			if ( name.equals( "April Shower" ) )
			{
				return InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0 &&
					( !KoLCharacter.inBadMoon() || KoLCharacter.kingLiberated() ) &&
					StandardRequest.isAllowed( "Clan Item", "April Shower" ) &&
					!Limitmode.limitClan();
			}
			if ( name.equals( "Campground" ) )
			{
				return !Limitmode.limitCampground() && !KoLCharacter.isEd() && !KoLCharacter.inNuclearAutumn();
			}
			if ( name.equals( "Comfy Sofa" ) )
			{
				return !Limitmode.limitClan();
			}
			if ( name.contains( "Doc Galaktik's" ) )
			{
				return true;
			}
			if ( name.equals( "Free rests" ) )
			{
				return KoLCharacter.freeRestsAvailable() > 0;
			}
			if ( name.equals( "Nunnery (Frat Warrior)" ) )
			{
				return Preferences.getString( "sidequestNunsCompleted" ).equals( "fratboy" );
			}
			if ( name.equals( "Nunnery (War Hippy)" ) )
			{
				return Preferences.getString( "sidequestNunsCompleted" ).equals( "hippy" );
			}
		}
		return false;
	}

	public static final String[][] getRestoreData( final String level )
	{
		Iterator<String> it = RestoresDatabase.restoreNames.iterator();

		int restores = 0;
		int count = RestoresDatabase.restoreNames.size();
		
		if ( count > 0 )
		{
			String[][] restoreData = new String[ count ][ 7 ];
			while ( it.hasNext() )
			{
				String current = it.next();
				if ( ( level.equals( "available" ) && RestoresDatabase.restoreAvailable( current, false ) ) ||
					( level.equals( "obtainable" ) && RestoresDatabase.restoreAvailable( current, true ) ) ||
					level.equals( "all" ) )
				{
					restoreData[ restores ][ 0 ] = current;
					restoreData[ restores ][ 1 ] = RestoresDatabase.getType( current );
					restoreData[ restores ][ 2 ] = RestoresDatabase.getHPRange( current );
					restoreData[ restores ][ 3 ] = RestoresDatabase.getMPRange( current );
					restoreData[ restores ][ 4 ] = String.valueOf( RestoresDatabase.getAdvCost( current ) );
					int usesLeft = RestoresDatabase.getUsesLeft( current );
					restoreData[ restores ][ 5 ] = usesLeft == -1 ? "Unlimited" : Integer.toString( usesLeft );
					restoreData[ restores ][ 6 ] = RestoresDatabase.getNotes( current );
					restores++;
				}
			}
			return restoreData;
		}

		return null;
	}

	public static final boolean restoresMaxHP( final String restore )
	{
		return RestoresDatabase.hpMinByName.get( restore ).equals( "[HP]" );
	}

	public static final boolean restoresMaxMP( final String restore )
	{
		return RestoresDatabase.mpMinByName.get( restore ).equals( "[MP]" );
	}

	private static final boolean edSafe( final String hpRestore )
	{
		if ( hpRestore.equals( "cotton bandages" ) || hpRestore.equals( "linen bandages" ) || hpRestore.equals( "silk bandages" ) )
		{
			return true;
		}
		return false;
	}
}
