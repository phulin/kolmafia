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

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.BookType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GenieRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.IslandRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.VolcanoIslandRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.AdventureResultArray;

public class BreakfastManager
{
	private static final AdventureResult [] toys = new AdventureResult []
	{
		ItemPool.get( ItemPool.HOBBY_HORSE, 1 ),
		ItemPool.get( ItemPool.BALL_IN_A_CUP, 1 ),
		ItemPool.get( ItemPool.SET_OF_JACKS, 1 ),
		ItemPool.get( ItemPool.BAG_OF_CANDY, 1 ),
		ItemPool.get( ItemPool.EMBLEM_AKGYXOTH, 1 ),
		ItemPool.get( ItemPool.IDOL_AKGYXOTH, 1 ),
		ItemPool.get( ItemPool.BURROWGRUB_HIVE, 1 ),
		ItemPool.get( ItemPool.GNOLL_EYE, 1 ),
		ItemPool.get( ItemPool.KOL_CON_SIX_PACK, 1 ),
		ItemPool.get( ItemPool.TRIVIAL_AVOCATIONS_GAME, 1 ),
		ItemPool.get( ItemPool.CREEPY_VOODOO_DOLL, 1 ),
		ItemPool.get( ItemPool.CURSED_KEG, 1 ),
		ItemPool.get( ItemPool.CURSED_MICROWAVE, 1 ),
		ItemPool.get( ItemPool.TACO_FLIER, 1 ),
		ItemPool.get( ItemPool.WARBEAR_SODA_MACHINE, 1 ),
		ItemPool.get( ItemPool.WARBEAR_BREAKFAST_MACHINE, 1 ),
		ItemPool.get( ItemPool.WARBEAR_BANK, 1 ),
		ItemPool.get( ItemPool.CHRONER_TRIGGER, 1 ),
		ItemPool.get( ItemPool.CHRONER_CROSS, 1 ),
		ItemPool.get( ItemPool.PICKY_TWEEZERS, 1 ),
		ItemPool.get( ItemPool.COCKTAIL_SHAKER, 1 ),
		ItemPool.get( ItemPool.BACON_MACHINE, 1 ),
		ItemPool.get( ItemPool.TOASTER, 1 ),
		ItemPool.get( ItemPool.SCHOOL_OF_HARD_KNOCKS_DIPLOMA, 11 ),
		ItemPool.get( ItemPool.CSA_FIRE_STARTING_KIT, 1 ),
		ItemPool.get( ItemPool.PUMP_UP_HIGH_TOPS, 1 ),
		ItemPool.get( ItemPool.PUMP_UP_HIGH_TOPS, 1 ), // You can pump them more than once
		ItemPool.get( ItemPool.PUMP_UP_HIGH_TOPS, 1 ), // Three times in fact
	};

	private static final AdventureResult VIP_LOUNGE_KEY = ItemPool.get( ItemPool.VIP_LOUNGE_KEY, 1 );

