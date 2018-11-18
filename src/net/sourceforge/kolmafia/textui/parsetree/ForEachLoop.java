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

import java.io.PrintStream;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.List;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Parser;

public class ForEachLoop
	extends Loop
{
	private final List<VariableReference> variableReferences;
	private final Value aggregate;

	// For runtime error messages
	String fileName;
	int lineNumber;

	public ForEachLoop( final Scope scope,
			    final List<VariableReference> variableReferences,
			    final Value aggregate, final Parser parser )
	{
		super( scope );
		this.variableReferences = variableReferences;
		this.aggregate = aggregate;
		this.fileName = parser.getShortFileName();
		this.lineNumber = parser.getLineNumber();
	}

	public List<VariableReference> getVariableReferences()
	{
		return this.variableReferences;
	}

	public Value getAggregate()
	{
		return this.aggregate;
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( Interpreter.STATE_EXIT );
			return null;
		}

		interpreter.traceIndent();
		if ( interpreter.isTracing() )
		{
			interpreter.trace( this.toString() );
		}

		// Evaluate the aggref to get the slice
		AggregateValue slice = (AggregateValue) this.aggregate.execute( interpreter );
		interpreter.captureValue( slice );
		if ( interpreter.getState() == Interpreter.STATE_EXIT )
		{
			interpreter.traceUnindent();
			return null;
		}

		// Iterate over the slice with bound keyvar

		ListIterator<VariableReference> it = this.variableReferences.listIterator();
		Value retval = this.executeSlice( interpreter, slice, it, it.next() );

		if ( interpreter.getState() == Interpreter.STATE_BREAK )
		{
			interpreter.setState( Interpreter.STATE_NORMAL );
		}

		return retval;
	}

	private Value executeSlice( final Interpreter interpreter, final AggregateValue slice,
				    final ListIterator<VariableReference> it, final VariableReference variable )
	{
		// Get the next key variable
		VariableReference nextVariable = it.hasNext() ? it.next() : null;

		// If the slice is an AggregateLiteral, must execute it to
		// initialize the values.
		if ( slice instanceof AggregateLiteral )
		{
			slice.execute( interpreter );
		}

		// Get an iterator over the keys for the slice
		Iterator<Value> keys = slice.iterator();
		
		int stackPos = interpreter.iterators.size();
		interpreter.iterators.add( null );	// key
		interpreter.iterators.add( slice );	// map
		interpreter.iterators.add( keys );	// iterator

		// While there are further keys
		while ( keys.hasNext() )
		{
			// Get current key
			Value key;

			try
			{
				key = keys.next();
				interpreter.iterators.set( stackPos, key );
			}
			catch ( ConcurrentModificationException e )
			{
				interpreter.setLineAndFile( this.fileName, this.lineNumber );
				throw interpreter.runtimeException( "Map modified within foreach" );
			}

			// Bind variable to key
			variable.setValue( interpreter, key );

			if ( interpreter.isTracing() )
			{
				interpreter.trace( "Key: " + key );
			}

			// If there are more indices to bind, recurse
			Value result;
			if ( nextVariable != null )
			{
				Value nextSlice = slice.aref( key, interpreter );
				if ( nextVariable.getType() instanceof AggregateType )
				{
					// typedef, for example
					nextVariable.setValue( interpreter, nextSlice );
					result = super.execute( interpreter );
				}
				else if ( nextSlice instanceof AggregateValue )
				{
					interpreter.traceIndent();
					result = this.executeSlice( interpreter, (AggregateValue) nextSlice, it, nextVariable );
				}
				else	// value var instead of key var
				{
					nextVariable.setValue( interpreter, nextSlice );
					result = super.execute( interpreter );
				}
			}
			else
			{
				// Otherwise, execute scope
				result = super.execute( interpreter );
			}

			if ( interpreter.getState() == Interpreter.STATE_NORMAL )
			{
				continue;
			}

			if ( nextVariable != null )
			{
				it.previous();
			}

			interpreter.traceUnindent();
			interpreter.iterators.remove( stackPos + 2 );
			interpreter.iterators.remove( stackPos + 1 );
			interpreter.iterators.remove( stackPos );
			return result;
		}

		if ( nextVariable != null )
		{
			it.previous();
		}

		interpreter.traceUnindent();
		interpreter.iterators.remove( stackPos + 2 );
		interpreter.iterators.remove( stackPos + 1 );
		interpreter.iterators.remove( stackPos );
		return DataTypes.VOID_VALUE;
	}

	@Override
	public String toString()
	{
		return "foreach";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<FOREACH>" );

		for ( VariableReference current : this.getVariableReferences() )
		{
			current.print( stream, indent + 1 );
		}

		this.getAggregate().print( stream, indent + 1 );
		this.getScope().print( stream, indent + 1 );
	}
}
