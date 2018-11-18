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

import net.sourceforge.kolmafia.objectpool.EffectPool;

import net.sourceforge.kolmafia.persistence.EffectDatabase;

public class ModifierExpression
	extends Expression
{
	public ModifierExpression( String text, String name )
	{
		super( text, name );
	}

	public static ModifierExpression getInstance( String text, String name )
	{
		ModifierExpression expr = new ModifierExpression( text, name );
		String errors = expr.getExpressionErrors();
		if ( errors != null )
		{
			KoLmafia.updateDisplay( errors );
		}
		return expr;
	}

	@Override
	protected void initialize()
	{
		// The first check also matches "[zone(The Slime Tube)]"
		// Hence the second check.
		String type = Modifiers.getTypeFromLookup( this.name );
		if ( type.equals( "Effect" ) && text.contains( "T" ) )
		{
			int effectId = EffectDatabase.getEffectId( Modifiers.getNameFromLookup( this.name ) );
			if ( effectId != -1 )
			{
				this.effect = EffectPool.get( effectId, 0 );
			}
		}
	}

	@Override
	protected String validBytecodes()
	{
		return super.validBytecodes() + "ABCDEFGHIJKLMNPRSTUWXY";
	}

	@Override
	protected String function()
	{
		if ( this.optional( "loc(" ) )
		{
			return this.literal( this.until( ")" ), 'l' );
		}
		if ( this.optional( "zone(" ) )
		{
			return this.literal( this.until( ")" ), 'z' );
		}
		if ( this.optional( "event(" ) )
		{
			return this.literal( this.until( ")" ), 'v' );
		}
		if ( this.optional( "fam(" ) )
		{
			return this.literal( this.until( ")" ), 'w' );
		}
		if ( this.optional( "famattr(" ) )
		{
			return this.literal( this.until( ")" ), 'i' );
		}
		if ( this.optional( "mainhand(" ) )
		{
			return this.literal( this.until( ")" ), 'h' );
		}
		if ( this.optional( "equipped(" ) )
		{
			return this.literal( this.until( ")" ).toLowerCase(), 'g' );
		}
		if ( this.optional( "effect(" ) )
		{
			return this.literal( this.until( ")" ), 'e' );
		}
		if ( this.optional( "res(" ) )
		{
			return this.literal( this.until( ")" ), 'b' );
		}
		if ( this.optional( "class(" ) )
		{
			return this.literal( this.until( ")" ), 'n' );
		}
		if ( this.optional( "skill(" ) )
		{
			return this.literal( this.until( ")" ), 'd' );
		}
		if ( this.optional( "env(" ) )
		{
			return this.literal( this.until( ")" ), 'j' );
		}
		if ( this.optional( "path(" ) )
		{
			return this.literal( this.until( ")" ), '\u0092' );
		}
		if ( this.optional( "mod(" ) )
		{
			return this.literal( this.until( ")" ), '\u0093' );
		}
		if ( this.optional( "interact(" ) )
		{
			return this.literal( this.until( ")" ), '\u0094' );
		}
		if ( this.optional( "mus" ) )
		{
			return "\u0080";
		}
		if ( this.optional( "mys" ) )
		{
			return "\u0081";
		}
		if ( this.optional( "mox" ) )
		{
			return "\u0082";
		}

		return null;
	}
}
