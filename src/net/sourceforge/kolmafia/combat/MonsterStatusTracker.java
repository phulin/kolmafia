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

package net.sourceforge.kolmafia.combat;


import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.MonsterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;


public class MonsterStatusTracker
{
	private static MonsterData monsterData = null;
	private static String lastMonsterName = "";

	private static int healthModifier = 0;
	private static int attackModifier = 0;
	private static int defenseModifier = 0;
	private static int healthManuel = 0;
	private static int attackManuel = 0;
	private static int defenseManuel = 0;
	private static boolean manuelFound = false;
	private static int originalHealth = 0;
	private static int originalAttack = 0;
	private static int originalDefense = 0;

	public static final void reset()
	{
		MonsterStatusTracker.healthModifier = 0;
		MonsterStatusTracker.attackModifier = 0;
		MonsterStatusTracker.defenseModifier = 0;
		MonsterStatusTracker.healthManuel = 0;
		MonsterStatusTracker.attackManuel = 0;
		MonsterStatusTracker.defenseManuel = 0;
		MonsterStatusTracker.manuelFound = false;
	}

	public static final MonsterData getLastMonster()
	{
		return MonsterStatusTracker.monsterData;
	}

	public static final String getLastMonsterName()
	{
		return MonsterStatusTracker.lastMonsterName;
	}

	public static final void setNextMonsterName( String monsterName )
	{
		MonsterStatusTracker.setNextMonsterName( monsterName, false );
	}

	public static final void setNextMonsterName( String monsterName, final boolean transformed )
	{
		MonsterStatusTracker.reset();

		MonsterStatusTracker.monsterData = MonsterDatabase.findMonster( monsterName );

		if ( MonsterStatusTracker.monsterData == null && EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getItemId() == ItemPool.SWORD_PREPOSITIONS )
		{
			monsterName = StringUtilities.lookupPrepositions( monsterName );
			MonsterStatusTracker.monsterData = MonsterDatabase.findMonster( monsterName );
		}

		if ( MonsterStatusTracker.monsterData == null )
		{
			if ( monsterName.startsWith( "the " ) || monsterName.startsWith( "The " ))
			{
				MonsterStatusTracker.monsterData = MonsterDatabase.findMonster( monsterName.substring( 4 ) );
				if ( MonsterStatusTracker.monsterData != null )
				{
					monsterName = monsterName.substring( 4 );
				}
			}
			else if ( monsterName.startsWith( "el " ) || monsterName.startsWith( "la " ) || monsterName.startsWith( "El " ) || monsterName.startsWith( "La " ) )
			{
				MonsterStatusTracker.monsterData = MonsterDatabase.findMonster( monsterName.substring( 3 ) );
				if ( MonsterStatusTracker.monsterData != null )
				{
					monsterName = monsterName.substring( 3 );
				}
			}
		}

		if ( MonsterStatusTracker.monsterData == null )
		{
			// Temporarily register the unknown monster so that
			// consult scripts can see it as such	
			MonsterStatusTracker.monsterData = MonsterDatabase.registerMonster( monsterName );
		}

		// If we saved an array of random modifiers, apply them
		MonsterStatusTracker.monsterData = MonsterStatusTracker.monsterData.handleRandomModifiers();
		MonsterStatusTracker.monsterData = MonsterStatusTracker.monsterData.handleMonsterLevel();
		if ( transformed )
		{
			MonsterStatusTracker.monsterData = MonsterStatusTracker.monsterData.transform();
		}

		MonsterStatusTracker.originalHealth = MonsterStatusTracker.monsterData.getHP();
		MonsterStatusTracker.originalAttack = MonsterStatusTracker.monsterData.getAttack();
		MonsterStatusTracker.originalDefense = MonsterStatusTracker.monsterData.getDefense();

		MonsterStatusTracker.lastMonsterName = monsterName;
	}

	public static final boolean dropsItem( int itemId )
	{
		if ( itemId == 0 || MonsterStatusTracker.monsterData == null )
		{
			return false;
		}

		AdventureResult item = ItemPool.get( itemId, 1 );

		return MonsterStatusTracker.monsterData.getItems().contains( item );
	}

	public static final boolean dropsItems( List<AdventureResult> items )
	{
		if ( items.isEmpty() || MonsterStatusTracker.monsterData == null )
		{
			return false;
		}

		return MonsterStatusTracker.monsterData.getItems().containsAll( items );
	}

	public static final boolean shouldSteal()
	{
		// If the user doesn't want smart pickpocket behavior, don't give it
		if ( !Preferences.getBoolean( "safePickpocket" ) )
		{
			return true;
		}

		if ( MonsterStatusTracker.monsterData == null )
		{
			return true;
		}

		return MonsterStatusTracker.monsterData.shouldSteal();
	}