	public static void getBreakfast( final boolean runComplete )
	{
		String limitmode = KoLCharacter.getLimitmode();
		if ( limitmode != null )
		{
			// Current limitmodes are transient states you can
			// enter and leave during a run. If KoL ever implements
			// a limitmode for an entire run, we may have to
			// revisit this.
			//
			// Individual breakfast actions already check for zones
			// and places being limited.

			return;
		}

		SpecialOutfit.createImplicitCheckpoint();

		if ( runComplete )
		{
			checkRumpusRoom();
			checkVIPLounge();
			readGuildManual();
			getHermitClovers();
			harvestGarden();
			useSpinningWheel();
			visitBigIsland();
			visitVolcanoIsland();
			checkJackass();
			makePocketWishes();
			if ( Preferences.getBoolean( "useCrimboToys" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
			{
				useToys();
			}
			collectAnticheese();
			collectSeaJelly();
		}

		boolean recoverMana = Preferences.getBoolean( "loginRecovery" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) );

		boolean done = true;

		done &= castSkills( recoverMana, 0 );
		done &= castBookSkills( recoverMana, 0 );

		Preferences.setBoolean( "breakfastCompleted", done );

		SpecialOutfit.restoreImplicitCheckpoint();
		KoLmafia.forceContinue();
	}

	public static void checkRumpusRoom()
	{
		if ( !Limitmode.limitClan() && Preferences.getBoolean( "visitRumpus" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			ClanRumpusRequest.getBreakfast();
			KoLmafia.forceContinue();
		}
	}

	public static void checkVIPLounge()
	{
		if ( !Limitmode.limitClan() && InventoryManager.hasItem( VIP_LOUNGE_KEY ) && Preferences.getBoolean( "visitLounge" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			ClanLoungeRequest.getBreakfast();
			KoLmafia.forceContinue();
		}
	}

	public static void readGuildManual()
	{
		if ( Preferences.getBoolean( "_guildManualUsed" ) )
		{
			return;
		}

		if ( !Preferences.getBoolean( "readManual" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			return;
		}

		if ( KoLCharacter.isAvatarOfBoris() )
		{
			// Can't even get a Guild Manual in AxeCore
			return;
		}

		if ( KoLCharacter.isMuscleClass() && KoLCharacter.inBeecore () )
		{
			// Can't use "Manual of Labor" in Beecore.
			return;
		}

		int manualId =
			KoLCharacter.isMuscleClass() ? ItemPool.MUS_MANUAL :
			KoLCharacter.isMysticalityClass() ? ItemPool.MYS_MANUAL :
			ItemPool.MOX_MANUAL;

		AdventureResult manual = ItemPool.get( manualId, 1 );

		if ( InventoryManager.hasItem( manual ) )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( manual ) );
			KoLmafia.forceContinue();
		}
	}

	private static void useToys()
	{
		boolean useCloset = true;
		boolean useStorage = KoLCharacter.canInteract();

		AdventureResultArray closetItems = useCloset ? new AdventureResultArray() : null;
		AdventureResultArray storageItems = useStorage ? new AdventureResultArray() : null;
		ArrayList<UseItemRequest> requests = new ArrayList<UseItemRequest>();

		for ( AdventureResult toy : toys )
		{
			int itemId = toy.getItemId();

			if ( KoLCharacter.inBeecore() && ItemDatabase.unusableInBeecore( itemId ) )
			{
				continue;
			}

			if ( KoLCharacter.inGLover() && ItemDatabase.unusableInGLover( itemId ) )
			{
				continue;
			}

			// Special cases
			if ( itemId == ItemPool.CSA_FIRE_STARTING_KIT && Preferences.getInteger( "choiceAdventure595" ) == 0 )
			{
				continue;
			}

			int needed = toy.getCount();
			int available = 0;
			int count = 0;

			if ( ( count = toy.getCount( KoLConstants.inventory ) ) > 0 )
			{
				// Use from inventory
				available += Math.min( count, needed );
			}

			if ( useCloset &&
			     ( available < needed ) &&
			     ( count = toy.getCount( KoLConstants.closet ) ) > 0 )
			{
				// Remove from closet
				int take = Math.min( count, needed - available );
				closetItems.add( toy.getInstance( take ) );
				available += take;
			}

			if ( useStorage &&
			     ( available < needed ) &&
			     ( count = toy.getCount( KoLConstants.storage ) ) > 0 )
			{
				// Pull from storage
				int take = Math.min( count, needed - available );
				storageItems.add( toy.getInstance( take ) );
				available += take;
			}

			// If we have none, skip this toy.
			if ( available == 0 )
			{
				continue;
			}

			// It's OK if we don't have as many as we'd like

			// Make a request to use the toy
			requests.add( UseItemRequest.getInstance( toy.getInstance( available ) ) );
		}

		// If nothing to do, do nothing!
		if ( requests.size() == 0 )
		{
			return;
		}

		// Pull items that are in storage but not inventory or the closet
		if ( useStorage && storageItems.size() > 0 )
		{
			RequestThread.postRequest( new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, storageItems.toArray(), false ) );
		}

		// Move items that are in the closet into inventory
		if ( useCloset && closetItems.size() > 0 )
		{
			RequestThread.postRequest( new ClosetRequest( ClosetRequest.CLOSET_TO_INVENTORY, closetItems.toArray() ) );
		}

		// Use the toys!
		for ( UseItemRequest request : requests )
		{
			AdventureResult toy = request.getItemUsed();
			int slot = KoLCharacter.equipmentSlot( toy );

			RequestThread.postRequest( request );
			KoLmafia.forceContinue();

			// If the toy is equipment, we had it equipped, and
			// "using" it unequipped it, re-equip it
			if ( slot != EquipmentManager.NONE && !KoLCharacter.hasEquipped( toy, slot ) )
			{
				RequestThread.postRequest( new EquipmentRequest( toy, slot ) );
				KoLmafia.forceContinue();
			}
		}
	}

	public static void getHermitClovers()
	{
		if ( KoLCharacter.inBadMoon() || KoLCharacter.inNuclearAutumn() || Limitmode.limitZone( "Mountain" ) )
		{
			return;
		}

		if ( Preferences.getBoolean( "grabClovers" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			int count = HermitRequest.cloverCount();
			if ( count > 0 )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hermit " + count + " ten-leaf clover" );
			}

			KoLmafia.forceContinue();
		}
	}

	public static void harvestGarden()
	{
		if ( KoLCharacter.isEd() || KoLCharacter.inNuclearAutumn() || Limitmode.limitCampground() )
		{
			return;
		}

		String crop = Preferences.getString( "harvestGarden" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) );
		if ( CampgroundRequest.hasCropOrBetter( crop ) )
		{
			CampgroundRequest.harvestCrop();
		}
	}

