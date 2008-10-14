/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import java.io.BufferedReader;
import java.util.ArrayList;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

/**
 * An extension of a <code>GenericRequest</code> which specifically handles donating to the Hall of the Legends of the
 * Times of Old.
 */

public class SendGiftRequest
	extends TransferItemRequest
{
	private final int desiredCapacity;
	private final String recipient, message;
	private final GiftWrapper wrappingType;
	private final int maxCapacity, materialCost;
	private static final LockableListModel PACKAGES = new LockableListModel();
	static
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "packages.txt", KoLConstants.PACKAGES_VERSION );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length >= 4 )
			{
				SendGiftRequest.PACKAGES.add( new GiftWrapper(
					data[ 0 ], StringUtilities.parseInt( data[ 1 ] ),
					StringUtilities.parseInt( data[ 2 ] ),
					StringUtilities.parseInt( data[ 3 ] ) ) );
			}
		}
	}

	private static class GiftWrapper
	{
		private final StringBuffer name;
		private final int radio, maxCapacity, materialCost;

		public GiftWrapper( final String name, final int radio, final int maxCapacity, final int materialCost )
		{
			this.radio = radio;
			this.maxCapacity = maxCapacity;
			this.materialCost = materialCost;

			this.name = new StringBuffer();
			this.name.append( "Send it in a " );
			this.name.append( name );
			this.name.append( " for " );
			this.name.append( materialCost );
			this.name.append( " meat" );
		}

		public String toString()
		{
			return this.name.toString();
		}
	}

	public SendGiftRequest( final String recipient, final String message, final int desiredCapacity,
		final Object[] attachments )
	{
		this( recipient, message, desiredCapacity, attachments, false );
	}

	public SendGiftRequest( final String recipient, final String message, final int desiredCapacity,
		final Object[] attachments, final boolean isFromStorage )
	{
		super( "town_sendgift.php", attachments );

		this.recipient = recipient;
		this.message = CharacterEntities.unescape( message );
		this.desiredCapacity = desiredCapacity;

		this.wrappingType = (GiftWrapper) SendGiftRequest.PACKAGES.get( desiredCapacity );
		this.maxCapacity = this.wrappingType.maxCapacity;
		this.materialCost = this.wrappingType.materialCost;

		this.addFormField( "action", "Yep." );
		this.addFormField( "towho", KoLmafia.getPlayerId( this.recipient ) );
		this.addFormField( "note", this.message );
		this.addFormField( "insidenote", this.message );
		this.addFormField( "whichpackage", String.valueOf( this.wrappingType.radio ) );

		// You can take from inventory (0) or Hagnks (1)
		this.addFormField( "fromwhere", isFromStorage ? "1" : "0" );

		if ( isFromStorage )
		{
			this.source = KoLConstants.storage;
			this.destination = new ArrayList();
		}
	}

	public int getCapacity()
	{
		return this.maxCapacity;
	}

	public boolean alwaysIndex()
	{
		return true;
	}

	public TransferItemRequest getSubInstance( final Object[] attachments )
	{
		return new SendGiftRequest(
			this.recipient, this.message, this.desiredCapacity, attachments, this.source == KoLConstants.storage );
	}

	public String getSuccessMessage()
	{
		return "<td>Package sent.</td>";
	}

	public String getItemField()
	{
		return this.source == KoLConstants.storage ? "hagnks_whichitem" : "whichitem";
	}

	public String getQuantityField()
	{
		return this.source == KoLConstants.storage ? "hagnks_howmany" : "howmany";
	}

	public String getMeatField()
	{
		return this.source == KoLConstants.storage ? "hagnks_sendmeat" : "sendmeat";
	}

	public static final LockableListModel getPackages()
	{
		// Which packages are available depends on ascension count.
		// You start with two packages and receive an additional
		// package every three ascensions you complete.

		LockableListModel packages = new LockableListModel();
		int packageCount = Math.min( KoLCharacter.getAscensions() / 3 + 2, 11 );

		packages.addAll( SendGiftRequest.PACKAGES.subList( 0, packageCount + 1 ) );
		return packages;
	}

	public void processResults()
	{
		super.processResults();
		if ( this.responseText.indexOf( this.getSuccessMessage() ) != -1 && this.materialCost > 0 )
		{
			ResultProcessor.processResult( new AdventureResult( AdventureResult.MEAT, 0 - this.materialCost ) );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "town_sendgift.php" ) )
		{
			return false;
		}

		return TransferItemRequest.registerRequest(
			"send a gift", urlString,
			urlString.indexOf( "fromwhere=1" ) != -1 ? KoLConstants.storage : KoLConstants.inventory, null, "sendmeat",
			0 );
	}

	public boolean allowMementoTransfer()
	{
		return true;
	}

	public boolean allowUntradeableTransfer()
	{
		return true;
	}

	public String getStatusMessage()
	{
		return "Sending package to " + KoLmafia.getPlayerName( this.recipient );
	}
}