	public static final int getMonsterHealth()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.originalHealth - MonsterStatusTracker.healthModifier;
	}

	public static final void healMonster( int amount )
	{
		MonsterStatusTracker.healthModifier -= amount;

		if ( MonsterStatusTracker.healthModifier < 0 )
		{
			MonsterStatusTracker.healthModifier = 0;
		}
	}

	public static final void damageMonster( int amount )
	{
		MonsterStatusTracker.healthModifier += amount;
	}

	public static final void resetAttackAndDefense()
	{
		MonsterStatusTracker.attackModifier = 0;
		MonsterStatusTracker.defenseModifier = 0;
	}

	public static final int getMonsterBaseAttack()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.monsterData.getAttack();
	}

	public static final int getMonsterAttack()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		int baseAttack = MonsterStatusTracker.originalAttack;
		int adjustedAttack = baseAttack + MonsterStatusTracker.attackModifier;
		return baseAttack == 0 ? adjustedAttack: Math.max( adjustedAttack, 1 );
	}

	public static final int getMonsterOriginalAttack()
	{
		return MonsterStatusTracker.monsterData == null  ? 0 : MonsterStatusTracker.originalAttack;
	}

	public static final Element getMonsterAttackElement()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return Element.NONE;
		}

		return MonsterStatusTracker.monsterData.getAttackElement();
	}

	public static final void lowerMonsterAttack( int amount )
	{
		MonsterStatusTracker.attackModifier -= amount;
	}

	public static final int getMonsterAttackModifier()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.attackModifier;
	}

	public static final boolean willUsuallyDodge()
	{
		return MonsterStatusTracker.willUsuallyDodge( 0 );
	}

	public static final boolean willUsuallyDodge( final int attackModifier )
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return false;
		}

		return MonsterStatusTracker.monsterData.willUsuallyDodge( MonsterStatusTracker.attackModifier + attackModifier );
	}

	public static final int getMonsterDefense()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		int baseDefense = MonsterStatusTracker.originalDefense;
		int adjustedDefense = baseDefense + MonsterStatusTracker.defenseModifier;
		return baseDefense == 0 ? adjustedDefense : Math.max( adjustedDefense, 1 );
	}

	public static final Element getMonsterDefenseElement()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return Element.NONE;
		}

		return MonsterStatusTracker.monsterData.getDefenseElement();
	}

	public static final Phylum getMonsterPhylum()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return Phylum.NONE;
		}

		return MonsterStatusTracker.monsterData.getPhylum();
	}

	public static final void lowerMonsterDefense( int amount )
	{
		MonsterStatusTracker.defenseModifier -= amount;
	}

	public static final int getMonsterDefenseModifier()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.defenseModifier;
	}

	public static final boolean willUsuallyMiss()
	{
		return MonsterStatusTracker.willUsuallyMiss( 0 );
	}

	public static final boolean willUsuallyMiss( final int defenseModifier )
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return false;
		}

		return MonsterStatusTracker.monsterData.willUsuallyMiss( MonsterStatusTracker.defenseModifier + defenseModifier );
	}

	public static final int getMonsterRawInitiative()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.monsterData.getRawInitiative();
	}

	public static final int getMonsterInitiative()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.monsterData.getInitiative();
	}

	public static final int getJumpChance()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.monsterData.getJumpChance();
	}

	public static int getPoisonLevel()
	{
		if ( MonsterStatusTracker.monsterData == null )
		{
			return 0;
		}

		return MonsterStatusTracker.monsterData.getPoison();
	}

	public static void setManuelStats( int attack, int defense, int hp )
	{
		// Save what Manuel reported. These are the stats at the END of
		// the round's actions - including those which automatically
		// fired on round 0 before the player did anything.
		MonsterStatusTracker.attackManuel = attack;
		MonsterStatusTracker.defenseManuel = defense;
		MonsterStatusTracker.healthManuel = hp;

		// If we don't know anything about this monster, assume that
		// Manuel is showing the original stats - even though, as
		// described above, that's not always the case.
		if ( !manuelFound && MonsterStatusTracker.originalAttack == 0 )
		{
			MonsterStatusTracker.originalAttack = attack;
			MonsterStatusTracker.originalDefense = defense;
			MonsterStatusTracker.originalHealth = hp;
		}

		MonsterStatusTracker.manuelFound = true;
	}

	public static void applyManuelStats()
	{
		if ( manuelFound )
		{
			MonsterStatusTracker.attackModifier = MonsterStatusTracker.attackManuel - MonsterStatusTracker.originalAttack;
			MonsterStatusTracker.defenseModifier = MonsterStatusTracker.defenseManuel - MonsterStatusTracker.originalDefense;
			MonsterStatusTracker.healthModifier = MonsterStatusTracker.originalHealth - MonsterStatusTracker.healthManuel;
		}
	}
}