	public static void useSpinningWheel()
	{
		if ( KoLCharacter.isEd() || KoLCharacter.inNuclearAutumn() || Limitmode.limitCampground() )
		{
			return;
		}

		CampgroundRequest.useSpinningWheel();
	}

	public static boolean castSkills( final boolean allowRestore, final int manaRemaining )
	{
		String skillSetting = Preferences.getString( "breakfastAlways" );
		for ( int i = 0; i < UseSkillRequest.BREAKFAST_ALWAYS_SKILLS.length; ++i )
		{
			String skill = UseSkillRequest.BREAKFAST_ALWAYS_SKILLS[ i ];

			if ( !skillSetting.contains( skill ) )
			{
				continue;
			}

			if ( !KoLCharacter.hasSkill( skill ) )
			{
				continue;
			}

			BreakfastManager.castSkill( skill, Integer.MAX_VALUE, false, 0 );
		}

		String suffix = ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" );

		skillSetting = Preferences.getString( "breakfast" + suffix );
		if ( skillSetting.equals( "" ) )
		{
			return true;
		}

		boolean pathedSummons = Preferences.getBoolean( "pathedSummons" + suffix );
		boolean limitExceeded = true;

		for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
		{
			String skill = UseSkillRequest.BREAKFAST_SKILLS[ i ];

			if ( !skillSetting.contains( skill ) )
			{
				continue;
			}

			if ( !KoLCharacter.hasSkill( skill ) )
			{
				continue;
			}

			if ( pathedSummons )
			{
				if ( ( skill.equals( "Pastamastery" ) || skill.equals( "Lunch Break" ) || skill.equals( "Spaghetti Breakfast" ) ) &&
				     !KoLCharacter.canEat() )
				{
					continue;
				}

				if ( ( skill.equals( "Advanced Cocktailcrafting" ) || skill.equals( "Grab a Cold One" ) ) &&
				     ( !KoLCharacter.canDrink() || KoLCharacter.inHighschool() ) )
				{
					continue;
				}

				if ( ( skill.equals( "Summon Crimbo Candy" ) || skill.equals( "Spaghetti Breakfast" ) ) &&
				     KoLCharacter.inBeecore() )
				{
					continue;
				}
			}

			limitExceeded &= BreakfastManager.castSkill( skill, Integer.MAX_VALUE, allowRestore, manaRemaining );
		}

		return limitExceeded;
	}

