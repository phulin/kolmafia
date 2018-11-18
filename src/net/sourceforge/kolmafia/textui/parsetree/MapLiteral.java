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

package net.sourceforge.kolmafia.textui.parsetree;

import java.util.Iterator;
import java.util.List;

import net.sourceforge.kolmafia.textui.Interpreter;

public class MapLiteral
	extends AggregateLiteral
{
	private AggregateValue aggr = null;
	private final List<Value> keys;
	private final List<Value> values;

        public MapLiteral( final AggregateType type, final List<Value> keys, final List<Value> values )
	{
		super( type );
		this.keys = keys;
		this.values = values;
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		this.aggr = (AggregateValue)this.type.initialValue();

		Iterator<Value> keyIterator = this.keys.iterator();
		Iterator<Value> valIterator = this.values.iterator();

		while ( keyIterator.hasNext() && valIterator.hasNext() )
		{
			Value key = keyIterator.next().execute( interpreter );
			Value val = valIterator.next().execute( interpreter );
			this.aggr.aset( key, val );
		}

		return this.aggr;
	}

	@Override
	public int count()
	{
		if ( this.aggr != null )
		{
			return this.aggr.count();
		}
		return this.keys.size();
	}
}
