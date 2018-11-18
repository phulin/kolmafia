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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

public class EudoraCommand
	extends AbstractCommand
{
	public EudoraCommand()
	{
		this.usage = " penpal|game|xi|newyou - switch to the specified correspondent";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.trim();

		String requestString = "account.php?am=1&action=whichpenpal&ajax=1&pwd=" +
			  GenericRequest.passwordHash + "&value=";

		if ( parameters.equals( "penpal" ) )
		{
			GenericRequest request = new GenericRequest( requestString + "1" );
			request.run();
			ApiRequest.updateStatus();
			if ( KoLCharacter.getEudora().equals( "Penpal" ) )
			{
				KoLmafia.updateDisplay( "Switched to Pen Pal" );
			}
			else
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Cannot switch to Pen Pal" );
			}
		}
		else if ( parameters.equals( "game" ) )
		{
			GenericRequest request = new GenericRequest( requestString + "2" );
			request.run();
			ApiRequest.updateStatus();
			if ( KoLCharacter.getEudora().equals( "GameInformPowerDailyPro Magazine" ) )
			{
				KoLmafia.updateDisplay( "Switched to Game Magazine" );
			}
			else
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Cannot switch to Game Magazine" );
			}
		}
		else if ( parameters.equals( "xi" ) )
		{
			GenericRequest request = new GenericRequest( requestString + "3" );
			request.run();
			ApiRequest.updateStatus();
			if ( KoLCharacter.getEudora().equals( "Xi Receiver Unit" ) )
			{
				KoLmafia.updateDisplay( "Switched to Xi Receiver" );
			}
			else
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Cannot switch to Xi Receiver" );
			}
		}
		else if ( parameters.equals( "newyou" ) )
		{
			GenericRequest request = new GenericRequest( requestString + "4" );
			request.run();
			ApiRequest.updateStatus();
			if ( KoLCharacter.getEudora().equals( "New-You Club" ) )
			{
				KoLmafia.updateDisplay( "Switched to New-You Club" );
			}
			else
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Cannot switch to New-You Club" );
			}
		}
		else if ( parameters.length() == 0 )
		{
			KoLmafia.updateDisplay( "Current correspondent is " + KoLCharacter.getEudora() );
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "That is not a valid correspondent" );
		}
	}
}
