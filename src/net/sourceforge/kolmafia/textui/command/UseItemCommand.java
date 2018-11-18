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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;

import net.sourceforge.kolmafia.request.DrinkItemRequest;
import net.sourceforge.kolmafia.request.EatItemRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.SushiRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class UseItemCommand
	extends AbstractCommand
{
	public UseItemCommand()
	{
		this.usage = "[?] [either] <item> [, <item>]... - use/consume items";
	}

	@Override
	public void run( String command, final String parameters )
	{
		if ( command.equals( "overdrink" ) || command.equals( "drinksilent" ) )
		{
			DrinkItemRequest.ignorePrompt();
			command = "drink";
		}
		else if ( command.equals( "eatsilent" ) )
		{
			EatItemRequest.ignorePrompt();
			command = "eat";
		}

		String limitmode = KoLCharacter.getLimitmode();
		SpecialOutfit.createImplicitCheckpoint();

		UseItemCommand.use( command, parameters );

		if ( KoLCharacter.getLimitmode() != limitmode )
		{
			SpecialOutfit.discardImplicitCheckpoint();
		}
		else
		{
			SpecialOutfit.restoreImplicitCheckpoint();
		}
	}

	public static void use( final String command, String parameters )
	{
		UseItemCommand.use( command, parameters, false );
	}

	public static boolean use( final String command, String parameters, boolean sim )
	{
		if ( parameters.equals( "" ) )
		{
			return false;
		}

		boolean either = parameters.startsWith( "either " );
		if ( either )
		{
			parameters = parameters.substring( 7 ).trim();
		}

		if ( !sim && ( command.equals( "eat" ) || command.equals( "eatsilent" ) ) )
		{
			if ( KoLCharacter.inBadMoon() && KitchenCommand.visit( parameters ) )
			{
				return false;
			}
			if ( KoLCharacter.canadiaAvailable() && RestaurantCommand.makeChezSnooteeRequest( parameters ) )
			{
				return false;
			}
			if ( RestaurantCommand.makeHotDogStandRequest( command, parameters ) )
			{
				return false;
			}
		}

		if ( !sim && ( command.equals( "drink" ) || command.equals( "overdrink" ) ) )
		{
			if ( KoLCharacter.inBadMoon() && KitchenCommand.visit( parameters ) )
			{
				return false;
			}
			if ( KoLCharacter.gnomadsAvailable() && RestaurantCommand.makeMicroBreweryRequest( parameters ) )
			{
				return false;
			}
			if ( RestaurantCommand.makeSpeakeasyRequest( command, parameters ) )
			{
				return false;
			}
		}

		// Now, handle the instance where the first item is actually
		// the quantity desired, and the next is the amount to use
		int consumptionType = KoLConstants.NO_CONSUME;
		Match filter;

		if ( command.equals( "eat" ) || command.equals( "eatsilent" ) )
		{
			consumptionType = KoLConstants.CONSUME_EAT;
			filter = Match.FOOD;
		}
		else if ( command.equals( "ghost" ) )
		{
			consumptionType = KoLConstants.CONSUME_GHOST;
			filter = Match.FOOD;
		}
		else if ( command.equals( "drink" ) || command.equals( "overdrink" ) )
		{
			consumptionType = KoLConstants.CONSUME_DRINK;
			filter = Match.BOOZE;
		}
		else if ( command.equals( "hobo" ) )
		{
			consumptionType = KoLConstants.CONSUME_HOBO;
			filter = Match.BOOZE;
		}
		else if ( command.equals( "chew" ) )
		{
			consumptionType = KoLConstants.CONSUME_SPLEEN;
			filter = Match.SPLEEN;
		}
		else if ( command.equals( "slimeling" ) )
		{
			consumptionType = KoLConstants.CONSUME_SLIME;
			filter = Match.EQUIP;
		}
		else if ( command.equals( "robo" ) )
		{
			consumptionType = KoLConstants.CONSUME_ROBO;
			filter = Match.ROBO;
		}
		else
		{
			filter = Match.USE;
		}

		AdventureResult[] itemList = ItemFinder.getMatchingItemList( parameters, !sim, null, filter );

		for ( int level = either ? 0 : 2; level <= 2; ++level )
		{
			// level=0: use only items in inventory, exit on first success
			// level=1: buy/make as needed, exit on first success
			// level=2: use all items in list, buy/make as needed
			for ( AdventureResult currentMatch: itemList )
			{
				int itemId = currentMatch.getItemId();
				if ( itemId == -1 )
				{
					// We matched a name but didn't resolve
					// it to item ID. This can happen with
					// unidentified bang potions and slime
					// vials - or sushi
					String name = currentMatch.toString();
					String sushi = SushiRequest.isSushiName( name );
					if ( sushi != null )
					{
						if ( !sim )
						{
							RequestLogger.printLine( "For now, you must 'create " + sushi + "'" );
						}
						continue;
					}

					currentMatch = currentMatch.resolveBangPotion();
					itemId = currentMatch.getItemId();
				}

				if ( itemId == -1 )
				{
					if ( !sim )
					{
						RequestLogger.printLine( "You have not yet identified the " + currentMatch.toString() );
					}
					continue;
				}

				int consumpt = ItemDatabase.getConsumptionType( itemId );

				if ( command.equals( "eat" ) && consumpt == KoLConstants.CONSUME_FOOD_HELPER )
				{ // allowed
				}
				else if ( command.equals( "eat" ) || command.equals( "ghost" ) )
				{
					if ( consumpt != KoLConstants.CONSUME_EAT )
					{
						KoLmafia.updateDisplay(
							MafiaState.ERROR, currentMatch.getName() + " cannot be consumed." );
						return false;
					}
				}

				if ( command.equals( "drink" ) && consumpt == KoLConstants.CONSUME_DRINK_HELPER )
				{ // allowed
				}
				else if ( command.equals( "drink" ) || command.equals( "hobo" ) )
				{
					if ( consumpt != KoLConstants.CONSUME_DRINK )
					{
						KoLmafia.updateDisplay(
							MafiaState.ERROR, currentMatch.getName() + " is not an alcoholic beverage." );
						return false;
					}
				}
				else if ( command.equals( "chew" ) )
				{
					if ( consumpt != KoLConstants.CONSUME_SPLEEN )
					{
						KoLmafia.updateDisplay(
							MafiaState.ERROR, currentMatch.getName() + " is not a spleen toxin." );
						return false;
					}
				}

				if ( command.equals( "use" ) )
				{
					switch ( consumpt )
					{
					case KoLConstants.CONSUME_EAT:
					case KoLConstants.CONSUME_FOOD_HELPER:
						KoLmafia.updateDisplay( MafiaState.ERROR, currentMatch.getName() + " must be eaten." );
						return false;
					case KoLConstants.CONSUME_DRINK:
					case KoLConstants.CONSUME_DRINK_HELPER:
						KoLmafia.updateDisplay( MafiaState.ERROR, currentMatch.getName() + " must be drunk." );
						return false;
					case KoLConstants.CONSUME_SPLEEN:
						KoLmafia.updateDisplay( MafiaState.ERROR, currentMatch.getName() + " must be chewed." );
						return false;
					}
				}

				int have = currentMatch.getCount( KoLConstants.inventory );
				if ( level > 0 || have > 0 )
				{
					if ( level == 0 && have < currentMatch.getCount() )
					{
						currentMatch = currentMatch.getInstance( have );
					}
					if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
					{
						RequestLogger.printLine( currentMatch.toString() );
					}
					else
					{
						UseItemRequest request =
							consumptionType != KoLConstants.NO_CONSUME ? 
							UseItemRequest.getInstance( consumptionType, currentMatch ) :
							UseItemRequest.getInstance( currentMatch );

						if ( sim )
						{
							// UseItemRequest doesn't really have a "sim" mode, but we can do a pretty good approximation
							// by checking if maximumUses > 0 and we can physically retrieve the item.
							return	UseItemRequest.maximumUses( currentMatch.getItemId() ) > 0 &&
								!InventoryManager.simRetrieveItem( currentMatch ).equalsIgnoreCase( "fail" );
						}
						RequestThread.postRequest( request );
						while ( FightRequest.inMultiFight && KoLmafia.permitsContinue() )
						{
							FightRequest.INSTANCE.run();
						}
					}

					if ( level < 2 )
					{
						return false;
					}
				}
			}
		}
		return true;
	}
}