	public static boolean castSkill( final String name, final int casts, final boolean allowRestore, final int manaRemaining )
	{
		UseSkillRequest skill = UseSkillRequest.getInstance( name );

		int maximumCast = skill.getMaximumCast();
		if ( maximumCast <= 0 )
		{
			return true;
		}

		int castCount = Math.min( casts, maximumCast );
		if ( castCount > 0 && !allowRestore )
		{
			int available = KoLCharacter.getCurrentMP() - manaRemaining;
			int perCast = SkillDatabase.getMPConsumptionById( SkillDatabase.getSkillId( name ) );
			if ( perCast != 0 )
			{
				castCount = Math.min( castCount, available / perCast );
			}
		}

		if ( castCount == 0 )
		{
			return false;
		}

		skill.setBuffCount( castCount );
		RequestThread.postRequest( skill );

		return castCount == maximumCast && UseSkillRequest.lastUpdate.equals( "" );
	}

	public static boolean castBookSkills( final boolean allowRestore, final int manaRemaining )
	{
		boolean done = true;

		done &= castBookSkills( getBreakfastTomeSkills(), BookType.TOME, allowRestore, manaRemaining );
		done &= castBookSkills( getBreakfastGrimoireSkills(), BookType.GRIMOIRE, allowRestore, manaRemaining );
		castBookSkills( getBreakfastLibramSkills(), BookType.LIBRAM, allowRestore, manaRemaining );

		return done;
	}

	public static List getBreakfastTomeSkills()
	{
		return BreakfastManager.getBreakfastBookSkills( "tomeSkills", UseSkillRequest.TOME_SKILLS );
	}

	public static List getBreakfastGrimoireSkills()
	{
		return BreakfastManager.getBreakfastBookSkills( "grimoireSkills", UseSkillRequest.GRIMOIRE_SKILLS );
	}

	public static List getBreakfastLibramSkills()
	{
		return BreakfastManager.getBreakfastBookSkills( "libramSkills", UseSkillRequest.LIBRAM_SKILLS );
	}

	private static List getBreakfastBookSkills( final String setting, final String [] skills )
	{
		String suffix = KoLCharacter.canInteract() ? "Softcore" : "Hardcore";
		String name = Preferences.getString( setting + suffix );
		ArrayList<String> list = new ArrayList<String>();

		if ( name.equals( "none" ) || name.equals( "" ) )
		{
			return list;
		}

		// This check is only here because the skill name was changed
		// It can likely be removed eventually
		// Added October 2014
		if ( name.equals( "Summon Candy Hearts" ) )
		{
			name = "Summon Candy Heart";
			Preferences.setString( setting + suffix, name );
		}

		boolean castAll = name.equals( "all" );

		// Determine how many skills we will cast from this list
		for ( int i = 0; i < skills.length; ++i )
		{
			String skillName = skills[ i ];

			if ( !castAll && !name.equals( skillName ) )
			{
				continue;
			}

			if ( !KoLCharacter.hasSkill( skillName ) )
			{
				continue;
			}

			list.add( skillName );
		}

		return list;
	}

