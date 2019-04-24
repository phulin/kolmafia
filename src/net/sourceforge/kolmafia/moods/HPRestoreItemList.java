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
 * "AS IS" AND ANY EXPRESS OR IHPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IHPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEHPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.moods;

import java.util.HashMap;

import javax.swing.JCheckBox;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ChateauRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest.RequestType;
import net.sourceforge.kolmafia.request.FalloutShelterRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.Limitmode;

import net.sourceforge.kolmafia.textui.command.NunneryCommand;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class HPRestoreItemList
{
	private static boolean purchaseBasedSort = false;
	private static HashMap<String, HPRestoreItem> restoreByName = new HashMap<String, HPRestoreItem>();

	public static final HPRestoreItem WALRUS = new HPRestoreItem( "Tongue of the Walrus", 35 );

	private static final HPRestoreItem SOFA = new HPRestoreItem( "sleep on your clan sofa", Integer.MAX_VALUE );
	private static final HPRestoreItem CHATEAU = new HPRestoreItem( "rest at the chateau", 250 );
	private static final HPRestoreItem CAMPGROUND = new HPRestoreItem( "rest at your campground", 40 );
	private static final HPRestoreItem FREEREST = new HPRestoreItem( "free rest", 40 );
	private static final HPRestoreItem DISCONAP = new HPRestoreItem( "Disco Nap", 20 );

	private static final HPRestoreItem NUNS = new HPRestoreItem( "visit the nuns", 1000 );
	private static final HPRestoreItem HERBS =
		new HPRestoreItem( "Medicinal Herb's medicinal herbs", Integer.MAX_VALUE, 100 );

	public static final HPRestoreItem HOTTUB = new HPRestoreItem( "relaxing hot tub", Integer.MAX_VALUE );
	public static final HPRestoreItem SCROLL = new HPRestoreItem( "scroll of drastic healing", Integer.MAX_VALUE );
	private static final HPRestoreItem MASSAGE_OIL = new HPRestoreItem( "scented massage oil", Integer.MAX_VALUE );
	private static final HPRestoreItem COCOON = new HPRestoreItem( "Cannelloni Cocoon", 1000 );
	private static final HPRestoreItem SHAKE_IT_OFF = new HPRestoreItem( "Shake It Off", Integer.MAX_VALUE );
	private static final HPRestoreItem GRUB = new HPRestoreItem( "plump juicy grub", 95 );
	private static final HPRestoreItem DOCS_UNGUENT = new HPRestoreItem( "Doc Galaktik's Pungent Unguent", 4, 30 );
	private static final HPRestoreItem DOCS_ELIXIR = new HPRestoreItem( "Doc Galaktik's Homeopathic Elixir", 19, 120 );
	private static final HPRestoreItem GELATINOUS_RECONSTRUCTION = new HPRestoreItem( "Gelatinous Reconstruction", 13 );

	public static final HPRestoreItem[] CONFIGURES = new HPRestoreItem[]
	{
		HPRestoreItemList.SOFA,
		HPRestoreItemList.CHATEAU,
		HPRestoreItemList.CAMPGROUND,
		HPRestoreItemList.FREEREST,
		HPRestoreItemList.HERBS,
		HPRestoreItemList.SCROLL,
		HPRestoreItemList.MASSAGE_OIL,
		HPRestoreItemList.COCOON,
		HPRestoreItemList.SHAKE_IT_OFF,
		HPRestoreItemList.NUNS,
		HPRestoreItemList.HOTTUB,
		new HPRestoreItem( "Camp Scout pup tent", 1000 ),
		new HPRestoreItem( "pixel energy tank", 1000 ),
		new HPRestoreItem( "extra-strength red potion", 200 ),
		new HPRestoreItem( "red pixel potion", 110 ),
		new HPRestoreItem( "really thick bandage", 109 ),
		new HPRestoreItem( "Pok&euml;mann band-aid", 100 ),
		new HPRestoreItem( "filthy poultice", 100 ),
		new HPRestoreItem( "gauze garter", 100 ),
		new HPRestoreItem( "red potion", 100 ),
		new HPRestoreItem( "bottle of Vangoghbitussin", 100 ),
		HPRestoreItemList.GRUB,
		new HPRestoreItem( "elven medi-pack", 90 ),
		new HPRestoreItem( "generic healing potion", 77 ),
		new HPRestoreItem( "generic restorative potion", 77 ),
		new HPRestoreItem( "cotton candy bale", 61 ),
		new HPRestoreItem( "ancient Magi-Wipes", 55 ),
		new HPRestoreItem( "cotton candy pillow", 51 ),
		new HPRestoreItem( "green pixel potion", 50 ),
		new HPRestoreItem( "cartoon heart", 50 ),
		new HPRestoreItem( "phonics down", 48 ),
		new HPRestoreItem( "cotton candy cone", 39 ),
		new HPRestoreItem( "palm-frond fan", 37 ),
		new HPRestoreItem( "honey-dipped locust", 36 ),
		HPRestoreItemList.WALRUS,
		new HPRestoreItem( "red paisley oyster egg", 33 ),
		new HPRestoreItem( "red polka-dot oyster egg", 33 ),
		new HPRestoreItem( "red striped oyster egg", 33 ),
		new HPRestoreItem( "cotton candy plug", 28 ),
		new HPRestoreItem( "tiny house", 22 ),
		new HPRestoreItem( "cotton candy skoshe", 22 ),
		HPRestoreItemList.DISCONAP,
		new HPRestoreItem( "Lasagna Bandages", 20 ),
		HPRestoreItemList.DOCS_ELIXIR,
		new HPRestoreItem( "cast", 17 ),
		new HPRestoreItem( "cotton candy smidgen", 17 ),
		new HPRestoreItem( "sugar shard", 15 ),
		new HPRestoreItem( "cotton candy pinch", 9 ),
		new HPRestoreItem( "forest tears", 7 ),
		HPRestoreItemList.DOCS_UNGUENT,
		new HPRestoreItem( "Notes from the Elfpocalypse, Chapter I", 35 ),
		new HPRestoreItem( "Notes from the Elfpocalypse, Chapter II", 35 ),
		new HPRestoreItem( "Notes from the Elfpocalypse, Chapter III", 35 ),
		new HPRestoreItem( "Notes from the Elfpocalypse, Chapter IV", 35 ),
		new HPRestoreItem( "Notes from the Elfpocalypse, Chapter V", 35 ),
		new HPRestoreItem( "Notes from the Elfpocalypse, Chapter VI", 35 ),
		new HPRestoreItem( "dueling turtle", 15 ),
		HPRestoreItemList.GELATINOUS_RECONSTRUCTION,
	};

	public static final void setPurchaseBasedSort( final boolean purchaseBasedSort )
	{
		HPRestoreItemList.purchaseBasedSort = purchaseBasedSort;
	}

	public static int getHealthRestored( String restoreName )
	{
		HPRestoreItem restoreItem = (HPRestoreItem) HPRestoreItemList.restoreByName.get( restoreName );
		return restoreItem == null ? Integer.MIN_VALUE : restoreItem.healthPerUse;
	}

	public static void updateHealthRestored()
	{
		HPRestoreItemList.CAMPGROUND.healthPerUse = KoLCharacter.getRestingHP();
		HPRestoreItemList.FREEREST.healthPerUse =
			(Preferences.getBoolean( "restUsingChateau" ) && Preferences.getBoolean( "chateauAvailable" ) ) ?
			250 :
			KoLCharacter.getRestingHP();
		HPRestoreItemList.SOFA.healthPerUse = KoLCharacter.getLevel() * 5 + 1;
		HPRestoreItemList.DISCONAP.healthPerUse = KoLCharacter.hasSkill( "Adventurer of Leisure" ) ? 40 : 20;
		HPRestoreItemList.DOCS_UNGUENT.purchaseCost = QuestDatabase.isQuestFinished( Quest.DOC ) ? 20 : 30;
		HPRestoreItemList.DOCS_ELIXIR.purchaseCost = QuestDatabase.isQuestFinished( Quest.DOC ) ? 80 : 120;
	}

	public static final boolean contains( final AdventureResult item )
	{
		return getHealthRestored( item.getName() ) != Integer.MIN_VALUE;
	}

	public static final JCheckBox[] getCheckboxes()
	{
		String hpRestoreSetting = Preferences.getString( "hpAutoRecoveryItems" );
		// Automatically convert changed restorative name
		hpRestoreSetting = StringUtilities.singleStringReplace( hpRestoreSetting, "free disco rest", "free rest" );
		JCheckBox[] restoreCheckbox = new JCheckBox[ HPRestoreItemList.CONFIGURES.length ];

		for ( int i = 0; i < HPRestoreItemList.CONFIGURES.length; ++i )
		{
			restoreCheckbox[ i ] = new JCheckBox( HPRestoreItemList.CONFIGURES[ i ].toString() );
			restoreCheckbox[ i ].setSelected( hpRestoreSetting.contains( HPRestoreItemList.CONFIGURES[ i ].toString().toLowerCase() ) );
		}

		return restoreCheckbox;
	}

	public static final void updateCheckboxes( final JCheckBox[] restoreCheckbox )
	{
		String hpRestoreSetting = Preferences.getString( "hpAutoRecoveryItems" );

		for ( int i = 0; i < HPRestoreItemList.CONFIGURES.length; ++i )
		{
			restoreCheckbox[ i ].setSelected( hpRestoreSetting.contains( HPRestoreItemList.CONFIGURES[ i ].toString().toLowerCase() ) );
		}
	}

	public static class HPRestoreItem
		implements Comparable<HPRestoreItem>
	{
		private final String restoreName;
		private int healthPerUse;
		private int purchaseCost;
		private int spleenHit;

		private int skillId;
		private AdventureResult itemUsed;

		public HPRestoreItem( final String restoreName, final int healthPerUse )
		{
			this( restoreName, healthPerUse, 0 );
		}

		public HPRestoreItem( final String restoreName, final int healthPerUse, final int purchaseCost )
		{
			this.restoreName = restoreName;
			this.healthPerUse = healthPerUse;
			this.purchaseCost = purchaseCost;
			this.spleenHit = 0;

			HPRestoreItemList.restoreByName.put( restoreName, this );

			if ( ItemDatabase.contains( restoreName ) )
			{
				this.itemUsed = ItemPool.get( restoreName, 1 );
				this.spleenHit = ConsumablesDatabase.getSpleenHit( restoreName );
				this.skillId = -1;
			}
			else if ( SkillDatabase.contains( restoreName ) )
			{
				this.itemUsed = null;
				this.skillId = SkillDatabase.getSkillId( restoreName );
			}
			else
			{
				this.itemUsed = null;
				this.skillId = -1;
			}
		}

		public boolean isSkill()
		{
			return this.skillId != -1;
		}

		public AdventureResult getItem()
		{
			return this.itemUsed;
		}

		public int getHealthRestored()
		{
			return Math.min( this.healthPerUse, KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP() );
		}

		public boolean usableInCurrentPath()
		{
			if ( KoLCharacter.isEd() )
			{
				return false;
			}

			if ( this.itemUsed == null )
			{
				return true;
			}

			int itemId = this.itemUsed.getItemId();

			if ( KoLCharacter.inBeecore() )
			{
				return !ItemDatabase.unusableInBeecore( itemId );
			}

			if ( KoLCharacter.inGLover() )
			{
				return !ItemDatabase.unusableInGLover( itemId );
			}

			return true;
		}

		public int compareTo( final HPRestoreItem o )
		{
			// Health restores are special because skills are preferred
			// over items, so test for that first.

			HPRestoreItem hpi = (HPRestoreItem) o;

			if ( this.itemUsed == null && hpi.itemUsed != null )
			{
				return -1;
			}
			if ( this.itemUsed != null && hpi.itemUsed == null )
			{
				return 1;
			}

			float restoreAmount = KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP();
			float leftRatio = restoreAmount / this.getHealthRestored();
			float rightRatio = restoreAmount / hpi.getHealthRestored();

			// If you're comparing skills, then you compare MP cost for
			// casting the skill, with more expensive skills coming later.

			if ( this.itemUsed == null && this.skillId > 0 )
			{
				leftRatio =
					(float) ( Math.ceil( leftRatio ) * SkillDatabase.getMPConsumptionById( this.skillId ) );
				rightRatio =
					(float) ( Math.ceil( rightRatio ) * SkillDatabase.getMPConsumptionById( hpi.skillId ) );
			}
			else if ( HPRestoreItemList.purchaseBasedSort )
			{
				if ( this.purchaseCost != 0 || hpi.purchaseCost != 0 )
				{
					leftRatio = (float) Math.ceil( leftRatio ) * this.purchaseCost;
					rightRatio = (float) Math.ceil( rightRatio ) * hpi.purchaseCost;
				}
			}

			float ratioDifference = leftRatio - rightRatio;
			return ratioDifference > 0.0f ? 1 : ratioDifference < 0.0f ? -1 : 0;
		}

		public void recoverHP( final int needed, final boolean purchase )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}

			// None of these work in Ed
			if ( KoLCharacter.isEd() )
			{
				return;
			}

			if ( this == HPRestoreItemList.GRUB &&
			     !KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.FORM_OF_BIRD ) ) )
			{
				return;
			}

			if ( this == HPRestoreItemList.CHATEAU )
			{
				if ( ChateauRequest.chateauRestUsable() )
				{
					RequestThread.postRequest( new ChateauRequest( "chateau_restbox" ) );
				}
				return;
			}

			if ( this == HPRestoreItemList.CAMPGROUND )
			{
				if ( Limitmode.limitCampground() || KoLCharacter.isEd() )
				{
					return;
				}
				if ( !KoLCharacter.inNuclearAutumn() )
				{
					RequestThread.postRequest( new CampgroundRequest( "rest" ) );
				}
				else
				{
					RequestThread.postRequest( new FalloutShelterRequest( "vault1" ) );
				}
				return;
			}

			if ( this == HPRestoreItemList.FREEREST )
			{
				if ( Preferences.getInteger( "timesRested" ) < KoLCharacter.freeRestsAvailable() )
				{
					if ( ChateauRequest.chateauRestUsable() )
					{
						RequestThread.postRequest( new ChateauRequest( "chateau_restbox" ) );
					}
					else if ( !Limitmode.limitCampground() && !KoLCharacter.isEd() && !KoLCharacter.inNuclearAutumn() )
					{
						RequestThread.postRequest( new CampgroundRequest( "rest" ) );
					}
				}
				return;
			}

			if ( this == HPRestoreItemList.NUNS )
			{
				if ( Preferences.getInteger( "nunsVisits" ) >= 3 ) return;
				String side = Preferences.getString( "sidequestNunsCompleted" );
				if ( !side.equals( "fratboy" ) && !side.equals( "hippy" ) ) return;
				if ( KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP() < 1000 )
				{
					// don't waste this limited resource on small restores
					return;
				}
				if ( side.equals( "fratboy" ) &&
					(KoLCharacter.getMaximumMP() - KoLCharacter.getCurrentMP() < 1000) )
				{
					// don't waste the MP restoration, either
					return;
				}

				NunneryCommand.visit( "hp" );
				return;
			}

			if ( this == HPRestoreItemList.HOTTUB )
			{
				if ( Preferences.getInteger( "_hotTubSoaks" ) >= 5 )
				{
					// done for the day
					return;
				}

				if ( KoLCharacter.getCurrentHP() > KoLCharacter.getMaximumHP() / 2 )
				{
					// don't waste this limited resource on small restores
					return;
				}

				RequestThread.postRequest( new ClanLoungeRequest( ClanLoungeRequest.HOTTUB ) );
				return;
			}

			// Can't use items that consume more spleen than we have left

			if ( this.spleenHit > 0 &&
			     this.spleenHit > KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse() )
			{
				return;
			}

			// For all other instances, you will need to calculate
			// the number of times this technique must be used.

			int hpShort = needed - KoLCharacter.getCurrentHP();
			if ( hpShort <= 0 )
			{
				return;
			}

			int numberToUse = Math.max( (int) Math.floor( (float) hpShort / (float) this.getHealthRestored() ), 1 );

			if ( this == HPRestoreItemList.SOFA )
			{
				RequestThread.postRequest( ( new ClanRumpusRequest( RequestType.SOFA ) ).setTurnCount( numberToUse ) );
				return;
			}

			if ( SkillDatabase.contains( this.restoreName ) )
			{
				if ( !KoLCharacter.hasSkill( this.restoreName ) )
				{
					numberToUse = 0;
				}
			}
			else if ( ItemDatabase.contains( this.restoreName ) )
			{
				// In certain instances, you are able to buy
				// more of the given item from NPC stores, or
				// from the mall.

				int numberAvailable = this.itemUsed.getCount( KoLConstants.inventory );
				int itemId = this.itemUsed.getItemId();

				if ( purchase && numberAvailable < numberToUse )
				{
					int numberToBuy = numberAvailable;
					int unitPrice = ItemDatabase.getPriceById( itemId ) * 2;

					if ( this == HPRestoreItemList.HERBS && NPCStoreDatabase.contains( itemId ) )
					{
						numberToBuy = Math.min( KoLCharacter.getAvailableMeat() / unitPrice, 3 );
					}
					else if ( NPCStoreDatabase.contains( itemId ) )
					{
						numberToBuy = Math.min( KoLCharacter.getAvailableMeat() / unitPrice, numberToUse );
					}

					// We may need to switch outfits to buy the
					// recovery item, but make sure we are wearing
					// our original outfit before consuming it.

					SpecialOutfit.createImplicitCheckpoint();
					boolean success = InventoryManager.retrieveItem( this.itemUsed.getInstance( numberToBuy ) );
					SpecialOutfit.restoreImplicitCheckpoint();

					if ( !success )
					{
						return;
					}

					numberAvailable = this.itemUsed.getCount( KoLConstants.inventory );
				}

				numberToUse = Math.min( numberToUse, numberAvailable );
			}
			else
			{
				// It's neither a skill nor an item
				// We should have handled it above
				return;
			}

			// If you don't have any items to use, then return
			// without doing anything.

			if ( numberToUse <= 0 || !KoLmafia.permitsContinue() )
			{
				return;
			}

			if ( SkillDatabase.contains( this.restoreName ) )
			{
				RequestThread.postRequest( UseSkillRequest.getInstance( this.restoreName, "", numberToUse ) );
			}
			else if ( ItemDatabase.contains( this.restoreName ) )
			{
				RequestThread.postRequest( UseItemRequest.getInstance( this.itemUsed.getInstance( numberToUse ) ) );
			}
		}

		@Override
		public String toString()
		{
			return this.restoreName;
		}
	}
}
