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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SingleUseRequest
	extends CreateItemRequest
{
	final private AdventureResult[] ingredients;

	public SingleUseRequest( final Concoction conc )
	{
		super( "inv_use.php", conc );
		this.ingredients = conc.getIngredients();
	}

	@Override
	public void reconstructFields()
	{
		if ( this.ingredients == null )
		{
			return;
		}

		int use = this.ingredients[ 0 ].getItemId();
		int type = ItemDatabase.getConsumptionType( use );
		int count = this.getQuantityNeeded();

		if ( type == KoLConstants.CONSUME_USE ||
			ItemDatabase.getAttribute( use, ItemDatabase.ATTR_USABLE ) ||
			count == 1 )
		{
			this.constructURLString( "inv_use.php" );
			this.addFormField( "which", "3" );
			this.addFormField( "whichitem", String.valueOf( use ) );
			this.addFormField( "ajax", "1" );
		}
		else if ( type == KoLConstants.CONSUME_MULTIPLE || type == KoLConstants.CONSUME_AVATAR ||
			ItemDatabase.getAttribute( use, ItemDatabase.ATTR_MULTIPLE ) )
		{
			this.constructURLString( "multiuse.php" );
			this.addFormField( "action", "useitem" );
			this.addFormField( "quantity", String.valueOf( count ) );
			this.addFormField( "whichitem", String.valueOf( use ) );
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR,
				"SingleUseRequest of item marked neither USABLE nor MULTIPLE" );
		}
	}

	@Override
	public void run()
	{
		AdventureResult item = this.ingredients[ 0 ];
		int itemId = item.getItemId();
		if ( KoLCharacter.inBeecore() && ItemDatabase.unusableInBeecore( itemId ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You are too scared of bees to use " + item.getName() + " to create " + this.getName() );
			return;
		}

		if ( KoLCharacter.inGLover() && ItemDatabase.unusableInGLover( itemId ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You are too in love with Gs to use " + item.getName() + " to create " + this.getName() );
			return;
		}

		// Attempting to make the ingredients will pull the
		// needed items from the closet if they are missing.

		if ( !this.makeIngredients() )
		{
			return;
		}

		int type = ItemDatabase.getConsumptionType( itemId );
		int count = this.getQuantityNeeded();

		if ( count > 1 &&
		     (type == KoLConstants.CONSUME_USE || ItemDatabase.getAttribute( itemId, ItemDatabase.ATTR_USABLE )) )
		{
			// We have to create one at a time.
			for ( int i = 1; i <= count; ++i )
			{
				KoLmafia.updateDisplay( "Creating " + this.getName() + " (" + i + " of " + count + ")..." );
				super.run();
			}
		}
		else
		{
			// We create all at once.
			KoLmafia.updateDisplay( "Creating " + this.getName() + " (" + count + ")..." );
			super.run();
		}
	}

	@Override
	public void processResults()
	{
		SingleUseRequest.parseResponse( UseItemRequest.lastItemUsed, this.responseText );
		UseItemRequest.lastItemUsed = null;
	}

	public static final void parseResponse( AdventureResult item, String responseText )
	{
		int baseId = item.getItemId();
		int count = item.getCount();
		String plural = ItemDatabase.getPluralName( baseId );
		if ( responseText.contains( "You don't have that many" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have that many " + plural );
			return;
		}
		if ( !responseText.contains( "You acquire" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Using " + count + " " + ( count == 1 ? item.getName() : plural ) + " doesn't make anything interesting." );
			return;
		}
		Concoction concoction = ConcoctionDatabase.singleUseCreation( baseId );

		if ( concoction == null )
		{
			return;
		}

		AdventureResult[] ingredients = concoction.getIngredients();

		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult ingredient = ingredients[ i ];
			ResultProcessor.processResult( ingredient.getInstance( -1 * ingredient.getCount() * count ) );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "multiuse.php" ) && !urlString.startsWith( "inv_use.php" ) )
		{
			return false;
		}

		Matcher itemMatcher = GenericRequest.WHICHITEM_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return false;
		}

		// Item ID of the base item
		int baseId = StringUtilities.parseInt( itemMatcher.group( 1 ) );

		if ( baseId == ItemPool.CHEWING_GUM )
		{
			return SewerRequest.registerRequest( urlString );
		}

		// Find result concoction
		Concoction concoction = ConcoctionDatabase.singleUseCreation( baseId );

		// If this is not a concoction, let somebody else log this.
		if ( concoction == null )
		{
			return false;
		}

		Matcher quantityMatcher = GenericRequest.QUANTITY_PATTERN.matcher( urlString );
		int count = quantityMatcher.find() ? StringUtilities.parseInt( quantityMatcher.group( 1 ) ) : 1;

		AdventureResult[] ingredients = concoction.getIngredients();

		// Punt if don't have enough of any ingredient.

		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult ingredient = ingredients[ i ];
			int have = ingredient.getCount( KoLConstants.inventory );
			int need = count * ingredient.getCount();
			if ( have < need )
			{
				return true;
			}
		}

		UseItemRequest.setLastItemUsed( ItemPool.get( baseId, count ) );

		StringBuilder text = new StringBuilder();
		text.append( "Use " );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult ingredient = ingredients[ i ];
			int used = count * ingredient.getCount();
			if ( i > 0 )
			{
				text.append( " + " );
			}

			text.append( used );
			text.append( " " );
			text.append( ingredient.getName() );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( text.toString() );

		return true;
	}
}
