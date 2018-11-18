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

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.EffectPool;

import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

public class ToggleCommand
	extends AbstractCommand
{
	public ToggleCommand()
	{
		this.usage = " [effect] - Toggle an effect to another effect";
	}

	// The plan is to have this be a more generic command for toggling effects, in case that becomes relevant.
	// Since there is only a single pair of effects to toggle currently, the parameter isn't actually used.
	@Override
	public void run( final String cmd, String parameters )
	{
		if ( KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.INTENSELY_INTERESTED ) ) ||
		     KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.SUPERFICIALLY_INTERESTED ) ) )
		{
			GenericRequest request = new CharSheetRequest();
			request.addFormField( "action", "newyouinterest" );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have an effect to toggle." );
		}
	}

}