	public static boolean castBookSkills( final List castable, final BookType type, final boolean allowRestore, final int manaRemaining )
	{
		int skillCount = castable.size();

		// If none, we are done
		if ( skillCount == 0 )
		{
			return true;
		}

		// Determine total number of times we will try to use skills of
		// this type.

		int totalCasts = 0;

		switch ( type )
		{
		case TOME:
			// In Ronin or Hardcore, Tomes can be used three times a day,
			// spread among all available tomes.
			// In other cases, all available tomes can be cast three times a day.
			totalCasts = KoLCharacter.canInteract() ? skillCount * 3 : 3;
			break;
		case GRIMOIRE:
			// Grimoires can be used once a day, each.
			totalCasts = skillCount;
			break;
		case LIBRAM:
			// Librams can be used as many times per day as you
			// have mana available.
			totalCasts = SkillDatabase.libramSkillCasts( KoLCharacter.getCurrentMP() - manaRemaining );
			// Note that if we allow MP to be restored, we could
			// potentially summon a lot more. Maybe someday...
			break;
		}

		if ( skillCount == 1 )
		{
			// We are casting exactly one skill from this list.
			String skillName = (String) castable.get(0);
			return BreakfastManager.castSkill( skillName, totalCasts, allowRestore, manaRemaining );
		}

		// Determine number of times we will cast each skill. Divide
		// evenly, with any excess going to first skill.

		int nextCast = totalCasts / skillCount;
		int cast = nextCast + totalCasts - ( nextCast * skillCount );

		boolean done = true;

		// We are casting more than one skill from this list. Cast one
		// at a time until we are done.

		for ( int i = 0; i < skillCount; ++i )
		{
			String skillName = (String) castable.get(i);

			done &= BreakfastManager.castSkill( skillName, cast, allowRestore, manaRemaining );
			cast = nextCast;
		}

		return done;
	}

