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

package net.sourceforge.kolmafia;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;

import net.sourceforge.kolmafia.listener.NamedListenerRegistry;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.RichardRequest;
import net.sourceforge.kolmafia.request.SpelunkyRequest;
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.GuildUnlockManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.Limitmode;

import net.sourceforge.kolmafia.swingui.GenericFrame;

import net.sourceforge.kolmafia.textui.command.CouncilCommand;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class KoLAdventure
	implements Comparable<KoLAdventure>, Runnable
{
	public static final String[][] DEMON_TYPES =
	{
		{ "Summoning Chamber", "Pies" },
		{ "Spooky Forest", "Preternatural Greed" },
		{ "Sonofa Beach", "Fit To Be Tide" },
		{ "Deep Fat Friars' Gate", "Big Flaming Whip" },
		{ "Haunted Bathroom", "Demonic Taint" },
		{ null, "pile of smoking rags" },
		{ null, "Drinks" },
		{ "Nemesis' Lair", "Existential Torment" },
		{ "Sinister Ancient Tablet", "Burning, Man" },
		{ "Strange Cube", "The Pleasures of the Flesh" },
		{ "Battlefield", "Infernal Thirst" },
		{ null, "Jacked In" },
	};

	public static final AdventureResult BEATEN_UP = EffectPool.get( EffectPool.BEATEN_UP, 4 );
	public static final AdventureResult PERFUME = EffectPool.get( EffectPool.KNOB_GOBLIN_PERFUME, 1 );

	public static KoLAdventure lastVisitedLocation = null;
	public static boolean locationLogged = false;
	public static String lastLocationName = null;
	public static String lastLocationURL = null;

	private boolean isValidAdventure = false;
	private boolean hasWanderers = false;
	private final String zone, parentZone, adventureId, formSource, adventureName, environment;
	private final int recommendedStat, waterLevel;

	private final String normalString, lowercaseString, parentZoneDescription;

	private GenericRequest request;
	private final AreaCombatData areaSummary;
	private final boolean isNonCombatsOnly;

	private static final Pattern ADVENTURE_AGAIN = Pattern.compile( "<a href=\"([^\"]*)\">Adventure Again \\((.*?)\\)</a>" );
	private static final HashSet<String> unknownAdventures = new HashSet<String>();

	/**
	 * Constructs a new <code>KoLAdventure</code> with the given specifications.
	 *
	 * @param formSource The form associated with the given adventure
	 * @param adventureId The identifier for this adventure, relative to its form
	 * @param adventureName The string form, or name of this adventure
	 */

	public KoLAdventure( final String zone, final String formSource, final String adventureId, final String adventureName )
	{
		this.formSource = formSource;
		this.adventureId = adventureId;

		this.zone = zone;
		this.adventureName = adventureName;

		this.normalString = this.zone + ": " + this.adventureName;
		this.lowercaseString = this.normalString.toLowerCase();

		this.parentZone = (String) AdventureDatabase.PARENT_ZONES.get( zone );
		this.parentZoneDescription = (String) AdventureDatabase.ZONE_DESCRIPTIONS.get( this.parentZone );

		this.environment = AdventureDatabase.getEnvironment( adventureName );

		this.recommendedStat = AdventureDatabase.getRecommendedStat( adventureName );

		this.waterLevel = AdventureDatabase.getWaterLevel( adventureName );

		this.hasWanderers = AdventureDatabase.hasWanderers( adventureName, formSource.equals( "adventure.php" ) );

		if ( formSource.equals( "dwarffactory.php" ) )
		{
			this.request = new DwarfFactoryRequest( "ware" );
		}
		else if ( formSource.equals( "clan_gym.php" ) )
		{
			this.request = new ClanRumpusRequest( ClanRumpusRequest.RequestType.fromString( adventureId ) );
		}
		else if ( formSource.equals( "clan_hobopolis.php" ) )
		{
			this.request = new RichardRequest( StringUtilities.parseInt( adventureId ) );
		}
		else if ( formSource.equals( "basement.php" ) )
		{
			this.request = new BasementRequest( adventureName );
		}
		else
		{
			this.request = new AdventureRequest( adventureName, formSource, adventureId );
		}

		this.areaSummary = AdventureDatabase.getAreaCombatData( adventureName );

		if ( adventureId == null )
		{
			this.isNonCombatsOnly = false;
		}
		else if ( this.areaSummary != null )
		{
			this.isNonCombatsOnly = this.areaSummary.combats() == 0 && this.areaSummary.getMonsterCount() == 0 &&
							this.areaSummary.getSuperlikelyMonsterCount() == 0;
		}
		else
		{
			this.isNonCombatsOnly = !( this.request instanceof AdventureRequest );
		}
	}

	public String toLowerCaseString()
	{
		return this.lowercaseString;
	}

	/**
	 * Returns the form source for this adventure.
	 */

	public String getFormSource()
	{
		return this.formSource;
	}

	/**
	 * Returns the name where this zone is found.
	 */

	public String getZone()
	{
		return this.zone;
	}

	public String getParentZone()
	{
		return this.parentZone;
	}

	public String getParentZoneDescription()
	{
		return this.parentZoneDescription;
	}

	public String getEnvironment()
	{
		return this.environment;
	}

	public int getRecommendedStat()
	{
		return this.recommendedStat;
	}

	public int getWaterLevel()
	{
		return this.waterLevel;
	}

	public boolean hasWanderers()
	{
		return this.hasWanderers;
	}

	/**
	 * Returns the name of this adventure.
	 */

	public String getAdventureName()
	{
		return this.adventureName;
	}

	public String getPrettyAdventureName( final String urlString )
	{
		if ( urlString.contains( "pyramid_state" ) )
		{
			return PyramidRequest.getPyramidLocationString( urlString );
		}
		if ( urlString.startsWith( "basement.php" ) )
		{
			return BasementRequest.getBasementLevelName();
		}
		if ( urlString.startsWith( "cellar.php" ) )
		{
			return TavernRequest.cellarLocationString( urlString );
		}
		// *** could do something with barrel.php here
		return this.adventureName;
	}

	// In chamber <b>#1</b> of the Daily Dungeon, you encounter ...
	// In the <b>5th</b> chamber of the Daily Dungeon, you encounter ...
	private static final Pattern DAILY_DUNGEON_CHAMBER = Pattern.compile( "<b>#?([\\d]+)(?:th)?</b>" );

	public static String getPrettyAdventureName( final String locationName, final String responseText )
	{
		if ( locationName.equals( "The Daily Dungeon" ) )
		{
			// Parse room number from responseText
			Matcher matcher = KoLAdventure.DAILY_DUNGEON_CHAMBER.matcher( responseText );
			if ( matcher.find() )
			{
				String room = matcher.group( 1 );
				return locationName + " (Room " + room + ")"; 
			}
		}
		return locationName;
	}

	/**
	 * Returns the adventure Id for this adventure.
	 *
	 * @return The adventure Id for this adventure
	 */

	public String getAdventureId()
	{
		return this.adventureId;
	}

	public AreaCombatData getAreaSummary()
	{
		return this.areaSummary;
	}

	public boolean isNonCombatsOnly()
	{
		return this.isNonCombatsOnly && !KoLAdventure.hasWanderingMonsters( this.formSource, this.adventureId );
	}

	public static final boolean hasWanderingMonsters( final String urlString, final String adventureId )
	{
		if ( !urlString.startsWith( "adventure.php" ) )
		{
			return false;
		}

		int advId = Integer.parseInt( adventureId );
		if ( advId == AdventurePool.THE_SHORE  ||
		     advId == AdventurePool.TRAINING_SNOWMAN ||
		     advId == AdventurePool.DIRE_WARREN ||
		     ( advId >= AdventurePool.GINGERBREAD_CIVIC && advId <= AdventurePool.GINGERBREAD_SEWERS ) )
		{
			return false;
		}

		// Romantic targets.

		String romanticTarget = Preferences.getString( "romanticTarget" );

		if ( romanticTarget != null && !romanticTarget.equals( "" ) )
		{
			return true;
		}

		// Holidays.

		String holiday = HolidayDatabase.getHoliday();

		if ( holiday.contains( "El Dia De Los Muertos Borrachos" ) ||
		     holiday.contains( "Feast of Boris" ) ||
		     holiday.contains( "Talk Like a Pirate Day" ) )
		{
			return true;
		}

		// Nemesis assassins.

		if ( !InventoryManager.hasItem( ItemPool.VOLCANO_MAP ) && QuestDatabase.isQuestLaterThan( Quest.NEMESIS, "step16" ) )
		{
			return true;
		}

		// Beecore.

		if ( KoLCharacter.inBeecore() )
		{
			return true;
		}

		// Mini-Hipster and Artistic Goth Kid

		FamiliarData familiar = KoLCharacter.getFamiliar();
		if ( familiar != null && ( familiar.getId() == FamiliarPool.HIPSTER || familiar.getId() == FamiliarPool.ARTISTIC_GOTH_KID ) &&
		     Preferences.getInteger( "_hipsterAdv" ) < 7 )
		{
			return true;
		}

		return false;
	}

	/**
	 * Returns the request associated with this adventure.
	 *
	 * @return The request for this adventure
	 */

	public GenericRequest getRequest()
	{
		return this.request;
	}

	public void overrideAdventuresUsed( int used )
	{
		if ( this.request instanceof AdventureRequest )
		{
			((AdventureRequest) this.request).overrideAdventuresUsed( used );
		}
	}

	/**
	 * Checks the map location of the given zone. This is to ensure that
	 * KoLmafia arms any needed flags (such as for the beanstalk).
	 */

	private void validate( boolean visitedCouncil )
	{
		this.isValidAdventure = false;

		if ( Limitmode.limitZone( this.zone ) )
		{
			return;
		}

		if ( this.zone.equals( "Astral" ) )
		{
			// Update the choice adventure setting

			if ( this.adventureId.equals( AdventurePool.BAD_TRIP_ID ) )
			{
				Preferences.setString( "choiceAdventure71", "1" );
			}
			else if ( this.adventureId.equals( AdventurePool.MEDIOCRE_TRIP_ID ) )
			{
				Preferences.setString( "choiceAdventure71", "2" );
			}
			else
			{
				Preferences.setString( "choiceAdventure71", "3" );
			}

			// If the player is not half-astral, then
			// make sure they are before continuing.

			AdventureResult effect = EffectPool.get( EffectPool.HALF_ASTRAL );
			if ( !KoLConstants.activeEffects.contains( effect ) )
			{
				AdventureResult mushroom = ItemPool.get( ItemPool.ASTRAL_MUSHROOM, 1 );
				this.isValidAdventure = InventoryManager.retrieveItem( mushroom );
				if ( this.isValidAdventure )
				{
					RequestThread.postRequest( UseItemRequest.getInstance( mushroom ) );
				}
			}

			this.isValidAdventure = KoLConstants.activeEffects.contains( effect );
			return;
		}

		// Fighting the Goblin King requires effects

		if ( this.formSource.equals( "cobbsknob.php" ) )
		{
			// You have two choices:

			// - Wear harem girl outfit and have Perfume effect
			// - Wear elite guard uniform and have cake

			// Assume that if you are currently wearing an outfit,
			// you want to use that method.
			if ( EquipmentManager.isWearingOutfit( OutfitPool.HAREM_OUTFIT ) )
			{
				// Harem girl
				if ( KoLConstants.activeEffects.contains( KoLAdventure.PERFUME ) )
				{
					// We are wearing the outfit and have the effect. Good to go!
					this.isValidAdventure = true;
					return;
				}

				if ( !KoLCharacter.inBeecore() &&
				     InventoryManager.retrieveItem( ItemPool.KNOB_GOBLIN_PERFUME ) )
				{
					// If we are in Beecore, we have to adventure to get
					// the effect. Otherwise, we can use the item
					RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.KNOB_GOBLIN_PERFUME ) );
					this.isValidAdventure = true;
					return;
				}
			}
			else if ( EquipmentManager.isWearingOutfit( OutfitPool.KNOB_ELITE_OUTFIT ) )
			{
				// Elite Guard
				if ( InventoryManager.retrieveItem( ItemPool.KNOB_CAKE ) )
				{
					// We are wearing the outfit and have the cake. Good to go!
					this.isValidAdventure = true;
					return;
				}
			}

			// If we get here, we are not currently wearing an
			// appropriate outfit or, if we are, we can't use it.
			// See what we have.

			int outfitId = OutfitPool.NONE;

			// Using the harem girl outfit requires only two pieces and a perfume.
			// Using the elite guard outfit requires three pieces and Fancy cooking.
			// Check for the harem girl first.

			if ( EquipmentManager.hasOutfit( OutfitPool.HAREM_OUTFIT ) &&
			     ( KoLConstants.activeEffects.contains( KoLAdventure.PERFUME ) ||
			       ( !KoLCharacter.inBeecore() &&
				 InventoryManager.retrieveItem( ItemPool.KNOB_GOBLIN_PERFUME ) ) ) )
			{
				// We have the harem girl outfit and either
				// have the effect or, if we are not in
				// Beecore, have a perfume.
				outfitId = OutfitPool.HAREM_OUTFIT;
			}
			else if ( EquipmentManager.hasOutfit( OutfitPool.KNOB_ELITE_OUTFIT ) &&
				  InventoryManager.retrieveItem( ItemPool.KNOB_CAKE ) )
			{
				// We have the elite guard uniform and have
				// made a cake.
				outfitId = OutfitPool.KNOB_ELITE_OUTFIT;
			}
			else
			{
				return;
			}

			// Wear the selected outfit.
			SpecialOutfit outfit = EquipmentDatabase.getOutfit( outfitId );
			RequestThread.postRequest( new EquipmentRequest( outfit ) );

			// If we selected the harem girl outfit, use a perfume
			if ( outfitId == OutfitPool.HAREM_OUTFIT && !KoLConstants.activeEffects.contains( KoLAdventure.PERFUME ) )
			{
				RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.KNOB_GOBLIN_PERFUME ) );
			}

			this.isValidAdventure = true;
			return;
		}

		if ( this.zone.equals( "Lab" ) )
		{
			this.isValidAdventure = InventoryManager.hasItem( ItemPool.get( ItemPool.LAB_KEY, 1 ) );
			return;
		}

		if ( this.zone.equals( "Menagerie" ) )
		{
			this.isValidAdventure = InventoryManager.hasItem( ItemPool.get( ItemPool.MENAGERIE_KEY, 1 ) );
			return;
		}

		// Adventuring in the mine warehouse or office requires either
		// Mining Gear or the Dwarvish War Uniform

		if ( this.formSource.equals( "dwarffactory.php" ) || this.adventureId.equals( AdventurePool.MINE_OFFICE_ID ) )
		{
			int id1 = OutfitPool.MINING_OUTFIT;
			int id2 = OutfitPool.DWARVISH_UNIFORM;

			if ( !EquipmentManager.isWearingOutfit( id1 ) && !EquipmentManager.isWearingOutfit( id2 ) )
			{
				SpecialOutfit outfit =
					EquipmentManager.hasOutfit( id1 ) ? EquipmentDatabase.getOutfit( id1 ) :
					EquipmentManager.hasOutfit( id2 ) ? EquipmentDatabase.getOutfit( id2 ) :
					null;

				if ( outfit == null )
				{
					return;
				}

				RequestThread.postRequest( new EquipmentRequest( outfit ) );
			}

			this.isValidAdventure = true;
			return;
		}

		// Disguise zones require outfits
		if ( !this.adventureId.equals( AdventurePool.COLA_BATTLEFIELD_ID ) &&
		     ( this.adventureName.contains( "Disguise" ) || this.adventureName.contains( "Uniform" ) ) )
		{
			int outfitId = EquipmentDatabase.getOutfitId( this );

			if ( outfitId > 0 && !EquipmentManager.isWearingOutfit( outfitId ) )
			{
				SpecialOutfit outfit = EquipmentDatabase.getOutfit( outfitId );
				if ( !EquipmentManager.retrieveOutfit( outfit ) )
				{
					return;
				}

				RequestThread.postRequest( new EquipmentRequest( outfit ) );
			}
			this.isValidAdventure = true;
			return;
		}

		// If the person has a continuum transfunctioner, then find
		// some way of equipping it.  If they do not have one, then
		// acquire one then try to equip it.

		if ( this.adventureId.equals( AdventurePool.PIXEL_REALM_ID ) || this.zone.equals( "Vanya's Castle" ) )
		{
			AdventureResult transfunctioner = ItemPool.get( ItemPool.TRANSFUNCTIONER, 1 );
			if ( !InventoryManager.hasItem( transfunctioner ) )
			{
				RequestThread.postRequest( new PlaceRequest( "forestvillage", "fv_mystic" ) );
				GenericRequest pixelRequest = new GenericRequest( "choice.php?whichchoice=664&option=1" );
				// The early steps cannot be skipped
				RequestThread.postRequest( pixelRequest );
				RequestThread.postRequest( pixelRequest );
				RequestThread.postRequest( pixelRequest );
			}

			if ( !KoLCharacter.hasEquipped( transfunctioner) )
			{
				RequestThread.postRequest( new EquipmentRequest( transfunctioner ) );
			}
			this.isValidAdventure = true;
			return;
		}

		if ( this.adventureId.equals( AdventurePool.PALINDOME_ID ) )
		{
			AdventureResult talisman = ItemPool.get( ItemPool.TALISMAN, 1 );
			if ( !KoLCharacter.hasEquipped( talisman ) )
			{
				if ( !InventoryManager.hasItem( talisman ) )
				{
					return;
				}

				RequestThread.postRequest( new EquipmentRequest( talisman ) );
			}

			if ( QuestDatabase.isQuestLaterThan( Quest.PALINDOME, QuestDatabase.UNSTARTED ) )
			{
				this.isValidAdventure = true;
				return;
			}

			GenericRequest request = new PlaceRequest( "plains" );
			RequestThread.postRequest( request );
			this.isValidAdventure = request.responseText.contains( "palinlink.gif" ) ;
			return;
		}

		if ( this.adventureId.equals( AdventurePool.HOBOPOLIS_SEWERS_ID ) )
		{
			// Don't auto-adventure unprepared in Hobopolis sewers

			this.isValidAdventure = !Preferences.getBoolean( "requireSewerTestItems" ) ||
				( KoLCharacter.hasEquipped( ItemPool.get( ItemPool.GATORSKIN_UMBRELLA, 1 ) ) &&
					KoLCharacter.hasEquipped( ItemPool.get( ItemPool.HOBO_CODE_BINDER, 1 ) ) &&
					InventoryManager.retrieveItem( ItemPool.SEWER_WAD ) &&
					InventoryManager.retrieveItem( ItemPool.OOZE_O ) &&
					InventoryManager.retrieveItem( ItemPool.DUMPLINGS ) &&
					InventoryManager.retrieveItem( ItemPool.OIL_OF_OILINESS, 3 ) );
			return;
		}

		if ( this.adventureId.equals( AdventurePool.HIDDEN_TEMPLE_ID ) )
		{
			boolean unlocked = KoLCharacter.getTempleUnlocked();
			if ( !unlocked )
			{
				// Visit the distant woods and take a look.
				RequestThread.postRequest( new GenericRequest( "woods" ) );
				unlocked = KoLCharacter.getTempleUnlocked();
			}

			this.isValidAdventure = unlocked;
			return;
		}

		if ( this.adventureId.equals( AdventurePool.SHROUDED_PEAK_ID ) )
		{
			String trapper = Preferences.getString( Quest.TRAPPER.getPref() );
			this.isValidAdventure = trapper.equals( "step3" ) || trapper.equals( "step4" );
			if ( Preferences.getString( "peteMotorbikeTires" ).equals( "Snow Tires" ) )
			{
				this.isValidAdventure = true;
				return;
			}
			if ( !this.isValidAdventure )
			{
				return;
			}

			this.isValidAdventure = KoLCharacter.getElementalResistanceLevels( Element.COLD ) > 4;
			return;
		}

		if ( this.adventureId.equals( AdventurePool.LOWER_CHAMBER_ID ) )
		{
			this.isValidAdventure = Preferences.getBoolean( "lowerChamberUnlock" );
			return;
		}

		if ( this.adventureId.equals( AdventurePool.SUMMONING_CHAMBER_ID ) )
		{
			this.isValidAdventure = QuestDatabase.isQuestLaterThan( Quest.MANOR, "step2" );
			return;
		}

		if ( this.adventureId.equals( AdventurePool.ELDRITCH_FISSURE_ID ) )
		{
			this.isValidAdventure = Preferences.getBoolean( "eldritchFissureAvailable" );
			return;
		}

		if ( this.adventureId.equals( AdventurePool.ELDRITCH_HORROR_ID ) )
		{
			this.isValidAdventure = Preferences.getBoolean( "eldritchHorrorAvailable" );
			return;
		}

		if ( !this.formSource.contains( "adventure.php" ) )
		{
			this.isValidAdventure = true;
			return;
		}

		if ( this.isValidAdventure )
		{
			return;
		}

		// If we're trying to take a trip, make sure it's the right one
		if ( this.zone.equals( "Astral" ) )
		{
			// You must be Half-Astral to go on a trip

			AdventureResult effect = EffectPool.get( EffectPool.HALF_ASTRAL );
			int astral = effect.getCount( KoLConstants.activeEffects );
			if ( astral == 0 )
			{
				RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.ASTRAL_MUSHROOM ) );
				if ( !KoLmafia.permitsContinue() )
				{
					this.isValidAdventure = false;
					return;
				}
			}

			// If we haven't selected a trip yet, do so now
			if ( astral == 5 )
			{
				String choice;
				if ( this.adventureId.equals( AdventurePool.BAD_TRIP_ID ) )
				{
					choice = "1";
				}
				else if ( this.adventureId.equals( AdventurePool.MEDIOCRE_TRIP_ID ) )
				{
					choice = "2";
				}
				else
				{
					choice = "3";
				}

				// Choose the trip
				Preferences.setString( "choiceAdventure71", choice );

				String name = this.getAdventureName();
				Preferences.setString( "chosenTrip", name );

				this.isValidAdventure = true;
				return;
			}

			this.isValidAdventure = true;
			return;
		}

		// The casino is unlocked provided the player
		// has a casino pass in their inventory.

		if ( this.zone.equals( "Casino" ) )
		{
			this.isValidAdventure = InventoryManager.retrieveItem( ItemPool.CASINO_PASS );
			return;
		}

		// The island is unlocked provided the player
		// has a dingy dinghy in their inventory.

		if ( this.zone.equals( "Island" ) )
		{
			this.isValidAdventure = KoLCharacter.mysteriousIslandAccessible();
			if ( this.isValidAdventure )
			{
				return;
			}

			this.isValidAdventure = InventoryManager.hasItem( ItemPool.DINGHY_PLANS );
			if ( !this.isValidAdventure )
			{
				return;
			}

			this.isValidAdventure = InventoryManager.hasItem( ItemPool.DINGY_PLANKS );
			if ( !this.isValidAdventure )
			{
				return;
			}

			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.DINGHY_PLANS ) );

			return;
		}

		// The dungeons of doom are only available if you've finished the quest

		if ( this.adventureId.equals( AdventurePool.DUNGEON_OF_DOOM_ID ) )
		{
			this.isValidAdventure = QuestLogRequest.isDungeonOfDoomAvailable();
			return;
		}

		// The Castle Basement is unlocked provided the player has the S.O.C.K
		// (legacy: rowboats give access but are no longer creatable)

		if ( this.adventureId.equals( AdventurePool.CASTLE_BASEMENT_ID ) )
		{
			this.isValidAdventure =
				InventoryManager.hasItem( ItemPool.get( ItemPool.SOCK, 1 ) ) ||
				InventoryManager.hasItem( ItemPool.get( ItemPool.ROWBOAT, 1 ) );
			return;
		}

		if ( this.adventureId.equals( AdventurePool.CASTLE_GROUND_ID ) )
		{
			this.isValidAdventure = Preferences.getInteger( "lastCastleGroundUnlock" ) == KoLCharacter.getAscensions();
			return;
		}

		if ( this.adventureId.equals( AdventurePool.CASTLE_TOP_ID ) )
		{
			this.isValidAdventure = Preferences.getInteger( "lastCastleTopUnlock" ) == KoLCharacter.getAscensions();
			return;
		}

		// The Hole in the Sky is unlocked provided the player has a steam-powered rocketship
		// (legacy: rowboats give access but are no longer creatable)

		if ( this.adventureId.equals( AdventurePool.HOLE_IN_THE_SKY_ID ) )
		{
			this.isValidAdventure =
				InventoryManager.hasItem( ItemPool.get( ItemPool.ROCKETSHIP, 1 ) ) ||
				InventoryManager.hasItem( ItemPool.get( ItemPool.ROWBOAT, 1 ) );
			return;
		}

		// The beanstalk is unlocked when the player has planted a
		// beanstalk -- but, the bean needs to be planted first.

		if ( this.adventureId.equals( AdventurePool.AIRSHIP_ID ) && !QuestDatabase.isQuestLaterThan( Quest.GARBAGE, QuestDatabase.STARTED ) )
		{
			// If the character is not at least level 10, they have
			// no chance to get to the beanstalk
			if ( KoLCharacter.getLevel() < 10 )
			{
				this.isValidAdventure = false;
				return;
			}

			GenericRequest request = new PlaceRequest( "plains" );
			RequestThread.postRequest( request );

			if ( !request.responseText.contains( "place.php?whichplace=beanstalk" ) )
			{
				// We see no beanstalk in the Nearby Plains.
				// Acquire an enchanted bean and plant it.
				if ( !KoLAdventure.getEnchantedBean() )
				{
					this.isValidAdventure = false;
					return;
				}

				// Make sure the Council has given you the quest
				if ( !QuestDatabase.isQuestLaterThan( Quest.GARBAGE, "unstarted" ) )
				{
					RequestThread.postRequest( CouncilCommand.COUNCIL_VISIT );
				}

				// Use the enchanted bean by clicking on the coffee grounds.
				RequestThread.postRequest( new PlaceRequest( "plains", "garbage_grounds" ) );
			}
			this.isValidAdventure = true;

			return;
		}

		if ( this.adventureId.equals( AdventurePool.HAUNTED_KITCHEN_ID ) ||
		     this.adventureId.equals( AdventurePool.HAUNTED_CONSERVATORY_ID ) )
		{
			// Haunted Kitchen & Conservatory
			this.isValidAdventure = QuestDatabase.isQuestLaterThan( Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.UNSTARTED );
			return;
		}
		if ( this.adventureId.equals( AdventurePool.HAUNTED_LIBRARY_ID ) )
		{
			// Haunted Library
			this.isValidAdventure = InventoryManager.hasItem( ItemPool.LIBRARY_KEY );
			return;
		}
		if ( this.adventureId.equals( AdventurePool.HAUNTED_BILLIARDS_ROOM_ID ) )
		{
			// Haunted Billiards Room
			this.isValidAdventure = InventoryManager.hasItem( ItemPool.BILLIARDS_KEY );
			return;
		}
		if ( this.adventureId.equals( AdventurePool.HAUNTED_BATHROOM_ID ) ||
		     this.adventureId.equals( AdventurePool.HAUNTED_BEDROOM_ID ) ||
		     this.adventureId.equals( AdventurePool.HAUNTED_GALLERY_ID ) )
		{
			// Haunted Bathroom, Bedroom & Gallery
			this.isValidAdventure = QuestDatabase.isQuestLaterThan( Quest.SPOOKYRAVEN_DANCE, QuestDatabase.STARTED );
			return;
		}
		if ( this.adventureId.equals( AdventurePool.HAUNTED_BALLROOM_ID ) )
		{
			// Haunted Ballroom
			this.isValidAdventure = QuestDatabase.isQuestLaterThan( Quest.SPOOKYRAVEN_DANCE, "step2" );
			return;
		}
		if ( this.adventureId.equals( AdventurePool.HAUNTED_LABORATORY_ID ) ||
		     this.adventureId.equals( AdventurePool.HAUNTED_NURSERY_ID ) ||
		     this.adventureId.equals( AdventurePool.HAUNTED_STORAGE_ROOM_ID ) )
		{
			// Haunted Lab, Nursery & Storage Room
			this.isValidAdventure = QuestDatabase.isQuestLaterThan( Quest.SPOOKYRAVEN_DANCE, "step3" );
			return;
		}

		if ( this.adventureId.equals( AdventurePool.MIDDLE_CHAMBER_ID ) )
		{
			this.isValidAdventure = Preferences.getBoolean( "middleChamberUnlock" );
			return;
		}

		if ( this.adventureId.equals( AdventurePool.BATRAT_ID ) ||
		     this.adventureId.equals( AdventurePool.BEANBAT_ID ) ||
		     this.adventureId.equals( AdventurePool.BOSSBAT_ID ) )
		{
			if ( this.adventureId.equals( AdventurePool.BATRAT_ID ) && QuestDatabase.isQuestLaterThan( Quest.BAT, QuestDatabase.STARTED ) )
			{
				this.isValidAdventure = true;
				return;
			}

			if ( this.adventureId.equals( AdventurePool.BEANBAT_ID ) && QuestDatabase.isQuestLaterThan( Quest.BAT, "step1" ) )
			{
				this.isValidAdventure = true;
				return;
			}

			if ( this.adventureId.equals( AdventurePool.BOSSBAT_ID ) && QuestDatabase.isQuestLaterThan( Quest.BAT, "step2" ) )
			{
				this.isValidAdventure = true;
				return;
			}

			int sonarToUse = 0;

			if ( Preferences.getString( Quest.BAT.getPref() ).equals( QuestDatabase.STARTED ) )
			{
				sonarToUse = 3;
			}
			else if ( Preferences.getString( Quest.BAT.getPref() ).equals( "step1" ) )
			{
				sonarToUse = 2;
			}
			else if ( Preferences.getString( Quest.BAT.getPref() ).equals( "step2" ) )
			{
				sonarToUse = 1;
			}

			AdventureResult sonar = ItemPool.get( ItemPool.SONAR, 1 );
			sonarToUse = Math.min( sonarToUse, sonar.getCount( KoLConstants.inventory ) );

			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.get( ItemPool.SONAR, sonarToUse ) ) );

			GenericRequest request = new PlaceRequest( "bathole" );
			RequestThread.postRequest( request );

			this.isValidAdventure =
				this.adventureId.equals( AdventurePool.BATRAT_ID ) ?
				request.responseText.contains( "batrockleft.gif" ) :
				this.adventureId.equals( AdventurePool.BEANBAT_ID ) ?
				request.responseText.contains( "batrockright.gif" ) :
				request.responseText.contains( "batrockbottom.gif" );

			return;
		}

		if ( this.adventureId.equals( AdventurePool.WHITEYS_GROVE_ID ) )
		{
			if ( !Preferences.getString( Quest.CITADEL.getPref() ).equals( "unstarted" ) ||
			     ( QuestDatabase.isQuestLaterThan( Quest.PALINDOME, "step2" ) && !KoLCharacter.isEd() ) )
			{
				this.isValidAdventure = true;
				return;
			}

			GenericRequest request = new GenericRequest( "woods.php" );
			RequestThread.postRequest( request );
			this.isValidAdventure = request.responseText.contains( "grove.gif" );

			if ( !visitedCouncil && !this.isValidAdventure )
			{
				GuildUnlockManager.unlockGuild();
				this.validate( true );
			}

			return;
		}

		if ( this.zone.equals( "McLarge" ) )
		{
			if ( this.adventureId.equals( AdventurePool.MINE_OFFICE_ID ) )
			{
				this.isValidAdventure = true;
				return;
			}
			if ( this.adventureId.equals( AdventurePool.ITZNOTYERZITZ_MINE_ID ) || 
			     this.adventureId.equals( AdventurePool.GOATLET_ID ) )
			{
				this.isValidAdventure = QuestDatabase.isQuestLaterThan( Quest.TRAPPER, QuestDatabase.STARTED );
			}
			else if ( this.adventureId.equals( AdventurePool.NINJA_SNOWMEN_ID ) ||
			          this.adventureId.equals( AdventurePool.EXTREME_SLOPE_ID ) )
			{
				this.isValidAdventure = QuestDatabase.isQuestLaterThan( Quest.TRAPPER, "step1" );
			}
			else if ( this.adventureId.equals( AdventurePool.ICY_PEAK_ID ) )
			{
				this.isValidAdventure = QuestDatabase.isQuestFinished( Quest.TRAPPER );
			}
			else
			{
				this.isValidAdventure = true;
			}

			if ( this.isValidAdventure )
			{
				return;
			}

			if ( !this.isValidAdventure && visitedCouncil )
			{
				KoLmafia.updateDisplay( "You must complete a trapper task first." );
				return;
			}

			RequestThread.postRequest( CouncilCommand.COUNCIL_VISIT );
			RequestThread.postRequest( new PlaceRequest( "mclargehuge", "trappercabin" ) );

			this.validate( true );
			return;
		}

		if ( this.zone.equals( "Highlands" ) )
		{
			if ( QuestDatabase.isQuestLaterThan( Quest.TOPPING, QuestDatabase.STARTED ) )
			{
				this.isValidAdventure = true;
				return;
			}

			return;
		}

		if ( this.adventureId.equals( AdventurePool.ICE_HOTEL_ID ) ||
		     this.adventureId.equals( AdventurePool.VYKEA_ID ) ||
		     this.adventureId.equals( AdventurePool.ICE_HOLE_ID ) )
		{
			boolean unlocked = Preferences.getBoolean( "coldAirportAlways" ) || Preferences.getBoolean( "_coldAirportToday" );
			if ( !unlocked )
			{
				// Visit the airport and take a look.
				RequestThread.postRequest( new PlaceRequest( "airport" ) );
				unlocked = Preferences.getBoolean( "coldAirportAlways" ) || Preferences.getBoolean( "_coldAirportToday" );
			}

			this.isValidAdventure = unlocked;
			return;
		}

		if ( this.adventureId.equals( AdventurePool.SMOOCH_ARMY_HQ_ID ) ||
		     this.adventureId.equals( AdventurePool.VELVET_GOLD_MINE_ID ) ||
		     this.adventureId.equals( AdventurePool.LAVACO_LAMP_FACTORY_ID ) ||
		     this.adventureId.equals( AdventurePool.BUBBLIN_CALDERA_ID ) )
		{
			boolean unlocked = Preferences.getBoolean( "hotAirportAlways" ) || Preferences.getBoolean( "_hotAirportToday" );
			if ( !unlocked )
			{
				// Visit the airport and take a look.
				RequestThread.postRequest( new PlaceRequest( "airport" ) );
				unlocked = Preferences.getBoolean( "hotAirportAlways" ) || Preferences.getBoolean( "_hotAirportToday" );
			}

			this.isValidAdventure = unlocked;
			return;
		}

		if ( this.adventureId.equals( AdventurePool.FUN_GUY_MANSION_ID ) ||
		     this.adventureId.equals( AdventurePool.SLOPPY_SECONDS_DINER_ID ) ||
		     this.adventureId.equals( AdventurePool.YACHT_ID ) )
		{
			boolean unlocked = Preferences.getBoolean( "sleazeAirportAlways" ) || Preferences.getBoolean( "_sleazeAirportToday" );
			if ( !unlocked )
			{
				// Visit the airport and take a look.
				RequestThread.postRequest( new PlaceRequest( "airport" ) );
				unlocked = Preferences.getBoolean( "sleazeAirportAlways" ) || Preferences.getBoolean( "_sleazeAirportToday" );
			}

			this.isValidAdventure = unlocked;
			return;
		}

		if ( this.adventureId.equals( AdventurePool.DR_WEIRDEAUX_ID ) ||
		     this.adventureId.equals( AdventurePool.SECRET_GOVERNMENT_LAB_ID ) ||
		     this.adventureId.equals( AdventurePool.DEEP_DARK_JUNGLE_ID ) )
		{
			boolean unlocked = Preferences.getBoolean( "spookyAirportAlways" ) || Preferences.getBoolean( "_spookyAirportToday" );
			if ( !unlocked )
			{
				// Visit the airport and take a look.

				RequestThread.postRequest( new PlaceRequest( "airport" ) );
				unlocked = Preferences.getBoolean( "spookyAirportAlways" ) || Preferences.getBoolean( "_spookyAirportToday" );
			}

			this.isValidAdventure = unlocked;
			return;
		}

		if ( this.adventureId.equals( AdventurePool.BARF_MOUNTAIN_ID ) ||
		     this.adventureId.equals( AdventurePool.GARBAGE_BARGES_ID ) ||
		     this.adventureId.equals( AdventurePool.TOXIC_TEACUPS_ID ) ||
		     this.adventureId.equals( AdventurePool.LIQUID_WASTE_SLUICE_ID ) )
		{
			boolean unlocked = Preferences.getBoolean( "stenchAirportAlways" ) || Preferences.getBoolean( "_stenchAirportToday" );
			if ( !unlocked )
			{
				// Visit the airport and take a look.

				RequestThread.postRequest( new PlaceRequest( "airport" ) );
				unlocked = Preferences.getBoolean( "stenchAirportAlways" ) || Preferences.getBoolean( "_stenchAirportToday" );
			}

			this.isValidAdventure = unlocked;
			return;
		}

		if ( this.adventureId.equals( AdventurePool.SPACEGATE_ID ) )
		{
			boolean unlocked = Preferences.getBoolean( "spacegateAlways" ) || Preferences.getBoolean( "_spacegateToday" );
			if ( !unlocked )
			{
				// Visit the mountains and take a look.
				RequestThread.postRequest( new PlaceRequest( "mountains" ) );
				unlocked = Preferences.getBoolean( "_spacegateToday" );
			}

			this.isValidAdventure = unlocked;
			return;
		}

		if ( this.adventureId.equals( AdventurePool.GINGERBREAD_CIVIC_CENTER_ID ) ||
		     this.adventureId.equals( AdventurePool.GINGERBREAD_TRAIN_STATION_ID ) ||
		     this.adventureId.equals( AdventurePool.GINGERBREAD_INDUSTRIAL_ZONE_ID ) ||
		     this.adventureId.equals( AdventurePool.GINGERBREAD_RETAIL_DISTRICT_ID ) ||
		     this.adventureId.equals( AdventurePool.GINGERBREAD_SEWERS_ID ) )
		{
			boolean unlocked = Preferences.getBoolean( "gingerbreadCityAvailable" ) || Preferences.getBoolean( "_gingerbreadCityToday" );
			if ( !unlocked )
			{
				// Visit the Mountains and take a look.
				RequestThread.postRequest( new PlaceRequest( "mountains" ) );
				unlocked = Preferences.getBoolean( "_gingerbreadCityToday" );
			}

			this.isValidAdventure = unlocked;
			return;
		}

		if ( this.adventureId.equals( AdventurePool.EDGE_OF_THE_SWAMP_ID ) )
		{
			this.isValidAdventure = QuestDatabase.isQuestLaterThan( Quest.SWAMP, "unstarted" );
			return;
		}

		if ( this.adventureId.equals( AdventurePool.DARK_AND_SPOOKY_SWAMP_ID ) )
		{
			this.isValidAdventure = Preferences.getBoolean( "maraisDarkUnlock" );
			return;
		}

		if ( this.adventureId.equals( AdventurePool.CORPSE_BOG_ID ) )
		{
			this.isValidAdventure = Preferences.getBoolean( "maraisCorpseUnlock" );
			return;
		}

		if ( this.adventureId.equals( AdventurePool.RUINED_WIZARDS_TOWER_ID ) )
		{
			this.isValidAdventure = Preferences.getBoolean( "maraisWizardUnlock" );
			return;
		}

		if ( this.adventureId.equals( AdventurePool.WILDLIFE_SANCTUARRRRRGH_ID ) )
		{
			this.isValidAdventure = Preferences.getBoolean( "maraisWildlifeUnlock" );
			return;
		}

		if ( this.adventureId.equals( AdventurePool.WEIRD_SWAMP_VILLAGE_ID ) )
		{
			this.isValidAdventure = Preferences.getBoolean( "maraisVillageUnlock" );
			return;
		}

		if ( this.adventureId.equals( AdventurePool.SWAMP_BEAVER_TERRITORY_ID ) )
		{
			this.isValidAdventure = Preferences.getBoolean( "maraisBeaverUnlock" );
			return;
		}

		this.isValidAdventure = true;
	}

	private static boolean getEnchantedBean()
	{
		// Do we have an enchanted bean? Can we get one easily?
		if ( InventoryManager.hasItem( ItemPool.ENCHANTED_BEAN ) )
		{
			return true;
		}

		// No. We can adventure for one. Ask the user if this is OK.
		if ( StaticEntity.isGUIRequired() && GenericFrame.instanceExists() &&
		     !InputFieldUtilities.confirm( "KoLmafia thinks you haven't planted an enchanted bean yet.	Would you like to have KoLmafia automatically adventure to obtain one?" ) )
		{
			return false;
		}

		// The user said "do it". So, do it!

		KoLAdventure sideTripLocation = AdventureDatabase.getAdventure( "Beanbat Chamber" );
		AdventureResult sideTripItem = ItemPool.get( ItemPool.ENCHANTED_BEAN, 1 );

		GoalManager.makeSideTrip( sideTripLocation, sideTripItem );

		return !KoLmafia.refusesContinue();
	}

	/**
	 * Retrieves the string form of the adventure contained within this encapsulation, which is generally the name of
	 * the adventure.
	 *
	 * @return The string form of the adventure
	 */

	@Override
	public String toString()
	{
		return this.normalString;
	}

	/**
	 * Executes the appropriate <code>GenericRequest</code> for the adventure encapsulated by this
	 * <code>KoLAdventure</code>.
	 */

	public void run()
	{
		if ( RecoveryManager.isRecoveryPossible() )
		{
			if ( !RecoveryManager.runThresholdChecks() )
			{
				return;
			}
		}

		this.validate( false );

		if ( !this.isValidAdventure )
		{
			StringBuilder sewerMessage = new StringBuilder();
			if (this.adventureId.equals( AdventurePool.HOBOPOLIS_SEWERS_ID )) {
				sewerMessage.append("requireSewerTestItems is true so: ");
				if (!(KoLCharacter.hasEquipped( ItemPool.get( ItemPool.GATORSKIN_UMBRELLA, 1 )))) {
					sewerMessage.append("Equip a gatorskin umbrella. ");
				}
				if (!(KoLCharacter.hasEquipped( ItemPool.get( ItemPool.HOBO_CODE_BINDER, 1 ) ) ) ) {
					sewerMessage.append("Equip a hobo code binder. ");
				}
				if (!InventoryManager.retrieveItem( ItemPool.SEWER_WAD )) {
					sewerMessage.append("Acquire 1 sewer wad. ");
				}
				if (!InventoryManager.retrieveItem( ItemPool.OOZE_O )) {
					sewerMessage.append("Acquire 1 bottle of Ooze-O. ");
				}
				if (!InventoryManager.retrieveItem( ItemPool.DUMPLINGS )) {
					sewerMessage.append("Acquire 1 unfortunate dumpling. ");
				}
				if (!InventoryManager.retrieveItem( ItemPool.OIL_OF_OILINESS, 3 ) ) {
					sewerMessage.append("Acquire 3 oil of oiliness. ");
				}
			} else {
				sewerMessage.append("That area is not available.");
			}
			KoLmafia.updateDisplay( MafiaState.ERROR, sewerMessage.toString() );
			return;
		}

		if ( this.getAdventureId().equals( AdventurePool.THE_SHORE_ID ) &&
		     KoLCharacter.getAvailableMeat() < ( KoLCharacter.inFistcore() ? 5 : 500 ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Insufficient funds for shore vacation." );
			return;
		}

		String action = Preferences.getString( "battleAction" );

		if ( this.request instanceof AdventureRequest && !this.adventureId.equals( AdventurePool.ORC_CHASM_ID ) )
		{
			if ( !this.isNonCombatsOnly() && action.contains( "dictionary" ) && FightRequest.DICTIONARY1.getCount( KoLConstants.inventory ) < 1 && FightRequest.DICTIONARY2.getCount( KoLConstants.inventory ) < 1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Sorry, you don't have a dictionary." );
				return;
			}
		}

		if ( this.areaSummary != null &&
		     !KoLCharacter.inZombiecore() &&
		     !KoLCharacter.inNuclearAutumn() &&
		     !KoLCharacter.inGLover() &&
		     this.areaSummary.poison() <= Preferences.getInteger( "autoAntidote" ) &&
		     !KoLCharacter.hasEquipped( ItemPool.get( ItemPool.BEZOAR_RING, 1 ) ) )
		{
			SpecialOutfit.createImplicitCheckpoint();
			InventoryManager.retrieveItem( ItemPool.ANTIDOTE );
			SpecialOutfit.restoreImplicitCheckpoint();
		}

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		// Make sure there are enough adventures to run the request
		// so that you don't spam the server unnecessarily.

		if ( KoLCharacter.getAdventuresLeft() == 0 || KoLCharacter.getAdventuresLeft() < this.request.getAdventuresUsed() )
		{
			KoLmafia.updateDisplay( MafiaState.PENDING, "Ran out of adventures." );
			return;
		}

		if ( !this.isNonCombatsOnly() && this.request instanceof AdventureRequest )
		{
			// Check for dictionaries as a battle strategy, if the
			// person is not adventuring at the chasm.

			if ( !this.adventureId.equals( AdventurePool.ORC_CHASM_ID ) && this.request.getAdventuresUsed() == 1 && action.contains( "dictionary" ) )
			{
				if ( !KoLCharacter.getFamiliar().isCombatFamiliar() )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "A dictionary would be useless there." );
					return;
				}
			}

			// If the person doesn't stand a chance of surviving,
			// automatically quit and tell them so.

			if ( action.startsWith( "attack" ) && this.areaSummary != null && !this.areaSummary.willHitSomething() )
			{
				if ( !KoLCharacter.getFamiliar().isCombatFamiliar() )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "You can't hit anything there." );
					return;
				}
			}

			if ( FightRequest.isInvalidShieldlessAttack( action ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR,
					"Your selected attack skill is useless without a shield." );
				return;
			}
		}

		if ( AdventureDatabase.isPotentialCloverAdventure( adventureName ) && InventoryManager.cloverProtectionActive() )
		{
			KoLmafia.protectClovers();
		}

		// If we get this far, then it is safe to run the request
		// (without spamming the server).

		KoLAdventure.setNextAdventure( this );

		if ( RecoveryManager.isRecoveryPossible() )
		{
			RecoveryManager.runBetweenBattleChecks( !this.isNonCombatsOnly() );

			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}
		}

		RequestThread.postRequest( this.request );
	}

	private static final Pattern ADVENTUREID_PATTERN = Pattern.compile( "snarfblat=([\\d]+)" );
	private static final Pattern MINE_PATTERN = Pattern.compile( "mine=([\\d]+)" );

	public static final KoLAdventure setLastAdventure( String adventureId, final String adventureName, String adventureURL, final String container )
	{
		KoLAdventure adventure = AdventureDatabase.getAdventureByURL( adventureURL );
		if ( adventure == null )
		{
			int index = adventureURL.indexOf( "?" );
			String adventurePage;

			if ( index != -1 )
			{
				adventurePage = adventureURL.substring( 0, index );
			}
			else
			{
				adventurePage = adventureURL;
			}

			if ( adventurePage.equals( "mining.php" ) )
			{
				Matcher matcher= KoLAdventure.MINE_PATTERN.matcher( adventureURL );
				adventureId = matcher.find() ? matcher.group( 1 ) : "0";
				adventureURL = adventurePage + "?mine=" + adventureId;
			}
			else if ( adventurePage.equals( "adventure.php" ) )
			{
				if ( adventureId.equals( "" ) )
				{
					Matcher matcher= KoLAdventure.ADVENTUREID_PATTERN.matcher( adventureURL );
					adventureId = matcher.find() ? matcher.group( 1 ) : "0";
				}
			}
			else if ( adventurePage.equals( "main.php" ) )
			{
				// This is "(none)" after a new ascension
				return null;
			}
			else if ( KoLAdventure.unknownAdventures.contains( adventureName ) )
			{
				// If we've already logged this one, don't do it again
				return null;
			}
			else
			{
				// Don't register as an adventure, but save name
				KoLAdventure.unknownAdventures.add( adventureName );
				Preferences.setString( "lastAdventure", adventureName );
				RequestLogger.updateSessionLog( "Unknown last adventure: id = '" + adventureId + "' name = '" + adventureName + "' URL = '" + adventureURL + "' container = '" + container + "'" );
				return null;
			}

			RequestLogger.printLine( "Adding new location: " + adventureName + " - " + adventureURL );

			// We could use "container" to pick the zone the adventure goes in

			// Detach strings from the responseText
			adventure = new KoLAdventure( "Override", new String( adventurePage ), new String( adventureId ), new String( adventureName ) );
			AdventureDatabase.addAdventure( adventure );
		}

		KoLAdventure.setLastAdventure( adventure );
		return adventure;
	}

	public static final void setLastAdventure( final KoLAdventure adventure )
	{
		if ( adventure == null )
		{
			return;
		}

		String adventureId = adventure.adventureId;
		String adventureName = adventure.adventureName;
		String adventureURL = adventure.formSource;

		KoLAdventure.lastVisitedLocation = adventure;
		KoLAdventure.lastLocationName = adventure.getPrettyAdventureName( adventureURL );
		KoLAdventure.lastLocationURL = adventureURL;
		Preferences.setString( "lastAdventure", adventureName );

		// If you were able to access some hidden city areas you must have unlocked them so update quest status
		if ( adventureId.equals( AdventurePool.HIDDEN_APARTMENT_ID ) &&
		     Preferences.getInteger( "hiddenApartmentProgress" ) == 0 )
		{
			Preferences.setInteger( "hiddenApartmentProgress", 1 );
		}
		else if ( adventureId.equals( AdventurePool.HIDDEN_HOSPITAL_ID ) &&
			  Preferences.getInteger( "hiddenHospitalProgress" ) == 0 )
		{
			Preferences.setInteger( "hiddenHospitalProgress", 1 );
		}
		else if ( adventureId.equals( AdventurePool.HIDDEN_OFFICE_ID ) &&
			  Preferences.getInteger( "hiddenOfficeProgress" ) == 0 )
		{
			Preferences.setInteger( "hiddenOfficeProgress", 1 );
		}
		else if ( adventureId.equals( AdventurePool.HIDDEN_BOWLING_ALLEY_ID ) &&
			  Preferences.getInteger( "hiddenBowlingAlleyProgress" ) == 0 )
		{
			Preferences.setInteger( "hiddenBowlingAlleyProgress", 1 );
		}
	}

	public static final void setNextAdventure( final String adventureName )
	{
		KoLAdventure adventure = AdventureDatabase.getAdventure( adventureName );
		if ( adventure == null )
		{
			Preferences.setString( "nextAdventure", adventureName );
			KoLCharacter.updateSelectedLocation( null );
			return;
		}
		KoLAdventure.setNextAdventure( adventure );
		EncounterManager.registerAdventure( adventureName );
	}

	public static final void setNextAdventure( final KoLAdventure adventure )
	{
		if ( adventure == null )
		{
			return;
		}

		Preferences.setString( "nextAdventure", adventure.adventureName );
		KoLCharacter.updateSelectedLocation( adventure );
		NamedListenerRegistry.fireChange( "(koladventure)" );
	}

	public static final KoLAdventure lastVisitedLocation()
	{
		return KoLAdventure.lastVisitedLocation;
	}

	public static final int lastAdventureId()
	{
		KoLAdventure location = KoLAdventure.lastVisitedLocation;

		return  location == null || !StringUtilities.isNumeric( location.adventureId ) ?
			0 : StringUtilities.parseInt( location.adventureId );
	}

	public static final String lastAdventureIdString()
	{
		KoLAdventure location = KoLAdventure.lastVisitedLocation;
		return location == null ? "" : location.adventureId;
	}

	public static final boolean recordToSession( final String urlString )
	{
		// This is the first half of logging an adventure location
		// given only the URL. We try to deduce where the player is
		// adventuring and save it for verification later. We also do
		// some location specific setup.

		if ( KoLmafia.isRefreshing() )
		{
			return false;
		}

		// See if this is a standard "adventure" in adventures.txt
		KoLAdventure adventure = KoLAdventure.findAdventure( urlString );
		if ( adventure != null )
		{
			adventure.prepareToAdventure( urlString );
			KoLAdventure.lastVisitedLocation = adventure;
			KoLAdventure.lastLocationName = adventure.getPrettyAdventureName( urlString );
			KoLAdventure.lastLocationURL = urlString;
			KoLAdventure.locationLogged = false;
			return true;
		}

		// No. See if it's a special "adventure"
		String location = AdventureDatabase.getUnknownName( urlString );
		if ( location == null )
		{
			return false;
		}

		if ( !urlString.contains( "?" ) )
		{
			return true;
		}

		KoLAdventure.lastVisitedLocation = null;
		KoLAdventure.lastLocationName = location;
		KoLAdventure.lastLocationURL = urlString;
		KoLAdventure.locationLogged = false;

		return true;
	}

	private static KoLAdventure findAdventure( final String urlString )
	{
		if ( urlString.equals( "barrel.php" ) )
		{
			return null;
		}
		if ( urlString.startsWith( "mining.php" ) && urlString.contains( "intro=1" ) )
		{
			return null;
		}
		return AdventureDatabase.getAdventureByURL( urlString );
	}

	private static final KoLAdventure findAdventureAgain( final String responseText )
	{
		// Look for an "Adventure Again" link and return the
		// KoLAdventure that it matches.
		Matcher matcher = ADVENTURE_AGAIN.matcher( responseText );
		if ( !matcher.find() )
		{
			return null;
		}

		return KoLAdventure.findAdventure( matcher.group(1) );
	}

	private void prepareToAdventure( final String urlString )
	{
		// If we are in a drunken stupor, return now.
		if ( KoLCharacter.isFallingDown() &&
		     !urlString.startsWith( "trickortreat" ) &&
		     !KoLCharacter.hasEquipped( ItemPool.get( ItemPool.DRUNKULA_WINEGLASS, 1 ) ) )
		{
			return;
		}

		int id = 0;

		if ( StringUtilities.isNumeric( this.adventureId ) )
		{
			id = StringUtilities.parseInt( this.adventureId );

			switch ( id )
			{

			case AdventurePool.FCLE:
				AdventureResult mop = ItemPool.get( ItemPool.MIZZENMAST_MOP, 1 );
				AdventureResult polish = ItemPool.get( ItemPool.BALL_POLISH, 1 );
				AdventureResult sham = ItemPool.get( ItemPool.RIGGING_SHAMPOO, 1 );
				if ( InventoryManager.hasItem( mop ) &&
				     InventoryManager.hasItem( polish ) &&
				     InventoryManager.hasItem( sham ) )
				{
					RequestThread.postRequest( UseItemRequest.getInstance( mop ) );
					RequestThread.postRequest( UseItemRequest.getInstance( polish ) );
					RequestThread.postRequest( UseItemRequest.getInstance( sham ) );
				}
				break;
			}
		}

		if ( !( this.getRequest() instanceof AdventureRequest ) || this.isValidAdventure )
		{
			return;
		}


		// Visit the untinker before adventuring at Degrassi Knoll.

		if ( id == AdventurePool.DEGRASSI_KNOLL_GARAGE )
		{
			UntinkerRequest.canUntinker();
		}

		this.isValidAdventure = true;

		// Make sure you're wearing the appropriate equipment
		// for the King's chamber in Cobb's knob.

		if ( this.formSource.equals( "cobbsknob.php" ) )
		{
			this.validate( true );
		}
	}

	// Automated adventuring in an area can result in a failure. We go into
	// the ERROR or the PENDING state, depending on whether we should stop
	// a script for attempting the adventure. The default is ERROR. Use
	// PENDING only when the script could not have known that the attempt
	// would fail.

	private static final Object [][] ADVENTURE_FAILURES =
	{
		// KoL bug: returning a blank page. This must be index 0.
		{
			"",
			"KoL returned a blank page.",
		},

		// Lots of places.
		{
			"It is recommended that you have at least",
			"Your stats are too low for this location.  Adventure manually to acknowledge or disable this warning.",
		},

		// Lots of places.
		{
			"You shouldn't be here",
			"You can't get to that area.",
		},

		// Lots of places.
		{
			"not yet be accessible",
			"You can't get to that area.",
		},

		// Lots of places.
		{
			"You can't get there",
			"You can't get to that area.",
		},

		// Lots of places.
		{
			"Seriously.  It's locked.",
			"You can't get to that area.",
		},

		// 8-bit realm and Vanya's Castle
		{
			"You can't get to the 8-bit realm right now",
			"You can't get to that area.",
		},

		// Out of adventures
		{
			"You're out of adventures",
			"You're out of adventures.",
			MafiaState.PENDING
		},

		// Out of adventures in the Daily Dungeon
		{
			"You don't have any adventures.",
			"You're out of adventures.",
			MafiaState.PENDING
		},

		// Out of adventures at Shore
		{
			"You don't have enough Adventures left",
			"You're out of adventures.",
			MafiaState.PENDING
		},

		// Out of meat at Shore
		{
			"You can't afford to go on a vacation",
			"You can't afford to go on a vacation.",
		},

		// Too drunk at shore
		{
			"You're too drunk to go on vacation",
			"You are too drunk to go on a vacation.",
		},

		// Beaten up at zero HP
		{
			"You're way too beaten up to go on an adventure right now",
			"You can't adventure at 0 HP.",
			MafiaState.PENDING
		},

		// Typical Tavern with less than 100 Meat
		{
			"Why go to the Tavern if you can't afford to drink?",
			"You can't afford to go out drinking.",
		},

		// The Road to White Citadel
		{
			"You've already found the White Citadel",
			"The Road to the White Citadel is already cleared.",
		},

		// Friar's Ceremony Location without the three items
		{
			"You don't appear to have all of the elements necessary to perform the ritual",
			"You don't have everything you need.",
		},

		// You need some sort of stench protection to adventure in there.
		// You're going to need some sort of stench protection if you want to adventure here.
		{
			"need some sort of stench protection",
			"You need stench protection.",
		},

		// You need some sort of protection from the cold if you're
		// going to visit the Icy Peak.
		{
			"You need some sort of protection from the cold",
			"You need cold protection.",
		},

		// You try to enter the Haunted Library, but the door is locked. I guess this particular information doesn't want to be free.
		{
			"I guess this particular information doesn't want to be free",
			"You need the Spookyraven library key.",
		},

		// Mining while drunk
		{
			"You're too drunk to spelunk, as it were",
			"You are too drunk to go there.",
		},

		// Pyramid Lower Chamber while drunk
		{
			"You're too drunk to screw around",
			"You are too drunk to go there.",
		},

		// You can't adventure there without some way of breathing underwater...
		{
			"without some way of breathing underwater",
			"You can't breathe underwater.",
		},

		// You can't adventure there now -- Gort wouldn't be able to breathe!
		{
			"wouldn't be able to breathe",
			"Your familiar can't breathe underwater.",
		},

		// It wouldn't be safe to go in there dressed like you are. You should consider a Mer-kin disguise.
		{
			"You should consider a Mer-kin disguise.",
			"You aren't wearing a Mer-kin disguise.",
		},

		// Attempting to enter the Cola Wars Battlefield with level > 5
		{
			"The temporal rift in the plains has closed",
			"The temporal rift has closed.",
			MafiaState.PENDING
		},

		// Out of your mining uniform, you are quickly identified as a
		// stranger and shown the door.
		{
			"you are quickly identified as a stranger",
			"You aren't wearing an appropriate uniform.",
		},

		// You're not properly equipped for that. Get into a uniform.
		{
			"Get into a uniform",
			"You aren't wearing an appropriate uniform.",
		},

		// There are no Frat soldiers left
		{
			"There are no Frat soldiers left",
			"There are no Frat soldiers left.",
		},

		// There are no Hippy soldiers left
		{
			"There are no Hippy soldiers left",
			"There are no Hippy soldiers left.",
		},

		// Spooky Gravy Burrow before told to go there:
		// You should probably stay out of there unless you have a good
		// reason to go in. Like if you were on a quest to find
		// something in there, or something.
		{
			"You should probably stay out of there",
			"You have not been given the quest to go there yet.",
			MafiaState.PENDING
		},

		// Worm Wood while not Absinthe Minded
		{
			"For some reason, you can't find your way back there",
			"You need to be Absinthe Minded to go there.",
			MafiaState.PENDING
		},

		// "You can't take it any more. The confusion, the nostalgia,
		// the inconsistent grammar. You break the bottle on the
		// ground, and stomp it to powder."
		{
			"You break the bottle on the ground",
			"You are no longer gazing into the bottle.",
			MafiaState.PENDING
		},

		// You're in the regular dimension now, and don't remember how
		// to get back there.
		{
			"You're in the regular dimension now",
			"You are no longer Half-Astral.",
			MafiaState.PENDING
		},

		// The Factory has faded back into the spectral mists, and
		// eldritch vapors and such.
		{
			"faded back into the spectral mists",
			"No one may know of its coming or going."
		},

		// You wander around the farm for a while, but can't find any
		// additional ducks to fight. Maybe some more will come out of
		// hiding by tomorrow.
		{
			"can't find any additional ducks",
			"Nothing more to do here today.",
			MafiaState.PENDING
		},

		// There are no more ducks here.
		{
			"no more ducks here",
			"Farm area cleared.",
			MafiaState.PENDING
		},

		// You don't know where that place is.
		{
			"You don't know where that place is.",
			"Use a \"DRINK ME\" potion before trying to adventure here.",
			MafiaState.PENDING
		},

		// Orchard failure - You try to enter the feeding chamber, but
		// your way is blocked by a wriggling mass of filthworm drones.
		// Looks like they don't let anything in here if they don't
		// recognize its smell.
		{
			"Looks like they don't let anything in here if they don't recognize its smell.",
			"Use a filthworm hatchling scent gland before trying to adventure here.",
		},

		// Orchard failure - You try to enter the royal guards'
		// chamber, but you're immediately shoved back out into the
		// tunnel. Looks like the guards will only let you in here if
		// you smell like food.
		{
			"Looks like the guards will only let you in here if you smell like food.",
			"Use a filthworm drone scent gland before trying to adventure here.",
		},

		// Orchard failure - You try to enter the filthworm queen's
		// chamber, but the guards outside the door block the entrance.
		// You must not smell right to 'em.
		{
			"You must not smell right to 'em.",
			"Use a filthworm royal guard scent gland before trying to adventure here.",
		},

		// Orchard failure - The filthworm queen has been slain, and the
		// hive lies empty 'neath the orchard.
		{
			"The filthworm queen has been slain",
			"The filthworm queen has been slain.",
		},

		// You've already retrieved all of the stolen Meat
		{
			"already retrieved all of the stolen Meat",
			"You already recovered the Nuns' Meat.",
			MafiaState.PENDING
		},

		// There are no hippy soldiers left -- the way to their camp is clear!
		{
			"the way to their camp is clear",
			"There are no hippy soldiers left.",
			MafiaState.PENDING
		},

		// Cobb's Knob King's Chamber after defeating the goblin king.
		{
			"You've already slain the Goblin King",
			"You already defeated the Goblin King.",
			MafiaState.PENDING
		},

		// The Haert of the Cyrpt after defeating the Bonerdagon
		{
			"Bonerdagon has been defeated",
			"You already defeated the Bonerdagon.",
			MafiaState.PENDING
		},

		// Any cyrpt area after defeating the sub-boss
		{
			"already undefiled",
			"Cyrpt area cleared.",
			MafiaState.PENDING
		},

		// The Summoning Chamber after Lord Spookyraven has been defeated
		//
		// You enter the Summoning Chamber.  The air is heavy with
		// evil, and otherworldly whispers echo melodramatically
		// through your mind
		{
			"otherworldly whispers",
			"You already defeated Lord Spookyraven.",
			MafiaState.PENDING
		},

		// Ed the undying defeated
		{
			"Ed the Undying sleeps once again",
			"Ed the Undying has already been defeated.",
			MafiaState.PENDING
		},

		// You probably shouldn't -- you don't trust those rats not to steal your token!
		{
			"don't trust those rats not to steal",
			"You don't trust those rats not to steal your token!.",
			MafiaState.PENDING
		},

		// That's too far to walk, and
		// <a href=clan_dreadsylvania.php?place=carriage>the Carriageman</a>
		// isn't drunk enough to take you there.
		{
			"That's too far to walk",
			"The Carriageman isn't drunk enough to take you there.",
			MafiaState.PENDING
		},

		// The forest is silent, those who stalked it having themselves been stalked.
		{
			"The forest is silent",
			"The Dreadsylvanian Woods boss has been defeated.",
			MafiaState.PENDING
		},

		// The village is now a ghost town in the figurative sense, rather than the literal.
		{
			"The village is now a ghost town",
			"The Dreadsylvanian Village boss has been defeated.",
			MafiaState.PENDING
		},

		// Look upon this castle, ye mighty, and despair, because the king is dead, baby.
		{
			"the king is dead, baby",
			"The Dreadsylvanian Castle boss has been defeated.",
			MafiaState.PENDING
		},

		// This part of the city is awfully unremarkable, now that
		// you've cleared that ancient protector spirit out.
		{
			"cleared that ancient protector spirit out",
			"You already defeated the protector spirit in that square.",
		},

		// Now that you've put something in the round depression in the
		// altar, the altar doesn't really do anything but look
		// neat. Those ancient guys really knew how to carve themselves
		// an altar, mmhmm.
		{
			"the altar doesn't really do anything but look neat",
			"You already used the altar in that square.",
		},

		// Here's poor Dr. Henry "Dakota" Fanning, Ph.D, R.I.P., lying
		// here in a pile just where you left him.
		{
			"lying here in a pile just where you left him",
			"You already looted Dr. Fanning in that square.",
		},

		// You wander into the empty temple and look around. Remember
		// when you were in here before, and tried to gank some old
		// amulet off of that mummy? And then its ghost came out and
		// tried to kill you? But you destroyed it, and won the ancient
		// doohickey?
		//
		// Good times, man. Good times.
		{
			"You wander into the empty temple",
			"You already looted the temple in that square.",
		},

		// You climb the stairs from the castle's basement, but the
		// door at the top is closed and you can't find the doorknob.
		//
		// You'll have to find another way up.
		{
			"You'll have to find another way up",
			"You haven't opened the ground floor of the castle yet.",
		},

		// You have to learn to walk before you can learn to fly.
		//
		// Also you can't get to the top floor of a building if you can't get to the ground floor.
		{
			"you can't get to the ground floor",
			"You haven't opened the ground floor of the castle yet.",
		},

		// The door at the top of the ground floor stairway is also
		// closed, and you're still too short to reach a doorknob
		// that's forty feet over your head.
		//
		// You'll have to figure out some other way to get upstairs.
		{
			"You'll have to figure out some other way to get upstairs",
			"You haven't opened the top floor of the castle yet.",
		},

		// The portal is open! Head home and prepare to save some children!
		{
			"prepare to save some children",
			"The portal is open.",
		},

		// It looks like things are running pretty smoothly at the
		// factory right now -- there's nobody to fight.
		{
			"things are running pretty smoothly",
			"Nothing more to do here today.",
		},

		// You should talk to Edwing before you head back in there, and
		// wait for him to formulate a plan.
		{
			"You should talk to Edwing",
			"Nothing more to do here today.",
		},

		// The compound is abandoned now...
		{
			"The compound is abandoned now",
			"Nothing more to do here today.",
		},

		// Between the wind and the weird spiky bits all over it, you
		// can't make it to the second story of the fortress without
		// some way of escaping gravity.
		{
			"some way of escaping gravity",
			"You are not wearing a warbear hoverbelt.",
		},

		// Your hoverbelt would totally do the trick to get you up
		// there, only it's out of juice.
		{
			"it's out of juice",
			"Your hoverbelt needs a new battery.",
		},

		// You float up to the third story of the fortress, but all you
		// find is a locked door with a keypad next to it, and you
		// don't have a code.
		{
			"you don't have a code",
			"You don't have a warbear badge.",
		},

		// There's nothing left of Ol' Scratch but a crater and a
		// stove.  Burnbarrel Blvd. is still hot, but it's no longer
		// bothered.  Or worth bothering with.
		{
			"There's nothing left of Ol' Scratch",
			"Nothing more to do here.",
			MafiaState.PENDING
		},

		// There's nothing left in Exposure Esplanade. All of the snow
		// forts have been crushed or melted, all of the igloos are
		// vacant, and all of the reindeer are off playing games
		// somewhere else.
		{
			"There's nothing left in Exposure Esplanade",
			"Nothing more to do here.",
			MafiaState.PENDING
		},

		// The Heap is empty.  Well, let me rephrase that.  It's still
		// full of garbage, but there's nobody and nothing of interest
		// mixed in with the garbage.
		{
			"The Heap is empty",
			"Nothing more to do here.",
			MafiaState.PENDING
		},

		// There's nothing going on here anymore -- the tombs of the
		// Ancient Hobo Burial Ground are all as silent as themselves.
		{
			"There's nothing going on here anymore",
			"Nothing more to do here.",
			MafiaState.PENDING
		},

		// There's nothing left in the Purple Light District.  All of
		// the pawn shops and adult bookshops have closed their doors
		// for good.
		{
			"There's nothing left in the Purple Light District",
			"Nothing more to do here.",
			MafiaState.PENDING
		},

		// The Hoboverlord has been defeated, and Hobopolis Town Square
		// lies empty.
		{
			"Hobopolis Town Square lies empty",
			"Nothing more to do here.",
			MafiaState.PENDING
		},

		// The bathrooms are empty now -- looks like you've taken care
		// of the elf hobo problem for the time being.
		{
			"bathrooms are empty now",
			"Nothing more to do here.",
			MafiaState.PENDING
		},

		// The Skies over Valhalls

		// You poke your head through the slash, and find yourself
		// looking at Valhalla from a dizzying height. Come down now,
		// they'll say, but there's no way you're going all the way
		// through that slash without some sort of transportation.
		{
			"there's no way you're going all the way through that slash",
			"You don't have a flying mount.",
			MafiaState.PENDING
		},

		// You can't do anything without some way of flying.  And
		// before you go pointing at all of the stuff in your inventory
		// that says it lets you fly or float or levitate or whatever,
		// that stuff won't work.  You're gonna need a hideous winged
		// yeti mount, because that's the only thing that can handle
		// this particular kind of flying.  Because of science.
		{
			"You can't do anything without some way of flying",
			"You don't have a flying mount.",
			MafiaState.PENDING
		},

		// There are at least two of everything up there, and you're
		// also worried that you might fall off your yeti. You should
		// maybe come back when you're at least slightly less drunk.
		{
			"You should  maybe come back when you're at least slightly less drunk",
			"You are too drunk.",
			MafiaState.PENDING
		},

		// You don't have the energy to attack a problem this size. Go
		// drink some soda or something.
		{
			"You don't have the energy to attack a problem this size",
			"You need at least 20% buffed max MP.",
			MafiaState.PENDING
		},

		// You're not in good enough shape to deal with a threat this
		// large. Go get some rest, or put on some band-aids or
		// something.
		{
			"You're not in good enough shape to deal with a threat this large",
			"You need at least 20% buffed max HP.",
			MafiaState.PENDING
		},

		// Your El Vibrato portal has run out of power. You should go
		// back to your campsite and charge it back up.
		{
			"Your El Vibrato portal has run out of power",
			"Your El Vibrato portal has run out of power",
			MafiaState.PENDING
		},

		// No longer Transpondent
		{
			"you don't know the transporter frequency",
			"You are no longer Transpondent.",
		},

		// No longer Transpondent
		{
			"without the proper transporter frequency",
			"You are no longer Transpondent.",
		},

		// No longer Dis Abled
		//
		// Remember that devilish folio you read?
		// No, you don't! You don't have it all still in your head!
		// Better find a new one you can read! I swear this:
		// 'Til you do, you can't visit the Suburbs of Dis!
		{
			"you can't visit the Suburbs of Dis",
			"You are no longer Dis Abled.",
		},

		// Abyssal Portals
		//
		// The area around the portal is quiet. Looks like you took
		// care of all of the seals. Maybe check back tomorrow.
		{
			"area around the portal is quiet",
			"The Abyssal Portal is quiet.",
		},

		// The Secret Government Laboratory
		//
		// You can't go in there without wearing a Personal Ventilation
		// Unit. Who knows what would happen if you breathed the air in
		// there.
		{
			"Who knows what would happen if you breathed the air",
			"You need to equip your Personal Ventilation Unit.",
		},

		// GameInformPowerPro video game levels
		//
		// You already cleared out this area.
		{
			"You already cleared out this area",
			"You already cleared out this area",
		},

		// This area is closed.
		{
			"This area is closed",
			"You completed the video game",
		},

		// You wander around, off the Florida Keys, but can't find anything
		{
			"off the Florida Keys",
			"You need a Tropical Contact High to go there.",
		},

		// You approach the adorable little door, but you can't figure
		// out how to open it. I guess it's a secret door that will
		// only open for gravy fairies.
		{
			"only open for gravy fairies",
			"You need to bring an elemental gravy fairy with you.",
		},

		// You shouldn't be here dressed like that. It's just not safe.
		{
			"You shouldn't be here dressed like that",
			"You can't pass as a pirate.",
		},

		// LOLmec's lair lies lempty. Empty.
		{
			"LOLmec's lair lies lempty",
			"You already beat LOLmec",
		},

		// Already beat Yomama.
		{
			"Already beat Yomama",
			"You already beat Yomama",
		},

		// The ghost has arrived. Your time has run out!
		{
			"The ghost has arrived",
			"Your tale of spelunking is over.",
		},

		// Gingerbread City
		{
			"The gingerbread city has collapsed.",
			"The gingerbread city has collapsed.",
		},

		// Hole in the sky

		// You can see it, but you can't get to it without some means of traveling...
		// 
		// TO SPACE
		{
			"you can't get to it",
			"You need a way to travel in space.",
		},

		// The spacegate is out of energy for today. You can explore
		// another planet tomorrow. Or the same planet, if you
		// want. The spacegate isn't the boss of you.
		{
			"out of energy for today",
			"The Spacegate is out of energy for today.",
		},

		// ou approach the door to the spire, but you can't bear the
		// silence. Maybe you should try those earplugs Tammy gave you.
		{
			"you can't bear the silence",
			"You are not wearing your anti-earplugs..",
		},

		// You stomp around the spire for a while, but it looks like
		// this level is all cleared out. They'll probably redeploy
		// more mimes to it by tomorrow.
		{
			"redeploy more mimes to it by tomorrow",
			"There are no more mimes left today.",
		},

		// Your G. E. M. beeps at you, indicating that your time in FantasyRealm has come to an end for today.
		{
			"your time in FantasyRealm has come to an end for today",
			"Your time in FantasyRealm is over for today.",
		},

		// Technically the Neverending Party is still going, because the Neverending Party never ends,
		// but right now everybody is passed out, so it's more of a Neverending Nap.
		// It'll probably get rowdy again by tomorrow
		{
			"It'll probably get rowdy again by tomorrow",
			"The Neverending Party is over for today.",
		},

		// You wouldn't be seen dead at the Neverending Party without your PARTY HARD shirt on.
		// Not today, at least, since you've already made an appearance in it.
		{
			"without your PARTY HARD shirt on",
			"Cannot adventure at The Neverending Party without your PARTY HARD shirt on.",
		},

		// That isn't a place you can get to the way you're dressed.
		// (Clicking Last Adventure having unequipped Talisman o' Namsilat)
		{
			"That isn't a place you can get to the way you're dressed",
			"You're not equipped properly to adventure there.",
		},
	};

	public static final int findAdventureFailure( String responseText )
	{
		// KoL is known to sometimes simply return a blank page as a
		// failure to adventure.
		if ( responseText.length() == 0 )
		{
			return 0;
		}

		// Sometimes we want to take some action even on a failure
		if ( responseText.contains( "There are no Hippy soldiers left" ) )
		{
			Preferences.setInteger( "hippiesDefeated", 1000 );
		}
		else if ( responseText.contains( "There are no Frat soldiers left" ) )
		{
			Preferences.setInteger( "fratboysDefeated", 1000 );
		}
		
		for ( int i = 1; i < ADVENTURE_FAILURES.length; ++i )
		{
			if ( responseText.contains( (String) ADVENTURE_FAILURES[ i ][ 0 ] ) )
			{
				return i;
			}
		}

		return -1;
	}

	public static final String adventureFailureMessage( int index )
	{
		if ( index >= 0 && index < ADVENTURE_FAILURES.length )
		{
			return (String) ADVENTURE_FAILURES[ index ][ 1 ];
		}

		return null;
	}

	public static final MafiaState adventureFailureSeverity( int index )
	{
		if ( index >= 0 && index < ADVENTURE_FAILURES.length && ADVENTURE_FAILURES[ index ].length > 2 )
		{
			return (MafiaState) ADVENTURE_FAILURES[ index ][ 2 ];
		}

		return MafiaState.ERROR;
	}

	public static final boolean recordToSession( final String urlString, final String responseText )
	{
		// This is the second half of logging an adventure location
		// after we've submitted the URL and gotten a response, after,
		// perhaps, being redirected. Given the old URL, the new URL,
		// and the response, we can often do a better job of figuring
		// out where we REALLY adventured - if anywhere.

		if ( KoLmafia.isRefreshing() )
		{
			return true;
		}

		// Only do this once per adventure attempt.
		if ( KoLAdventure.locationLogged )
		{
			return true;
		}

		String location = KoLAdventure.lastLocationName;
		if ( location == null )
		{
			return false;
		}

		// Only do this once per adventure attempt.
		KoLAdventure.locationLogged = true;

		String lastURL = KoLAdventure.lastLocationURL;

		if ( lastURL.equals( "basement.php" ) )
		{
			return true;
		}

		// See if we've been redirected away from the URL that started
		// us adventuring

		if ( urlString.equals( lastURL ) )
		{
			// No. It is possible that we didn't adventure at all
			if ( KoLAdventure.findAdventureFailure( responseText ) >= 0 )
			{
				return false;
			}

			// See if there is an "adventure again" link, and if
			// so, whether it points to where we thought we went.
			KoLAdventure again = KoLAdventure.findAdventureAgain( responseText );
			if ( again != null && again != KoLAdventure.lastVisitedLocation )
			{
				location = again.getPrettyAdventureName( urlString );
				KoLAdventure.lastVisitedLocation = again;
				KoLAdventure.lastLocationName = location;
				KoLAdventure.lastLocationURL = urlString;
			}
		}
		else if ( urlString.equals( "cove.php" ) )
		{
			// Redirected from Pirate Cove to the cove map
			return false;
		}
		else if ( urlString.startsWith( "mining.php" ) )
		{
			// Redirected to a mine
			return false;
		}
		else if ( urlString.startsWith( "fight.php" ) )
		{
			// Redirected to a fight. We may or may not be
			// adventuring where we thought we were. If your
			// autoattack one-hit-kills the foe, the adventure
			// again link will tell us where you were.
			KoLAdventure again = KoLAdventure.findAdventureAgain( responseText );
			if ( again != null && again != lastVisitedLocation )
			{
				location = again.adventureName;
				KoLAdventure.lastVisitedLocation = again;
				KoLAdventure.lastLocationName = location;
			}
		}
		else if ( urlString.startsWith( "choice.php" ) )
		{
			// Redirected to a choice. We may or may not be
			// adventuring where we thought we were.
		}

		// Customize location name, perhaps
		location = KoLAdventure.getPrettyAdventureName( location, responseText );

		// Update selected adventure information in order to
		// keep the GUI synchronized.

		KoLAdventure.setLastAdventure( KoLAdventure.lastVisitedLocation );
		KoLAdventure.setNextAdventure( KoLAdventure.lastVisitedLocation );
		EncounterManager.registerAdventure( location );

		String limitmode = KoLCharacter.getLimitmode();
		String message = null;
		if ( limitmode == Limitmode.SPELUNKY )
		{
			message = "{" + SpelunkyRequest.getTurnsLeft() + "} " + location;
		}
		else if ( limitmode == Limitmode.BATMAN )
		{
			message = "{" + BatManager.getTimeLeftString() + "} " + location;
		}
		else
		{
			message = "[" + KoLAdventure.getAdventureCount() + "] " + location;
		}
		RequestLogger.printLine();
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		String encounter = "";

		if ( urlString.startsWith( "basement.php" ) )
		{
			encounter = BasementRequest.getBasementLevelSummary();
		}

		if ( !encounter.equals( "" ) )
		{
			RequestLogger.printLine( encounter );
			RequestLogger.updateSessionLog( encounter );
		}

		return true;
	}

	public static final int getAdventureCount()
	{
		return Preferences.getBoolean( "logReverseOrder" ) ? KoLCharacter.getAdventuresLeft() : KoLCharacter.getCurrentRun() + 1;
	}

	public int compareTo( final KoLAdventure o )
	{
		if ( o == null || !( o instanceof KoLAdventure ) )
		{
			return 1;
		}

		KoLAdventure ka = (KoLAdventure) o;

		// Put things with no evade rating at bottom of list.

		int evade1 = this.areaSummary == null ? Integer.MAX_VALUE : this.areaSummary.minEvade();
		int evade2 = ka.areaSummary == null ? Integer.MAX_VALUE : ka.areaSummary.minEvade();

		if ( evade1 == evade2 )
		{
			return this.adventureName.compareTo( ka.adventureName );
		}

		return evade1 - evade2;
	}
}
