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

import net.sourceforge.kolmafia.preferences.Preferences;

public class PyramidRequest
{
	public static final String getPyramidLocationString( final String urlString )
	{
		if ( !urlString.contains( "pyramid_state" ) )
		{
			return null;
		}

		String position = PyramidRequest.getPyramidPositionString();
		return "The Lower Chambers (" + position + ")";
	}

	public static final String getPyramidPositionString()
	{
		switch ( Preferences.getInteger( "pyramidPosition" ) )
		{
		case 1:
			return !Preferences.getBoolean( "pyramidBombUsed" ) ?
				"Empty/Rubble": "Empty/Empty/Ed's Chamber";
		case 2:
			return "Rats/Token";
		case 3:
			return "Rubble/Bomb";
		case 4:
			return "Token/Empty";
		case 5:
			return "Bomb/Rats";
		}

		return "Unknown";
	}

	public static final int advancePyramidPosition()
	{
		int position = Preferences.getInteger( "pyramidPosition" );
		if ( ++position > 5 )
		{
			position = 1;
		}
		Preferences.setInteger( "pyramidPosition", position );
		return position;
	}
}