	private static void checkJackass()
	{
		if ( Preferences.getBoolean( "_defectiveTokenChecked" ) || Limitmode.limitZone( "Town" ) )
		{
			return;
		}

		if ( Preferences.getBoolean( "checkJackass" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			ArcadeRequest.checkJackassPlumber();
			KoLmafia.forceContinue();
		}
	}

	private static void makePocketWishes()
	{
		if ( !InventoryManager.hasItem( ItemPool.GENIE_BOTTLE ) )
		{
			return;
		}

		// Can't use Pocket Wishes in G Lover
		if ( KoLCharacter.inGLover() )
		{
			return;
		}

		if ( Preferences.getBoolean( "makePocketWishes" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			int num = 3 - Preferences.getInteger( "_genieWishesUsed" );
			for ( int i = 0; i < num; i++ )
			{
				RequestThread.postRequest( new GenieRequest( "for more wishes" ) );
				KoLmafia.forceContinue();
			}
		}
	}

	public static void visitVolcanoIsland()
	{
		if ( !Limitmode.limitZone( "Volcano" ) )
		{
			VolcanoIslandRequest.getBreakfast();
		}
	}

	public static void visitBigIsland()
	{
		if ( Limitmode.limitZone( "Island" ) || Limitmode.limitZone( "IsleWar" ) )
		{
			return;
		}

		// Don't visit the Farm Stand in Fistcore, since you will just
		// donate the profits to charity

		if ( Preferences.getInteger( "lastFilthClearance" ) == KoLCharacter.getAscensions() &&
		     !KoLCharacter.inFistcore() )
		{
			visitHippy();
		}

		if ( !IslandManager.warProgress().equals( "started" ) )
		{
			return;
		}

		SpecialOutfit hippy = EquipmentDatabase.getAvailableOutfit( OutfitPool.WAR_HIPPY_OUTFIT );
		SpecialOutfit fratboy = EquipmentDatabase.getAvailableOutfit( OutfitPool.WAR_FRAT_OUTFIT );

		String lighthouse = Preferences.getString( "sidequestLighthouseCompleted" );
		SpecialOutfit lighthouseOutfit = sidequestOutfit( lighthouse, hippy, fratboy );

		String farm = Preferences.getString( "sidequestFarmCompleted" );
		SpecialOutfit farmOutfit = sidequestOutfit( farm, hippy, fratboy );

		// If we can't get to (or don't need to get to) either
		// sidequest location, nothing more to do.

		if ( lighthouseOutfit == null && farmOutfit == null )
		{
			return;
		}

		// Visit locations accessible in current outfit

		SpecialOutfit current = EquipmentManager.currentOutfit();

		if ( farmOutfit != null && current == farmOutfit )
		{
			visitFarmer();
			farmOutfit = null;
		}

		if ( lighthouseOutfit != null && current == lighthouseOutfit )
		{
			visitPyro();
			lighthouseOutfit = null;
		}

		// Visit locations accessible in one outfit

		current = nextOutfit( farmOutfit, lighthouseOutfit );
		if ( current == null )
		{
			return;
		}

		if ( current == farmOutfit )
		{
			visitFarmer();
			farmOutfit = null;
		}

		if ( current == lighthouseOutfit )
		{
			visitPyro();
			lighthouseOutfit = null;
		}

		// Visit locations accessible in other outfit

		current = nextOutfit( farmOutfit, lighthouseOutfit );
		if ( current == null )
		{
			return;
		}

		if ( current == farmOutfit )
		{
			visitFarmer();
			farmOutfit = null;
		}

		if ( current == lighthouseOutfit )
		{
			visitPyro();
			lighthouseOutfit = null;
		}
	}

	public static void visitHippy()
	{
		if ( Preferences.getBoolean( "_hippyMeatCollected" ) )
		{
			return;
		}
		KoLmafia.updateDisplay( "Collecting cut of hippy profits..." );
		RequestThread.postRequest( new GenericRequest( "shop.php?whichshop=hippy" ) );
		KoLmafia.forceContinue();
	}

	public static void visitFarmer()
	{
		IslandRequest request = IslandRequest.getFarmerRequest();
		if ( request != null )
		{
			RequestThread.postRequest( request );
			KoLmafia.forceContinue();
		}
	}

	public static void visitPyro()
	{
		IslandRequest request = IslandRequest.getPyroRequest();
		if ( request != null )
		{
			RequestThread.postRequest( request );
			KoLmafia.forceContinue();
		}
	}

	private static SpecialOutfit nextOutfit( final SpecialOutfit one, final SpecialOutfit two )
	{
		SpecialOutfit outfit = ( one != null ) ? one : two;
		if ( outfit != null )
		{
			RequestThread.postRequest( new EquipmentRequest( outfit ) );
		}
		return outfit;
	}

	public static SpecialOutfit sidequestOutfit( String winner, final SpecialOutfit hippy, final SpecialOutfit fratboy )
	{
		if ( winner.equals( "hippy" ) )
		{
			return hippy;
		}

		if ( winner.equals( "fratboy" ) )
		{
			return fratboy;
		}

		return null;
	}

	private static void collectAnticheese()
	{
		if ( KoLCharacter.desertBeachAccessible() && KoLCharacter.getCurrentDays() >= Preferences.getInteger( "lastAnticheeseDay" ) + 5 )
		{
			RequestThread.postRequest( new PlaceRequest( "desertbeach", "db_nukehouse" ) );
		}
	}

	private static void collectSeaJelly()
	{
		if ( Limitmode.limitZone( "The Sea" ) || Limitmode.limitFamiliars() )
		{
			return;
		}

		if ( Preferences.getBoolean( "_seaJellyHarvested" ) )
		{
			return;
		}

		if ( QuestDatabase.isQuestStep( QuestDatabase.Quest.SEA_OLD_GUY, QuestDatabase.UNSTARTED ) )
		{
			return;
		}

		FamiliarData jellyfish = KoLCharacter.findFamiliar( FamiliarPool.SPACE_JELLYFISH );
		if ( jellyfish == null )
		{
			return;
		}

		KoLmafia.updateDisplay( "Collecting sea jelly..." );

		FamiliarData currentFam = KoLCharacter.getFamiliar();

		RequestThread.postRequest( new FamiliarRequest( jellyfish ) );
		RequestThread.postRequest( new PlaceRequest( "thesea", "thesea_left2", false ) );
		RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1219&option=1" ) );
		RequestThread.postRequest( new FamiliarRequest( currentFam ) );	

		KoLmafia.forceContinue();
	}
}
